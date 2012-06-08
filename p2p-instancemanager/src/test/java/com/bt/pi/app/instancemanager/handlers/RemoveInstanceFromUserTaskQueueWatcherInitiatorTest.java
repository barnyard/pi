package com.bt.pi.app.instancemanager.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.app.instancemanager.handlers.RemoveInstanceFromUserTaskProcessingQueueContinuation;
import com.bt.pi.app.instancemanager.handlers.RemoveInstanceFromUserTaskQueueWatcherInitiator;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class RemoveInstanceFromUserTaskQueueWatcherInitiatorTest {

    private RemoveInstanceFromUserTaskQueueWatcherInitiator watcherInitiator;
    private RemoveInstanceFromUserTaskProcessingQueueContinuation queueContinuation;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        watcherInitiator = new RemoveInstanceFromUserTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == queueContinuation);
            }
        };

        // act
        watcherInitiator.setRemoveInstanceFromUserTaskProcessingQueueContinuation(queueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        watcherInitiator = new RemoveInstanceFromUserTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = watcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(60 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(135 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(135 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
