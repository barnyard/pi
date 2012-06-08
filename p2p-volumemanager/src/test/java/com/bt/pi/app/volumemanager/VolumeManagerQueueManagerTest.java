package com.bt.pi.app.volumemanager;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VolumeManagerQueueManagerTest {
    private String nodeId = "nodeId";

    @Mock
    private CreateVolumeTaskQueueWatcherInitiator createVolumeTaskQueueWatcherInitiator;
    @Mock
    private AttachVolumeTaskQueueWatcherInitiator attachVolumeTaskQueueWatcherInitiator;
    @Mock
    private DetachVolumeTaskQueueWatcherInitiator detachVolumeTaskQueueWatcherInitiator;
    @Mock
    private DeleteVolumeTaskQueueWatcherInitiator deleteVolumeTaskQueueWatcherInitiator;
    @Mock
    private CreateSnapshotTaskQueueWatcherInitiator createSnapshotTaskQueueWatcherInitiator;
    @Mock
    private DeleteSnapshotTaskQueueWatcherInitiator deleteSnapshotTaskQueueWatcherInitiator;
    @Mock
    private RemoveSnapshotFromUserTaskQueueWatcherInitiator removeSnapshotFromUserTaskQueueWatcherInitiator;
    @Mock
    private RemoveVolumeFromUserTaskQueueWatcherInitiator removeVolumeFromUserTaskQueueWatcherInitiator;

    @InjectMocks
    private VolumeManagerQueueManager volumeManagerQueueManager = new VolumeManagerQueueManager();

    @Test
    public void testCreateQueueWatchers() throws Exception {
        // act
        volumeManagerQueueManager.createVolumeApplicationWatchers(nodeId);

        // assert
        verify(createVolumeTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeId);
        verify(attachVolumeTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeId);
        verify(detachVolumeTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeId);
        verify(deleteVolumeTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeId);
        verify(createSnapshotTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeId);
        verify(deleteSnapshotTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeId);
        verify(removeSnapshotFromUserTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeId);
        verify(removeVolumeFromUserTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeId);
    }

    @Test
    public void testRemoveQueueWatchers() throws Exception {
        // act
        volumeManagerQueueManager.removeVolumeApplicationWatchers();

        // assert
        verify(createVolumeTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(attachVolumeTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(detachVolumeTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(deleteVolumeTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(createSnapshotTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(deleteSnapshotTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(removeSnapshotFromUserTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(removeVolumeFromUserTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
    }
}
