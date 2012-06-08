package com.bt.pi.app.volumemanager;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.volumemanager.handlers.DetachVolumeHandler;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@Component
public class DetachVolumeTaskProcessingQueueContinuation extends AbstractVolumeTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final String UNABLE_TO_GET_VOLUME_S = "Unable to get Volume: %s";
    private static final Log LOG = LogFactory.getLog(DetachVolumeTaskProcessingQueueContinuation.class);
    @Resource
    private DetachVolumeHandler detachHandler;
    @Resource(type = VolumeManagerApplication.class)
    private MessageContextFactory messageContextFactory;

    public DetachVolumeTaskProcessingQueueContinuation() {
        this.detachHandler = null;
        this.messageContextFactory = null;
    }

    @Override
    public void receiveResult(final String url, final String nodeId) {
        LOG.debug(String.format("receiveResult(%s, %s)", url, nodeId));
        PId id = getPiIdbuilder().getPIdForEc2AvailabilityZone(url);
        DhtWriter writer = getDhtClientFactory().createWriter();
        final List<VolumeState> volumeStateCache = new ArrayList<VolumeState>();

        writer.update(id, new UpdateResolvingPiContinuation<Volume>() {
            @Override
            public Volume update(Volume existingEntity, Volume requestedEntity) {
                if (null == existingEntity) {
                    LOG.warn(String.format(UNABLE_TO_GET_VOLUME_S, url));
                    return null;
                }

                if (VolumeState.AVAILABLE.equals(existingEntity.getStatus())) {
                    LOG.warn(String.format("volume status is %s, so assuming already detached", existingEntity.getStatus()));
                    PId detachVolumeQueueId = getPiIdbuilder().getPiQueuePId(PiQueue.DETACH_VOLUME).forLocalScope(PiQueue.DETACH_VOLUME.getNodeScope());
                    getTaskProcessingQueueHelper().removeUrlFromQueue(detachVolumeQueueId, url);
                    return null;
                }

                volumeStateCache.clear();
                volumeStateCache.add(existingEntity.getStatus());

                LOG.info(String.format("setting volume %s state to %s to allow re-processing", existingEntity.getUrl(), VolumeState.IN_USE));
                existingEntity.setStatus(VolumeState.IN_USE);
                return existingEntity;
            }

            @Override
            public void handleResult(Volume result) {
                if (null == result) {
                    LOG.warn(String.format(UNABLE_TO_GET_VOLUME_S, url));
                    return;
                }
                if (!VolumeState.AVAILABLE.equals(result.getStatus()) && null != result.getInstanceId()) {
                    result.setStatus(volumeStateCache.get(0));
                    detachHandler.detachVolume(result, messageContextFactory.newMessageContext(), nodeId);
                } else {
                    LOG.debug("Not detaching volume as VolumeState is " + result.getStatus());
                }
            }
        });
    }
}
