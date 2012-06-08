package com.bt.pi.app.networkmanager.addressing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.networkmanager.dhcp.DhcpManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;

@RunWith(MockitoJUnitRunner.class)
public class PrivateIpAddressManagerTest {
    private PrivateIpAddressManager privateIpAddressManager;
    private static final String ALLOCATED_PUBLIC_ADDRESS_1 = "1.2.3.4";
    private static final String ALLOCATED_PUBLIC_ADDRESS_2 = "5.6.7.8";
    private String instanceWithNoPublicIp = "i-noPublicIpInstance";
    private String instanceId2 = "i-nstanceId2";
    private String instanceId3 = "i-nstanceId3";
    private SecurityGroup securityGroup1;
    private SecurityGroup securityGroup2;
    @Mock
    ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    DhcpManager dhcpManager;

    @Before
    public void before() {
        privateIpAddressManager = new PrivateIpAddressManager();
        privateIpAddressManager.setDhcpManager(dhcpManager);
        privateIpAddressManager.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);

        securityGroup1 = new SecurityGroup("bozo", "default", 10L, "172.0.0.0", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup1.getInstances().put(instanceWithNoPublicIp, new InstanceAddress("172.0.0.3", null, "aa:aa:aa:aa:aa:aa"));
        securityGroup1.getInstances().put(instanceId2, new InstanceAddress("172.0.0.2", ALLOCATED_PUBLIC_ADDRESS_1, "aa:aa:aa:aa:aa:aa"));

        securityGroup2 = new SecurityGroup("nutter", "default", 11L, "172.0.0.16", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup2.getInstances().put(instanceId3, new InstanceAddress("172.0.0.18", ALLOCATED_PUBLIC_ADDRESS_2, "aa:aa:aa:aa:aa:aa"));
    }

    @Test
    public void testAllocatePrivateIpAddress() {
        // act
        String res = privateIpAddressManager.allocateAndSetPrivateIpAddress("newInstanceId", "aa:bb:cc:dd:ee:ff", securityGroup1);

        // assert
        assertEquals("172.0.0.4", res);
    }

    @Test
    public void shouldAllocateAlreadyAllocatedPrivateIpAddress() {
        // act
        String res = privateIpAddressManager.allocateAndSetPrivateIpAddress(instanceId2, "aa:bb:cc:dd:ee:ff", securityGroup1);

        // assert
        assertEquals("172.0.0.2", res);
    }

    @Test(expected = NetworkNotFoundException.class)
    public void testAllocatePrivateIpAddressUnknownNet() {
        // act
        privateIpAddressManager.allocateAndSetPrivateIpAddress("newInstanceId", "aa:bb:cc:dd:ee:ff", null);
    }

    @Test(expected = AddressNotAssignedException.class)
    public void testAllocatePrivateIpAddressNoAddressesLeft() {
        // setup
        SecurityGroup fullSecurityGroup = new SecurityGroup("full", "full", 99L, "172.100.0.0", "255.255.255.254", "147.149.2.5", new HashSet<NetworkRule>());

        // act
        privateIpAddressManager.allocateAndSetPrivateIpAddress("newInstanceId", "aa:bb:cc:dd:ee:ff", fullSecurityGroup);
    }

    @Test
    public void testFreePrivateIpAddressForInstance() {
        // act
        privateIpAddressManager.freePrivateIpAddressForInstance(instanceId3, securityGroup2);

        // assert
        assertNull(securityGroup2.getInstances().get(instanceId3));
    }

    @Test
    public void shouldNoOpIfInstanceIdIsNotFound() {
        // act
        privateIpAddressManager.freePrivateIpAddressForInstance("i-notfound", securityGroup2);

        // assert
        assertEquals(1, securityGroup2.getInstances().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAllowDhcpDaemonRefresh() {
        // act
        privateIpAddressManager.refreshDhcpDaemon();

        // assert
        verify(dhcpManager).requestDhcpRefresh(isA(List.class));
    }

    @Test
    public void shouldUpdateInstanceIfInstanceIpDetailsAreNotSetInSecurityGroup() {
        // setup
        String instanceId = "i-4sg3";
        String macAddress = "aa:bb:cc:dd:ee:ff";
        SecurityGroup securityGroup3 = new SecurityGroup("bozo", "default", 10L, "172.0.0.0", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup3.getInstances().put(instanceId, null);

        // act
        privateIpAddressManager.allocateAndSetPrivateIpAddress(instanceId, macAddress, securityGroup3);

        // assert
        assertNotNull(securityGroup3.getInstances().get(instanceId));
        assertEquals("172.0.0.2", securityGroup3.getInstances().get(instanceId).getPrivateIpAddress());
        assertEquals(macAddress, securityGroup3.getInstances().get(instanceId).getMacAddress());
    }
}
