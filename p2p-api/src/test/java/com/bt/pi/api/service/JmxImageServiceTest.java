package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.MachineType;

@RunWith(MockitoJUnitRunner.class)
public class JmxImageServiceTest {
    @InjectMocks
    private JmxImageService jmxImageService = new JmxImageService();
    @Mock
    private ImageServiceImpl imageService;
    private String kernelId = "kernelId";
    private String ramdiskId = "ramdiskId";
    private String ownerId = "ownerId";
    private String imageManifestLocation = "imageManifestLocation";
    private String imageId = "imageId";

    @Before
    public void setUp() throws Exception {
        when(imageService.registerImage(ownerId, imageManifestLocation, MachineType.KERNEL)).thenReturn(kernelId);
        when(imageService.registerImage(ownerId, imageManifestLocation, MachineType.RAMDISK)).thenReturn(ramdiskId);
        when(imageService.deregisterImage(ownerId, imageId, MachineType.RAMDISK)).thenReturn(true);
        when(imageService.deregisterImage(ownerId, imageId, MachineType.KERNEL)).thenReturn(true);
    }

    @Test
    public void testRegisterKernel() {
        // act
        String result = this.jmxImageService.registerKernel(ownerId, imageManifestLocation);

        // assert
        assertEquals(kernelId, result);
    }

    @Test
    public void testRegisterRamdisk() {
        // act
        String result = this.jmxImageService.registerRamdisk(ownerId, imageManifestLocation);

        // assert
        assertEquals(ramdiskId, result);
    }

    @Test
    public void testDeregisterKernel() {
        // act
        boolean result = this.jmxImageService.deregisterKernel(ownerId, imageId);

        // assert
        assertTrue(result);
    }

    @Test
    public void testDeregisterRamdisk() {
        // act
        boolean result = this.jmxImageService.deregisterRamdisk(ownerId, imageId);

        // assert
        assertTrue(result);
    }
}
