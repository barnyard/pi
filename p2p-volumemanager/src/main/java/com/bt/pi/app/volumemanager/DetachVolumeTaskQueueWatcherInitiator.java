package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = DetachVolumeTaskQueueWatcherInitiator.ONE_HOUR, initialQueueWatcherIntervalMillis = DetachVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_TWENTY_SECS, repeatingQueueWatcherIntervalMillis = DetachVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_TWENTY_SECS, staleQueueItemMillisProperty = "stale.detach.volume.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.detach.volume.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.detach.volume.queue.watcher.interval.millis")
@Component
public class DetachVolumeTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {
    protected static final String DETACH_VOLUME_QUEUE_WATCHER = "DETACH_VOLUME_QUEUE_WATCHER";
    protected static final int ONE_HOUR = 60 * 60 * 1000;
    protected static final int TWO_HUNDRED_TWENTY_SECS = 220 * 1000;

    public DetachVolumeTaskQueueWatcherInitiator() {
        super(DETACH_VOLUME_QUEUE_WATCHER, PiQueue.DETACH_VOLUME.getPiLocation());
    }

    @Resource
    public void setDetachVolumeTaskProcessingQueueContinuation(DetachVolumeTaskProcessingQueueContinuation detachVolumeTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(detachVolumeTaskProcessingQueueContinuation);
    }

    @Resource
    public void setDetachVolumeTaskProcessingQueueRetriesExhaustedContinuation(DetachVolumeTaskProcessingQueueRetriesExhaustedContinuation detachVolumeTaskProcessingQueueRetriesExhaustedContinuation) {
        setTaskProcessingQueueRetriesExhaustedContinuation(detachVolumeTaskProcessingQueueRetriesExhaustedContinuation);
    }
}
