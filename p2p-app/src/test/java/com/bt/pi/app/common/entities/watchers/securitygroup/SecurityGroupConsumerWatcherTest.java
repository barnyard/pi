package com.bt.pi.app.common.entities.watchers.securitygroup;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.leased.LeasedResourceAllocationRecordHeartbeater;
import com.bt.pi.core.application.resource.watched.FiniteLifespanConsumerCheckCallback;
import com.bt.pi.core.application.resource.watched.FiniteLifespanConsumerStatusChecker;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class SecurityGroupConsumerWatcherTest {
    private SecurityGroupConsumerWatcher securityGroupConsumerWatcher;
    private String instanceId;
    private PId id;
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;
    private Instance instance;
    private KoalaIdFactory koalaIdFactory;
    private LeasedResourceAllocationRecordHeartbeater allocatableResourceIndexHeartbeatTimestamper;
    private PId publicIpIndexId;
    private InstanceNetworkManager networkManager;
    private InstanceAddressManager addressManager;
    private FiniteLifespanConsumerStatusChecker checker;
    private boolean isInstanceActive;
    private boolean isInstanceMissing;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        instanceId = "i-4irCUAdr";
        isInstanceActive = true;
        id = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        publicIpIndexId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();
        instance = mock(Instance.class);
        when(instance.getInstanceId()).thenReturn(instanceId);
        when(instance.getUserId()).thenReturn("user-id");
        when(instance.getSecurityGroupName()).thenReturn("default");
        when(instance.getPublicIpAddress()).thenReturn("10.0.0.2");

        dhtClientFactory = mock(DhtClientFactory.class);
        checker = mock(FiniteLifespanConsumerStatusChecker.class);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                FiniteLifespanConsumerCheckCallback<Instance> activeCallback = (FiniteLifespanConsumerCheckCallback<Instance>) invocation.getArguments()[0];
                FiniteLifespanConsumerCheckCallback<Instance> inactiveCallback = (FiniteLifespanConsumerCheckCallback<Instance>) invocation.getArguments()[1];
                if (isInstanceActive)
                    activeCallback.handleCallback(instance);
                else
                    inactiveCallback.handleCallback(isInstanceMissing ? null : instance);
                return null;
            }
        }).when(checker).check(isA(FiniteLifespanConsumerCheckCallback.class), isA(FiniteLifespanConsumerCheckCallback.class));

        allocatableResourceIndexHeartbeatTimestamper = mock(LeasedResourceAllocationRecordHeartbeater.class);

        networkManager = mock(InstanceNetworkManager.class);
        addressManager = mock(InstanceAddressManager.class);

        securityGroupConsumerWatcher = new SecurityGroupConsumerWatcher(id, instanceId, dhtClientFactory, piIdBuilder, allocatableResourceIndexHeartbeatTimestamper, networkManager, addressManager) {
            @Override
            protected FiniteLifespanConsumerStatusChecker createChecker(PId consumerRecordId) {
                assertEquals(id, consumerRecordId);
                return checker;
            }
        };
    }

    @Test
    public void shouldHeartbeatWhenInstancePending() {
        // act
        securityGroupConsumerWatcher.run();

        // assert
        verify(allocatableResourceIndexHeartbeatTimestamper).heartbeat(publicIpIndexId, Arrays.asList(new Long[] { IpAddressUtils.ipToLong("10.0.0.2") }), Arrays.asList(new String[] { instanceId }));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReleaseNetworkWhenInstanceInactive() {
        // setup
        isInstanceActive = false;

        // act
        securityGroupConsumerWatcher.run();

        // assert
        verify(networkManager).releaseNetworkForInstance("user-id", "default", instanceId);
        verify(addressManager).releasePublicIpAddressForInstance(eq(instanceId), eq("user-id:default"), isA(GenericContinuation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDoNothingButHandleMissingInstanceGracefully() {
        // setup
        isInstanceActive = false;
        isInstanceMissing = true;

        // act
        securityGroupConsumerWatcher.run();

        // assert
        verify(allocatableResourceIndexHeartbeatTimestamper, never()).heartbeat(publicIpIndexId, Arrays.asList(new Long[] { IpAddressUtils.ipToLong("10.0.0.2") }), Arrays.asList(new String[] { instanceId }));
        verify(networkManager, never()).releaseNetworkForInstance("user-id", "default", instanceId);
        verify(addressManager, never()).releasePublicIpAddressForInstance(eq(instanceId), eq("user-id:default"), isA(GenericContinuation.class));
    }
}
