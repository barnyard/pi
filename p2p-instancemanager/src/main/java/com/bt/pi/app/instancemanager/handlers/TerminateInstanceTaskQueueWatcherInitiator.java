package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = TerminateInstanceTaskQueueWatcherInitiator.THIRTY_MINUTE, initialQueueWatcherIntervalMillis = TerminateInstanceTaskQueueWatcherInitiator.THREE_AND_HALF_MINS, repeatingQueueWatcherIntervalMillis = TerminateInstanceTaskQueueWatcherInitiator.THREE_AND_HALF_MINS, staleQueueItemMillisProperty = "stale.terminate.instance.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.terminate.instance.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.terminate.instance.queue.watcher.interval.millis")
@Component
public class TerminateInstanceTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {

    public static final String TERMINATE_INSTANCE_QUEUE_WATCHER_NAME = "TERMINATE_INSTANCE_QUEUE_WATCHER";
    public static final int THIRTY_MINUTE = 30 * 60 * 1000;
    public static final int THREE_AND_HALF_MINS = 210 * 1000;

    public TerminateInstanceTaskQueueWatcherInitiator() {
        super(TERMINATE_INSTANCE_QUEUE_WATCHER_NAME, PiQueue.TERMINATE_INSTANCE.getPiLocation());
    }

    @Resource
    public void setTerminateInstanceTaskProcessingQueueContinuation(TerminateInstanceTaskProcessingQueueContinuation aTerminateInstanceTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(aTerminateInstanceTaskProcessingQueueContinuation);
    }
}
