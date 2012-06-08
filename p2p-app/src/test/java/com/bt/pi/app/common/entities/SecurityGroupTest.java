package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.leased.LeasedAllocatedResource;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.scope.NodeScope;

public class SecurityGroupTest {
    private static final String TEST_PUBLIC_IP = "1.2.3.4";
    private static final long VLAN_ID = 372;
    private SecurityGroup securityGroup;
    private String instanceId1 = "instance1";
    private String instanceId2 = "instance2";
    private NetworkRule networkRule1;

    @Before
    public void setUp() throws Exception {
        this.securityGroup = new SecurityGroup("admin", "default", VLAN_ID, "172.29.1.1", "255.255.255.240", "147.149.2.5", null);
        securityGroup.getInstances().put(instanceId1, new InstanceAddress("172.0.0.2", null, "d0:0d:11:22:33:44"));
        securityGroup.getInstances().put(instanceId2, new InstanceAddress("172.0.0.3", TEST_PUBLIC_IP, "d0:0d:11:22:33:44"));

        networkRule1 = new NetworkRule();
        networkRule1.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
        networkRule1.setSourceNetworks(new String[] { "0.0.0.0/0" });
        networkRule1.setDestinationSecurityGroupName("default");
        networkRule1.setNetworkProtocol(NetworkProtocol.TCP);
        networkRule1.setPortRangeMin(0);
        networkRule1.setPortRangeMax(0);

        NetworkRule networkRule2 = new NetworkRule();
        networkRule2.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
        networkRule2.setSourceNetworks(new String[] { "0.0.0.0/0" });
        networkRule2.setDestinationSecurityGroupName("default");
        networkRule2.setNetworkProtocol(NetworkProtocol.UDP);
        networkRule2.setPortRangeMin(1);
        networkRule2.setPortRangeMax(2);

        securityGroup.setNetworkRules(new HashSet<NetworkRule>(Arrays.asList(new NetworkRule[] { networkRule1, networkRule2 })));
    }

    @Test
    public void shouldCreateCorrectKeyFromStaticConstructor() {
        assertEquals(securityGroup.getUrl(), SecurityGroup.getUrl(securityGroup.getOwnerIdGroupNamePair().getOwnerId(), securityGroup.getOwnerIdGroupNamePair().getGroupName()));
    }

    @Test
    public void shouldBeAbleToRoundTripToFromJson() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();

        // act
        String json = koalaJsonParser.getJson(securityGroup);

        // assert
        assertEquals(securityGroup, koalaJsonParser.getObject(json, SecurityGroup.class));
    }

    @Test
    public void shouldCalculateRouterAddressFromNetworkAddress() {
        // act
        String routerAddress = this.securityGroup.getRouterAddress();

        // assert
        assertEquals("172.29.1.2", routerAddress);
    }

    @Test
    public void shouldGetNullRouterAddressWhenNetworkAddressIsNull() {
        // setup
        securityGroup.setNetworkAddress(null);

        // act
        String routerAddress = this.securityGroup.getRouterAddress();

        // assert
        assertEquals(null, routerAddress);
    }

    @Test
    public void shouldGetNullBroadcastAddressWhenNetworkAddressIsNull() {
        // setup
        securityGroup.setNetworkAddress(null);

        // act
        String res = this.securityGroup.getBroadcastAddress();

        // assert
        assertEquals(null, res);
    }

    @Test
    public void shouldGetNullBroadcastAddressWhenNetmaskIsNull() {
        // setup
        securityGroup.setNetmask(null);

        // act
        String res = this.securityGroup.getBroadcastAddress();

        // assert
        assertEquals(null, res);
    }

    @Test
    public void shouldCalculateAllPublicAddressesForASecurityGroup() {
        // act
        List<String> allPrivateAddresses = this.securityGroup.getPrivateAddresses();

        // assert
        assertEquals(13, allPrivateAddresses.size());
        for (int i = 0; i < allPrivateAddresses.size(); i++) {
            assertEquals("172.29.1." + (i + 3), allPrivateAddresses.get(i));
        }
    }

    @Test
    public void shouldGetSlashnetFromSuppliedNetmask() {
        // act
        int res = securityGroup.getSlashnet();

        // assert
        assertEquals(28, res);
    }

    @Test
    public void shouldClone() throws Exception {
        // act
        SecurityGroup clone = new SecurityGroup(securityGroup);

        // assert
        assertTrue(securityGroup.equals(clone));
        assertEquals(securityGroup.hashCode(), clone.hashCode());
        assertNotSame(clone, securityGroup);

        assertEquals(clone.getVlanId(), securityGroup.getVlanId());
        assertEquals(clone.getVlanId(), securityGroup.getVlanId());

        assertEquals(clone.getSecurityGroupId(), securityGroup.getSecurityGroupId());
        assertEquals(clone.getNetworkAddress(), securityGroup.getNetworkAddress());
        assertEquals(clone.getBroadcastAddress(), securityGroup.getBroadcastAddress());
        assertEquals(clone.getNetmask(), securityGroup.getNetmask());
        assertEquals(clone.getDnsAddress(), securityGroup.getDnsAddress());

        assertNotSame(clone.getInstances(), securityGroup.getInstances());
        assertEquals(2, clone.getInstances().size());
        assertEquals(clone.getInstances().get(instanceId1), securityGroup.getInstances().get(instanceId1));
        assertNotSame(clone.getInstances().get(instanceId1), securityGroup.getInstances().get(instanceId1));
        assertEquals(clone.getInstances().get(instanceId2), securityGroup.getInstances().get(instanceId2));
        assertNotSame(clone.getInstances().get(instanceId2), securityGroup.getInstances().get(instanceId2));

        assertNotSame(clone.getNetworkRules(), securityGroup.getNetworkRules());
        assertEquals(clone.getNetworkRules().size(), securityGroup.getNetworkRules().size());
        assertTrue(clone.getNetworkRules().containsAll(securityGroup.getNetworkRules()));
        assertThatTheNetworkRuleReferencesAreDifferent(clone, securityGroup);
    }

    private void assertThatTheNetworkRuleReferencesAreDifferent(SecurityGroup clone, SecurityGroup original) {
        NetworkRule[] clonedNetworkRules = getNetworkRulesAsArray(clone);
        NetworkRule[] originalNetworkRules = getNetworkRulesAsArray(original);

        for (int i = 0; i < clonedNetworkRules.length; i++) {
            for (int j = 0; j < originalNetworkRules.length; j++) {
                if (clonedNetworkRules[i].equals(originalNetworkRules[j]))
                    assertNotSame(clonedNetworkRules[i], originalNetworkRules[j]);
            }
        }
    }

    private NetworkRule[] getNetworkRulesAsArray(SecurityGroup group) {
        NetworkRule[] networkRules = new NetworkRule[group.getNetworkRules().size()];
        return group.getNetworkRules().toArray(networkRules);
    }

    @Test
    public void testAddNetworkRule() throws Exception {
        // setup
        NetworkRule networkRule = new NetworkRule();

        // act
        securityGroup.addNetworkRule(networkRule);

        // assert
        assertTrue(securityGroup.getNetworkRules().contains(networkRule));
    }

    @Test
    public void testThatDuplicateNetworkRulesDontGetAddedTwice() throws Exception {
        // setup
        int sizeBefore = securityGroup.getNetworkRules().size();
        NetworkRule networkRule = new NetworkRule();
        securityGroup.addNetworkRule(networkRule);

        // act
        securityGroup.addNetworkRule(networkRule);

        // assert
        assertTrue(securityGroup.getNetworkRules().contains(networkRule));
        assertEquals(sizeBefore + 1, securityGroup.getNetworkRules().size());
    }

    @Test
    public void shouldAddInstance() {
        // setup
        String testMac = "testMacAddress";
        String testInstanceId = "addInstance";
        String testPrivateIp = "testPrivateIpAddress";
        String testPublicIp = "testPublicIp";

        // act
        securityGroup.addInstance(testInstanceId, testPrivateIp, testPublicIp, testMac);

        // assert
        assertTrue(securityGroup.containsInstance(testInstanceId));
        assertEquals(testMac, securityGroup.getInstances().get(testInstanceId).getMacAddress());
        assertEquals(testPublicIp, securityGroup.getInstances().get(testInstanceId).getPublicIpAddress());
        assertEquals(testPrivateIp, securityGroup.getInstances().get(testInstanceId).getPrivateIpAddress());
    }

    @Test
    public void testContainsInstance() {
        // act&assert
        assertTrue(securityGroup.containsInstance(instanceId1));
        assertFalse(securityGroup.containsInstance("unknownInsanceId"));
    }

    @Test
    public void testContainsPublicIpAddress() {
        // act & assert
        assertTrue(securityGroup.containsPublicIp(TEST_PUBLIC_IP));
        assertFalse(securityGroup.containsPublicIp("555.666.777.888"));
    }

    @Test
    public void testEqualsNotSecurityGroup() {
        // act
        boolean result = securityGroup.equals("A String");

        // assert
        assertFalse(result);
    }

    @Test
    public void testEqualsSame() {
        // act
        boolean result = securityGroup.equals(securityGroup);

        // assert
        assertTrue(result);
    }

    @Test
    public void testEqualsNull() {
        // act
        boolean result = securityGroup.equals(null);

        // assert
        assertFalse(result);
    }

    @Test
    public void shouldCalculateBroadcastAddressFromNetworkAndNetmask() {
        // act
        String res = securityGroup.getBroadcastAddress();

        // assert
        assertEquals("172.29.1.16", res);
    }

    @Test
    public void shouldRemoveNetworkRules() {
        // setup
        securityGroup.addNetworkRule(new NetworkRule());
        assertTrue(securityGroup.getNetworkRules().size() > 0);

        // act
        securityGroup.removeNetworkRules();

        // assert
        assertEquals(0, securityGroup.getNetworkRules().size());
    }

    @Test
    public void shouldRemoveSingleNetworkRule() {
        // setup
        assertTrue(securityGroup.getNetworkRules().contains(networkRule1));

        // act
        boolean result = securityGroup.removeNetworkRule(networkRule1);

        // assert
        assertTrue("The networkrule was not removed", result);
        assertEquals(1, securityGroup.getNetworkRules().size());
    }

    @Test
    public void shouldNotRemoveARuleThatIsntThere() {
        // setup
        NetworkRule unknownNetworkRule = new NetworkRule();
        assertFalse(securityGroup.getNetworkRules().contains(unknownNetworkRule));

        // act
        boolean result = securityGroup.removeNetworkRule(unknownNetworkRule);

        // assert
        assertFalse("Nothing should have been removed from the securitygroup", result);
    }

    @Test
    public void shouldNotPukeIfNetworkRulesSetIsNull() {
        // setup
        securityGroup.setNetworkRules(null);

        // act
        boolean result = securityGroup.removeNetworkRule(networkRule1);

        // assert
        assertFalse("Nothing should have been removed from the securitygroup", result);
    }

    @Test
    public void shouldBeAbleToGetSubnetAddressAsLong() {
        // act
        long subnetAddr = securityGroup.getNetworkAddressLong();

        // assert
        assertEquals(IpAddressUtils.ipToLong("172.29.1.1"), subnetAddr);
    }

    @Test
    public void shouldHaveCorrectAnnotationsForVlanResourceHeartbeats() throws Exception {
        // act
        LeasedAllocatedResource res = SecurityGroup.class.getMethod("getVlanId").getAnnotation(LeasedAllocatedResource.class);

        // assert
        assertEquals(VlanAllocationIndex.URL, res.allocationRecordUri());
        assertEquals(NodeScope.REGION, res.allocationRecordScope());
    }

    @Test
    public void shouldHaveCorrectAnnotationsForSubnetResourceHeartbeats() throws Exception {
        // act
        LeasedAllocatedResource res = SecurityGroup.class.getMethod("getNetworkAddressLong").getAnnotation(LeasedAllocatedResource.class);

        // assert
        assertEquals(SubnetAllocationIndex.URL, res.allocationRecordUri());
        assertEquals(NodeScope.REGION, res.allocationRecordScope());
    }

    @Test
    public void shouldBeAbleToReadOldFormatJson() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();
        String json = "{" + "        \"type\" : \"SecurityGroup\"," + "\"instances\" : {" + "  \"instance1\" : {" + "    \"macAddress\" : \"d0:0d:11:22:33:44\"," + "    \"privateIpAddress\" : \"172.0.0.2\"" + "  }," + "  \"instance2\" : {"
                + "    \"macAddress\" : \"d0:0d:11:22:33:44\"," + "    \"publicIpAddress\" : \"1.2.3.4\"," + "    \"privateIpAddress\" : \"172.0.0.3\"" + "  }" + " }," + " \"url\" : \"sg:admin:default\"," + " \"ownerIdGroupNamePair\" : {"
                + "   \"type\" : \"OwnerIdGroupNamePair\"," + "   \"url\" : \"ogp:admin:default\"," + "   \"ownerId\" : \"admin\"," + "   \"groupName\" : \"default\"," + "   \"securityGroupId\" : \"admin:default\"," + "   \"uriScheme\" : \"ogp\","
                + "   \"version\" : 0" + "}," + " \"vlanId\" : 372," + " \"networkAddress\" : \"172.29.1.1\"," + "\"netmask\" : \"255.255.255.240\"," + "\"dnsAddress\" : \"147.149.2.5\"," + "\"networkRules\" : [ {"
                + "  \"ownerIdGroupNamePair\" : [ ]," + "  \"networkRuleType\" : \"FIREWALL_OPEN\"," + "  \"sourceNetworks\" : [ \"0.0.0.0/0\" ]," + "  \"destinationSecurityGroupName\" : \"default\"," + "  \"networkProtocol\" : \"UDP\","
                + "  \"portRangeMin\" : 1," + "  \"portRangeMax\" : 2" + "}, {" + " \"ownerIdGroupNamePair\" : [ ]," + " \"networkRuleType\" : \"FIREWALL_OPEN\"," + " \"sourceNetworks\" : [ \"0.0.0.0/0\" ],"
                + " \"destinationSecurityGroupName\" : \"default\"," + " \"networkProtocol\" : \"TCP\"," + " \"portRangeMin\" : 0," + " \"portRangeMax\" : 0" + "} ]," + "\"deleted\" : false," + "\"uriScheme\" : \"sg\"," + "\"version\" : 0" + "}";

        // act
        Object result = koalaJsonParser.getObject(json, SecurityGroup.class);

        // assert
        assertEquals(securityGroup, result);
        SecurityGroup securityGroupResult = (SecurityGroup) result;
        assertEquals("admin", securityGroupResult.getOwnerIdGroupNamePair().getOwnerId());
        assertEquals("default", securityGroupResult.getOwnerIdGroupNamePair().getGroupName());
        assertEquals("admin:default", securityGroupResult.getOwnerIdGroupNamePair().getSecurityGroupId());
    }
}
