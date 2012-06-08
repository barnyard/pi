package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillisProperty = "stale.attach.volume.queue.item.millis", staleQueueItemMillis = AttachVolumeTaskQueueWatcherInitiator.ONE_HOUR, initialQueueWatcherIntervalMillis = AttachVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_TEN_SECS, repeatingQueueWatcherIntervalMillis = AttachVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_TEN_SECS, initialQueueWatcherIntervalMillisProperty = "initial.attach.volume.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.attach.volume.queue.watcher.interval.millis")
@Component
public class AttachVolumeTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {
    protected static final String ATTACH_VOLUME_QUEUE_WATCHER = "ATTACH_VOLUME_QUEUE_WATCHER";
    protected static final int ONE_HOUR = 60 * 60 * 1000;
    protected static final int TWO_HUNDRED_TEN_SECS = 210 * 1000;

    public AttachVolumeTaskQueueWatcherInitiator() {
        super(ATTACH_VOLUME_QUEUE_WATCHER, PiQueue.ATTACH_VOLUME.getPiLocation());
    }

    @Resource
    public void setAttachVolumeTaskProcessingQueueContinuation(AttachVolumeTaskProcessingQueueContinuation attachVolumeTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(attachVolumeTaskProcessingQueueContinuation);
    }

    @Resource
    public void setAttachVolumeTaskProcessingQueueRetriesExhaustedContinuation(AttachVolumeTaskProcessingQueueRetriesExhaustedContinuation attachVolumeTaskProcessingQueueRetriesExhaustedContinuation) {
        setTaskProcessingQueueRetriesExhaustedContinuation(attachVolumeTaskProcessingQueueRetriesExhaustedContinuation);
    }
}
