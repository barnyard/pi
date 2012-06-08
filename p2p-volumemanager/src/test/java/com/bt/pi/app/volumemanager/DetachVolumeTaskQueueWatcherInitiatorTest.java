package com.bt.pi.app.volumemanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class DetachVolumeTaskQueueWatcherInitiatorTest {
    private DetachVolumeTaskQueueWatcherInitiator detachVolumeTaskQueueWatcherInitiator;
    private DetachVolumeTaskProcessingQueueContinuation detachVolumeTaskProcessingQueueContinuation;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        detachVolumeTaskQueueWatcherInitiator = new DetachVolumeTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == detachVolumeTaskProcessingQueueContinuation);
            }
        };

        // act
        detachVolumeTaskQueueWatcherInitiator.setDetachVolumeTaskProcessingQueueContinuation(detachVolumeTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        detachVolumeTaskQueueWatcherInitiator = new DetachVolumeTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = detachVolumeTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(60 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(220 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(220 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
