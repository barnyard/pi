package com.bt.pi.app.volumemanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class CreateSnapshotTaskQueueWatcherInitiatorTest {
    private CreateSnapshotTaskQueueWatcherInitiator createSnapshotTaskQueueWatcherInitiator;
    private CreateSnapshotTaskProcessingQueueContinuation createSnapshotTaskProcessingQueueContinuation;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        createSnapshotTaskQueueWatcherInitiator = new CreateSnapshotTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == createSnapshotTaskProcessingQueueContinuation);
            }
        };

        // act
        createSnapshotTaskQueueWatcherInitiator.setCreateSnapshotTaskProcessingQueueContinuation(createSnapshotTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        createSnapshotTaskQueueWatcherInitiator = new CreateSnapshotTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = createSnapshotTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(60 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(230 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(230 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
