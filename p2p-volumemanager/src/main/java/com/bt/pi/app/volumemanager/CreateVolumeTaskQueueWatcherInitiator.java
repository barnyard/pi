package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = CreateVolumeTaskQueueWatcherInitiator.THIRTY_MINUTES, initialQueueWatcherIntervalMillis = CreateVolumeTaskQueueWatcherInitiator.ONE_HUNDRED_NINETY_SECS, repeatingQueueWatcherIntervalMillis = CreateVolumeTaskQueueWatcherInitiator.ONE_HUNDRED_NINETY_SECS, staleQueueItemMillisProperty = "stale.create.volume.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.create.volume.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.create.volume.queue.watcher.interval.millis")
@Component
public class CreateVolumeTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {
    protected static final String CREATE_VOLUME_QUEUE_WATCHER = "CREATE_VOLUME_QUEUE_WATCHER";
    protected static final int ONE_HUNDRED_NINETY_SECS = 190 * 1000;
    protected static final int THIRTY_MINUTES = 30 * 60 * 1000;

    public CreateVolumeTaskQueueWatcherInitiator() {
        super(CREATE_VOLUME_QUEUE_WATCHER, PiQueue.CREATE_VOLUME.getPiLocation());
    }

    @Resource
    public void setCreateVolumeTaskProcessingQueueContinuation(CreateVolumeTaskProcessingQueueContinuation createVolumeTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(createVolumeTaskProcessingQueueContinuation);
    }

    @Resource
    public void setCreateVolumeTaskProcessingQueueRetriesExhaustedContinuation(CreateVolumeTaskProcessingQueueRetriesExhaustedContinuation createVolumeTaskProcessingQueueRetriesExhaustedContinuation) {
        setTaskProcessingQueueRetriesExhaustedContinuation(createVolumeTaskProcessingQueueRetriesExhaustedContinuation);
    }
}
