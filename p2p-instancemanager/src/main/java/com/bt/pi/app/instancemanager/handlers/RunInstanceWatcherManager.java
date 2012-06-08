package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = RunInstanceWatcherManager.THIRTY_MINS, initialQueueWatcherIntervalMillis = RunInstanceWatcherManager.TWO_HUNDRED_SEVENTY_SECS, repeatingQueueWatcherIntervalMillis = RunInstanceWatcherManager.TWO_HUNDRED_SEVENTY_SECS, staleQueueItemMillisProperty = "stale.run.instance.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.run.instance.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.run.instance.queue.watcher.interval.millis")
@Component
public class RunInstanceWatcherManager extends TaskProcessingQueueWatcherInitiatorBase {
    protected static final String RUN_INSTANCE_QUEUE_WATCHER = "RUN_INSTANCE_QUEUE_WATCHER";
    protected static final int THIRTY_MINS = 30 * 60 * 1000;
    protected static final int TWO_HUNDRED_SEVENTY_SECS = 270 * 1000;

    public RunInstanceWatcherManager() {
        super(RUN_INSTANCE_QUEUE_WATCHER, PiQueue.RUN_INSTANCE.getPiLocation());
    }

    @Resource
    public void setRunInstanceTaskProcessingQueueContinuation(RunInstanceTaskProcessingQueueContinuation runInstanceTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(runInstanceTaskProcessingQueueContinuation);
    }

    @Resource
    public void setRunInstanceTaskProcessingExhaustedInstanceContinuation(RunInstanceTaskProcessingExhaustedInstanceContinuation runInstanceTaskProcessingExhaustedInstanceContinuation) {
        setTaskProcessingQueueRetriesExhaustedContinuation(runInstanceTaskProcessingExhaustedInstanceContinuation);
    }

    @Override
    public void createTaskProcessingQueueWatcher(String nodeId) {
        super.createTaskProcessingQueueWatcher(nodeId);
    }
}
