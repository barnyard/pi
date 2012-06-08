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
import com.bt.pi.app.volumemanager.handlers.AttachVolumeHandler;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class AttachVolumeTaskProcessingQueueContinuationTest {
    @InjectMocks
    private AttachVolumeTaskProcessingQueueContinuation attachVolumeTaskProcessingQueueContinuation = new AttachVolumeTaskProcessingQueueContinuation();
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private AttachVolumeHandler attachHandler;
    @Mock
    private MessageContextFactory messageContextFactory;
    @Mock
    private MessageContext messageContext;
    @Mock
    private DhtWriter writer;
    @Mock
    private Volume volume;
    @Mock
    private PId id;
    private String nodeId = "0101111222";
    private String url = "vol:vol-123";
    private CountDownLatch latch = new CountDownLatch(1);
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PId attachVolumeQueueId;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(this.messageContextFactory.newMessageContext()).thenReturn(messageContext);
        when(this.dhtClientFactory.createWriter()).thenReturn(writer);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(url)).thenReturn(id);
        when(volume.getVolumeId()).thenReturn("vol-123");
        when(volume.getUrl()).thenReturn(url);
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
        when(piIdBuilder.getPiQueuePId(PiQueue.ATTACH_VOLUME)).thenReturn(attachVolumeQueueId);
        when(attachVolumeQueueId.forLocalScope(PiQueue.ATTACH_VOLUME.getNodeScope())).thenReturn(attachVolumeQueueId);
    }

    @Test
    public void testReceiveResult() throws InterruptedException {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.ATTACHING);

        // act
        attachVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(volume).setStatus(VolumeState.AVAILABLE);
        verify(this.attachHandler).attachVolume(volume, messageContext, nodeId);
    }

    @Test
    public void shouldNotResetVolumeStatusToAvailableIfItIsAlreadyInUse() throws InterruptedException {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);

        // act
        attachVolumeTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(volume, never()).setStatus(VolumeState.AVAILABLE);
        verify(this.attachHandler, never()).attachVolume(volume, messageContext, nodeId);
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(eq(attachVolumeQueueId), eq(url));
    }
}
