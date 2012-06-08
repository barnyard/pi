package com.bt.pi.app.volumemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class CreateSnapshotTaskProcessingQueueContinuation extends AbstractVolumeTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(CreateSnapshotTaskProcessingQueueContinuation.class);

    public CreateSnapshotTaskProcessingQueueContinuation() {
    }

    @Override
    public void receiveResult(final String url, final String nodeId) {
        LOG.debug(String.format("receiveResult(%s, %s)", url, nodeId));

        /*
        1. update snapshot status back to PENDING
        2. update volume status back to either IN_USE or AVAILABLE depending on what it is now
        3. anycast the snapshot
        */

        PId id = getPiIdbuilder().getPIdForEc2AvailabilityZone(url);
        DhtWriter writer = getDhtClientFactory().createWriter();
        final StringBuffer volumeIdCache = new StringBuffer();

        writer.update(id, new UpdateResolvingPiContinuation<Snapshot>() {
            @Override
            public Snapshot update(Snapshot existingEntity, Snapshot requestedEntity) {
                volumeIdCache.setLength(0);
                if (null == existingEntity)
                    return null;
                volumeIdCache.append(existingEntity.getVolumeId());
                if (SnapshotState.COMPLETE.equals(existingEntity.getStatus())) {
                    LOG.info(String.format("snapshot %s is complete, so not re-processing", existingEntity.getSnapshotId()));
                    PId createSnapshotQueueId = getPiIdbuilder().getPiQueuePId(PiQueue.CREATE_SNAPSHOT).forLocalScope(PiQueue.CREATE_SNAPSHOT.getNodeScope());
                    getTaskProcessingQueueHelper().removeUrlFromQueue(createSnapshotQueueId, url);
                    return null;
                }
                LOG.info(String.format("setting snapshot %s state to %s to allow re-processing", existingEntity.getSnapshotId(), SnapshotState.PENDING));
                existingEntity.setStatus(SnapshotState.PENDING);
                return existingEntity;
            }

            @Override
            public void handleResult(Snapshot result) {
                LOG.debug(String.format("handleResult(%s)", result));
                updateVolumeStatus(volumeIdCache.toString(), result);
            }
        });
    }

    private void updateVolumeStatus(final String volumeId, final Snapshot snapshot) {
        LOG.debug(String.format("updateVolumeStatus(%s, %s)", volumeId, snapshot));
        revertVolumeStatus(volumeId, new PiContinuation<Volume>() {
            @Override
            public void handleResult(Volume result) {
                if (null != snapshot)
                    sendAnycast(snapshot, PiTopics.CREATE_SNAPSHOT, PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(result.getRegionCode(), result.getAvailabilityZoneCode()), EntityMethod.CREATE);
            }
        });
    }
}
