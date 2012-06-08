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
public class CreateVolumeTaskProcessingQueueContinuation extends AbstractVolumeTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(CreateVolumeTaskProcessingQueueContinuation.class);

    public CreateVolumeTaskProcessingQueueContinuation() {
    }

    @Override
    public void receiveResult(final String url, final String nodeId) {
        LOG.debug(String.format("receiveResult(%s, %s)", url, nodeId));
        PId id = getPiIdbuilder().getPIdForEc2AvailabilityZone(url);
        DhtWriter writer = getDhtClientFactory().createWriter();

        writer.update(id, new UpdateResolvingPiContinuation<Volume>() {
            @Override
            public Volume update(Volume existingEntity, Volume requestedEntity) {
                if (null == existingEntity)
                    return null;
                if (existingEntity.getStatus().ordinal() > VolumeState.CREATING.ordinal()) {
                    LOG.debug(String.format("not reprocessing volume %s as state is %s", existingEntity.getVolumeId(), existingEntity.getStatus()));
                    PId createVolumeQueueId = getPiIdbuilder().getPiQueuePId(PiQueue.CREATE_VOLUME).forLocalScope(PiQueue.CREATE_VOLUME.getNodeScope());
                    getTaskProcessingQueueHelper().removeUrlFromQueue(createVolumeQueueId, url);
                    return null;
                }
                LOG.info(String.format("setting volume %s state to %s to allow re-processing", existingEntity.getUrl(), VolumeState.CREATING));
                existingEntity.setStatus(VolumeState.CREATING);
                return existingEntity;
            }

            @Override
            public void handleResult(Volume result) {
                if (null != result)
                    sendAnycast(result, PiTopics.CREATE_VOLUME, PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(result.getRegionCode(), result.getAvailabilityZoneCode()), EntityMethod.CREATE);
            }
        });
    }
}
