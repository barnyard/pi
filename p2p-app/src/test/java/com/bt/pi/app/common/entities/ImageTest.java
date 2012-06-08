package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.core.parser.KoalaJsonParser;

public class ImageTest {
    private String imageId = "img";
    private String kernelId = "ker";
    private String ramdiskId = "ram";
    private String manifestLocation = "man";
    private String ownerId = "owner";
    private String architecture = "i386";
    private boolean isPublic = true;
    private ImagePlatform platform = ImagePlatform.opensolaris;
    private ImageState state = ImageState.AVAILABLE;
    private MachineType machineType = MachineType.KERNEL;

    @Test
    public void shouldLoadDefaultConstruct() {
        // setup

        // act
        Image image = new Image();
        image.setImageId(imageId);
        image.setKernelId(kernelId);
        image.setManifestLocation(manifestLocation);
        image.setOwnerId(ownerId);
        image.setRamdiskId(ramdiskId);
        image.setArchitecture(architecture);
        image.setPublic(isPublic);
        image.setPlatform(platform);
        image.setState(state);
        image.setMachineType(MachineType.KERNEL);

        // assert
        assertEquals(imageId, image.getImageId());
        assertEquals(kernelId, image.getKernelId());
        assertEquals(ramdiskId, image.getRamdiskId());
        assertEquals(manifestLocation, image.getManifestLocation());
        assertEquals(ownerId, image.getOwnerId());
        assertEquals(architecture, image.getArchitecture());
        assertEquals(isPublic, image.isPublic());
        assertEquals(platform, image.getPlatform());
        assertEquals(state, image.getState());
        assertEquals(machineType, image.getMachineType());
    }

    @Test
    public void shouldLoadOverloadConstructor() {
        // setup
        // act
        Image image = new Image(imageId, kernelId, ramdiskId, manifestLocation, ownerId, architecture, platform, isPublic, MachineType.KERNEL);

        // assert
        assertEquals(imageId, image.getImageId());
        assertEquals(kernelId, image.getKernelId());
        assertEquals(ramdiskId, image.getRamdiskId());
        assertEquals(manifestLocation, image.getManifestLocation());
        assertEquals(ownerId, image.getOwnerId());
        assertEquals(architecture, image.getArchitecture());
        assertEquals(isPublic, image.isPublic());
        assertEquals(machineType, image.getMachineType());
    }

    @Test
    public void shouldJsonRoundTrip() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();
        Image image = new Image(imageId, kernelId, ramdiskId, manifestLocation, ownerId, architecture, platform, isPublic, MachineType.KERNEL);

        // act
        String json = koalaJsonParser.getJson(image);
        Image reverse = (Image) koalaJsonParser.getObject(json, Image.class);

        // assert
        assertEquals(image, reverse);
    }
}
