package com.bt.pi.app.volumemanager.handlers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class AttachVolumeHandlerTest {
    @InjectMocks
    private AttachVolumeHandler attachHandler = new AttachVolumeHandler();
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
    private PId runInstanceQueueId;
    @Mock
    private PId attachVolumeQueueId;
    private String instanceId = "i-123456";
    private Volume volume = new Volume();

    @Before
    public void setUp() throws Exception {
        when(piIdBuilder.getPId(anyString())).thenReturn(volumePastryId);
        when(volumePastryId.forGlobalAvailablityZoneCode(anyInt())).thenReturn(volumePastryId);
        when(this.dhtClientFactory.createWriter()).thenReturn(this.dhtWriter);
        when(this.dhtClientFactory.createReader()).thenReturn(this.dhtReader);
        when(piIdBuilder.getPiQueuePId(PiQueue.RUN_INSTANCE)).thenReturn(runInstanceQueueId);
        when(runInstanceQueueId.forLocalScope(PiQueue.RUN_INSTANCE.getNodeScope())).thenReturn(runInstanceQueueId);
        when(piIdBuilder.getPiQueuePId(PiQueue.ATTACH_VOLUME)).thenReturn(attachVolumeQueueId);
        when(attachVolumeQueueId.forLocalScope(PiQueue.ATTACH_VOLUME.getNodeScope())).thenReturn(attachVolumeQueueId);
        volume.setInstanceId(instanceId);
        volume.setVolumeId("v-789");
        volume.setStatus(VolumeState.AVAILABLE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttachVolumeNoVolumeId() {
        // setup

        // act
        this.attachHandler.attachVolume(volume, receivedMessageContext, nodeId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttachVolumeNoInstanceId() {
        // setup

        // act
        this.attachHandler.attachVolume(volume, receivedMessageContext, nodeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAttachVolume() throws InterruptedException {
        // setup
        final String url = "vol:v-789";
        final Volume volume = mock(Volume.class);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE).thenReturn(VolumeState.ATTACHING);
        when(volume.getInstanceId()).thenReturn(instanceId);
        when(volume.getVolumeId()).thenReturn("v-789");
        when(volume.getUrl()).thenReturn(url);
        final String device = "/dev/sdb2";
        when(volume.getDevice()).thenReturn(device);

        final Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instance.setNodeId(nodeId);

        final CountDownLatch latch = new CountDownLatch(1);

        when(this.koalaIdFactory.buildPIdFromHexString(nodeId)).thenReturn(instanceManagerNodeId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId))).thenReturn(instancePastryId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(eq(url))).thenReturn(0x1234);
        when(piIdBuilder.getPId(volume)).thenReturn(volumePastryId);
        when(volumePastryId.forGlobalAvailablityZoneCode(0x1234)).thenReturn(volumePastryId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("rawtypes")
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(volume, volume);
                continuation.handleResult(volume);

                // clear instanceId so that we can assert it gets set later
                volume.setInstanceId(null);

                return null;
            }
        }).when(this.dhtWriter).update(eq(volumePastryId), isA(UpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("rawtypes")
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[2];
                continuation.update(volume, volume);
                continuation.handleResult(volume);
                return null;
            }
        }).when(this.dhtWriter).update(eq(volumePastryId), isA(Volume.class), isA(UpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("rawtypes")
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[1];
                continuation.handleResult(instance);
                return null;
            }
        }).when(this.dhtReader).getAsync(isA(PId.class), isA(PiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("rawtypes")
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[4];
                continuation.handleResult(volume);
                return null;
            }
        }).when(receivedMessageContext).routePiMessageToApplication(eq(instanceManagerNodeId), eq(EntityMethod.UPDATE), eq(volume), eq(InstanceManagerApplication.APPLICATION_NAME), isA(PiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Long attachTime = (Long) invocation.getArguments()[0];
                Long now = System.currentTimeMillis();
                assertTrue(Math.abs(now - attachTime) < 5000);
                return null;
            }
        }).when(volume).setAttachTime(anyLong());

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TaskProcessingQueueContinuation continuation = (TaskProcessingQueueContinuation) invocation.getArguments()[3];
                continuation.receiveResult(url, nodeId);
                latch.countDown();
                return null;
            }
        }).when(this.taskProcessingQueueHelper).setNodeIdOnUrl(eq(attachVolumeQueueId), eq(url), eq(nodeId), isA(TaskProcessingQueueContinuation.class));

        // act
        this.attachHandler.attachVolume(volume, receivedMessageContext, nodeId);

        // assert
        latch.await(1000, TimeUnit.MILLISECONDS);
        verify(volume).setStatus(VolumeState.ATTACHING);
        verify(volume).setStatus(VolumeState.IN_USE);
        verify(volume).setInstanceId(instanceId);
        verify(volume).setDevice(device);
        verify(volume).setAttachTime(anyLong());
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(attachVolumeQueueId, url);
    }
}
