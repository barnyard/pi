package com.bt.pi.app.volumemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class DeleteSnapshotTaskProcessingQueueRetriesExhaustedContinuationTest {
    @InjectMocks
    private DeleteSnapshotTaskProcessingQueueRetriesExhaustedContinuation deleteSnapshotTaskProcessingQueueRetriesExhaustedContinuation = new DeleteSnapshotTaskProcessingQueueRetriesExhaustedContinuation();
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    private String volumeId = "v-12345";
    @Mock
    private PId snapshotPastryId;
    @Mock
    private DhtWriter dhtWriter;
    private String nodeId = "nodeId";
    private String snapshotId = "snap-12345678";
    private Snapshot snapshot = new Snapshot(snapshotId, null, SnapshotState.DELETING, 0, 0.0, "", "");
    private String snapshotUrl = "snap:" + snapshotId;

    @SuppressWarnings("unchecked")
    @Test
    public void testReceiveResult() throws Exception {
        // setup
        // when(snapshot.getStatus()).thenReturn(SnapshotState.DELETING);
        // when(snapshot.getSnapshotId()).thenReturn(snapshotId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(snapshotUrl)).thenReturn(snapshotPastryId);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation updateResolvingPiContinuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                updateResolvingPiContinuation.update(snapshot, null);
                updateResolvingPiContinuation.handleResult(snapshot);
                latch.countDown();
                return null;
            }
        }).when(dhtWriter).update(eq(snapshotPastryId), isA(UpdateResolvingPiContinuation.class));

        // act
        this.deleteSnapshotTaskProcessingQueueRetriesExhaustedContinuation.receiveResult(snapshotUrl, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(SnapshotState.ERROR, snapshot.getStatus());
    }
}
