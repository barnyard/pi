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

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
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
public class DeleteSnapshotTaskProcessingQueueContinuationTest {
    private String volumeId = "v-1234";
    private String snapshotId = "snap-1234";
    private String snapshotUrl = "snap:" + snapshotId;
    private String nodeId = "node1234";
    @InjectMocks
    private DeleteSnapshotTaskProcessingQueueContinuation deleteSnapshotTaskProcessingQueueContinuation = new DeleteSnapshotTaskProcessingQueueContinuation();
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
    private PId snapshotPastryId;
    @Mock
    private Snapshot snapshot;
    @Mock
    private PId deleteSnapshotQueueId;
    private int regionCode = 99;
    private int availabilityZoneCode = 88;
    private CountDownLatch updateLatch = new CountDownLatch(1);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(volumeManagerApplication.newLocalPubSubMessageContext(PiTopics.DELETE_SNAPSHOT)).thenReturn(pubSubMessageContext);
        when(volumeManagerApplication.newPubSubMessageContextFromGlobalAvzCode(eq(PiTopics.DELETE_SNAPSHOT), anyInt())).thenReturn(pubSubMessageContext);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(snapshotUrl)).thenReturn(snapshotPastryId);
        when(piIdBuilder.getPiQueuePId(PiQueue.DELETE_SNAPSHOT)).thenReturn(deleteSnapshotQueueId);
        when(deleteSnapshotQueueId.forLocalScope(PiQueue.DELETE_SNAPSHOT.getNodeScope())).thenReturn(deleteSnapshotQueueId);
        when(snapshot.getVolumeId()).thenReturn(volumeId);
        when(snapshot.getUrl()).thenReturn(snapshotUrl);
        when(volumeManagerApplication.newPubSubMessageContextFromGlobalAvzCode(eq(PiTopics.DELETE_VOLUME), anyInt())).thenReturn(pubSubMessageContext);

        when(snapshot.getRegionCode()).thenReturn(regionCode);
        when(snapshot.getAvailabilityZoneCode()).thenReturn(availabilityZoneCode);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object result = continuation.update(snapshot, null);
                continuation.handleResult(result);
                updateLatch.countDown();
                return null;
            }
        }).when(dhtWriter).update(eq(snapshotPastryId), isA(UpdateResolvingPiContinuation.class));
    }

    @Test
    public void shouldOwnTaskByPuttingTheNodeIdOnQueueItem() {
        // act
        deleteSnapshotTaskProcessingQueueContinuation.receiveResult(snapshotUrl, nodeId);

        // assert
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(deleteSnapshotQueueId, snapshotUrl, nodeId);
    }

    @Test
    public void shouldSetSnapshotStatusToCompleteToAllowReprocessing() throws Exception {
        // act
        deleteSnapshotTaskProcessingQueueContinuation.receiveResult(snapshotUrl, nodeId);

        // assert
        assertTrue(updateLatch.await(200, TimeUnit.MILLISECONDS));
        verify(snapshot).setStatus(SnapshotState.COMPLETE);
    }

    @Test
    public void shouldAnyCast() throws Exception {
        // act
        deleteSnapshotTaskProcessingQueueContinuation.receiveResult(snapshotUrl, nodeId);

        // assert
        assertTrue(updateLatch.await(200, TimeUnit.MILLISECONDS));
        verify(pubSubMessageContext).randomAnycast(EntityMethod.DELETE, snapshot);
    }

    @Test
    public void shouldNotUpdateStateOrAnycastIfStateIsAlreadyDeleted() throws Exception {
        // setup
        when(snapshot.getStatus()).thenReturn(SnapshotState.DELETED);

        // act
        deleteSnapshotTaskProcessingQueueContinuation.receiveResult(snapshotUrl, nodeId);

        // assert
        assertTrue(updateLatch.await(200, TimeUnit.MILLISECONDS));
        verify(snapshot, never()).setStatus(isA(SnapshotState.class));
        verify(pubSubMessageContext, never()).randomAnycast(EntityMethod.DELETE, snapshot);
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(eq(deleteSnapshotQueueId), eq(snapshotUrl));
    }
}
