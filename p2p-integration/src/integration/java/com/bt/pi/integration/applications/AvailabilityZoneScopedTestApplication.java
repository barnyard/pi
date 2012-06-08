package com.bt.pi.integration.applications;

import javax.annotation.Resource;

import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedSharedRecordConditionalApplicationActivator;

public class AvailabilityZoneScopedTestApplication extends TestApplication {
    @Resource(type = AvailabilityZoneScopedSharedRecordConditionalApplicationActivator.class)
    public void setApplicationActivator(ApplicationActivator applicationActivator) {
        super.setApplicationActivator(applicationActivator);
    }
}
