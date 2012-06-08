package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
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

import com.bt.pi.app.common.entities.BlockDeviceMapping;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAction;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class DeliverHandlerTest {
    private String nodeIdFull = "nodeIdFull";
    @InjectMocks
    private DeliverHandler deliverHandler = new DeliverHandler();
    @Mock
    private ReceivedMessageContext messageContext;
    @Mock
    private PId nodeId;
    @Mock
    private TerminateInstanceHandler terminateInstanceHandler;
    @Mock
    private RunningInstanceInteractionHandler runningInstanceInteractionHandler;
    @Mock
    private Instance instance;
    @Mock
    private AttachVolumeHandler attachVolHandler;
    @Mock
    private DetachVolumeHandler detachVolHandler;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private InstanceManagerApplication instanceManagerApplication;
    @Mock
    private PubSubMessageContext mockPubSubMessageContext;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PId attachVolumeQueueId;
    @Mock
    private PId detachVolumeQueueId;
    @Mock
    private PId createSnapshotQueueId;
    @Mock
    private Snapshot snapshot;
    @Mock
    private CreateSnapshotHandler createSnapshotHandler;

    @Before
    public void before() {
        when(instanceManagerApplication.getNodeIdFull()).thenReturn(nodeIdFull);

        when(piIdBuilder.getPiQueuePId(PiQueue.ATTACH_VOLUME)).thenReturn(attachVolumeQueueId);
        when(piIdBuilder.getPiQueuePId(PiQueue.DETACH_VOLUME)).thenReturn(detachVolumeQueueId);
        when(piIdBuilder.getPiQueuePId(PiQueue.CREATE_SNAPSHOT)).thenReturn(createSnapshotQueueId);

        when(attachVolumeQueueId.forLocalScope(PiQueue.ATTACH_VOLUME.getNodeScope())).thenReturn(attachVolumeQueueId);
        when(detachVolumeQueueId.forLocalScope(PiQueue.DETACH_VOLUME.getNodeScope())).thenReturn(detachVolumeQueueId);
        when(createSnapshotQueueId.forLocalScope(PiQueue.CREATE_SNAPSHOT.getNodeScope())).thenReturn(createSnapshotQueueId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.err.println(1);
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult((String) invocation.getArguments()[1], (String) invocation.getArguments()[2]);
                return null;
            }
        }).when(taskProcessingQueueHelper).setNodeIdOnUrl(isA(PId.class), isA(String.class), isA(String.class), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void shouldBailOutOfDeliverIfEntityIsNotInstance() throws Exception {
        // setup
        when(messageContext.getReceivedEntity()).thenReturn(new Reservation());

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(terminateInstanceHandler, never()).terminateInstance(isA(Instance.class));
    }

    @Test
    public void shouldBailOutOfDeliverIfEntityMethodIsNotDelete() throws Exception {
        // setup
        when(messageContext.getMethod()).thenReturn(EntityMethod.CREATE);

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(terminateInstanceHandler, never()).terminateInstance(isA(Instance.class));
    }

    @Test
    public void shouldNotUseRebootInstanceHandlerToRebootInstanceWhenRebootNotRequested() {
        // setup
        when(messageContext.getReceivedEntity()).thenReturn(instance);
        when(instance.isRestartRequested()).thenReturn(false);

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(runningInstanceInteractionHandler, never()).rebootInstance(eq(instance));
    }

    @Test
    public void shouldUseAttachVolumeHandlerToAttachVolumeAndSetNodeIdOnQueue() {
        // setup
        when(messageContext.getMethod()).thenReturn(EntityMethod.UPDATE);
        Volume volume = new Volume();
        volume.setStatus(VolumeState.ATTACHING);
        when(messageContext.getReceivedEntity()).thenReturn(volume);

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(this.attachVolHandler).attachVolume(volume, messageContext);
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(eq(attachVolumeQueueId), eq(volume.getUrl()), eq(nodeIdFull), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void shouldUseDetachVolumeHandlerToDetachVolumeAndSetNodeIdOnQueue() {
        // setup
        when(messageContext.getMethod()).thenReturn(EntityMethod.UPDATE);
        Volume volume = new Volume();
        volume.setStatus(VolumeState.DETACHING);
        when(messageContext.getReceivedEntity()).thenReturn(volume);

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(this.detachVolHandler).detachVolume(volume, messageContext);
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(eq(detachVolumeQueueId), eq(volume.getUrl()), eq(nodeIdFull), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void shouldUseDetachVolumeHandlerToForceDetachVolumeAndSetNodeIdOnQueue() {
        // setup
        when(messageContext.getMethod()).thenReturn(EntityMethod.UPDATE);
        Volume volume = new Volume();
        volume.setStatus(VolumeState.FORCE_DETACHING);
        when(messageContext.getReceivedEntity()).thenReturn(volume);

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(this.detachVolHandler).detachVolume(volume, messageContext);
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(eq(detachVolumeQueueId), eq(volume.getUrl()), eq(nodeIdFull), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void shouldUseRebootInstanceHandlerToRebootInstance() {
        // setup
        when(messageContext.getMethod()).thenReturn(EntityMethod.UPDATE);
        when(messageContext.getReceivedEntity()).thenReturn(instance);
        when(instance.isRestartRequested()).thenReturn(true);

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(runningInstanceInteractionHandler).rebootInstance(eq(instance));
    }

    @Test
    public void shouldUseTerminateInstanceHandlerToTerminateInstance() {
        // setup
        when(messageContext.getMethod()).thenReturn(EntityMethod.DELETE);
        when(messageContext.getReceivedEntity()).thenReturn(instance);

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(this.terminateInstanceHandler).terminateInstance(eq(instance));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void terminateInstanceShouldAnyCastToVolumeManagerForAnyAttachedDevices() throws Exception {
        // setup
        final String volumeId1 = "v-123";
        final Volume volume1 = new Volume();
        volume1.setVolumeId(volumeId1);
        volume1.setInstanceId(instance.getInstanceId());

        final String volumeId2 = "v-456";
        final Volume volume2 = new Volume();
        volume2.setVolumeId(volumeId2);
        volume2.setInstanceId(instance.getInstanceId());
        volume2.setAvailabilityZone("baynard");

        List<BlockDeviceMapping> blockDeviceMappings = new ArrayList<BlockDeviceMapping>();
        blockDeviceMappings.add(new BlockDeviceMapping(volumeId1));
        blockDeviceMappings.add(new BlockDeviceMapping(volumeId2));
        when(instance.getBlockDeviceMappings()).thenReturn(blockDeviceMappings);

        final PId id1 = mock(PId.class);
        when(piIdBuilder.getPId(volume1)).thenReturn(id1);
        when(id1.forGlobalAvailablityZoneCode(anyInt())).thenReturn(id1);
        final PId id2 = mock(PId.class);
        when(piIdBuilder.getPId(volume2)).thenReturn(id2);
        when(id2.forGlobalAvailablityZoneCode(anyInt())).thenReturn(id2);
        when(piIdBuilder.getPId(isA(Volume.class))).thenReturn(id1).thenReturn(id2);

        when(dhtClientFactory.createReader()).thenReturn(dhtReader);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PId id = (PId) invocation.getArguments()[0];
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[1];
                if (id.equals(id1))
                    continuation.handleResult(volume1);
                else if (id.equals(id2))
                    continuation.handleResult(volume2);

                return null;
            }
        }).when(dhtReader).getAsync(isA(PId.class), isA(PiContinuation.class));
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(volumeId1)).thenReturn(0x1234);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(volumeId2)).thenReturn(0x1234);
        when(instanceManagerApplication.newPubSubMessageContextFromGlobalAvzCode(PiTopics.DETACH_VOLUME, 0x1234)).thenReturn(mockPubSubMessageContext);

        final CountDownLatch continuationLatch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(null, null);
                continuationLatch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(isA(PId.class), anyString(), anyInt(), isA(TaskProcessingQueueContinuation.class));

        // act
        deliverHandler.terminateInstance(instance);

        // assert
        assertTrue(continuationLatch.await(250, TimeUnit.MILLISECONDS));
        verify(mockPubSubMessageContext).sendAnycast(eq(EntityMethod.UPDATE), argThat(new ArgumentMatcher<Volume>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof Volume))
                    return false;
                Volume arg = (Volume) argument;
                if (!arg.getVolumeId().equals(volumeId1))
                    return false;
                if (!arg.getStatus().equals(VolumeState.FORCE_DETACHING))
                    return false;
                return true;
            }
        }));

        verify(mockPubSubMessageContext).sendAnycast(eq(EntityMethod.UPDATE), argThat(new ArgumentMatcher<Volume>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof Volume))
                    return false;
                Volume arg = (Volume) argument;
                if (!arg.getVolumeId().equals(volumeId2))
                    return false;
                if (!arg.getStatus().equals(VolumeState.FORCE_DETACHING))
                    return false;
                return true;
            }
        }));
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(detachVolumeQueueId), eq(volume1.getUrl()), eq(Integer.parseInt(DeliverHandler.DEFAULT_VOLUME_TASK_RETRIES)), isA(TaskProcessingQueueContinuation.class));
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(detachVolumeQueueId), eq(volume2.getUrl()), eq(Integer.parseInt(DeliverHandler.DEFAULT_VOLUME_TASK_RETRIES)), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void shouldHandleSnapshotCreationMessagesAndSetNodeIdOnQueue() {
        // setup
        when(messageContext.getReceivedEntity()).thenReturn(snapshot);
        when(snapshot.getUrl()).thenReturn("url");

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(createSnapshotHandler).createSnapshot(snapshot, messageContext);
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(eq(createSnapshotQueueId), eq("url"), eq(nodeIdFull), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void shouldPauseInstanceIfInstanceActionIsPause() {
        // setup
        when(messageContext.getMethod()).thenReturn(EntityMethod.UPDATE);
        when(messageContext.getReceivedEntity()).thenReturn(instance);
        when(instance.anyActionRequired()).thenReturn(InstanceAction.PAUSE);

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(runningInstanceInteractionHandler).pauseInstance(eq(instance));
    }

    @Test
    public void shouldUnPauseInstanceIfInstanceActionIsUnPaused() {
        // setup
        when(messageContext.getMethod()).thenReturn(EntityMethod.UPDATE);
        when(messageContext.getReceivedEntity()).thenReturn(instance);
        when(instance.anyActionRequired()).thenReturn(InstanceAction.UNPAUSE);

        // act
        deliverHandler.deliver(nodeId, messageContext);

        // assert
        verify(runningInstanceInteractionHandler).unPauseInstance(eq(instance));
    }
}
