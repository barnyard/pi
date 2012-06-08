/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DeleteSecurityGroupDocument;
import com.amazonaws.ec2.doc.x20090404.DeleteSecurityGroupResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DeleteSecurityGroupResponseType;
import com.bt.pi.api.service.SecurityGroupService;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DeleteSecurityGroup
 */
@Endpoint
public class DeleteSecurityGroupHandler extends HandlerBase {

    private static final Log LOG = LogFactory.getLog(DeleteSecurityGroupHandler.class);
    private static final String DELETE_SECURITY_GROUP = "DeleteSecurityGroup";

    private SecurityGroupService securityGroupService;

    public DeleteSecurityGroupHandler() {
        securityGroupService = null;
    }

    @Resource
    public void setSecurityGroupService(SecurityGroupService aSecurityGroupService) {
        securityGroupService = aSecurityGroupService;
    }

    @PayloadRoot(localPart = DELETE_SECURITY_GROUP, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DeleteSecurityGroupResponseDocument deleteSecurityGroup(com.amazonaws.ec2.doc.x20081201.DeleteSecurityGroupDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DeleteSecurityGroupResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = DELETE_SECURITY_GROUP, namespace = NAMESPACE_20090404)
    public DeleteSecurityGroupResponseDocument deleteSecurityGroup(DeleteSecurityGroupDocument requestDocument) {
        LOG.debug(String.format("deleteSecurityGroup(%s)", requestDocument));
        try {
            String groupName = requestDocument.getDeleteSecurityGroup().getGroupName();
            boolean result = securityGroupService.deleteSecurityGroup(getUserId(), groupName);

            DeleteSecurityGroupResponseDocument responseDocument = DeleteSecurityGroupResponseDocument.Factory.newInstance();
            DeleteSecurityGroupResponseType deleteSecurityGroupResponseType = responseDocument.addNewDeleteSecurityGroupResponse();
            deleteSecurityGroupResponseType.setReturn(result);
            deleteSecurityGroupResponseType.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(responseDocument);
            return (DeleteSecurityGroupResponseDocument) sanitiseXml(responseDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }

}
