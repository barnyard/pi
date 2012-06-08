package com.bt.pi.app.volumemanager.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.id.PId;

/*
 * handle the physical deletion of the snapshot file
 */
@Component("VolumeManager.DeleteSnapshotHandler")
public class DeleteSnapshotHandler extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(DeleteSnapshotHandler.class);
    private static final String DEFAULT_DELETE_COMMAND = "rm %s";
    private static final int DEFAULT_REMOVE_SNAPSHOT_FROM_USER_RETRIES = 5;
    private int removeSnapshotFromUserTaskQueueRetries = DEFAULT_REMOVE_SNAPSHOT_FROM_USER_RETRIES;
    private String deleteCommand = DEFAULT_DELETE_COMMAND;

    public DeleteSnapshotHandler() {
        super();
    }

    @Property(key = "volume.delete.command", defaultValue = DEFAULT_DELETE_COMMAND)
    public void setDeleteCommand(String value) {
        this.deleteCommand = value;
    }

    @Property(key = "remove.snapshot.from.user.task.queue.retries", defaultValue = "5")
    public void setRemoveSnapshotFromUserTaskQueueRetries(int num) {
        removeSnapshotFromUserTaskQueueRetries = num;
    }

    public void deleteSnapshot(final Snapshot snapshot, final String nodeId) {
        LOG.debug(String.format("deleteSnapshot(%s, %s)", snapshot, nodeId));
        final PId deleteSnapshotQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.DELETE_SNAPSHOT).forLocalScope(PiQueue.DELETE_SNAPSHOT.getNodeScope());
        getTaskProcessingQueueHelper().setNodeIdOnUrl(deleteSnapshotQueueId, snapshot.getUrl(), nodeId, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                updateSnapshotStatus(snapshot, new SnapshotStatusUpdateResolvingPiContinuation(SnapshotState.DELETING, SnapshotState.COMPLETE) {
                    @Override
                    public void handleResult(final Snapshot result) {
                        super.handleResult(result);
                        Thread thread = getTaskExecutor().createThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    deleteSnapshotInThread(result);
                                    getTaskProcessingQueueHelper().removeUrlFromQueue(deleteSnapshotQueueId, snapshot.getUrl());
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

    private void deleteSnapshotInThread(final Snapshot snapshot) {
        LOG.debug(String.format("deleteSnapshotInThread(%s)", snapshot));
        deleteSnapshotFromDisk(snapshot);
        updateSnapshotStatus(snapshot, new SnapshotStatusUpdateResolvingPiContinuation(SnapshotState.DELETED) {
            @Override
            public void handleResult(Snapshot result) {
                super.handleResult(result);

                LOG.debug(String.format("Adding task to remove snapshot %s from the user to the queues", result.getUrl()));
                getTaskProcessingQueueHelper().addUrlToQueue(getPiIdBuilder().getPiQueuePId(PiQueue.REMOVE_SNAPSHOT_FROM_USER).forLocalScope(PiQueue.REMOVE_SNAPSHOT_FROM_USER.getNodeScope()), result.getUrl(), removeSnapshotFromUserTaskQueueRetries);
            }
        });
    }

    private void deleteSnapshotFromDisk(final Snapshot snapshot) {
        LOG.debug(String.format("deleteSnapshotFromDisk(%s)", snapshot));
        final String filename = getSnapshotFilename(snapshot);
        String command = String.format(deleteCommand, filename);
        getCommandRunner().runNicely(command);
    }
}
