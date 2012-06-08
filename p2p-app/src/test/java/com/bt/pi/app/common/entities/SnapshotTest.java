package com.bt.pi.app.common.entities;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

public class SnapshotTest {

    private Snapshot snapshot;
    private String snapshotId = "snapshotId";
    private String volumeId = "volumeId";
    private SnapshotState status = SnapshotState.PENDING;
    private long startTime = System.currentTimeMillis();
    private double progress = 67.4;
    private String description = "description";
    private String ownerId = "ownerId";

    @Before
    public void before() {
    }

    @Test
    public void shouldConstructSnapshot() {
        // act
        snapshot = new Snapshot(snapshotId, volumeId, status, startTime, progress, description, ownerId);
        // assert
        assertEquals(snapshotId, snapshot.getSnapshotId());
        assertEquals(volumeId, snapshot.getVolumeId());
        assertEquals(status, snapshot.getStatus());
        assertEquals(startTime, snapshot.getStartTime());
        assertEquals(progress, snapshot.getProgress(), 1);
        assertEquals(description, snapshot.getDescription());
        assertEquals(ownerId, snapshot.getOwnerId());
    }

    @Test
    public void shouldConstructUsingDefault() {
        // setup
        snapshot = new Snapshot();
        // act
        snapshot.setDescription(description);
        snapshot.setOwnerId(ownerId);
        snapshot.setStartTime(startTime);
        snapshot.setStatus(status);
        snapshot.setVolumeId(volumeId);
        snapshot.setSnapshotId(snapshotId);
        snapshot.setProgress(progress);
        // assert
        assertEquals(snapshotId, snapshot.getSnapshotId());
        assertEquals(volumeId, snapshot.getVolumeId());
        assertEquals(status, snapshot.getStatus());
        assertEquals(startTime, snapshot.getStartTime());
        assertEquals(progress, snapshot.getProgress(), 1);
        assertEquals(description, snapshot.getDescription());
        assertEquals(ownerId, snapshot.getOwnerId());
    }

    @Test
    public void shouldSetStatusToDeleted() {
        // setup
        snapshot = new Snapshot(snapshotId, volumeId, status, startTime, progress, description, ownerId);
        // act
        snapshot.setDeleted(true);
        // assert
        assertEquals(SnapshotState.BURIED, snapshot.getStatus());

    }

    @Test
    public void shouldReturnDeletedStatus() {
        // setup
        snapshot = new Snapshot(snapshotId, volumeId, status, startTime, progress, description, ownerId);
        // act
        snapshot.setDeleted(true);
        // assert
        assertTrue(snapshot.isDeleted());
    }

    @Test
    public void testThatIsDeletedReturnsFalseForRecentlyDeletedSnapshots() throws Exception {
        // setup
        Snapshot snapshot = new Snapshot();
        snapshot.setStatus(SnapshotState.DELETED);
        snapshot.setStatusTimestamp(System.currentTimeMillis() - (10 * Volume.BURIED_TIME - 1));

        // act
        boolean result = snapshot.isDeleted();

        // assert
        assertFalse(result);
    }

    @Test
    public void testThatIsDeletedReturnsTrueForVeryOldVolumes() throws Exception {
        // setup
        Snapshot snapshot = new Snapshot();
        snapshot.setStatus(SnapshotState.DELETED);
        snapshot.setStatusTimestamp(System.currentTimeMillis() - (10 * Volume.BURIED_TIME + 1));

        // act
        boolean result = snapshot.isDeleted();

        // assert
        assertTrue(result);
    }
}
