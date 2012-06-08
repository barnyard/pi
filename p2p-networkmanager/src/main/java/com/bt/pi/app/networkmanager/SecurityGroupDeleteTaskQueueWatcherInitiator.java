package com.bt.pi.app.networkmanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = SecurityGroupDeleteTaskQueueWatcherInitiator.ONE_MINUTE, initialQueueWatcherIntervalMillis = SecurityGroupDeleteTaskQueueWatcherInitiator.ONE_MINUTE, repeatingQueueWatcherIntervalMillis = SecurityGroupDeleteTaskQueueWatcherInitiator.ONE_MINUTE, staleQueueItemMillisProperty = "stale.security.group.delete.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.security.group.delete.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.security.group.delete.queue.watcher.interval.millis")
@Component
public class SecurityGroupDeleteTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {

    public static final String SECURITY_GROUP_DELETE_QUEUE_WATCHER_NAME = "SECURITY_GROUP_DELETE_QUEUE_WATCHER";
    public static final int ONE_MINUTE = 60 * 1000;

    public SecurityGroupDeleteTaskQueueWatcherInitiator() {
        super(SECURITY_GROUP_DELETE_QUEUE_WATCHER_NAME, PiQueue.REMOVE_SECURITY_GROUP.getPiLocation());
    }

    @Resource
    public void setSecurityGroupDeleteTaskProcessingQueueContinuation(SecurityGroupDeleteTaskProcessingQueueContinuation securityGroupDeleteTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(securityGroupDeleteTaskProcessingQueueContinuation);
    }

}