package com.bt.pi.app.volumemanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueRetriesExhaustedContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class AttachVolumeTaskQueueWatcherInitiatorTest {
    private AttachVolumeTaskQueueWatcherInitiator attachVolumeTaskQueueWatcherInitiator;
    private AttachVolumeTaskProcessingQueueContinuation attachVolumeTaskProcessingQueueContinuation;
    private AttachVolumeTaskProcessingQueueRetriesExhaustedContinuation attachVolumeTaskProcessingQueueRetriesExhaustedContinuation;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        final AtomicBoolean isInvokedExhausted = new AtomicBoolean(false);
        attachVolumeTaskQueueWatcherInitiator = new AttachVolumeTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == attachVolumeTaskProcessingQueueContinuation);
            }

            @Override
            protected void setTaskProcessingQueueRetriesExhaustedContinuation(TaskProcessingQueueRetriesExhaustedContinuation aTaskProcessingQueueRetriesExhaustedContinuation) {
                isInvokedExhausted.set(aTaskProcessingQueueRetriesExhaustedContinuation == attachVolumeTaskProcessingQueueRetriesExhaustedContinuation);
            }
        };

        // act
        attachVolumeTaskQueueWatcherInitiator.setAttachVolumeTaskProcessingQueueContinuation(attachVolumeTaskProcessingQueueContinuation);
        attachVolumeTaskQueueWatcherInitiator.setAttachVolumeTaskProcessingQueueRetriesExhaustedContinuation(attachVolumeTaskProcessingQueueRetriesExhaustedContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
        assertThat(isInvokedExhausted.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        attachVolumeTaskQueueWatcherInitiator = new AttachVolumeTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = attachVolumeTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(60 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(210 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(210 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
