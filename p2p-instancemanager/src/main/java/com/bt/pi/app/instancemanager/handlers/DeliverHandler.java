package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.BlockDeviceMapping;
import com.bt.pi.app.common.entities.ConsoleOutput;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAction;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class DeliverHandler extends AbstractHandler {
    protected static final String DEFAULT_VOLUME_TASK_RETRIES = "5";
    private static final String IGNORING_UNEXPECTED_MESSAGE_TYPE_S_AND_METHOD_S = "Ignoring unexpected message type %s and method %s";
    private static final Log LOG = LogFactory.getLog(DeliverHandler.class);
    @Resource
    private AttachVolumeHandler attachVolHandler;
    @Resource
    private DetachVolumeHandler detachVolHandler;
    @Resource
    private CreateSnapshotHandler createSnapshotHandler;
    @Resource
    private RunningInstanceInteractionHandler runningInstanceInteractionHandler;
    @Resource
    private TerminateInstanceHandler terminateInstanceHandler;
    @Resource
    private InstanceManagerApplication instanceManagerApplication;
    private int volumeTaskRetries = Integer.parseInt(DEFAULT_VOLUME_TASK_RETRIES);

    public DeliverHandler() {
        this.attachVolHandler = null;
        this.detachVolHandler = null;
        this.runningInstanceInteractionHandler = null;
        this.terminateInstanceHandler = null;
        this.instanceManagerApplication = null;
    }

    public void deliver(final PId id, final ReceivedMessageContext receivedMessageContext) {
        if (receivedMessageContext.getReceivedEntity() instanceof Instance) {
            handleMessageWithInstanceEntity(receivedMessageContext);
        } else if (receivedMessageContext.getReceivedEntity() instanceof ConsoleOutput) {
            handleMessageWithConsoleOutputEntity(receivedMessageContext);
        } else if (receivedMessageContext.getReceivedEntity() instanceof Volume && EntityMethod.UPDATE.equals(receivedMessageContext.getMethod())) {
            handleMessageWithVolumeEntity(receivedMessageContext);
        } else if (receivedMessageContext.getReceivedEntity() instanceof Snapshot) {
            final Snapshot snapshot = (Snapshot) receivedMessageContext.getReceivedEntity();
            PId snapshotQueueId = getPIdForQueue(PiQueue.CREATE_SNAPSHOT);
            getTaskProcessingQueueHelper().setNodeIdOnUrl(snapshotQueueId, snapshot.getUrl(), instanceManagerApplication.getNodeIdFull(), new TaskProcessingQueueContinuation() {
                @Override
                public void receiveResult(String uri, String nodeId) {
                    createSnapshotHandler.createSnapshot(snapshot, receivedMessageContext);
                }
            });
        } else
            LOG.warn(String.format(IGNORING_UNEXPECTED_MESSAGE_TYPE_S_AND_METHOD_S, receivedMessageContext.getReceivedEntity(), receivedMessageContext.getMethod()));
    }

    private void handleMessageWithVolumeEntity(final ReceivedMessageContext receivedMessageContext) {
        final Volume volume = (Volume) receivedMessageContext.getReceivedEntity();
        if (VolumeState.ATTACHING.equals(volume.getStatus())) {
            PId volumeQueueId = getPIdForQueue(PiQueue.ATTACH_VOLUME);
            getTaskProcessingQueueHelper().setNodeIdOnUrl(volumeQueueId, volume.getUrl(), instanceManagerApplication.getNodeIdFull(), new TaskProcessingQueueContinuation() {
                @Override
                public void receiveResult(String uri, String nodeId) {
                    attachVolHandler.attachVolume(volume, receivedMessageContext);
                }
            });
        } else if (VolumeState.DETACHING.equals(volume.getStatus()) || VolumeState.FORCE_DETACHING.equals(volume.getStatus())) {
            PId volumeQueueId = getPIdForQueue(PiQueue.DETACH_VOLUME);
            getTaskProcessingQueueHelper().setNodeIdOnUrl(volumeQueueId, volume.getUrl(), instanceManagerApplication.getNodeIdFull(), new TaskProcessingQueueContinuation() {
                @Override
                public void receiveResult(String uri, String nodeId) {
                    detachVolHandler.detachVolume(volume, receivedMessageContext);
                }
            });
        } else
            LOG.warn(String.format(IGNORING_UNEXPECTED_MESSAGE_TYPE_S_AND_METHOD_S, receivedMessageContext.getReceivedEntity(), receivedMessageContext.getMethod()));
    }

    private PId getPIdForQueue(PiQueue queue) {
        return getPiIdBuilder().getPiQueuePId(queue).forLocalScope(queue.getNodeScope());
    }

    private void handleMessageWithConsoleOutputEntity(final ReceivedMessageContext receivedMessageContext) {
        if (EntityMethod.GET.equals(receivedMessageContext.getMethod())) {
            runningInstanceInteractionHandler.respondWithConsoleOutput(receivedMessageContext);
        } else
            LOG.warn(String.format(IGNORING_UNEXPECTED_MESSAGE_TYPE_S_AND_METHOD_S, receivedMessageContext.getReceivedEntity(), receivedMessageContext.getMethod()));
    }

    private void handleMessageWithInstanceEntity(final ReceivedMessageContext receivedMessageContext) {
        Instance instance = (Instance) receivedMessageContext.getReceivedEntity();
        if (EntityMethod.DELETE.equals(receivedMessageContext.getMethod())) {
            terminateInstance(instance);
        } else if (EntityMethod.UPDATE.equals(receivedMessageContext.getMethod()) && instance.isRestartRequested()) {
            this.runningInstanceInteractionHandler.rebootInstance(instance);
        } else if (EntityMethod.UPDATE.equals(receivedMessageContext.getMethod()) && InstanceAction.PAUSE.equals(instance.anyActionRequired())) {
            this.runningInstanceInteractionHandler.pauseInstance(instance);
        } else if (EntityMethod.UPDATE.equals(receivedMessageContext.getMethod()) && InstanceAction.UNPAUSE.equals(instance.anyActionRequired())) {
            this.runningInstanceInteractionHandler.unPauseInstance(instance);
        } else
            LOG.warn(String.format(IGNORING_UNEXPECTED_MESSAGE_TYPE_S_AND_METHOD_S, receivedMessageContext.getReceivedEntity(), receivedMessageContext.getMethod()));
    }

    // TODO: Remove node id from instance and remove instance as a shared entity
    public void terminateInstance(Instance instance) {
        LOG.debug("Terminating Instance:" + instance.getInstanceId());
        detachVolumes(instance);
        terminateInstanceHandler.terminateInstance(instance);
    }

    public void destroyInstance(Instance instance) {
        LOG.debug("Destroying Instance:" + instance.getInstanceId());
        terminateInstanceHandler.terminateInstance(instance);
    }

    private void detachVolumes(Instance instance) {
        LOG.debug(String.format("detachVolumes(%s)", instance));
        for (BlockDeviceMapping blockDeviceMapping : instance.getBlockDeviceMappings()) {
            String volumeId = blockDeviceMapping.getVolumeId();
            Volume volume = new Volume();
            volume.setVolumeId(volumeId);

            // lookup volume to get availability zone
            int volumeGlobalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(volume.getVolumeId());
            PId id = getPiIdBuilder().getPId(volume).forGlobalAvailablityZoneCode(volumeGlobalAvzCode);
            getDhtClientFactory().createReader().getAsync(id, new PiContinuation<Volume>() {
                @Override
                public void handleResult(final Volume readVolume) {
                    readVolume.setStatus(VolumeState.FORCE_DETACHING);

                    PId detachVolumeQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.DETACH_VOLUME).forLocalScope(PiQueue.DETACH_VOLUME.getNodeScope());
                    getTaskProcessingQueueHelper().addUrlToQueue(detachVolumeQueueId, readVolume.getUrl(), volumeTaskRetries, new TaskProcessingQueueContinuation() {
                        @Override
                        public void receiveResult(String uri, String nodeId) {
                            sendAnycast(readVolume);
                        }
                    });
                }
            });
        }

        instance.getBlockDeviceMappings().clear();
    }

    private void sendAnycast(Volume volume) {
        LOG.debug(String.format("sendAnycast(%s)", volume));
        int globalAvailabilityZoneCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(volume.getVolumeId());
        PubSubMessageContext pubSubMessageContext = instanceManagerApplication.newPubSubMessageContextFromGlobalAvzCode(PiTopics.DETACH_VOLUME, globalAvailabilityZoneCode);
        pubSubMessageContext.sendAnycast(EntityMethod.UPDATE, volume);
    }

    @Property(key = "volume.task.retries", defaultValue = DEFAULT_VOLUME_TASK_RETRIES)
    public void setVolumeTaskRetries(int value) {
        this.volumeTaskRetries = value;
    }
}
