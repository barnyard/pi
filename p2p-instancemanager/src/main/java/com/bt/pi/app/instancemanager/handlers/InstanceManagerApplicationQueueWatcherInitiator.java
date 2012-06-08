package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

@Component
public class InstanceManagerApplicationQueueWatcherInitiator {
    private RunInstanceWatcherManager runInstanceWatcherManager;
    private TerminateInstanceTaskQueueWatcherInitiator terminateInstanceTaskQueueWatcherInitiator;
    private PauseInstanceTaskQueueWatcherInitiator pauseInstanceTaskQueueWatcherInitiator;
    private RemoveInstanceFromUserTaskQueueWatcherInitiator removeInstanceFromUserTaskQueueWatcherInitiator;

    public InstanceManagerApplicationQueueWatcherInitiator() {
        this.runInstanceWatcherManager = null;
        this.terminateInstanceTaskQueueWatcherInitiator = null;
        this.pauseInstanceTaskQueueWatcherInitiator = null;
        this.removeInstanceFromUserTaskQueueWatcherInitiator = null;
    }

    @Resource
    public void setInstanceManagerApplicationWatcherManager(RunInstanceWatcherManager aInstanceManagerApplicationWatcherManager) {
        this.runInstanceWatcherManager = aInstanceManagerApplicationWatcherManager;
    }

    @Resource
    public void setTerminateInstanceTaskQueueWatcherInitiator(TerminateInstanceTaskQueueWatcherInitiator aTerminateInstanceTaskQueueWatcherInitiator) {
        terminateInstanceTaskQueueWatcherInitiator = aTerminateInstanceTaskQueueWatcherInitiator;
    }

    @Resource
    public void setPauseInstanceTaskQueueWatcherInitiator(PauseInstanceTaskQueueWatcherInitiator aPauseInstanceTaskQueueWatcherInitiator) {
        pauseInstanceTaskQueueWatcherInitiator = aPauseInstanceTaskQueueWatcherInitiator;
    }

    @Resource
    public void setRemoveInstanceFromUserTaskQueueWatcherInitiator(RemoveInstanceFromUserTaskQueueWatcherInitiator aRemoveInstanceFromUserTaskQueueWatcherInitiator) {
        removeInstanceFromUserTaskQueueWatcherInitiator = aRemoveInstanceFromUserTaskQueueWatcherInitiator;
    }

    public void initialiseWatchers(String nodeIdFull) {
        runInstanceWatcherManager.createTaskProcessingQueueWatcher(nodeIdFull);
        terminateInstanceTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeIdFull);
        pauseInstanceTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeIdFull);
        removeInstanceFromUserTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(nodeIdFull);
    }

    public void removeWatchers() {
        runInstanceWatcherManager.removeTaskProcessingQueueWatcher();
        terminateInstanceTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
        pauseInstanceTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
        removeInstanceFromUserTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
    }
}
