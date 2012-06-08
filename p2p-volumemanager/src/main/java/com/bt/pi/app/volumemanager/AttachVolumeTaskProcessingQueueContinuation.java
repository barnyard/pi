package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.volumemanager.handlers.AttachVolumeHandler;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@Component
public class AttachVolumeTaskProcessingQueueContinuation extends AbstractVolumeTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(AttachVolumeTaskProcessingQueueContinuation.class);
    private AttachVolumeHandler attachHandler;
    private MessageContextFactory messageContextFactory;

    public AttachVolumeTaskProcessingQueueContinuation() {
        this.attachHandler = null;
        this.messageContextFactory = null;
    }

    @Override
    public void receiveResult(final String url, final String nodeId) {
        LOG.debug(String.format("receiveResult(%s, %s)", url, nodeId));
        PId id = getPiIdbuilder().getPIdForEc2AvailabilityZone(url);
        DhtWriter writer = getDhtClientFactory().createWriter();

        writer.update(id, new UpdateResolvingPiContinuation<Volume>() {
            @Override
            public Volume update(Volume existingEntity, Volume requestedEntity) {
                if (existingEntity == null) {
                    return null;
                }

                if (VolumeState.IN_USE.equals(existingEntity.getStatus())) {
                    LOG.info(String.format("Volume: %s already in IN_USE state therefore not re-processing it", existingEntity.getVolumeId()));
                    PId attachVolumeQueueId = getPiIdbuilder().getPiQueuePId(PiQueue.ATTACH_VOLUME).forLocalScope(PiQueue.ATTACH_VOLUME.getNodeScope());
                    getTaskProcessingQueueHelper().removeUrlFromQueue(attachVolumeQueueId, url);
                    return null;
                }

                LOG.info(String.format("setting volume %s state to %s to allow re-processing", existingEntity.getUrl(), VolumeState.AVAILABLE));
                existingEntity.setStatus(VolumeState.AVAILABLE);
                return existingEntity;
            }

            @Override
            public void handleResult(Volume result) {
                if (result != null) {
                    result.setStatus(VolumeState.ATTACHING);
                    attachHandler.attachVolume(result, messageContextFactory.newMessageContext(), nodeId);
                }
            }
        });
    }

    @Resource
    public void setAttachHandler(AttachVolumeHandler anAttachHandler) {
        this.attachHandler = anAttachHandler;
    }

    @Resource(type = VolumeManagerApplication.class)
    public void setMessageContextFactory(MessageContextFactory aMessageContextFactory) {
        this.messageContextFactory = aMessageContextFactory;
    }
}
