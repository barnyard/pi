package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import rice.Continuation;
import rice.p2p.commonapi.Id;
import rice.p2p.scribe.Topic;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManagerException;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class AnycastHandlerTest {
    @InjectMocks
    private AnycastHandler anycastHandler = new AnycastHandler();
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    private String nodeIdFull = "nodeIdFull";
    private InstanceTypeConfiguration instanceTypeConfiguration;
    private int instanceTypeMemoryInMB = 1024;
    private String instanceType = "try.this.for.size";
    private Set<String> instancesToRun;
    @Mock
    private PId networkManagerId;
    private Instance instance1;
    private String instanceId1 = "instanceId1";
    private Instance instance2;
    private String instanceId2 = "instanceId2";
    @Mock
    private PId instancePastryId1;
    @Mock
    private PId instancePastryId2;
    @Mock
    private SystemResourceState systemResourceState;
    @Mock
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private CountDownLatch latch;
    @Mock
    private PId instanceTypesId;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private ThreadPoolTaskExecutor executor;
    private Reservation reservation;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private Topic topic;
    @Mock
    private DhtCache instanceTypesCache;
    @Mock
    private RunInstanceContinuationHandler instanceReservationHandler;
    private InstanceTypes instanceTypes;
    @Mock
    private RunInstanceContinuation runInstanceContinuation;
    @Mock
    private PId runInstanceQueueId;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        latch = new CountDownLatch(1);
        instanceTypeConfiguration = new InstanceTypeConfiguration(instanceType, 1, instanceTypeMemoryInMB, 5);
        instancesToRun = new HashSet<String>();
        instancesToRun.add(instanceId1);
        instance1 = new Instance();
        setupInstance(instance1, instanceId1);
        instance2 = new Instance();
        setupInstance(instance2, instanceId2);

        reservation = new Reservation();
        reservation.setInstanceIds(instancesToRun);
        reservation.setInstanceType(instanceType);

        instanceTypes = new InstanceTypes();
        instanceTypes.addInstanceType(instanceTypeConfiguration);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(executor).execute(isA(Runnable.class));

        when(piIdBuilder.getPId(SecurityGroup.getUrl(instance1.getUserId(), instance1.getSecurityGroupName()))).thenReturn(networkManagerId);
        when(piIdBuilder.getPId(SecurityGroup.getUrl(instance2.getUserId(), instance2.getSecurityGroupName()))).thenReturn(networkManagerId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instance1.getInstanceId())).thenReturn(123);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instance2.getInstanceId())).thenReturn(456);
        when(networkManagerId.forGlobalAvailablityZoneCode(123)).thenReturn(networkManagerId);
        when(networkManagerId.forGlobalAvailablityZoneCode(456)).thenReturn(networkManagerId);
        when(networkManagerId.forLocalAvailabilityZone()).thenReturn(networkManagerId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId1))).thenReturn(instancePastryId1);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId2))).thenReturn(instancePastryId2);
        when(piIdBuilder.getPId(PiQueue.RUN_INSTANCE.getUrl())).thenReturn(runInstanceQueueId);
        when(runInstanceQueueId.forLocalScope(PiQueue.RUN_INSTANCE.getNodeScope())).thenReturn(runInstanceQueueId);
        when(dhtClientFactory.createWriter()).thenReturn(this.dhtWriter);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(null, null);
                return null;
            }
        }).when(taskProcessingQueueHelper).setNodeIdOnUrl(eq(runInstanceQueueId), argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                System.err.println(argument);
                return Instance.getUrl(instanceId1).equals(argument) || Instance.getUrl(instanceId2).equals(argument);
            }
        }), eq(nodeIdFull), isA(TaskProcessingQueueContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Instance instanceToUse = null;
                if (instancePastryId1.equals(invocation.getArguments()[0]))
                    instanceToUse = instance1;
                else if (instancePastryId2.equals(invocation.getArguments()[0]))
                    instanceToUse = instance2;

                Instance updatedInstance = (Instance) ((UpdateResolvingContinuation<PiEntity, Exception>) invocation.getArguments()[1]).update(instanceToUse, null);
                ((UpdateResolvingContinuation<PiEntity, Exception>) invocation.getArguments()[1]).receiveResult(updatedInstance);
                latch.countDown();
                return null;
            }
        }).when(dhtWriter).update(isA(PId.class), (UpdateResolvingContinuation<PiEntity, Exception>) isA(Continuation.class));

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

        String topicIdStr = "2131232132132132132131";

        PId topicId = mock(PId.class);
        when(topicId.forLocalAvailabilityZone()).thenReturn(topicId);
        when(topicId.getIdAsHex()).thenReturn(topicIdStr);

        Id topicPastryId = mock(Id.class);

        when(topicPastryId.toStringFull()).thenReturn(topicIdStr);

        when(topic.getId()).thenReturn(topicPastryId);

        when(koalaIdFactory.buildPId(isA(PiLocation.class))).thenReturn(topicId);

        when(pubSubMessageContext.getTopicPId()).thenReturn(topicId);

        when(piIdBuilder.getPId(InstanceTypes.URL_STRING)).thenReturn(instanceTypesId);

        when(runInstanceContinuation.getNodeId()).thenReturn(nodeIdFull);
        when(instanceReservationHandler.getContinuation(anyString())).thenReturn(runInstanceContinuation);

        this.anycastHandler.setupInstanceTypesId();
        this.anycastHandler.onApplicationEvent(null);
    }

    private void setupInstance(Instance instance, String instanceId) {
        instance.setInstanceId(instanceId);
        instance.setInstanceType(instanceType);
        instance.setUserId("userId");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRouteMessageToNetworkManager() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Instance instanceArg = (Instance) invocation.getArguments()[2];
                RunInstanceContinuation continuationArg = (RunInstanceContinuation) invocation.getArguments()[4];
                assertEquals(instance1.getUserId(), instanceArg.getUserId());
                assertEquals(instance1.getSecurityGroupName(), instanceArg.getSecurityGroupName());
                assertEquals(nodeIdFull, continuationArg.getNodeId());
                return null;
            }
        }).when(pubSubMessageContext).routePiMessageToApplication(eq(networkManagerId), eq(EntityMethod.CREATE), isA(Instance.class), eq(NetworkManagerApplication.APPLICATION_NAME), isA(Continuation.class));

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertTrue(result);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(pubSubMessageContext).routePiMessageToApplication(eq(networkManagerId), eq(EntityMethod.CREATE), isA(Instance.class), eq(NetworkManagerApplication.APPLICATION_NAME), isA(Continuation.class));
    }

    @Test
    public void shouldRouteMsgToNetworkManagerApplicationForMultipleInstances() throws Exception {
        // setup
        instancesToRun.add(instanceId2);
        latch = new CountDownLatch(2);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Instance instanceArg = (Instance) invocation.getArguments()[2];
                assertEquals(instance1.getUserId(), instanceArg.getUserId());
                assertEquals(instance1.getSecurityGroupName(), instanceArg.getSecurityGroupName());
                RunInstanceContinuation continuationArg = (RunInstanceContinuation) invocation.getArguments()[4];
                assertEquals(nodeIdFull, continuationArg.getNodeId());
                return null;
            }
        }).when(pubSubMessageContext).routePiMessageToApplication(eq(networkManagerId), eq(EntityMethod.CREATE), isA(Instance.class), eq(NetworkManagerApplication.APPLICATION_NAME), isA(RunInstanceContinuation.class));

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertTrue(result);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(pubSubMessageContext, times(2)).routePiMessageToApplication(eq(networkManagerId), eq(EntityMethod.CREATE), isA(Instance.class), eq(NetworkManagerApplication.APPLICATION_NAME), isA(RunInstanceContinuation.class));
    }

    @Test
    public void shouldConvertMBtoKMForRequestedInstanceMemory() throws Exception {
        // setup

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertTrue(result);
        verify(dhtWriter).update(isA(PId.class), argThat(new ArgumentMatcher<UpdateResolvingContinuation<PiEntity, Exception>>() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean matches(Object argument) {
                Instance updatedInstance = (Instance) ((UpdateResolvingContinuation<PiEntity, Exception>) argument).update(instance1, null);
                return (instanceTypeMemoryInMB * 1024) == Integer.parseInt(updatedInstance.getMemoryInKB());
            }
        }));
    }

    @Test
    public void testReAnycastOfPartiallyCompletedReservation() throws Exception {
        // setup
        instancesToRun.add(instanceId2);
        when(systemResourceState.getFreeCores()).thenReturn(1).thenReturn(0);

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertTrue(result);
        verify(systemResourceState).reserveResources(instanceId2, instanceTypeConfiguration);
        verify(pubSubMessageContext).sendAnycast(eq(EntityMethod.CREATE), argThat(new ArgumentMatcher<Reservation>() {
            public boolean matches(Object argument) {
                Reservation arg = (Reservation) argument;
                assertEquals(1, arg.getInstanceIds().size());
                return true;
            }
        }));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInstanceIdIsRegisteredAsConsumerForAnyInstancesStartedOnThisNode() throws Exception {
        // setup
        instancesToRun.add(instanceId2);
        when(systemResourceState.getFreeCores()).thenReturn(1).thenReturn(0);

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertTrue(result);
        verify(consumedDhtResourceRegistry).registerConsumer(argThat(new ArgumentMatcher<PId>() {
            @Override
            public boolean matches(Object argument) {
                return argument == instancePastryId1 || argument == instancePastryId2;
            }
        }), argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument == instance1.getInstanceId() || argument == instance2.getInstanceId();
            }

        }), eq(Instance.class), isA(Continuation.class));
    }

    @Test
    public void shouldNotStartNewInstanceIfAlreadyRunning() throws Exception {
        // setup
        instance1.setState(InstanceState.RUNNING);

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(result);
        verify(piIdBuilder, never()).getPId(SecurityGroup.getUrl(instance1.getUserId(), instance1.getSecurityGroupName()));
        verify(systemResourceState).reserveResources(instance1.getInstanceId(), instanceTypeConfiguration);
        verify(systemResourceState).unreserveResources(instance1.getInstanceId());
    }

    @Test
    public void shouldNotStartNewInstanceIfNodeIdSet() throws Exception {
        // setup
        instance1.setState(InstanceState.PENDING);
        instance1.setNodeId("anodeid");

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(result);
        verify(piIdBuilder, never()).getPId(SecurityGroup.getUrl(instance1.getUserId(), instance1.getSecurityGroupName()));
        verify(systemResourceState).reserveResources(instance1.getInstanceId(), instanceTypeConfiguration);
        verify(systemResourceState).unreserveResources(instance1.getInstanceId());
    }

    @Test
    public void routeMsgToNetworkManagerApplicationNoResources() throws Exception {
        // setup
        when(systemResourceState.getFreeCores()).thenReturn(0);

        // act
        anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(piIdBuilder, never()).getPId(SecurityGroup.getUrl(instance1.getUserId(), instance1.getSecurityGroupName()));
        verify(systemResourceState, never()).reserveResources(instance1.getInstanceId(), instanceTypeConfiguration);
    }

    @Test
    public void shouldNotStartNewInstanceIfUnknownInstanceType() throws Exception {
        // setup
        reservation.setInstanceType("whoo what?");

        // act
        anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(piIdBuilder, never()).getPId(SecurityGroup.getUrl(instance1.getUserId(), instance1.getSecurityGroupName()));
        verify(systemResourceState, never()).reserveResources(isA(String.class), isA(InstanceTypeConfiguration.class));
        verify(systemResourceState, never()).unreserveResources(isA(String.class));
    }

    @Test
    public void shouldMarkInstancesInTaskProcessingQueueWithNodeId() {
        // setup
        instancesToRun.add(instanceId2);

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertTrue(result);
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(eq(runInstanceQueueId), eq(Instance.getUrl(instanceId1)), eq(nodeIdFull), isA(TaskProcessingQueueContinuation.class));
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(eq(runInstanceQueueId), eq(Instance.getUrl(instanceId2)), eq(nodeIdFull), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void shouldRejectMessageIfNotAReservation() {
        // setup
        PiEntity data = mock(PiEntity.class);

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, data, nodeIdFull);

        // assert
        assertFalse(result);
    }

    @Test
    public void shouldRejectReservationIfCannotRunAnyInstances() {
        // setup
        when(systemResourceState.getFreeCores()).thenReturn(0);

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertFalse(result);
        verify(systemResourceState, never()).reserveResources(instance1.getInstanceId(), instanceTypeConfiguration);
        verify(pubSubMessageContext, never()).sendAnycast(isA(EntityMethod.class), isA(PiEntity.class));
    }

    @Test
    public void shouldRejectReservationIfUnableToGetInformationFromLibvirt() {
        // setup
        when(systemResourceState.getFreeCores()).thenThrow(new LibvirtManagerException("FAIL"));

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, reservation, nodeIdFull);

        // assert
        assertFalse(result);
        verify(systemResourceState, never()).reserveResources(instance1.getInstanceId(), instanceTypeConfiguration);
        verify(pubSubMessageContext, never()).sendAnycast(isA(EntityMethod.class), isA(PiEntity.class));
    }

    @Test
    public void shouldRejectInstanceMessageIfNotEnoughResources() {
        // setup
        when(systemResourceState.getFreeCores()).thenReturn(0);

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, instance1, nodeIdFull);

        // assert
        assertFalse(result);
        verify(systemResourceState, never()).reserveResources(instance1.getInstanceId(), instanceTypeConfiguration);
    }

    @Test
    public void shouldAcceptInstanceMessageIfEnoughResources() {
        // setup

        // act
        boolean result = anycastHandler.handleAnycast(pubSubMessageContext, instance1, nodeIdFull);

        // assert
        assertTrue(result);
        verify(systemResourceState).reserveResources(instance1.getInstanceId(), instanceTypeConfiguration);
        verify(pubSubMessageContext).routePiMessageToApplication(eq(networkManagerId), eq(EntityMethod.CREATE), isA(Instance.class), eq(NetworkManagerApplication.APPLICATION_NAME), isA(RunInstanceContinuation.class));
    }

    @Test
    public void nodeStartedEventShouldRefreshInstanceTypes() throws Exception {
        // setup
        latch = new CountDownLatch(1);

        // act
        this.anycastHandler.onApplicationEvent(null);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldDeRegisterInstanceFromSharedResourceManager() {
        // setup
        when(piIdBuilder.getPIdForEc2AvailabilityZone(instance1)).thenReturn(instancePastryId1);

        // act
        this.anycastHandler.deRegisterInstanceWithSharedResourceManager(instance1);

        // assert
        verify(consumedDhtResourceRegistry).deregisterConsumer(instancePastryId1, instance1.getInstanceId());
    }
}
