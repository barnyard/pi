package com.bt.pi.api.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.amazonaws.ec2.doc.x20090404.InstanceStateType;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.VolumeState;

public class ConversionUtilsTest {

    private ConversionUtils conversionUtils = new ConversionUtils();

    @Test
    public void shouldReturnTerminatedForFailedInstanceType() {
        // act
        InstanceStateType newInstanceState = conversionUtils.getInstanceStateType(InstanceState.FAILED);

        // assert
        assertEquals(InstanceState.TERMINATED.toString().toLowerCase(), newInstanceState.getName());
    }

    @Test
    public void shouldConvertVolumeState() {
        assertEquals(VolumeState.AVAILABLE.toString(), conversionUtils.getVolumeStatusString(VolumeState.AVAILABLE_SNAPSHOTTING));
        assertEquals(VolumeState.IN_USE.toString(), conversionUtils.getVolumeStatusString(VolumeState.IN_USE_SNAPSHOTTING));
        assertEquals(VolumeState.IN_USE.toString(), conversionUtils.getVolumeStatusString(VolumeState.IN_USE));
        assertEquals(VolumeState.AVAILABLE.toString(), conversionUtils.getVolumeStatusString(VolumeState.AVAILABLE));
        assertEquals(VolumeState.DELETED.toString(), conversionUtils.getVolumeStatusString(VolumeState.DELETED));
        assertEquals(VolumeState.DELETED.toString(), conversionUtils.getVolumeStatusString(VolumeState.BURIED));
        assertEquals(VolumeState.FAILED.toString(), conversionUtils.getVolumeStatusString(VolumeState.FAILED));
        assertEquals(VolumeState.DETACHING.toString(), conversionUtils.getVolumeStatusString(VolumeState.FORCE_DETACHING));
    }

    @Test
    public void shouldConvertSnapshotState() {
        assertEquals(SnapshotState.PENDING.toString(), conversionUtils.getSnapshotStatusString(SnapshotState.PENDING));
        assertEquals(SnapshotState.PENDING.toString(), conversionUtils.getSnapshotStatusString(SnapshotState.CREATING));
        assertEquals(SnapshotState.COMPLETE.toString(), conversionUtils.getSnapshotStatusString(SnapshotState.COMPLETE));
        assertEquals(SnapshotState.COMPLETE.toString(), conversionUtils.getSnapshotStatusString(SnapshotState.DELETING));
        assertEquals(SnapshotState.DELETED.toString(), conversionUtils.getSnapshotStatusString(SnapshotState.DELETED));
        assertEquals(SnapshotState.DELETED.toString(), conversionUtils.getSnapshotStatusString(SnapshotState.BURIED));
    }
}
