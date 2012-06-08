/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.images;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;

@Component
public class LinuxPlatformBuilder extends PlatformBuilder {
    private static final Log LOG = LogFactory.getLog(LinuxPlatformBuilder.class);

    public LinuxPlatformBuilder() {
        super();
    }

    @Override
    public void build(Instance instance, String key) {
        try {
            getInstanceImageManager().verify(instance.getSourceImagePath(), "Image Path is null or empty");
            getInstanceImageManager().verify(instance.getSourceKernelPath(), "Kernel Path is null or empty");

            getInstanceImageManager().setupRequiredDirectories(instance);

            String instanceImagePath = getInstanceImageManager().prepareImage(instance, instance.getSourceImagePath());
            getInstanceImageManager().prepareImage(instance, instance.getSourceKernelPath());

            boolean useRamdisk = false;
            try {
                getInstanceImageManager().prepareImage(instance, instance.getSourceRamdiskPath());
                useRamdisk = true;
            } catch (IllegalArgumentException e) {
                LOG.info(String.format("No ramdisk path provided while running instance: %s", instance.toString()));
            }

            getInstanceImageManager().embedKey(key, instanceImagePath);
            getInstanceImageManager().setupSwapPartition(instance);
            getInstanceImageManager().setupEphemeralPartition(instance, instanceImagePath, instance.getImageSizeInMB());

            String libvirtXml = getInstanceImageManager().generateLibvirtXml(instance, ImagePlatform.linux, useRamdisk, VlanAddressUtils.getBridgeNameForVlan(instance.getVlanId()));
            getInstanceImageManager().startInstance(instance, libvirtXml);
        } catch (IOException e) {
            throw new PlatformBuilderException("Unable to build instance", e);
        }
    }
}
