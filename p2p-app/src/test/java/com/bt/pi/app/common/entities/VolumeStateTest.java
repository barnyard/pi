package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VolumeStateTest {

    @Test
    public void testValues() throws Exception {
        assertEquals(12, VolumeState.values().length);
    }

    @Test
    public void testUsingGetValueInsteadOfValueOfMethodForEnum() {
        for (VolumeState volumeState : VolumeState.values()) {
            assertEquals(volumeState, VolumeState.getValue(volumeState.toString().toUpperCase()));
        }
    }

    @Test
    public void testToString() {
        assertEquals("creating", VolumeState.CREATING.toString());
        assertEquals("available", VolumeState.AVAILABLE.toString());
        assertEquals("available-snapshotting", VolumeState.AVAILABLE_SNAPSHOTTING.toString());
        assertEquals("in-use", VolumeState.IN_USE.toString());
        assertEquals("in-use-snapshotting", VolumeState.IN_USE_SNAPSHOTTING.toString());
    }
}
