package com.bt.pi.app.networkmanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.PiUriParser;

@Component
public class AssociateAddressTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final String COLON = ":";
    private static final Log LOG = LogFactory.getLog(AssociateAddressTaskProcessingQueueContinuation.class);
    private MessageContextFactory messageContextFactory;
    private PiIdBuilder piIdBuilder;

    public AssociateAddressTaskProcessingQueueContinuation() {
        messageContextFactory = null;
        piIdBuilder = null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource(type = NetworkManagerApplication.class)
    public void setMessageContextFactory(MessageContextFactory aMessageContextFactory) {
        this.messageContextFactory = aMessageContextFactory;
    }

    @Override
    public void receiveResult(String uri, String nodeId) {
        LOG.debug(String.format("Received associate address request %s as node %s - will send it to sec group", uri, nodeId));
        PiUriParser piUriParser = new PiUriParser(uri);
        String instanceId = piUriParser.getParameter(ResourceSchemes.INSTANCE.toString());
        String securityGroupId = piUriParser.getParameter(ResourceSchemes.SECURITY_GROUP.toString());
        String ownerId = securityGroupId.split(COLON)[0];
        String securityGroupName = securityGroupId.split(COLON)[1];
        String publicIpAddress = piUriParser.getResourceId();

        PublicIpAddress sparsePublicIp = new PublicIpAddress();
        sparsePublicIp.setInstanceId(instanceId);
        sparsePublicIp.setIpAddress(publicIpAddress);
        sparsePublicIp.setOwnerId(ownerId);
        sparsePublicIp.setSecurityGroupName(securityGroupName);

        int instanceGlobalAvzCode = piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instanceId);
        PId networkManagerId = piIdBuilder.getPId(SecurityGroup.getUrl(securityGroupId)).forGlobalAvailablityZoneCode(instanceGlobalAvzCode);

        LOG.info(String.format("Routing stale address assignment queue task %s to active network manager for sec group %s", uri, securityGroupId));
        MessageContext messageContext = messageContextFactory.newMessageContext();
        messageContext.routePiMessage(networkManagerId, EntityMethod.CREATE, sparsePublicIp);
    }
}
