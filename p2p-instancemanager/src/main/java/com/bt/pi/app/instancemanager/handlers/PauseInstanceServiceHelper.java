package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAction;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class PauseInstanceServiceHelper extends AbstractHandler {
    public static final int RETRIES = 5;
    private static final Log LOG = LogFactory.getLog(PauseInstanceServiceHelper.class);
    private MessageContextFactory messageContextFactory;

    public PauseInstanceServiceHelper() {
        this.messageContextFactory = null;
    }

    @Resource(type = ReportingApplication.class)
    public void setMessageContextFactory(MessageContextFactory aMessageContextFactory) {
        this.messageContextFactory = aMessageContextFactory;
    }

    public void pauseInstance(final Instance instance) {
        LOG.debug(String.format("pauseInstance(%s)", instance));
        addInstanceToPauseRetryQueue(instance);
        sendMessageToInstanceManager(instance, InstanceAction.PAUSE);
    }

    public void unPauseInstance(final Instance instance) {
        LOG.debug(String.format("unpauseInstance(%s)", instance));
        sendMessageToInstanceManager(instance, InstanceAction.UNPAUSE);
    }

    private void addInstanceToPauseRetryQueue(Instance instance) {
        int globalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(instance.getInstanceId());
        PId pauseInstanceQueueId = getPiIdBuilder().getPId(PiQueue.PAUSE_INSTANCE.getUrl()).forGlobalAvailablityZoneCode(globalAvzCode);
        getTaskProcessingQueueHelper().addUrlToQueue(pauseInstanceQueueId, instance.getUrl(), RETRIES);
    }

    private void sendMessageToInstanceManager(final Instance instance, InstanceAction action) {
        PId nodeId = getPiIdBuilder().getNodeIdFromNodeId(instance.getNodeId());
        instance.setActionRequired(action);
        MessageContext messageContext = messageContextFactory.newMessageContext();
        messageContext.routePiMessageToApplication(nodeId, EntityMethod.UPDATE, instance, InstanceManagerApplication.APPLICATION_NAME);
    }
}
