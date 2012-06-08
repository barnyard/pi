package com.bt.pi.api.service;

import javax.annotation.Resource;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.MachineType;

@ManagedResource(description = "Service to allow registration of kernels and ramdisks via JMX", objectName = "bean:name=jmxImageService")
@Component
public class JmxImageService {
    private static final String USERNAME = "userName";
    private static final String DESCRIPTION1 = "the user who uploaded the bundle";
    private static final String DESCRIPTION2 = "<bucket name>/<manifest file name>";
    private static final String MANIFEST_LOCATION = "manifestLocation";
    private static final String IMAGE_ID = "imageId";
    private static final String DESCRIPTION3 = "the ID of the image to de-register";

    @Resource
    private ManagementImageService imageService;

    public JmxImageService() {
        this.imageService = null;
    }

    @ManagedOperation(description = "Register a machine kernel")
    @ManagedOperationParameters({ @ManagedOperationParameter(name = USERNAME, description = DESCRIPTION1), @ManagedOperationParameter(name = MANIFEST_LOCATION, description = DESCRIPTION2) })
    public String registerKernel(String userId, String imageManifestLocation) {
        return this.imageService.registerImage(userId, imageManifestLocation, MachineType.KERNEL);
    }

    @ManagedOperation(description = "Register a machine ramdisk")
    @ManagedOperationParameters({ @ManagedOperationParameter(name = USERNAME, description = DESCRIPTION1), @ManagedOperationParameter(name = MANIFEST_LOCATION, description = DESCRIPTION2) })
    public String registerRamdisk(String userId, String imageManifestLocation) {
        return this.imageService.registerImage(userId, imageManifestLocation, MachineType.RAMDISK);
    }

    @ManagedOperation(description = "De-register a machine kernel")
    @ManagedOperationParameters({ @ManagedOperationParameter(name = USERNAME, description = DESCRIPTION1), @ManagedOperationParameter(name = IMAGE_ID, description = DESCRIPTION3) })
    public boolean deregisterKernel(String userId, String imageId) {
        return this.imageService.deregisterImage(userId, imageId, MachineType.KERNEL);
    }

    @ManagedOperation(description = "De-register a machine ramdisk")
    @ManagedOperationParameters({ @ManagedOperationParameter(name = USERNAME, description = DESCRIPTION1), @ManagedOperationParameter(name = IMAGE_ID, description = DESCRIPTION3) })
    public boolean deregisterRamdisk(String userId, String imageId) {
        return this.imageService.deregisterImage(userId, imageId, MachineType.RAMDISK);
    }
}
