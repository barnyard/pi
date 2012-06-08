package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = RemoveVolumeFromUserTaskQueueWatcherInitiator.BURIED_TIME, initialQueueWatcherIntervalMillis = RemoveVolumeFromUserTaskQueueWatcherInitiator.FOUR_MINUTES, repeatingQueueWatcherIntervalMillis = RemoveVolumeFromUserTaskQueueWatcherInitiator.FOUR_MINUTES, staleQueueItemMillisProperty = "stale.remove.volume.from.user.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.remove.volume.from.user.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.remove.volume.from.user.queue.watcher.interval.millis")
@Component
public class RemoveVolumeFromUserTaskQueueWatcherInitiator extends TaskProcessingQueueWatcherInitiatorBase {
    public static final String REMOVE_VOLUME_FROM_USER_QUEUE_WATCHER_NAME = "REMOVE_VOLUME_FROM_USER_QUEUE_WATCHER";
    public static final int FOUR_MINUTES = 4 * 60 * 1000;
    public static final int BURIED_TIME = Volume.BURIED_TIME;

    public RemoveVolumeFromUserTaskQueueWatcherInitiator() {
        super(REMOVE_VOLUME_FROM_USER_QUEUE_WATCHER_NAME, PiQueue.REMOVE_VOLUME_FROM_USER.getPiLocation());
    }

    @Resource
    public void setRemoveVolumeFromUserTaskProcessingQueueContinuation(RemoveVolumeFromUserTaskProcessingQueueContinuation removeVolumeFromUserTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(removeVolumeFromUserTaskProcessingQueueContinuation);
    }
}
