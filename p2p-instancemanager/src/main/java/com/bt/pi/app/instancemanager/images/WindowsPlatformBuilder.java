/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.images;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;

@Component
public class WindowsPlatformBuilder extends PlatformBuilder {
    public WindowsPlatformBuilder() {
        super();
    }

    @Override
    public void build(Instance instance, String key) {
        try {
            getInstanceImageManager().verify(instance.getSourceImagePath(), "Image Path is null or empty");
            getInstanceImageManager().setupRequiredDirectories(instance);
            getInstanceImageManager().prepareImage(instance, instance.getSourceImagePath());

            getInstanceImageManager().setupEphemeralPartition(instance, instance.getSourceImagePath(), instance.getImageSizeInMB());

            String libvirtXml = getInstanceImageManager().generateLibvirtXml(instance, ImagePlatform.windows, false, VlanAddressUtils.getBridgeNameForVlan(instance.getVlanId()));

            getInstanceImageManager().startInstance(instance, libvirtXml);
        } catch (IOException e) {
            throw new PlatformBuilderException("Unable to build instance", e);
        }
    }
}
