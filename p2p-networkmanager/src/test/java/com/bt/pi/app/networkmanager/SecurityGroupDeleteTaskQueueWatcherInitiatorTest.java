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
public class SecurityGroupDeleteTaskQueueWatcherInitiatorTest {

    private SecurityGroupDeleteTaskProcessingQueueContinuation securityGroupDeleteTaskProcessingQueueContinuation;
    private SecurityGroupDeleteTaskQueueWatcherInitiator securityGroupDeleteTaskQueueWatcherInitiator;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        securityGroupDeleteTaskQueueWatcherInitiator = new SecurityGroupDeleteTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == securityGroupDeleteTaskProcessingQueueContinuation);
            }
        };

        // act
        securityGroupDeleteTaskQueueWatcherInitiator.setSecurityGroupDeleteTaskProcessingQueueContinuation(securityGroupDeleteTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        securityGroupDeleteTaskQueueWatcherInitiator = new SecurityGroupDeleteTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = securityGroupDeleteTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(60 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(60 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
