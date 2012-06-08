package com.bt.pi.app.instancemanager.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class PauseInstanceTaskQueueWatcherInitiatorTest {

    private PauseInstanceTaskProcessingQueueContinuation pauseInstanceTaskProcessingQueueContinuation;
    private PauseInstanceTaskQueueWatcherInitiator pauseInstanceTaskQueueWatcherInitiator;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        pauseInstanceTaskQueueWatcherInitiator = new PauseInstanceTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == pauseInstanceTaskProcessingQueueContinuation);
            }
        };

        // act
        pauseInstanceTaskQueueWatcherInitiator.setPauseInstanceTaskProcessingQueueContinuation(pauseInstanceTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        pauseInstanceTaskQueueWatcherInitiator = new PauseInstanceTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = pauseInstanceTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(5 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(270 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(270 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
