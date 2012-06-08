/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.List;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.Volume;

/**
 * Service class for Elastic Block Storage methods
 */
public interface ElasticBlockStorageService {

    Volume attachVolume(String ownerId, String volumeId, String instanceId, String device);

    Snapshot createSnapshot(String ownerId, String volumeId, String description);

    Volume createVolume(String ownerId, int sizeInGigaBytes, String availabilityZone, String snapshotId);

    boolean deleteSnapshot(String ownerId, String snapshotId);

    boolean deleteVolume(String ownerId, String volumeId);

    List<Snapshot> describeSnapshots(String ownerId, List<String> snapshotIds);

    List<Volume> describeVolumes(String ownerId, List<String> volumeIds);

    Volume detachVolume(String ownerId, String volumeId, String instanceId, String device, boolean force);
}
