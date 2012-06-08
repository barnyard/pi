package com.bt.pi.app.volumemanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class DeleteSnapshotTaskQueueWatcherInitiatorTest {

    private DeleteSnapshotTaskProcessingQueueContinuation deleteSnapshotTaskProcessingQueueContinuation;
    private DeleteSnapshotTaskQueueWatcherInitiator deleteSnapshotTaskQueueWatcherInitiator;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        deleteSnapshotTaskQueueWatcherInitiator = new DeleteSnapshotTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == deleteSnapshotTaskProcessingQueueContinuation);
            }
        };

        // act
        deleteSnapshotTaskQueueWatcherInitiator.setDeleteSnapshotTaskProcessingQueueContinuation(deleteSnapshotTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        deleteSnapshotTaskQueueWatcherInitiator = new DeleteSnapshotTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = deleteSnapshotTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(15 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(250 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(250 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
