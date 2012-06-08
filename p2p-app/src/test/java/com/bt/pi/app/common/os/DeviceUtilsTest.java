package com.bt.pi.app.common.os;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

public class DeviceUtilsTest {
    private static final String ETH_20 = "eth0.20";
    private static final String PIBR_20 = "pibr20";
    private CommandRunner commandRunner;
    private DeviceUtils deviceUtils;
    private CommandResult deviceExistsCommandResult;
    private CommandResult deviceHasAddressCommandResult;
    private CommandResult deviceEnslavedCommandResult;

    @Before
    public void setUp() throws Exception {
        List<String> listDevicesOutputLines = new ArrayList<String>();
        listDevicesOutputLines.add(ETH_20);
        listDevicesOutputLines.add(PIBR_20);
        listDevicesOutputLines.add("pibr28:  159005    3668    0    0    0     0          0      3640    68163    1814    0    0    0     0       0          0");
        listDevicesOutputLines.add("eth0.28:  159047    3669    0    0    0     0          0         0   123439    3536    0    0    0     0       0          0");
        listDevicesOutputLines.add("eth0.29:12360505  223345    0    0    0     0          0        18 215841954  199100    0    0    0     0       0          0");
        listDevicesOutputLines.add("pibr44:21097267321 16726393    0    0    0     0          0    337535 2073685172 9732810    0    0    0     0       0          0");

        List<String> listDevAddrsOutputLines = new ArrayList<String>();
        listDevAddrsOutputLines.add("some blurb");
        listDevAddrsOutputLines.add("	link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00");
        listDevAddrsOutputLines.add("	inet 127.0.0.1/8 scope host lo");
        listDevAddrsOutputLines.add("inet6 ::1/128 scope host ");
        listDevAddrsOutputLines.add("");
        listDevAddrsOutputLines.add("valid_lft forever preferred_lft forever");
        listDevAddrsOutputLines.add("\n");
        listDevAddrsOutputLines.add("inet blah 11.22.33.44/32 blah");
        listDevAddrsOutputLines.add("inet 5.6.7.8 ");

        List<String> listBridgesOutputLines = new ArrayList<String>();
        listBridgesOutputLines.add("   pibr17                8000.0018f3468dc3       no              eth0.17  ");
        listBridgesOutputLines.add("pibr18                8000.0018f3468dc3       no              eth0.18");
        listBridgesOutputLines.add("pibr19                8000.0018f3468dc3       no              eth0.19");
        listBridgesOutputLines.add("pibr20                8000.000000000000       no");
        listBridgesOutputLines.add("pibr21                8000.0018f3468dc3       no              eth0.21");
        listBridgesOutputLines.add("pibr22                8000.0018f3468dc3       no              eth0.22");
        listBridgesOutputLines.add("pibr380               8000.0018f3468dc1       no              vif4.0");
        listBridgesOutputLines.add("                                                              eth0.380");
        listBridgesOutputLines.add("pibr381               8000.0018f3468dc1       no              vif4.0");
        listBridgesOutputLines.add("pibr382               8000.0018f3468dc1       no              vif4.0");
        listBridgesOutputLines.add("                                                              eth0.381");
        listBridgesOutputLines.add("garbage");
        listBridgesOutputLines.add("");

        deviceExistsCommandResult = mock(CommandResult.class);
        when(deviceExistsCommandResult.getOutputLines()).thenReturn(listDevicesOutputLines);

        deviceHasAddressCommandResult = mock(CommandResult.class);
        when(deviceHasAddressCommandResult.getOutputLines()).thenReturn(listDevAddrsOutputLines);

        deviceEnslavedCommandResult = mock(CommandResult.class);
        when(deviceEnslavedCommandResult.getOutputLines()).thenReturn(listBridgesOutputLines);

        commandRunner = mock(CommandRunner.class);
        when(commandRunner.run("cat /proc/net/dev")).thenReturn(deviceExistsCommandResult);
        when(commandRunner.run("ip addr show dev eth0")).thenReturn(deviceHasAddressCommandResult);
        when(commandRunner.run("brctl show")).thenReturn(deviceEnslavedCommandResult);

        deviceUtils = new DeviceUtils();
        deviceUtils.setCommandRunner(commandRunner);
    }

    @Test
    public void shouldFindExistingDevInProcNetDev() {
        // act
        boolean res = this.deviceUtils.deviceExists(ETH_20);

        // assert
        assertEquals(true, res);
    }

    @Test
    public void shouldNotFindAbsentDevInProcNetDev() {
        // act
        boolean res = this.deviceUtils.deviceExists("peth.21");

        // assert
        assertEquals(false, res);
    }

    @Test
    public void shouldFindAddressInDeviceIfPresent() {
        // act
        boolean res = this.deviceUtils.deviceHasAddress("eth0", "11.22.33.44");

        // assert
        assertEquals(true, res);
    }

    @Test
    public void shouldFindAddressWithSlashnetInDeviceIfPresent() {
        // act
        boolean res = this.deviceUtils.deviceHasAddress("eth0", "11.22.33.44/32");

        // assert
        assertEquals(true, res);
    }

    @Test
    public void shouldNotFindAddressInDeviceIfNotPresentNoSlashnet() {
        // act
        boolean res = this.deviceUtils.deviceHasAddress("eth0", "11.22.33.4");

        // assert
        assertEquals(false, res);
    }

    @Test
    public void shouldNotFindAddressInDeviceIfNotPresent() {
        // act
        boolean res = this.deviceUtils.deviceHasAddress("eth0", "3.2.1.0/32");

        // assert
        assertEquals(false, res);
    }

    @Test
    public void shouldReturnAllDevicesOnInterfaceEth0() {
        // act
        List<String> devices = deviceUtils.getAllVlanDevicesForInterface("eth0");

        // assert
        assertEquals(3, devices.size());
        assertEquals(true, devices.contains(ETH_20));
        assertEquals(true, devices.contains("eth0.28"));
        assertEquals(true, devices.contains("eth0.29"));
    }

    @Test
    public void shouldBeAbleToDetectDeviceAlreadyEnslavedToBridge() {
        // act
        boolean res = this.deviceUtils.deviceEnslavedToBridge("eth0.19", "pibr19");

        // assert
        assertTrue(res);
    }

    @Test
    public void shouldBeAbleToTellWhenDeviceNotEnslavedToGivenBridge() {
        // act
        boolean res = this.deviceUtils.deviceEnslavedToBridge("eth0.18", "pibr19");

        // assert
        assertFalse(res);
    }

    @Test
    public void shouldBeAbleToTellWhenDeviceNotEnslavedToAnyBridge() {
        // act
        boolean res = this.deviceUtils.deviceEnslavedToBridge("eth0.20", "pibr20");

        // assert
        assertFalse(res);
    }

    @Test
    public void shouldBeAbleToTellWhenDeviceNotEnslavedToImaginaryBridge() {
        // act
        boolean res = this.deviceUtils.deviceEnslavedToBridge("eth0.19", "imaginary");

        // assert
        assertFalse(res);
    }

    @Test
    public void shouldBeAbleToTellWhenDeviceEnslavedToBridgeWithMultipleInterfaces() {
        // act
        boolean res = this.deviceUtils.deviceEnslavedToBridge("eth0.380", "pibr380");

        // assert
        assertTrue(res);
    }

    @Test
    public void shouldBeAbleToTellWhenDeviceNotEnslavedToBridgeWithMultipleInterfaces() {
        // act
        boolean res = this.deviceUtils.deviceEnslavedToBridge("eth0.381", "pibr381");

        // assert
        assertFalse(res);
    }

    @Test
    public void shouldReturnBridgeForVifDevice() {
        // setup
        String device = "vif4.0";

        // act
        List<String> result = this.deviceUtils.getBridgesForDevice(device);

        // assert
        assertEquals(3, result.size());
        assertTrue(result.contains("pibr380"));
        assertTrue(result.contains("pibr381"));
        assertTrue(result.contains("pibr382"));
    }

    @Test
    public void shouldReturnBridgeForEthDevice() {
        // setup
        String device = "eth0.380";
        String bridge = "pibr380";

        // act
        List<String> result = this.deviceUtils.getBridgesForDevice(device);

        // assert
        assertEquals(1, result.size());
        assertEquals(bridge, result.get(0));
    }

    @Test
    public void shouldReturnNullForMissingDevice() {
        // setup
        String device = "fred";

        // act
        List<String> result = this.deviceUtils.getBridgesForDevice(device);

        // assert
        assertEquals(0, result.size());
    }

    @Test
    public void testGetFirstBridgeForDeviceTabbedOutput() {
        // setup
        List<String> lines = Arrays.asList("bridge name\tbridge id\tSTP enabled\tinterfaces", "pibr141\t8000.001e68c5e652\tno\tvif16.0", "\t\teth0.141", "pibr385\t8000.001e68c5e652\tno\tvif957.0", "\t\t\t\teth0.385",
                "pibr386\t8000.001e68c5e652\tno\teth0.386", "virbr0\t8000.000000000000\tyes", "xenbr.eth0\t8000.feffffffffff\tno\tpeth0", "\t\t\t\tvif0.0", "xenbr.eth1\t8000.feffffffffff\tno\tpeth1", "\t\t\t\t\tvif0.1");
        CommandResult commandResult = mock(CommandResult.class);
        when(commandResult.getOutputLines()).thenReturn(lines);
        when(commandRunner.run("brctl show")).thenReturn(commandResult);

        // act
        List<String> result = this.deviceUtils.getBridgesForDevice("vif957.0");

        // assert
        assertEquals(1, result.size());
        assertEquals("pibr385", result.get(0));
    }
}
