package com.bt.pi.app.instancemanager.images;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;

public class SolarisPlatformBuilderTest {

    InstanceImageManager instanceImageManager;
    SolarisPlatformBuilder solarisPlatformBuilder;
    Instance instance;
    int vlanId = 3;

    @Before
    public void setUp() throws Exception {
        instanceImageManager = mock(InstanceImageManager.class);
        instance = new Instance();
        instance.setVlanId(vlanId);

        solarisPlatformBuilder = new SolarisPlatformBuilder();
        solarisPlatformBuilder.setInstanceImageManager(instanceImageManager);
    }

    @Test
    public void shouldVerifyImageKernelAndRamdiskFile() {
        // setup
        instance.setSourceImagePath("image");
        instance.setSourceKernelPath("kernel");
        instance.setSourceRamdiskPath("ramdisk");

        // act
        solarisPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).verify(eq("image"), anyString());
        verify(instanceImageManager).verify(eq("kernel"), anyString());
        verify(instanceImageManager).verify(eq("ramdisk"), anyString());
    }

    @Test
    public void shouldCreateRequiredDirectoriesForSolarisImage() {
        // setup

        // act
        solarisPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).setupRequiredDirectories(instance);
    }

    @Test
    public void shouldPrepareImageForSolaris() throws Exception {
        // setup
        instance.setSourceImagePath("image");

        // act
        solarisPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).prepareImage(instance, "image");
    }

    @Test
    public void shouldPrepareKernelForSolaris() throws Exception {
        // setup
        instance.setSourceKernelPath("kernel");

        // act
        solarisPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).prepareImage(instance, "kernel");
    }

    @Test
    public void shouldPrepareRamdiskforSolaris() throws Exception {
        // setup
        instance.setSourceRamdiskPath("ramdisk");

        // act
        solarisPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).prepareImage(instance, "ramdisk");
    }

    @Test
    public void shouldSetupEphemeralPartitionForSolaris() throws Exception {
        // setup
        instance.setImageSizeInMB(100);
        when(instanceImageManager.prepareImage(eq(instance), anyString())).thenReturn("imagepath");

        // act
        solarisPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).setupEphemeralPartition(instance, "imagepath", 100);
    }

    @Test
    public void shouldGenerateLibvirtXmlForSolaris() {
        // setup
        String expectedMacAddress = "d0:0d:ab:cd";

        instance.setPrivateMacAddress(expectedMacAddress);

        // act
        solarisPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).generateLibvirtXml(instance, ImagePlatform.opensolaris, true, VlanAddressUtils.getBridgeNameForVlan(vlanId));
    }

    @Test
    public void shouldStartWindowsInstanceWithGeneratedLibvirtXml() throws Exception {
        // setup
        String libvirtXml = "<xml>abc</xml>";
        when(instanceImageManager.generateLibvirtXml(instance, ImagePlatform.opensolaris, true, VlanAddressUtils.getBridgeNameForVlan(vlanId))).thenReturn(libvirtXml);

        // act
        solarisPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).startInstance(instance, libvirtXml);
    }
}
