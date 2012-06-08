package com.bt.pi.app.volumemanager.handlers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.app.volumemanager.handlers.AbstractHandler.SnapshotStatusUpdateResolvingPiContinuation;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.util.SerialExecutor;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateSnapshotHandlerTest {
    private static final String NTFS_VOLS_FOLDER = "var";
    private static final String SNAPSHOTS_FOLDER = "var/snapshots";
    private static final String RSYNC_COMMAND = "cp %s %s";
    @InjectMocks
    private CreateSnapshotHandler createSnapshotHandler = new CreateSnapshotHandler();
    private String nodeId = "12345678";
    @Mock
    private MessageContext receivedMessageContext;
    @Mock
    private Snapshot snapshot;
    private String volumeId = "vol-12345678";
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PiIdBuilder piIdBuilder;
    private String snapshotId = "snap-12345678";
    @Mock
    private PId piQueueId;
    @Mock
    private Volume volume;
    @Mock
    private PId piVolumeId;
    @Mock
    private PId piSnapshotId;
    private int globalAvailabilityZoneCode = 22;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter writer;
    @Mock
    private DhtReader reader;
    @Mock
    private CommandRunner commandRunner;
    private CountDownLatch volumeUpdateLatch = new CountDownLatch(1);
    private CountDownLatch snapshotUpdateLatch = new CountDownLatch(1);
    @Mock
    private SerialExecutor serialExecutor;
    private Thread thread;
    private String instanceId = "i-12345678";
    @Mock
    private PId piInstanceId;
    @Mock
    private Instance instance;
    private String instanceManagerNodeId = "898989898998989";
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private PId piInstanceManagerId;
    private CountDownLatch instanceReadLatch;
    private CountDownLatch continuationLatch = new CountDownLatch(1);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void before() {
        createSnapshotHandler.setRsyncCommand(RSYNC_COMMAND);
        createSnapshotHandler.setSnapshotFolder(SNAPSHOTS_FOLDER);
        createSnapshotHandler.setNfsVolumesDirectory(NTFS_VOLS_FOLDER);

        when(snapshot.getVolumeId()).thenReturn(volumeId);
        when(snapshot.getUrl()).thenReturn(Snapshot.getUrl(snapshotId));
        when(snapshot.getSnapshotId()).thenReturn(snapshotId);
        when(snapshot.getStatus()).thenReturn(SnapshotState.PENDING);
        when(piIdBuilder.getPiQueuePId(isA(PiQueue.class))).thenReturn(piQueueId);
        when(piQueueId.forLocalScope(isA(NodeScope.class))).thenReturn(piQueueId);
        when(piIdBuilder.getPId(eq(Volume.getUrl(volumeId)))).thenReturn(piVolumeId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(volumeId)).thenReturn(globalAvailabilityZoneCode);
        when(piVolumeId.forGlobalAvailablityZoneCode(globalAvailabilityZoneCode)).thenReturn(piVolumeId);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        when(volume.getVolumeId()).thenReturn(volumeId);
        when(dhtClientFactory.createWriter()).thenReturn(writer);
        when(dhtClientFactory.createReader()).thenReturn(reader);
        when(piIdBuilder.getPId(snapshot)).thenReturn(piSnapshotId);
        when(piIdBuilder.getPId(eq(Snapshot.getUrl(snapshotId)))).thenReturn(piSnapshotId);
        when(piIdBuilder.getPId(eq(volume))).thenReturn(piVolumeId);
        when(piIdBuilder.getPId(isA(Volume.class))).thenReturn(piVolumeId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(snapshotId)).thenReturn(globalAvailabilityZoneCode);
        when(piSnapshotId.forGlobalAvailablityZoneCode(globalAvailabilityZoneCode)).thenReturn(piSnapshotId);
        when(instance.getNodeId()).thenReturn(instanceManagerNodeId);
        when(volume.getInstanceId()).thenReturn(instanceId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId))).thenReturn(piInstanceId);
        when(koalaIdFactory.buildPIdFromHexString(instanceManagerNodeId)).thenReturn(piInstanceManagerId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TaskProcessingQueueContinuation continuation = (TaskProcessingQueueContinuation) invocation.getArguments()[3];
                continuation.receiveResult(Snapshot.getUrl(snapshotId), nodeId);
                return null;
            }
        }).when(taskProcessingQueueHelper).setNodeIdOnUrl(eq(piQueueId), eq(Snapshot.getUrl(snapshotId)), eq(nodeId), isA(TaskProcessingQueueContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object result = continuation.update(volume, null);
                volumeUpdateLatch.countDown();
                if (null != result)
                    continuation.handleResult(volume);
                return null;
            }
        }).when(writer).update(eq(piVolumeId), isA(UpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                SnapshotStatusUpdateResolvingPiContinuation continuation = (SnapshotStatusUpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(snapshot, null);
                snapshotUpdateLatch.countDown();
                if (VolumeState.AVAILABLE.equals(volume.getStatus()))
                    when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE_SNAPSHOTTING);
                else
                    when(volume.getStatus()).thenReturn(VolumeState.IN_USE_SNAPSHOTTING);
                continuation.handleResult(snapshot);
                return null;
            }
        }).when(writer).update(eq(piSnapshotId), isA(SnapshotStatusUpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                thread = new Thread(r);
                thread.start();
                return null;
            }
        }).when(serialExecutor).execute(isA(Runnable.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[1];
                continuation.handleResult(instance);
                instanceReadLatch.countDown();
                return null;
            }
        }).when(reader).getAsync(eq(piInstanceId), isA(PiContinuation.class));

        instanceReadLatch = new CountDownLatch(0);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation<Snapshot> continuation = (PiContinuation<Snapshot>) invocation.getArguments()[4];
                when(snapshot.getStatus()).thenReturn(SnapshotState.COMPLETE);
                continuation.handleResult(snapshot);
                continuationLatch.countDown();
                return null;
            }
        }).when(receivedMessageContext).routePiMessageToApplication(eq(piInstanceManagerId), eq(EntityMethod.CREATE), eq(snapshot), eq(InstanceManagerApplication.APPLICATION_NAME), isA(PiContinuation.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSnapshotShouldThrowIfNoVolumeId() {
        // setup
        when(snapshot.getVolumeId()).thenReturn(null);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSnapshotShouldThrowIfNoSnapshotId() {
        // setup
        when(snapshot.getSnapshotId()).thenReturn(null);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSnapshotShouldThrowIfNotPending() {
        // setup
        when(snapshot.getStatus()).thenReturn(SnapshotState.CREATING);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);
    }

    @Test
    public void createSnapshotShouldMarkTaskOnQueue() throws Exception {
        // setup

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(eq(piQueueId), eq(Snapshot.getUrl(snapshotId)), eq(nodeId), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void createSnapshotShouldNotContinueWhenVolumeNotInUseOrAvailable() throws Exception {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.ATTACHING);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(volumeUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        verify(volume, never()).setStatus(isA(VolumeState.class));
    }

    @Test
    public void createSnapshotShouldSetVolumeStateWhenInuse() throws InterruptedException {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(volumeUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        verify(volume).setStatus(eq(VolumeState.IN_USE_SNAPSHOTTING));
    }

    @Test
    public void createSnapshotShouldSetVolumeStateWhenAvailable() throws InterruptedException {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(volumeUpdateLatch.await(100, TimeUnit.MILLISECONDS));
        verify(volume).setStatus(eq(VolumeState.AVAILABLE_SNAPSHOTTING));
    }

    @Test
    public void createSnapshotShouldSetsnapshotStateToCreating() throws InterruptedException {
        // setup

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(snapshotUpdateLatch.await(100, TimeUnit.MILLISECONDS));
        verify(snapshot).setStatus(SnapshotState.CREATING);
    }

    @Test
    public void createSnapshotShouldCallCommandRunnerWhenVolumeStateIsAvailable() throws Exception {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(snapshotUpdateLatch.await(100, TimeUnit.MILLISECONDS));
        thread.join(1000);
        verify(commandRunner).runNicely(String.format(RSYNC_COMMAND, NTFS_VOLS_FOLDER + "/" + volumeId, SNAPSHOTS_FOLDER + "/" + snapshotId));
    }

    @Test
    public void createSnapshotShouldSetSnapshotStateToErrorIfRsyncCommandNotSuccessful() throws Exception {
        // setup
        snapshotUpdateLatch = new CountDownLatch(2);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);
        doThrow(new CommandExecutionException("")).when(commandRunner).runNicely(isA(String.class));

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(snapshotUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        thread.join(1000);
        verify(snapshot).setStatus(SnapshotState.CREATING);
        verify(snapshot).setStatus(SnapshotState.ERROR);
    }

    @Test
    public void createSnapshotShouldSetSnapshotStateToCompletedWhenSuccess() throws Exception {
        // setup
        snapshotUpdateLatch = new CountDownLatch(2);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(snapshotUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        thread.join(1000);
        verify(snapshot).setStatus(SnapshotState.CREATING);
        verify(snapshot).setStatus(SnapshotState.COMPLETE);
        verify(snapshot).setProgress(eq(100.0));
    }

    @Test
    public void createSnapshotShouldSetVolumeStatusBackToAvailableIfSuccessful() throws Exception {
        // setup
        snapshotUpdateLatch = new CountDownLatch(2);
        volumeUpdateLatch = new CountDownLatch(2);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(snapshotUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(volumeUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        thread.join(1000);
        verify(volume).setStatus(VolumeState.AVAILABLE_SNAPSHOTTING);
        verify(volume).setStatus(VolumeState.AVAILABLE);
    }

    @Test
    public void createSnapshotShouldRemoveTaskFromQueueIfSuccess() throws Exception {
        // setup
        snapshotUpdateLatch = new CountDownLatch(2);
        volumeUpdateLatch = new CountDownLatch(2);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(snapshotUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(volumeUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        thread.join(1000);
        verify(taskProcessingQueueHelper).removeUrlFromQueue(piQueueId, Snapshot.getUrl(snapshotId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createSnapshotShouldSendMessageToInstanceManagerWhenVolumeStateIsInUse() throws Exception {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        instanceReadLatch = new CountDownLatch(1);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(snapshotUpdateLatch.await(100, TimeUnit.MILLISECONDS));
        assertTrue(volumeUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(instanceReadLatch.await(200, TimeUnit.MILLISECONDS));
        verify(receivedMessageContext).routePiMessageToApplication(eq(piInstanceManagerId), eq(EntityMethod.CREATE), eq(snapshot), eq(InstanceManagerApplication.APPLICATION_NAME), isA(PiContinuation.class));
    }

    @Test
    public void createSnapshotShouldSetSnapshotStatusToCompleteOnReplyFromInstanceManager() throws Exception {
        // setup
        snapshotUpdateLatch = new CountDownLatch(2);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        instanceReadLatch = new CountDownLatch(1);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(snapshotUpdateLatch.await(100, TimeUnit.MILLISECONDS));
        assertTrue(volumeUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(instanceReadLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(continuationLatch.await(200, TimeUnit.MILLISECONDS));
        verify(snapshot).setStatus(SnapshotState.CREATING);
        verify(snapshot).setStatus(SnapshotState.COMPLETE);
        verify(snapshot).setProgress(eq(100.0));
    }

    @Test
    public void createSnapshotShouldSetVolumeStatusToInUseOnReplyFromInstanceManager() throws Exception {
        // setup
        snapshotUpdateLatch = new CountDownLatch(2);
        volumeUpdateLatch = new CountDownLatch(2);
        instanceReadLatch = new CountDownLatch(1);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(snapshotUpdateLatch.await(100, TimeUnit.MILLISECONDS));
        assertTrue(volumeUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(instanceReadLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(continuationLatch.await(200, TimeUnit.MILLISECONDS));
        verify(volume).setStatus(VolumeState.IN_USE_SNAPSHOTTING);
        verify(volume).setStatus(VolumeState.IN_USE);
    }

    @Test
    public void createSnapshotShouldRemoveSnapshotFromQueueOnReplyFromInstanceManager() throws Exception {
        // setup
        snapshotUpdateLatch = new CountDownLatch(2);
        volumeUpdateLatch = new CountDownLatch(2);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        instanceReadLatch = new CountDownLatch(1);

        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);

        // assert
        assertTrue(snapshotUpdateLatch.await(100, TimeUnit.MILLISECONDS));
        assertTrue(volumeUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(instanceReadLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(continuationLatch.await(200, TimeUnit.MILLISECONDS));
        verify(taskProcessingQueueHelper).removeUrlFromQueue(piQueueId, Snapshot.getUrl(snapshotId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createSnapshotShouldNotModifyDhtIfInstanceManagerIsNotSuccessful() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation<Snapshot> continuation = (PiContinuation<Snapshot>) invocation.getArguments()[4];
                when(snapshot.getStatus()).thenReturn(SnapshotState.ERROR);
                continuation.handleResult(snapshot);
                continuationLatch.countDown();
                return null;
            }
        }).when(receivedMessageContext).routePiMessageToApplication(eq(piInstanceManagerId), eq(EntityMethod.CREATE), eq(snapshot), eq(InstanceManagerApplication.APPLICATION_NAME), isA(PiContinuation.class));
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        instanceReadLatch = new CountDownLatch(1);
        // act
        this.createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext, nodeId);
        // assert
        assertTrue(snapshotUpdateLatch.await(100, TimeUnit.MILLISECONDS));
        assertTrue(volumeUpdateLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(instanceReadLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(continuationLatch.await(200, TimeUnit.MILLISECONDS));
        verify(snapshot, never()).setStatus(SnapshotState.COMPLETE);
        verify(volume, never()).setStatus(VolumeState.IN_USE);
        verify(taskProcessingQueueHelper, never()).removeUrlFromQueue(piQueueId, Snapshot.getUrl(snapshotId));
    }
}