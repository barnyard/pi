package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = RemoveInstanceFromUserTaskQueueWatcherInitiator.ONE_HOUR, initialQueueWatcherIntervalMillis = RemoveInstanceFromUserTaskQueueWatcherInitiator.ONE_HUNDRED_THIRTY_FIVE_SECS, repeatingQueueWatcherIntervalMillis = RemoveInstanceFromUserTaskQueueWatcherInitiator.ONE_HUNDRED_THIRTY_FIVE_SECS, staleQueueItemMillisProperty = "stale.remove.instance.from.user.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.remove.instance.from.user.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.remove.instance.from.user.queue.watcher.interval.millis")
@Component
public class RemoveInstanceFromUserTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {
    public static final String REMOVE_INSTANCE_FROM_USER_QUEUE_WATCHER_NAME = "REMOVE_INSTANCE_FROM_USER_QUEUE_WATCHER";
    public static final int ONE_HOUR = 60 * 60 * 1000;
    public static final int ONE_HUNDRED_THIRTY_FIVE_SECS = 135 * 1000;

    public RemoveInstanceFromUserTaskQueueWatcherInitiator() {
        super(REMOVE_INSTANCE_FROM_USER_QUEUE_WATCHER_NAME, PiQueue.REMOVE_INSTANCE_FROM_USER.getPiLocation());
    }

    @Resource
    public void setRemoveInstanceFromUserTaskProcessingQueueContinuation(RemoveInstanceFromUserTaskProcessingQueueContinuation removeSnapshotFromUserTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(removeSnapshotFromUserTaskProcessingQueueContinuation);
    }
}
