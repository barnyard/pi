package com.bt.pi.app.instancemanager.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.app.instancemanager.handlers.TerminateInstanceTaskProcessingQueueContinuation;
import com.bt.pi.app.instancemanager.handlers.TerminateInstanceTaskQueueWatcherInitiator;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class TerminateInstanceTaskQueueWatcherInitiatorTest {

    private TerminateInstanceTaskProcessingQueueContinuation terminateInstanceTaskProcessingQueueContinuation;
    private TerminateInstanceTaskQueueWatcherInitiator terminateInstanceTaskQueueWatcherInitiator;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        terminateInstanceTaskQueueWatcherInitiator = new TerminateInstanceTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == terminateInstanceTaskProcessingQueueContinuation);
            }
        };

        // act
        terminateInstanceTaskQueueWatcherInitiator.setTerminateInstanceTaskProcessingQueueContinuation(terminateInstanceTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        terminateInstanceTaskQueueWatcherInitiator = new TerminateInstanceTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = terminateInstanceTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(30 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(210 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(210 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
