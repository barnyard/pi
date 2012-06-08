package com.bt.pi.integration.applications;

import javax.annotation.Resource;

import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.application.activation.RegionScopedSharedRecordConditionalApplicationActivator;

public class RegionScopedTestApplication extends TestApplication {
    @Resource(type = RegionScopedSharedRecordConditionalApplicationActivator.class)
    public void setApplicationActivator(ApplicationActivator applicationActivator) {
        super.setApplicationActivator(applicationActivator);
    }
}
