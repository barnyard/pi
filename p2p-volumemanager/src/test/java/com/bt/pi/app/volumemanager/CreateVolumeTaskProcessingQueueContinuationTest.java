package com.bt.pi.app.volumemanager;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class CreateVolumeTaskProcessingQueueContinuationTest {
    @InjectMocks
    private CreateVolumeTaskProcessingQueueContinuation createVolumeTaskProcessingQueueContinuation = new CreateVolumeTaskProcessingQueueContinuation();
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private VolumeManagerApplication volumeManagerApplication;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private DhtWriter writer;
    @Mock
    private PId id;
    @Mock
    private Volume volume;
    private String nodeId = "0101111222";
    private String volumeId = "vol-123";
    private String url = "vol:" + volumeId;
    private int availabilityZoneCode = 99;
    private CountDownLatch latch = new CountDownLatch(1);
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PId createVolumeQueueId;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        when(this.dhtClientFactory.createWriter()).thenReturn(writer);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(url)).thenReturn(id);
        when(volume.getUrl()).thenReturn(url);
        when(volume.getAvailabilityZoneCode()).thenReturn(availabilityZoneCode);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object updated = continuation.update(volume, null);
                continuation.handleResult(updated);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(id), isA(UpdateResolvingPiContinuation.class));
        when(volumeManagerApplication.newPubSubMessageContextFromGlobalAvzCode(eq(PiTopics.CREATE_VOLUME), eq(availabilityZoneCode))).thenReturn(pubSubMessageContext);
        when(volume.getVolumeId()).thenReturn(volumeId);
        when(piIdBuilder.getPiQueuePId(PiQueue.CREATE_VOLUME)).thenReturn(createVolumeQueueId);
        when(createVolumeQueueId.forLocalScope(PiQueue.CREATE_VOLUME.getNodeScope())).thenReturn(createVolumeQueueId);
    }

    @Test
    public void testReceiveResult() throws Exception {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.CREATING);

        // act
        createVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        verify(volume).setStatus(VolumeState.CREATING);
        verify(pubSubMessageContext).randomAnycast(EntityMethod.CREATE, volume);
    }

    @Test
    public void shouldNotReprocessIfAlreadyAvailable() throws Exception {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        // act
        createVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        verify(volume, never()).setStatus(VolumeState.CREATING);
        verify(pubSubMessageContext, never()).randomAnycast(EntityMethod.CREATE, volume);
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(eq(createVolumeQueueId), eq(url));
    }

    @Test
    public void shouldNotReprocessIfAlreadyAvailableAndSnapshotting() throws Exception {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE_SNAPSHOTTING);

        // act
        createVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        verify(volume, never()).setStatus(VolumeState.CREATING);
        verify(pubSubMessageContext, never()).randomAnycast(EntityMethod.CREATE, volume);
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(eq(createVolumeQueueId), eq(url));
    }
}
