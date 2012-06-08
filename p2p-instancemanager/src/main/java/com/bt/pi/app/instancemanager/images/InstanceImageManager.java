/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.images;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.os.FileManager;
import com.bt.pi.app.instancemanager.libvirt.DomainNotFoundException;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManager;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.util.common.CommandRunner;

@Component
public class InstanceImageManager {
    private static final int FIFTY_MB = 50;
    private static final int ONE = 1;
    private static final String S_TERMINATED_S = "%s-terminated-%s";
    private static final String IS_NULL = " is null";
    private static final String S_EPHEMERAL = "%s/ephemeral";
    private static final String S_SWAP = "%s/swap";
    private static final String MKFS_COMMAND = "nice -n +10 ionice -c3 mkfs.ext3 -F %s >/dev/null 2>&1";
    private static final String MKSWAP_COMMAND = "mkswap %s >/dev/null";
    private static final String DD_COMMAND = "%s dd bs=1M count=0 seek=%d if=/dev/zero of=%s 2>/dev/null";
    private static final int ONE_THOUSAND_AND_TWENTY_FOUR = 1024;
    private static final String S_SLASH_S_SLASH_S = "%s/%s/%s";
    private static final String PATH_TO_PI_CACHE = "%s/pi/cache";
    private static final String UNABLE_TO_CREATE_DIRECTORY = "Unable to create directory:";
    private static final String S_SLASH_S = "%s/%s";
    private static final Log LOG = LogFactory.getLog(InstanceImageManager.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    @Resource(type = FileImageLoader.class)
    private ImageLoader imageLoader;
    @Resource
    private CommandRunner commandRunner;
    @Resource
    private LibvirtManager libvirtManager;
    @Resource
    private FileManager fileManager;

    private String embedKeyPath;
    private String instancesDirectory;
    private String ioNiceCommand;
    private long swapSize;
    private String ddCommand;
    private AtomicBoolean shouldRetainFailedInstanceArtifacts;

    public InstanceImageManager() {
        imageLoader = null;
        commandRunner = null;
        libvirtManager = null;
        shouldRetainFailedInstanceArtifacts = new AtomicBoolean();
        ddCommand = DD_COMMAND;
        fileManager = null;
    }

    @Property(key = "embed.key.path", defaultValue = "/opt/pi/current/bin/add_key.sh")
    public void setEmbedKeyPath(String anEmbedKeyPath) {
        embedKeyPath = anEmbedKeyPath;
    }

    @Property(key = "swap.size", defaultValue = "512")
    public void setSwapSize(long aSwapSize) {
        swapSize = aSwapSize;
    }

    @Property(key = "instances.directory", defaultValue = "var/instances")
    public void setInstancesDirectory(String anInstancesDirectory) {
        instancesDirectory = anInstancesDirectory;
    }

    @Property(key = "retain.failed.instance.artifacts", defaultValue = "true")
    public void setRetainFaileInstanceArtifacts(boolean shouldRetain) {
        shouldRetainFailedInstanceArtifacts.set(shouldRetain);
    }

    @Property(key = "ionice.command", defaultValue = "ionice -c3")
    public void setIoNiceCommand(String anIoNiceCommand) {
        ioNiceCommand = anIoNiceCommand;
    }

    @Property(key = "dd.command", defaultValue = DD_COMMAND)
    public void setDdCommand(String aDdCommand) {
        this.ddCommand = aDdCommand;
    }

    public void setCommandRunner(CommandRunner aCommandRunner) {
        this.commandRunner = aCommandRunner;
    }

    public void embedKey(String key, String instanceImagePath) throws IOException {
        // call the shell script to embed the key
        LOG.debug(String.format("embedKey(%s, %s)", key, instanceImagePath));
        if (key != null) {
            String keyTemplate = generateKeyTemplateFile(key); // generate key
            String command = String.format("%s %s %s", embedKeyPath, instanceImagePath, keyTemplate);
            commandRunner.run(command);
            FileUtils.deleteQuietly(new File(keyTemplate));
        }
    }

    public void setupSwapPartition(Instance instance) {
        LOG.debug(String.format("setupSwapPartition(%s)", instance));
        String swapDiskPath = String.format(S_SWAP, getInstanceDirectory(instance.getUserId(), instance.getInstanceId()));

        commandRunner.runInShell(String.format(ddCommand, ioNiceCommand, swapSize, swapDiskPath));
        commandRunner.runInShell(String.format(MKSWAP_COMMAND, swapDiskPath));
    }

    public void setupEphemeralPartition(Instance instance, String instanceImagePath, int imageSizeInMB) {
        LOG.debug(String.format("setupEphemeralPartition(%s, %s, %d)", instance, instanceImagePath, imageSizeInMB));
        String ephemeralDiskPath = String.format(S_EPHEMERAL, getInstanceDirectory(instance.getUserId(), instance.getInstanceId()));
        int ephemeralDiskSize = imageSizeInMB - getImageDiskSize(instanceImagePath);

        if (ephemeralDiskSize < ONE) {
            ephemeralDiskSize = FIFTY_MB;
        }

        commandRunner.runInShell(String.format(ddCommand, ioNiceCommand, ephemeralDiskSize, ephemeralDiskPath));
        commandRunner.runInShell(String.format(MKFS_COMMAND, ephemeralDiskPath));
    }

    public String prepareImage(Instance instance, String imagePath) throws IOException {
        LOG.debug(String.format("prepareImage(%s, %s)", instance, imagePath));
        if (!org.springframework.util.StringUtils.hasText(imagePath))
            throw new IllegalArgumentException("Image path should contain some value");

        String savedImageLocation = this.imageLoader.saveImage(imagePath, getPiCacheDirectory());
        String savedImageFilename = copyImage(savedImageLocation, getInstanceDirectory(instance.getUserId(), instance.getInstanceId()));

        String preparedImagePath = String.format(S_SLASH_S, getInstanceDirectory(instance.getUserId(), instance.getInstanceId()), savedImageFilename);

        LOG.info(String.format("prepareImage completed: %s", preparedImagePath));

        return preparedImagePath;
    }

    public void setupRequiredDirectories(Instance instance) {
        LOG.debug(String.format("setupRequiredDirectories(%s)", instance));
        // create pi cache and instance directory
        createDirectoryIfItDoesNotExist(getPiCacheDirectory());
        createDirectoryIfItDoesNotExist(getInstanceDirectory(instance.getUserId(), instance.getInstanceId()));
    }

    public String generateLibvirtXml(Instance instance, ImagePlatform platform, boolean useRamdisk, String bridgeDevice) {
        return this.libvirtManager.generateLibvirtXml(instance, platform, getInstanceDirectory(instance.getUserId(), instance.getInstanceId()), useRamdisk, instance.getPrivateMacAddress(), bridgeDevice, instance.getMemoryInKB(),
                String.valueOf(instance.getVcpus()));
    }

    public void startInstance(Instance instance, String libvirtXml) {
        LOG.debug(String.format("startInstance(%s, %s)", instance, libvirtXml));
        this.libvirtManager.startInstance(libvirtXml, instance.getInstanceId(), getInstanceDirectory(instance.getUserId(), instance.getInstanceId()));
    }

    public void stopInstance(Instance instance) {
        stopInstance(instance, true);
    }

    private void stopInstance(Instance instance, boolean forced) {
        LOG.debug(String.format("stopInstance(%s,%s)", instance, forced));
        Domain domain = libvirtManager.lookupInstance(instance.getInstanceId());
        if (domain == null)
            return;
        if (forced)
            libvirtManager.destroyInstance(domain);
        else
            libvirtManager.stopInstance(domain);

        renameInstanceDirectoryForArchive(instance);
    }

    private void renameInstanceDirectoryForArchive(Instance instance) {
        LOG.debug(String.format("renameInstanceDirectoryForArchive(%s)", instance.getInstanceId()));
        String instanceDirectory = getInstanceDirectory(instance.getUserId(), instance.getInstanceId());

        String instanceArchiveDirectory = String.format(S_TERMINATED_S, instanceDirectory, DATE_FORMAT.format(new Date()));
        if (!fileManager.fileExists(instanceDirectory)) {
            LOG.warn(String.format("not renaming instance directory %s as not found", instanceDirectory));
            return;
        }
        if (fileManager.fileExists(instanceArchiveDirectory)) {
            LOG.warn(String.format("not renaming instance directory as %s already exists", instanceArchiveDirectory));
            return;
        }
        commandRunner.run(String.format("mv %s %s", instanceDirectory, instanceArchiveDirectory));
    }

    public void rebootInstance(Instance instance) {
        LOG.debug(String.format("rebootInstance(%s)", instance));
        Domain domain = libvirtManager.lookupInstance(instance.getInstanceId());
        if (domain == null)
            return;

        libvirtManager.reboot(domain);
    }

    public boolean isInstanceRunning(Instance instance) {
        LOG.debug(String.format("isInstanceRunning(%s)", instance));
        return libvirtManager.isInstanceRunning(instance.getInstanceId());
    }

    public Collection<String> getRunningInstances() {
        LOG.debug("getRunningInstances()");
        return libvirtManager.getAllRunningInstances();
    }

    public Collection<String> getCrashedInstances() {
        LOG.debug("getCrashedInstances()");
        return libvirtManager.getAllCrashedInstances();
    }

    public void verify(String path, String message) {
        LOG.debug(String.format("verify(%s, %s)", path, message));
        if (null == path || StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException(message);
        }
    }

    private int getImageDiskSize(String imagePath) {
        LOG.debug(String.format("getImageDiskSize(%s)", imagePath));
        return (int) (new File(imagePath).length() / ONE_THOUSAND_AND_TWENTY_FOUR / ONE_THOUSAND_AND_TWENTY_FOUR);
    }

    private String generateKeyTemplateFile(String key) throws IOException {
        File tmpFile = File.createTempFile("sckey", null);
        FileUtils.writeStringToFile(tmpFile, key);
        return tmpFile.getAbsolutePath();
    }

    private String copyImage(String savedImageLocation, String anInstanceDirectory) throws IOException {
        LOG.debug(String.format("copyImage(%s, %s)", savedImageLocation, anInstanceDirectory));
        File savedImageFile = new File(savedImageLocation);
        FileUtils.copyFileToDirectory(savedImageFile, new File(anInstanceDirectory));
        return savedImageFile.getName();
    }

    public boolean doesDirectoryExist(String directory) {
        if (StringUtils.isEmpty(directory)) {
            throw new IllegalArgumentException(directory + IS_NULL);
        }

        return doesDirectoryExist(new File(directory));
    }

    private boolean doesDirectoryExist(File directory) {
        return directory.exists();
    }

    public void createDirectoryIfItDoesNotExist(String directoryPath) {
        if (StringUtils.isEmpty(directoryPath)) {
            throw new IllegalArgumentException(directoryPath + IS_NULL);
        }

        File directory = new File(directoryPath);
        if (!doesDirectoryExist(directory)) {
            try {
                FileUtils.forceMkdir(directory);
            } catch (IOException e) {
                throw new RuntimeException(UNABLE_TO_CREATE_DIRECTORY + directoryPath);
            }
        }
    }

    private String getPiCacheDirectory() {
        return String.format(PATH_TO_PI_CACHE, instancesDirectory);
    }

    private String getInstanceDirectory(String ownerName, String instanceId) {
        return String.format(S_SLASH_S_SLASH_S, instancesDirectory, ownerName, instanceId);
    }

    public long getDomainIdForInstance(String instanceId) {
        LOG.debug(String.format("getDomainIdForInstance(%s)", instanceId));
        Domain domain = libvirtManager.lookupInstance(instanceId);
        if (null == domain)
            throw new DomainNotFoundException(instanceId);
        try {
            return domain.getID();
        } catch (LibvirtException e) {
            LOG.error("unable to get domain ID for instance " + instanceId);
            throw new DomainNotFoundException();
        }
    }

    public boolean isInstanceCrashed(Instance instance) {
        LOG.debug(String.format("isInstanceCrashed(%s)", instance));
        return libvirtManager.isInstanceCrashed(instance.getInstanceId());
    }
}
