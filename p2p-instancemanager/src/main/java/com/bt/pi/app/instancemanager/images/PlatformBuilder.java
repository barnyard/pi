/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.images;

import javax.annotation.Resource;

import com.bt.pi.app.common.entities.Instance;

public abstract class PlatformBuilder {
    private InstanceImageManager instanceImageManager;

    public PlatformBuilder() {
        instanceImageManager = null;
    }

    @Resource
    public void setInstanceImageManager(InstanceImageManager anInstanceImageManager) {
        this.instanceImageManager = anInstanceImageManager;
    }

    protected InstanceImageManager getInstanceImageManager() {
        return instanceImageManager;
    }

    public abstract void build(Instance instance, String key);

}
