package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SnapshotStateTest {

    @Test
    public void testToString() {
        assertEquals("pending", SnapshotState.PENDING.toString());
        assertEquals("creating", SnapshotState.CREATING.toString());
        assertEquals("complete", SnapshotState.COMPLETE.toString());
        assertEquals("error", SnapshotState.ERROR.toString());
        assertEquals("deleting", SnapshotState.DELETING.toString());
        assertEquals("deleted", SnapshotState.DELETED.toString());
    }
}
