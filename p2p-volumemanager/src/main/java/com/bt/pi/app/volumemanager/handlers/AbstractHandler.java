package com.bt.pi.app.volumemanager.handlers;

import java.io.File;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

/*
 * base class for handlers
 */
public abstract class AbstractHandler extends com.bt.pi.app.instancemanager.handlers.AbstractHandler {
    protected static class VolumeStatusUpdateResolvingPiContinuation extends UpdateResolvingPiContinuation<Volume> {
        private VolumeState volumeState;
        private VolumeState lastVolumeState;

        public VolumeStatusUpdateResolvingPiContinuation(VolumeState newVolumeState) {
            this.volumeState = newVolumeState;
        }

        public VolumeStatusUpdateResolvingPiContinuation(VolumeState newVolumeState, VolumeState expectedLastVolumeState) {
            this.volumeState = newVolumeState;
            this.lastVolumeState = expectedLastVolumeState;
        }

        @Override
        public void handleResult(Volume result) {
            LOG.debug(String.format(VOLUME_S_STATUS_UPDATED_TO_S, result.getVolumeId(), result.getStatus()));
        }

        @Override
        public Volume update(Volume existingEntity, Volume requestedEntity) {
            if (lastVolumeState != null && !lastVolumeState.equals(existingEntity.getStatus()))
                throw new IllegalArgumentException(String.format("volume %s must be in %s state", existingEntity.getVolumeId(), lastVolumeState));
            existingEntity.setStatus(volumeState);
            return existingEntity;
        }
    }

    protected static class SnapshotStatusUpdateResolvingPiContinuation extends UpdateResolvingPiContinuation<Snapshot> {
        private SnapshotState snapshotState;
        private SnapshotState lastSnapshotState;

        public SnapshotStatusUpdateResolvingPiContinuation(SnapshotState newSnapshotState) {
            snapshotState = newSnapshotState;
            lastSnapshotState = null;
        }

        public SnapshotStatusUpdateResolvingPiContinuation(SnapshotState newSnapshotState, SnapshotState oldSnapshotState) {
            snapshotState = newSnapshotState;
            lastSnapshotState = oldSnapshotState;
        }

        @Override
        public void handleResult(Snapshot result) {
            LOG.debug(String.format(SNAPSHOT_S_STATUS_UPDATED_TO_S, result.getSnapshotId(), result.getStatus()));
        }

        @Override
        public Snapshot update(Snapshot existingEntity, Snapshot requestedEntity) {
            if (null == existingEntity)
                return null;
            if (lastSnapshotState != null && !lastSnapshotState.equals(existingEntity.getStatus()))
                throw new IllegalArgumentException(String.format("snapshot %s must be in %s state but it is in %s state", existingEntity.getSnapshotId(), lastSnapshotState, existingEntity.getStatus()));

            existingEntity.setStatus(snapshotState);
            return existingEntity;
        }
    }

    private static final String VOLUME_S_STATUS_UPDATED_TO_S = "Volume: %s status updated to %s";
    private static final String SNAPSHOT_S_STATUS_UPDATED_TO_S = "Snapshot: %s status updated to %s";
    private static final String DEFAULT_SNAPSHOT_FOLDER = "var/snapshots";
    private static final Log LOG = LogFactory.getLog(AbstractHandler.class);

    private String snapshotFolder = DEFAULT_SNAPSHOT_FOLDER;

    public AbstractHandler() {
        super();
    }

    @Property(key = "snapshot.folder", defaultValue = DEFAULT_SNAPSHOT_FOLDER)
    public void setSnapshotFolder(String aSnapshotFolder) {
        this.snapshotFolder = aSnapshotFolder;
    }

    protected void throwIllegalArgument(String message) {
        LOG.error(message);
        throw new IllegalArgumentException(message);
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        File dir = new File(getNfsVolumesDirectory());
        if (dir.exists()) {
            if (dir.isDirectory())
                return;
            LOG.warn(String.format("configured path %s exists but is not a directory", getNfsVolumesDirectory()));
        }
        LOG.warn(String.format("configured path %s does not exist", getNfsVolumesDirectory()));
    }

    protected String getSnapshotFilename(Snapshot snapshot) {
        return getSnapshotFilename(snapshot.getSnapshotId());
    }

    protected String getSnapshotFilename(String snapshotId) {
        return String.format("%s/%s", this.snapshotFolder, snapshotId);
    }

    protected void updateVolumeStatus(Volume volume, UpdateResolvingPiContinuation<Volume> continuation) {
        LOG.debug(String.format("updateVolumeStatus(%s, %s)", volume.getVolumeId(), continuation));
        DhtWriter dhtWriter = getDhtClientFactory().createWriter();
        PId id = getPiIdBuilder().getPId(volume).forGlobalAvailablityZoneCode(getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(volume.getVolumeId()));
        dhtWriter.update(id, continuation);
    }

    protected void updateSnapshotStatus(Snapshot snapshot, UpdateResolvingPiContinuation<Snapshot> continuation) {
        LOG.debug(String.format("updateSnapshotStatus(%s, %s)", snapshot.getSnapshotId(), continuation));
        DhtWriter dhtWriter = getDhtClientFactory().createWriter();
        PId id = getPiIdBuilder().getPId(snapshot).forGlobalAvailablityZoneCode(getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(snapshot.getSnapshotId()));
        dhtWriter.update(id, continuation);
    }

    protected void sendSparseVolumeToInstanceManager(final Volume sparseVolume, final MessageContext receivedMessageContext, final String instanceId, final Continuation<Volume, Exception> continuation) {
        LOG.debug(String.format("sendSparseVolumeToInstanceManager(%s, %s, %s, %s)", sparseVolume, receivedMessageContext, instanceId, continuation));
        DhtReader dhtReader = getDhtClientFactory().createReader();
        PId id = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        // lookup instance record
        dhtReader.getAsync(id, new PiContinuation<Instance>() {
            @Override
            public void handleResult(Instance result) {

                // TODO: if the instance is gone we need to handle this more gracefully

                // use the node id from the Instance to send the Volume to the Instance manager.
                String nodeId = result.getNodeId();
                LOG.debug("instance manager nodeId: " + nodeId);
                PId destinationNodeId = getKoalaIdFactory().buildPIdFromHexString(nodeId);
                // call InstanceManager
                receivedMessageContext.routePiMessageToApplication(destinationNodeId, EntityMethod.UPDATE, sparseVolume, InstanceManagerApplication.APPLICATION_NAME, continuation);
            }
        });
    }
}
