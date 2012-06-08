package com.bt.pi.app.volumemanager;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
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
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class CreateSnapshotTaskProcessingQueueRetriesExhaustedContinuationTest {
    @InjectMocks
    private CreateSnapshotTaskProcessingQueueRetriesExhaustedContinuation createSnapshotTaskProcessingQueueRetriesExhaustedContinuation = new CreateSnapshotTaskProcessingQueueRetriesExhaustedContinuation();
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter writer;
    @Mock
    private PId piVolumeId;
    @Mock
    private PId piSnapshotId;
    @Mock
    private Volume volume;
    @Mock
    private Snapshot snapshot;
    private String nodeId = "0101111222";
    private String volumeId = "vol-123";
    private String volumeUrl = "vol:" + volumeId;
    private String snapshotId = "snap-123";
    private String snapshotUrl = "snap:" + snapshotId;
    private int globalAvailabilityZoneCode = 22;
    private CountDownLatch latch = new CountDownLatch(2);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void before() {
        when(this.dhtClientFactory.createWriter()).thenReturn(writer);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(snapshotUrl)).thenReturn(piSnapshotId);
        when(volume.getUrl()).thenReturn(volumeUrl);
        when(volume.getAvailabilityZoneCode()).thenReturn(globalAvailabilityZoneCode);
        when(volume.getVolumeId()).thenReturn(volumeId);
        when(snapshot.getVolumeId()).thenReturn(volumeId);
        when(snapshot.getSnapshotId()).thenReturn(snapshotId);
        when(this.piIdBuilder.getPId(volumeUrl)).thenReturn(piVolumeId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(volumeId)).thenReturn(globalAvailabilityZoneCode);
        when(piVolumeId.forGlobalAvailablityZoneCode(globalAvailabilityZoneCode)).thenReturn(piVolumeId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object updated = continuation.update(snapshot, null);
                assertTrue(updated instanceof Snapshot);
                continuation.handleResult(snapshot);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(piSnapshotId), isA(UpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object updated = continuation.update(volume, null);
                assertTrue(updated instanceof Volume);
                continuation.handleResult(volume);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(piVolumeId), isA(UpdateResolvingPiContinuation.class));
    }

    @Test
    public void testReceiveResultWhenVolumeIsAvailableSnapshotting() throws InterruptedException {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE_SNAPSHOTTING);

        // act
        createSnapshotTaskProcessingQueueRetriesExhaustedContinuation.receiveResult(snapshotUrl, nodeId);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        verify(snapshot).setStatus(SnapshotState.ERROR);
        verify(volume).setStatus(VolumeState.AVAILABLE);
    }

    @Test
    public void testReceiveResultWhenVolumeIsInUseSnapshotting() throws InterruptedException {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE_SNAPSHOTTING);

        // act
        createSnapshotTaskProcessingQueueRetriesExhaustedContinuation.receiveResult(snapshotUrl, nodeId);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        verify(snapshot).setStatus(SnapshotState.ERROR);
        verify(volume).setStatus(VolumeState.IN_USE);
    }
}
