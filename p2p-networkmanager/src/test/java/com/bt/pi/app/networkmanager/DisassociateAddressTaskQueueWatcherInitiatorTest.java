package com.bt.pi.app.networkmanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class DisassociateAddressTaskQueueWatcherInitiatorTest {
    private DisassociateAddressTaskProcessingQueueContinuation disassociateAddressTaskProcessingQueueContinuation;
    private DisassociateAddressTaskQueueWatcherInitiator disassociateAddressTaskQueueWatcherInitiator;

    @Test
    public void shouldCallSuperClassToSetWatcherInitiator() {
        // setup
        final AtomicBoolean isInvoked = new AtomicBoolean(false);
        disassociateAddressTaskQueueWatcherInitiator = new DisassociateAddressTaskQueueWatcherInitiator() {
            @Override
            protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
                isInvoked.set(aTaskProcessingQueueContinuation == disassociateAddressTaskProcessingQueueContinuation);
            }
        };

        // act
        disassociateAddressTaskQueueWatcherInitiator.setDisassociateAddressTaskProcessingQueueContinuation(disassociateAddressTaskProcessingQueueContinuation);

        // assert
        assertThat(isInvoked.get(), is(true));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // setup
        disassociateAddressTaskQueueWatcherInitiator = new DisassociateAddressTaskQueueWatcherInitiator();

        // act
        TaskProcessingQueueWatcherProperties annotation = disassociateAddressTaskQueueWatcherInitiator.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(120 * 1000, annotation.staleQueueItemMillis());
        assertEquals(120 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(120 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
