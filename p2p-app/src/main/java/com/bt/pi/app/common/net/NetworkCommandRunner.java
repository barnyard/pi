/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;
import com.bt.pi.app.common.os.DeviceUtils;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@Component
public class NetworkCommandRunner {
    static final int MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME = 10000;
    private static final String DEFAULT_ARPING_COMMAND = "arping -f -U -I %s -s %s %s";
    private static final String SLASH = "/";
    private static final String IP_ADDR_FLUSH_DEVICE = "ip addr flush %s";
    private static final String BRIDGE_ALREADY_EXISTS = "Tried to add bridge %s, but bridge already exists";
    private static final String INTERFACE_VLAN_ALREADY_EXISTS = "Tried to add managed vlan %d, but vlan already exists";
    private static final String IP_LINK_SET_DEV_UP = "ip link set dev %s up";
    private static final String BRCTL_ADDIF_BRIDGE_DEVICE = "brctl addif %s %s";
    private static final String BRCTL_DELIF_BRIDGE_DEVICE = "brctl delif %s %s";
    private static final String BRCTL_ADDBR = "brctl addbr %s";
    private static final String BRCTL_DELBR = "brctl delbr %s";
    private static final String VCONFIG_ADD_INTERFACE_VLAN = "vconfig add %s %d";
    private static final String BRINGING_DOWN_NETWORK_D_BUT_DEVICE_S_NOT_FOUND_WILL_NOT_ATTEMPT_TO_REMOVE_IT = "Bringing down network %d, but device %s not found - will not attempt to remove it";
    private static final String IP_ADDR_SHOW_DEVICE = "ip addr show %s";
    private static final String SCOPE_GLOBAL = "scope global";
    private static final String IP_LINK_SET_DEV_S_UP = IP_LINK_SET_DEV_UP;
    private static final String IP_LINK_SET_DEV_S_DOWN = "ip link set dev %s down";
    private static final String SPACE = " ";
    private static final String COLON = ":";
    private static final String PUBLIC_GATEWAY_IP = "public.gateway.ip";
    private static final String KEY_ARPING_COMMAND = "arping.command";
    private static final String MTU_SIZE = "mtu.size";
    private static final Log LOG = LogFactory.getLog(NetworkCommandRunner.class);

    private String publicGatewayIpAddress;
    private DeviceUtils deviceUtils;
    private CommandRunner commandRunner;
    private int mtuSize;
    private Object[] ipAddrLock;
    private Object[] vlanLock;
    private Object[] bridgeLock;
    private String arpingCommand;

    public NetworkCommandRunner() {
        super();
        commandRunner = null;
        deviceUtils = null;
        publicGatewayIpAddress = null;
        ipAddrLock = new Object[0];
        vlanLock = new Object[0];
        bridgeLock = new Object[0];
        arpingCommand = DEFAULT_ARPING_COMMAND;
    }

    @Property(key = PUBLIC_GATEWAY_IP)
    public void setPublicGatewayIpAddress(String address) {
        this.publicGatewayIpAddress = address;
    }

    @Property(key = MTU_SIZE, defaultValue = "1500")
    public void setMtuSize(int anMtuSize) {
        this.mtuSize = anMtuSize;
    }

    @Property(key = KEY_ARPING_COMMAND, defaultValue = DEFAULT_ARPING_COMMAND)
    public void setArpingCommand(String anArpingCommand) {
        arpingCommand = anArpingCommand;
    }

    @Resource
    public void setDeviceUtils(DeviceUtils aDeviceUtils) {
        this.deviceUtils = aDeviceUtils;
    }

    @Resource
    public void setCommandRunner(CommandRunner aCommandRunner) {
        this.commandRunner = aCommandRunner;
    }

    public void ipAddressAdd(String ipAddress, String iface) {
        LOG.debug(String.format("ipAddressAdd(%s, %s", ipAddress, iface));
        synchronized (ipAddrLock) {
            if (!deviceUtils.deviceHasAddress(iface, ipAddress)) {
                String address = IpAddressUtils.addSlash32ToIpAddress(ipAddress);
                String commandLine = String.format("ip addr add %s dev %s", address, iface);
                commandRunner.run(commandLine, MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
            } else {
                LOG.info(String.format("Device %s already has address %s, no need to add", iface, ipAddress));
            }
        }
    }

    public void ipAddressDelete(String ipAddress, String iface) {
        LOG.debug(String.format("ipAddressDelete(%s, %s)", ipAddress, iface));
        List<String> oneAddr = Arrays.asList(new String[] { ipAddress });
        ipAddressesDelete(oneAddr, iface);
    }

    public void ipAddressesDelete(Collection<String> ipAddresses, String iface) {
        LOG.debug(String.format("deleteIpAddressesFromInterface(%s, %s)", ipAddresses, iface));
        if (!deviceUtils.deviceExists(iface)) {
            LOG.warn(String.format("Will not attempt to remove addresess %s from device %s - device does not exist!", ipAddresses, iface));
            return;
        }

        for (String ipAddress : ipAddresses) {
            synchronized (ipAddrLock) {
                if (deviceUtils.deviceHasAddress(iface, ipAddress)) {
                    String address = IpAddressUtils.addSlash32ToIpAddress(ipAddress);
                    String commandLine = String.format("ip addr del %s dev %s", address, iface);
                    commandRunner.run(commandLine, MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
                } else {
                    LOG.debug(String.format("Will not attempt to remove address %s from device %s - device does not have that address", ipAddress, iface));
                }
            }
        }
    }

    public void addManagedNetwork(long vlanId, String publicInterface) {
        LOG.debug(String.format("addManagedNetwork(%s, %d", publicInterface, vlanId));

        String vlanInterface = getVlanInterface(vlanId, publicInterface);
        synchronized (vlanLock) {
            if (!deviceUtils.deviceExists(vlanInterface)) {
                String vlanConfigCommand = String.format(VCONFIG_ADD_INTERFACE_VLAN, publicInterface, vlanId);
                commandRunner.run(vlanConfigCommand, MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
            } else {
                LOG.info(String.format(INTERFACE_VLAN_ALREADY_EXISTS, vlanId));
            }
        }

        String newBridgeName = VlanAddressUtils.getBridgeNameForVlan(vlanId);
        synchronized (bridgeLock) {
            if (!deviceUtils.deviceExists(newBridgeName)) {
                String newBridgeCommand = String.format(BRCTL_ADDBR, newBridgeName);
                commandRunner.run(newBridgeCommand, MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
            } else {
                LOG.info(String.format(BRIDGE_ALREADY_EXISTS, newBridgeName));
            }

            if (!deviceUtils.deviceEnslavedToBridge(vlanInterface, newBridgeName)) {
                String brctlIfCommand = String.format(BRCTL_ADDIF_BRIDGE_DEVICE, newBridgeName, vlanInterface);
                commandRunner.run(brctlIfCommand, MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
            }
        }

        String bringBridgeUpCommand = String.format(IP_LINK_SET_DEV_UP, newBridgeName);
        commandRunner.run(bringBridgeUpCommand);

        String bringInterfaceUpCommand = String.format(IP_LINK_SET_DEV_UP + " mtu %s", vlanInterface, this.mtuSize);
        commandRunner.run(bringInterfaceUpCommand);
    }

    public void refreshXenVifOnBridge(long vlanId, long domainId) {
        LOG.debug(String.format("refreshXenVifOnBridge(%d, %d)", vlanId, domainId));
        String bridgeName = VlanAddressUtils.getBridgeNameForVlan(vlanId);
        String device = String.format("vif%d.0", domainId);
        synchronized (bridgeLock) {
            if (!deviceUtils.deviceExists(bridgeName))
                return;
            if (!deviceUtils.deviceExists(device))
                return;
            if (deviceUtils.deviceEnslavedToBridge(device, bridgeName))
                return;

            List<String> currentBridges = deviceUtils.getBridgesForDevice(device);
            for (String currentBridge : currentBridges) {
                commandRunner.run(String.format(BRCTL_DELIF_BRIDGE_DEVICE, currentBridge, device), MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
            }
            String brctlIfCommand = String.format(BRCTL_ADDIF_BRIDGE_DEVICE, bridgeName, device);
            commandRunner.run(brctlIfCommand, MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
        }
    }

    public String getVlanInterface(long vlanId, String publicInterface) {
        return String.format("%s.%d", publicInterface, vlanId);
    }

    public void removeManagedNetwork(long vlanId, String publicInterface) {
        LOG.debug(String.format("removeManagedNetwork(%d, %s)", vlanId, publicInterface));

        String bridgeName = VlanAddressUtils.getBridgeNameForVlan(vlanId);
        synchronized (bridgeLock) {
            if (deviceUtils.deviceExists(bridgeName)) {
                String ipLinkBridgeCommand = String.format(IP_LINK_SET_DEV_S_DOWN, bridgeName);
                commandRunner.run(ipLinkBridgeCommand, MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);

                String deleteBridgeCommand = String.format(BRCTL_DELBR, bridgeName);
                commandRunner.run(deleteBridgeCommand, MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
            } else {
                LOG.warn(String.format(BRINGING_DOWN_NETWORK_D_BUT_DEVICE_S_NOT_FOUND_WILL_NOT_ATTEMPT_TO_REMOVE_IT, vlanId, bridgeName));
            }
        }

        synchronized (vlanLock) {
            String vlanInterface = getVlanInterface(vlanId, publicInterface);
            if (deviceUtils.deviceExists(vlanInterface)) {
                String ipLinkIfCommand = String.format(IP_LINK_SET_DEV_S_DOWN, vlanInterface);
                commandRunner.run(ipLinkIfCommand, MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);

                String vconfigCommand = String.format("vconfig rem %s", vlanInterface);
                commandRunner.run(vconfigCommand, MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
            } else {
                LOG.warn(String.format(BRINGING_DOWN_NETWORK_D_BUT_DEVICE_S_NOT_FOUND_WILL_NOT_ATTEMPT_TO_REMOVE_IT, vlanId, vlanInterface));
            }
        }
    }

    public void addGatewayIp(String routerAddress, int slashNet, String broadcastAddress, String devName) {
        LOG.debug(String.format("addGatewayIp(%s, %d, %s, %s)", routerAddress, slashNet, broadcastAddress, devName));

        synchronized (bridgeLock) {
            commandRunner.run(String.format(IP_ADDR_FLUSH_DEVICE, devName));
            commandRunner.run(String.format("ip addr add %s/%d broadcast %s dev %s", routerAddress, slashNet, broadcastAddress, devName));
            commandRunner.run(String.format(IP_LINK_SET_DEV_S_UP, devName));
        }
    }

    public void deleteGatewayIp(String routerAddress, int slashNet, String broadcastAddress, long vlanId) {
        LOG.debug(String.format("deleteGatewayIp(%s, %d, %s, %d)", routerAddress, slashNet, broadcastAddress, vlanId));
        String bridgeName = VlanAddressUtils.getBridgeNameForVlan(vlanId);
        synchronized (bridgeLock) {
            if (deviceUtils.deviceExists(bridgeName)) {
                commandRunner.run(String.format("ip addr del %s/%d broadcast %s dev %s", routerAddress, slashNet, broadcastAddress, bridgeName), MAX_ALLOWED_NETWORK_COMMAND_EXECUTION_TIME);
            } else {
                LOG.info(String.format("Not deleting gateway ip for %d, device %s not found", vlanId, bridgeName));
            }
        }
    }

    public List<String> getAllAddressesOnDevice(String device) {
        LOG.debug(String.format("getAllAddressesOnDevice(%s)", device));
        CommandResult result = commandRunner.run(String.format(IP_ADDR_SHOW_DEVICE, device));
        List<String> addresses = new ArrayList<String>();
        for (String line : result.getOutputLines()) {
            if (line.contains(SCOPE_GLOBAL)) {
                addresses.add(IpAddressUtils.removeSlash32FromIpAddress(line.trim().split(SPACE)[1]));
            }
        }
        return addresses;
    }

    public String getIpAddressForDevice(String device) {
        LOG.debug(String.format("getIpAddressForDevice(%s)", device));
        CommandResult result = commandRunner.run(String.format("ifconfig %s", device));
        String address = "";
        for (String line : result.getOutputLines()) {
            if (line.contains("inet addr")) {
                address = line.trim().split(SPACE)[1].split(COLON)[1];
            }
        }
        return address;
    }

    public void ifUp(String device) {
        LOG.debug(String.format("ifUp(%s)", device));
        try {
            commandRunner.run(String.format("ifconfig %s up", device));
        } catch (CommandExecutionException e) {
            LOG.error(String.format("error starting interface for device %s", device), e);
        }
    }

    public void addDefaultGatewayRouteToDevice(String device) {
        LOG.debug(String.format("addDefaultGatewayRouteToDevice(%s)", device));
        if (this.publicGatewayIpAddress == null) {
            LOG.error(String.format("%s not set, so cannot add default gateway to device %s", PUBLIC_GATEWAY_IP, device), null);
            return;
        }
        try {
            if (isDefaultGateWayPresent(device)) {
                LOG.debug(String.format("Gateway %s is already has a route on device %s", publicGatewayIpAddress, device));
            } else
                this.commandRunner.run(String.format("route add -net default gw %s dev %s", this.publicGatewayIpAddress, device));
        } catch (CommandExecutionException e) {
            LOG.info(String.format("error adding default gateway %s for device %s", this.publicGatewayIpAddress, device));
        }
    }

    public boolean isDefaultGateWayPresent(String device) {
        boolean gatewayPresent = false;
        CommandResult result = this.commandRunner.run("route");

        for (String line : result.getOutputLines()) {
            if (line.startsWith("default") && line.contains(publicGatewayIpAddress) && line.contains(device)) {
                gatewayPresent = true;
                break;
            }
        }
        return gatewayPresent;
    }

    public Map<String, String> getAllAddressesOnAllPiBridges() {
        Map<String, String> piBridgeAddresses = new HashMap<String, String>();
        try {
            Collection<String> piBridges = new ArrayList<String>();
            CommandResult brctlResult = commandRunner.run("brctl show");
            for (String line : brctlResult.getOutputLines()) {
                if (line.startsWith("pibr")) {
                    // we are using the tokenizer just so that we get \t space or whatever else.
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    String piBridge = tokenizer.nextToken().trim();
                    LOG.debug(String.format("Found pi bridge:%s from line: %s", piBridge, line));
                    piBridges.add(piBridge);
                }
            }

            CommandResult ipAddrResult = commandRunner.run("ip -o addr show");
            for (String line : ipAddrResult.getOutputLines()) {
                if (line.contains(SCOPE_GLOBAL)) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    // skip the 1st token
                    tokenizer.nextToken();
                    String piBridge = tokenizer.nextToken().trim();
                    if (piBridges.contains(piBridge)) {
                        // skip a token
                        tokenizer.nextToken();

                        String address = tokenizer.nextToken().trim().split(SLASH)[0];
                        LOG.debug(String.format("Found address %s for bridge %s", address, piBridge));
                        piBridgeAddresses.put(address, piBridge);
                    }
                }
            }
        } catch (CommandExecutionException e) {
            LOG.info("error getting all pi bridges" + e);
        }
        return piBridgeAddresses;
    }

    public void sendArping(String sourceIpAddress, String networkInterface) {
        LOG.debug(String.format("sendArping(sourceIpAddress=%s, networkInterface=%s)", sourceIpAddress, networkInterface));

        try {
            String ipAddress = IpAddressUtils.removeSlashnetFromIpAddress(sourceIpAddress);
            commandRunner.run(String.format(arpingCommand, networkInterface, ipAddress, this.publicGatewayIpAddress));
        } catch (CommandExecutionException e) {
            LOG.warn(String.format("arping command failed: %s - %s", arpingCommand, e));
        }
    }

    public void addIpAddressAndSendArping(String sourceIpAddress, String networkInterface) {
        this.ipAddressAdd(sourceIpAddress, networkInterface);
        this.sendArping(sourceIpAddress, networkInterface);
    }
}
