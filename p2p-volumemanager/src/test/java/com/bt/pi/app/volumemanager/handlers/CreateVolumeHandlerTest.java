package com.bt.pi.app.volumemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
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
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.SerialExecutor;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateVolumeHandlerTest {
    @InjectMocks
    private CreateVolumeHandler createHandler = new CreateVolumeHandler();
    private String tmpdir = System.getProperty("java.io.tmpdir") + "/volumes";
    @Mock
    private CommandRunner commandRunner;
    private String ddCommand = "%s and %d";
    @Mock
    private SerialExecutor serialExecutor;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    private String nodeId = "99001122";
    @Mock
    private PId volumePastryId;
    @Mock
    private PId createVolumeQueueId;
    private Volume volume = new Volume();
    private String volumeId = "abc123";
    private Integer size = 2;
    private Thread thread;
    private String snapshotId = "snap-12345678";
    private String copyCommand = "cp %s %s";
    private String snapshotsDir = System.getProperty("java.io.tmpdir") + "/snapshots";;

    @Before
    public void setUp() throws Exception {
        new File(this.tmpdir).mkdirs();
        this.createHandler.setNfsVolumesDirectory(tmpdir);
        this.createHandler.setDdCommand(this.ddCommand);
        this.createHandler.setCreateVolumeCopyCommand(this.copyCommand);
        this.createHandler.setSnapshotFolder(snapshotsDir);

        when(piIdBuilder.getPId(volumeId)).thenReturn(volumePastryId);
        when(piIdBuilder.getPId(volume)).thenReturn(volumePastryId);
        when(volumePastryId.forGlobalAvailablityZoneCode(anyInt())).thenReturn(volumePastryId);
        when(piIdBuilder.getPiQueuePId(PiQueue.CREATE_VOLUME)).thenReturn(createVolumeQueueId);
        when(createVolumeQueueId.forLocalScope(PiQueue.CREATE_VOLUME.getNodeScope())).thenReturn(createVolumeQueueId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                thread = new Thread(r);
                thread.start();
                return null;
            }
        }).when(serialExecutor).execute(isA(Runnable.class));

        when(this.dhtClientFactory.createWriter()).thenReturn(this.dhtWriter);
        volume.setVolumeId(volumeId);
        volume.setSizeInGigaBytes(size);
        volume.setStatus(VolumeState.CREATING);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TaskProcessingQueueContinuation continuation = (TaskProcessingQueueContinuation) invocation.getArguments()[3];
                continuation.receiveResult(volume.getUrl(), nodeId);
                return null;
            }
        }).when(this.taskProcessingQueueHelper).setNodeIdOnUrl(eq(createVolumeQueueId), eq(volume.getUrl()), eq(nodeId), isA(TaskProcessingQueueContinuation.class));
    }

    @After
    public void after() throws Exception {
        FileUtils.deleteQuietly(new File(this.tmpdir));
    }

    @Test
    public void testAfterPropertiesSet() throws Exception {
        // setup
        FileUtils.deleteQuietly(new File(this.tmpdir));
        TestLoggingAppender loggingAppender = new TestLoggingAppender();

        // act
        this.createHandler.afterPropertiesSet();

        // assert
        assertTrue(loggingAppender.getMessages().contains(String.format("configured path %s does not exist", this.tmpdir)));
    }

    public class TestLoggingAppender extends org.apache.log4j.AppenderSkeleton {
        private List<String> messages = new ArrayList<String>();

        public TestLoggingAppender() {
            Logger logger = Logger.getLogger(com.bt.pi.app.volumemanager.handlers.AbstractHandler.class);
            logger.addAppender(this);
        }

        public List<String> getMessages() {
            return this.messages;
        }

        @Override
        protected void append(LoggingEvent loggingEvent) {
            this.messages.add(loggingEvent.getMessage().toString());
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

    @Test
    public void testAfterPropertiesSetDirAlreadyExists() throws Exception {
        // setup

        // act
        this.createHandler.afterPropertiesSet();

        // assert
        assertTrue(new File(this.tmpdir).exists());
        assertTrue(new File(this.tmpdir).isDirectory());
    }

    public void testAfterPropertiesSetDirAlreadyExistsNotDir() throws Exception {
        // setup
        FileUtils.deleteQuietly(new File(this.tmpdir));
        FileUtils.writeStringToFile(new File(this.tmpdir), "blah");
        TestLoggingAppender loggingAppender = new TestLoggingAppender();

        // act
        this.createHandler.afterPropertiesSet();

        // assert
        assertTrue(loggingAppender.getMessages().contains(String.format("configured path %s exists but is not a directory", this.tmpdir)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateVolume() throws Exception {
        // setup
        this.createHandler.afterPropertiesSet();

        CommandResult commandResult = mock(CommandResult.class);
        when(this.commandRunner.runNicely(isA(String.class))).thenReturn(commandResult);

        final CountDownLatch latch = new CountDownLatch(2);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("rawtypes")
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(volume, volume);
                if (latch.getCount() == 2)
                    assertEquals(VolumeState.CREATING, volume.getStatus());
                continuation.handleResult(volume);
                latch.countDown();
                return null;
            }
        }).when(this.dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        // act
        boolean result = this.createHandler.createVolume(volume, nodeId);

        // assert
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        thread.join(2000);
        assertTrue(result);
        assertEquals(VolumeState.AVAILABLE, volume.getStatus());
        String expectedDDCommand = String.format(this.ddCommand, this.tmpdir + "/" + volumeId, size * 1024);
        verify(this.commandRunner).runNicely(expectedDDCommand);
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(eq(createVolumeQueueId), eq(volume.getUrl()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateVolumeUsingSnapshot() throws Exception {
        // setup
        this.createHandler.afterPropertiesSet();
        volume.setSnapshotId(snapshotId);

        CommandResult commandResult = mock(CommandResult.class);
        when(this.commandRunner.runNicely(isA(String.class))).thenReturn(commandResult);

        final CountDownLatch latch = new CountDownLatch(2);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("rawtypes")
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(volume, volume);
                if (latch.getCount() == 2)
                    assertEquals(VolumeState.CREATING, volume.getStatus());
                continuation.handleResult(volume);
                latch.countDown();
                return null;
            }
        }).when(this.dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        // act
        boolean result = this.createHandler.createVolume(volume, nodeId);

        // assert
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        thread.join(2000);
        assertTrue(result);
        assertEquals(VolumeState.AVAILABLE, volume.getStatus());
        String expectedDDCommand = String.format(this.copyCommand, this.snapshotsDir + "/" + snapshotId, this.tmpdir + "/" + volumeId);
        verify(this.commandRunner).runNicely(expectedDDCommand);
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(eq(createVolumeQueueId), eq(volume.getUrl()));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testCreateVolumeNotInPendingState() throws Exception {
        // setup
        volume.setStatus(VolumeState.IN_USE);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TaskProcessingQueueContinuation continuation = (TaskProcessingQueueContinuation) invocation.getArguments()[3];
                continuation.receiveResult(volume.getUrl(), nodeId);
                return null;
            }
        }).when(this.taskProcessingQueueHelper).setNodeIdOnUrl(eq(createVolumeQueueId), eq(volume.getUrl()), eq(nodeId), isA(TaskProcessingQueueContinuation.class));

        final AtomicInteger continuationCount = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("rawtypes")
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                latch.countDown();
                continuation.update(volume, volume);
                continuationCount.incrementAndGet(); // should never reach here
                continuation.handleResult(volume);
                return null;
            }
        }).when(this.dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        // act
        try {
            this.createHandler.createVolume(volume, nodeId);
        } catch (IllegalArgumentException e) {
            assertEquals(0, createHandler.getCurrentVolumeCreationCount());
            throw e;
        }
    }
}
