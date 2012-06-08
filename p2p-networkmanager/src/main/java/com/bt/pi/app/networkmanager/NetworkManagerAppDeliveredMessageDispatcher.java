package com.bt.pi.app.networkmanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.networkmanager.handlers.AssociateAddressWithInstanceHandler;
import com.bt.pi.app.networkmanager.handlers.DisassociateAddressFromInstanceHandler;
import com.bt.pi.app.networkmanager.handlers.InstanceNetworkRefreshHandler;
import com.bt.pi.app.networkmanager.handlers.InstanceNetworkSetupHandler;
import com.bt.pi.app.networkmanager.handlers.InstanceNetworkTeardownHandler;
import com.bt.pi.app.networkmanager.handlers.SecurityGroupNetworkUpdateHandler;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;

@Component
public class NetworkManagerAppDeliveredMessageDispatcher {
    private static final Log LOG = LogFactory.getLog(NetworkManagerAppDeliveredMessageDispatcher.class);
    private static final String S_IGNORING_PI_MESSAGE_S_WITH_METHOD_S = "%s ignoring pi message %s with method %s";
    private InstanceNetworkSetupHandler instanceNetworkSetupHandler;
    private InstanceNetworkRefreshHandler instanceNetworkRefreshHandler;
    private InstanceNetworkTeardownHandler instanceNetworkTeardownHandler;
    private SecurityGroupNetworkUpdateHandler securityGroupNetworkUpdateHandler;
    private AssociateAddressWithInstanceHandler associateAddressWithInstanceHandler;
    private DisassociateAddressFromInstanceHandler disassociateAddressFromInstanceHandler;
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    private PiIdBuilder piIdBuilder;

    public NetworkManagerAppDeliveredMessageDispatcher() {
        instanceNetworkSetupHandler = null;
        instanceNetworkTeardownHandler = null;
        instanceNetworkRefreshHandler = null;
        securityGroupNetworkUpdateHandler = null;
        associateAddressWithInstanceHandler = null;
        disassociateAddressFromInstanceHandler = null;
        taskProcessingQueueHelper = null;
        piIdBuilder = null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setInstanceNetworkSetupHandler(InstanceNetworkSetupHandler aInstanceNetworkSetupHandler) {
        this.instanceNetworkSetupHandler = aInstanceNetworkSetupHandler;
    }

    @Resource
    public void setInstanceNetworkTeardownHandler(InstanceNetworkTeardownHandler aInstanceNetworkTeardownHandler) {
        this.instanceNetworkTeardownHandler = aInstanceNetworkTeardownHandler;
    }

    @Resource
    public void setInstanceNetworkRefreshHandler(InstanceNetworkRefreshHandler aInstanceNetworkRefreshHandler) {
        this.instanceNetworkRefreshHandler = aInstanceNetworkRefreshHandler;
    }

    @Resource
    public void setSecurityGroupNetworkUpdateHandler(SecurityGroupNetworkUpdateHandler aSecurityGroupNetworkUpdateHandler) {
        this.securityGroupNetworkUpdateHandler = aSecurityGroupNetworkUpdateHandler;
    }

    @Resource
    public void setAssociateAddressWithInstanceHandler(AssociateAddressWithInstanceHandler anAssociateAddressWithInstanceHandler) {
        this.associateAddressWithInstanceHandler = anAssociateAddressWithInstanceHandler;
    }

    @Resource
    public void setDisassociateAddressFromInstanceHandler(DisassociateAddressFromInstanceHandler aDisassociateAddressFromInstanceHandler) {
        this.disassociateAddressFromInstanceHandler = aDisassociateAddressFromInstanceHandler;
    }

    @Resource
    public void setTaskProcessingQueueHelper(TaskProcessingQueueHelper aTaskProcessingQueueHelper) {
        this.taskProcessingQueueHelper = aTaskProcessingQueueHelper;
    }

    public void dispatchToHandler(MessageContext messageContext, EntityMethod method, PiEntity receivedEntity) {
        LOG.debug(String.format("dispatch(%s, %s)", messageContext, receivedEntity));
        if (receivedEntity instanceof Instance) {
            handleInstance(messageContext, method, receivedEntity);
        } else if (receivedEntity instanceof SecurityGroup) {
            handleSecurityGroup(method, receivedEntity);
        } else if (receivedEntity instanceof PublicIpAddress) {
            handleAddress(method, receivedEntity);
        } else {
            LOG.info(String.format(S_IGNORING_PI_MESSAGE_S_WITH_METHOD_S, this.getClass().getSimpleName(), receivedEntity, method));
        }
    }

    private void handleSecurityGroup(EntityMethod method, PiEntity receivedEntity) {
        SecurityGroup securityGroup = (SecurityGroup) receivedEntity;
        if (method.equals(EntityMethod.UPDATE)) {
            securityGroupNetworkUpdateHandler.handle(method, securityGroup);
            taskProcessingQueueHelper.removeUrlFromQueue(piIdBuilder.getPiQueuePId(PiQueue.UPDATE_SECURITY_GROUP).forLocalScope(PiQueue.UPDATE_SECURITY_GROUP.getNodeScope()), securityGroup.getUrl());
        } else if (method.equals(EntityMethod.DELETE)) {
            securityGroupNetworkUpdateHandler.handle(method, securityGroup);
            taskProcessingQueueHelper.removeUrlFromQueue(piIdBuilder.getPiQueuePId(PiQueue.REMOVE_SECURITY_GROUP).forLocalScope(PiQueue.REMOVE_SECURITY_GROUP.getNodeScope()), securityGroup.getUrl());
        } else {
            LOG.info(String.format(S_IGNORING_PI_MESSAGE_S_WITH_METHOD_S, this.getClass().getSimpleName(), receivedEntity, method));
        }
    }

    private void handleAddress(EntityMethod method, PiEntity receivedEntity) {
        PublicIpAddress address = (PublicIpAddress) receivedEntity;
        if (method.equals(EntityMethod.CREATE)) {
            associateAddressWithInstanceHandler.handle(address);
        } else if (method.equals(EntityMethod.DELETE)) {
            disassociateAddressFromInstanceHandler.handle(address);
        } else {
            LOG.warn(String.format(S_IGNORING_PI_MESSAGE_S_WITH_METHOD_S, this.getClass().getSimpleName(), receivedEntity, method));
        }
    }

    private void handleInstance(MessageContext messageContext, EntityMethod method, PiEntity receivedEntity) {
        Instance instance = (Instance) receivedEntity;
        if (!(messageContext instanceof ReceivedMessageContext))
            throw new IllegalArgumentException(String.format("Expected received message context so a reply can be sent"));

        ReceivedMessageContext receivedMessageContext = (ReceivedMessageContext) messageContext;
        if (method.equals(EntityMethod.CREATE)) {
            instanceNetworkSetupHandler.handle(instance, receivedMessageContext);
        } else if (method.equals(EntityMethod.UPDATE)) {
            instanceNetworkRefreshHandler.handle(instance, receivedMessageContext);
        } else if (method.equals(EntityMethod.DELETE)) {
            instanceNetworkTeardownHandler.handle(instance, receivedMessageContext);
        } else {
            LOG.info(String.format(S_IGNORING_PI_MESSAGE_S_WITH_METHOD_S, this.getClass().getSimpleName(), receivedMessageContext.getReceivedEntity(), method));
        }
    }
}
