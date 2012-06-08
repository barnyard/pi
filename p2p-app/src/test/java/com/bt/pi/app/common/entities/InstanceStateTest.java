package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InstanceStateTest {

    @Test
    public void testGetDisplayName() {
        assertEquals("pending", InstanceState.PENDING.getDisplayName());
        assertEquals("running", InstanceState.RUNNING.getDisplayName());
        assertEquals("crashed", InstanceState.CRASHED.getDisplayName());
        assertEquals("shutting-down", InstanceState.SHUTTING_DOWN.getDisplayName());
        assertEquals("terminated", InstanceState.FAILED.getDisplayName());
        assertEquals("terminated", InstanceState.TERMINATED.getDisplayName());
    }

    @Test
    public void testGetCode() {
        assertEquals(0, InstanceState.PENDING.getCode());
        assertEquals(16, InstanceState.RUNNING.getCode());
        assertEquals(24, InstanceState.CRASHED.getCode());
        assertEquals(32, InstanceState.SHUTTING_DOWN.getCode());
        assertEquals(48, InstanceState.FAILED.getCode());
        assertEquals(48, InstanceState.TERMINATED.getCode());
    }
}
