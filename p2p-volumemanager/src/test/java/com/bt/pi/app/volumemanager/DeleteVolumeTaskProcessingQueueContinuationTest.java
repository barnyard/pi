package com.bt.pi.app.volumemanager;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
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
public class DeleteVolumeTaskProcessingQueueContinuationTest {
    private String volumeId = "v-1234";
    private String volumeUrl = "vol:" + volumeId;
    private String nodeId = "node1234";
    @InjectMocks
    private DeleteVolumeTaskProcessingQueueContinuation deleteVolumeTaskProcessingQueueContinuation = new DeleteVolumeTaskProcessingQueueContinuation();
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private VolumeManagerApplication volumeManagerApplication;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private PId volumePastryId;
    @Mock
    private Volume volume;
    @Mock
    private PId deleteVolumeQueueId;
    private int availabilityZoneCode = 99;
    private int regionCode = 99;
    private CountDownLatch latch = new CountDownLatch(1);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(volumeManagerApplication.newLocalPubSubMessageContext(PiTopics.DELETE_VOLUME)).thenReturn(pubSubMessageContext);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(volumeUrl)).thenReturn(volumePastryId);
        when(piIdBuilder.getPiQueuePId(PiQueue.DELETE_VOLUME)).thenReturn(deleteVolumeQueueId);
        when(deleteVolumeQueueId.forLocalScope(PiQueue.DELETE_VOLUME.getNodeScope())).thenReturn(deleteVolumeQueueId);
        when(volume.getVolumeId()).thenReturn(volumeId);
        when(volume.getUrl()).thenReturn(volumeUrl);
        when(volume.getAvailabilityZoneCode()).thenReturn(availabilityZoneCode);
        when(volume.getRegionCode()).thenReturn(regionCode);
        when(volumeManagerApplication.newPubSubMessageContextFromGlobalAvzCode(eq(PiTopics.DELETE_VOLUME), anyInt())).thenReturn(pubSubMessageContext);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object updated = continuation.update(volume, null);
                continuation.handleResult(updated);
                latch.countDown();
                return null;
            }
        }).when(dhtWriter).update(eq(volumePastryId), isA(UpdateResolvingPiContinuation.class));
    }

    @Test
    public void shouldOwnTaskByPuttingTheNodeIdOnQueueItem() throws InterruptedException {
        // act
        deleteVolumeTaskProcessingQueueContinuation.receiveResult(volumeUrl, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(deleteVolumeQueueId, volumeUrl, nodeId);
    }

    @Test
    public void shouldSendAnycastMessageToDeleteVolumeTopicWithNoAvailabilityZoneInVolume() throws InterruptedException {
        // setup

        // act
        deleteVolumeTaskProcessingQueueContinuation.receiveResult(volumeUrl, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(pubSubMessageContext).randomAnycast(EntityMethod.DELETE, volume);
        verify(volume).setStatus(VolumeState.AVAILABLE);
    }

    @Test
    public void shouldSendRandomAnycastMessageToDeleteVolumeTopicWithAvailabilityZoneInVolume() throws InterruptedException {
        // setup

        // act
        deleteVolumeTaskProcessingQueueContinuation.receiveResult(volumeUrl, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(pubSubMessageContext).randomAnycast(EntityMethod.DELETE, volume);
    }

    @Test
    public void shouldNotUpdateStateOrAnycastIfAlreadyDeleted() throws InterruptedException {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.DELETED);

        // act
        deleteVolumeTaskProcessingQueueContinuation.receiveResult(volumeUrl, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(pubSubMessageContext, never()).randomAnycast(EntityMethod.DELETE, volume);
        verify(volume, never()).setStatus(isA(VolumeState.class));
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(eq(deleteVolumeQueueId), eq(volumeUrl));
    }
}
