package com.bt.pi.app.networkmanager.addressing.resolution;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.core.node.NodeStartedEvent;

@RunWith(MockitoJUnitRunner.class)
public class NetworkInconsistencyResolverTest {
    @Mock
    private AddressInconsistencyResolver addressInconsistencyResolver;
    @Mock
    private VlanInconsistencyResolver vlanInconsistencyResolver;
    @Mock
    private NetworkManagerTearDownHelper networkManagerTearDownHelper;

    private NetworkInconsistencyResolver networkInconsistencyResolver = new NetworkInconsistencyResolver();

    @Before
    public void before() {
        networkInconsistencyResolver.setAddressInconsistencyResolver(addressInconsistencyResolver);
        networkInconsistencyResolver.setVlanInconsistencyResolver(vlanInconsistencyResolver);
        networkInconsistencyResolver.setNetworkManagerTearDownHelper(networkManagerTearDownHelper);
    }

    @Test
    public void testRefreshCallsTheAppropriateHandlers() {
        // act
        networkInconsistencyResolver.refreshIpAddressesOnPublicInterfaceAndBridges();

        // assert
        verify(addressInconsistencyResolver, never()).refreshIpAddressesOnBridges();
        verify(addressInconsistencyResolver, never()).refreshIpAddressesOnPublicInterface();
        verify(vlanInconsistencyResolver, never()).refreshVirtualNetworks();
    }

    @Test
    public void testRefreshCallsTheAppropriateHandlersWhenNodeIsStarted() {
        // setup
        networkInconsistencyResolver.onApplicationEvent(new NodeStartedEvent(new Object()));

        // act
        networkInconsistencyResolver.refreshIpAddressesOnPublicInterfaceAndBridges();

        // assert
        verify(addressInconsistencyResolver).refreshIpAddressesOnBridges();
        verify(addressInconsistencyResolver).refreshIpAddressesOnPublicInterface();
        verify(vlanInconsistencyResolver).refreshVirtualNetworks();
    }

    @Test
    public void test() {
        // setup
        ArrayList<SecurityGroup> groups = new ArrayList<SecurityGroup>();

        // act
        networkInconsistencyResolver.tearDownNetworkManagerAddressesForGroups(groups);

        // verify
        verify(networkManagerTearDownHelper).removeAllAddressesFromSecurityGroups(eq(groups));
    }
}
