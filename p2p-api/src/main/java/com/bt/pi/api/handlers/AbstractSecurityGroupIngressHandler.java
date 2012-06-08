/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.amazonaws.ec2.doc.x20090404.IpPermissionSetType;
import com.amazonaws.ec2.doc.x20090404.IpPermissionType;
import com.amazonaws.ec2.doc.x20090404.UserIdGroupPairSetType;
import com.bt.pi.app.common.entities.NetworkProtocol;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.NetworkRuleType;
import com.bt.pi.app.common.entities.OwnerIdGroupNamePair;

public abstract class AbstractSecurityGroupIngressHandler extends HandlerBase {

    public AbstractSecurityGroupIngressHandler() {
    }

    protected List<NetworkRule> extractNetworkRules(IpPermissionSetType ipPermissions, String groupName) {
        List<NetworkRule> networkRules = new ArrayList<NetworkRule>();
        for (IpPermissionType ipPerm : ipPermissions.getItemArray()) {
            NetworkRule networkRule = new NetworkRule();
            networkRule.setDestinationSecurityGroupName(groupName);
            NetworkProtocol protocol = NetworkProtocol.valueOf(ipPerm.getIpProtocol().toUpperCase(Locale.ENGLISH));
            networkRule.setNetworkProtocol(protocol);
            networkRule.setPortRangeMax(ipPerm.getToPort());
            networkRule.setPortRangeMin(ipPerm.getFromPort());
            String[] ipRanges = new String[ipPerm.getIpRanges().getItemArray().length];
            for (int i = 0; i < ipRanges.length; i++) {
                ipRanges[i] = ipPerm.getIpRanges().getItemArray(i).getCidrIp();
            }
            networkRule.setSourceNetworks(ipRanges);
            UserIdGroupPairSetType userIdGroupPairSetType = ipPerm.getGroups();
            if (null != userIdGroupPairSetType) {
                OwnerIdGroupNamePair[] ownerIdGroupNamePairs = new OwnerIdGroupNamePair[userIdGroupPairSetType.sizeOfItemArray()];
                for (int i = 0; i < ownerIdGroupNamePairs.length; i++) {
                    OwnerIdGroupNamePair ownerIdGroupNamePair = new OwnerIdGroupNamePair();
                    ownerIdGroupNamePair.setGroupName(userIdGroupPairSetType.getItemArray(i).getGroupName());
                    ownerIdGroupNamePair.setOwnerId(userIdGroupPairSetType.getItemArray(i).getUserId());
                    ownerIdGroupNamePairs[i] = ownerIdGroupNamePair;
                }
                networkRule.setOwnerIdGroupNamePair(ownerIdGroupNamePairs);
            } else {
                networkRule.setOwnerIdGroupNamePair(new OwnerIdGroupNamePair[0]);
            }
            networkRule.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
            networkRules.add(networkRule);
        }
        return networkRules;
    }
}
