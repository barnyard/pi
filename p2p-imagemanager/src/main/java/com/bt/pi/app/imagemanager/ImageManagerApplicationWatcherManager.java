package com.bt.pi.app.imagemanager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@TaskProcessingQueueWatcherProperties(staleQueueItemMillis = ImageManagerApplicationWatcherManager.THIRTY_MINS, initialQueueWatcherIntervalMillis = ImageManagerApplicationWatcherManager.TWO_HUNDRED_SISTY_SECS, repeatingQueueWatcherIntervalMillis = ImageManagerApplicationWatcherManager.TWO_HUNDRED_SISTY_SECS, staleQueueItemMillisProperty = "stale.decrypt.image.queue.item.millis", initialQueueWatcherIntervalMillisProperty = "initial.decrypt.image.queue.watcher.interval.millis", repeatingQueueWatcherIntervalMillisProperty = "repeating.decrypt.image.queue.watcher.interval.millis")
@Component
public class ImageManagerApplicationWatcherManager extends TaskProcessingQueueWatcherInitiatorBase {
    protected static final String DECRYPT_IMAGE_QUEUE_WATCHER = "DECRYPT_IMAGE_QUEUE_WATCHER";
    protected static final int THIRTY_MINS = 30 * 60 * 1000;
    protected static final int TWO_HUNDRED_SISTY_SECS = 260 * 1000;

    public ImageManagerApplicationWatcherManager() {
        super(DECRYPT_IMAGE_QUEUE_WATCHER, PiQueue.DECRYPT_IMAGE.getPiLocation());
    }

    @Resource
    public void setDecryptImageTaskProcessingQueueContinuation(DecryptImageTaskProcessingQueueContinuation decryptImageTaskProcessingQueueContinuation) {
        setTaskProcessingQueueContinuation(decryptImageTaskProcessingQueueContinuation);
    }

    @Resource
    public void setDecryptImageTaskProcessingQueueRetriesExhaustedContinutation(DecryptImageTaskProcessingQueueRetriesExhaustedContinutation decryptImageTaskProcessingQueueRetriesExhaustedContinutation) {
        setTaskProcessingQueueRetriesExhaustedContinuation(decryptImageTaskProcessingQueueRetriesExhaustedContinutation);
    }
}
