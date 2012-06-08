package com.bt.pi.app.networkmanager.handlers;

import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.PId;

@Component
public class InstanceNetworkRefreshHandler {
    private static final Log LOG = LogFactory.getLog(InstanceNetworkRefreshHandler.class);
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private PiIdBuilder piIdBuilder;
    private InstanceNetworkSetupHandler instanceNetworkSetupHandler;
    private DhtClientFactory dhtClientFactory;
    private DhtCache dhtCache;

    public InstanceNetworkRefreshHandler() {
        consumedDhtResourceRegistry = null;
        piIdBuilder = null;
        dhtCache = null;
        dhtClientFactory = null;
        instanceNetworkSetupHandler = null;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setInstanceNetworkSetupHandler(InstanceNetworkSetupHandler aInstanceNetworkSetupHandler) {
        this.instanceNetworkSetupHandler = aInstanceNetworkSetupHandler;
    }

    @Resource
    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource(name = "generalCache")
    public void setDhtCache(DhtCache aDhtCache) {
        this.dhtCache = aDhtCache;
    }

    public void handle(final Instance instance, final ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("handle(%s)", instance));
        if (instance.isDead()) {
            LOG.debug(String.format("Instance %s is not deemed active!", instance));
            return;
        }

        PId securityGroupRecordId = piIdBuilder.getPId(SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName())).forLocalRegion();
        SecurityGroup securityGroup = consumedDhtResourceRegistry.getCachedEntity(securityGroupRecordId);
        if (securityGroup != null && securityGroup.containsInstance(instance.getInstanceId()) && instance.getPrivateIpAddress() != null && !securityGroup.getPrivateAddresses().contains(instance.getPrivateIpAddress())) {
            LOG.debug(String.format("Clearing out private ip address for instance %s in security group %s as it is outside the subnet range for the security group", instance.getInstanceId(), instance.getSecurityGroupName()));
            consumedDhtResourceRegistry.update(securityGroupRecordId, new UpdateResolvingPiContinuation<SecurityGroup>() {
                @Override
                public SecurityGroup update(SecurityGroup existingEntity, SecurityGroup requestedEntity) {
                    existingEntity.getInstances().get(instance.getInstanceId()).setPrivateIpAddress(null);
                    return existingEntity;
                }

                @Override
                public void handleResult(SecurityGroup result) {
                    LOG.debug(String.format("Cleared out private ip address for instance %s in security group %s", instance.getInstanceId(), instance.getSecurityGroupName()));
                    setupNetworkForInstance(instance, receivedMessageContext);
                }
            });
        } else {
            Set<String> consumers = consumedDhtResourceRegistry.getAllConsumers(securityGroupRecordId);
            if (consumers.contains(instance.getInstanceId())) {
                LOG.debug(String.format("Instance %s is already registered with the resource manager for its sec group", instance.getInstanceId()));
                checkInstancePublicIpAgainstSecurityGroup(instance, securityGroupRecordId);
                return;
            }
            setupNetworkForInstance(instance, receivedMessageContext);
        }
    }

    private void setupNetworkForInstance(final Instance instance, final ReceivedMessageContext receivedMessageContext) {
        LOG.info(String.format("Network manager app got active instance %s for user %s and sec group %s - acquiring this instance", instance.getInstanceId(), instance.getUserId(), instance.getSecurityGroupName()));
        // check DHT
        DhtReader reader = dhtClientFactory.createReader();
        reader.getAsync(piIdBuilder.getPIdForEc2AvailabilityZone(instance), new PiContinuation<Instance>() {
            @Override
            public void handleResult(Instance result) {
                if (result.getState() != InstanceState.SHUTTING_DOWN && result.getState() != InstanceState.TERMINATED) {
                    instanceNetworkSetupHandler.handle(instance, false, receivedMessageContext);
                }
            }
        });
    }

    private void checkInstancePublicIpAgainstSecurityGroup(final Instance instance, PId securityGroupDhtId) {
        LOG.debug(String.format("checkInstancePublicIpAgainstSecurityGroup(%s, %s)", instance, securityGroupDhtId));
        SecurityGroup securityGroup = consumedDhtResourceRegistry.getCachedEntity(securityGroupDhtId);
        if (securityGroup == null) {
            LOG.debug(String.format("Sec group %s not found in resoure manager", securityGroupDhtId));
            return;
        }
        InstanceAddress instanceAddress = securityGroup.getInstances().get(instance.getInstanceId());
        if (instanceAddress != null && instanceAddress.getPublicIpAddress() != null) {
            String publicIpInSecurityGroup = instanceAddress.getPublicIpAddress();
            if (!publicIpInSecurityGroup.equals(instance.getPublicIpAddress())) {
                LOG.debug(String.format("Instance rec for %s says public ip %s, sec group says rec %s - let's ask public ip index rec who is right", instance.getInstanceId(), instance.getPublicIpAddress(), publicIpInSecurityGroup));
                handleInstancePublicIpAddressDiscrepancyAgainstSecurityGroup(instance, publicIpInSecurityGroup);
            }
        } else {
            LOG.debug(String.format("No assignment of %s to %s found in sec group record for %s", instance.getPublicIpAddress(), instance.getInstanceId(), securityGroup.getSecurityGroupId()));
        }
    }

    private void handleInstancePublicIpAddressDiscrepancyAgainstSecurityGroup(final Instance instance, final String publicIpInSecurityGroup) {
        LOG.debug(String.format("handleInstancePublicIpAddressDiscrepancyAgainstSecurityGroup(%s, %s)", instance.getInstanceId(), publicIpInSecurityGroup));
        PId publicIpIndexRecordId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();
        dhtCache.get(publicIpIndexRecordId, new PiContinuation<PublicIpAllocationIndex>() {
            @Override
            public void handleResult(PublicIpAllocationIndex result) {
                if (result == null) {
                    LOG.warn("Got null public ip index record");
                    return;
                }

                Entry<Long, InstanceRecord> existingAllocation = result.getExistingAddressAllocationForInstance(instance.getInstanceId());
                if (existingAllocation == null) {
                    LOG.debug(String.format("No public ip allocation for %s found in public ip index record", instance.getInstanceId()));
                    return;
                }

                String existiingIpAddressInIndexRecord = IpAddressUtils.longToIp(existingAllocation.getKey());
                if (existiingIpAddressInIndexRecord.equals(publicIpInSecurityGroup)) {
                    LOG.debug(String.format("public ip index says sec group allocation of %s to %s is correct - let's bring the instnace record into line", publicIpInSecurityGroup, instance.getInstanceId()));
                    updateInstancePublicIp(instance.getInstanceId(), publicIpInSecurityGroup);
                } else {
                    LOG.debug(String.format("public ip index disagrees with sec group allocation of %s to %s giving instance record benefit of doubt", publicIpInSecurityGroup, instance.getInstanceId()));
                }
            }
        });
    }

    private void updateInstancePublicIp(final String instanceId, final String publicIpAddress) {
        LOG.debug(String.format("updateInstancePublicIp(%s, %s)", instanceId, publicIpAddress));
        PId instanceRecordId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        DhtWriter dhtWriter = dhtClientFactory.createWriter();
        dhtWriter.update(instanceRecordId, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.setPublicIpAddress(publicIpAddress);
                return existingEntity;
            }

            @Override
            public void handleResult(Instance result) {
                LOG.debug(String.format("Update of public ip for instance %s to %s wrote record %s", instanceId, publicIpAddress, result));
            }
        });
    }
}
