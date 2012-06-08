package com.bt.pi.app.common.conf;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class XenConfigurationParserTest {

    XenConfigurationParser xenConfigurationParser;

    @Before
    public void setUp() throws Exception {
        xenConfigurationParser = new XenConfigurationParser();
        xenConfigurationParser.init("src/test/resources/xend-config.sxp");
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOExceptionIfUnableToFindConfigurationFile() throws IOException {
        // act
        xenConfigurationParser = new XenConfigurationParser();
        xenConfigurationParser.init("does-not-exist.sxp");
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionIfUnableToFindKeyInConfigurationFile() {
        // setup

        // act
        xenConfigurationParser.getIntValue("foo");
    }

    @Test
    public void shouldGetValidValueFor_xendrelocationhostsallow_FromTheXenConfigurationFile() {
        // setup

        // act
        String value = xenConfigurationParser.getValue("xend-relocation-hosts-allow");

        // assert
        assertEquals("^localhost$ ^localhost\\\\.localdomain$", value);
    }

    @Test
    public void shouldGetValid_Dom0MinMem_AsLongFromTheXenConfigurationFile() {
        // setup

        // act
        long dom0minmem = xenConfigurationParser.getLongValue("dom0-min-mem");

        // assert
        assertEquals(768, dom0minmem);
    }

    @Test
    public void shouldGetValid_Dom0MinMem_AsIntFromTheXenConfigurationFile() {
        // setup

        // act
        int dom0minmem = xenConfigurationParser.getIntValue("dom0-min-mem");

        // assert
        assertEquals(768, dom0minmem);
    }

    @Test
    public void testEntireMap() throws Exception {
        // setup

        // act
        Map<String, String> result = xenConfigurationParser.getMap();

        // assert
        assertEquals("yes", result.get("xend-unix-server"));
        assertEquals("/var/lib/xend/xend-socket", result.get("xend-unix-path"));
        assertEquals("^localhost$ ^localhost\\\\.localdomain$", result.get("xend-relocation-hosts-allow"));
        assertEquals("rocks-network-bridge", result.get("network-script"));
        assertEquals("vif-bridge", result.get("vif-script"));
        assertEquals("1", result.get("dom0-cpus"));
        assertEquals("", result.get("vncpasswd"));
        assertEquals("768", result.get("dom0-min-mem"));
    }
}
