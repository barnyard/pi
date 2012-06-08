package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import rice.Continuation;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeMultiClient;
import rice.p2p.scribe.Topic;
import rice.pastry.PastryNode;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.images.InstanceImageManager;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.scribe.KoalaScribeImpl;
import com.bt.pi.core.util.MDCHelper;

@RunWith(MockitoJUnitRunner.class)
public class InstanceManagerApplicationTest {
    private String instanceType = "try.this.for.size";
    private String instanceId = "instanceId";
    private String instanceId2 = "instanceId2";
    private CountDownLatch latch;
    private Instance instance;
    private Instance instance2;
    private Reservation reservation;
    private InstanceTypeConfiguration instanceTypeConfiguration;
    @Mock
    private Topic topic;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private PId instancePastryId;
    @Mock
    private PId instancePastryId2;
    @Mock
    private PId instanceTypesId;
    @Mock
    private PId networkManagerId;
    private NodeHandle sourceNodeHandle;
    private rice.pastry.Id nodeId;
    @Mock
    private ThreadPoolTaskExecutor executor;
    @Mock
    private KoalaJsonParser koalaJsonParser;
    @Mock
    private KoalaScribeImpl koalaScribeImpl;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private SystemResourceState systemResourceState;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private InstanceImageManager instanceImageManager;
    @InjectMocks
    private InstanceManagerApplication instanceManagerApplication = new InstanceManagerApplication() {
        @Override
        public String getNodeIdFull() {
            return nodeId.toStringFull();
        }

        @Override
        public java.util.Collection<rice.pastry.NodeHandle> getLeafNodeHandles() {
            return leafNodeHandles;
        }
    };
    private int instanceTypeMemoryInMB;
    private String transactionUID = "abc123";
    @Mock
    private AnycastHandler anycastHandler;
    private InstanceTypes instanceTypes;
    @Mock
    private DeliverHandler deliverHandler;
    @Mock
    private AlwaysOnApplicationActivator alwaysOnApplicationActivator;
    @Mock
    private XenRefreshHandler xenRefreshHandler;
    @Mock
    private rice.pastry.NodeHandle leafNodeHandle1;
    @Mock
    private rice.pastry.NodeHandle leafNodeHandle2;
    protected Collection<rice.pastry.NodeHandle> leafNodeHandles = Arrays.asList(new rice.pastry.NodeHandle[] { leafNodeHandle1, leafNodeHandle2 });
    @Mock
    private KoalaIdUtils koalaIdUtils;
    @Mock
    private InstanceManagerApplicationSubscriptionHelper instanceManagerApplicationSubscriptionHelper;
    @Mock
    private DhtCache instanceTypesCache;
    @Mock
    private InstanceManagerApplicationQueueWatcherInitiator instanceManagerApplicationQueueWatcherInitiator;

    @Before
    public void setup() {
        MDCHelper.putTransactionUID(transactionUID);
        setupInstanceAndInstanceType();
        setupMocks();

        instanceManagerApplication.setInstanceManagerActivationCheckPeriodSecs(321);
        instanceManagerApplication.setInstanceManagerStartTimeoutMillis(123);
        instanceManagerApplication.setInstanceDirectory("instances.directory");
        instanceManagerApplication.setImagePath("image.path");
        instanceManagerApplication.start(setupPastryNode(instanceManagerApplication), null, null, null);
        instanceManagerApplication.onApplicationEvent(new NodeStartedEvent(new Object()));

        setupReservation();
        setupLatch();
    }

    public void setupReservation() {
        reservation = new Reservation();
        reservation.setInstanceIds(new TreeSet<String>(Arrays.asList(new String[] { instanceId })));
        reservation.setInstanceType(instanceType);
    }

    public void setupLatch() {
        latch = new CountDownLatch(1);
    }

    @SuppressWarnings("unchecked")
    private void setupMocks() {
        nodeId = rice.pastry.Id.build("testNode");

        PId topicId = mock(PId.class);
        String topicIdStr = "123456789012345678901234567890123456000";
        when(topicId.getIdAsHex()).thenReturn(topicIdStr);
        when(topic.getId()).thenReturn(rice.pastry.Id.build(topicIdStr));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(executor).execute(isA(Runnable.class));

        when(topic.toString()).thenReturn("test topic");

        when(koalaScribeImpl.subscribeToTopic(isA(String.class), isA(ScribeMultiClient.class))).thenReturn(topic);

        when(koalaIdFactory.buildPId(isA(PiLocation.class))).thenReturn(topicId);

        setupPiBuilderExpectationsForInstance(instance, instanceId, instancePastryId);
        setupPiBuilderExpectationsForInstance(instance2, instanceId2, instancePastryId2);

        when(instanceImageManager.doesDirectoryExist(isA(String.class))).thenReturn(true);

        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Instance instanceToUse = null;
                if (instancePastryId.equals(invocation.getArguments()[0]))
                    instanceToUse = instance;
                else if (instancePastryId2.equals(invocation.getArguments()[0]))
                    instanceToUse = instance2;

                ((Continuation<PiEntity, Exception>) invocation.getArguments()[1]).receiveResult(instanceToUse);
                latch.countDown();
                return null;
            }
        }).when(dhtReader).getAsync(isA(PId.class), isA(Continuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Instance instanceToUse = null;
                if (instancePastryId.equals(invocation.getArguments()[0]))
                    instanceToUse = instance;
                else if (instancePastryId2.equals(invocation.getArguments()[0]))
                    instanceToUse = instance2;

                Instance updatedInstance = (Instance) ((UpdateResolvingContinuation<PiEntity, Exception>) invocation.getArguments()[1]).update(instanceToUse, null);
                ((UpdateResolvingContinuation<PiEntity, Exception>) invocation.getArguments()[1]).receiveResult(updatedInstance);
                latch.countDown();
                return null;
            }
        }).when(dhtWriter).update(isA(PId.class), (UpdateResolvingContinuation<PiEntity, Exception>) isA(Continuation.class));

        when(pubSubMessageContext.getTopicPId()).thenReturn(topicId);
        when(pubSubMessageContext.getNodeHandle()).thenReturn(sourceNodeHandle);

        instanceTypes = new InstanceTypes();
        instanceTypes.addInstanceType(instanceTypeConfiguration);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[1];
                continuation.handleResult(instanceTypes);
                latch.countDown();
                return null;
            }
        }).when(instanceTypesCache).get(eq(instanceTypesId), isA(PiContinuation.class));

        when(systemResourceState.getFreeCores()).thenReturn(50);
        when(systemResourceState.getFreeDiskInMB()).thenReturn(200 * 1024L);
        when(systemResourceState.getFreeMemoryInMB()).thenReturn(20 * 1024L);
        when(koalaIdUtils.isIdClosestToMe(eq(nodeId.toStringFull()), eq(leafNodeHandles), isA(Id.class), eq(NodeScope.AVAILABILITY_ZONE))).thenReturn(false).thenReturn(true);
    }

    private void setupPiBuilderExpectationsForInstance(Instance instance, String instanceId, PId instancePastryId) {
        when(piIdBuilder.getPIdForEc2AvailabilityZone(instance)).thenReturn(instancePastryId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId))).thenReturn(instancePastryId);
        when(piIdBuilder.getPId(SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName()))).thenReturn(networkManagerId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instance.getInstanceId())).thenReturn(123);
        when(networkManagerId.forGlobalAvailablityZoneCode(123)).thenReturn(networkManagerId);
    }

    private void setupInstanceAndInstanceType() {
        instance = new Instance();
        instance2 = new Instance();

        final AtomicBoolean firstHeartbeatDone = new AtomicBoolean(false);

        setupInstance(instance, instanceId);
        setupInstance(instance2, instanceId2);

        instanceTypeMemoryInMB = 1024;
        instanceTypeConfiguration = new InstanceTypeConfiguration(instanceType, 1, instanceTypeMemoryInMB, 5);
    }

    private void setupInstance(Instance instance, String instanceId) {
        instance.setInstanceId(instanceId);
        instance.setInstanceType(instanceType);
        instance.setUserId("userId");
    }

    private PastryNode setupPastryNode(InstanceManagerApplication application) {
        Endpoint endpoint = mock(Endpoint.class);

        PastryNode pastryNode = mock(PastryNode.class);
        when(pastryNode.buildEndpoint(application, application.getApplicationName())).thenReturn(endpoint);
        when(pastryNode.getId()).thenReturn(nodeId);
        when(pastryNode.getNodeId()).thenReturn(nodeId);

        return pastryNode;
    }

    @Test
    public void shouldGetActivationTimeFromConfig() {
        // act
        int res = instanceManagerApplication.getActivationCheckPeriodSecs();

        // assert
        assertEquals(321, res);
    }

    @Test
    public void shouldGetStartTimeFromConfigIfDefined() {
        // act
        long res = instanceManagerApplication.getStartTimeout();

        // assert
        assertEquals(123, res);
    }

    @Test
    public void shouldGetStartTimeUnitAsMillis() {
        // act
        TimeUnit res = instanceManagerApplication.getStartTimeoutUnit();

        // assert
        assertEquals(TimeUnit.MILLISECONDS, res);
    }

    @Test
    public void shouldNotHandleAnyCastIfTopicIsNotRunInstance() {
        // act
        boolean anycastResponse = instanceManagerApplication.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, null);

        // assert
        assertFalse(anycastResponse);
    }

    @Test
    public void shouldNotBuildNetworkManagerApplicationKeyIfDataIsNotAnInstanceOfInstanceEntity() throws Exception {
        // setup
        PiEntity data = new User();

        // act
        instanceManagerApplication.deliver(pubSubMessageContext, EntityMethod.CREATE, data);

        // assert
        verify(piIdBuilder, never()).getPId(SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName()));
    }

    @Test
    public void terminateInstanceShouldDelegateToDeliverHandler() {
        // setup

        // act
        instanceManagerApplication.terminateInstance(instance);

        // assert
        verify(deliverHandler).terminateInstance(instance);
    }

    @Test
    public void destroyInstanceShouldDelegateToDeliverHandler() {
        // setup

        // act
        instanceManagerApplication.destroyInstance(instance);

        // assert
        verify(deliverHandler).destroyInstance(instance);
    }

    class MyInstanceManagerApplicationResourceWatcher extends InstanceManagerApplication {
        boolean subscribeInvoked = false;
        boolean unsubscribeInvoked = false;
        CountDownLatch latch = null;

        public MyInstanceManagerApplicationResourceWatcher() {
            this(1);
        }

        public MyInstanceManagerApplicationResourceWatcher(int numberOfLatches) {
            super();
            latch = new CountDownLatch(numberOfLatches);
        }

        @Override
        public String getNodeIdFull() {
            return nodeId.toStringFull();
        }

        @Override
        public java.util.Collection<rice.pastry.NodeHandle> getLeafNodeHandles() {
            return leafNodeHandles;
        }

        @Override
        public void subscribe(PId topicId, ScribeMultiClient listener) {
            subscribeInvoked = true;
        }

        @Override
        public void unsubscribe(PId topicId, ScribeMultiClient listener) {
            unsubscribeInvoked = true;
        }
    }

    @Test
    public void shouldCheckCapacityAndSubscribeWhenRequired() throws Exception {
        // act
        instanceManagerApplication.checkCapacityAndSubscribeUnSubscribe();

        // assert
        verify(instanceManagerApplicationSubscriptionHelper).checkCapacityAndSubscribeUnSubscribe();
    }

    class InstanceManagerApplicationRefreshInstance extends InstanceManagerApplication {
        boolean terminateInvoked = false;
        boolean destroyInvoked = false;
        MessageContext messageContext = mock(MessageContext.class);

        public InstanceManagerApplicationRefreshInstance() {
            super();
            setInstanceImageManager(instanceImageManager);
            setPiIdBuilder(piIdBuilder);
            setDhtClientFactory(dhtClientFactory);
        }

        @Override
        void destroyInstance(Instance anInstance) {
            assertEquals(instance, anInstance);
            destroyInvoked = true;
        }

        @Override
        void terminateInstance(Instance anInstance) {
            assertEquals(instance, anInstance);
            terminateInvoked = true;
        }

        @Override
        public MessageContext newMessageContext() {
            return messageContext;
        }
    }

    @Test
    public void instanceRefreshShouldNotTerminateTheInstanceInPendingState() {
        // setup
        instanceManagerApplication = new InstanceManagerApplicationRefreshInstance();
        instance.setState(InstanceState.PENDING);

        // act
        instanceManagerApplication.handleInstanceRefresh(instance);

        // assert
        assertFalse(((InstanceManagerApplicationRefreshInstance) instanceManagerApplication).terminateInvoked);
    }

    @Test
    public void instanceRefreshShouldDestroyInstanceIfUnableToTerminateIn2Attempts() {
        // setup
        instanceManagerApplication = new InstanceManagerApplicationRefreshInstance();

        instance.setState(InstanceState.TERMINATED);
        long currentTimeMillis = System.currentTimeMillis();
        long lastInstanceRefreshInterval = Instance.DEFAULT_REPEATING_INTERVAL_MILLIS * 3;

        instance.setLastHeartbeatTimestamp(currentTimeMillis - lastInstanceRefreshInterval);

        // act
        instanceManagerApplication.handleInstanceRefresh(instance);

        // assert
        assertFalse(((InstanceManagerApplicationRefreshInstance) instanceManagerApplication).terminateInvoked);
        assertTrue(((InstanceManagerApplicationRefreshInstance) instanceManagerApplication).destroyInvoked);
    }

    @Test
    public void testInstanceRefreshStatusIsTerminated() throws Exception {
        // setup
        instanceManagerApplication = new InstanceManagerApplicationRefreshInstance();
        instance.setState(InstanceState.TERMINATED);

        // act
        instanceManagerApplication.handleInstanceRefresh(instance);

        // assert
        assertTrue(((InstanceManagerApplicationRefreshInstance) instanceManagerApplication).terminateInvoked);
    }

    @Test
    public void shouldNotTerminateInstanceIfItHasntBeenAbleToHeartBeat() throws Exception {
        // setup
        instanceManagerApplication = new InstanceManagerApplicationRefreshInstance();
        instanceManagerApplication.setRetainFaileInstanceArtifacts(true);
        instance.setState(InstanceState.RUNNING);
        instance.setLastHeartbeatTimestamp(1L);
        when(instanceImageManager.isInstanceRunning(instance)).thenReturn(false);

        // act
        instanceManagerApplication.handleInstanceRefresh(instance);

        // assert
        assertFalse(((InstanceManagerApplicationRefreshInstance) instanceManagerApplication).terminateInvoked);
    }

    @Test
    public void testInstanceRefreshStatusIsRunningButDeadAccordingToXen() throws Exception {
        // setup
        instanceManagerApplication = new InstanceManagerApplicationRefreshInstance();
        instance.setState(InstanceState.RUNNING);
        when(instanceImageManager.isInstanceRunning(instance)).thenReturn(false);

        // act
        instanceManagerApplication.handleInstanceRefresh(instance);

        // assert
        assertTrue(((InstanceManagerApplicationRefreshInstance) instanceManagerApplication).terminateInvoked);
    }

    @Test
    public void testInstanceRefreshStatusIsRunningButDeadAccordingToXenButWeKeepTheArtifacts() throws Exception {
        // setup
        instanceManagerApplication = new InstanceManagerApplicationRefreshInstance();
        instance.setState(InstanceState.RUNNING);
        when(instanceImageManager.isInstanceRunning(instance)).thenReturn(false);
        instanceManagerApplication.setRetainFaileInstanceArtifacts(true);

        // act
        instanceManagerApplication.handleInstanceRefresh(instance);

        // assert
        assertFalse(((InstanceManagerApplicationRefreshInstance) instanceManagerApplication).terminateInvoked);
    }

    @Test
    public void testInstanceRefreshStatusIsRunningCausesHeartbeatsToHappen() throws Exception {
        // setup
        instanceManagerApplication = new InstanceManagerApplicationRefreshInstance();
        instance.setState(InstanceState.RUNNING);
        instance.heartbeat();
        when(instanceImageManager.isInstanceRunning(instance)).thenReturn(true);

        // act
        instanceManagerApplication.handleInstanceRefresh(instance);

        // assert
        verify(((InstanceManagerApplicationRefreshInstance) instanceManagerApplication).messageContext).routePiMessageToApplication(networkManagerId, EntityMethod.UPDATE, instance, NetworkManagerApplication.APPLICATION_NAME);
    }

    @Test
    public void shouldSentHeartbeatToNetworkManagerEvenWhenInstanceRecordHeartbeatThrows() throws Exception {
        // setup
        instanceManagerApplication = new InstanceManagerApplicationRefreshInstance();
        instance.setState(InstanceState.RUNNING);
        when(instanceImageManager.isInstanceRunning(instance)).thenReturn(true);

        doAnswer(new Answer<Instance>() {
            @Override
            public Instance answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingContinuation urc = (UpdateResolvingContinuation) invocation.getArguments()[1];
                urc.receiveException(new RuntimeException("oops"));
                return null;
            }
        }).when(dhtWriter).update(isA(PId.class), (UpdateResolvingContinuation<PiEntity, Exception>) isA(Continuation.class));

        // act
        instanceManagerApplication.handleInstanceRefresh(instance);

        // assert
        verify(((InstanceManagerApplicationRefreshInstance) instanceManagerApplication).messageContext).routePiMessageToApplication(networkManagerId, EntityMethod.UPDATE, instance, NetworkManagerApplication.APPLICATION_NAME);
    }

    @Test
    public void testRegisterRunningInstances() throws Exception {
        // setup
        when(instanceImageManager.getRunningInstances()).thenReturn(Arrays.asList(new String[] { instance.getInstanceId(), instance2.getInstanceId() }));

        // act
        instanceManagerApplication.becomeActive();

        // assert
        verify(xenRefreshHandler).run();
    }

    @Test
    public void shouldNotBecomeActiveAsInstanceDirectoryDoesntExistAndCouldNotBeCreated() {
        // setup
        doThrow(new IllegalArgumentException("bang! And the dirt is gone!")).when(instanceImageManager).createDirectoryIfItDoesNotExist("instances.directory");

        // act
        boolean active = instanceManagerApplication.becomeActive();

        // assert
        assertFalse(active);
    }

    @Test
    public void shouldNotBecomeActiveAsImageDirectoryDoesntExist() {
        // setup
        when(instanceImageManager.doesDirectoryExist("image.path")).thenReturn(false);

        // act
        boolean active = instanceManagerApplication.becomeActive();

        // assert
        assertFalse(active);
    }

    @Test
    public void shouldRecheckResourcesAfterAnycastHandled() throws Exception {
        // setup
        when(this.anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeId.toStringFull())).thenReturn(true);

        // act
        boolean anycastResponse = instanceManagerApplication.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, reservation);

        // assert
        assertTrue(anycastResponse);
        verify(instanceManagerApplicationSubscriptionHelper).checkCapacityAndSubscribeUnSubscribe();
    }

    @Test
    public void shouldRecheckCapacityWhenInstanceTerminated() throws Exception {
        // act
        instanceManagerApplication.instanceTerminated(instance);

        // assert
        verify(instanceManagerApplicationSubscriptionHelper).checkCapacityAndSubscribeUnSubscribe();
    }

    @Test
    public void shouldRemoveInstanceFromResourceRegistryWhenInstanceTerminated() {
        // act
        instanceManagerApplication.instanceTerminated(instance);

        // assert
        verify(anycastHandler).deRegisterInstanceWithSharedResourceManager(instance);
    }

    @Test
    public void testDeliverShouldDelegateToDeliverHandler() {
        // setup
        ReceivedMessageContext messageContext = mock(ReceivedMessageContext.class);
        PId id = mock(PId.class);

        // act
        this.instanceManagerApplication.deliver(id, messageContext);

        // assert
        verify(this.deliverHandler).deliver(id, messageContext);
    }

    @Test
    public void shouldActivateWatcherManagerOnApplicationStart() {
        // act
        // this.instanceManagerApplication.onApplicationStarting(); <--- already run in setup

        // assert
        verify(instanceManagerApplicationQueueWatcherInitiator).initialiseWatchers(nodeId.toStringFull());
    }

    @Test
    public void shouldSubscribeToTopicOnBecomeActiveIfResourcesAreAvailable() throws Exception {
        // setup
        when(anycastHandler.hasEnoughResources(isA(InstanceTypeConfiguration.class))).thenReturn(true);

        // act
        this.instanceManagerApplication.becomeActive();

        // assert
        verify(this.instanceManagerApplicationSubscriptionHelper).checkCapacityAndSubscribeUnSubscribe();
    }

    @Test
    public void handleNodeDepartureShouldCheckQueueWatchingStatus() {
        // setup
        when(koalaIdUtils.isIdClosestToMe(eq(nodeId.toStringFull()), eq(leafNodeHandles), isA(Id.class), eq(NodeScope.AVAILABILITY_ZONE))).thenReturn(false);

        // act
        instanceManagerApplication.handleNodeDeparture(nodeId.toStringFull());

        // assert
        verify(instanceManagerApplicationQueueWatcherInitiator).removeWatchers();
    }

    @Test
    public void handleNodeArrivalShouldCheckQueueWatchingStatus() {
        // setup
        when(koalaIdUtils.isIdClosestToMe(eq(nodeId.toStringFull()), eq(leafNodeHandles), isA(Id.class), eq(NodeScope.AVAILABILITY_ZONE))).thenReturn(false);

        // act
        instanceManagerApplication.handleNodeArrival(nodeId.toStringFull());

        // assert
        verify(instanceManagerApplicationQueueWatcherInitiator).removeWatchers();
    }

    @Test
    public void shouldRegisterXenRefreshHandlerWithTheNodeIdOnAppStart() {
        // act
        // done in setup();

        // assert
        verify(xenRefreshHandler).registerWatcher(nodeId.toStringFull());
    }

}
