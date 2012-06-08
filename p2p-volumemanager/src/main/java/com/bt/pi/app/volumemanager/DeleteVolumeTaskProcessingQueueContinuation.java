package com.bt.pi.app.volumemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class DeleteVolumeTaskProcessingQueueContinuation extends AbstractVolumeTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(DeleteVolumeTaskProcessingQueueContinuation.class);

    public DeleteVolumeTaskProcessingQueueContinuation() {
    }

    @Override
    public void receiveResult(final String uri, String nodeId) {
        LOG.debug(String.format("Received queue item for delete volume: Url: %s, NodeId: %s", uri, nodeId));
        PId deleteVolumeQueueId = getPiIdbuilder().getPiQueuePId(PiQueue.DELETE_VOLUME).forLocalScope(PiQueue.DELETE_VOLUME.getNodeScope());
        getTaskProcessingQueueHelper().setNodeIdOnUrl(deleteVolumeQueueId, uri, nodeId);

        PId volumePastryId = getPiIdbuilder().getPIdForEc2AvailabilityZone(uri);
        DhtWriter writer = getDhtClientFactory().createWriter();

        writer.update(volumePastryId, new UpdateResolvingPiContinuation<Volume>() {
            @Override
            public Volume update(Volume existingEntity, Volume requestedEntity) {
                if (null == existingEntity)
                    return null;
                if (VolumeState.DELETED.equals(existingEntity.getStatus())) {
                    LOG.info(String.format("volume %s is already deleted, so not re-processing", existingEntity.getVolumeId()));
                    PId deleteVolumeQueueId = getPiIdbuilder().getPiQueuePId(PiQueue.DELETE_VOLUME).forLocalScope(PiQueue.DELETE_VOLUME.getNodeScope());
                    getTaskProcessingQueueHelper().removeUrlFromQueue(deleteVolumeQueueId, uri);
                    return null;
                }
                LOG.info(String.format("setting volume %s state to %s to allow re-processing", existingEntity.getUrl(), VolumeState.AVAILABLE));
                existingEntity.setStatus(VolumeState.AVAILABLE);
                return existingEntity;
            }

            @Override
            public void handleResult(Volume result) {
                if (null != result)
                    sendAnycast(result, PiTopics.DELETE_VOLUME, PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(result.getRegionCode(), result.getAvailabilityZoneCode()), EntityMethod.DELETE);
            }
        });
    }
}
