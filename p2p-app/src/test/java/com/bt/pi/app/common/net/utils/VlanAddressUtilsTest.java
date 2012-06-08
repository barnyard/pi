package com.bt.pi.app.common.net.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

public class VlanAddressUtilsTest {
    private static final String PETH_20 = "peth.20";
    private CommandRunner commandRunner;
    private CommandResult commandResult;

    @Before
    public void setUp() throws Exception {
        List<String> outputLines = new ArrayList<String>();
        outputLines.add(PETH_20);

        commandResult = mock(CommandResult.class);
        when(commandResult.getOutputLines()).thenReturn(outputLines);

        commandRunner = mock(CommandRunner.class);
        when(commandRunner.run("cat /proc/net/dev")).thenReturn(commandResult);
    }

    @Test
    public void shouldMapVlanToBridgeName() {
        // setup
        int vlanId = 567;

        // act
        String res = VlanAddressUtils.getBridgeNameForVlan(vlanId);

        // assert
        assertEquals("pibr567", res);
    }

    @Test
    public void shouldMapInstanceToMacAddressWithOldInstanceId() {
        // act
        String res = VlanAddressUtils.getMacAddressFromInstanceId("i-4A1D08F7");

        // assert
        assertTrue("mac address didn't begin with : 00:d5:44:3f:c8:8f", res.startsWith("00:d5:44:3f:c8:8f"));
    }

    @Test
    public void shouldMapInstanceToMacAddress() {
        // act
        String res = VlanAddressUtils.getMacAddressFromInstanceId("i-07Y0iuIY");

        // assert
        assertTrue("mac address didn't begin with : 00:63:d4:23:a5:7e:", res.startsWith("00:63:d4:23:a5:7e"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenAskedToMapShortInstanceIdToMacAddress() {
        // act
        VlanAddressUtils.getMacAddressFromInstanceId("i-4321");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenAskedToMapInstanceIdWithWrongPrefixToMacAddress() {
        // act
        VlanAddressUtils.getMacAddressFromInstanceId("x-43210987");
    }
}
