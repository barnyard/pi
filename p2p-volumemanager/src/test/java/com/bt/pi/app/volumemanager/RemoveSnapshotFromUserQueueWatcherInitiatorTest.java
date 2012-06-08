package com.bt.pi.app.volumemanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class RemoveSnapshotFromUserQueueWatcherInitiatorTest {
    private RemoveSnapshotFromUserTaskQueueWatcherInitiator watcherInitiator;
    private RemoveSnapshotFromUserTaskProcessingQueueContinuation queueContinuation;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        watcherInitiator = new RemoveSnapshotFromUserTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == queueContinuation);
            }
        };

        // act
        watcherInitiator.setRemoveSnapshotFromUserTaskProcessingQueueContinuation(queueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        watcherInitiator = new RemoveSnapshotFromUserTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = watcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(6 * 60 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(5 * 60 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(5 * 60 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
