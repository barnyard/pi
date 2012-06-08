package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = RemoveSnapshotFromUserTaskQueueWatcherInitiator.BURIED_TIME, initialQueueWatcherIntervalMillis = RemoveSnapshotFromUserTaskQueueWatcherInitiator.FIVE_MINUTES, repeatingQueueWatcherIntervalMillis = RemoveSnapshotFromUserTaskQueueWatcherInitiator.FIVE_MINUTES, staleQueueItemMillisProperty = "stale.remove.snapshot.from.user.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.remove.snapshot.from.user.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.remove.snapshot.from.user.queue.watcher.interval.millis")
@Component
public class RemoveSnapshotFromUserTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {
    public static final String REMOVE_SNAPSHOT_FROM_USER_QUEUE_WATCHER_NAME = "REMOVE_SNAPSHOT_FROM_USER_QUEUE_WATCHER";
    public static final int FIVE_MINUTES = 5 * 60 * 1000;
    public static final int BURIED_TIME = Snapshot.BURIED_TIME;

    public RemoveSnapshotFromUserTaskQueueWatcherInitiator() {
        super(REMOVE_SNAPSHOT_FROM_USER_QUEUE_WATCHER_NAME, PiQueue.REMOVE_SNAPSHOT_FROM_USER.getPiLocation());
    }

    @Resource
    public void setRemoveSnapshotFromUserTaskProcessingQueueContinuation(RemoveSnapshotFromUserTaskProcessingQueueContinuation removeSnapshotFromUserTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(removeSnapshotFromUserTaskProcessingQueueContinuation);
    }
}
