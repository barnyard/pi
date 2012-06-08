package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InstanceCheckpointTest {

    @Test
    public void testInstanceCheckpoint() {
        // setup
        InstanceCheckpoint instanceCheckpoint = new InstanceCheckpoint();

        // act
        instanceCheckpoint.setState("state");

        // assert
        assertEquals("state", instanceCheckpoint.getState());
    }
}
