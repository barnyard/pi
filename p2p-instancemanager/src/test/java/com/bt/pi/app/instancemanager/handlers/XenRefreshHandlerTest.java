package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import rice.Continuation;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.images.InstanceImageManager;
import com.bt.pi.app.instancemanager.libvirt.DomainNotFoundException;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.app.networkmanager.net.VirtualNetworkBuilder;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ XenRefreshHandler.class, LogFactory.class })
@PowerMockIgnore({ "org.apache.log4j.*" })
public class XenRefreshHandlerTest {
    @InjectMocks
    private XenRefreshHandler xenRefreshHandler = new XenRefreshHandler();
    private static final String NODE_ID = "nodeId";
    @Mock
    private InstanceImageManager instanceImageManager;
    @Mock
    private WatcherService watcherService;
    @Mock
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private VirtualNetworkBuilder virtualNetworkBuilder;
    @Mock
    private InstanceManagerApplication instanceManagerApplication;
    @Mock
    private PId id1;
    @Mock
    private PId id2;
    @Mock
    private PId id3;
    @Mock
    private MessageContext messageContext;
    private Instance instance;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    private String INSTANCE_A = "i-123";
    private String INSTANCE_B = "i-456";
    private String INSTANCE_C = "i-678";
    @Mock
    private Log log;

    @Before
    public void setup() {
        instance = new Instance();
        instance.setVlanId(123);
        instance.setInstanceId(INSTANCE_A);
        instance.setNodeId(NODE_ID);

        when(consumedDhtResourceRegistry.getCachedEntity(isA(PId.class))).thenReturn(instance);
        when(instanceManagerApplication.newMessageContext()).thenReturn(messageContext);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
    }

    @Test
    public void shouldRegisterItselfAsAWatcherService() {
        // setup

        // act
        xenRefreshHandler.registerWatcher(NODE_ID);

        // assert
        verify(watcherService).replaceTask("XenRefreshHandler", xenRefreshHandler, 5 * 1000, 60 * 1000);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRegisterAllRunningInstancesWithTheSharedResourceManager() {
        // setup
        Collection<String> instanceList = Arrays.asList(INSTANCE_A, INSTANCE_B, INSTANCE_C);
        when(instanceImageManager.getRunningInstances()).thenReturn(instanceList);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1).thenReturn(id2).thenReturn(id3);

        // act
        xenRefreshHandler.run();

        // assert
        verify(consumedDhtResourceRegistry).registerConsumer(eq(id1), eq(INSTANCE_A), eq(Instance.class), (Continuation) anyObject());
        verify(consumedDhtResourceRegistry).registerConsumer(eq(id2), eq(INSTANCE_B), eq(Instance.class), (Continuation) anyObject());
        verify(consumedDhtResourceRegistry).registerConsumer(eq(id3), eq(INSTANCE_C), eq(Instance.class), (Continuation) anyObject());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSetupVirtualNetworkAndHeartbeatToNetworkManagerApp() {
        // setup
        Collection<String> instanceList = Arrays.asList(INSTANCE_A);
        when(instanceImageManager.getRunningInstances()).thenReturn(instanceList);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1);
        when(piIdBuilder.getPId(SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName()))).thenReturn(id2);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(eq(instance.getInstanceId()))).thenReturn(5432);
        when(id2.forGlobalAvailablityZoneCode(eq(5432))).thenReturn(id2);
        long domainId = 2345;
        when(instanceImageManager.getDomainIdForInstance(INSTANCE_A)).thenReturn(domainId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((GenericContinuation<Boolean>) invocation.getArguments()[3]).handleResult(true);
                return null;
            }
        }).when(consumedDhtResourceRegistry).registerConsumer(eq(id1), eq(instance.getInstanceId()), eq(Instance.class), isA(GenericContinuation.class));

        // act
        xenRefreshHandler.run();

        // assert
        verify(virtualNetworkBuilder).setUpVirtualNetworkForInstance(instance.getVlanId(), instance.getInstanceId());
        verify(messageContext).routePiMessageToApplication(id2, EntityMethod.UPDATE, instance, NetworkManagerApplication.APPLICATION_NAME);
        verify(virtualNetworkBuilder).refreshXenVifOnBridge(instance.getVlanId(), domainId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCarryOnProcessingInstanceIdsAfterUncheckedException() {
        // setup
        Collection<String> instanceList = Arrays.asList(INSTANCE_A, INSTANCE_B, INSTANCE_C);
        when(instanceImageManager.getRunningInstances()).thenReturn(instanceList);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1).thenThrow(new IllegalArgumentException()).thenReturn(id3);

        // act
        xenRefreshHandler.run();

        // assert
        verify(consumedDhtResourceRegistry).registerConsumer(eq(id1), eq(INSTANCE_A), eq(Instance.class), (Continuation) anyObject());
        verify(consumedDhtResourceRegistry).registerConsumer(eq(id3), eq(INSTANCE_C), eq(Instance.class), (Continuation) anyObject());
    }

    @Test
    public void registerConsumerShouldHandleDomainNotFoundException() {
        // setup
        Collection<String> instanceList = Arrays.asList(INSTANCE_A);
        when(instanceImageManager.getRunningInstances()).thenReturn(instanceList);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1);
        when(instanceImageManager.getDomainIdForInstance(INSTANCE_A)).thenThrow(new DomainNotFoundException(INSTANCE_A));
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(eq(instance.getInstanceId()))).thenReturn(5432);
        when(id2.forGlobalAvailablityZoneCode(eq(5432))).thenReturn(id2);
        when(piIdBuilder.getPId(SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName()))).thenReturn(id2);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                GenericContinuation continuation = (GenericContinuation) invocation.getArguments()[3];
                continuation.handleResult(true);
                return null;
            }
        }).when(consumedDhtResourceRegistry).registerConsumer(eq(id1), eq(INSTANCE_A), eq(Instance.class), (Continuation) anyObject());
        PowerMockito.mockStatic(LogFactory.class);
        when(LogFactory.getLog(XenRefreshHandler.class)).thenReturn(log);

        // act
        xenRefreshHandler.run();

        // assert
        verify(log).warn("domain not found: " + INSTANCE_A);
    }

    @Test
    public void registerConsumerShouldHandleMissingInstanceInConsumedDhtResourceRegistry() {
        // setup
        when(consumedDhtResourceRegistry.getCachedEntity(isA(PId.class))).thenReturn(null);
        Collection<String> instanceList = Arrays.asList(INSTANCE_A);
        when(instanceImageManager.getRunningInstances()).thenReturn(instanceList);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1);
        PowerMockito.mockStatic(LogFactory.class);
        when(LogFactory.getLog(XenRefreshHandler.class)).thenReturn(log);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                GenericContinuation continuation = (GenericContinuation) invocation.getArguments()[3];
                continuation.handleResult(true);
                return null;
            }
        }).when(consumedDhtResourceRegistry).registerConsumer(eq(id1), eq(INSTANCE_A), eq(Instance.class), (Continuation) anyObject());
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).when(log).debug(anyObject());
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).when(log).error(anyObject(), isA(Throwable.class));

        // act
        xenRefreshHandler.run();

        // assert
        verify(log).warn("instance " + INSTANCE_A + " not found in ConsumedDhtResourceRegistry");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateNodeIdAndHostnameForAnInstanceWithDifferentNodeId() throws UnknownHostException {
        Collection<String> instanceList = Arrays.asList(INSTANCE_A);
        when(instanceImageManager.getRunningInstances()).thenReturn(instanceList);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1);

        instance.setNodeId("PreviousNodeId");
        when(consumedDhtResourceRegistry.getCachedEntity(id1)).thenReturn(instance);

        GenericContinuationAnswer<Boolean> gcB = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(gcB).when(consumedDhtResourceRegistry).registerConsumer(eq(id1), eq(instance.getInstanceId()), eq(Instance.class), isA(GenericContinuation.class));

        UpdateResolvingContinuationAnswer gcU = new UpdateResolvingContinuationAnswer(instance);
        doAnswer(gcU).when(dhtWriter).update(eq(id1), (PiEntity) isNull(), isA(UpdateResolvingPiContinuation.class));

        xenRefreshHandler.registerWatcher(NODE_ID);

        // act
        xenRefreshHandler.run();

        // assert
        Instance updatedInstance = (Instance) gcU.getResult();
        assertEquals(NODE_ID, updatedInstance.getNodeId());
        assertEquals(InetAddress.getLocalHost().getHostName(), updatedInstance.getHostname());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotUpdateNodeIdAndHostnameForAnInstanceWithSameNodeId() {
        // setup
        BlockingDhtWriter blockingDhtWriter = mock(BlockingDhtWriter.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(blockingDhtWriter);

        Collection<String> instanceList = Arrays.asList(INSTANCE_A);
        when(instanceImageManager.getRunningInstances()).thenReturn(instanceList);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1);

        instance.setNodeId(NODE_ID);
        when(consumedDhtResourceRegistry.getCachedEntity(id1)).thenReturn(instance);

        GenericContinuationAnswer<Boolean> gcB = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(gcB).when(consumedDhtResourceRegistry).registerConsumer(eq(id1), eq(instance.getInstanceId()), eq(Instance.class), isA(GenericContinuation.class));

        xenRefreshHandler.registerWatcher(NODE_ID);

        // act
        xenRefreshHandler.run();

        // assert
        verify(blockingDhtWriter, never()).update(any(PId.class), any(PiEntity.class), any(UpdateResolvingPiContinuation.class));
    }

    @Test
    public void shouldUpdateInstanceStatusOfCrashedDomain() {
        // setup
        Collection<String> instanceList = Arrays.asList(INSTANCE_A);
        instance.setState(InstanceState.RUNNING);
        when(instanceImageManager.getCrashedInstances()).thenReturn(instanceList);

        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1);

        UpdateResolvingContinuationAnswer gcU = new UpdateResolvingContinuationAnswer(instance);
        doAnswer(gcU).when(dhtWriter).update(eq(id1), (PiEntity) isNull(), isA(UpdateResolvingPiContinuation.class));

        // act
        xenRefreshHandler.run();

        // assert
        assertEquals(InstanceState.CRASHED, instance.getState());
    }

    @Test
    public void shouldFailGracefullyIfCrashedInstanceNotFoundInDht() {
        // setup
        Collection<String> instanceList = Arrays.asList(INSTANCE_A);
        instance.setState(InstanceState.RUNNING);
        when(instanceImageManager.getCrashedInstances()).thenReturn(instanceList);

        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1);

        UpdateResolvingContinuationAnswer gcU = new UpdateResolvingContinuationAnswer(null);
        doAnswer(gcU).when(dhtWriter).update(eq(id1), (PiEntity) isNull(), isA(UpdateResolvingPiContinuation.class));

        // act
        xenRefreshHandler.run();

        // assert
        assertEquals(InstanceState.RUNNING, instance.getState());
    }

    @Test
    public void shouldRevertInstanceStateOfPreviouslyCrashedInstance() {
        // setup
        instance.setState(InstanceState.CRASHED);
        Collection<String> instanceList = Arrays.asList(INSTANCE_A);
        when(instanceImageManager.getRunningInstances()).thenReturn(instanceList);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(id1);

        when(consumedDhtResourceRegistry.getCachedEntity(id1)).thenReturn(instance);

        GenericContinuationAnswer<Boolean> gcB = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(gcB).when(consumedDhtResourceRegistry).registerConsumer(eq(id1), eq(instance.getInstanceId()), eq(Instance.class), isA(GenericContinuation.class));

        UpdateResolvingContinuationAnswer gcU = new UpdateResolvingContinuationAnswer(instance);
        doAnswer(gcU).when(dhtWriter).update(eq(id1), (PiEntity) isNull(), isA(UpdateResolvingPiContinuation.class));

        // act
        xenRefreshHandler.run();

        // assert
        assertEquals(InstanceState.RUNNING, instance.getState());
    }
}
