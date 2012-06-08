package com.bt.pi.app.common.entities.watchers.securitygroup;

import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.resource.watched.FiniteLifespanConsumerCheckCallback;
import com.bt.pi.core.application.resource.watched.FiniteLifespanConsumerStatusChecker;
import com.bt.pi.core.continuation.LoggingContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

/**
 * This class does a bunch of housekeepy stuff around watching a security group in the resource manager. Currently it
 * will: 1. reconcile any public ip discrepancies between public ip index record and sec group 2. remove any address
 * allocations for stale instances in the sec group
 * 
 */
public class SecurityGroupRefreshContinuation extends PiContinuation<SecurityGroup> {
    private static final String RECEIVED_RESULT_S = "Received result: %s";
    private static final Log LOG = LogFactory.getLog(SecurityGroupRefreshContinuation.class);
    private PiIdBuilder piIdBuilder;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private DhtClientFactory dhtClientFactory;

    public SecurityGroupRefreshContinuation(PiIdBuilder aPiIdBuilder, DhtClientFactory aDhtClientFactory, ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        piIdBuilder = aPiIdBuilder;
        consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
        dhtClientFactory = aDhtClientFactory;
    }

    @Override
    public void handleResult(SecurityGroup result) {
        LOG.debug(String.format(RECEIVED_RESULT_S, result));
        checkForStaleInstances(result);
        checkPublicIpAddressAllocationsForSecurityGroupInstances(result);
    }

    /**
     * Ensure there are no dead instances with allocated addresses left in the security group
     * 
     * @param securityGroup
     */
    private void checkForStaleInstances(SecurityGroup securityGroup) {
        LOG.debug(String.format("checkForStaleInstances(%s)", securityGroup.getSecurityGroupId()));
        PId securityGroupResourceId = piIdBuilder.getPId(securityGroup).forLocalRegion();
        Set<String> consumerSet = consumedDhtResourceRegistry.getAllConsumers(securityGroupResourceId);
        LOG.debug(String.format("Current consumer set for sec group %s: %s", securityGroup.getSecurityGroupId(), consumerSet));
        for (Entry<String, InstanceAddress> e : securityGroup.getInstances().entrySet()) {
            String instanceId = e.getKey();
            if (consumerSet.contains(instanceId)) {
                LOG.debug(String.format("Instance %s is currently registered as a consumer of sec group %s", instanceId, securityGroup.getSecurityGroupId()));
                continue;
            } else {
                LOG.info(String.format("Instance %s is NOT registered as a consumer of sec group %s. Lets see if its still running", instanceId, securityGroup.getSecurityGroupId()));
                removeInstanceAddressAllocationFromSecurityGroupIfStale(securityGroup, instanceId);
            }
        }
    }

    private void removeInstanceAddressAllocationFromSecurityGroupIfStale(final SecurityGroup securityGroup, final String instanceId) {
        LOG.debug(String.format("removeInstanceAddressAllocationFromSecurityGroupIfStale(%s)", securityGroup.getSecurityGroupId()));
        PId consumerRecordId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        FiniteLifespanConsumerStatusChecker instanceStatusChecker = createInstanceStatusChecker(instanceId, consumerRecordId);
        instanceStatusChecker.check(null, new FiniteLifespanConsumerCheckCallback<Instance>() {
            @Override
            public void handleCallback(Instance instance) {
                LOG.info(String.format("Instance %s found not to be running - removing its public ip from sec group %s", instanceId, securityGroup.getSecurityGroupId()));
                removeInstanceAddressFromSecurityGroup(securityGroup, instanceId);
            }
        });
    }

    protected FiniteLifespanConsumerStatusChecker createInstanceStatusChecker(String instanceId, PId consumerRecordId) {
        return new FiniteLifespanConsumerStatusChecker(instanceId, consumerRecordId, dhtClientFactory);
    }

    private void removeInstanceAddressFromSecurityGroup(SecurityGroup securityGroup, final String instanceId) {
        LOG.debug(String.format("removeInstanceAddressFromSecurityGroup(%s)", securityGroup.getSecurityGroupId()));
        final PId securityGroupRecordId = piIdBuilder.getPId(securityGroup).forLocalRegion();
        consumedDhtResourceRegistry.update(securityGroupRecordId, new UpdateResolvingPiContinuation<SecurityGroup>() {
            @Override
            public SecurityGroup update(SecurityGroup existingEntity, SecurityGroup requestedEntity) {
                InstanceAddress removedAddressRecord = existingEntity.getInstances().remove(instanceId);
                if (removedAddressRecord != null) {
                    LOG.info(String.format("Provisionally released stale address record %s for instance %s", removedAddressRecord, instanceId));
                    return existingEntity;
                } else {
                    return null;
                }
            }

            @Override
            public void handleResult(SecurityGroup result) {
                LOG.debug(String.format(RECEIVED_RESULT_S, result));
                if (result != null) {
                    LOG.debug(String.format("Refreshing sec group %s", securityGroupRecordId));
                    consumedDhtResourceRegistry.refresh(securityGroupRecordId, new LoggingContinuation<PiEntity>());
                }
            }
        });
    }

    private void checkPublicIpAddressAllocationsForSecurityGroupInstances(final SecurityGroup securityGroup) {
        LOG.debug(String.format("checkPublicIpAddressAllocationsForSecurityGroupInstances(%s)", securityGroup));
        PId publicIpAddressIndexRecordId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();
        DhtReader dhtReader = dhtClientFactory.createReader();
        dhtReader.getAsync(publicIpAddressIndexRecordId, new PiContinuation<PublicIpAllocationIndex>() {
            @Override
            public void handleResult(PublicIpAllocationIndex publicIpAllocationIndex) {
                if (publicIpAllocationIndex == null || publicIpAllocationIndex.getCurrentAllocations() == null) {
                    LOG.warn(String.format("Got null public ip address record"));
                    return;
                }

                for (Entry<String, InstanceAddress> addressAssignmentEntry : securityGroup.getInstances().entrySet()) {
                    String currentInstanceId = addressAssignmentEntry.getKey();
                    String currentPublicIpAddress = addressAssignmentEntry.getValue().getPublicIpAddress();

                    Entry<Long, InstanceRecord> addressAssignmentInIndexRecord = publicIpAllocationIndex.getExistingAddressAllocationForInstance(currentInstanceId);
                    if (addressAssignmentInIndexRecord == null) {
                        LOG.debug(String.format("Assignment of pub addr %s to instance %s from sec group %s not found in index record", currentPublicIpAddress, currentInstanceId, securityGroup.getSecurityGroupId()));
                        continue;
                    }

                    String addressInIndexRecord = IpAddressUtils.longToIp(addressAssignmentInIndexRecord.getKey());
                    if (addressInIndexRecord != null && !addressInIndexRecord.equals(currentPublicIpAddress)) {
                        LOG.info(String.format("Found discrepancy in addressing for instance %s: between sec group address %s and public ip index address %s - going to correct the sec group address", currentInstanceId, currentPublicIpAddress,
                                addressInIndexRecord));
                        setPublicIpAddressForInstanceInSecurityGroup(addressInIndexRecord, currentInstanceId, securityGroup);
                    }
                }
            }
        });
    }

    private void setPublicIpAddressForInstanceInSecurityGroup(final String publicIpAddress, final String instanceId, final SecurityGroup securityGroup) {
        LOG.debug(String.format("setPublicIpAddressForInstanceInSecurityGroup(%s, %s, %s", publicIpAddress, instanceId, securityGroup));
        PId securityGroupRecordId = piIdBuilder.getPId(securityGroup).forLocalRegion();
        consumedDhtResourceRegistry.update(securityGroupRecordId, new UpdateResolvingPiContinuation<SecurityGroup>() {
            @Override
            public SecurityGroup update(SecurityGroup existingEntity, SecurityGroup requestedEntity) {
                InstanceAddress addressesForInstance = existingEntity.getInstances().get(instanceId);
                if (addressesForInstance != null) {
                    LOG.debug(String.format("Provisionally set public ip for instance %s in sec group record %s to %s", instanceId, securityGroup.getSecurityGroupId(), publicIpAddress));
                    addressesForInstance.setPublicIpAddress(publicIpAddress);
                    return existingEntity;
                } else {
                    LOG.warn(String.format("No record of instance %s found in security group %s", instanceId, securityGroup.getSecurityGroupId()));
                    return null;
                }
            }

            @Override
            public void handleResult(SecurityGroup result) {
                LOG.debug(String.format("Update of public ip to %s for instance %s wrote sec group %s", publicIpAddress, instanceId, result));
            }
        });
    }
}
