package com.bt.pi.app.volumemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeleteVolumeHandlerTest {
    @InjectMocks
    private DeleteVolumeHandler deleteHandler = new DeleteVolumeHandler();
    private String tmpdir = System.getProperty("java.io.tmpdir") + "/volumes";
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private CommandRunner commandRunner;
    @Mock
    private ThreadPoolTaskExecutor taskExecutor;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private PId volumePastryId;
    @Mock
    private PId userPastryId;
    private String userId = "fred";
    private User user;
    private String volumeId = "abc123";
    @Mock
    private PId deleteVolumeQueueId;
    @Mock
    private PId removeVolumeFromUserQueueId;
    private String nodeId = "1234";
    private Volume volume = new Volume();

    @Before
    public void setUp() throws Exception {
        volume.setVolumeId(volumeId);
        FileUtils.deleteQuietly(new File(this.tmpdir));
        this.deleteHandler.setNfsVolumesDirectory(tmpdir);

        when(piIdBuilder.getPId(anyString())).thenReturn(volumePastryId);
        when(volumePastryId.forGlobalAvailablityZoneCode(anyInt())).thenReturn(volumePastryId);
        when(piIdBuilder.getPId(User.getUrl(userId))).thenReturn(userPastryId);
        when(piIdBuilder.getPiQueuePId(PiQueue.DELETE_VOLUME)).thenReturn(deleteVolumeQueueId);
        when(piIdBuilder.getPiQueuePId(PiQueue.REMOVE_VOLUME_FROM_USER)).thenReturn(removeVolumeFromUserQueueId);
        when(deleteVolumeQueueId.forLocalScope(PiQueue.DELETE_VOLUME.getNodeScope())).thenReturn(deleteVolumeQueueId);
        when(removeVolumeFromUserQueueId.forLocalScope(PiQueue.REMOVE_VOLUME_FROM_USER.getNodeScope())).thenReturn(removeVolumeFromUserQueueId);

        user = new User();
        user.getVolumeIds().add(volumeId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                return new Thread(r);
            }
        }).when(taskExecutor).createThread(isA(Runnable.class));

        when(this.dhtClientFactory.createWriter()).thenReturn(this.dhtWriter);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TaskProcessingQueueContinuation continuation = (TaskProcessingQueueContinuation) invocation.getArguments()[3];
                continuation.receiveResult(volume.getUrl(), nodeId);
                return null;
            }
        }).when(taskProcessingQueueHelper).setNodeIdOnUrl(eq(deleteVolumeQueueId), eq(volume.getUrl()), eq(nodeId), isA(TaskProcessingQueueContinuation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteVolume() throws Exception {
        // setup
        volume.setStatus(VolumeState.AVAILABLE);
        volume.setOwnerId(userId);

        this.deleteHandler.afterPropertiesSet();

        CommandResult commandResult = mock(CommandResult.class);
        when(this.commandRunner.run(isA(String.class))).thenReturn(commandResult);

        final AtomicInteger continuationCount = new AtomicInteger();

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(volume, volume);
                if (continuationCount.get() < 1)
                    assertEquals(VolumeState.DELETING, volume.getStatus());
                continuationCount.addAndGet(1);
                continuation.handleResult(volume);
                return null;
            }
        }).when(this.dhtWriter).update(eq(volumePastryId), isA(UpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(user, user);
                continuation.handleResult(user);
                return null;
            }
        }).when(this.dhtWriter).update(eq(userPastryId), isA(UpdateResolvingPiContinuation.class));
        when(piIdBuilder.getPId(volume)).thenReturn(volumePastryId);

        // act
        this.deleteHandler.deleteVolume(volume, nodeId);

        // assert
        int count = 0;
        while (count < 10 && !VolumeState.DELETED.equals(volume.getStatus())) {
            Thread.sleep(100);
            count++;
        }
        assertEquals(VolumeState.DELETED, volume.getStatus());
        assertEquals(2, continuationCount.get());
        verify(this.commandRunner).runNicely(String.format("rm %s", this.tmpdir + "/" + volumeId));
        verify(taskProcessingQueueHelper).addUrlToQueue(removeVolumeFromUserQueueId, volume.getUrl(), 5);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testDeleteVolumeNotInCreatedState() throws Exception {
        // setup
        volume.setStatus(VolumeState.IN_USE);
        when(piIdBuilder.getPId(volume)).thenReturn(volumePastryId);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(volume, volume);
                return null;
            }
        }).when(this.dhtWriter).update(eq(volumePastryId), isA(UpdateResolvingPiContinuation.class));

        // act
        try {
            this.deleteHandler.deleteVolume(volume, nodeId);
        } catch (IllegalArgumentException e) {
            assertEquals("volume " + volumeId + " must be in available state", e.getMessage());
            assertEquals(VolumeState.IN_USE, volume.getStatus());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRemoveQueueItemForVolumeAfterDeletingVolume() throws Exception {
        // setup
        volume.setOwnerId(userId);
        volume.setStatus(VolumeState.AVAILABLE);
        when(piIdBuilder.getPId(volume)).thenReturn(volumePastryId);
        GenericContinuationAnswer<Volume> gcaVolume = new GenericContinuationAnswer<Volume>(volume, 1);
        doAnswer(gcaVolume).when(dhtWriter).update(eq(volumePastryId), isA(UpdateResolvingPiContinuation.class));

        UpdateResolvingContinuationAnswer urca = new UpdateResolvingContinuationAnswer(user);
        doAnswer(urca).when(dhtWriter).update(eq(userPastryId), isA(UpdateResolvingPiContinuation.class));

        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer<Volume>() {
            @Override
            public Volume answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).removeUrlFromQueue(any(PId.class), anyString());

        // act
        deleteHandler.deleteVolume(volume, nodeId);

        latch.await(1000, TimeUnit.SECONDS);

        // assert
        verify(taskProcessingQueueHelper).removeUrlFromQueue(deleteVolumeQueueId, volume.getUrl());
    }
}
