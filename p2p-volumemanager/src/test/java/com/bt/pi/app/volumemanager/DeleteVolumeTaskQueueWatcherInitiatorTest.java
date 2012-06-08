package com.bt.pi.app.volumemanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class DeleteVolumeTaskQueueWatcherInitiatorTest {

    private DeleteVolumeTaskProcessingQueueContinuation deleteVolumeTaskProcessingQueueContinuation;
    private DeleteVolumeTaskQueueWatcherInitiator deleteVolumeApplicationTaskQueueWatcherInitiator;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        deleteVolumeApplicationTaskQueueWatcherInitiator = new DeleteVolumeTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == deleteVolumeTaskProcessingQueueContinuation);
            }
        };

        // act
        deleteVolumeApplicationTaskQueueWatcherInitiator.setDeleteVolumeTaskProcessingQueueContinuation(deleteVolumeTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        deleteVolumeApplicationTaskQueueWatcherInitiator = new DeleteVolumeTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = deleteVolumeApplicationTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(30 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(200 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(200 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
