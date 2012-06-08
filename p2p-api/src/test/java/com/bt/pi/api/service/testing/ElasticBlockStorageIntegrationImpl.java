package com.bt.pi.api.service.testing;

import java.util.ArrayList;
import java.util.List;

import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;

public class ElasticBlockStorageIntegrationImpl implements ElasticBlockStorageService {

    private String snapshotId = "snapshotId";
    private String volumeId = "volumeId";
    private SnapshotState status = SnapshotState.PENDING;
    private long startTime = 1256653231969l;
    private double progress = 67.4;
    private String description = "description";

    public List<Snapshot> describeSnapshots(String ownerId, List<String> snapshotIds) {
        List<Snapshot> listOfSnapshots = new ArrayList<Snapshot>();
        if (snapshotIds.size() > 0) {
            for (String id : snapshotIds)
                listOfSnapshots.add(new Snapshot(id, volumeId, status, startTime, progress, description, ownerId));
        } else {
            listOfSnapshots.add(new Snapshot(snapshotId, volumeId, status, startTime, progress, description, ownerId));
        }
        return listOfSnapshots;
    }

    public Volume attachVolume(String ownerId, String volumeId, String instanceId, String device) {
        if (instanceId.equals("i-123"))
            return new Volume("userid", volumeId, instanceId, device, VolumeState.IN_USE, 1257173910461l);
        else
            return new Volume("userid", volumeId, instanceId, device, VolumeState.DELETED, 1257173910461l);
    }

    public Snapshot createSnapshot(String ownerId, String volumeId, String description) {
        Snapshot value = new Snapshot();
        value.setVolumeId(volumeId);
        value.setSnapshotId("snap-123");
        value.setStatus(SnapshotState.PENDING);
        value.setProgress(10.0);
        value.setStartTime(startTime);
        return value;
    }

    public Volume createVolume(String ownerId, int size, String availabilityZone, String snapshotId) {

        Volume volume = new Volume();
        volume.setAvailabilityZone(availabilityZone);
        volume.setVolumeId("v-123");
        volume.setSizeInGigaBytes(size);
        volume.setStatus(VolumeState.CREATING);
        volume.setCreateTime(System.currentTimeMillis());
        int ONE_HUNDRED = 100;
        if (size > ONE_HUNDRED)
            throw new IllegalArgumentException("size must be less than " + ONE_HUNDRED);
        else
            volume.setStatus(VolumeState.CREATING);

        return volume;
    }

    public boolean deleteSnapshot(String ownerId, String snapshotId) {
        return true;
    }

    public boolean deleteVolume(String ownerId, String volumeId) {
        return true;
    }

    public List<Volume> describeVolumes(String ownerId, List<String> volumeIds) {

        long FIXED_TIME = 1864445563437L;

        List<Volume> volumes = new ArrayList<Volume>();
        for (String volumeId : volumeIds) {
            Volume volume = new Volume();
            volume.setAttachTime(FIXED_TIME);
            volume.setAvailabilityZone("IceCube");
            volume.setCreateTime(FIXED_TIME);
            volume.setDevice("device");
            volume.setOwnerId("owner");
            volume.setSizeInGigaBytes(800);
            volume.setSnapshotId("snap-123");
            volume.setStatus(VolumeState.IN_USE);
            volume.setVolumeId(volumeId);
            volumes.add(volume);
        }
        return volumes;

    }

    public Volume detachVolume(String ownerId, String volumeId, String instanceId, String device, boolean force) {
        Volume volume = new Volume();
        volume.setAttachTime(1864445563437L);
        volume.setStatus(VolumeState.DELETED);
        volume.setInstanceId(instanceId);
        volume.setDevice(device);
        volume.setVolumeId(volumeId);
        return volume;
    }
}
