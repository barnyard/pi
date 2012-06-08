package com.bt.pi.app.networkmanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class SecurityGroupUpdateTaskQueueWatcherInitiatorTest {

    private SecurityGroupUpdateTaskProcessingQueueContinuation securityGroupUpdateTaskProcessingQueueContinuation;
    private SecurityGroupUpdateTaskQueueWatcherInitiator securityGroupUpdateTaskQueueWatcherInitiator;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        securityGroupUpdateTaskQueueWatcherInitiator = new SecurityGroupUpdateTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == securityGroupUpdateTaskProcessingQueueContinuation);
            }
        };

        // act
        securityGroupUpdateTaskQueueWatcherInitiator.setSecurityGroupUpdateTaskProcessingQueueContinuation(securityGroupUpdateTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        securityGroupUpdateTaskQueueWatcherInitiator = new SecurityGroupUpdateTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = securityGroupUpdateTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(90 * 1000, annotation.staleQueueItemMillis());
        assertEquals(90 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(90 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
