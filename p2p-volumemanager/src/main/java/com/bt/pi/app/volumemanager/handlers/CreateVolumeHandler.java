package com.bt.pi.app.volumemanager.handlers;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.SerialExecutorRunnable;

/*
 * handle the physical creation of the volume file
 * TODO: what happens if the file already exists?
 */
@Component("VolumeManager.CreateVolumeHandler")
public class CreateVolumeHandler extends AbstractHandler {
    private static final int ONE_THOUSAND_AND_24 = 1024;
    private static final Log LOG = LogFactory.getLog(CreateVolumeHandler.class);
    private static final String DEFAULT_DD_COMMAND = "dd if=/dev/zero of=%s count=0 seek=%d bs=1M";
    private static final String DEFAULT_CREATE_VOLUME_COPY_COMMAND = "cp %s %s";
    private String ddCommand = DEFAULT_DD_COMMAND;
    private AtomicInteger currentVolumeCreationCount;

    private String createVolumeCopyCommand = DEFAULT_CREATE_VOLUME_COPY_COMMAND;

    public CreateVolumeHandler() {
        super();
        this.currentVolumeCreationCount = new AtomicInteger();
    }

    @Property(key = "volume.dd.command", defaultValue = DEFAULT_DD_COMMAND)
    public void setDdCommand(String value) {
        this.ddCommand = value;
    }

    @Property(key = "create.volume.copy.command", defaultValue = DEFAULT_CREATE_VOLUME_COPY_COMMAND)
    public void setCreateVolumeCopyCommand(String copyCommand) {
        this.createVolumeCopyCommand = copyCommand;
    }

    public boolean createVolume(final Volume theVolume, final String nodeId) {
        LOG.debug(String.format("createVolume(%s, %s)", theVolume, nodeId));

        PId createVolumeQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.CREATE_VOLUME).forLocalScope(PiQueue.CREATE_VOLUME.getNodeScope());
        getTaskProcessingQueueHelper().setNodeIdOnUrl(createVolumeQueueId, theVolume.getUrl(), nodeId, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                updateVolumeStatus(theVolume, new VolumeStatusUpdateResolvingPiContinuation(VolumeState.CREATING, VolumeState.CREATING) {
                    @Override
                    public void handleResult(final Volume result) {
                        LOG.debug(String.format("handleResult(%s)", result));
                        super.handleResult(result);

                        getSerialExecutor().execute(new SerialExecutorRunnable(PiQueue.CREATE_VOLUME.getUrl(), result.getUrl()) {
                            @Override
                            public void run() {
                                try {
                                    formatVolume(result);
                                    updateVolumeStatus(result, new VolumeStatusUpdateResolvingPiContinuation(VolumeState.AVAILABLE));
                                    PId createVolumeQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.CREATE_VOLUME).forLocalScope(PiQueue.CREATE_VOLUME.getNodeScope());
                                    getTaskProcessingQueueHelper().removeUrlFromQueue(createVolumeQueueId, result.getUrl());
                                } catch (Throwable t) {
                                    LOG.error(t.getMessage(), t);
                                }
                            }
                        });
                    }
                });
            }
        });
        return true;
    }

    private void formatVolume(final Volume volume) {
        LOG.debug(String.format("formatVolume(%s)", volume));
        final String filename = getVolumeFilename(volume.getVolumeId());
        String command = null;
        if (StringUtils.isEmpty(volume.getSnapshotId()))
            command = String.format(ddCommand, filename, volume.getSizeInGigaBytes() * ONE_THOUSAND_AND_24);
        else
            command = String.format(this.createVolumeCopyCommand, getSnapshotFilename(volume.getSnapshotId()), filename);
        getCommandRunner().runNicely(command);
    }

    // for unit testing
    protected int getCurrentVolumeCreationCount() {
        return currentVolumeCreationCount.get();
    }
}
