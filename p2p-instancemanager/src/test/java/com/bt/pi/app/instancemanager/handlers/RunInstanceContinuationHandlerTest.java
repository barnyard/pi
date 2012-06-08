package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.Continuation;

import com.bt.pi.app.common.entities.Instance;

@RunWith(MockitoJUnitRunner.class)
public class RunInstanceContinuationHandlerTest {
    @InjectMocks
    private RunInstanceContinuationHandler runInstanceContinuationHandler = new RunInstanceContinuationHandler();
    @Mock
    private RunInstanceHandler runInstanceHandler;
    private String nodeIdFull = "1234";

    @Test
    public void testGetContinuation() {
        // act
        Continuation<Instance, Exception> result = runInstanceContinuationHandler.getContinuation(nodeIdFull);

        // assert
        assertTrue(result instanceof RunInstanceContinuation);
        RunInstanceContinuation runInstanceContinuation = (RunInstanceContinuation) result;
        assertEquals(nodeIdFull, runInstanceContinuation.getNodeId());
    }
}
