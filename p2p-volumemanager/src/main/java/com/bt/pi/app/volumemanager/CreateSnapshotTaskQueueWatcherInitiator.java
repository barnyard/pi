package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillisProperty = "stale.create.snapshot.queue.item.millis", staleQueueItemMillis = CreateSnapshotTaskQueueWatcherInitiator.ONE_HOUR, initialQueueWatcherIntervalMillis = CreateSnapshotTaskQueueWatcherInitiator.TWO_HUNDRED_THIRTY_SECS, repeatingQueueWatcherIntervalMillis = CreateSnapshotTaskQueueWatcherInitiator.TWO_HUNDRED_THIRTY_SECS, initialQueueWatcherIntervalMillisProperty = "initial.create.snapshot.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.create.snapshot.queue.watcher.interval.millis")
@Component
public class CreateSnapshotTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {
    protected static final String CREATE_SNAPSHOT_QUEUE_WATCHER = "CREATE_SNAPSHOT_QUEUE_WATCHER";
    protected static final int ONE_HOUR = 60 * 60 * 1000;
    protected static final int TWO_HUNDRED_THIRTY_SECS = 230 * 1000;

    public CreateSnapshotTaskQueueWatcherInitiator() {
        super(CREATE_SNAPSHOT_QUEUE_WATCHER, PiQueue.CREATE_SNAPSHOT.getPiLocation());
    }

    @Resource
    public void setCreateSnapshotTaskProcessingQueueContinuation(CreateSnapshotTaskProcessingQueueContinuation createSnapshotTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(createSnapshotTaskProcessingQueueContinuation);
    }

    @Resource
    public void setCreateSnapshotTaskProcessingQueueRetriesExhaustedContinuation(CreateSnapshotTaskProcessingQueueRetriesExhaustedContinuation createSnapshotTaskProcessingQueueRetriesExhaustedContinuation) {
        setTaskProcessingQueueRetriesExhaustedContinuation(createSnapshotTaskProcessingQueueRetriesExhaustedContinuation);
    }
}
