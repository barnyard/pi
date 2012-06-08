package com.bt.pi.app.instancemanager.handlers;

import java.io.File;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.SerialExecutor;
import com.bt.pi.core.util.common.CommandRunner;

/*
 * base class for handlers
 */
public abstract class AbstractHandler {
    private static final String S_S = "%s/%s";
    private static final Log LOG = LogFactory.getLog(AbstractHandler.class);
    private static final String DEFAULT_NFS_VOLUMES_DIRECTORY = "var/volumes/remote";
    private static final String DEFAULT_LOCAL_VOLUMES_DIRECTORY = "var/volumes/local";
    private static final String DEV = "/dev/";
    private static final String DEFAULT_COPY_COMMAND = "cp";
    private static final String DEFAULT_DELETE_COMMAND = "rm";
    private static final String DEFAULT_RSYNC_COMMAND = "cp %s %s";
    private static final String DEFAULT_SNAPSHOT_FOLDER = "var/snapshots";

    private String nfsVolumesDirectory = DEFAULT_NFS_VOLUMES_DIRECTORY;
    private String localVolumesDirectory = DEFAULT_LOCAL_VOLUMES_DIRECTORY;
    @Resource
    private SerialExecutor serialExecutor;
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;
    @Resource
    private DhtClientFactory dhtClientFactory;
    private PiIdBuilder piIdBuilder;
    @Resource
    private LibvirtManager libvirtManager;
    @Resource
    private CommandRunner commandRunner;
    private String copyCommand = DEFAULT_COPY_COMMAND;
    private String deleteCommand = DEFAULT_DELETE_COMMAND;
    private KoalaIdFactory koalaIdFactory;
    @Resource
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Resource
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Resource(name = "generalCache")
    private DhtCache instanceTypesCache;
    private String snapshotFolder = DEFAULT_SNAPSHOT_FOLDER;
    private String rsyncCommand = DEFAULT_RSYNC_COMMAND;

    public AbstractHandler() {
        this.taskExecutor = null;
        this.dhtClientFactory = null;
        this.piIdBuilder = null;
        this.libvirtManager = null;
        this.commandRunner = null;
        this.koalaIdFactory = null;
        this.consumedDhtResourceRegistry = null;
        this.taskProcessingQueueHelper = null;
        this.instanceTypesCache = null;
        this.serialExecutor = null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    public void setThreadPoolTaskExecutor(ThreadPoolTaskExecutor aTaskExecutor) {
        this.taskExecutor = (ThreadPoolTaskExecutor) aTaskExecutor;
    }

    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    public void setCommandRunner(CommandRunner aCommandRunner) {
        this.commandRunner = aCommandRunner;
    }

    protected DhtCache getInstanceTypesCache() {
        return instanceTypesCache;
    }

    protected TaskProcessingQueueHelper getTaskProcessingQueueHelper() {
        return taskProcessingQueueHelper;
    }

    protected CommandRunner getCommandRunner() {
        return this.commandRunner;
    }

    protected LibvirtManager getLibvirtManager() {
        return libvirtManager;
    }

    protected PiIdBuilder getPiIdBuilder() {
        return piIdBuilder;
    }

    protected DhtClientFactory getDhtClientFactory() {
        return dhtClientFactory;
    }

    protected SerialExecutor getSerialExecutor() {
        return serialExecutor;
    }

    protected ThreadPoolTaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    protected KoalaIdFactory getKoalaIdFactory() {
        return koalaIdFactory;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory aKoalaIdFactory) {
        this.koalaIdFactory = aKoalaIdFactory;
    }

    protected ConsumedDhtResourceRegistry getConsumedDhtResourceRegistry() {
        return consumedDhtResourceRegistry;
    }

    @Property(key = "nfs.volumes.directory", defaultValue = DEFAULT_NFS_VOLUMES_DIRECTORY)
    public void setNfsVolumesDirectory(String value) {
        this.nfsVolumesDirectory = value;
    }

    protected String getNfsVolumesDirectory() {
        return nfsVolumesDirectory;
    }

    @Property(key = "local.volumes.directory", defaultValue = DEFAULT_LOCAL_VOLUMES_DIRECTORY)
    public void setLocalVolumesDirectory(String value) {
        this.localVolumesDirectory = value;
    }

    @Property(key = "attach.volume.copy.command", defaultValue = DEFAULT_COPY_COMMAND)
    public void setCopyCommand(String value) {
        this.copyCommand = value;
    }

    @Property(key = "detach.volume.delete.command", defaultValue = DEFAULT_DELETE_COMMAND)
    public void setDeleteCommand(String value) {
        this.deleteCommand = value;
    }

    protected String getCopyCommand() {
        return copyCommand;
    }

    protected String getDeleteCommand() {
        return deleteCommand;
    }

    protected String getAbsoluteLocalVolumesDirectory() {
        return new File(localVolumesDirectory).getAbsolutePath();
    }

    protected String getRelativeNfsVolumeFilename(Volume volume) {
        return String.format(S_S, nfsVolumesDirectory, volume.getVolumeId());
    }

    protected String getAbsoluteLocalVolumeFilename(String volumeId) {
        return String.format(S_S, getAbsoluteLocalVolumesDirectory(), volumeId);
    }

    protected String getRelativeLocalVolumeFilename(Volume volume) {
        return String.format(S_S, localVolumesDirectory, volume.getVolumeId());
    }

    @Property(key = "snapshot.folder", defaultValue = DEFAULT_SNAPSHOT_FOLDER)
    public void setSnapshotFolder(String aSnapshotFolder) {
        this.snapshotFolder = aSnapshotFolder;
    }

    @Property(key = "snapshot.rsync.command", defaultValue = DEFAULT_RSYNC_COMMAND)
    public void setRsyncCommand(String aRsyncCommand) {
        this.rsyncCommand = aRsyncCommand;
    }

    protected String getRsyncCommand() {
        return rsyncCommand;
    }

    protected String getSnapshotFolder() {
        return snapshotFolder;
    }

    protected void updateVolumeStatus(Volume volume, UpdateResolvingPiContinuation<Volume> continuation) {
        LOG.debug(String.format("updateVolumeStatus(%s, %s)", volume.getVolumeId(), continuation));
        DhtWriter dhtWriter = getDhtClientFactory().createWriter();
        PId id = getPiIdBuilder().getPId(volume);
        dhtWriter.update(id, continuation);
    }

    protected String cleanDevice(final String original) {
        if (null == original)
            return null;
        if (original.startsWith(DEV))
            return original.substring(DEV.length());
        return original;
    }

    protected String getVolumeFilename(String volumeId) {
        LOG.debug(String.format("getVolumeFilename(%s)", volumeId));
        return String.format(S_S, getNfsVolumesDirectory(), volumeId);
    }

    protected String getSnapshotFilename(Snapshot snapshot) {
        return String.format(S_S, this.getSnapshotFolder(), snapshot.getSnapshotId());
    }

    protected void createSnapshotInDisk(Snapshot snapshot, final String volumeFilename) {
        final String snapshotFilename = getSnapshotFilename(snapshot);
        getCommandRunner().runNicely(String.format(getRsyncCommand(), volumeFilename, snapshotFilename));
    }
}
