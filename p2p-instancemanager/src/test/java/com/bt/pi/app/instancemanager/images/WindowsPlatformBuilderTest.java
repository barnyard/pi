package com.bt.pi.app.instancemanager.images;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;

public class WindowsPlatformBuilderTest {

    WindowsPlatformBuilder windowsPlatformBuilder;
    InstanceImageManager instanceImageManager;
    Instance instance;
    int vlanId = 6;

    @Before
    public void setUp() throws Exception {
        instance = new Instance();
        instance.setSourceImagePath("i-123");
        instance.setVlanId(vlanId);

        instanceImageManager = mock(InstanceImageManager.class);

        windowsPlatformBuilder = new WindowsPlatformBuilder();
        windowsPlatformBuilder.setInstanceImageManager(instanceImageManager);
    }

    @Test
    public void shouldVerifyWindowsSourceImage() {
        // act
        windowsPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).verify(eq(instance.getSourceImagePath()), isA(String.class));
    }

    @Test
    public void shouldCreateRequiredDirectoriesForInstance() {
        // setup

        // act
        windowsPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).setupRequiredDirectories(instance);
    }

    @Test
    public void shouldPrepareImageForWindows() throws Exception {
        // setup

        // act
        windowsPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).prepareImage(instance, instance.getSourceImagePath());
    }

    @Test(expected = PlatformBuilderException.class)
    public void shouldThrowPlatformBuilderExceptionIfUnableToBuildWindowsImage() throws Exception {
        // setup
        doThrow(new PlatformBuilderException("Unable to Build Platform", new Exception())).when(this.instanceImageManager).prepareImage(isA(Instance.class), isA(String.class));

        // act
        windowsPlatformBuilder.build(instance, null);
    }

    @Test
    public void shouldSetupEphemeralPartitionForWindows() {
        // setup

        // act
        windowsPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).setupEphemeralPartition(instance, instance.getSourceImagePath(), instance.getImageSizeInMB());
    }

    @Test
    public void shouldGenerateLibvirtXmlForWindowsImage() throws Exception {
        // setup
        String expectedMacAddress = "d0:0d:ab:cd";
        instance.setPrivateMacAddress(expectedMacAddress);

        // act
        windowsPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).generateLibvirtXml(instance, ImagePlatform.windows, false, VlanAddressUtils.getBridgeNameForVlan(instance.getVlanId()));
    }

    @Test
    public void shouldStartWindowsInstanceWithGeneratedLibvirtXml() throws Exception {
        String libvirtXml = "<xml>abc</xml>";
        // setup
        when(instanceImageManager.generateLibvirtXml(instance, ImagePlatform.windows, false, VlanAddressUtils.getBridgeNameForVlan(instance.getVlanId()))).thenReturn(libvirtXml);

        // act
        windowsPlatformBuilder.build(instance, null);

        // assert
        verify(instanceImageManager).startInstance(instance, libvirtXml);
    }
}
