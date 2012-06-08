package com.bt.pi.app.common.entities.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.entity.TaskProcessingQueue;

public class QueueOwnerRemovalContinuationTest {

    QueueOwnerRemovalContinuation continuation;
    String owner = "owner";

    @Before
    public void before() {

        continuation = new QueueOwnerRemovalContinuation(owner) {
            @Override
            public void handleResult(TaskProcessingQueue result) {
            }
        };
    }

    @Test
    public void testUpdate() {
        // setup
        TaskProcessingQueue existingEntity = mock(TaskProcessingQueue.class);

        // act
        continuation.update(existingEntity, null);

        // verify
        verify(existingEntity).removeOwnerFromAllTasks(eq(continuation.getOwner()));
    }

    @Test
    public void testOwnerIsSetFromConstructor() {

        // act
        assertEquals(owner, continuation.getOwner());
    }

}
