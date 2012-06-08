package com.bt.pi.app.instancemanager.images;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;

@Component
public class SolarisPlatformBuilder extends PlatformBuilder {
    private static final Log LOG = LogFactory.getLog(LinuxPlatformBuilder.class);

    public SolarisPlatformBuilder() {
        super();
    }

    @Override
    public void build(Instance instance, String key) {
        try {
            getInstanceImageManager().verify(instance.getSourceImagePath(), "Image file is null or empty");
            getInstanceImageManager().verify(instance.getSourceKernelPath(), "Kernel file is null or empty");
            getInstanceImageManager().verify(instance.getSourceRamdiskPath(), "Ramdisk file is null or empty");

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

            getInstanceImageManager().setupEphemeralPartition(instance, instanceImagePath, instance.getImageSizeInMB());

            String libvirtXml = getInstanceImageManager().generateLibvirtXml(instance, ImagePlatform.opensolaris, useRamdisk, VlanAddressUtils.getBridgeNameForVlan(instance.getVlanId()));
            this.getInstanceImageManager().startInstance(instance, libvirtXml);
        } catch (IOException e) {
            throw new PlatformBuilderException("Unable to build instance", e);
        }
    }
}
