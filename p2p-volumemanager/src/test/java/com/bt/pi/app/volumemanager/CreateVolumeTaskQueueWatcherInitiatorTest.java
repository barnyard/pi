package com.bt.pi.app.volumemanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class CreateVolumeTaskQueueWatcherInitiatorTest {
    private CreateVolumeTaskQueueWatcherInitiator createVolumeTaskQueueWatcherInitiator;
    private CreateVolumeTaskProcessingQueueContinuation createVolumeTaskProcessingQueueContinuation;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        createVolumeTaskQueueWatcherInitiator = new CreateVolumeTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == createVolumeTaskProcessingQueueContinuation);
            }
        };

        // act
        createVolumeTaskQueueWatcherInitiator.setCreateVolumeTaskProcessingQueueContinuation(createVolumeTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        createVolumeTaskQueueWatcherInitiator = new CreateVolumeTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = createVolumeTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(30 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(190 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(190 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
