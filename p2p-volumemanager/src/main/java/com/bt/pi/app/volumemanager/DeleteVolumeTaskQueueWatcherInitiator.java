package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = DeleteVolumeTaskQueueWatcherInitiator.THIRTY_MINUTES, initialQueueWatcherIntervalMillis = DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS, repeatingQueueWatcherIntervalMillis = DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS, staleQueueItemMillisProperty = "stale.delete.volume.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.delete.volume.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.delete.volume.queue.watcher.interval.millis")
@Component
public class DeleteVolumeTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {

    public static final String DELETE_VOLUME_QUEUE_WATCHER_NAME = "DELETE_VOLUME_QUEUE_WATCHER";
    public static final int TWO_HUNDRED_SECS = 200 * 1000;
    public static final int THIRTY_MINUTES = 30 * 60 * 1000;

    public DeleteVolumeTaskQueueWatcherInitiator() {
        super(DELETE_VOLUME_QUEUE_WATCHER_NAME, PiQueue.DELETE_VOLUME.getPiLocation());
    }

    @Resource
    public void setDeleteVolumeTaskProcessingQueueContinuation(DeleteVolumeTaskProcessingQueueContinuation aDeleteVolumeTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(aDeleteVolumeTaskProcessingQueueContinuation);
    }

    @Resource
    public void setDeleteVolumeTaskProcessingQueueRetriesExhaustedContinuation(DeleteVolumeTaskProcessingQueueRetriesExhaustedContinuation continuation) {
        setTaskProcessingQueueRetriesExhaustedContinuation(continuation);
    }
}
