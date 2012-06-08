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

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class CreateVolumeTaskProcessingQueueRetriesExhaustedContinuationTest {
    @InjectMocks
    private CreateVolumeTaskProcessingQueueRetriesExhaustedContinuation createVolumeTaskProcessingQueueRetriesExhaustedContinuation = new CreateVolumeTaskProcessingQueueRetriesExhaustedContinuation();
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    private String volumeId = "v-12345";
    private String volumeUri = "vol:" + volumeId;
    @Mock
    private PId volumePastryId;
    @Mock
    private DhtWriter dhtWriter;
    private String nodeId = "nodeId";
    private Volume volume = new Volume();

    @SuppressWarnings("unchecked")
    @Test
    public void testReceiveResult() throws Exception {
        // setup
        volume.setVolumeId(volumeId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(volumeUri)).thenReturn(volumePastryId);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation updateResolvingPiContinuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                updateResolvingPiContinuation.update(volume, null);
                updateResolvingPiContinuation.handleResult(volume);
                latch.countDown();
                return null;
            }
        }).when(dhtWriter).update(eq(volumePastryId), isA(UpdateResolvingPiContinuation.class));

        // act
        this.createVolumeTaskProcessingQueueRetriesExhaustedContinuation.receiveResult(volumeUri, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(VolumeState.FAILED, volume.getStatus());
    }
}
