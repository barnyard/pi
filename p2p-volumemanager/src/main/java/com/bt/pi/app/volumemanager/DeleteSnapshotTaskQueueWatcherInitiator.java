package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = DeleteSnapshotTaskQueueWatcherInitiator.FIFTEEN_MINUTES, initialQueueWatcherIntervalMillis = DeleteSnapshotTaskQueueWatcherInitiator.TWO_HUNDRED_FIFTY_SECS, repeatingQueueWatcherIntervalMillis = DeleteSnapshotTaskQueueWatcherInitiator.TWO_HUNDRED_FIFTY_SECS, staleQueueItemMillisProperty = "stale.delete.snapshot.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.delete.snapshot.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.delete.snapshot.queue.watcher.interval.millis")
@Component
public class DeleteSnapshotTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {

    public static final String DELETE_SNAPSHOT_QUEUE_WATCHER_NAME = "DELETE_SNAPSHOT_QUEUE_WATCHER";
    public static final int TWO_HUNDRED_FIFTY_SECS = 250 * 1000;
    public static final int FIFTEEN_MINUTES = 15 * 60 * 1000;

    public DeleteSnapshotTaskQueueWatcherInitiator() {
        super(DELETE_SNAPSHOT_QUEUE_WATCHER_NAME, PiQueue.DELETE_SNAPSHOT.getPiLocation());
    }

    @Resource
    public void setDeleteSnapshotTaskProcessingQueueContinuation(DeleteSnapshotTaskProcessingQueueContinuation aDeleteSnapshotTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(aDeleteSnapshotTaskProcessingQueueContinuation);
    }

    @Resource
    public void setDeleteSnapshotTaskProcessingQueueRetriesExhaustedContinuation(DeleteSnapshotTaskProcessingQueueRetriesExhaustedContinuation continuation) {
        setTaskProcessingQueueRetriesExhaustedContinuation(continuation);
    }
}
