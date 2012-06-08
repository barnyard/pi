package com.bt.pi.app.volumemanager.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.SerialExecutorRunnable;

@Component("VolumeManager.CreateSnapshotHandler")
public class CreateSnapshotHandler extends AbstractHandler {
    private static final int PROGRESS_COMPLETE = 100;
    private static final Log LOG = LogFactory.getLog(CreateSnapshotHandler.class);

    public CreateSnapshotHandler() {
        super();
    }

    // initial call - for now we just update snapshot status and send a message to the instance's node for it to do the
    // snapshot. We also process the callback from the instance manager, after that to allow us to update the snapshot
    // status again.
    public boolean createSnapshot(final Snapshot sparseSnapshot, final MessageContext receivedMessageContext, final String nodeId) {
        LOG.debug(String.format("createSnapshot(%s, %s, %s)", sparseSnapshot, receivedMessageContext, nodeId));
        validateSnapshot(sparseSnapshot);

        /*
        1. validate sparse shapshot
        2. mark task on queue
        3. check that volume is IN_USE or AVAILABLE
        4. set VOLUME state to prevent it being changed during snapshotting
        5. update snapshot state to CREATING
        6. if volume is AVAILABLE then copy from remote (gluster) drive to gluster snapshot
        7. if volume is ATTACHED then send message to instance manager that will
            a) copy the volume from the local drive to the gluster snaphot
            c) update percentage done?
            d) return reply to here
        8. update snapshot state to complete
        9. revert volume to previous state
        10. remove task from queue
        */
        try {
            PId createSnapshotQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.CREATE_SNAPSHOT).forLocalScope(PiQueue.CREATE_SNAPSHOT.getNodeScope());
            getTaskProcessingQueueHelper().setNodeIdOnUrl(createSnapshotQueueId, sparseSnapshot.getUrl(), nodeId, new TaskProcessingQueueContinuation() {
                @Override
                public void receiveResult(String uri, String nodeId) {
                    Volume sparseVolume = new Volume();
                    sparseVolume.setVolumeId(sparseSnapshot.getVolumeId());
                    updateVolumeStatus(sparseVolume, new UpdateResolvingPiContinuation<Volume>() {
                        @Override
                        public Volume update(Volume existingEntity, Volume requestedEntity) {
                            if (null == existingEntity) {
                                return null;
                            }
                            if (VolumeState.IN_USE.equals(existingEntity.getStatus())) {
                                existingEntity.setStatus(VolumeState.IN_USE_SNAPSHOTTING);
                                return existingEntity;
                            }
                            if (VolumeState.AVAILABLE.equals(existingEntity.getStatus())) {
                                existingEntity.setStatus(VolumeState.AVAILABLE_SNAPSHOTTING);
                                return existingEntity;
                            }

                            return null;
                        }

                        @Override
                        public void handleResult(final Volume result) {
                            if (null == result) {
                                return;
                            }
                            LOG.debug(String.format("updateSnapshotStateToCreating(%s, %s)", sparseSnapshot, result));
                            updateSnapshotStatus(sparseSnapshot, new SnapshotStatusUpdateResolvingPiContinuation(SnapshotState.CREATING, SnapshotState.PENDING) {
                                @Override
                                public void handleResult(Snapshot snapshot) {
                                    super.handleResult(snapshot);
                                    if (null == snapshot)
                                        return;
                                    if (VolumeState.AVAILABLE_SNAPSHOTTING.equals(result.getStatus()))
                                        createSnapshotInThread(snapshot, result);
                                    else
                                        sendSparseSnapshotToInstanceManager(snapshot, result, receivedMessageContext);
                                }
                            });
                        }
                    });
                }
            });
            return true;
        } catch (Throwable t) {
            LOG.warn("Exception while updating snapshot", t);
            return false;
        }
    }

    private void validateSnapshot(final Snapshot sparseSnapshot) {
        if (null == sparseSnapshot.getVolumeId())
            throwIllegalArgument("volumeId cannot be null");
        if (null == sparseSnapshot.getSnapshotId())
            throwIllegalArgument("snapshotId cannot be null");
        if (!SnapshotState.PENDING.equals(sparseSnapshot.getStatus()))
            throwIllegalArgument(String.format("snapshot %s must be %s", sparseSnapshot.getSnapshotId(), SnapshotState.PENDING));
    }

    private void createSnapshotInThread(final Snapshot snapshot, final Volume volume) {
        LOG.debug(String.format("createSnapshotInThread(%s, %s)", snapshot, volume));
        getSerialExecutor().execute(new SerialExecutorRunnable(PiQueue.CREATE_SNAPSHOT.getUrl(), snapshot.getUrl()) {
            @Override
            public void run() {
                try {
                    createSnapshotInDisk(snapshot, getVolumeFilename(volume.getVolumeId()));
                    updateSnapshotStatus(snapshot, new SnapshotStatusUpdateResolvingPiContinuation(SnapshotState.COMPLETE) {
                        @Override
                        public void handleResult(final Snapshot snapshotResult) {
                            updateVolumeStatusAndRemoveSnapshotFromQueue(volume, VolumeState.AVAILABLE, snapshotResult);
                        }

                        @Override
                        public Snapshot update(Snapshot aExistingEntity, Snapshot aRequestedEntity) {
                            if (aExistingEntity != null)
                                aExistingEntity.setProgress(PROGRESS_COMPLETE);
                            return super.update(aExistingEntity, aRequestedEntity);
                        }
                    });
                } catch (Throwable t) {
                    LOG.error(t.getMessage(), t);

                    updateSnapshotStatus(snapshot, new SnapshotStatusUpdateResolvingPiContinuation(SnapshotState.ERROR));
                }
            }
        });
    }

    private void sendSparseSnapshotToInstanceManager(final Snapshot snapshot, final Volume volume, final MessageContext messageContext) {
        LOG.debug(String.format("sendSparseSnapshotToInstanceManager(%s, %s, %s)", snapshot, volume, messageContext));
        String instanceId = volume.getInstanceId();
        PId id = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        DhtReader dhtReader = getDhtClientFactory().createReader();
        dhtReader.getAsync(id, new PiContinuation<Instance>() {
            @Override
            public void handleResult(Instance result) {
                // use the node id from the Instance to send the Volume to the Instance manager.
                String nodeId = result.getNodeId();
                LOG.debug("instance manager nodeId: " + nodeId);
                PId destinationNodeId = getKoalaIdFactory().buildPIdFromHexString(nodeId);
                // call InstanceManager
                messageContext.routePiMessageToApplication(destinationNodeId, EntityMethod.CREATE, snapshot, InstanceManagerApplication.APPLICATION_NAME, new PiContinuation<Snapshot>() {
                    @Override
                    public void handleResult(final Snapshot snapshotResult) {
                        if (snapshotResult.getStatus().equals(SnapshotState.COMPLETE)) {
                            updateSnapshotStatus(snapshotResult, new SnapshotStatusUpdateResolvingPiContinuation(SnapshotState.COMPLETE) {
                                @Override
                                public void handleResult(Snapshot result) {
                                    super.handleResult(result);
                                    updateVolumeStatusAndRemoveSnapshotFromQueue(volume, VolumeState.IN_USE, snapshotResult);
                                }

                                @Override
                                public Snapshot update(Snapshot aExistingEntity, Snapshot aRequestedEntity) {
                                    if (aExistingEntity != null)
                                        aExistingEntity.setProgress(PROGRESS_COMPLETE);
                                    return super.update(aExistingEntity, aRequestedEntity);
                                }
                            });
                        } else {
                            LOG.warn(String.format("The Instance Manager returned error in snapshot creation for snapshot %s", snapshot.getSnapshotId()));
                        }
                    }
                });
            }
        });
    }

    private void updateVolumeStatusAndRemoveSnapshotFromQueue(Volume volume, final VolumeState newStatus, final Snapshot snapshot) {
        LOG.debug(String.format("updateVolumeStatusAndRemoveSnapshotFromQueue(%s, %s, %s)", volume, newStatus, snapshot));
        updateVolumeStatus(volume, new UpdateResolvingPiContinuation<Volume>() {
            @Override
            public Volume update(Volume existingEntity, Volume requestedEntity) {
                if (existingEntity == null)
                    return null;
                existingEntity.setStatus(newStatus);
                return existingEntity;
            }

            @Override
            public void handleResult(Volume result) {
                if (null == result)
                    return;
                LOG.debug(String.format("Volume %s status updated to %s", result.getVolumeId(), result.getStatus()));
                PId createSnapshotQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.CREATE_SNAPSHOT).forLocalScope(PiQueue.CREATE_SNAPSHOT.getNodeScope());
                getTaskProcessingQueueHelper().removeUrlFromQueue(createSnapshotQueueId, snapshot.getUrl());
            }
        });
    }
}
