/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.net;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.Continuation;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.watchers.securitygroup.InstanceNetworkManager;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.SubnetAllocationResult;
import com.bt.pi.app.common.net.SubnetAllocator;
import com.bt.pi.app.common.net.VlanAllocator;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;
import com.bt.pi.app.networkmanager.addressing.PrivateIpAddressManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.id.PId;

@Component
public class NetworkManager implements InstanceNetworkManager {
    private static final String DELEGATING_HANDLING_OF_EXCEPTION_S_S_TO_CALLING_CONTINUATION = "Delegating handling of exception %s (%s) to calling continuation";
    private static final Log LOG = LogFactory.getLog(NetworkManager.class);
    private VirtualNetworkBuilder virtualNetworkBuilder;
    private PiIdBuilder piIdBuilder;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private PrivateIpAddressManager privateIpAddressManager;
    private VlanAllocator vlanAllocator;
    private SubnetAllocator subnetAllocator;

    public NetworkManager() {
        super();
        this.virtualNetworkBuilder = null;
        this.subnetAllocator = null;
        this.piIdBuilder = null;
        this.vlanAllocator = null;
        this.consumedDhtResourceRegistry = null;
        this.privateIpAddressManager = null;
    }

    @Resource
    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    @Resource
    public void setVirtualNetworkBuilder(VirtualNetworkBuilder aVirtualNetworkBuilder) {
        this.virtualNetworkBuilder = aVirtualNetworkBuilder;
    }

    @Resource
    public void setVlanAllocator(VlanAllocator aVlanAllocator) {
        this.vlanAllocator = aVlanAllocator;
    }

    @Resource
    public void setSubnetAllocator(SubnetAllocator aSubnetAllocator) {
        this.subnetAllocator = aSubnetAllocator;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setPrivateIpAddressManager(PrivateIpAddressManager aPrivateIpAddressManager) {
        this.privateIpAddressManager = aPrivateIpAddressManager;
    }

    public void setupNetworkForInstance(final Instance instance, final Continuation<Instance, Exception> resContinuation) {
        LOG.debug(String.format("setupNetworkForInstance(%s)", instance));

        final PId securityGroupRecordId = piIdBuilder.getPId(SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName())).forLocalRegion();
        final String securityGroupId = String.format(SecurityGroup.SEC_GROUP_ID_FORMAT_STRING, instance.getUserId(), instance.getSecurityGroupName());
        LOG.debug("Security group DHT ID: " + securityGroupRecordId.toStringFull());

        consumedDhtResourceRegistry.registerConsumer(securityGroupRecordId, instance.getInstanceId(), SecurityGroup.class, new GenericContinuation<Boolean>() {
            @Override
            public void handleResult(final Boolean isFirst) {
                LOG.debug(String.format("Registered instance %s for sec group %s, was first consumer? %s", instance.getInstanceId(), instance.getSecurityGroupName(), isFirst));
                vlanAllocator.allocateVlanInLocalRegion(securityGroupId, new GenericContinuation<Long>() {
                    @Override
                    public void handleException(Exception exception) {
                        LOG.debug(String.format(DELEGATING_HANDLING_OF_EXCEPTION_S_S_TO_CALLING_CONTINUATION, exception.getClass().getName(), exception.getMessage()));
                        consumedDhtResourceRegistry.deregisterConsumer(securityGroupRecordId, instance.getInstanceId());
                        resContinuation.receiveException(exception);
                    }

                    @Override
                    public void handleResult(final Long vlanResult) {
                        LOG.debug(String.format("Vlan allocation returned %d", vlanResult));
                        if (vlanResult == null) {
                            throw new NetworkCreationException(String.format("Abandoning network setup for group %s as vlan was not created", securityGroupId));
                        }

                        subnetAllocator.allocateSubnetInLocalRegion(securityGroupId, new GenericContinuation<SubnetAllocationResult>() {
                            @Override
                            public void handleException(Exception exception) {
                                LOG.debug(String.format(DELEGATING_HANDLING_OF_EXCEPTION_S_S_TO_CALLING_CONTINUATION, exception.getClass().getName(), exception.getMessage()));
                                consumedDhtResourceRegistry.deregisterConsumer(securityGroupRecordId, instance.getInstanceId());
                                resContinuation.receiveException(exception);
                            }

                            @Override
                            public void handleResult(SubnetAllocationResult subnetResult) {
                                LOG.debug(String.format("Subnet allocation returned %s", subnetResult));
                                if (subnetResult == null) {
                                    throw new NetworkCreationException(String.format("Abandoning network setup for group %s as subnet was not created", securityGroupId));
                                }

                                allocatePrivateAddress(instance, vlanResult, subnetResult, resContinuation);
                            }
                        });
                    }
                });
            }
        });
    }

    protected void allocatePrivateAddress(final Instance instance, final long allocatedVlanId, final SubnetAllocationResult subnetAllocationResult, final Continuation<Instance, Exception> resContinuation) {
        LOG.debug(String.format("allocatePrivateAddress(%s, %d, %s)", instance, allocatedVlanId, resContinuation));
        final Instance instanceInfoHolder = new Instance();
        final PId securityGroupRecordId = piIdBuilder.getPId(SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName())).forLocalRegion();

        consumedDhtResourceRegistry.update(securityGroupRecordId, new UpdateResolvingPiContinuation<SecurityGroup>() {
            @Override
            public SecurityGroup update(SecurityGroup existing, SecurityGroup mine) {
                LOG.info(String.format("Setting up security group %s with vlan %d for instance %s, as resource %s", existing.getUrl(), allocatedVlanId, instance.getInstanceId(), securityGroupRecordId.toStringFull()));
                existing.setVlanId(allocatedVlanId);
                existing.setNetworkAddress(IpAddressUtils.longToIp(subnetAllocationResult.getSubnetBaseAddress()));
                existing.setNetmask(IpAddressUtils.longToIp(subnetAllocationResult.getSubnetMask()));
                existing.setDnsAddress(subnetAllocationResult.getDnsAddress());

                String privateMacAddress = VlanAddressUtils.getMacAddressFromInstanceId(instance.getInstanceId());
                instanceInfoHolder.setPrivateMacAddress(privateMacAddress);

                String privateIpAddress = privateIpAddressManager.allocateAndSetPrivateIpAddress(instance.getInstanceId(), instanceInfoHolder.getPrivateMacAddress(), existing);
                instanceInfoHolder.setPrivateIpAddress(privateIpAddress);

                LOG.debug(String.format("Provisionally allocated mac %s and priv ip %s to instance %s", privateMacAddress, privateIpAddress, instance.getInstanceId()));
                return existing;
            }

            @Override
            public void handleException(Exception exception) {
                LOG.debug(String.format(DELEGATING_HANDLING_OF_EXCEPTION_S_S_TO_CALLING_CONTINUATION, exception.getClass().getName(), exception.getMessage()));
                consumedDhtResourceRegistry.deregisterConsumer(securityGroupRecordId, instance.getInstanceId());
                resContinuation.receiveException(exception);
            }

            @Override
            public void handleResult(SecurityGroup result) {
                LOG.debug(String.format("Update of sec group %s for address allocation for instance %s wrote result %s", securityGroupRecordId, instance.getInstanceId(), result));
                if (result == null) {
                    LOG.info(String.format("Abandoning network setup for group %s as sec group was not updated", SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName())));
                    throw new NetworkCreationException(String.format("Unable to create network for instance: %s. As security group could not be written.", instance));
                }

                virtualNetworkBuilder.setUpVirtualNetworkForSecurityGroup((SecurityGroup) result);
                privateIpAddressManager.refreshDhcpDaemon();

                instance.setVlanId((int) allocatedVlanId);
                instance.setPrivateMacAddress(instanceInfoHolder.getPrivateMacAddress());
                instance.setPrivateIpAddress(instanceInfoHolder.getPrivateIpAddress());
                resContinuation.receiveResult(instance);
            }
        });
    }

    /* (non-Javadoc)
     * @see com.bt.pi.app.common.net.InstanceNetworkManager#releaseNetworkForInstance(java.lang.String, java.lang.String, java.lang.String)
     */
    public void releaseNetworkForInstance(String userId, String networkName, final String instanceId) {
        LOG.debug(String.format("releaseNetworkForInstance(%s, %s, %s)", userId, networkName, instanceId));

        final String securityGroupUrl = SecurityGroup.getUrl(userId, networkName);
        final PId securityGroupRecordId = piIdBuilder.getPId(securityGroupUrl).forLocalRegion();

        consumedDhtResourceRegistry.update(securityGroupRecordId, new UpdateResolvingPiContinuation<SecurityGroup>() {
            @Override
            public SecurityGroup update(SecurityGroup existing, SecurityGroup mine) {
                LOG.debug(String.format("Going to free private address for instance %s in sec group %s", instanceId, existing.getSecurityGroupId()));
                privateIpAddressManager.freePrivateIpAddressForInstance(instanceId, existing);
                return existing;
            }

            @Override
            public void handleResult(SecurityGroup result) {
                LOG.debug(String.format("Updated sec group %s to free private address for instance %s - sec group: %s", securityGroupUrl, instanceId, result));
                boolean noOtherConsumersRemain = consumedDhtResourceRegistry.deregisterConsumer(securityGroupRecordId, instanceId);
                if (!noOtherConsumersRemain) {
                    LOG.debug(String.format("Leaving security group %s for instance %s, as it is being used by (an)other consumer(s)", securityGroupUrl, instanceId));
                    return;
                }

                vlanAllocator.releaseVlanInLocalRegion(result.getSecurityGroupId());
                subnetAllocator.releaseSubnetInLocalRegion(result.getSecurityGroupId());
                virtualNetworkBuilder.tearDownVirtualNetworkForSecurityGroup(result);
            }
        });
    }
}