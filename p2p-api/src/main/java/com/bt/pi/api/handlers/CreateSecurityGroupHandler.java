/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.CreateSecurityGroupDocument;
import com.amazonaws.ec2.doc.x20090404.CreateSecurityGroupResponseDocument;
import com.amazonaws.ec2.doc.x20090404.CreateSecurityGroupResponseType;
import com.bt.pi.api.service.SecurityGroupService;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for CreateSecurityGroup
 */
@Endpoint
public class CreateSecurityGroupHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(CreateSecurityGroupHandler.class);
    private static final String CREATE_SECURITY_GROUP = "CreateSecurityGroup";

    private SecurityGroupService securityGroupService;

    public CreateSecurityGroupHandler() {
        securityGroupService = null;
    }

    @Resource
    public void setSecurityGroupService(SecurityGroupService aSecurityGroupService) {
        securityGroupService = aSecurityGroupService;
    }

    @PayloadRoot(localPart = CREATE_SECURITY_GROUP, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.CreateSecurityGroupResponseDocument createSecurityGroup(com.amazonaws.ec2.doc.x20081201.CreateSecurityGroupDocument requestDocument) {
        LOG.debug(requestDocument);
        return (com.amazonaws.ec2.doc.x20081201.CreateSecurityGroupResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = CREATE_SECURITY_GROUP, namespace = NAMESPACE_20090404)
    public CreateSecurityGroupResponseDocument createSecurityGroup(CreateSecurityGroupDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            CreateSecurityGroupResponseDocument resultDocument = CreateSecurityGroupResponseDocument.Factory.newInstance();
            CreateSecurityGroupResponseType addNewCreateSecurityGroupResponse = resultDocument.addNewCreateSecurityGroupResponse();

            boolean result = securityGroupService.createSecurityGroup(getUserId(), requestDocument.getCreateSecurityGroup().getGroupName(), requestDocument.getCreateSecurityGroup().getGroupDescription());

            addNewCreateSecurityGroupResponse.setReturn(result);
            addNewCreateSecurityGroupResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (CreateSecurityGroupResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
