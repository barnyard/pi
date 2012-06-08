/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DescribeSecurityGroupsDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeSecurityGroupsResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeSecurityGroupsResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeSecurityGroupsSetItemType;
import com.amazonaws.ec2.doc.x20090404.IpPermissionSetType;
import com.amazonaws.ec2.doc.x20090404.IpPermissionType;
import com.amazonaws.ec2.doc.x20090404.IpRangeItemType;
import com.amazonaws.ec2.doc.x20090404.IpRangeSetType;
import com.amazonaws.ec2.doc.x20090404.SecurityGroupItemType;
import com.amazonaws.ec2.doc.x20090404.SecurityGroupSetType;
import com.amazonaws.ec2.doc.x20090404.UserIdGroupPairSetType;
import com.amazonaws.ec2.doc.x20090404.UserIdGroupPairType;
import com.bt.pi.api.service.SecurityGroupService;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DescribeRegions
 */
@Endpoint
public class DescribeSecurityGroupsHandler extends HandlerBase {

    private static final Log LOG = LogFactory.getLog(DescribeSecurityGroupsHandler.class);
    private static final String DESCRIBE_SECURITY_GROUPS = "DescribeSecurityGroups";
    private SecurityGroupService securityGroupService;

    public DescribeSecurityGroupsHandler() {
        securityGroupService = null;
    }

    @Resource
    public void setSecurityGroupService(SecurityGroupService aSecurityGroupService) {
        securityGroupService = aSecurityGroupService;
    }

    @PayloadRoot(localPart = DESCRIBE_SECURITY_GROUPS, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DescribeSecurityGroupsResponseDocument describeSecurityGroups(com.amazonaws.ec2.doc.x20081201.DescribeSecurityGroupsDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DescribeSecurityGroupsResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = DESCRIBE_SECURITY_GROUPS, namespace = NAMESPACE_20090404)
    public DescribeSecurityGroupsResponseDocument describeSecurityGroups(DescribeSecurityGroupsDocument requestDocument) {
        LOG.debug(String.format("describeSecurityGroups(%s)", requestDocument));
        try {
            DescribeSecurityGroupsResponseDocument resultDocument = DescribeSecurityGroupsResponseDocument.Factory.newInstance();
            DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = resultDocument.addNewDescribeSecurityGroupsResponse();
            SecurityGroupSetType securityGroupSetType = SecurityGroupSetType.Factory.newInstance();

            List<String> securityGroupNames = new ArrayList<String>();
            if (null != requestDocument.getDescribeSecurityGroups().getSecurityGroupSet())
                for (DescribeSecurityGroupsSetItemType name : requestDocument.getDescribeSecurityGroups().getSecurityGroupSet().getItemArray())
                    securityGroupNames.add(name.getGroupName());

            List<SecurityGroup> securityGroupResult = securityGroupService.describeSecurityGroups(getUserId(), securityGroupNames);

            SecurityGroupItemType[] securityGroupItemTypes = new SecurityGroupItemType[securityGroupResult.size()];

            for (int i = 0; i < securityGroupResult.size(); i++) {
                SecurityGroupItemType securityGroupItemType = SecurityGroupItemType.Factory.newInstance();
                securityGroupItemType.setGroupName(securityGroupResult.get(i).getOwnerIdGroupNamePair().getGroupName());
                securityGroupItemType.setGroupDescription(securityGroupResult.get(i).getDescription());
                securityGroupItemType.setOwnerId(securityGroupResult.get(i).getOwnerIdGroupNamePair().getOwnerId());

                IpPermissionSetType ipPermissionSetType = securityGroupItemType.addNewIpPermissions();
                addRules(securityGroupResult.get(i), ipPermissionSetType);

                securityGroupItemTypes[i] = securityGroupItemType;
            }

            securityGroupSetType.setItemArray(securityGroupItemTypes);
            describeSecurityGroupsResponseType.setSecurityGroupInfo(securityGroupSetType);
            describeSecurityGroupsResponseType.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (DescribeSecurityGroupsResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }

    private void addRules(SecurityGroup securityGroup, IpPermissionSetType ipPermissionSetType) {
        for (NetworkRule networkRule : securityGroup.getNetworkRules()) {
            IpPermissionType ipPermissionType = ipPermissionSetType.addNewItem();
            ipPermissionType.setFromPort(networkRule.getPortRangeMin());
            ipPermissionType.setToPort(networkRule.getPortRangeMax());
            UserIdGroupPairSetType userIdGroupPairSetType = getUserIdGroupNameSet(networkRule);
            ipPermissionType.setGroups(userIdGroupPairSetType);
            ipPermissionType.setIpProtocol(networkRule.getNetworkProtocol().name());
            IpRangeSetType ipRangeSetType = IpRangeSetType.Factory.newInstance();
            IpRangeItemType[] ipRangeItemTypes = getSourceNetworks(networkRule);
            ipRangeSetType.setItemArray(ipRangeItemTypes);
            ipPermissionType.setIpRanges(ipRangeSetType);
        }
    }

    private UserIdGroupPairSetType getUserIdGroupNameSet(NetworkRule networkRule) {
        UserIdGroupPairSetType userIdGroupPairSetType = UserIdGroupPairSetType.Factory.newInstance();
        for (int i = 0; i < networkRule.getOwnerIdGroupNamePair().length; i++) {
            UserIdGroupPairType userIdGroupPairType = userIdGroupPairSetType.addNewItem();
            userIdGroupPairType.setGroupName(networkRule.getOwnerIdGroupNamePair()[i].getGroupName());
            userIdGroupPairType.setUserId(networkRule.getOwnerIdGroupNamePair()[i].getOwnerId());
        }
        return userIdGroupPairSetType;
    }

    private IpRangeItemType[] getSourceNetworks(NetworkRule networkRule) {
        IpRangeItemType[] ipRangeItemTypes = new IpRangeItemType[networkRule.getSourceNetworks().length];
        for (int k = 0; k < networkRule.getSourceNetworks().length; k++) {
            IpRangeItemType ipRangeItemType = IpRangeItemType.Factory.newInstance();
            ipRangeItemType.setCidrIp(networkRule.getSourceNetworks()[k]);
            ipRangeItemTypes[k] = ipRangeItemType;
        }
        return ipRangeItemTypes;
    }
}
