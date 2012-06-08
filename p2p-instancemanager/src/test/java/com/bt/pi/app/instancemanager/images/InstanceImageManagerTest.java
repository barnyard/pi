package com.bt.pi.app.instancemanager.images;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.os.FileManager;
import com.bt.pi.app.instancemanager.libvirt.DomainNotFoundException;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManager;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManagerException;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FileUtils.class, InstanceImageManager.class })
@PowerMockIgnore({ "org.apache.commons.logging.*", "org.apache.log4j.*" })
public class InstanceImageManagerTest {
    private static final String USERID = "koala";
    private static final String INSTANCES_DIRECTORY = "path/to/instances";
    private static final String EMBED_KEY_PATH = "path/to/add_key.sh";
    private static final long SWAP_SIZE = 512;
    private static final String IONICE_COMMAND = "ionice -c3";
    private static final String INSTANCE_ID = "i-123";

    @InjectMocks
    private InstanceImageManager instanceImageManager = new InstanceImageManager();
    @Mock
    private ImageLoader imageLoader;
    @Mock
    private CommandRunner commandRunner;
    @Mock
    private LibvirtManager libvirtManager;
    private Instance instance;
    @Mock
    private FileManager fileManager;
    @Mock
    private Domain domain;

    @Before
    public void setUp() throws Exception {
        when(libvirtManager.lookupInstance(INSTANCE_ID)).thenReturn(domain);
        instanceImageManager.setEmbedKeyPath(EMBED_KEY_PATH);
        instanceImageManager.setInstancesDirectory(INSTANCES_DIRECTORY);
        instanceImageManager.setSwapSize(SWAP_SIZE);
        instanceImageManager.setIoNiceCommand(IONICE_COMMAND);
    }

    @Before
    public void setupInstance() {
        instance = new Instance();
        instance.setInstanceId(INSTANCE_ID);
        instance.setUserId(USERID);
        instance.setKernelId("kernelId");
        instance.setImageId("imageId");
    }

    @After
    public void after() throws Exception {
        FileUtils.deleteQuietly(new File(String.format("%s/%s/%s", INSTANCES_DIRECTORY, USERID, INSTANCE_ID)));
    }

    @Test
    public void shouldSetupPiCacheDirectoryAndInstanceDirectoryIfTheyDontExist() throws IOException {
        // setup
        PowerMockito.mockStatic(FileUtils.class);

        // act
        instanceImageManager.setupRequiredDirectories(instance);

        // assert
        PowerMockito.verifyStatic(times(2));
        FileUtils.forceMkdir(argThat(new ArgumentMatcher<File>() {
            @Override
            public boolean matches(Object argument) {
                File file = (File) argument;
                return file.getPath().equals(INSTANCES_DIRECTORY + "/pi/cache") || file.getPath().equals(INSTANCES_DIRECTORY + "/" + USERID + "/" + INSTANCE_ID);
            }
        }));
    }

    @Test
    public void shouldTellImageLoadertoLoadImage() throws IOException {
        // setup
        PowerMockito.mockStatic(FileUtils.class);
        String savedImageFileName = "savedImageFileName";
        when(imageLoader.saveImage(INSTANCES_DIRECTORY, INSTANCES_DIRECTORY + "/pi/cache")).thenReturn(savedImageFileName);

        // act
        instanceImageManager.prepareImage(instance, INSTANCES_DIRECTORY);

        // assert
        PowerMockito.verifyStatic();
        FileUtils.copyFileToDirectory(new File(savedImageFileName), new File(INSTANCES_DIRECTORY + "/" + USERID + "/" + INSTANCE_ID));
    }

    @Test
    public void shouldReturnImagePathAfterPrepareImage() throws IOException {
        // setup
        String imagePath = "http://localhost/image.file";
        PowerMockito.mockStatic(FileUtils.class);
        String savedImageFileName = "savedImageFileName";
        when(imageLoader.saveImage(imagePath, INSTANCES_DIRECTORY + "/pi/cache")).thenReturn(savedImageFileName);

        // act
        String actualInstanceImagePath = instanceImageManager.prepareImage(instance, imagePath);

        // assert
        assertEquals(String.format("%s/%s/%s/%s", INSTANCES_DIRECTORY, USERID, INSTANCE_ID, savedImageFileName), actualInstanceImagePath);
    }

    @Test
    public void shouldEmbedKeyInImage() throws IOException {
        // setup
        PowerMockito.mockStatic(FileUtils.class);
        String keyName = "key.name";
        final String instanceImagePath = "/state/partition1/pi/instances/owner/i-123/image.file";

        // act
        instanceImageManager.embedKey(keyName, instanceImagePath);

        // assert
        verify(this.commandRunner).run(argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                String command = (String) argument;
                String[] split = command.split(" ");
                if (split.length != 3)
                    return false;
                if (!EMBED_KEY_PATH.equals(split[0]))
                    return false;
                if (!instanceImagePath.equals(split[1]))
                    return false;
                if (!split[2].startsWith(System.getProperty("java.io.tmpdir") + "/sckey"))
                    return false;
                return true;
            }
        }));
    }

    @Test
    public void shouldCreateSwapPartitionWithDefaultSizeAndFormatIt() {
        // act
        instanceImageManager.setupSwapPartition(instance);

        // assert
        verify(this.commandRunner).runInShell(String.format("ionice -c3 dd bs=1M count=0 seek=%d if=/dev/zero of=%s/%s/%s/swap 2>/dev/null", SWAP_SIZE, INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId()));
        verify(this.commandRunner).runInShell(String.format("mkswap %s/%s/%s/swap >/dev/null", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId()));
    }

    @Test
    public void shouldCreateSwapPartitionWithSwapSizeSetAndFormatIt() {
        // setup
        final long newSwapSize = 1024;
        instanceImageManager.setSwapSize(newSwapSize);

        // act
        instanceImageManager.setupSwapPartition(instance);

        // assert
        verify(this.commandRunner).runInShell(String.format("ionice -c3 dd bs=1M count=0 seek=%s if=/dev/zero of=%s/%s/%s/swap 2>/dev/null", newSwapSize, INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId()));
        verify(this.commandRunner).runInShell(String.format("mkswap %s/%s/%s/swap >/dev/null", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId()));
    }

    @Test
    public void shouldCreateEphemeralDiskAndFormatIt() throws Exception {
        // setup
        String instanceImagePath = String.format("%s/image.file", INSTANCES_DIRECTORY);
        File mockFile = mock(File.class);
        when(mockFile.length()).thenReturn(100L * 1024 * 1024);
        PowerMockito.whenNew(File.class).withArguments(instanceImagePath).thenReturn(mockFile);
        File realFile = new File(String.format("%s/%s/%s", INSTANCES_DIRECTORY, USERID, INSTANCE_ID));
        PowerMockito.whenNew(File.class).withArguments(String.format("%s/%s/%s", INSTANCES_DIRECTORY, USERID, INSTANCE_ID)).thenReturn(realFile);

        // act
        instanceImageManager.setupEphemeralPartition(instance, instanceImagePath, 1024);

        // assert
        verify(commandRunner).runInShell(String.format("ionice -c3 dd bs=1M count=0 seek=924 if=/dev/zero of=%s/%s/%s/ephemeral 2>/dev/null", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId()));
        verify(commandRunner).runInShell(String.format("nice -n +10 ionice -c3 mkfs.ext3 -F %s/%s/%s/ephemeral >/dev/null 2>&1", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId()));
    }

    @Test
    public void shouldGenerateLibvirtXmlForInstance() {
        // setup
        ImagePlatform platform = ImagePlatform.linux;
        String privateMacAddress = "d0:0d:ab:cd";
        String bridgeDevice = "pibr11";
        String memory = "128";
        int vcpus = 2;
        instance.setMemoryInKB(memory);
        instance.setVcpus(vcpus);
        instance.setPrivateMacAddress(privateMacAddress);

        // act
        instanceImageManager.generateLibvirtXml(instance, platform, true, bridgeDevice);

        // assert
        verify(libvirtManager).generateLibvirtXml(instance, platform, String.format("%s/%s/%s", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId()), true, privateMacAddress, bridgeDevice, memory, String.valueOf(vcpus));
    }

    @Test
    public void shouldStartInstanceWithLibvirtXml() {
        String libvirtXml = "<domain>name</domain";

        // act
        instanceImageManager.startInstance(instance, libvirtXml);

        // assert
        verify(libvirtManager).startInstance(libvirtXml, INSTANCE_ID, String.format("%s/%s/%s", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId()));
    }

    @Test
    public void shouldStopLibvirtInstance() {
        // setup
        Domain domain = mock(Domain.class);
        when(libvirtManager.lookupInstance(INSTANCE_ID)).thenReturn(domain);

        // act
        instanceImageManager.stopInstance(instance);

        // assert
        verify(libvirtManager).lookupInstance(INSTANCE_ID);
        verify(libvirtManager).destroyInstance(domain);
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowLibvirtManagerExceptionIfUnableToFindInstance() {
        // setup
        doThrow(new LibvirtManagerException("Unable to find instance:" + INSTANCE_ID)).when(libvirtManager).lookupInstance(INSTANCE_ID);

        // act
        instanceImageManager.stopInstance(instance);
    }

    @Test
    public void shouldDestroyLibvirtInstance() {
        // setup
        Domain domain = mock(Domain.class);
        when(libvirtManager.lookupInstance(INSTANCE_ID)).thenReturn(domain);

        // act
        instanceImageManager.stopInstance(instance);

        // assert
        verify(libvirtManager).lookupInstance(INSTANCE_ID);
        verify(libvirtManager).destroyInstance(domain);
    }

    @Test
    public void shouldRebootLibvirtInstance() {
        // setup
        Domain domain = mock(Domain.class);
        when(libvirtManager.lookupInstance(INSTANCE_ID)).thenReturn(domain);

        // act
        instanceImageManager.rebootInstance(instance);

        // assert
        verify(libvirtManager).lookupInstance(INSTANCE_ID);
        verify(libvirtManager).reboot(domain);
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowLibvirtManagerExceptionIfUnableToFindInstanceWhenRebooting() {
        // setup
        doThrow(new LibvirtManagerException("Unable to find instance:" + INSTANCE_ID)).when(libvirtManager).lookupInstance(INSTANCE_ID);

        // act
        instanceImageManager.rebootInstance(instance);

        // assert
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowLibvirtManagerExceptionIfInstanceFailedToStop() {
        // setup
        Domain domain = mock(Domain.class);
        when(libvirtManager.lookupInstance(INSTANCE_ID)).thenReturn(domain);
        doThrow(new LibvirtManagerException("Unable to stop instance:" + INSTANCE_ID)).when(libvirtManager).destroyInstance(domain);

        // act
        instanceImageManager.stopInstance(instance);
    }

    @Test
    public void shouldNotDeleteInstanceArtifacts() throws Exception {
        // setup
        PowerMockito.mockStatic(FileUtils.class);
        when(fileManager.fileExists(anyString())).thenReturn(true);
        instanceImageManager.setRetainFaileInstanceArtifacts(true);
        instance.setState(InstanceState.FAILED);

        // act
        instanceImageManager.stopInstance(instance);

        // verify
        PowerMockito.verifyStatic(never());
        FileUtils.deleteDirectory(new File(String.format("%s/%s/%s", INSTANCES_DIRECTORY, USERID, INSTANCE_ID)));
        verify(commandRunner, never()).runNicelyInShell(anyString());
    }

    @Test
    public void shouldRenameInstanceDirectoryWhenStoppingInstance() {
        // setup
        String instanceDirectoryName = String.format("%s/%s/%s", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId());
        String archivedDirectoryName = String.format("%s/%s/%s-terminated-%s", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId(), new SimpleDateFormat("yyyyMMdd").format(new Date()));
        when(fileManager.fileExists(instanceDirectoryName)).thenReturn(true);
        when(fileManager.fileExists(archivedDirectoryName)).thenReturn(false);

        // act
        instanceImageManager.stopInstance(instance);

        // assert
        verify(commandRunner).run(String.format("mv %s %s", instanceDirectoryName, archivedDirectoryName));
    }

    @Test
    public void shouldNotRenameInstanceDirectoryWhenStoppingInstanceIfItHasBeenRenamedByAnotherThread() throws Exception {
        // setup
        String instanceDirectoryName = String.format("%s/%s/%s", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId());
        String archivedDirectoryName = String.format("%s/%s/%s-terminated-%s", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId(), new SimpleDateFormat("yyyyMMdd").format(new Date()));
        when(fileManager.fileExists(instanceDirectoryName)).thenReturn(false);
        when(fileManager.fileExists(archivedDirectoryName)).thenReturn(false);

        // act
        instanceImageManager.stopInstance(instance);

        // assert
        verify(commandRunner, never()).run(String.format("mv %s %s", instanceDirectoryName, archivedDirectoryName));
    }

    @Test
    public void shouldNotRenameInstanceDirectoryWhenStoppingInstanceIfArchiveAlreadyExists() throws Exception {
        // setup
        String instanceDirectoryName = String.format("%s/%s/%s", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId());
        String archivedDirectoryName = String.format("%s/%s/%s-terminated-%s", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId(), new SimpleDateFormat("yyyyMMdd").format(new Date()));
        when(fileManager.fileExists(instanceDirectoryName)).thenReturn(true);
        when(fileManager.fileExists(archivedDirectoryName)).thenReturn(true);

        // act
        instanceImageManager.stopInstance(instance);

        // assert
        verify(commandRunner, never()).run(String.format("mv %s %s", instanceDirectoryName, archivedDirectoryName));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentIfSourceImageIsNull() throws Exception {
        instanceImageManager.prepareImage(instance, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentIfSourceImageIsBlank() throws Exception {
        instanceImageManager.prepareImage(instance, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfVerificationFailed() {
        // setup
        String path = "";
        String message = "Not found";

        // act
        instanceImageManager.verify(path, message);
    }

    @Test
    public void shouldGetAllRunningInstances() throws Exception {
        // setup
        Collection<String> domains = Arrays.asList(new String[] { "domain1", "domain2" });
        when(libvirtManager.getAllRunningInstances()).thenReturn(domains);

        // act
        Collection<String> result = instanceImageManager.getRunningInstances();

        // assert
        assertEquals(2, result.size());
        for (int i = 1; i <= 2; i++)
            assertTrue(result.contains("domain" + i));
    }

    @Test
    public void shouldGetAllCrashedInstances() throws Exception {
        // setup
        Collection<String> domains = Arrays.asList(new String[] { "domain1", "domain2" });
        when(libvirtManager.getAllCrashedInstances()).thenReturn(domains);

        // act
        Collection<String> result = instanceImageManager.getCrashedInstances();

        // assert
        assertEquals(2, result.size());
        for (int i = 1; i <= 2; i++)
            assertTrue(result.contains("domain" + i));
    }

    @Test
    public void testGetDomainIdForInstance() throws LibvirtException {
        // setup
        Integer domainId = 1234;
        when(domain.getID()).thenReturn(domainId);

        // act
        long result = instanceImageManager.getDomainIdForInstance(INSTANCE_ID);

        // assert
        assertEquals(domainId.longValue(), result);
    }

    @Test(expected = DomainNotFoundException.class)
    public void testGetDomainIdForInstanceNotFound() throws LibvirtException {
        // setup
        when(libvirtManager.lookupInstance(INSTANCE_ID)).thenReturn(null);

        // act
        instanceImageManager.getDomainIdForInstance(INSTANCE_ID);
    }

    @Test(expected = DomainNotFoundException.class)
    public void testGetDomainIdForInstanceBadId() throws LibvirtException {
        // setup
        LibvirtException mockException = mock(LibvirtException.class);
        when(domain.getID()).thenThrow(mockException);

        // act
        instanceImageManager.getDomainIdForInstance(INSTANCE_ID);
    }

    @Test
    public void testIsInstanceCrashed() throws Exception {
        // setup
        when(libvirtManager.isInstanceCrashed(INSTANCE_ID)).thenReturn(true);
        // act
        // assert

        assertTrue(instanceImageManager.isInstanceCrashed(instance));
    }

    @Test
    public void shouldSetEphemeralDiskSizeTo50MIfItIsZero() throws Exception {
        String instanceImagePath = String.format("%s/image.file", INSTANCES_DIRECTORY);
        File mockFile = mock(File.class);
        when(mockFile.length()).thenReturn(100L * 1024 * 1024);
        PowerMockito.whenNew(File.class).withArguments(instanceImagePath).thenReturn(mockFile);

        File realFile = new File(String.format("%s/%s/%s", INSTANCES_DIRECTORY, USERID, INSTANCE_ID));
        PowerMockito.whenNew(File.class).withArguments(String.format("%s/%s/%s", INSTANCES_DIRECTORY, USERID, INSTANCE_ID)).thenReturn(realFile);

        // act
        instanceImageManager.setupEphemeralPartition(instance, instanceImagePath, 100);

        // assert
        verify(commandRunner).runInShell(String.format("ionice -c3 dd bs=1M count=0 seek=50 if=/dev/zero of=%s/%s/%s/ephemeral 2>/dev/null", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId()));
        verify(commandRunner).runInShell(String.format("nice -n +10 ionice -c3 mkfs.ext3 -F %s/%s/%s/ephemeral >/dev/null 2>&1", INSTANCES_DIRECTORY, instance.getUserId(), instance.getInstanceId()));
    }
}
