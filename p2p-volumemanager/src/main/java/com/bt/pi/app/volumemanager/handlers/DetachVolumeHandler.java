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
 * here we do any work necessary to stop "serving" the volume (currently nothing) then pass message to the correct Instance node
 * to detach the volume in xen
 */
@Component("VolumeManager.DetachVolumeHandler")
public class DetachVolumeHandler extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(DetachVolumeHandler.class);

    public DetachVolumeHandler() {
        super();
    }

    // initial call - for now we just update volumes status and send a message to the instance's node for it to do the
    // xen detach. We expect a reply from the instance manager, after that to allow us to update the volume status
    // again.
    public void detachVolume(final Volume sparseVolume, final MessageContext receivedMessageContext, final String nodeId) {
        LOG.debug(String.format("detachVolume(%s, %s, %s)", sparseVolume, receivedMessageContext, nodeId));
        if (null == sparseVolume.getVolumeId())
            throwIllegalArgument("volumeId cannot be null");
        if (null == sparseVolume.getInstanceId())
            throwIllegalArgument("instanceId cannot be null");

        PId detachVolumeQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.DETACH_VOLUME).forLocalScope(PiQueue.DETACH_VOLUME.getNodeScope());
        getTaskProcessingQueueHelper().setNodeIdOnUrl(detachVolumeQueueId, sparseVolume.getUrl(), nodeId, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                final String instanceId = sparseVolume.getInstanceId();

                updateVolumeStatus(sparseVolume, new VolumeStatusUpdateResolvingPiContinuation(sparseVolume.getStatus(), VolumeState.IN_USE) {
                    @Override
                    public void handleResult(Volume result) {
                        super.handleResult(result);
                        sendSparseVolumeToInstanceManager(sparseVolume, receivedMessageContext, instanceId, new PiContinuation<Volume>() {
                            @Override
                            public void handleResult(Volume result) {
                                LOG.debug("processing reply from instance manager for volume " + result);
                                DhtWriter dhtWriter = getDhtClientFactory().createWriter();
                                PId id = getPiIdBuilder().getPId(result).forGlobalAvailablityZoneCode(getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(result.getVolumeId()));
                                dhtWriter.update(id, result, new VolumeUpdatingContinuation(getTaskProcessingQueueHelper(), result.getStatus(), getPiIdBuilder()));
                            }
                        });
                    }
                });
            }
        });
    }

    private static class VolumeUpdatingContinuation extends UpdateResolvingPiContinuation<Volume> {
        private TaskProcessingQueueHelper taskProcessingQueueHelper;
        private final VolumeState volumeStatus;
        private PiIdBuilder piIdBuilder;

        public VolumeUpdatingContinuation(TaskProcessingQueueHelper aTaskProcessingQueueHelper, VolumeState aVolumeStatus, PiIdBuilder aPiIdBuilder) {
            this.taskProcessingQueueHelper = aTaskProcessingQueueHelper;
            this.volumeStatus = aVolumeStatus;
            this.piIdBuilder = aPiIdBuilder;
        }

        @Override
        public void handleResult(Volume result) {
            if (!VolumeState.AVAILABLE.equals(result.getStatus())) {
                LOG.debug(String.format("not removing %s from %s, as status is %s", result.getVolumeId(), PiQueue.DETACH_VOLUME.toString(), result.getStatus()));
                return;
            }
            PId detachVolumeQueueId = piIdBuilder.getPiQueuePId(PiQueue.DETACH_VOLUME).forLocalScope(PiQueue.DETACH_VOLUME.getNodeScope());
            taskProcessingQueueHelper.removeUrlFromQueue(detachVolumeQueueId, result.getUrl());
        }

        @Override
        public Volume update(Volume existingEntity, Volume requestedEntity) {
            Volume volumeToUpdate2 = (Volume) existingEntity;
            volumeToUpdate2.setStatus(this.volumeStatus);
            if (VolumeState.AVAILABLE.equals(this.volumeStatus))
                volumeToUpdate2.setInstanceId(null);
            return volumeToUpdate2;
        }
    }
}
