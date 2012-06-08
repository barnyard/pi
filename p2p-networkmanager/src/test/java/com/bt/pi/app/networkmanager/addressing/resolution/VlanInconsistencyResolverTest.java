package com.bt.pi.app.networkmanager.addressing.resolution;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.Continuation;

import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.common.os.DeviceUtils;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.resource.ConsumedUriResourceRegistry;

@RunWith(MockitoJUnitRunner.class)
public class VlanInconsistencyResolverTest {
    private static final String VNET_PRIVATE_INTERFACE = "eth0";

    private Set<URI> uris;

    @Mock
    private NetworkCommandRunner networkCommandRunner;
    @Mock
    private ConsumedUriResourceRegistry consumedUriResourceRegistry;
    @Mock
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    private DeviceUtils deviceUtils;
    @Mock
    private AddressDeleteQueue addressDeleteQueue;

    @InjectMocks
    private VlanInconsistencyResolver vlanInconsistencyResolver = new VlanInconsistencyResolver();

    @Before
    public void setup() {
        setupSharedImmutableEntityResourceManager();
        setupDeviceUtils();
        setupAddressDeleteQueue();

        vlanInconsistencyResolver.setVnetPrivateInterface(VNET_PRIVATE_INTERFACE);
    }

    private void setupSharedImmutableEntityResourceManager() {
        uris = new HashSet<URI>();
        uris.add(URI.create("vlan:1"));
        uris.add(URI.create("vlan:2"));
        when(consumedUriResourceRegistry.getResourceIdsByScheme(ResourceSchemes.VIRTUAL_NETWORK.toString())).thenReturn(uris);
    }

    @SuppressWarnings("unchecked")
    private void setupDeviceUtils() {
        String device1 = "device1";
        String device2 = "device2";
        String device3 = "device3";
        List<String> deviceList = mock(List.class);

        when(networkCommandRunner.getVlanInterface(1, VNET_PRIVATE_INTERFACE)).thenReturn(device1);
        when(networkCommandRunner.getVlanInterface(2, VNET_PRIVATE_INTERFACE)).thenReturn(device2);
        when(networkCommandRunner.getVlanInterface(3, VNET_PRIVATE_INTERFACE)).thenReturn(device3);
        when(deviceUtils.getDeviceList()).thenReturn(deviceList);
        when(deviceUtils.deviceExists(device1, deviceList)).thenReturn(true);
        when(deviceUtils.deviceExists(device2, deviceList)).thenReturn(false);
        when(deviceUtils.getAllVlanDevicesForInterface(VNET_PRIVATE_INTERFACE, deviceList)).thenReturn(Arrays.asList(new String[] { VNET_PRIVATE_INTERFACE + ".1", VNET_PRIVATE_INTERFACE + ".2", VNET_PRIVATE_INTERFACE + ".3" }));
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

    @Test
    public void shouldAddManagedNetworkForAllVlanIdsInSharedResourceManager() {
        // act
        vlanInconsistencyResolver.refreshVirtualNetworks();

        // assert
        verify(networkCommandRunner, never()).addManagedNetwork(1, VNET_PRIVATE_INTERFACE);
        verify(networkCommandRunner).addManagedNetwork(2, VNET_PRIVATE_INTERFACE);
    }

    @Test
    public void shouldRemoveManagedNetworkForAllPhysicalVlansNotInSharedResourceManager() {
        // act
        vlanInconsistencyResolver.refreshVirtualNetworks();

        // assert
        verify(networkCommandRunner, never()).removeManagedNetwork(1, VNET_PRIVATE_INTERFACE);
        verify(networkCommandRunner, never()).removeManagedNetwork(2, VNET_PRIVATE_INTERFACE);
        verify(networkCommandRunner).removeManagedNetwork(3, VNET_PRIVATE_INTERFACE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddManagedNetworkForSecurityGroupsNotInVlanRegistry() throws Exception {
        // setup
        List<SecurityGroup> securityGroupList = createSecurityGroupList();
        when(consumedDhtResourceRegistry.getByType(SecurityGroup.class)).thenReturn(securityGroupList);

        // act
        vlanInconsistencyResolver.refreshVirtualNetworks();

        // assert
        verify(consumedUriResourceRegistry, never()).registerConsumer(eq(URI.create("vlan:1")), eq("test:secgrp"), isA(Continuation.class));
        verify(consumedUriResourceRegistry, never()).registerConsumer(eq(URI.create("vlan:2")), eq("test:secgrp"), isA(Continuation.class));
        verify(consumedUriResourceRegistry).registerConsumer(eq(URI.create("vlan:3")), eq("test:secgrp"), isA(Continuation.class));
        verify(networkCommandRunner).addManagedNetwork(3, VNET_PRIVATE_INTERFACE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotAddManagedNetworkForNullVlans() {
        // setup
        List<SecurityGroup> securityGroupList = createSecurityGroupList();
        when(consumedDhtResourceRegistry.getByType(SecurityGroup.class)).thenReturn(securityGroupList);
        String ownerId = "test";
        String groupName = "secgrp";
        securityGroupList.add(new SecurityGroup(ownerId, groupName, null, null, null, null, null));

        // act
        vlanInconsistencyResolver.refreshVirtualNetworks();

        // assert
        verify(consumedUriResourceRegistry, never()).registerConsumer(eq(URI.create("vlan:1")), eq("test:secgrp"), isA(Continuation.class));
        verify(consumedUriResourceRegistry, never()).registerConsumer(eq(URI.create("vlan:2")), eq("test:secgrp"), isA(Continuation.class));
        verify(consumedUriResourceRegistry).registerConsumer(eq(URI.create("vlan:3")), eq("test:secgrp"), isA(Continuation.class));
        verify(consumedUriResourceRegistry, never()).registerConsumer(eq(URI.create("vlan:null")), eq("test:secgrp"), isA(Continuation.class));
        verify(networkCommandRunner).addManagedNetwork(3, VNET_PRIVATE_INTERFACE);
    }

    private List<SecurityGroup> createSecurityGroupList() {
        String ownerId = "test";
        String groupName = "secgrp";

        List<SecurityGroup> securityGroupList = new ArrayList<SecurityGroup>();
        securityGroupList.add(new SecurityGroup(ownerId, groupName, 1L, null, null, null, null));
        securityGroupList.add(new SecurityGroup(ownerId, groupName, 2L, null, null, null, null));
        securityGroupList.add(new SecurityGroup(ownerId, groupName, 3L, null, null, null, null));
        return securityGroupList;
    }
}
