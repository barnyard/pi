/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.IpPermissionSetType;
import com.amazonaws.ec2.doc.x20090404.RevokeSecurityGroupIngressDocument;
import com.amazonaws.ec2.doc.x20090404.RevokeSecurityGroupIngressResponseDocument;
import com.amazonaws.ec2.doc.x20090404.RevokeSecurityGroupIngressResponseType;
import com.bt.pi.api.service.SecurityGroupService;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for revoking security group ingress rules
 */
@Endpoint
public class RevokeSecurityGroupIngressHandler extends AbstractSecurityGroupIngressHandler {

    private static final Log LOG = LogFactory.getLog(RevokeSecurityGroupIngressHandler.class);
    private static final String REVOKE_SECURITY_GROUP_INGRESS = "RevokeSecurityGroupIngress";

    private SecurityGroupService securityGroupService;

    public RevokeSecurityGroupIngressHandler() {
        securityGroupService = null;
    }

    @Resource
    public void setSecurityGroupService(SecurityGroupService aSecurityGroupService) {
        securityGroupService = aSecurityGroupService;
    }

    @PayloadRoot(localPart = REVOKE_SECURITY_GROUP_INGRESS, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.RevokeSecurityGroupIngressResponseDocument revokeSecurityGroupIngress(com.amazonaws.ec2.doc.x20081201.RevokeSecurityGroupIngressDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.RevokeSecurityGroupIngressResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = REVOKE_SECURITY_GROUP_INGRESS, namespace = NAMESPACE_20090404)
    public RevokeSecurityGroupIngressResponseDocument revokeSecurityGroupIngress(RevokeSecurityGroupIngressDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            IpPermissionSetType ipPermissions = requestDocument.getRevokeSecurityGroupIngress().getIpPermissions();
            String groupName = requestDocument.getRevokeSecurityGroupIngress().getGroupName();
            List<NetworkRule> networkRules = extractNetworkRules(ipPermissions, groupName);
            boolean result = securityGroupService.revokeIngress(getUserId(), groupName, networkRules);

            RevokeSecurityGroupIngressResponseDocument resultDocument = RevokeSecurityGroupIngressResponseDocument.Factory.newInstance();
            RevokeSecurityGroupIngressResponseType addNewRevokeSecurityGroupIngressResponse = resultDocument.addNewRevokeSecurityGroupIngressResponse();
            addNewRevokeSecurityGroupIngressResponse.setReturn(result);
            addNewRevokeSecurityGroupIngressResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (RevokeSecurityGroupIngressResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
