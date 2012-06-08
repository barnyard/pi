package com.bt.pi.app.volumemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class DeleteSnapshotTaskProcessingQueueContinuation extends AbstractVolumeTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(DeleteSnapshotTaskProcessingQueueContinuation.class);

    public DeleteSnapshotTaskProcessingQueueContinuation() {
    }

    @Override
    public void receiveResult(final String uri, String nodeId) {
        LOG.debug(String.format("Received queue item for delete snapshot: Url: %s, NodeId: %s", uri, nodeId));
        PId deleteSnapshotQueueId = getPiIdbuilder().getPiQueuePId(PiQueue.DELETE_SNAPSHOT).forLocalScope(PiQueue.DELETE_SNAPSHOT.getNodeScope());
        getTaskProcessingQueueHelper().setNodeIdOnUrl(deleteSnapshotQueueId, uri, nodeId);

        PId snapshotPastryId = getPiIdbuilder().getPIdForEc2AvailabilityZone(uri);
        DhtWriter writer = getDhtClientFactory().createWriter();

        writer.update(snapshotPastryId, new UpdateResolvingPiContinuation<Snapshot>() {
            @Override
            public Snapshot update(Snapshot existingEntity, Snapshot requestedEntity) {
                if (null == existingEntity)
                    return null;
                if (SnapshotState.DELETED.equals(existingEntity.getStatus())) {
                    LOG.info(String.format("snapshot %s already deleted, not re-processing", existingEntity.getSnapshotId()));
                    PId deleteSnapshotQueueId = getPiIdbuilder().getPiQueuePId(PiQueue.DELETE_SNAPSHOT).forLocalScope(PiQueue.DELETE_SNAPSHOT.getNodeScope());
                    getTaskProcessingQueueHelper().removeUrlFromQueue(deleteSnapshotQueueId, uri);
                    return null;
                }
                LOG.info(String.format("setting snapshot %s state to %s to allow re-processing", existingEntity.getUrl(), SnapshotState.COMPLETE));
                existingEntity.setStatus(SnapshotState.COMPLETE);
                return existingEntity;
            }

            @Override
            public void handleResult(final Snapshot snapshot) {
                if (null != snapshot)
                    sendAnycast(snapshot, PiTopics.DELETE_SNAPSHOT, PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(snapshot.getRegionCode(), snapshot.getAvailabilityZoneCode()), EntityMethod.DELETE);
            }
        });
    }
}
