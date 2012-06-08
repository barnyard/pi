package com.bt.pi.api.service.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.api.ApiException;
import com.bt.pi.api.service.IllegalStateException;
import com.bt.pi.api.service.SecurityGroupService;
import com.bt.pi.api.service.ServiceBaseImpl;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.NetworkProtocol;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.NetworkRuleType;
import com.bt.pi.app.common.entities.OwnerIdGroupNamePair;
import com.bt.pi.app.common.entities.SecurityGroup;

public class SecurityGroupServiceIntegrationImpl extends ServiceBaseImpl implements SecurityGroupService {
    private static final Log LOG = LogFactory.getLog(SecurityGroupServiceIntegrationImpl.class);

    private static final long VLAN_ID = 372;

    public boolean createSecurityGroup(String ownerId, String groupName, String groupDescription) {
        if (groupDescription.equalsIgnoreCase("apiException"))
            throw new ApiException("client error") {
                private static final long serialVersionUID = 1L;
            };
        if (groupDescription.equalsIgnoreCase("apiServerException"))
            throw new NullPointerException();
        if (groupDescription.equalsIgnoreCase("illegalStateException"))
            throw new IllegalStateException("oh dear!");
        if (groupName.equals("default"))
            return true;

        return false;
    }

    public List<SecurityGroup> describeSecurityGroups(String ownerId, List<String> securityGroups) {
        LOG.debug(String.format("describeSecurityGroups(%s, %s)", ownerId, securityGroups));
        int resultCount = 1;
        if (securityGroups != null && securityGroups.size() > 0)
            resultCount = securityGroups.size();

        List<SecurityGroup> securityGroupResult = new ArrayList<SecurityGroup>();

        for (int i = 0; i < resultCount; i++) {
            String index = i > 0 ? Integer.toString(i) : "";
            SecurityGroup securityGroup = new SecurityGroup("admin" + index, "default" + index);
            securityGroup.setVlanId(VLAN_ID);
            securityGroup.setNetworkAddress("172.29.1.1");
            securityGroup.setNetmask("255.255.255.240");
            securityGroup.setDescription("147.149.2.5");
            securityGroup.setDescription("description");

            securityGroup.getInstances().put("172.0.0.2", new InstanceAddress("172.0.0.2", null, "d0:0d:11:22:33:44"));
            securityGroup.getInstances().put("172.0.0.3", new InstanceAddress("172.0.0.3", "1.2.3.4", "d0:0d:11:22:33:44"));

            NetworkRule networkRule1 = new NetworkRule();
            networkRule1.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
            networkRule1.setSourceNetworks(new String[] { "0.0.0.0/0" });
            networkRule1.setDestinationSecurityGroupName("default");
            networkRule1.setNetworkProtocol(NetworkProtocol.TCP);
            networkRule1.setPortRangeMin(0);
            networkRule1.setPortRangeMax(0);
            OwnerIdGroupNamePair[] ownerIdGroupPair = new OwnerIdGroupNamePair[] { new OwnerIdGroupNamePair("otheruser", "othergroup") };
            networkRule1.setOwnerIdGroupNamePair(ownerIdGroupPair);

            NetworkRule networkRule2 = new NetworkRule();
            networkRule2.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
            networkRule2.setSourceNetworks(new String[] { "0.0.0.0/0" });
            networkRule2.setDestinationSecurityGroupName("default");
            networkRule2.setNetworkProtocol(NetworkProtocol.UDP);
            networkRule2.setPortRangeMin(1);
            networkRule2.setPortRangeMax(2);

            securityGroup.setNetworkRules(new HashSet<NetworkRule>(Arrays.asList(new NetworkRule[] { networkRule1, networkRule2 })));
            securityGroupResult.add(securityGroup);
        }

        return securityGroupResult;
    }

    public boolean authoriseIngress(String ownerId, String groupName, List<NetworkRule> networkRules) {
        if (groupName.equals("default"))
            return true;
        return false;
    }

    public boolean revokeIngress(String ownerId, String groupName, List<NetworkRule> networkRules) {
        if (groupName.equals("default"))
            return true;
        return false;
    }

    public boolean deleteSecurityGroup(String userId, String groupName) {
        if (groupName.equals("group2Delete"))
            return true;
        return false;
    }
}
