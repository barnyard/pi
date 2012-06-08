package com.bt.pi.app.instancemanager.handlers;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.Continuation;

import com.bt.pi.app.common.entities.BlockDeviceMapping;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.conf.Property;

@Component
public class VolumeBackupHandler extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(VolumeBackupHandler.class);
    private static final int ONE = 1;
    private static final String DEFAULT_BACKUP_VOLUME_COMMAND = "cp %s %s";
    private static final String DEFAULT_OVERWRITE_VOLUME_COMMAND = "mv %s %s";
    private String backupVolumeCommand = DEFAULT_BACKUP_VOLUME_COMMAND;
    private String overwriteVolumeCommand = DEFAULT_OVERWRITE_VOLUME_COMMAND;

    @Resource
    private DetachVolumeHandler detachVolumeHandler;

    public VolumeBackupHandler() {
        detachVolumeHandler = null;
    }

    @Property(key = "backup.volume.command", defaultValue = DEFAULT_BACKUP_VOLUME_COMMAND)
    public void setBackupVolumeCommand(String command) {
        this.backupVolumeCommand = command;
    }

    @Property(key = "overwrite.volume.command", defaultValue = DEFAULT_OVERWRITE_VOLUME_COMMAND)
    public void setOverwriteVolumeCommand(String command) {
        this.overwriteVolumeCommand = command;
    }

    public void startBackup(final Continuation<Object, Exception> continuation) {
        getTaskExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Instance> runningInstances = getConsumedDhtResourceRegistry().getByType(Instance.class);
                    LOG.debug(String.format("Backing up volumes on %d instances", runningInstances.size()));
                    for (Instance instance : runningInstances) {
                        backupBlockDevices(instance);
                    }

                    if (continuation != null)
                        continuation.receiveResult(null);
                } catch (Throwable t) {
                    LOG.error(t.getMessage(), t);
                    if (continuation != null)
                        continuation.receiveException(new RuntimeException(t));
                }
            }
        });
    }

    private void backupBlockDevices(Instance instance) {
        List<BlockDeviceMapping> blockDeviceMappings = instance.getBlockDeviceMappings();
        LOG.debug(String.format("Backing up %d volumes for instance: %s", blockDeviceMappings.size(), instance.getInstanceId()));
        for (BlockDeviceMapping blockDevice : blockDeviceMappings) {
            LOG.debug(String.format("Backing up volume: %s for instance: %s", blockDevice.getVolumeId(), instance.getInstanceId()));
            File tempFile = null;
            boolean acquiredLock = false;
            try {
                acquiredLock = detachVolumeHandler.acquireLock(blockDevice.getVolumeId(), ONE, TimeUnit.MILLISECONDS);
                if (!acquiredLock) {
                    LOG.debug(String.format("Unable to acquire lock to backup volume %s, so skipping backup", blockDevice.getVolumeId()));
                    continue;
                }

                tempFile = createTempFile(blockDevice);
                String localVolumeFilename = getAbsoluteLocalVolumeFilename(blockDevice.getVolumeId());
                String command = String.format(backupVolumeCommand, localVolumeFilename, tempFile.getPath());
                getCommandRunner().runNicely(command);

                if (!existsFile(localVolumeFilename))
                    LOG.debug(String.format("Not renaming temp file for volume %s back to original volume since local file has been deleted in the interim", blockDevice.getVolumeId()));
                else {
                    String remoteVolumePath = String.format("%s/%s", getNfsVolumesDirectory(), blockDevice.getVolumeId());
                    String overwriteCommand = String.format(overwriteVolumeCommand, tempFile.getPath(), remoteVolumePath);
                    LOG.debug(String.format("%s - Overwriting backup %s to original volume %s", overwriteCommand, tempFile.getPath(), remoteVolumePath));
                    getCommandRunner().runNicely(overwriteCommand);
                }
            } catch (IOException e) {
                LOG.error(String.format("Unable to create temporary file to copy volume: %s", blockDevice.getVolumeId()), e);
            } catch (CommandExecutionException e) {
                if (null != tempFile && tempFile.exists()) {
                    LOG.error(String.format("Unable to backup volume: %s - Removing temp file: %s", blockDevice.getVolumeId(), tempFile.getPath()), e);
                    boolean tempFileDeleted = tempFile.delete();
                    if (!tempFileDeleted) {
                        LOG.warn(String.format("Unable to delete temp file: %s", tempFile.getPath()));
                    }
                }
            } catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            } finally {
                releaseDetachLock(blockDevice, acquiredLock);
            }
        }
    }

    private void releaseDetachLock(BlockDeviceMapping blockDevice, boolean acquiredLock) {
        if (acquiredLock)
            detachVolumeHandler.releaseLock(blockDevice.getVolumeId());
    }

    protected boolean existsFile(String localVolumeFilename) {
        return new File(localVolumeFilename).exists();
    }

    protected File createTempFile(BlockDeviceMapping blockDevice) throws IOException {
        return File.createTempFile(blockDevice.getVolumeId() + "-", "-backup", new File(getNfsVolumesDirectory()));
    }
}
