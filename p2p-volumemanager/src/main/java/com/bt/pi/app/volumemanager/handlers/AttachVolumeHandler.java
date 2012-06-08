package com.bt.pi.app.volumemanager.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

/*
 * here we do any work necessary to "serve" the volume (currently nothing) then pass message to the correct Instance node
 * to attach the volume in xen
 */
@Component("VolumeManager.AttachVolumeHandler")
public class AttachVolumeHandler extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(AttachVolumeHandler.class);

    public AttachVolumeHandler() {
        super();
    }

    // initial call - for now we just update volumes status and send a message to the instance's node for it to do the
    // xen attach. We also process the callback from the instance manager, after that to allow us to update the volume
    // status again.
    public void attachVolume(final Volume sparseVolume, final MessageContext receivedMessageContext, final String nodeId) {
        LOG.debug(String.format("attachVolume(%s, %s, %s)", sparseVolume, receivedMessageContext, nodeId));
        if (null == sparseVolume.getVolumeId())
            throwIllegalArgument("volumeId cannot be null");
        if (null == sparseVolume.getInstanceId())
            throwIllegalArgument("instanceId cannot be null");
        if (null == sparseVolume.getDevice())
            throwIllegalArgument("device cannot be null");

        PId attachVolumeQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.ATTACH_VOLUME).forLocalScope(PiQueue.ATTACH_VOLUME.getNodeScope());
        getTaskProcessingQueueHelper().setNodeIdOnUrl(attachVolumeQueueId, sparseVolume.getUrl(), nodeId, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                final String instanceId = sparseVolume.getInstanceId();
                updateVolumeStatus(sparseVolume, new VolumeStatusUpdateResolvingPiContinuation(VolumeState.ATTACHING, VolumeState.AVAILABLE) {
                    @Override
                    public Volume update(Volume existingEntity, Volume requestedEntity) {
                        Volume updatedVolume = super.update(existingEntity, requestedEntity);
                        updatedVolume.setInstanceId(instanceId);
                        updatedVolume.setDevice(sparseVolume.getDevice());
                        return updatedVolume;
                    }

                    @Override
                    public void handleResult(Volume result) {
                        super.handleResult(result);
                        sendSparseVolumeToInstanceManager(sparseVolume, receivedMessageContext, instanceId, new PiContinuation<Volume>() {
                            @Override
                            public void handleResult(Volume result) {
                                LOG.debug("processing reply from instance manager for volume " + result.getVolumeId());
                                DhtWriter dhtWriter = getDhtClientFactory().createWriter();
                                PId id = getPiIdBuilder().getPId(result).forGlobalAvailablityZoneCode(getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(result.getVolumeId()));

                                dhtWriter.update(id, result, new VolumeUpdatingContinuation(getTaskProcessingQueueHelper(), getPiIdBuilder()));
                            }
                        });
                    }
                });
            }
        });
    }

    private static class VolumeUpdatingContinuation extends UpdateResolvingPiContinuation<Volume> {
        private TaskProcessingQueueHelper taskProcessingQueueHelper;
        private PiIdBuilder piIdBuilder;

        public VolumeUpdatingContinuation(TaskProcessingQueueHelper aTaskProcessingQueueHelper, PiIdBuilder aPiIdBuilder) {
            this.piIdBuilder = aPiIdBuilder;
            this.taskProcessingQueueHelper = aTaskProcessingQueueHelper;
        }

        @Override
        public void handleResult(Volume result) {
            LOG.debug(String.format("handleResult(%s)", result));
            PId attachVolumeQueueId = piIdBuilder.getPiQueuePId(PiQueue.ATTACH_VOLUME).forLocalScope(PiQueue.ATTACH_VOLUME.getNodeScope());
            taskProcessingQueueHelper.removeUrlFromQueue(attachVolumeQueueId, result.getUrl());
        }

        @Override
        public Volume update(Volume existingEntity, Volume requestedEntity) {
            LOG.debug(String.format("update(%s, %s)", existingEntity, requestedEntity));
            Volume volumeToUpdate2 = (Volume) existingEntity;
            volumeToUpdate2.setStatus(VolumeState.IN_USE);
            volumeToUpdate2.setAttachTime(System.currentTimeMillis());
            return volumeToUpdate2;
        }
    }
}
