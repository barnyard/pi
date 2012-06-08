/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.os;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.util.common.CommandRunner;

@Component
public class DeviceUtils {
    private static final String WHITESPACE = "\\s";
    private static final String BRCTL_SHOW = "brctl show";
    private static final String SLASH = "/";
    private static final int ZERO = 0;
    private static final Log LOG = LogFactory.getLog(DeviceUtils.class);
    private static final String DEVICE_LIST = "cat /proc/net/dev";
    private static final String DEVICE_ADDRS_LIST_S = "ip addr show dev %s";
    private static final String PIBR = "pibr";
    private static final String SPACE = " ";
    private static final String COLON = ":";
    private CommandRunner commandRunner;

    public DeviceUtils() {
        commandRunner = null;
    }

    @Resource
    public void setCommandRunner(CommandRunner aCommandRunner) {
        this.commandRunner = aCommandRunner;
    }

    public boolean deviceExists(String devName) {
        LOG.debug(String.format("deviceExists(%s)", devName));
        List<String> vconfig = getDeviceList();
        return deviceExists(devName, vconfig);
    }

    public boolean deviceExists(String devName, List<String> vconfig) {
        for (String line : vconfig)
            if (line.trim().startsWith(devName))
                return true;
        return false;
    }

    public List<String> getDeviceList() {
        return commandRunner.run(DEVICE_LIST).getOutputLines();
    }

    public boolean deviceHasAddress(String devName, String address) {
        LOG.debug(String.format("deviceHasAddress(%s, %s)", devName, address));
        List<String> lines = commandRunner.run(String.format(DEVICE_ADDRS_LIST_S, devName)).getOutputLines();
        for (String line : lines) {
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) {
                String nextToken = st.nextToken();
                if (address.equals(nextToken) || (nextToken.indexOf(SLASH) > -1 && address.equals(nextToken.split(SLASH)[0]))) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean deviceEnslavedToBridge(String device, String bridge) {
        LOG.debug(String.format("deviceEnslavedToBridge(%s, %s)", device, bridge));
        List<String> lines = commandRunner.run(BRCTL_SHOW).getOutputLines();
        boolean gotBridge = false;
        for (String line : lines) {
            boolean gotDevice = false;
            if (line.contains(PIBR))
                gotBridge = false;
            if (line.contains(bridge))
                gotBridge = true;
            if (line.contains(device))
                gotDevice = true;
            if (gotBridge && gotDevice)
                return true;
        }
        return false;
    }

    public List<String> getBridgesForDevice(String device) {
        LOG.debug(String.format("getFirstBridgeForDevice(%s)", device));
        List<String> result = new ArrayList<String>();

        List<String> lines = commandRunner.run(BRCTL_SHOW).getOutputLines();

        String currentBridge = null;
        for (String line : lines) {
            if (line.contains(PIBR))
                currentBridge = line.trim().split(WHITESPACE)[0];
            if (line.contains(device) && currentBridge != null)
                result.add(currentBridge);
        }

        return result;
    }

    public List<String> getAllVlanDevicesForInterface(String iface) {
        LOG.debug(String.format("getAllVlanDevicesForInterface(%s)", iface));
        List<String> vconfig = getDeviceList();
        return getAllVlanDevicesForInterface(iface, vconfig);
    }

    public List<String> getAllVlanDevicesForInterface(String iface, List<String> vconfig) {
        List<String> devices = new ArrayList<String>();
        for (String line : vconfig) {
            if (line.trim().startsWith(String.format("%s.", iface)))
                devices.add(line.split(SPACE)[ZERO].split(COLON)[ZERO]);
        }
        return devices;
    }
}
