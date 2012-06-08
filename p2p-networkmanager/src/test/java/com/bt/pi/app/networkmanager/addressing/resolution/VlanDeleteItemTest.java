package com.bt.pi.app.networkmanager.addressing.resolution;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.net.NetworkCommandRunner;

public class VlanDeleteItemTest {
    private NetworkCommandRunner networkCommandRunner;
    private VlanDeleteItem vlanDeleteItem;
    private long vlanId = 100;
    private String iface = "eth0";

    @Before
    public void before() {
        networkCommandRunner = mock(NetworkCommandRunner.class);
        vlanDeleteItem = new VlanDeleteItem(vlanId, iface, networkCommandRunner);
    }

    @Test
    public void shouldDeleteVlanItemByRemovingManagedNetwork() {
        // setup

        // act
        vlanDeleteItem.delete();

        // assert
        verify(networkCommandRunner).removeManagedNetwork(vlanId, iface);
    }
}
