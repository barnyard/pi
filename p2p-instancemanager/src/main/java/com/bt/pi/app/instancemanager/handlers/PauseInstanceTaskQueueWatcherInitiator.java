package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = PauseInstanceTaskQueueWatcherInitiator.FIVE_MINUTE, initialQueueWatcherIntervalMillis = PauseInstanceTaskQueueWatcherInitiator.FOUR_AND_HALF_MINS, repeatingQueueWatcherIntervalMillis = PauseInstanceTaskQueueWatcherInitiator.FOUR_AND_HALF_MINS, staleQueueItemMillisProperty = "stale.pause.instance.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.pause.instance.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.pause.instance.queue.watcher.interval.millis")
@Component
public class PauseInstanceTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {

    public static final String PAUSE_INSTANCE_QUEUE_WATCHER_NAME = "PAUSE_INSTANCE_QUEUE_WATCHER";
    public static final int FIVE_MINUTE = 5 * 60 * 1000;
    public static final int FOUR_AND_HALF_MINS = 270 * 1000;

    public PauseInstanceTaskQueueWatcherInitiator() {
        super(PAUSE_INSTANCE_QUEUE_WATCHER_NAME, PiQueue.PAUSE_INSTANCE.getPiLocation());
    }

    @Resource
    public void setPauseInstanceTaskProcessingQueueContinuation(PauseInstanceTaskProcessingQueueContinuation aPauseInstanceTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(aPauseInstanceTaskProcessingQueueContinuation);
    }
}
