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
public class DisassociateAddressTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final String COLON = ":";
    private static final Log LOG = LogFactory.getLog(DisassociateAddressTaskProcessingQueueContinuation.class);
    private MessageContextFactory messageContextFactory;
    private PiIdBuilder piIdBuilder;

    public DisassociateAddressTaskProcessingQueueContinuation() {
        messageContextFactory = null;
        piIdBuilder = null;
    }

    @Resource(type = NetworkManagerApplication.class)
    public void setMessageContextFactory(MessageContextFactory aMessageContextFactory) {
        this.messageContextFactory = aMessageContextFactory;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Override
    public void receiveResult(String uri, String nodeId) {
        LOG.debug(String.format("Received disassociate address request %s as node %s - will send it to sec group", uri, nodeId));
        PiUriParser piUriParser = new PiUriParser(uri);
        String securityGroupId = piUriParser.getParameter(ResourceSchemes.SECURITY_GROUP.toString());
        String instanceId = piUriParser.getParameter(ResourceSchemes.INSTANCE.toString());
        String publicIpAddress = piUriParser.getResourceId();

        PublicIpAddress sparsePublicIpAddress = new PublicIpAddress();
        sparsePublicIpAddress.setIpAddress(publicIpAddress);
        sparsePublicIpAddress.setInstanceId(instanceId);
        sparsePublicIpAddress.setOwnerId(securityGroupId.split(COLON)[0]);
        sparsePublicIpAddress.setSecurityGroupName(securityGroupId.split(COLON)[1]);

        int instanceGlobalAvzCode = piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instanceId);
        PId networkManagerId = piIdBuilder.getPId(SecurityGroup.getUrl(securityGroupId)).forGlobalAvailablityZoneCode(instanceGlobalAvzCode);

        LOG.info(String.format("Routing stale address disassociation queue task %s to active network manager for sec group %s", uri, securityGroupId));
        MessageContext messageContext = messageContextFactory.newMessageContext();
        messageContext.routePiMessage(networkManagerId, EntityMethod.DELETE, sparsePublicIpAddress);
    }
}
