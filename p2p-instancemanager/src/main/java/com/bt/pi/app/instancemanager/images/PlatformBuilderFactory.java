/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.images;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.images.platform.ImagePlatform;

@Component
public class PlatformBuilderFactory {
    private LinuxPlatformBuilder linuxPlatformBuilder;
    private WindowsPlatformBuilder windowsPlatformBuilder;
    private SolarisPlatformBuilder solarisPlatformBuilder;

    public PlatformBuilderFactory() {
    }

    public PlatformBuilder getFor(ImagePlatform platform) {

        if (platform == null) {
            return linuxPlatformBuilder;
        } else if (platform.equals(ImagePlatform.windows)) {
            return windowsPlatformBuilder;
        } else if (platform.equals(ImagePlatform.opensolaris)) {
            return solarisPlatformBuilder;
        } else {
            return linuxPlatformBuilder;
        }
    }

    @Resource
    public void setLinuxPlatformBuilder(LinuxPlatformBuilder aLinuxPlatformBuilder) {
        this.linuxPlatformBuilder = aLinuxPlatformBuilder;
    }

    @Resource
    public void setWindowsPlatformBuilder(WindowsPlatformBuilder aWindowsPlatformBuilder) {
        this.windowsPlatformBuilder = aWindowsPlatformBuilder;
    }

    @Resource
    public void setSolarisPlatformBuilder(SolarisPlatformBuilder aSolarisPlatformBuilder) {
        this.solarisPlatformBuilder = aSolarisPlatformBuilder;

    }
}
