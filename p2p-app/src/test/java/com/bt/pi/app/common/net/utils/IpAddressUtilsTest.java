package com.bt.pi.app.common.net.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IpAddressUtilsTest {
    @Test
    public void testRoundTrip() {
        // setup
        String[] ips = new String[] { "11.23.41.23/16", "10.19.1.200/24", "10.19.1.245/8", "123.123.123.123", "0.0.0.0", "12.34.56.78", "255.255.255.255" };
        // act
        for (String ip : ips) {
            long l = IpAddressUtils.ipToLong(ip);
            String result = IpAddressUtils.longToIp(l);
            // assert
            assertEquals(ip, result);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIpToLongTooBig() {
        // setup
        String ip = "255.256.255.255";

        // act
        IpAddressUtils.ipToLong(ip);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIpToLongBadFormat() {
        // setup
        String ip = "255.255.aa.255";

        // act
        IpAddressUtils.ipToLong(ip);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIpToLongBadSeparator() {
        // setup
        String ip = "255.255,255.255";

        // act
        IpAddressUtils.ipToLong(ip);
    }

    @Test
    public void testIncrement() {
        // setup
        String ip = "123.123.123.123";

        // act
        String result = IpAddressUtils.increment(ip, 2);

        // assert
        assertEquals("123.123.123.125", result);
    }

    @Test
    public void testNetmaskIpToSlashnet() {
        // setup
        String netmask = "255.0.0.0";

        // act
        int slashnet = IpAddressUtils.netmaskToSlashnet(netmask);

        // assert
        assertEquals(8, slashnet);
    }

    @Test
    public void testNetmaskLongToSlashnet() {
        // setup
        long netmask = IpAddressUtils.ipToLong("255.0.0.0");

        // act
        int slashnet = IpAddressUtils.netmaskToSlashnet(netmask);

        // assert
        assertEquals(8, slashnet);
    }

    @Test
    public void testSlashnetFromAddrs() {
        // setup
        int addrs = 256;

        // act
        int slashnet = IpAddressUtils.slashnetFromAddrs(addrs);

        // assert
        assertEquals(24, slashnet);
    }

    @Test
    public void testNetmaskLongToNetSize() {
        // setup
        long netmask = IpAddressUtils.ipToLong("255.255.255.240");

        // act
        long netSize = IpAddressUtils.netmaskToNetSize(netmask);

        // assert
        assertEquals(16, netSize);
    }

    @Test
    public void testNetSizeToNetmaskLong() {
        // act
        long netmask = IpAddressUtils.netSizeToNetmask(256);

        // assert
        assertEquals("255.255.255.0", IpAddressUtils.longToIp(netmask));
    }

    @Test
    public void testSlashnetSize() {
        // act
        int res = IpAddressUtils.addrsInSlashnet(28);

        // assert
        assertEquals(16, res);
    }

    @Test
    public void testSlashnetSizeAgain() {
        // act
        int res = IpAddressUtils.addrsInSlashnet(24);

        // assert
        assertEquals(256, res);
    }

    @Test
    public void testIsIpAddressWithBadAddresses() {
        // act & assert
        assertFalse(IpAddressUtils.isIpAddress("1.321.2.2"));
        assertFalse(IpAddressUtils.isIpAddress("1.2.3"));
        assertFalse(IpAddressUtils.isIpAddress("1.-2.2.4"));
        assertFalse(IpAddressUtils.isIpAddress("1.bob.2.4"));
        assertFalse(IpAddressUtils.isIpAddress("1.2.3.4/33"));
        assertFalse(IpAddressUtils.isIpAddress("1.2.3.4/-1"));
        assertFalse(IpAddressUtils.isIpAddress("1.2.3.4/aa"));
    }

    @Test
    public void testIsIpAddress() {
        // act & assert
        assertTrue(IpAddressUtils.isIpAddress("1.255.2.2"));
        assertTrue(IpAddressUtils.isIpAddress("1.2.3.4"));
        assertTrue(IpAddressUtils.isIpAddress("1.2.3.4/0"));
        assertTrue(IpAddressUtils.isIpAddress("1.2.3.4/1"));
        assertTrue(IpAddressUtils.isIpAddress("1.2.3.4/32"));
    }

    @Test
    public void shouldRemoveSlashNetFromIpAddress() {
        // act
        String ipAddress = IpAddressUtils.removeSlash32FromIpAddress("10.19.1.1/32");
        String ipAddress2 = IpAddressUtils.removeSlash32FromIpAddress("10.19.1.2");
        String ipAddress3 = IpAddressUtils.removeSlashnetFromIpAddress("10.19.1.4/21");

        // assert
        assertEquals("10.19.1.1", ipAddress);
        assertEquals("10.19.1.2", ipAddress2);
        assertEquals("10.19.1.4", ipAddress3);
    }

    @Test
    public void shouldAddSlashNetToIpAddress() {
        // act
        String ipAddress = IpAddressUtils.addSlash32ToIpAddress("10.19.1.3");
        String ipAddress2 = IpAddressUtils.addSlash32ToIpAddress("10.19.1.4/32");

        // assert
        assertEquals("10.19.1.3/32", ipAddress);
        assertEquals("10.19.1.4/32", ipAddress2);
    }
}
