package com.bt.pi.app.common.entities.watchers.securitygroup;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.resource.AbstractResourceWatchingStrategy;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.resource.DefaultDhtResourceRefreshRunner;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.PId;

@Component
@Scope("prototype")
public class SecurityGroupResourceWatchingStrategy extends AbstractResourceWatchingStrategy<PId, SecurityGroup> {
    private static final Log LOG = LogFactory.getLog(SecurityGroupResourceWatchingStrategy.class);
    private PiIdBuilder piIdBuilder;
    private InstanceNetworkManager instanceNetworkManager;
    private InstanceAddressManager instanceAddressManager;
    private DhtClientFactory dhtClientFactory;

    public SecurityGroupResourceWatchingStrategy() {
        piIdBuilder = null;
        dhtClientFactory = null;
        instanceNetworkManager = null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setInstanceNetworkManager(InstanceNetworkManager aNetworkManager) {
        this.instanceNetworkManager = aNetworkManager;
    }

    @Resource
    public void setInstanceAddressManager(InstanceAddressManager aInstanceAddressManager) {
        this.instanceAddressManager = aInstanceAddressManager;
    }

    @Override
    public Runnable getSharedResourceRefreshRunner(PId securityGroupId) {
        LOG.debug(String.format("getSharedResourceRefreshRunner(%s)", securityGroupId));
        SecurityGroupRefreshContinuation securityGroupRefreshContinuation = new SecurityGroupRefreshContinuation(piIdBuilder, dhtClientFactory, (ConsumedDhtResourceRegistry) getCachingConsumedResourceRegistry());
        return new DefaultDhtResourceRefreshRunner<SecurityGroup>(securityGroupId, getCachingConsumedResourceRegistry(), getLeasedResourceAllocationRecordHeartbeater(), securityGroupRefreshContinuation);
    }

    @Override
    public Runnable getConsumerWatcher(PId securityGroupId, String instanceId) {
        LOG.debug(String.format("getConsumerWatcher(%s, %s)", securityGroupId, instanceId));
        return new SecurityGroupConsumerWatcher(securityGroupId, instanceId, dhtClientFactory, piIdBuilder, getLeasedResourceAllocationRecordHeartbeater(), instanceNetworkManager, instanceAddressManager);
    }
}
