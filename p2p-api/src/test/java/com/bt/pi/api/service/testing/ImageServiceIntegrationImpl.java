package com.bt.pi.api.service.testing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bt.pi.api.service.ManagementImageService;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.images.platform.ImagePlatform;

public class ImageServiceIntegrationImpl implements ManagementImageService {

    public boolean deregisterImage(String ownerId, String imageId) {
        return true;
    }

    public String registerImage(String ownerId, String imageManifestLocation) {
        return "kmi-123";
    }

    public Set<Image> describeImages(String string, List<String> imageIds) {
        Set<Image> images = new HashSet<Image>();
        images.add(new Image("kmi-111", "k-111", "r-111", "manifest", "userid", "architecture", ImagePlatform.linux, true, MachineType.KERNEL));
        images.add(new Image("kmi-222", "k-222", "r-111", "manifest", "userid", "architecture", ImagePlatform.linux, true, MachineType.KERNEL));
        return images;
    }

    @Override
    public boolean deregisterImageWithoutMachineTypeCheck(String ownerId, String imageId) {
        return false;
    }

    @Override
    public boolean deregisterImage(String userId, String imageId, MachineType machineType) {
        return false;
    }

    @Override
    public String registerImage(String userId, String imageManifestLocation, MachineType machineType) {
        return null;
    }
}
