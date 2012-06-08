package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.app.common.entities.InstanceState;

public class InstanceStateTransitionTest {

    @Test
    public void testConstructor() {
        // act
        InstanceStateTransition transition = new InstanceStateTransition(InstanceState.RUNNING, InstanceState.SHUTTING_DOWN);

        assertEquals(InstanceState.RUNNING, transition.getPreviousState());
        assertEquals(InstanceState.SHUTTING_DOWN, transition.getNextState());
    }

    @Test
    public void testGettersAndSetter() {
        // setup
        InstanceStateTransition transition = new InstanceStateTransition();

        // act
        transition.setNextState(InstanceState.PENDING);
        transition.setPreviousState(transition.getNextState());

        // assert
        assertEquals(InstanceState.PENDING, transition.getPreviousState());

    }
}
