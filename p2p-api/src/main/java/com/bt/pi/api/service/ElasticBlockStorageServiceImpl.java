/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeBase;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class ElasticBlockStorageServiceImpl extends ServiceHelperBase implements ElasticBlockStorageService {
    private static final Log LOG = LogFactory.getLog(ElasticBlockStorageServiceImpl.class);
    private static final String SIX_HOURS = "21600000";
    private static final String DEFAULT_DELETED_AND_STALE_MILLIS = SIX_HOURS;
    private static final String DEFAULT_VOLUME_TASK_RETRIES = "5";

    private long deletedAndStaleMillis = Long.parseLong(DEFAULT_DELETED_AND_STALE_MILLIS);
    private int volumeTaskRetries = Integer.parseInt(DEFAULT_VOLUME_TASK_RETRIES);

    public ElasticBlockStorageServiceImpl() {
    }

    @Property(key = "describe.volume.deleted.and.stale.millis", defaultValue = DEFAULT_DELETED_AND_STALE_MILLIS)
    public void setDeletedAndStaleMillis(long value) {
        this.deletedAndStaleMillis = value;
    }

    @Property(key = "volume.task.retries", defaultValue = DEFAULT_VOLUME_TASK_RETRIES)
    public void setVolumeTaskRetries(int value) {
        this.volumeTaskRetries = value;
    }

    public Volume attachVolume(String ownerId, String volumeId, String instanceId, String device) {
        LOG.debug(String.format("attachVolume(%s, %s, %s, %s)", ownerId, volumeId, instanceId, device));

        if (null == volumeId)
            throwIllegalArgument("volumeId must be provided");
        if (null == device)
            throwIllegalArgument("device must be provided");

        // ensure instance id and volume id are for same avz
        final int volumeGlobalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(volumeId);
        int instanceGlobalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(instanceId);
        if (volumeGlobalAvzCode != instanceGlobalAvzCode)
            throw new IllegalArgumentException(String.format("Volume %s and instance %s are not in the same availability zone", volumeId, instanceId));

        // validate it's a valid volume that belongs to me
        readVolume(volumeId, ownerId, VolumeState.AVAILABLE);

        checkInstance(instanceId);

        final Volume result = new Volume();
        result.setVolumeId(volumeId);
        result.setInstanceId(instanceId);
        result.setDevice(device);
        result.setStatus(VolumeState.ATTACHING);
        result.setAttachTime(System.currentTimeMillis());

        // add to task processing queue
        PId attachVolumeQueueId = getPiIdBuilder().getPId(PiQueue.ATTACH_VOLUME.getUrl()).forGlobalAvailablityZoneCode(instanceGlobalAvzCode);
        getTaskProcessingQueueHelper().addUrlToQueue(attachVolumeQueueId, result.getUrl(), volumeTaskRetries, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                // anycast message to volume manager
                sendAnycast(EntityMethod.UPDATE, result, PiTopics.ATTACH_VOLUME, volumeGlobalAvzCode);
            }
        });

        return result;
    }

    private void sendAnycast(EntityMethod entityMethod, VolumeBase volume, PiTopics piTopic, int globalAvailabilityZoneCode) {
        PubSubMessageContext pubSubMessageContext = getApiApplicationManager().newPubSubMessageContextFromGlobalAvzCode(piTopic, globalAvailabilityZoneCode);
        pubSubMessageContext.randomAnycast(entityMethod, volume);
    }

    public Snapshot createSnapshot(String ownerId, String volumeId, String description) {
        LOG.debug(String.format("createSnapshot(%s, %s, %s)", ownerId, volumeId, description));
        if (StringUtils.isEmpty(volumeId))
            throwIllegalArgument("Volume ID cannot be null or empty");
        final Volume volume = readVolume(volumeId, ownerId, Arrays.asList(VolumeState.AVAILABLE, VolumeState.IN_USE));
        final int globalAvailabilityZoneCode = PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(volume.getRegionCode(), volume.getAvailabilityZoneCode());
        String snapshotId = getIdFactory().createNewSnapshotId(globalAvailabilityZoneCode);
        final Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotId(snapshotId);
        snapshot.setVolumeId(volumeId);
        snapshot.setOwnerId(ownerId);
        snapshot.setDescription(description);
        snapshot.setStartTime(System.currentTimeMillis());
        snapshot.setCreateTime(System.currentTimeMillis());
        snapshot.setStatus(SnapshotState.PENDING);
        snapshot.setAvailabilityZone(volume.getAvailabilityZone());
        snapshot.setAvailabilityZoneCode(volume.getAvailabilityZoneCode());
        snapshot.setRegionCode(volume.getRegionCode());

        getUserService().addSnapshotToUser(ownerId, snapshotId);

        PId snapshotDhtId = getPiIdBuilder().getPIdForEc2AvailabilityZone(snapshot);
        BlockingDhtWriter blockingDhtWriter = getDhtClientFactory().createBlockingWriter();
        blockingDhtWriter.put(snapshotDhtId, snapshot);

        PId createSnapshotQueueId = getPiIdBuilder().getPId(PiQueue.CREATE_SNAPSHOT.getUrl()).forGlobalAvailablityZoneCode(volume.getAvailabilityZoneCode());
        getTaskProcessingQueueHelper().addUrlToQueue(createSnapshotQueueId, snapshot.getUrl(), volumeTaskRetries, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                // anycast message to volume manager
                sendAnycast(EntityMethod.CREATE, snapshot, PiTopics.CREATE_SNAPSHOT, globalAvailabilityZoneCode);
            }
        });

        return snapshot;
    }

    public Volume createVolume(String ownerId, int sizeInGigaBytes, String availabilityZoneName, String snapshotId) {
        LOG.debug(String.format("createVolume(%s, %d, %s, %s)", ownerId, sizeInGigaBytes, availabilityZoneName, snapshotId));
        if (StringUtils.isNotBlank(snapshotId)) {
            readSnapshot(snapshotId, ownerId, SnapshotState.COMPLETE);
        } else if (sizeInGigaBytes < 1)
            throw new IllegalArgumentException("size must be greater than 0");

        final AvailabilityZone availabilityZone;
        if (StringUtils.isNotEmpty(availabilityZoneName))
            availabilityZone = getAvailabilityZoneByName(availabilityZoneName);
        else
            availabilityZone = getLocalAvailabilityZone();

        String volumeId = getIdFactory().createNewVolumeId(availabilityZone.getGlobalAvailabilityZoneCode());
        final Volume result = new Volume();
        result.setVolumeId(volumeId);
        result.setSizeInGigaBytes(sizeInGigaBytes);
        result.setStatus(VolumeState.CREATING);
        result.setAvailabilityZone(availabilityZoneName);
        result.setAvailabilityZoneCode(availabilityZone.getAvailabilityZoneCodeWithinRegion());
        result.setRegionCode(availabilityZone.getRegionCode());
        result.setSnapshotId(snapshotId);
        result.setOwnerId(ownerId);
        result.setCreateTime(System.currentTimeMillis());

        getUserService().addVolumeToUser(ownerId, result.getVolumeId());

        PId volumeDhtId = getPiIdBuilder().getPIdForEc2AvailabilityZone(result);
        BlockingDhtWriter blockingDhtWriter = getDhtClientFactory().createBlockingWriter();
        blockingDhtWriter.put(volumeDhtId, result);

        // add to task processing queue
        PId createVolumeQueueId = getPiIdBuilder().getPId(PiQueue.CREATE_VOLUME.getUrl()).forGlobalAvailablityZoneCode(availabilityZone.getGlobalAvailabilityZoneCode());
        getTaskProcessingQueueHelper().addUrlToQueue(createVolumeQueueId, result.getUrl(), volumeTaskRetries, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                // anycast message to volume manager
                sendAnycast(EntityMethod.CREATE, result, PiTopics.CREATE_VOLUME, availabilityZone.getGlobalAvailabilityZoneCode());
            }
        });

        return result;
    }

    public boolean deleteSnapshot(String ownerId, String snapshotId) {
        LOG.debug(String.format("deleteSnapshot(%s,%s)", ownerId, snapshotId));
        // validate it's a valid snapshot and belongs to me
        final Snapshot snapshot = readSnapshot(snapshotId, ownerId, Arrays.asList(SnapshotState.COMPLETE, SnapshotState.DELETED));
        if (snapshot != null && SnapshotState.DELETED.equals(snapshot.getStatus())) {
            LOG.debug(String.format("snapshot %s already deleted", snapshotId));
            return true;
        }
        snapshot.setStatus(SnapshotState.DELETING);
        // add task to processing queue

        PId deleteSnapshotQueueId = getPiIdBuilder().getPId(PiQueue.DELETE_SNAPSHOT.getUrl()).forGlobalAvailablityZoneCode(snapshot.getAvailabilityZoneCode());

        getTaskProcessingQueueHelper().addUrlToQueue(deleteSnapshotQueueId, snapshot.getUrl(), volumeTaskRetries, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                // anycast message to volume manager
                sendAnycast(EntityMethod.DELETE, snapshot, PiTopics.DELETE_SNAPSHOT, PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(snapshot.getRegionCode(), snapshot.getAvailabilityZoneCode()));
            }
        });

        return true;
    }

    public boolean deleteVolume(String ownerId, String volumeId) {
        LOG.debug(String.format("deleteVolume(%s,%s)", ownerId, volumeId));
        // validate it's a valid volume that belongs to me
        Volume existingVolume = readVolume(volumeId, ownerId, Arrays.asList(VolumeState.AVAILABLE, VolumeState.DELETED));
        if (existingVolume != null && VolumeState.DELETED.equals(existingVolume.getStatus())) {
            LOG.debug(String.format("volume %s already deleted", volumeId));
            return true;
        }

        final Volume volume = new Volume();
        volume.setOwnerId(ownerId);
        volume.setVolumeId(volumeId);
        volume.setStatus(VolumeState.DELETING);

        // add to task processing queue
        final int globalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(volumeId);
        PId deleteVolumeQueueId = getPiIdBuilder().getPId(PiQueue.DELETE_VOLUME.getUrl()).forGlobalAvailablityZoneCode(globalAvzCode);
        getTaskProcessingQueueHelper().addUrlToQueue(deleteVolumeQueueId, volume.getUrl(), volumeTaskRetries, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                // anycast message to volume manager
                sendAnycast(EntityMethod.DELETE, volume, PiTopics.DELETE_VOLUME, globalAvzCode);
            }
        });

        return true;
    }

    public List<Snapshot> describeSnapshots(String ownerId, List<String> snapshotIds) {
        LOG.debug(String.format("describeSnapshots(%s,%s)", ownerId, snapshotIds));
        List<String> snapshotIdsToFetch = findSnapshotIdsForUser(ownerId, snapshotIds);
        final List<Snapshot> results = new ArrayList<Snapshot>();
        final List<PId> ids = new ArrayList<PId>();
        for (String snapshotId : snapshotIdsToFetch) {
            ids.add(getPiIdBuilder().getPIdForEc2AvailabilityZone(Snapshot.getUrl(snapshotId)));
        }
        scatterGather(ids, new PiContinuation<Snapshot>() {
            @Override
            public void handleResult(Snapshot result) {
                if (volumeBaseIsDeletedAndStale(result))
                    return;
                results.add(result);
            }
        });

        return results;

    }

    public List<Volume> describeVolumes(String ownerId, List<String> volumeIds) {
        LOG.debug(String.format("describeVolumes(%s, %s)", ownerId, volumeIds));
        List<String> volumeIdsToFetch = findVolumeIdsForUser(ownerId, volumeIds);
        LOG.debug(String.format("fetching volume records for %s", volumeIdsToFetch));

        final List<Volume> results = new ArrayList<Volume>();
        final List<PId> ids = new ArrayList<PId>();
        for (String volumeId : volumeIdsToFetch) {
            ids.add(getPiIdBuilder().getPIdForEc2AvailabilityZone(Volume.getUrl(volumeId)));
        }
        scatterGather(ids, new PiContinuation<Volume>() {
            @Override
            public void handleResult(Volume result) {
                if (volumeBaseIsDeletedAndStale(result))
                    return;
                results.add(result);
            }

        });

        return results;
    }

    private boolean volumeBaseIsDeletedAndStale(VolumeBase volumeBase) {
        if (volumeBase == null)
            return true;

        if (!volumeBase.isDeleted())
            return false;

        if (System.currentTimeMillis() - volumeBase.getStatusTimestamp() < deletedAndStaleMillis)
            return false;
        return true;
    }

    private List<String> findVolumeIdsForUser(String ownerId, List<String> volumeIds) {
        LOG.debug(String.format("findVolumeIdsForUser(%s, %s)", ownerId, volumeIds));
        List<String> result = new ArrayList<String>();

        User user = (User) getUserManagementService().getUser(ownerId);
        for (String volumeId : user.getVolumeIds())
            if (volumeIds == null || volumeIds.isEmpty() || volumeIds.contains(volumeId))
                result.add(volumeId);

        return result;
    }

    private List<String> findSnapshotIdsForUser(String ownerId, List<String> snapshotIds) {
        LOG.debug(String.format("findSnapshotIdsForUser(%s, %s)", ownerId, snapshotIds));
        List<String> result = new ArrayList<String>();

        User user = (User) getUserManagementService().getUser(ownerId);
        for (String snapshotId : user.getSnapshotIds())
            if (snapshotIds == null || snapshotIds.isEmpty() || snapshotIds.contains(snapshotId))
                result.add(snapshotId);

        return result;
    }

    public Volume detachVolume(String ownerId, String volumeId, String instanceId, String device, final boolean force) {
        LOG.debug(String.format("detachVolume(%s, %s, %s, %s, %s)", ownerId, volumeId, instanceId, device, force));
        // validate it's a valid volume that belongs to me
        Volume volume = readVolume(volumeId, ownerId, VolumeState.IN_USE);
        if (StringUtils.isNotEmpty(instanceId))
            if (!instanceId.equals(volume.getInstanceId()))
                throwIllegalArgument(String.format("volume %s is not attached to instance %s", volumeId, instanceId));
        if (StringUtils.isNotEmpty(device))
            if (!device.equals(volume.getDevice()))
                throwIllegalArgument(String.format("volume %s is not attached as device %s", volumeId, device));

        final Volume result = new Volume();
        result.setAttachTime(System.currentTimeMillis());
        result.setVolumeId(volumeId);
        result.setInstanceId(volume.getInstanceId());
        result.setDevice(volume.getDevice());
        result.setStatus(VolumeState.DETACHING);

        final Volume sparseVolume = new Volume();
        sparseVolume.setVolumeId(volumeId);
        sparseVolume.setInstanceId(volume.getInstanceId());
        sparseVolume.setDevice(volume.getDevice());
        sparseVolume.setStatus(VolumeState.DETACHING);
        if (force)
            sparseVolume.setStatus(VolumeState.FORCE_DETACHING);

        // add to task processing queue
        final int globalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(volumeId);
        PId detachVolumeQueueId = getPiIdBuilder().getPId(PiQueue.DETACH_VOLUME.getUrl()).forGlobalAvailablityZoneCode(globalAvzCode);
        getTaskProcessingQueueHelper().addUrlToQueue(detachVolumeQueueId, result.getUrl(), volumeTaskRetries, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                // anycast message to volume manager
                sendAnycast(EntityMethod.UPDATE, sparseVolume, PiTopics.DETACH_VOLUME, globalAvzCode);
            }
        });

        return result;
    }

    private Volume readVolume(String volumeId, String ownerId, VolumeState requiredVolumeState) {
        return readVolume(volumeId, ownerId, Arrays.asList(requiredVolumeState));
    }

    private Volume readVolume(String volumeId, String ownerId, List<VolumeState> requiredVolumeState) {
        LOG.debug(String.format("readVolume(%s, %s, %s)", volumeId, ownerId, requiredVolumeState));
        PId id = getPiIdBuilder().getPIdForEc2AvailabilityZone(Volume.getUrl(volumeId));
        BlockingDhtReader blockingDhtReader = getDhtClientFactory().createBlockingReader();
        Volume result = (Volume) blockingDhtReader.get(id);
        if (null == result)
            throwIllegalArgument(String.format("volume %s not found", volumeId));
        if (!ownerId.equals(result.getOwnerId()))
            throwIllegalArgument(String.format("volume %s is not owned by %s", volumeId, ownerId));
        if (!requiredVolumeState.contains(result.getStatus()))
            throwIllegalArgument(String.format("volume %s is %s, should be in %s state", volumeId, result.getStatus(), requiredVolumeState));
        return result;
    }

    private Snapshot readSnapshot(String snapshotId, String ownerId, SnapshotState requiredSnapshotState) {
        return readSnapshot(snapshotId, ownerId, Arrays.asList(requiredSnapshotState));
    }

    private Snapshot readSnapshot(String snapshotId, String ownerId, List<SnapshotState> requiredSnapshotStates) {
        if (StringUtils.isEmpty(snapshotId))
            throw new IllegalArgumentException("Snapshot ID cannot be null or empty");
        PId snapshotPastryId = getPiIdBuilder().getPIdForEc2AvailabilityZone(Snapshot.getUrl(snapshotId));
        BlockingDhtReader blockingDhtReader = getDhtClientFactory().createBlockingReader();
        Snapshot result = (Snapshot) blockingDhtReader.get(snapshotPastryId);
        if (null == result)
            throw new IllegalArgumentException(String.format("snapshot %s not found", snapshotId));
        if (!result.getOwnerId().equals(ownerId))
            throw new IllegalArgumentException(String.format("snapshot %s is now owned by %s", snapshotId, ownerId));
        if (!requiredSnapshotStates.contains(result.getStatus()))
            throw new IllegalArgumentException(String.format("snapshot %s is not in %s state", snapshotId, requiredSnapshotStates));
        return result;
    }

    private void throwIllegalArgument(String message) {
        LOG.error(message);
        throw new IllegalArgumentException(message);
    }

    private void checkInstance(String instanceId) {
        if (null == instanceId)
            throwIllegalArgument("instanceId must be provided");
        PId id = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        BlockingDhtReader blockingDhtReader = getDhtClientFactory().createBlockingReader();
        Instance result = (Instance) blockingDhtReader.get(id);
        if (null == result)
            throwIllegalArgument(String.format("instance %s not found", instanceId));
        if (InstanceState.RUNNING.equals(result.getState()))
            return;
        throw new IllegalArgumentException("instance must be running");
    }
}
