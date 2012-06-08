package com.bt.pi.app.networkmanager.addressing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.id.PId;

@Component
public class AddressAssignmentExecutor {
    private static final Log LOG = LogFactory.getLog(AddressAssignmentExecutor.class);
    private static final String DEFAULT_PUBLIC_INTERFACE = "eth0";
    private static final String VNET_INTERFACE = "vnet.public.interface";
    private IpTablesManager ipTablesManager;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private PiIdBuilder piIdBuilder;
    private String publicInterface;
    private NetworkCommandRunner networkCommandRunner;

    public AddressAssignmentExecutor() {
        this.ipTablesManager = null;
        this.consumedDhtResourceRegistry = null;
        this.networkCommandRunner = null;
        this.piIdBuilder = null;
        this.publicInterface = DEFAULT_PUBLIC_INTERFACE;
    }

    @Resource
    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    @Resource
    public void setIpTablesManager(IpTablesManager aIpTablesManager) {
        this.ipTablesManager = aIpTablesManager;
    }

    @Resource
    public void setNetworkCommandRunner(NetworkCommandRunner aNetworkCommandRunner) {
        this.networkCommandRunner = aNetworkCommandRunner;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        piIdBuilder = aPiIdBuilder;
    }

    @Property(key = VNET_INTERFACE, defaultValue = DEFAULT_PUBLIC_INTERFACE)
    public void setVnetInterface(String value) {
        this.publicInterface = value;
    }

    public void assignPublicIpAddressToInstance(final String publicIpAddress, final String instanceId, final GenericContinuation<Boolean> resultContinuation) {
        LOG.debug(String.format("assignPublicIpAddressToInstance(%s, %s)", publicIpAddress, instanceId));
        Collection<SecurityGroup> securityGroups = (Collection<SecurityGroup>) consumedDhtResourceRegistry.getByType(SecurityGroup.class);
        // check that this address isn't already allocated
        for (SecurityGroup securityGroup : securityGroups) {
            if (securityGroup.containsPublicIp(publicIpAddress)) {
                LOG.warn(String.format("source address %s already allocated to security group: %s with id: %s", publicIpAddress, securityGroup.getOwnerIdGroupNamePair().getGroupName(), securityGroup.getUrl()));
            }
        }

        boolean foundSecGroup = false;
        for (SecurityGroup securityGroup : securityGroups) {
            // if security group has this instance id, set public ip address
            if (securityGroup.containsInstance(instanceId)) {
                foundSecGroup = true;
                LOG.debug(String.format("Assigning %s to instance %s on security group: %s", publicIpAddress, instanceId, securityGroup.getSecurityGroupId()));
                final List<String> releasedPublicIpAddresses = new ArrayList<String>();
                final PId secGroupId = piIdBuilder.getPId(securityGroup).forLocalRegion();
                consumedDhtResourceRegistry.update(secGroupId, new UpdateResolvingPiContinuation<SecurityGroup>() {
                    @Override
                    public SecurityGroup update(SecurityGroup existingEntity, SecurityGroup requestedEntity) {
                        LOG.debug(String.format("Updating security group: %s  instance: %s with public address: %s", existingEntity, instanceId, publicIpAddress));
                        releasedPublicIpAddresses.clear();

                        String existingPublicIpAddress = existingEntity.getInstances().get(instanceId).getPublicIpAddress();
                        if (existingPublicIpAddress != null) {
                            LOG.debug(String.format("Sec group %s had instance %s as using addr %s - provisionally releasing that address", existingEntity.getSecurityGroupId(), instanceId, existingPublicIpAddress));
                            releasedPublicIpAddresses.add(existingPublicIpAddress);
                        }

                        existingEntity.getInstances().get(instanceId).setPublicIpAddress(publicIpAddress);
                        return existingEntity;
                    }

                    @Override
                    public void handleException(Exception e) {
                        LOG.debug(String.format("Handling exception %s whilst assigning %s to %s", e.getMessage(), publicIpAddress, instanceId));
                        resultContinuation.handleResult(false);
                    }

                    @Override
                    public void handleResult(SecurityGroup result) {
                        LOG.debug(String.format("Assign address %s to %s write result: %s", publicIpAddress, instanceId, result));
                        if (result == null)
                            throw new AddressNotAssignedException(String.format("Unable to assign %s to instance %s", publicIpAddress, instanceId));

                        if (!releasedPublicIpAddresses.isEmpty()) {
                            LOG.debug(String.format("Releasing discarded public IP addresses %s", releasedPublicIpAddresses));
                            for (String releasedPublicIpAddress : releasedPublicIpAddresses)
                                networkCommandRunner.ipAddressDelete(releasedPublicIpAddress, publicInterface);
                        }

                        networkCommandRunner.addIpAddressAndSendArping(publicIpAddress, publicInterface);
                        ipTablesManager.refreshIpTables();

                        resultContinuation.handleResult(true);
                    }
                });
            }
        }
        if (!foundSecGroup) {
            LOG.warn(String.format("No sec groups containing instace %s found when trying to assign %s to that instance", instanceId, publicIpAddress));
            resultContinuation.handleResult(false);
        }
    }

    public void unassignPublicIpAddressFromInstance(final String instanceId, final String securityGroupId, final GenericContinuation<Boolean> resultContinuation) {
        LOG.debug(String.format("unassignPublicIpAddressFromInstance(%s, %s)", instanceId, securityGroupId));
        final AtomicBoolean noAddressAssignedToInstance = new AtomicBoolean();
        final List<String> releasedAddrs = new ArrayList<String>();
        PId securityGroupDhtId = piIdBuilder.getPId(SecurityGroup.getUrl(securityGroupId)).forLocalRegion();
        consumedDhtResourceRegistry.update(securityGroupDhtId, new UpdateResolvingPiContinuation<SecurityGroup>() {
            @Override
            public SecurityGroup update(SecurityGroup existingEntity, SecurityGroup requestedEntity) {
                InstanceAddress instanceAddress = existingEntity.getInstances().get(instanceId);
                if (instanceAddress != null && instanceAddress.getPublicIpAddress() != null) {
                    String existingPublicIpAddress = existingEntity.getInstances().get(instanceId).getPublicIpAddress();
                    LOG.debug(String.format("Setting public address for instance %s in sec group %s to null (currently set to %s)", instanceId, securityGroupId, existingPublicIpAddress));
                    releasedAddrs.clear();
                    if (existingPublicIpAddress != null)
                        releasedAddrs.add(existingPublicIpAddress);
                    existingEntity.getInstances().get(instanceId).setPublicIpAddress(null);
                    return existingEntity;
                } else {
                    LOG.info(String.format("Instance %s had no allocated addresses in record for security group %s", instanceId, securityGroupId));
                    noAddressAssignedToInstance.set(true);
                    return null;
                }
            }

            @Override
            public void handleException(Exception e) {
                LOG.debug(String.format("Handling exception %s whilst unassigning public address from %s", e.getMessage(), instanceId));
                resultContinuation.handleResult(false);
            }

            @Override
            public void handleResult(SecurityGroup result) {
                LOG.debug(String.format("Address unassignment for instance %s updated sec group %s", instanceId, result));
                if (result == null) {
                    boolean isSuccess = noAddressAssignedToInstance.get();
                    resultContinuation.handleResult(isSuccess);
                    return;
                }

                if (releasedAddrs.size() > 0) {
                    LOG.debug(String.format("Removing released addr %s", releasedAddrs.get(0)));
                    networkCommandRunner.ipAddressDelete(releasedAddrs.get(0), publicInterface);
                }

                ipTablesManager.refreshIpTables();
                resultContinuation.handleResult(true);
            }
        });
    }
}
