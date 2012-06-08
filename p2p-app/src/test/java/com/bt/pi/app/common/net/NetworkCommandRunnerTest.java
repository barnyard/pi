package com.bt.pi.app.common.net;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.os.DeviceUtils;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.util.common.CommandLineParser;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class NetworkCommandRunnerTest {
    private static final int vlanId = 372;
    private static final int mtuSize = 1496;
    @InjectMocks
    private NetworkCommandRunner networkCommandRunner = new NetworkCommandRunner();
    @Mock
    private CommandRunner commandRunner;
    @Mock
    private DeviceUtils deviceUtils;
    private long domainId = 34;
    private String bridgeName = "pibr" + vlanId;
    private String vifName = "vif" + domainId + ".0";

    @Before
    public void before() {
        when(this.deviceUtils.deviceExists(String.format("pibr%d", vlanId))).thenReturn(false);
        when(this.deviceUtils.deviceExists(String.format("eth0.%d", vlanId))).thenReturn(false);
        networkCommandRunner.setMtuSize(mtuSize);
    }

    @Test
    public void testIpAddressAdd() throws Exception {
        // setup
        when(deviceUtils.deviceHasAddress("eth0", "12.12.12.12")).thenReturn(false);

        // act
        networkCommandRunner.ipAddressAdd("12.12.12.12", "eth0");

        // assert
        verify(this.commandRunner).run(eq("ip addr add 12.12.12.12/32 dev eth0"), eq(10000L));
    }

    @Test
    public void testIpAddressAddWithNetworkSize() throws Exception {
        // setup
        when(deviceUtils.deviceHasAddress("eth0", "12.12.12.12/24")).thenReturn(false);

        // act
        networkCommandRunner.ipAddressAdd("12.12.12.12/24", "eth0");

        // assert
        verify(this.commandRunner).run(eq("ip addr add 12.12.12.12/24 dev eth0"), eq(10000L));
    }

    @Test
    public void testIpAddressAddDoesNothingWhenAddressAlreadyOnDevice() throws Exception {
        // setup
        when(deviceUtils.deviceHasAddress("eth0", "12.12.12.12")).thenReturn(true);

        // act
        networkCommandRunner.ipAddressAdd("12.12.12.12", "eth0");

        // assert
        verify(this.commandRunner, never()).run(eq("ip addr add 12.12.12.12/32 dev eth0"));
    }

    @Test
    @org.junit.Ignore
    public void realTestForActualIpAddrAddFailureOnLinux() {
        // setup
        commandRunner = new CommandRunner();
        commandRunner.setExecutor(Executors.newFixedThreadPool(4));
        commandRunner.setCommandLineParser(new CommandLineParser());

        deviceUtils = new DeviceUtils();
        deviceUtils.setCommandRunner(commandRunner);

        networkCommandRunner = new NetworkCommandRunner();
        networkCommandRunner.setDeviceUtils(deviceUtils);
        networkCommandRunner.setCommandRunner(commandRunner);

        /*
         * IMPORTANT - check this ip addr and i/f before running manually!
         * =========================================================
         */
        String ipAddress = "10.11.12.13/32";
        String iface = "eth1";
        /*
         * =========================================================
         */

        // act
        try {
            networkCommandRunner.ipAddressAdd(ipAddress, iface);
            networkCommandRunner.ipAddressAdd(ipAddress, iface);
        } catch (CommandExecutionException e) {
            System.err.println(e.getErrorLines());
            e.printStackTrace();
        }
    }

    @Test
    public void testIpAddressDelete() throws Exception {
        // setup
        when(deviceUtils.deviceExists("eth0")).thenReturn(true);
        when(deviceUtils.deviceHasAddress("eth0", "12.12.12.12")).thenReturn(true);

        // act
        networkCommandRunner.ipAddressDelete("12.12.12.12", "eth0");

        // assert
        verify(this.commandRunner).run(eq("ip addr del 12.12.12.12/32 dev eth0"), eq(10000L));
    }

    @Test
    public void testIpAddressesDelete() throws Exception {
        // setup
        when(deviceUtils.deviceExists("eth0")).thenReturn(true);
        when(deviceUtils.deviceHasAddress("eth0", "12.12.12.12")).thenReturn(true);
        when(deviceUtils.deviceHasAddress("eth0", "34.34.34.34")).thenReturn(true);

        // act
        networkCommandRunner.ipAddressesDelete(Arrays.asList(new String[] { "12.12.12.12", "34.34.34.34" }), "eth0");

        // assert
        verify(this.commandRunner).run(eq("ip addr del 12.12.12.12/32 dev eth0"), eq(10000L));
        verify(this.commandRunner).run(eq("ip addr del 34.34.34.34/32 dev eth0"), eq(10000L));
    }

    @Test
    public void testIpAddressesDeleteWithNetworkSize() throws Exception {
        // setup
        when(deviceUtils.deviceExists("eth0")).thenReturn(true);
        when(deviceUtils.deviceHasAddress("eth0", "12.12.12.12/24")).thenReturn(true);
        when(deviceUtils.deviceHasAddress("eth0", "34.34.34.34/28")).thenReturn(true);

        // act
        networkCommandRunner.ipAddressesDelete(Arrays.asList(new String[] { "12.12.12.12/24", "34.34.34.34/28" }), "eth0");

        // assert
        verify(this.commandRunner).run(eq("ip addr del 12.12.12.12/24 dev eth0"), eq(10000L));
        verify(this.commandRunner).run(eq("ip addr del 34.34.34.34/28 dev eth0"), eq(10000L));
    }

    @Test
    public void testIpAddressesDeleteWhenNoInterface() throws Exception {
        // setup
        when(deviceUtils.deviceExists("eth0")).thenReturn(false);

        // act
        networkCommandRunner.ipAddressesDelete(Arrays.asList(new String[] { "12.12.12.12", "34.34.34.34" }), "eth0");

        // assert
        verify(this.commandRunner, never()).run(anyString());
    }

    @Test
    public void testIpAddressesDeleteWhenNoAddressOnInterface() throws Exception {
        // setup
        when(deviceUtils.deviceExists("eth0")).thenReturn(true);
        when(deviceUtils.deviceHasAddress("eth0", "12.12.12.12")).thenReturn(false);
        when(deviceUtils.deviceHasAddress("eth0", "34.34.34.34")).thenReturn(true);

        // act
        networkCommandRunner.ipAddressesDelete(Arrays.asList(new String[] { "12.12.12.12", "34.34.34.34" }), "eth0");

        // assert
        verify(this.commandRunner, never()).run(eq("ip addr del 12.12.12.12/32 dev eth0"));
        verify(this.commandRunner).run(eq("ip addr del 34.34.34.34/32 dev eth0"), eq(10000L));
    }

    @Test
    public void testAddNetwork() throws Exception {
        // setup
        CommandResult commandResult1 = new CommandResult(0, Arrays.asList(new String[] {}), Arrays.asList(new String[] {}));
        when(this.commandRunner.run(eq("cat /proc/net/dev"))).thenReturn(commandResult1);

        // act
        networkCommandRunner.addManagedNetwork(vlanId, "eth0");

        // assert
        verify(this.commandRunner).run(eq("vconfig add eth0 " + vlanId), eq(10000L));
        verify(this.commandRunner).run(eq(String.format("brctl addbr pibr%d", vlanId)), eq(10000L));
        verify(this.commandRunner).run(eq(String.format("brctl addif pibr%d eth0.%d", vlanId, vlanId)), eq(10000L));
        verify(this.commandRunner).run(eq(String.format("ip link set dev pibr%d up", vlanId)));
        verify(this.commandRunner).run(eq(String.format("ip link set dev eth0.%d up mtu %s", vlanId, mtuSize)));
    }

    @Test
    public void testAddNetworkDoesNotAddVirtualIntercaceIfAlreadyPresent() throws Exception {
        // setup
        CommandResult commandResult1 = new CommandResult(0, Arrays.asList(new String[] {}), Arrays.asList(new String[] {}));
        when(this.commandRunner.run(eq("cat /proc/net/dev"))).thenReturn(commandResult1);

        when(deviceUtils.deviceExists(String.format("eth0.%d", vlanId))).thenReturn(true);

        // act
        networkCommandRunner.addManagedNetwork(vlanId, "eth0");

        // assert
        verify(this.commandRunner, never()).run(eq("vconfig add eth0 " + vlanId), eq(10000L));
        verify(this.commandRunner).run(eq(String.format("brctl addbr pibr%d", vlanId)), eq(10000L));
        verify(this.commandRunner).run(eq(String.format("brctl addif pibr%d eth0.%d", vlanId, vlanId)), eq(10000L));
        verify(this.commandRunner).run(eq(String.format("ip link set dev pibr%d up", vlanId)));
        verify(this.commandRunner).run(eq(String.format("ip link set dev eth0.%d up mtu %s", vlanId, mtuSize)));
    }

    @Test
    public void testAddNetworkDoesNotAddBridgeIfAlreadyPresent() throws Exception {
        // setup
        CommandResult commandResult1 = new CommandResult(0, Arrays.asList(new String[] {}), Arrays.asList(new String[] {}));
        when(this.commandRunner.run(eq("cat /proc/net/dev"))).thenReturn(commandResult1);

        when(deviceUtils.deviceExists(String.format("pibr%d", vlanId))).thenReturn(true);

        // act
        networkCommandRunner.addManagedNetwork(vlanId, "eth0");

        // assert
        verify(this.commandRunner).run(eq("vconfig add eth0 " + vlanId), eq(10000L));
        verify(this.commandRunner, never()).run(eq(String.format("brctl addbr pibr%d", vlanId)), eq(10000L));
        verify(this.commandRunner).run(eq(String.format("brctl addif pibr%d eth0.%d", vlanId, vlanId)), eq(10000L));
        verify(this.commandRunner).run(eq(String.format("ip link set dev pibr%d up", vlanId)));
        verify(this.commandRunner).run(eq(String.format("ip link set dev eth0.%d up mtu %s", vlanId, mtuSize)));
    }

    @Test
    public void testAddNetworkDoesNotEnslaveVirtualInterfaceWhenAlreadyEnslaved() throws Exception {
        // setup
        CommandResult commandResult1 = new CommandResult(0, Arrays.asList(new String[] {}), Arrays.asList(new String[] {}));
        when(this.commandRunner.run(eq("cat /proc/net/dev"))).thenReturn(commandResult1);

        when(deviceUtils.deviceEnslavedToBridge(String.format("eth0.%d", vlanId), String.format("pibr%d", vlanId))).thenReturn(true);

        // act
        networkCommandRunner.addManagedNetwork(vlanId, "eth0");

        // assert
        verify(this.commandRunner).run(eq("vconfig add eth0 " + vlanId), eq(10000L));
        verify(this.commandRunner).run(eq(String.format("brctl addbr pibr%d", vlanId)), eq(10000L));
        verify(this.commandRunner, never()).run(eq(String.format("brctl addif pibr%d eth0.%d", vlanId, vlanId)), eq(10000L));
        verify(this.commandRunner).run(eq(String.format("ip link set dev pibr%d up", vlanId)));
        verify(this.commandRunner).run(eq(String.format("ip link set dev eth0.%d up mtu %s", vlanId, mtuSize)));
    }

    @Test
    public void shouldAddManagedNodeNetworkIfTheDeviceAndBridgeDoesntExist() {
        // setup
        String publicInterface = "eth0";
        int vlan = 100;

        // act
        this.networkCommandRunner.addManagedNetwork(vlan, publicInterface);

        // assert
        verify(this.commandRunner).run(eq("vconfig add eth0 " + vlan), eq(10000L));
        verify(this.commandRunner).run(eq("brctl addbr pibr100"), eq(10000L));
        verify(this.commandRunner).run(eq("brctl addif pibr100 eth0.100"), eq(10000L));
        verify(this.commandRunner).run(eq("ip link set dev pibr100 up"));
        verify(this.commandRunner).run(eq("ip link set dev eth0.100 up mtu 1496"));
    }

    @Test
    public void shouldNotAddTaggedInterfaceIfItAlreadyExistsOnNode() {
        // setup
        String publicInterface = "eth0";
        int vlan = 100;

        when(this.deviceUtils.deviceExists("eth0.100")).thenReturn(true);

        // act
        this.networkCommandRunner.addManagedNetwork(vlan, publicInterface);

        // assert
        verify(this.commandRunner, never()).run(eq("vconfig add eth0 " + vlan), eq(10000L));
        verify(this.commandRunner).run(eq("brctl addbr pibr100"), eq(10000L));
        verify(this.commandRunner).run(eq("brctl addif pibr100 eth0.100"), eq(10000L));
        verify(this.commandRunner).run(eq("ip link set dev pibr100 up"));
        verify(this.commandRunner).run(eq("ip link set dev eth0.100 up mtu 1496"));
    }

    @Test
    public void shouldNotAddBridgeIfItAlreadyExistsOnNode() {
        // setup
        String publicInterface = "eth0";
        int vlan = 100;

        when(this.deviceUtils.deviceExists("pibr100")).thenReturn(true);

        // act
        this.networkCommandRunner.addManagedNetwork(vlan, publicInterface);

        // assert
        verify(this.commandRunner).run(eq("vconfig add eth0 " + vlan), eq(10000L));
        verify(this.commandRunner, never()).run(eq("brctl addbr pibr100"), eq(10000L));
        verify(this.commandRunner).run(eq("brctl addif pibr100 eth0.100"), eq(10000L));
        verify(this.commandRunner).run(eq("ip link set dev pibr100 up"));
        verify(this.commandRunner).run(eq("ip link set dev eth0.100 up mtu 1496"));
    }

    @Test
    public void shouldNotAddTaggedInterfaceAndBridgeIfTheyAlreadyExist() {
        // setup
        String publicInterface = "eth0";
        int vlan = 100;

        when(this.deviceUtils.deviceExists("eth0.100")).thenReturn(true);
        when(this.deviceUtils.deviceExists("pibr100")).thenReturn(true);

        // act
        this.networkCommandRunner.addManagedNetwork(vlan, publicInterface);

        // assert
        verify(this.commandRunner, never()).run(eq("vconfig add eth0 " + vlan), eq(10000L));
        verify(this.commandRunner, never()).run(eq("brctl addbr pibr100"), eq(10000L));
        verify(this.commandRunner).run(eq("brctl addif pibr100 eth0.100"), eq(10000L));
        verify(this.commandRunner).run(eq("ip link set dev pibr100 up"));
        verify(this.commandRunner).run(eq("ip link set dev eth0.100 up mtu 1496"));
    }

    @Test
    public void testRemoveNetwork() throws Exception {
        // setup
        CommandResult commandResult1 = new CommandResult(0, Arrays.asList(new String[] {}), Arrays.asList(new String[] {}));
        when(this.commandRunner.run(eq("cat /proc/net/dev"))).thenReturn(commandResult1);

        when(deviceUtils.deviceExists(String.format("eth0.%d", vlanId))).thenReturn(true);
        when(deviceUtils.deviceExists(String.format("pibr%d", vlanId))).thenReturn(true);

        // act
        networkCommandRunner.removeManagedNetwork(vlanId, "eth0");

        // assert
        verify(this.commandRunner).run(String.format("ip link set dev eth0.%d down", vlanId), 10000L);
        verify(this.commandRunner).run(String.format("ip link set dev pibr%d down", vlanId), 10000L);
        verify(this.commandRunner).run(String.format("brctl delbr pibr%d", vlanId), 10000L);
        verify(this.commandRunner).run(String.format("vconfig rem eth0.%d", vlanId), 10000L);
    }

    @Test
    public void testRemoveNetworkDoesNothingWhenVirtualInterfaceDevicesUnknown() throws Exception {
        // setup
        CommandResult commandResult1 = new CommandResult(0, Arrays.asList(new String[] {}), Arrays.asList(new String[] {}));
        when(this.commandRunner.run(eq("cat /proc/net/dev"))).thenReturn(commandResult1);

        when(deviceUtils.deviceExists(String.format("eth0.%d", vlanId))).thenReturn(false);
        when(deviceUtils.deviceExists(String.format("pibr%d", vlanId))).thenReturn(true);

        // act
        networkCommandRunner.removeManagedNetwork(vlanId, "eth0");

        // assert
        verify(this.commandRunner).run(String.format("ip link set dev pibr%d down", vlanId), 10000L);
        verify(this.commandRunner, never()).run(String.format("ip link set dev eth0.%d down", vlanId), 10000L);
        verify(this.commandRunner, never()).run(String.format("vconfig rem eth0.%d", vlanId), 10000L);
    }

    @Test
    public void testRemoveNetworkDoesNothingWhenBridgeDevicesUnknown() throws Exception {
        // setup
        CommandResult commandResult1 = new CommandResult(0, Arrays.asList(new String[] {}), Arrays.asList(new String[] {}));
        when(this.commandRunner.run(eq("cat /proc/net/dev"))).thenReturn(commandResult1);

        when(deviceUtils.deviceExists(String.format("eth0.%d", vlanId))).thenReturn(true);
        when(deviceUtils.deviceExists(String.format("pibr%d", vlanId))).thenReturn(false);

        // act
        networkCommandRunner.removeManagedNetwork(vlanId, "eth0");

        // assert
        verify(this.commandRunner, never()).run(String.format("ip link set dev pibr%d down", vlanId), 10000L);
        verify(this.commandRunner).run(String.format("ip link set dev eth0.%d down", vlanId), 10000L);
        verify(this.commandRunner).run(String.format("vconfig rem eth0.%d", vlanId), 10000L);
    }

    @Test
    public void testAddGatewayIp() {
        // act
        networkCommandRunner.addGatewayIp("172.29.23.33", 28, "172.29.23.47", "pibr" + vlanId);

        // assert
        verify(this.commandRunner).run(eq(String.format("ip addr flush pibr%d", vlanId)));
        verify(this.commandRunner).run(eq(String.format("ip link set dev pibr%d up", vlanId)));
        verify(this.commandRunner).run(eq(String.format("ip addr add 172.29.23.33/28 broadcast 172.29.23.47 dev pibr%d", vlanId)));
    }

    @Test
    public void testDeleteGatewayIp() {
        // setup
        when(deviceUtils.deviceExists(String.format("pibr%d", vlanId))).thenReturn(true);

        // act
        networkCommandRunner.deleteGatewayIp("172.29.23.33", 28, "172.29.23.47", vlanId);

        // assert
        verify(this.commandRunner).run(eq(String.format("ip addr del 172.29.23.33/28 broadcast 172.29.23.47 dev pibr%d", vlanId)), eq(10000L));
    }

    @Test
    public void testDeleteGatewayIpDoesNotExecuteCommandWhenNoDevice() {
        // setup
        when(deviceUtils.deviceExists(String.format("pibr%d", vlanId))).thenReturn(false);

        // act
        networkCommandRunner.deleteGatewayIp("172.29.23.33", 28, "172.29.23.47", vlanId);

        // assert
        verify(this.commandRunner, never()).run(eq(String.format("ip addr del 172.29.23.33/28 broadcast 172.29.23.47 dev pibr%d", vlanId)));
    }

    @Test
    public void shouldReturnAllAddressesOnDevice() {
        // setup
        List<String> output = new ArrayList<String>();
        output.add("2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast qlen 1000");
        output.add("link/ether 00:18:f3:46:8d:c3 brd ff:ff:ff:ff:ff:ff");
        output.add("inet 192.168.0.16/16 brd 192.168.255.255 scope global eth0");
        output.add("inet 10.249.162.101/32 scope global eth0");
        output.add("	inet 10.249.162.205/32 scope global eth0");
        output.add("	inet 10.249.162.201/32 scope global eth0");
        output.add("    inet 10.249.162.211/28 scope global eth0");
        output.add("	inet6 fe80::218:f3ff:fe46:8dc3/64 scope link");
        output.add("		valid_lft forever preferred_lft forever");
        CommandResult commandResult = new CommandResult(0, output, new ArrayList<String>());
        when(commandRunner.run("ip addr show eth0")).thenReturn(commandResult);

        // act
        List<String> addresses = networkCommandRunner.getAllAddressesOnDevice("eth0");

        // assert
        assertEquals(true, addresses.contains("10.249.162.205"));
        assertEquals(true, addresses.contains("10.249.162.211/28"));
        assertEquals(5, addresses.size());
    }

    @Test
    public void shouldReturnDeviceIpAddress() {
        // setup
        ArrayList<String> list = new ArrayList<String>();
        list.add("eth0      Link encap:Ethernet  HWaddr 00:18:F3:46:8D:C1");
        list.add("			inet addr:172.31.250.1  Bcast:172.31.255.255  Mask:255.255.0.0");
        list.add("			inet6 addr: fe80::218:f3ff:fe46:8dc1/64 Scope:Link");
        CommandResult result = new CommandResult(0, list, new ArrayList<String>());
        when(commandRunner.run("ifconfig eth0")).thenReturn(result);

        // act
        String ipAddress = networkCommandRunner.getIpAddressForDevice("eth0");

        // assert
        assertEquals("172.31.250.1", ipAddress);
    }

    @Test
    public void testBringUpIf() {
        // setup
        String dev = "eth1";

        // act
        this.networkCommandRunner.ifUp(dev);

        // assert
        verify(this.commandRunner).run("ifconfig " + dev + " up");
    }

    @Test
    public void shouldDoNothingIfPublicGatewayNotSetWhenAskedToAddDefaultGatewayToDevice() {
        // setup
        String dev = "eth1";

        // act
        this.networkCommandRunner.addDefaultGatewayRouteToDevice(dev);

        // act
        verify(this.commandRunner, never()).run(anyString());
    }

    @Test
    public void testAddDefaultGatewayToDevice() {
        // setup
        String dev = "eth1";
        String address = "12.34.56.78";
        this.networkCommandRunner.setPublicGatewayIpAddress(address);
        when(commandRunner.run("route")).thenReturn(getRouteOutputWithNoDefaultGateWay());

        // act
        this.networkCommandRunner.addDefaultGatewayRouteToDevice(dev);

        // act
        verify(this.commandRunner).run(String.format("route add -net default gw %s dev %s", address, dev));
    }

    @Test
    public void testAddDefaultGatewayToDeviceDoesntAddAsItAlreadyExists() {
        // setup
        String dev = "eth1";
        String address = "12.34.56.78";
        this.networkCommandRunner.setPublicGatewayIpAddress(address);
        when(commandRunner.run("route")).thenReturn(getRouteOutputWithDefaulteGateWayPresent());

        // act
        this.networkCommandRunner.addDefaultGatewayRouteToDevice(dev);

        // act
        verify(this.commandRunner, never()).run(String.format("route add -net default gw %s dev %s", address, dev));
    }

    @Test
    public void shouldGetAllAddressesOnAllPiBridges() {
        // setup
        when(commandRunner.run("brctl show")).thenReturn(getBrctlShowOutput());
        when(commandRunner.run("ip -o addr show")).thenReturn(getIpAddrListPiBridgeOutput());

        // act
        Map<String, String> addressesOnPiBridges = this.networkCommandRunner.getAllAddressesOnAllPiBridges();

        // assert
        assertEquals(3, addressesOnPiBridges.size());
        assertEquals("pibr101", addressesOnPiBridges.get("172.31.0.65"));
        assertEquals("pibr102", addressesOnPiBridges.get("172.31.0.66"));
        assertEquals("pibr103", addressesOnPiBridges.get("172.31.0.67"));
    }

    @Test
    public void testRefreshXenVifOnBridge() {
        // setup
        when(deviceUtils.deviceExists(bridgeName)).thenReturn(true);
        when(deviceUtils.deviceExists(vifName)).thenReturn(true);
        when(deviceUtils.deviceEnslavedToBridge(vifName, bridgeName)).thenReturn(false);

        // act
        this.networkCommandRunner.refreshXenVifOnBridge(vlanId, domainId);

        // assert
        verify(this.commandRunner).run("brctl addif " + bridgeName + " " + vifName, NetworkCommandRunner.MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
    }

    @Test
    public void testRefreshXenVifOnBridgeWhenNotEnslavedBridgeNotExist() {
        // setup
        when(deviceUtils.deviceExists(bridgeName)).thenReturn(false);
        when(deviceUtils.deviceEnslavedToBridge(vifName, bridgeName)).thenReturn(false);

        // act
        this.networkCommandRunner.refreshXenVifOnBridge(vlanId, domainId);

        // assert
        verify(this.commandRunner, never()).run("brctl addif " + bridgeName + " " + vifName, NetworkCommandRunner.MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
    }

    @Test
    public void testRefreshXenVifOnBridgeWhenNotEnslavedVifNotExist() {
        // setup
        when(deviceUtils.deviceExists(bridgeName)).thenReturn(true);
        when(deviceUtils.deviceExists(vifName)).thenReturn(false);
        when(deviceUtils.deviceEnslavedToBridge(vifName, bridgeName)).thenReturn(false);

        // act
        this.networkCommandRunner.refreshXenVifOnBridge(vlanId, domainId);

        // assert
        verify(this.commandRunner, never()).run("brctl addif " + bridgeName + " " + vifName, NetworkCommandRunner.MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
    }

    @Test
    public void testRefreshXenVifOnBridgeWhenAlreadyEnslaved() {
        // setup
        when(deviceUtils.deviceExists(bridgeName)).thenReturn(true);
        when(deviceUtils.deviceExists(vifName)).thenReturn(true);
        when(deviceUtils.deviceEnslavedToBridge(vifName, bridgeName)).thenReturn(true);

        // act
        this.networkCommandRunner.refreshXenVifOnBridge(vlanId, domainId);

        // assert
        verify(this.commandRunner, never()).run("brctl addif " + bridgeName + " " + vifName, NetworkCommandRunner.MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
    }

    @Test
    public void testRefreshXenVifOnBridgeWhenEnslavedToAnotherBridge() {
        when(deviceUtils.deviceExists(bridgeName)).thenReturn(true);
        when(deviceUtils.deviceExists(vifName)).thenReturn(true);
        String anotherBridgeName = "pibr999";
        String andAnotherBridgeName = "pibr333";
        when(deviceUtils.getBridgesForDevice(vifName)).thenReturn(Arrays.asList(anotherBridgeName, andAnotherBridgeName));

        // act
        this.networkCommandRunner.refreshXenVifOnBridge(vlanId, domainId);

        // assert
        verify(this.commandRunner).run("brctl delif " + anotherBridgeName + " " + vifName, NetworkCommandRunner.MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
        verify(this.commandRunner).run("brctl delif " + andAnotherBridgeName + " " + vifName, NetworkCommandRunner.MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
        verify(this.commandRunner).run("brctl addif " + bridgeName + " " + vifName, NetworkCommandRunner.MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
    }

    @Test
    public void shouldSendArpingForSourceIpAddressToPublicGateway() {
        // setup
        this.networkCommandRunner.setPublicGatewayIpAddress("10.19.1.1");

        // act
        this.networkCommandRunner.sendArping("192.168.1.68", "eth1");

        // assert
        verify(this.commandRunner, times(1)).run("arping -f -U -I eth1 -s 192.168.1.68 10.19.1.1");
    }

    @Test
    public void shouldSetArpingCommand() {
        // setup
        this.networkCommandRunner.setPublicGatewayIpAddress("10.19.1.1");
        this.networkCommandRunner.setArpingCommand("arping -I %s -s %s %s");

        // act
        this.networkCommandRunner.sendArping("192.168.1.68", "eth1");

        // assert
        verify(this.commandRunner, times(1)).run("arping -I eth1 -s 192.168.1.68 10.19.1.1");
    }

    @Test
    public void shouldAddIpAddressAndSendArping() {
        // setup
        when(deviceUtils.deviceHasAddress("eth1", "192.168.1.68")).thenReturn(false);
        this.networkCommandRunner.setPublicGatewayIpAddress("10.19.1.1");

        // act
        this.networkCommandRunner.addIpAddressAndSendArping("192.168.1.68", "eth1");

        // assert
        verify(this.commandRunner).run(eq("ip addr add 192.168.1.68/32 dev eth1"), eq(10000L));
        verify(this.commandRunner, times(1)).run("arping -f -U -I eth1 -s 192.168.1.68 10.19.1.1");
    }

    @Test
    public void shouldRemoveSlashnetBeforeArping() {
        // setup
        this.networkCommandRunner.setPublicGatewayIpAddress("10.19.1.1");

        // act
        this.networkCommandRunner.sendArping("192.168.1.68/32", "eth1");
        this.networkCommandRunner.sendArping("192.168.1.69/21", "eth1");

        // assert
        verify(this.commandRunner, times(1)).run("arping -f -U -I eth1 -s 192.168.1.68 10.19.1.1");
        verify(this.commandRunner, times(1)).run("arping -f -U -I eth1 -s 192.168.1.69 10.19.1.1");
    }

    private CommandResult getRouteOutputWithDefaulteGateWayPresent() {
        ArrayList<String> routeOutput = new ArrayList<String>();
        addBaseRouteOutput(routeOutput);
        routeOutput.add("default         12.34.56.78       0.0.0.0         UG    0      0        0 eth1");
        CommandResult routeResult = new CommandResult(0, routeOutput, new ArrayList<String>());
        return routeResult;
    }

    private CommandResult getRouteOutputWithNoDefaultGateWay() {
        ArrayList<String> routeOutput = new ArrayList<String>();
        addBaseRouteOutput(routeOutput);
        CommandResult routeResult = new CommandResult(0, routeOutput, new ArrayList<String>());
        return routeResult;
    }

    private void addBaseRouteOutput(ArrayList<String> routeOutput) {
        routeOutput.add("Kernel IP routing table");
        routeOutput.add("Destination     Gateway         Genmask         Flags Metric Ref    Use Iface");
        routeOutput.add("255.255.255.255 *               255.255.255.255 UH    0      0        0 eth0");
        routeOutput.add("pidev.baynard.c pidev.local     255.255.255.255 UGH   0      0        0 eth0");
        routeOutput.add("172.32.0.128    *               255.255.255.240 U     0      0        0 pibr250");
        routeOutput.add("10.19.1.0       *               255.255.255.0   U     0      0        0 eth1");
        routeOutput.add("10.19.5.0       *               255.255.255.0   U     0      0        0 eth0");
        routeOutput.add("192.168.122.0   *               255.255.255.0   U     0      0        0 virbr0");
        routeOutput.add("169.254.0.0     *               255.255.0.0     U     0      0        0 eth0");
        routeOutput.add("224.0.0.0       *               255.0.0.0       U     0      0        0 eth0");
    }

    private CommandResult getBrctlShowOutput() {
        ArrayList<String> brctlOutput = new ArrayList<String>();
        brctlOutput.add("bridge name     bridge id               STP enabled     interfaces");
        brctlOutput.add("pibr101\t8000.001e68c60d42       no              vif1.0");
        brctlOutput.add("                                                          eth0.201");
        brctlOutput.add("pibr102    8000.000000000000       no");
        brctlOutput.add("                                                          eth0.201");
        brctlOutput.add("pibr103         8000.001e68c60d42       no              vif1.0");
        brctlOutput.add("                                                          eth0.201");
        brctlOutput.add("virbr0          8000.000000000000       yes");
        brctlOutput.add("xenbr.eth0              8000.feffffffffff       no              peth0");
        brctlOutput.add("                                                                vif0.0");

        CommandResult brctlResult = new CommandResult(0, brctlOutput, new ArrayList<String>());
        return brctlResult;
    }

    private CommandResult getIpAddrListPiBridgeOutput() {
        ArrayList<String> addrList = new ArrayList<String>();
        addrList.add("260: pibr103: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue \\    link/ether 00:1e:68:c5:e7:6e brd ff:ff:ff:ff:ff:ff");
        addrList.add("260: pibr103    inet 172.31.0.67/28 brd 10.19.128.63 scope global pibr103");
        addrList.add("262: pibr100: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue \\    link/ether 00:1e:68:c5:e7:6e brd ff:ff:ff:ff:ff:ff");
        addrList.add("262: pibr100    inet 10.19.128.1/28 brd 10.19.128.15 scope global pibr100");
        addrList.add("264: pibr102: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue \\    link/ether 00:1e:68:c5:e7:6e brd ff:ff:ff:ff:ff:ff");
        addrList.add("264: pibr102    inet 172.31.0.66 brd 10.19.128.47 scope global pibr102");
        addrList.add("266: pibr359: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue \\    link/ether 00:1e:68:c5:e7:6e brd ff:ff:ff:ff:ff:ff");
        addrList.add("266: pibr359    inet 10.19.143.177/28 brd 10.19.143.191 scope global pibr359");
        addrList.add("268: pibr101: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue \\    link/ether 00:1e:68:c5:e7:6e brd ff:ff:ff:ff:ff:ff");
        addrList.add("268: pibr101    inet 172.31.0.65/28 brd 10.19.128.31 scope global pibr101");

        return new CommandResult(0, addrList, new ArrayList<String>());
    }
}