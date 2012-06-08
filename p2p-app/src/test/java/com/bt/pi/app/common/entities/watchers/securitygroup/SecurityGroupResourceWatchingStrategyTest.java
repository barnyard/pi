package com.bt.pi.app.common.entities.watchers.securitygroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.resource.DefaultDhtResourceRefreshRunner;
import com.bt.pi.core.application.resource.leased.LeasedResourceAllocationRecordHeartbeater;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.PId;

public class SecurityGroupResourceWatchingStrategyTest {
    private SecurityGroupResourceWatchingStrategy securityGroupResourceWatchingStrategy;
    private PId id;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private LeasedResourceAllocationRecordHeartbeater allocatableResourceIndexHeartbeatTimestamper;
    private InstanceNetworkManager networkManager;
    private InstanceAddressManager instanceAddressManager;
    private DhtClientFactory dhtClientFactory;

    @Before
    public void before() {
        allocatableResourceIndexHeartbeatTimestamper = mock(LeasedResourceAllocationRecordHeartbeater.class);
        networkManager = mock(InstanceNetworkManager.class);
        id = mock(PId.class);
        dhtClientFactory = mock(DhtClientFactory.class);
        consumedDhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);

        securityGroupResourceWatchingStrategy = new SecurityGroupResourceWatchingStrategy();
        securityGroupResourceWatchingStrategy.setLeasedResourceAllocationRecordHeartbeater(allocatableResourceIndexHeartbeatTimestamper);
        securityGroupResourceWatchingStrategy.setCachingConsumedResourceRegistry(consumedDhtResourceRegistry);
        securityGroupResourceWatchingStrategy.setInstanceNetworkManager(networkManager);
        securityGroupResourceWatchingStrategy.setInstanceAddressManager(instanceAddressManager);
        securityGroupResourceWatchingStrategy.setDhtClientFactory(dhtClientFactory);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldGetRefreshRunner() {
        // act
        Runnable res = securityGroupResourceWatchingStrategy.getSharedResourceRefreshRunner(id);

        // assert
        assertTrue(res instanceof DefaultDhtResourceRefreshRunner);
        DefaultDhtResourceRefreshRunner<SecurityGroup> runner = (DefaultDhtResourceRefreshRunner<SecurityGroup>) res;
        assertTrue(runner.getRefresHandlingContinuation() instanceof SecurityGroupRefreshContinuation);
        assertEquals(id, runner.getResourceId());
        assertEquals(consumedDhtResourceRegistry, runner.getSharedResourceManager());
    }

    @Test
    public void shouldGetConsumerWatcher() {
        // act
        Runnable res = securityGroupResourceWatchingStrategy.getConsumerWatcher(id, "instance-id");

        // assert
        assertTrue(res instanceof SecurityGroupConsumerWatcher);
        SecurityGroupConsumerWatcher runner = (SecurityGroupConsumerWatcher) res;
        assertEquals(id, runner.getSecurityGroupId());
        assertEquals("instance-id", runner.getInstanceId());
    }
}
