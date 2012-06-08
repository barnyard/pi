package com.bt.pi.app.networkmanager.addressing.resolution;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class AddressInconsistencyResolverTest {
    private static final String ALLOCATED_PUBLIC_ADDRESS_1 = "1.2.3.4";
    private static final String ALLOCATED_PUBLIC_ADDRESS_2 = "5.6.7.8";
    private static final String ALLOCATED_PUBLIC_ADDRESS_3 = "9.10.11.12";
    private static final String UNALLOCATED_ADDRESS = "13.14.15.16";
    private static final String VNET_PUBLIC_INTERFACE = "eth1";

    private String instanceWithNoPublicIp = "i-noPublicIpInstance";
    private String instanceId1 = "i-nstanceId";
    private String instanceId2 = "i-nstanceId2";
    private String instanceId3 = "i-nstanceId3";
    private String instanceId4 = "i-nstanceId4";

    @Mock
    private PId publicIpIndexId;
    @Mock
    private PId subnetAllocationIndexId;

    private PublicIpAllocationIndex publicIpIndex;
    private SubnetAllocationIndex subnetAllocationIndex;
    private List<SecurityGroup> securityGroups;

    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    private DhtCache dhtCache;
    @Mock
    private NetworkCommandRunner networkCommandRunner;
    @Mock
    private AddressDeleteQueue addressDeleteQueue;
    @Mock
    private Executor executor;

    private AddressInconsistencyResolver addressInconsistencyResolver = new AddressInconsistencyResolver();

    @Before
    public void setup() {
        setupSharedResourceManager();
        setupPiIdBuilder();
        setupAddressDeleteQueue();
        setupDhtCache();
        setupExecutor();

        addressInconsistencyResolver.setVnetPublicInterface(VNET_PUBLIC_INTERFACE);
        addressInconsistencyResolver.setAddressDeleteQueue(addressDeleteQueue);
        addressInconsistencyResolver.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
        addressInconsistencyResolver.setDhtCache(dhtCache);
        addressInconsistencyResolver.setNetworkCommandRunner(networkCommandRunner);
        addressInconsistencyResolver.setPiIdBuilder(piIdBuilder);
        addressInconsistencyResolver.setExecutor(executor);
    }

    private void setupExecutor() {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(executor).execute(isA(Runnable.class));
    }

    private void setupSharedResourceManager() {
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

        when(consumedDhtResourceRegistry.getByType(eq(SecurityGroup.class))).thenReturn(securityGroups);
    }

    private void setupPiIdBuilder() {
        when(piIdBuilder.getPId(PublicIpAllocationIndex.URL)).thenReturn(publicIpIndexId);
        when(publicIpIndexId.forLocalRegion()).thenReturn(publicIpIndexId);
        when(piIdBuilder.getPId(SubnetAllocationIndex.URL)).thenReturn(subnetAllocationIndexId);
        when(subnetAllocationIndexId.forLocalRegion()).thenReturn(subnetAllocationIndexId);
    }

    private void setupAddressDeleteQueue() {
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                ((AddressDeleteQueueItem) invocation.getArguments()[0]).delete();
                return true;
            }
        }).when(addressDeleteQueue).add(isA(AddressDeleteQueueItem.class));
    }

    @SuppressWarnings("unchecked")
    private void setupDhtCache() {
        publicIpIndex = new PublicIpAllocationIndex();
        publicIpIndex.getAllocationMap().put(IpAddressUtils.ipToLong(ALLOCATED_PUBLIC_ADDRESS_1), new InstanceRecord(instanceId1, "userId"));
        publicIpIndex.getAllocationMap().put(IpAddressUtils.ipToLong(ALLOCATED_PUBLIC_ADDRESS_2), new InstanceRecord(instanceId3, "abuserId"));

        Set<ResourceRange> ranges = new HashSet<ResourceRange>();
        ranges.add(new ResourceRange(IpAddressUtils.ipToLong("1.2.3.1"), IpAddressUtils.ipToLong("1.2.3.5")));
        ranges.add(new ResourceRange(IpAddressUtils.ipToLong("5.6.7.1"), IpAddressUtils.ipToLong("5.6.7.10")));
        ranges.add(new ResourceRange(IpAddressUtils.ipToLong("9.10.11.1"), IpAddressUtils.ipToLong("9.10.11.15")));
        ranges.add(new ResourceRange(IpAddressUtils.ipToLong("13.14.15.1"), IpAddressUtils.ipToLong("13.14.15.20")));
        publicIpIndex.setResourceRanges(ranges);

        subnetAllocationIndex = new SubnetAllocationIndex();
        Set<ResourceRange> subnetRanges = new HashSet<ResourceRange>();
        subnetRanges.add(new ResourceRange(IpAddressUtils.ipToLong("1.2.3.1"), IpAddressUtils.ipToLong("1.2.3.5")));
        subnetRanges.add(new ResourceRange(IpAddressUtils.ipToLong("5.6.7.1"), IpAddressUtils.ipToLong("5.6.7.10")));
        subnetAllocationIndex.setResourceRanges(subnetRanges);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].equals(publicIpIndexId))
                    ((PiContinuation<PublicIpAllocationIndex>) invocation.getArguments()[1]).handleResult(publicIpIndex);
                else if (invocation.getArguments()[0].equals(subnetAllocationIndexId))
                    ((PiContinuation<SubnetAllocationIndex>) invocation.getArguments()[1]).handleResult(subnetAllocationIndex);
                return null;
            }
        }).when(dhtCache).get(isA(PId.class), isA(PiContinuation.class));

    }

    @Test
    public void refreshIPsShouldRemoveIPAddressesNotInSecurityGroups() {
        // setup
        spoofAddressesOnDevice(ALLOCATED_PUBLIC_ADDRESS_1, ALLOCATED_PUBLIC_ADDRESS_2, ALLOCATED_PUBLIC_ADDRESS_3, UNALLOCATED_ADDRESS);

        // act
        addressInconsistencyResolver.refreshIpAddressesOnPublicInterface();

        // assert
        verify(networkCommandRunner).ipAddressDelete(UNALLOCATED_ADDRESS + "/32", VNET_PUBLIC_INTERFACE);

    }

    @Test
    public void refreshIPsShouldAddIPAddressesNotOnAdapter() {
        // setup
        spoofAddressesOnDevice(ALLOCATED_PUBLIC_ADDRESS_1, ALLOCATED_PUBLIC_ADDRESS_2);

        // act
        addressInconsistencyResolver.refreshIpAddressesOnPublicInterface();

        // assert
        verify(networkCommandRunner).addIpAddressAndSendArping(ALLOCATED_PUBLIC_ADDRESS_3 + "/32", VNET_PUBLIC_INTERFACE);
    }

    @Test
    public void refreshIPsShouldNotRemoveAddressesThatArentPublicAddresses() {
        // setup
        final String notPublicAddress = "200.200.200.200";
        spoofAddressesOnDevice(ALLOCATED_PUBLIC_ADDRESS_1, ALLOCATED_PUBLIC_ADDRESS_2, ALLOCATED_PUBLIC_ADDRESS_3, notPublicAddress);

        // act
        addressInconsistencyResolver.refreshIpAddressesOnPublicInterface();

        // assert
        verify(networkCommandRunner, never()).ipAddressDelete(anyString(), anyString());
        verify(networkCommandRunner, never()).ipAddressAdd(anyString(), anyString());
    }

    @Test
    public void refreshIPsShouldMakeNoChangesWhenNoneAreNeeded() {
        // setup
        spoofAddressesOnDevice(ALLOCATED_PUBLIC_ADDRESS_1, ALLOCATED_PUBLIC_ADDRESS_2, ALLOCATED_PUBLIC_ADDRESS_3);

        // act
        addressInconsistencyResolver.refreshIpAddressesOnPublicInterface();

        // assert
        verify(networkCommandRunner, never()).ipAddressDelete(anyString(), anyString());
        verify(networkCommandRunner, never()).ipAddressAdd(anyString(), anyString());
    }

    @Test
    public void shouldAddIpAddressOnPiBridgeIfSecurityGroupHasThem() {
        // setup
        Map<String, String> allPiBridgeAddresses = new HashMap<String, String>();
        allPiBridgeAddresses.put("172.0.0.1/28", "pibr101");
        allPiBridgeAddresses.put("1.2.3.4/28", "pibr102");

        when(networkCommandRunner.getAllAddressesOnAllPiBridges()).thenReturn(allPiBridgeAddresses);

        // act
        addressInconsistencyResolver.refreshIpAddressesOnBridges();

        // assert
        for (SecurityGroup securityGroup : securityGroups)
            verify(networkCommandRunner).addGatewayIp(securityGroup.getRouterAddress(), securityGroup.getSlashnet(), securityGroup.getBroadcastAddress(), VlanAddressUtils.getBridgeNameForVlan(securityGroup.getVlanId()));
    }

    private void spoofAddressesOnDevice(String... publicAddresses) {
        List<String> publicAddressList = new ArrayList<String>();
        for (int i = 0; i < publicAddresses.length; i++)
            publicAddressList.add(publicAddresses[i]);

        when(networkCommandRunner.getAllAddressesOnDevice(VNET_PUBLIC_INTERFACE)).thenReturn(publicAddressList);
    }

}
