package com.bt.pi.app.volumemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import com.bt.pi.app.volumemanager.handlers.DetachVolumeHandler;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class DetachVolumeTaskProcessingQueueContinuationTest {
    @InjectMocks
    private DetachVolumeTaskProcessingQueueContinuation detachVolumeTaskProcessingQueueContinuation = new DetachVolumeTaskProcessingQueueContinuation();
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DetachVolumeHandler detachHandler;
    @Mock
    private MessageContextFactory messageContextFactory;
    @Mock
    private MessageContext messageContext;
    @Mock
    private PId volumePastryId;
    private Volume volume;
    private String nodeId = "0101111222";
    private String volumeId = "vol-123";
    private String url = "vol:" + volumeId;
    private String instanceId = "i-12345678";
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PId detachVolumeQueueId;
    private CountDownLatch latch = new CountDownLatch(1);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(this.messageContextFactory.newMessageContext()).thenReturn(messageContext);
        when(this.dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(url)).thenReturn(volumePastryId);
        when(piIdBuilder.getPiQueuePId(PiQueue.DETACH_VOLUME)).thenReturn(detachVolumeQueueId);
        when(detachVolumeQueueId.forLocalScope(PiQueue.DETACH_VOLUME.getNodeScope())).thenReturn(detachVolumeQueueId);
        volume = new Volume();
        volume.setVolumeId(volumeId);
        volume.setInstanceId(instanceId);
        volume.setStatus(VolumeState.DETACHING);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object updated = continuation.update(volume, null);
                if (null != updated) {
                    Volume updatedVolume = (Volume) updated;
                    assertEquals(VolumeState.IN_USE, updatedVolume.getStatus());
                }
                continuation.handleResult(updated);
                latch.countDown();
                return null;
            }
        }).when(dhtWriter).update(eq(volumePastryId), isA(UpdateResolvingPiContinuation.class));
    }

    @Test
    public void testReceiveResult() throws Exception {
        // setup

        // act
        detachVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(VolumeState.DETACHING, volume.getStatus());
        verify(this.detachHandler).detachVolume(volume, messageContext, nodeId);
    }

    @Test
    public void testReceiveResultForce() throws Exception {
        // setup
        volume.setStatus(VolumeState.FORCE_DETACHING);

        // act
        detachVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(VolumeState.FORCE_DETACHING, volume.getStatus());
        verify(this.detachHandler).detachVolume(volume, messageContext, nodeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDoNothingIfUnableToLookupVolume() throws InterruptedException {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(null, null);
                latch.countDown();
                return null;
            }
        }).when(dhtWriter).update(eq(volumePastryId), isA(UpdateResolvingPiContinuation.class));

        // act
        detachVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(this.detachHandler, never()).detachVolume(volume, messageContext, nodeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotInvokeHandlerIfAlreadyDetached() throws InterruptedException {
        // setup
        volume.setStatus(VolumeState.AVAILABLE);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object updated = continuation.update(volume, null);
                assertNull(updated);
                continuation.handleResult(null);
                latch.countDown();
                return null;
            }
        }).when(dhtWriter).update(eq(volumePastryId), isA(UpdateResolvingPiContinuation.class));

        // act
        detachVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(this.detachHandler, never()).detachVolume(volume, messageContext, nodeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotInvokeHandlerIfVolumeHasNoInstanceId() throws InterruptedException {
        // setup
        volume.setStatus(VolumeState.IN_USE);
        volume.setInstanceId(null);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object updated = continuation.update(volume, null);
                assertNotNull(updated);
                continuation.handleResult(updated);
                latch.countDown();
                return null;
            }
        }).when(dhtWriter).update(eq(volumePastryId), isA(UpdateResolvingPiContinuation.class));

        // act
        detachVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(this.detachHandler, never()).detachVolume(volume, messageContext, nodeId);
    }

    @Test
    public void shouldRemoveFromQueueIfAlreadyDetached() throws InterruptedException {
        // setup
        volume.setStatus(VolumeState.AVAILABLE);

        // act
        detachVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(eq(detachVolumeQueueId), eq(url));
    }
}
