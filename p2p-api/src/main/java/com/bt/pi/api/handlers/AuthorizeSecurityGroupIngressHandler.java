/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.AuthorizeSecurityGroupIngressDocument;
import com.amazonaws.ec2.doc.x20090404.AuthorizeSecurityGroupIngressResponseDocument;
import com.amazonaws.ec2.doc.x20090404.AuthorizeSecurityGroupIngressResponseType;
import com.amazonaws.ec2.doc.x20090404.IpPermissionSetType;
import com.bt.pi.api.service.SecurityGroupService;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for AuthorizeSecurityGroupIngress
 */
@Endpoint
public class AuthorizeSecurityGroupIngressHandler extends AbstractSecurityGroupIngressHandler {
    private static final Log LOG = LogFactory.getLog(AuthorizeSecurityGroupIngressHandler.class);
    private static final String AUTHORIZE_SECURITY_GROUP_INGRESS = "AuthorizeSecurityGroupIngress";

    private SecurityGroupService securityGroupService;

    public AuthorizeSecurityGroupIngressHandler() {
        securityGroupService = null;
    }

    @Resource
    public void setSecurityGroupService(SecurityGroupService aSecurityGroupService) {
        securityGroupService = aSecurityGroupService;
    }

    @PayloadRoot(localPart = AUTHORIZE_SECURITY_GROUP_INGRESS, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.AuthorizeSecurityGroupIngressResponseDocument authorizeSecurityGroupIngress(com.amazonaws.ec2.doc.x20081201.AuthorizeSecurityGroupIngressDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.AuthorizeSecurityGroupIngressResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = AUTHORIZE_SECURITY_GROUP_INGRESS, namespace = NAMESPACE_20090404)
    public AuthorizeSecurityGroupIngressResponseDocument authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            AuthorizeSecurityGroupIngressResponseDocument resultDocument = AuthorizeSecurityGroupIngressResponseDocument.Factory.newInstance();
            AuthorizeSecurityGroupIngressResponseType addNewAuthorizeSecurityGroupIngressResponse = resultDocument.addNewAuthorizeSecurityGroupIngressResponse();

            IpPermissionSetType ipPermissions = requestDocument.getAuthorizeSecurityGroupIngress().getIpPermissions();
            String groupName = requestDocument.getAuthorizeSecurityGroupIngress().getGroupName();
            List<NetworkRule> networkRules = extractNetworkRules(ipPermissions, groupName);

            boolean result = securityGroupService.authoriseIngress(getUserId(), requestDocument.getAuthorizeSecurityGroupIngress().getGroupName(), networkRules);
            addNewAuthorizeSecurityGroupIngressResponse.setReturn(result);
            addNewAuthorizeSecurityGroupIngressResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (AuthorizeSecurityGroupIngressResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
