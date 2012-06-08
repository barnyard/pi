package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DescribeSecurityGroupsDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeSecurityGroupsResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeSecurityGroupsType;
import com.amazonaws.ec2.doc.x20081201.SecurityGroupSetType;
import com.amazonaws.ec2.doc.x20081201.UserIdGroupPairSetType;
import com.bt.pi.api.service.SecurityGroupService;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.NetworkProtocol;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.NetworkRuleType;
import com.bt.pi.app.common.entities.OwnerIdGroupNamePair;
import com.bt.pi.app.common.entities.SecurityGroup;

public class DescribeSecurityGroupsHandlerTest extends AbstractHandlerTest {

    private static final long VLAN_ID = 372;
    private DescribeSecurityGroupsHandler describeSecurityGroupsHandler;
    private SecurityGroupService securityGroupService;
    private SecurityGroup securityGroup;

    @Before
    public void before() {
        super.before();
        securityGroup = new SecurityGroup("admin", "default");
        securityGroup.setVlanId(VLAN_ID);
        securityGroup.setNetworkAddress("172.29.1.1");
        securityGroup.setNetmask("255.255.255.240");
        securityGroup.setDescription("147.149.2.5");
        securityGroup.setDescription("description");
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
        List<SecurityGroup> securityGroupResult = new ArrayList<SecurityGroup>();
        securityGroupResult.add(securityGroup);

        describeSecurityGroupsHandler = new DescribeSecurityGroupsHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        securityGroupService = mock(SecurityGroupService.class);
        describeSecurityGroupsHandler.setSecurityGroupService(securityGroupService);
        when(securityGroupService.describeSecurityGroups("userid", new ArrayList<String>())).thenReturn(securityGroupResult);
    }

    @Test
    public void testDescribeSecurityGroupsEmpty() throws Exception {
        // setup
        DescribeSecurityGroupsDocument requestDocument = DescribeSecurityGroupsDocument.Factory.newInstance();
        DescribeSecurityGroupsType describeSecurityGroupsType = requestDocument.addNewDescribeSecurityGroups();
        describeSecurityGroupsType.addNewSecurityGroupSet();

        // act
        DescribeSecurityGroupsResponseDocument response = describeSecurityGroupsHandler.describeSecurityGroups(requestDocument);

        // assert
        SecurityGroupSetType securityGroupInfo = response.getDescribeSecurityGroupsResponse().getSecurityGroupInfo();
        assertEquals(1, securityGroupInfo.getItemArray().length);
        assertEquals("default", securityGroupInfo.getItemArray(0).getGroupName());
        assertEquals("admin", securityGroupInfo.getItemArray(0).getOwnerId());
        assertEquals("description", securityGroupInfo.getItemArray(0).getGroupDescription());
        assertEquals(2, securityGroup.getNetworkRules().size());
        assertEquals("0.0.0.0/0", securityGroupInfo.getItemArray(0).getIpPermissions().getItemArray(0).getIpRanges().getItemArray(0).getCidrIp());
        assertEquals(2, securityGroupInfo.getItemArray(0).getIpPermissions().sizeOfItemArray());

        UserIdGroupPairSetType other = securityGroupInfo.getItemArray(0).getIpPermissions().getItemArray(0).getGroups().sizeOfItemArray() > 0 ? securityGroupInfo.getItemArray(0).getIpPermissions().getItemArray(0).getGroups() : securityGroupInfo
                .getItemArray(0).getIpPermissions().getItemArray(1).getGroups();
        assertEquals("otheruser", other.getItemArray(0).getUserId());
        assertEquals("othergroup", other.getItemArray(0).getGroupName());
    }

}
