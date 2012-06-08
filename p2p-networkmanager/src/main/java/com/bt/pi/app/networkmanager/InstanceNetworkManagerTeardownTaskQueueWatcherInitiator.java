package com.bt.pi.app.networkmanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = InstanceNetworkManagerTeardownTaskQueueWatcherInitiator.ONE_MINUTE, initialQueueWatcherIntervalMillis = InstanceNetworkManagerTeardownTaskQueueWatcherInitiator.ONE_MINUTE, repeatingQueueWatcherIntervalMillis = InstanceNetworkManagerTeardownTaskQueueWatcherInitiator.ONE_MINUTE, staleQueueItemMillisProperty = "stale.security.group.delete.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.security.group.delete.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.security.group.delete.queue.watcher.interval.millis")
@Component
public class InstanceNetworkManagerTeardownTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {
    public static final String INSTANCE_NETWORK_MANAGER_TEARDOWN_QUEUE_WATCHER_NAME = "INSTANCE_NETWORK_MANAGER_TEARDOWN_QUEUE_WATCHER";
    public static final int ONE_MINUTE = 60 * 1000;

    public InstanceNetworkManagerTeardownTaskQueueWatcherInitiator() {
        super(INSTANCE_NETWORK_MANAGER_TEARDOWN_QUEUE_WATCHER_NAME, PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN.getPiLocation());
    }

    @Resource
    public void setInstanceNetworkManagerTeardownTaskProcessingQueueContinuation(InstanceNetworkManagerTeardownTaskProcessingQueueContinuation instanceNetworkManagerTeardownTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(instanceNetworkManagerTeardownTaskProcessingQueueContinuation);
    }
}