package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeBase;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

public abstract class AbstractVolumeTaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(AbstractVolumeTaskProcessingQueueContinuation.class);
    private PiIdBuilder piIdbuilder;
    private DhtClientFactory dhtClientFactory;
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Resource
    private VolumeManagerApplication volumeManagerApplication;

    public AbstractVolumeTaskProcessingQueueContinuation() {
        this.piIdbuilder = null;
        this.dhtClientFactory = null;
        this.taskProcessingQueueHelper = null;
    }

    @Resource
    public void setPiIdbuilder(PiIdBuilder aPiIdbuilder) {
        this.piIdbuilder = aPiIdbuilder;
    }

    protected PiIdBuilder getPiIdbuilder() {
        return piIdbuilder;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    protected DhtClientFactory getDhtClientFactory() {
        return dhtClientFactory;
    }

    @Resource
    public void setTaskProcessingQueueHelper(TaskProcessingQueueHelper aTaskProcessingQueueHelper) {
        taskProcessingQueueHelper = aTaskProcessingQueueHelper;
    }

    protected TaskProcessingQueueHelper getTaskProcessingQueueHelper() {
        return taskProcessingQueueHelper;
    }

    protected void sendAnycast(VolumeBase volume, PiTopics piTopic, int availabilityZoneCode, EntityMethod entityMethod) {
        LOG.debug(String.format("sendAnycast(%s, %d, %s)", volume, availabilityZoneCode, entityMethod));
        PubSubMessageContext pubSubMessageContext = null;
        if (availabilityZoneCode <= 0)
            pubSubMessageContext = volumeManagerApplication.newLocalPubSubMessageContext(piTopic);
        else {
            pubSubMessageContext = volumeManagerApplication.newPubSubMessageContextFromGlobalAvzCode(piTopic, availabilityZoneCode);
        }
        pubSubMessageContext.randomAnycast(entityMethod, volume);
    }

    protected static class VolumeStatusUpdatingContinuation extends UpdateResolvingPiContinuation<Volume> {
        private VolumeState newVolumeState;
        private Object actionMessage;

        public VolumeStatusUpdatingContinuation(VolumeState volumeState, String action) {
            this.newVolumeState = volumeState;
            this.actionMessage = action;
        }

        @Override
        public Volume update(Volume existingEntity, Volume requestedEntity) {
            LOG.error(String.format("setting volume %s state to %s as %s retries are exhausted", existingEntity.getUrl(), newVolumeState, actionMessage));
            existingEntity.setStatus(newVolumeState);
            return existingEntity;
        }

        @Override
        public void handleResult(Volume result) {
            LOG.debug(String.format("volume %s now set to %s", result.getVolumeId(), result.getStatus()));
        }
    }

    protected static class SnapshotStatusUpdatingContinuation extends UpdateResolvingPiContinuation<Snapshot> {
        private SnapshotState newSnapshotState;
        private String actionMessage;

        public SnapshotStatusUpdatingContinuation(SnapshotState snapshotState, String action) {
            this.newSnapshotState = snapshotState;
            this.actionMessage = action;
        }

        @Override
        public Snapshot update(Snapshot existingEntity, Snapshot requestedEntity) {
            LOG.error(String.format("setting snapshot %s state to %s as %s retries are exhausted", existingEntity.getUrl(), newSnapshotState, actionMessage));
            existingEntity.setStatus(newSnapshotState);
            return existingEntity;
        }

        @Override
        public void handleResult(Snapshot result) {
            LOG.debug(String.format("snapshot %s now set to %s", result.getSnapshotId(), result.getStatus()));
        }
    }

    protected void revertVolumeStatus(String volumeId, final PiContinuation<Volume> continuation) {
        LOG.debug(String.format("revertVolumeStatus(%s, %s)", volumeId, continuation));
        DhtWriter dhtWriter = getDhtClientFactory().createWriter();
        PId id = getPiIdbuilder().getPId(Volume.getUrl(volumeId)).forGlobalAvailablityZoneCode(getPiIdbuilder().getGlobalAvailabilityZoneCodeFromEc2Id(volumeId));
        dhtWriter.update(id, new UpdateResolvingPiContinuation<Volume>() {
            @Override
            public Volume update(Volume existingEntity, Volume requestedEntity) {
                if (existingEntity == null)
                    return null;
                if (VolumeState.AVAILABLE_SNAPSHOTTING.equals(existingEntity.getStatus()))
                    existingEntity.setStatus(VolumeState.AVAILABLE);
                if (VolumeState.IN_USE_SNAPSHOTTING.equals(existingEntity.getStatus()))
                    existingEntity.setStatus(VolumeState.IN_USE);
                return existingEntity;
            }

            @Override
            public void handleResult(Volume result) {
                if (null == result)
                    return;
                LOG.debug(String.format("Volume %s status updated to %s", result.getVolumeId(), result.getStatus()));
                if (null == continuation)
                    return;
                continuation.handleResult(result);
            }
        });
    }
}
