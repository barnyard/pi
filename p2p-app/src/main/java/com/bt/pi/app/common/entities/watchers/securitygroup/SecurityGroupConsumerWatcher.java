package com.bt.pi.app.common.entities.watchers.securitygroup;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.leased.LeasedResourceAllocationRecordHeartbeater;
import com.bt.pi.core.application.resource.watched.FiniteLifespanConsumerCheckCallback;
import com.bt.pi.core.application.resource.watched.FiniteLifespanConsumerStatusChecker;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.PId;

public class SecurityGroupConsumerWatcher implements Runnable {
    private static final Log LOG = LogFactory.getLog(SecurityGroupConsumerWatcher.class);
    private PiIdBuilder piIdBuilder;
    private PId securityGroupRecordId;
    private String instanceId;
    private LeasedResourceAllocationRecordHeartbeater leasedResourceAllocationRecordHeartbeater;
    private InstanceNetworkManager instanceNetworkManager;
    private InstanceAddressManager instanceAddressManager;
    private DhtClientFactory dhtClientFactory;

    public SecurityGroupConsumerWatcher(PId aSecurityGroupRecordId, String anInstanceId, DhtClientFactory aDhtClientFactory, PiIdBuilder aPiIdBuilder, LeasedResourceAllocationRecordHeartbeater aLeasedResourceAllocationRecordHeartbeater,
            InstanceNetworkManager aNetworkManager, InstanceAddressManager aInstanceAddressManager) {
        securityGroupRecordId = aSecurityGroupRecordId;
        instanceId = anInstanceId;

        dhtClientFactory = aDhtClientFactory;
        piIdBuilder = aPiIdBuilder;
        leasedResourceAllocationRecordHeartbeater = aLeasedResourceAllocationRecordHeartbeater;
        instanceNetworkManager = aNetworkManager;
        instanceAddressManager = aInstanceAddressManager;
    }

    protected PId getSecurityGroupId() {
        return securityGroupRecordId;
    }

    protected String getInstanceId() {
        return instanceId;
    }

    @Override
    public void run() {
        LOG.info(String.format("Running sec group consumer watcher for sec group %s and instance %s", securityGroupRecordId, instanceId));
        FiniteLifespanConsumerCheckCallback<Instance> instanceActiveCallback = new FiniteLifespanConsumerCheckCallback<Instance>() {
            public void handleCallback(Instance instance) {
                heartbeatAddressForInstance(instance);
            }
        };
        FiniteLifespanConsumerCheckCallback<Instance> instanceInactiveCallback = new FiniteLifespanConsumerCheckCallback<Instance>() {
            public void handleCallback(Instance instance) {
                if (instance == null)
                    LOG.warn(String.format("Instance %s not found in dht, doing nothing", instanceId));
                else
                    releaseResourcesForStaleInstance(instance);
            }
        };

        PId instanceRecordId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        FiniteLifespanConsumerStatusChecker checker = createChecker(instanceRecordId);
        checker.check(instanceActiveCallback, instanceInactiveCallback);
    }

    protected FiniteLifespanConsumerStatusChecker createChecker(PId consumerRecordId) {
        return new FiniteLifespanConsumerStatusChecker(instanceId, consumerRecordId, dhtClientFactory);
    }

    private void releaseResourcesForStaleInstance(final Instance instance) {
        LOG.debug(String.format("releaseResourcesForStaleInstance(%s)", instance));
        String userId = instance.getUserId();
        String networkName = instance.getSecurityGroupName();
        instanceNetworkManager.releaseNetworkForInstance(userId, networkName, instance.getInstanceId());
        instanceAddressManager.releasePublicIpAddressForInstance(instance.getInstanceId(), String.format(SecurityGroup.SEC_GROUP_ID_FORMAT_STRING, instance.getUserId(), instance.getSecurityGroupName()), new GenericContinuation<Boolean>() {
            @Override
            public void handleResult(Boolean result) {
                LOG.debug(String.format("Release of public addr for stale instance %s returned %s", instance.getInstanceId(), result));
            }
        });
    }

    private void heartbeatAddressForInstance(Instance instance) {
        LOG.debug(String.format("heartbeatAddressForInstance(%s)", instance));
        String publicIpAddress = instance.getPublicIpAddress();
        if (publicIpAddress == null) {
            LOG.warn(String.format("Null public IP addr for instance %s", instance));
            return;
        }

        PId indexRecordId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();
        List<Long> resources = new ArrayList<Long>();
        resources.add(IpAddressUtils.ipToLong(publicIpAddress));

        List<String> consumerIds = new ArrayList<String>();
        consumerIds.add(instance.getInstanceId());

        leasedResourceAllocationRecordHeartbeater.heartbeat(indexRecordId, resources, consumerIds);
    }
}
