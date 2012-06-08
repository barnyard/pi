package com.bt.pi.app.networkmanager.handlers;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.networkmanager.addressing.resolution.NetworkInconsistencyResolver;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.id.PId;

public class NetworkCleanupHandlerTest {
    private NetworkCleanupHandler networkCleanupHandler;
    private NetworkInconsistencyResolver addressInconsistencyResolver;
    private IpTablesManager ipTablesManager;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private PId id;

    @Before
    public void before() {
        id = mock(PId.class);

        SecurityGroup group = mock(SecurityGroup.class);
        ArrayList<SecurityGroup> groups = new ArrayList<SecurityGroup>();
        groups.add(group);

        addressInconsistencyResolver = mock(NetworkInconsistencyResolver.class);
        ipTablesManager = mock(IpTablesManager.class);
        consumedDhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);
        when(consumedDhtResourceRegistry.getCachedEntity(eq(id))).thenReturn(group);
        when(consumedDhtResourceRegistry.getByType(eq(SecurityGroup.class))).thenReturn(groups);

        networkCleanupHandler = new NetworkCleanupHandler();
        networkCleanupHandler.setNetworkInconsistencyResolver(addressInconsistencyResolver);
        networkCleanupHandler.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
        networkCleanupHandler.setIpTablesManager(ipTablesManager);
    }

    @Test
    public void shouldBeAbleToReleaseNetworkAndPublicIpAddressForAllSecGroups() {
        // act
        networkCleanupHandler.releaseAllSecurityGroups();

        // assert
        verify(consumedDhtResourceRegistry).clearAll(SecurityGroup.class);
        verify(addressInconsistencyResolver).refreshIpAddressesOnPublicInterfaceAndBridges();
        verify(addressInconsistencyResolver).tearDownNetworkManagerAddressesForGroups(anyListOf(SecurityGroup.class));
        verify(ipTablesManager).refreshIpTables();
    }

    @Test
    public void shouldBeAbleToReleaseNetworkAndPublicIpAddressForASingleSecGroup() {
        // act
        networkCleanupHandler.releaseSecurityGroup(id);

        // assert
        verify(consumedDhtResourceRegistry).clearResource(id);
        verify(addressInconsistencyResolver).refreshIpAddressesOnPublicInterfaceAndBridges();
        verify(ipTablesManager).refreshIpTables();
        verify(addressInconsistencyResolver).tearDownNetworkManagerAddressesForGroups(anyListOf(SecurityGroup.class));
    }
}
