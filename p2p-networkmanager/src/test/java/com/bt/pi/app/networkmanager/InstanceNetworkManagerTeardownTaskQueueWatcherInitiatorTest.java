package com.bt.pi.app.networkmanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@RunWith(MockitoJUnitRunner.class)
public class InstanceNetworkManagerTeardownTaskQueueWatcherInitiatorTest {
    private InstanceNetworkManagerTeardownTaskProcessingQueueContinuation instanceNetworkManagerTeardownTaskProcessingQueueContinuation;
    private InstanceNetworkManagerTeardownTaskQueueWatcherInitiator instanceNetworkManagerTeardownTaskQueueWatcherInitiator;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        instanceNetworkManagerTeardownTaskQueueWatcherInitiator = new InstanceNetworkManagerTeardownTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == instanceNetworkManagerTeardownTaskProcessingQueueContinuation);
            }
        };

        // act
        instanceNetworkManagerTeardownTaskQueueWatcherInitiator.setInstanceNetworkManagerTeardownTaskProcessingQueueContinuation(instanceNetworkManagerTeardownTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        instanceNetworkManagerTeardownTaskQueueWatcherInitiator = new InstanceNetworkManagerTeardownTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = instanceNetworkManagerTeardownTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(60 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(60 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
