package com.bt.pi.integration.applications;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.payload.EchoPayload;

public abstract class TestApplication extends KoalaPastryApplicationBase {
    private static final int START_TIMEOUT_SEC = 5;
    private static final int ACTIVATION_CHECK_INTERVAL_SECS = 10;
    private static final Log LOG = LogFactory.getLog(TestApplication.class);

    private String applicationName;
    private ApplicationActivator applicationActivator;
    private List<String> preferablyExcludedApplications;

    public TestApplication() {
        preferablyExcludedApplications = new ArrayList<String>();
    }

    protected void setApplicationActivator(ApplicationActivator anApplicationActivator) {
        this.applicationActivator = anApplicationActivator;
    }

    @Override
    protected void onApplicationStarting() {
        LOG.debug("<><><><><><> onApplicationStarting:" + getApplicationName());
        super.onApplicationStarting();
    }

    @Override
    public boolean becomeActive() {
        LOG.debug("<><><><><><> becomeActive:" + getApplicationName());
        return true;
    }

    @Override
    public void deliver(PId id, ReceivedMessageContext messageContext) {
        if (messageContext.getReceivedEntity() instanceof EchoPayload) {
            LOG.debug(String.format("%s message received.", getApplicationName()));
        }
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
        System.err.println("Node: " + nodeId + " has left the ring.");

    }

    @Override
    public void becomePassive() {
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
    }

    @Override
    public int getActivationCheckPeriodSecs() {
        return ACTIVATION_CHECK_INTERVAL_SECS;
    }

    @Override
    public long getStartTimeout() {
        return START_TIMEOUT_SEC;
    }

    @Override
    public TimeUnit getStartTimeoutUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public List<String> getPreferablyExcludedApplications() {
        return preferablyExcludedApplications;
    }

    public void setPreferablyExcludedApplicationList(String preferablyExcludedApplications) {
        this.preferablyExcludedApplications = Arrays.asList(preferablyExcludedApplications.split(","));
    }
}
