package com.bt.pi.app.networkmanager.net;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.app.networkmanager.iptables.IpTablesUpdateException;
import com.bt.pi.core.application.resource.ConsumedUriResourceRegistry;
import com.bt.pi.core.continuation.LoggingContinuation;

@RunWith(MockitoJUnitRunner.class)
public class VirtualNetworkBuilderTest {
    private URI vlan_10 = URI.create(String.format("%s:10", ResourceSchemes.VIRTUAL_NETWORK));
    @InjectMocks
    private VirtualNetworkBuilder virtualNetworkBuilder = new VirtualNetworkBuilder();
    @Mock
    private NetworkCommandRunner networkCommandRunner;
    @Mock
    private IpTablesManager ipTablesManager;
    private SecurityGroup securityGroup;
    @Mock
    private ConsumedUriResourceRegistry consumedUriResourceRegistry;
    private String privateInterface = "eth0";

    @Before
    public void before() {
        securityGroup = new SecurityGroup("bozo", "default", 10L, "172.0.0.0", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup.getInstances().put("i-123", new InstanceAddress("172.0.0.3", null, "aa:aa:aa:aa:aa:aa"));
        securityGroup.getInstances().put("i-456", new InstanceAddress("172.0.0.2", "1.2.3.4", "aa:aa:aa:aa:aa:aa"));

        virtualNetworkBuilder.setVnetInterface(privateInterface);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSetUpVirtualNetworForSecGroup() throws Exception {
        // act
        virtualNetworkBuilder.setUpVirtualNetworkForSecurityGroup(securityGroup);

        // assert
        verify(this.ipTablesManager).refreshIpTables();
        verify(this.networkCommandRunner).addManagedNetwork(10, "eth0");
        verify(this.networkCommandRunner).addGatewayIp("172.0.0.1", 28, "172.0.0.15", "pibr" + 10);
        verify(this.consumedUriResourceRegistry).registerConsumer(eq(vlan_10), eq("bozo:default"), isA(LoggingContinuation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSetUpVirtualNetworForInstance() throws Exception {
        // act
        virtualNetworkBuilder.setUpVirtualNetworkForInstance(10L, "i-456");

        // assert
        verify(this.networkCommandRunner).addManagedNetwork(10, "eth0");
        verify(this.ipTablesManager, never()).refreshIpTables();
        verify(this.networkCommandRunner, never()).addGatewayIp(anyString(), anyInt(), anyString(), anyString());
        verify(this.consumedUriResourceRegistry).registerConsumer(eq(vlan_10), eq("i-456"), isA(LoggingContinuation.class));
    }

    @Test(expected = NetworkCreationException.class)
    public void shouldThrowWhenFailedToSetupVlanForSecurityGroup() {
        // setup
        doThrow(new IpTablesUpdateException("oops")).when(this.networkCommandRunner).addManagedNetwork(10, "eth0");

        // act
        virtualNetworkBuilder.setUpVirtualNetworkForSecurityGroup(securityGroup);
    }

    @Test
    public void testStopNetworkForSecurityGroup() {
        // setup
        when(this.consumedUriResourceRegistry.deregisterConsumer(vlan_10, "bozo:default")).thenReturn(true);

        // act
        virtualNetworkBuilder.tearDownVirtualNetworkForSecurityGroup(securityGroup);

        // assert
        verify(this.networkCommandRunner).removeManagedNetwork(10, "eth0");
        verify(this.ipTablesManager).refreshIpTables();
        verify(this.networkCommandRunner).deleteGatewayIp("172.0.0.1", 28, "172.0.0.15", 10);
        verify(this.consumedUriResourceRegistry).deregisterConsumer(vlan_10, "bozo:default");
    }

    @Test
    public void testStopNetworkForInstance() {
        // setup
        when(this.consumedUriResourceRegistry.deregisterConsumer(vlan_10, "i-456")).thenReturn(true);

        // act
        virtualNetworkBuilder.tearDownVirtualNetworkForInstance(10, "i-456");

        // assert
        verify(this.networkCommandRunner).removeManagedNetwork(10, "eth0");
        verify(this.ipTablesManager, never()).refreshIpTables();
        verify(this.networkCommandRunner, never()).deleteGatewayIp(anyString(), anyInt(), anyString(), anyLong());
        verify(this.consumedUriResourceRegistry).deregisterConsumer(vlan_10, "i-456");
    }

    @Test
    public void testStopNetworkForSecurityGroupWhenInstanceConsumersRemain() {
        // setup
        when(this.consumedUriResourceRegistry.deregisterConsumer(vlan_10, "bozo:default")).thenReturn(false);

        // act
        virtualNetworkBuilder.tearDownVirtualNetworkForSecurityGroup(securityGroup);

        // assert
        verify(this.networkCommandRunner, never()).removeManagedNetwork(anyLong(), anyString());
        verify(this.ipTablesManager).refreshIpTables();
        verify(this.networkCommandRunner).deleteGatewayIp("172.0.0.1", 28, "172.0.0.15", 10);
        verify(this.consumedUriResourceRegistry).deregisterConsumer(vlan_10, "bozo:default");
    }

    @Test
    public void testStopNetworkForInstanceWhenSecurityGroupConsumersRemain() {
        // setup
        when(this.consumedUriResourceRegistry.deregisterConsumer(vlan_10, "i-456")).thenReturn(false);

        // act
        virtualNetworkBuilder.tearDownVirtualNetworkForInstance(10, "i-456");

        // assert
        verify(this.networkCommandRunner, never()).removeManagedNetwork(anyLong(), anyString());
        verify(this.ipTablesManager, never()).refreshIpTables();
        verify(this.networkCommandRunner, never()).deleteGatewayIp(anyString(), anyInt(), anyString(), anyLong());
        verify(this.consumedUriResourceRegistry).deregisterConsumer(vlan_10, "i-456");
    }

    @Test
    public void testUpdateNetworkRules() {
        // act
        this.virtualNetworkBuilder.refreshNetwork();

        // assert
        verify(this.ipTablesManager).refreshIpTables();
    }

    @Test
    public void testRefreshXenVifOnBridge() {
        // setup
        long vlanId = 12;
        long domainId = 34;

        // act
        this.virtualNetworkBuilder.refreshXenVifOnBridge(vlanId, domainId);

        // assert
        verify(this.networkCommandRunner).refreshXenVifOnBridge(vlanId, domainId);
    }
}
