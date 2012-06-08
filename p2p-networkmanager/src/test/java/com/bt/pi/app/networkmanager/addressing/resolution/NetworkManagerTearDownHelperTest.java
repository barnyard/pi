package com.bt.pi.app.networkmanager.addressing.resolution;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
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
import com.bt.pi.app.networkmanager.net.VirtualNetworkBuilder;

@RunWith(MockitoJUnitRunner.class)
public class NetworkManagerTearDownHelperTest {
    private static final String ALLOCATED_PUBLIC_ADDRESS_1 = "1.2.3.4";
    private static final String ALLOCATED_PUBLIC_ADDRESS_2 = "5.6.7.8";
    private static final String ALLOCATED_PUBLIC_ADDRESS_3 = "9.10.11.12";

    private List<SecurityGroup> securityGroups;

    private NetworkManagerTearDownHelper networkManagerTearDownHelper;
    @Mock
    private VirtualNetworkBuilder virtualNetworkBuilder;

    private String instanceWithNoPublicIp = "i-noPublicIpInstance";
    private String instanceId2 = "i-nstanceId2";
    private String instanceId3 = "i-nstanceId3";
    private String instanceId4 = "i-nstanceId4";

    @Before
    public void before() {
        SecurityGroup securityGroup1 = new SecurityGroup("bozo", "default", 10L, "172.0.0.0", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup1.setNetmask("1.1.1.1");
        securityGroup1.getInstances().put(instanceWithNoPublicIp, new InstanceAddress("172.0.0.3", null, "aa:aa:aa:aa:aa:aa"));
        securityGroup1.getInstances().put(instanceId2, new InstanceAddress("172.0.0.2", ALLOCATED_PUBLIC_ADDRESS_1, "aa:aa:aa:aa:aa:aa"));

        SecurityGroup securityGroup2 = new SecurityGroup("nutter", "default", 11L, "172.0.0.16", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup2.setNetmask("1.1.1.1");
        securityGroup2.getInstances().put(instanceId3, new InstanceAddress("172.0.0.18", ALLOCATED_PUBLIC_ADDRESS_2, "aa:aa:aa:aa:aa:aa"));

        SecurityGroup securityGroup3 = new SecurityGroup("abc", "default", 12L, "172.0.0.32", null, null, new HashSet<NetworkRule>());
        securityGroup3.setNetmask("1.1.1.1");
        securityGroup3.getInstances().put(instanceId4, new InstanceAddress("172.0.0.45", ALLOCATED_PUBLIC_ADDRESS_3, "d0:0d:4a:1d:08:f7"));

        securityGroups = new ArrayList<SecurityGroup>();
        securityGroups.add(securityGroup1);
        securityGroups.add(securityGroup2);
        securityGroups.add(securityGroup3);

        networkManagerTearDownHelper = new NetworkManagerTearDownHelper();
        networkManagerTearDownHelper.setVirtualNetworkBuilder(virtualNetworkBuilder);
    }

    @Test
    public void testRemoveAllAddressesFromSecurityGroups() {
        // act
        networkManagerTearDownHelper.removeAllAddressesFromSecurityGroups(securityGroups);

        // verify
        verify(virtualNetworkBuilder, times(3)).tearDownVirtualNetworkForSecurityGroup(isA(SecurityGroup.class));
    }

}
