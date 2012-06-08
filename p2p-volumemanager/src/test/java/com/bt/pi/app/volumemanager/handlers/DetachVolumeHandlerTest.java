package com.bt.pi.app.volumemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class DetachVolumeHandlerTest {
    private static final String INSTANCE_ID = "i-123456";
    private static final String VOLUME_ID = "v-00125678";
    @InjectMocks
    private DetachVolumeHandler detachHandler = new DetachVolumeHandler();
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    private String nodeId = "99001122";
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PId volumePastryId;
    @Mock
    private ReceivedMessageContext receivedMessageContext;
    @Mock
    private PId instanceManagerNodeId;
    @Mock
    private PId instancePastryId;
    @Mock
    private PId detachVolumeQueueId;
    private Volume volume = new Volume();
    private Instance instance = new Instance();
    private Volume sparseVolume = new Volume();

    @Before
    public void setUp() throws Exception {
        when(piIdBuilder.getPId(anyString())).thenReturn(volumePastryId);
        when(piIdBuilder.getPId(isA(Volume.class))).thenReturn(volumePastryId);
        when(volumePastryId.forGlobalAvailablityZoneCode(anyInt())).thenReturn(volumePastryId);
        when(piIdBuilder.getPiQueuePId(PiQueue.DETACH_VOLUME)).thenReturn(detachVolumeQueueId);
        when(detachVolumeQueueId.forLocalScope(PiQueue.DETACH_VOLUME.getNodeScope())).thenReturn(detachVolumeQueueId);
        when(this.dhtClientFactory.createWriter()).thenReturn(this.dhtWriter);
        when(this.dhtClientFactory.createReader()).thenReturn(this.dhtReader);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TaskProcessingQueueContinuation continuation = (TaskProcessingQueueContinuation) invocation.getArguments()[3];
                continuation.receiveResult(volume.getUrl(), nodeId);
                return null;
            }
        }).when(taskProcessingQueueHelper).setNodeIdOnUrl(eq(detachVolumeQueueId), eq(Volume.getUrl(VOLUME_ID)), eq(nodeId), isA(TaskProcessingQueueContinuation.class));
        instance.setInstanceId(INSTANCE_ID);
        instance.setNodeId("n-123");
        sparseVolume = createVolume(VolumeState.DETACHING);
        volume = createVolume(VolumeState.IN_USE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetachVolumeNoVolumeId() {
        // setup
        sparseVolume.setVolumeId(null);

        // act
        this.detachHandler.detachVolume(sparseVolume, receivedMessageContext, nodeId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetachVolumeNoInstanceId() {
        // setup
        sparseVolume.setInstanceId(null);

        // act
        this.detachHandler.detachVolume(sparseVolume, receivedMessageContext, nodeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldChangeVolumeStatusToDetachingFromInUse() {
        // setup
        UpdateResolvingContinuationAnswer a = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(a).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        // act
        this.detachHandler.detachVolume(sparseVolume, receivedMessageContext, nodeId);

        // assert
        Volume result = (Volume) a.getResult();
        assertEquals(VolumeState.DETACHING, result.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldChangeVolumeStatusToForceDetachingFromInUse() {
        // setup
        sparseVolume.setStatus(VolumeState.FORCE_DETACHING);
        UpdateResolvingContinuationAnswer a = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(a).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        // act
        this.detachHandler.detachVolume(sparseVolume, receivedMessageContext, nodeId);

        // assert
        Volume result = (Volume) a.getResult();
        assertEquals(VolumeState.FORCE_DETACHING, result.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSendSparseVolumeToInstanceManagerApplication() {
        // setup
        UpdateResolvingContinuationAnswer a = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(a).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        GenericContinuationAnswer<Instance> b = new GenericContinuationAnswer<Instance>(instance);
        doAnswer(b).when(dhtReader).getAsync(isA(PId.class), isA(PiContinuation.class));

        when(this.koalaIdFactory.buildPIdFromHexString("n-123")).thenReturn(instanceManagerNodeId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(INSTANCE_ID))).thenReturn(instancePastryId);

        // act
        this.detachHandler.detachVolume(sparseVolume, receivedMessageContext, nodeId);

        // assert
        verify(receivedMessageContext).routePiMessageToApplication(eq(instanceManagerNodeId), eq(EntityMethod.UPDATE), eq(sparseVolume), eq(InstanceManagerApplication.APPLICATION_NAME), isA(PiContinuation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateVolumeStatusWithTheVolumeStatusReturnedFromInstanceManagerApplication() {
        // setup
        final VolumeState volumeStateReturnedFromInstanceManager = VolumeState.AVAILABLE;

        UpdateResolvingContinuationAnswer a = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(a).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        GenericContinuationAnswer<Instance> b = new GenericContinuationAnswer<Instance>(instance);
        doAnswer(b).when(dhtReader).getAsync(isA(PId.class), isA(PiContinuation.class));

        when(this.koalaIdFactory.buildPIdFromHexString("n-123")).thenReturn(instanceManagerNodeId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(INSTANCE_ID))).thenReturn(instancePastryId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation<Volume> piC = (PiContinuation<Volume>) invocation.getArguments()[4];
                volume.setStatus(volumeStateReturnedFromInstanceManager);
                piC.handleResult(volume);
                return null;
            }
        }).when(receivedMessageContext).routePiMessageToApplication(isA(PId.class), eq(EntityMethod.UPDATE), isA(Volume.class), anyString(), isA(PiContinuation.class));

        when(piIdBuilder.getPId(VOLUME_ID)).thenReturn(volumePastryId);

        UpdateResolvingContinuationAnswer d = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(d).when(dhtWriter).update(isA(PId.class), isA(PiEntity.class), isA(UpdateResolvingPiContinuation.class));

        // act
        this.detachHandler.detachVolume(sparseVolume, receivedMessageContext, nodeId);

        // assert
        Volume actualVolume = (Volume) d.getResult();
        assertEquals(volumeStateReturnedFromInstanceManager, actualVolume.getStatus());
        assertNull(actualVolume.getInstanceId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotRemoveInstanceIdFromVolumeWhenStatusFromInstanceManagerIsInUse() {
        // setup
        final VolumeState volumeStateReturnedFromInstanceManager = VolumeState.IN_USE;

        UpdateResolvingContinuationAnswer a = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(a).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        GenericContinuationAnswer<Instance> b = new GenericContinuationAnswer<Instance>(instance);
        doAnswer(b).when(dhtReader).getAsync(isA(PId.class), isA(PiContinuation.class));

        when(this.koalaIdFactory.buildPIdFromHexString("n-123")).thenReturn(instanceManagerNodeId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(INSTANCE_ID))).thenReturn(instancePastryId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation<Volume> piC = (PiContinuation<Volume>) invocation.getArguments()[4];
                volume.setStatus(volumeStateReturnedFromInstanceManager);
                piC.handleResult(volume);
                return null;
            }
        }).when(receivedMessageContext).routePiMessageToApplication(isA(PId.class), eq(EntityMethod.UPDATE), isA(Volume.class), anyString(), isA(PiContinuation.class));

        when(piIdBuilder.getPId(VOLUME_ID)).thenReturn(volumePastryId);

        UpdateResolvingContinuationAnswer d = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(d).when(dhtWriter).update(isA(PId.class), isA(PiEntity.class), isA(UpdateResolvingPiContinuation.class));

        // act
        this.detachHandler.detachVolume(sparseVolume, receivedMessageContext, nodeId);

        // assert
        Volume actualVolume = (Volume) d.getResult();
        assertEquals(volumeStateReturnedFromInstanceManager, actualVolume.getStatus());
        assertEquals(INSTANCE_ID, actualVolume.getInstanceId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRemoveDetachVolumeTaskFromTaskProcessingQueue() {
        // setup
        UpdateResolvingContinuationAnswer a = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(a).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        GenericContinuationAnswer<Instance> b = new GenericContinuationAnswer<Instance>(instance);
        doAnswer(b).when(dhtReader).getAsync(isA(PId.class), isA(PiContinuation.class));

        when(this.koalaIdFactory.buildPIdFromHexString("n-123")).thenReturn(instanceManagerNodeId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(INSTANCE_ID))).thenReturn(instancePastryId);

        final Volume volumeFromInstanceManager = createVolume(VolumeState.AVAILABLE);
        GenericContinuationAnswer<Volume> c = new GenericContinuationAnswer<Volume>(volumeFromInstanceManager);
        doAnswer(c).when(receivedMessageContext).routePiMessageToApplication(isA(PId.class), eq(EntityMethod.UPDATE), isA(Volume.class), anyString(), isA(PiContinuation.class));

        when(piIdBuilder.getPId(VOLUME_ID)).thenReturn(volumePastryId);

        UpdateResolvingContinuationAnswer d = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(d).when(dhtWriter).update(isA(PId.class), isA(PiEntity.class), isA(UpdateResolvingPiContinuation.class));

        // act
        this.detachHandler.detachVolume(sparseVolume, receivedMessageContext, nodeId);

        // assert
        verify(taskProcessingQueueHelper).removeUrlFromQueue(detachVolumeQueueId, volume.getUrl());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotRemoveDetachVolumeTaskFromTaskProcessingQueueIfStillInUse() {
        // setup
        UpdateResolvingContinuationAnswer a = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(a).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        GenericContinuationAnswer<Instance> b = new GenericContinuationAnswer<Instance>(instance);
        doAnswer(b).when(dhtReader).getAsync(isA(PId.class), isA(PiContinuation.class));

        when(this.koalaIdFactory.buildPIdFromHexString("n-123")).thenReturn(instanceManagerNodeId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(INSTANCE_ID))).thenReturn(instancePastryId);

        GenericContinuationAnswer<Volume> c = new GenericContinuationAnswer<Volume>(volume);
        doAnswer(c).when(receivedMessageContext).routePiMessageToApplication(isA(PId.class), eq(EntityMethod.UPDATE), isA(Volume.class), anyString(), isA(PiContinuation.class));

        when(piIdBuilder.getPId(VOLUME_ID)).thenReturn(volumePastryId);

        UpdateResolvingContinuationAnswer d = new UpdateResolvingContinuationAnswer(volume);
        doAnswer(d).when(dhtWriter).update(isA(PId.class), isA(PiEntity.class), isA(UpdateResolvingPiContinuation.class));

        // act
        this.detachHandler.detachVolume(sparseVolume, receivedMessageContext, nodeId);

        // assert
        verify(taskProcessingQueueHelper, never()).removeUrlFromQueue(detachVolumeQueueId, volume.getUrl());
    }

    private Volume createVolume(VolumeState volumeState) {
        Volume volume = new Volume();
        volume.setVolumeId(VOLUME_ID);
        volume.setStatus(volumeState);
        volume.setInstanceId(INSTANCE_ID);
        return volume;
    }
}
