package com.bt.pi.integration.applications;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedSharedRecordConditionalApplicationActivator;
import com.bt.pi.core.id.PId;

@Component
public class Avz1Application extends KoalaPastryApplicationBase {
    public static final String APPLICATION_NAME = Avz1Application.class.getSimpleName();
    private static final Log LOG = LogFactory.getLog(Avz1Application.class);

    private ApplicationActivator applicationActivator;

    @Override
    public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
        // TODO Auto-generated method stub

    }

    @Resource(type = AvailabilityZoneScopedSharedRecordConditionalApplicationActivator.class)
    public void setApplicationActivator(ApplicationActivator anApplicationActivator) {
        applicationActivator = anApplicationActivator;
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean becomeActive() {
        LOG.debug("<><><><><><> becomeActive:" + APPLICATION_NAME);
        return true;
    }

    @Override
    public void becomePassive() {
        // TODO Auto-generated method stub

    }

    @Override
    public int getActivationCheckPeriodSecs() {
        return 10;
    }

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    @Override
    public long getStartTimeout() {
        return 300;
    }

    @Override
    public TimeUnit getStartTimeoutUnit() {
        return TimeUnit.SECONDS;
    }

}
