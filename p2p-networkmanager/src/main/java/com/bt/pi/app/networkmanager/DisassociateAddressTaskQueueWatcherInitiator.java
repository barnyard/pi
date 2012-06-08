package com.bt.pi.app.networkmanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = DisassociateAddressTaskQueueWatcherInitiator.TWO_MINUTES, initialQueueWatcherIntervalMillis = DisassociateAddressTaskQueueWatcherInitiator.TWO_MINUTES, repeatingQueueWatcherIntervalMillis = DisassociateAddressTaskQueueWatcherInitiator.TWO_MINUTES, staleQueueItemMillisProperty = "stale.associate.address.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.associate.address.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.associate.address.queue.watcher.interval.millis")
@Component
public class DisassociateAddressTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {
    public static final String DISASSOCIATE_ADDRESS_QUEUE_WATCHER_NAME = "DISASSOCIATE_ADDRESS_QUEUE_WATCHER";
    public static final int TWO_MINUTES = 120000;

    public DisassociateAddressTaskQueueWatcherInitiator() {
        super(DISASSOCIATE_ADDRESS_QUEUE_WATCHER_NAME, PiQueue.DISASSOCIATE_ADDRESS.getPiLocation());
    }

    @Resource
    public void setDisassociateAddressTaskProcessingQueueContinuation(DisassociateAddressTaskProcessingQueueContinuation disassociateAddressTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(disassociateAddressTaskProcessingQueueContinuation);
    }
}
