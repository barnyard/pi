package com.bt.pi.app.networkmanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = AssociateAddressTaskQueueWatcherInitiator.TWO_MINUTES, initialQueueWatcherIntervalMillis = AssociateAddressTaskQueueWatcherInitiator.TWO_MINUTES, repeatingQueueWatcherIntervalMillis = AssociateAddressTaskQueueWatcherInitiator.TWO_MINUTES, staleQueueItemMillisProperty = "stale.associate.address.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.associate.address.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.associate.address.queue.watcher.interval.millis")
@Component
public class AssociateAddressTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {
    public static final String ASSOCIATE_ADDRESS_QUEUE_WATCHER_NAME = "ASSOCIATE_ADDRESS_QUEUE_WATCHER";
    public static final int TWO_MINUTES = 120000;

    public AssociateAddressTaskQueueWatcherInitiator() {
        super(ASSOCIATE_ADDRESS_QUEUE_WATCHER_NAME, PiQueue.ASSOCIATE_ADDRESS.getPiLocation());
    }

    @Resource
    public void setAssociateAddressTaskProcessingQueueContinuation(AssociateAddressTaskProcessingQueueContinuation associateAddressTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(associateAddressTaskProcessingQueueContinuation);
    }
}
