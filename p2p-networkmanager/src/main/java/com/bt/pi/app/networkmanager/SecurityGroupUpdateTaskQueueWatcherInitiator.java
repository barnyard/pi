package com.bt.pi.app.networkmanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = SecurityGroupUpdateTaskQueueWatcherInitiator.NINETY_SECONDS, initialQueueWatcherIntervalMillis = SecurityGroupUpdateTaskQueueWatcherInitiator.NINETY_SECONDS, repeatingQueueWatcherIntervalMillis = SecurityGroupUpdateTaskQueueWatcherInitiator.NINETY_SECONDS, staleQueueItemMillisProperty = "stale.security.group.update.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.security.group.update.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.security.group.update.queue.watcher.interval.millis")
@Component
public class SecurityGroupUpdateTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {

    public static final String SECURITY_GROUP_UPDATE_QUEUE_WATCHER_NAME = "SECURITY_GROUP_UPDATE_QUEUE_WATCHER";
    public static final int NINETY_SECONDS = 90 * 1000;

    public SecurityGroupUpdateTaskQueueWatcherInitiator() {
        super(SECURITY_GROUP_UPDATE_QUEUE_WATCHER_NAME, PiQueue.UPDATE_SECURITY_GROUP.getPiLocation());
    }

    @Resource
    public void setSecurityGroupUpdateTaskProcessingQueueContinuation(SecurityGroupUpdateTaskProcessingQueueContinuation securityGroupUpdateTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(securityGroupUpdateTaskProcessingQueueContinuation);
    }
}
