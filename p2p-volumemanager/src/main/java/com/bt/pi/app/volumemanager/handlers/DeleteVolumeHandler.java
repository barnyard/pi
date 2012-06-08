package com.bt.pi.app.volumemanager.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.id.PId;

/*
 * handle the physical deletion of the volume file
 */
@Component("VolumeManager.DeleteVolumeHandler")
public class DeleteVolumeHandler extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(DeleteVolumeHandler.class);
    private static final String DEFAULT_DELETE_COMMAND = "rm %s";
    private static final int DEFAULT_REMOVE_VOLUME_FROM_USER_RETRIES = 5;
    private int removeVolumeFromUserTaskQueueRetries = DEFAULT_REMOVE_VOLUME_FROM_USER_RETRIES;
    private String deleteCommand = DEFAULT_DELETE_COMMAND;

    public DeleteVolumeHandler() {
        super();
    }

    @Property(key = "volume.delete.command", defaultValue = DEFAULT_DELETE_COMMAND)
    public void setDeleteCommand(String value) {
        this.deleteCommand = value;
    }

    @Property(key = "remove.volume.from.user.task.queue.retries", defaultValue = "5")
    public void setRemoveVolumeFromUserTaskQueueRetries(int num) {
        removeVolumeFromUserTaskQueueRetries = num;
    }

    public void deleteVolume(final Volume volume, final String nodeId) {
        LOG.debug(String.format("deleteVolume(%s, %s)", volume, nodeId));
        final PId deleteVolumeQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.DELETE_VOLUME).forLocalScope(PiQueue.DELETE_VOLUME.getNodeScope());

        getTaskProcessingQueueHelper().setNodeIdOnUrl(deleteVolumeQueueId, volume.getUrl(), nodeId, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                updateVolumeStatus(volume, new VolumeStatusUpdateResolvingPiContinuation(VolumeState.DELETING, VolumeState.AVAILABLE) {
                    @Override
                    public void handleResult(final Volume result) {
                        super.handleResult(result);
                        Thread thread = getTaskExecutor().createThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    deleteVolumeInThread(result);
                                    PId deleteVolumeQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.DELETE_VOLUME).forLocalScope(PiQueue.DELETE_VOLUME.getNodeScope());
                                    getTaskProcessingQueueHelper().removeUrlFromQueue(deleteVolumeQueueId, volume.getUrl());
                                } catch (Throwable t) {
                                    LOG.error(t.getMessage(), t);
                                }
                            }
                        });
                        thread.setPriority(Thread.MIN_PRIORITY);
                        thread.start();
                    }
                });
            }
        });
    }

    private void deleteVolumeInThread(final Volume theVolume) {
        LOG.debug(String.format("deleteVolumeInThread(%s)", theVolume));
        deleteVolumeFromDisk(theVolume);
        updateVolumeStatus(theVolume, new VolumeStatusUpdateResolvingPiContinuation(VolumeState.DELETED) {
            @Override
            public void handleResult(Volume result) {
                super.handleResult(result);

                LOG.debug(String.format("Adding task to remove volume %s from the user to the queues", result.getUrl()));
                getTaskProcessingQueueHelper().addUrlToQueue(getPiIdBuilder().getPiQueuePId(PiQueue.REMOVE_VOLUME_FROM_USER).forLocalScope(PiQueue.REMOVE_SNAPSHOT_FROM_USER.getNodeScope()), result.getUrl(), removeVolumeFromUserTaskQueueRetries);
            }
        });
    }

    private void deleteVolumeFromDisk(final Volume volume) {
        LOG.debug(String.format("deleteVolumeFromDisk(%s)", volume));
        final String filename = getVolumeFilename(volume.getVolumeId());
        String command = String.format(deleteCommand, filename);
        getCommandRunner().runNicely(command);
    }
}
