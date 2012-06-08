package com.bt.pi.app.instancemanager.images;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;

public class LinuxPlatformBuilderTest {

    private LinuxPlatformBuilder linuxPlatformBuilder;
    private String imagePath = "image.path";
    private String kernelPath = "kernel.path";
    private String ramdiskPath = "ramdisk.path";
    private String key = "key";
    private String ownerName = "owner.name";
    private String instanceId = "i-123";
    private int imageSizeInMB = 1024;
    private String privateMacAddress = "d0:0d:45:1a:09:16";
    private String memory = "1048576";
    private int vcpus = 2;
    private int vlanId = 3;

    private InstanceImageManager instanceImageManager;
    private Instance instance;

    @Before
    public void setUp() throws Exception {
        instanceImageManager = mock(InstanceImageManager.class);

        linuxPlatformBuilder = new LinuxPlatformBuilder();
        linuxPlatformBuilder.setInstanceImageManager(instanceImageManager);
    }

    @Before
    public void setupInstance() {
        instance = new Instance();
        instance.setSourceImagePath(imagePath);
        instance.setSourceKernelPath(kernelPath);
        instance.setSourceRamdiskPath(ramdiskPath);
        instance.setUserId(ownerName);
        instance.setInstanceId(instanceId);
        instance.setImageSizeInMB(imageSizeInMB);
        instance.setPrivateMacAddress(privateMacAddress);
        instance.setMemoryInKB(memory);
        instance.setVcpus(vcpus);
        instance.setVlanId(vlanId);
    }

    @Test
    public void shouldPrepareImageKernelAndRamdiskImagesForLinuxImage() throws IOException {
        // setup

        // act
        linuxPlatformBuilder.build(instance, null);

        // assert
        verify(this.instanceImageManager).prepareImage(instance, imagePath);
        verify(this.instanceImageManager).prepareImage(instance, kernelPath);
        verify(this.instanceImageManager).prepareImage(instance, kernelPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfImagePathIsNotPassed() {
        // setup
        instance.setSourceImagePath("");
        doThrow(new IllegalArgumentException()).when(instanceImageManager).verify(isA(String.class), isA(String.class));

        // act
        linuxPlatformBuilder.build(instance, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfKernelPathIsNotPassed() {
        // setup
        instance.setSourceKernelPath("");
        doThrow(new IllegalArgumentException()).when(instanceImageManager).verify(isA(String.class), isA(String.class));

        // act
        linuxPlatformBuilder.build(instance, null);
    }

    @Test
    public void shouldEmbedKeyForLinuxPlatform() throws IOException {
        // setup
        String instanceImagePath = "/state/partition1/instances/owner/i-123/image.file";
        when(this.instanceImageManager.prepareImage(eq(instance), isA(String.class))).thenReturn(instanceImagePath);

        // act
        linuxPlatformBuilder.build(instance, key);

        // assert
        verify(this.instanceImageManager).embedKey(key, instanceImagePath);
    }

    @Test
    public void shouldPrepareEphemeralDiskAndSwapDiskForLinuxPlatform() throws IOException {
        // setup
        String instanceImagePath = "/etc/instances/image.file";
        when(this.instanceImageManager.prepareImage(eq(instance), isA(String.class))).thenReturn(instanceImagePath);

        // act
        linuxPlatformBuilder.build(instance, null);

        // assert
        verify(this.instanceImageManager).setupSwapPartition(instance);
        verify(this.instanceImageManager).setupEphemeralPartition(instance, instanceImagePath, imageSizeInMB);
    }

    @Test
    public void shouldGenerateLibvirtXmlWithTheRequiredPlatform() {
        // setup

        // act
        linuxPlatformBuilder.build(instance, null);

        // assert
        verify(this.instanceImageManager).generateLibvirtXml(instance, ImagePlatform.linux, true, VlanAddressUtils.getBridgeNameForVlan(vlanId));
    }

    @Test
    public void shouldStartInstanceWithLibvirtXml() {
        // setup
        String libvirtXml = "<domain>name</domain>";
        when(this.instanceImageManager.generateLibvirtXml(instance, ImagePlatform.linux, true, VlanAddressUtils.getBridgeNameForVlan(vlanId))).thenReturn(libvirtXml);

        // act
        linuxPlatformBuilder.build(instance, null);

        // assert
        verify(this.instanceImageManager).startInstance(instance, libvirtXml);
    }
}
