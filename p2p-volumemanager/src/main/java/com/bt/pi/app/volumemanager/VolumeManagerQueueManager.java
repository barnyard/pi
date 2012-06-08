package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

@Component
public class VolumeManagerQueueManager {
    @Resource
    private CreateVolumeTaskQueueWatcherInitiator createVolumeTaskQueueWatcherInitiator;
    @Resource
    private AttachVolumeTaskQueueWatcherInitiator attachVolumeTaskQueueWatcherInitiator;
    @Resource
    private DetachVolumeTaskQueueWatcherInitiator detachVolumeTaskQueueWatcherInitiator;
    @Resource
    private DeleteVolumeTaskQueueWatcherInitiator deleteVolumeTaskQueueWatcherInitiator;
    @Resource
    private CreateSnapshotTaskQueueWatcherInitiator createSnapshotTaskQueueWatcherInitiator;
    @Resource
    private DeleteSnapshotTaskQueueWatcherInitiator deleteSnapshotTaskQueueWatcherInitiator;
    @Resource
    private RemoveSnapshotFromUserTaskQueueWatcherInitiator removeSnapshotFromUserTaskQueueWatcherInitiator;
    @Resource
    private RemoveVolumeFromUserTaskQueueWatcherInitiator removeVolumeFromUserTaskQueueWatcherInitiator;

    public VolumeManagerQueueManager() {
        createVolumeTaskQueueWatcherInitiator = null;
        attachVolumeTaskQueueWatcherInitiator = null;
        detachVolumeTaskQueueWatcherInitiator = null;
        deleteVolumeTaskQueueWatcherInitiator = null;
        createSnapshotTaskQueueWatcherInitiator = null;
        deleteSnapshotTaskQueueWatcherInitiator = null;
        removeSnapshotFromUserTaskQueueWatcherInitiator = null;
        removeVolumeFromUserTaskQueueWatcherInitiator = null;
    }

    public void createVolumeApplicationWatchers(String nodeId) {
        createVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeId);
        attachVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeId);
        detachVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeId);
        deleteVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeId);
        createSnapshotTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeId);
        deleteSnapshotTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeId);
        removeSnapshotFromUserTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeId);
        removeVolumeFromUserTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeId);
    }

    public void removeVolumeApplicationWatchers() {
        createVolumeTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
        attachVolumeTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
        detachVolumeTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
        deleteVolumeTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
        createSnapshotTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
        deleteSnapshotTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
        removeSnapshotFromUserTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
        removeVolumeFromUserTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
    }

}
