/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.net.utils;

import java.util.regex.Pattern;

/* copied/corrupted from http://teneo.wordpress.com/2008/12/23/java-ip-address-to-integer-and-back/ */
public final class IpAddressUtils {
    public static final String MAX_IP_ADDRESS = "255.255.255.255";
    private static final int SLASHNET_VALUE = 32;
    private static final String DOT = ".";
    private static final int TWO_FIVE_SIX = 256;
    private static final int TWO_FIVE_FIVE = 255;
    private static final int THREE = 3;
    private static final int X_FF = 0xFF;
    private static final int EIGHT = 8;
    private static final int SIXTEEN = 16;
    private static final int TWENTY_FOUR = 24;
    private static final String REGEX_255 = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
    private static final Pattern IP_PATTERN = Pattern.compile("^(?:" + REGEX_255 + "\\.){3}" + REGEX_255 + "$");
    private static final String SLASH = "/";
    private static final String SLASHNET_32 = "/32";
    private static final int NUMBER_OF_BITS_IN_IP = 32;
    private static final String EMPTY = "";

    static {
        new IpAddressUtils(); // For Emma
    }

    private IpAddressUtils() {
    }

    public static String longToIp(long i) {
        long ipOnly = i & 0xFFFFFFFF;
        long netmask = (long) (i >> NUMBER_OF_BITS_IN_IP);
        StringBuffer result = new StringBuffer();
        result.append((ipOnly >> TWENTY_FOUR) & X_FF);
        result.append(DOT);
        result.append((ipOnly >> SIXTEEN) & X_FF);
        result.append(DOT);
        result.append((ipOnly >> EIGHT) & X_FF);
        result.append(DOT);
        result.append(ipOnly & X_FF);
        String fullIpAddress = "";
        if (netmask > 0) {
            fullIpAddress = result.toString() + SLASH + slashnetFromAddrs(netmask);
        } else
            fullIpAddress = result.toString();

        return fullIpAddress;
    }

    public static long ipToLong(String addr) {
        String ipAddress = addr;
        long netMask = 0;
        if (addr.contains(SLASH)) {
            String[] addressAndSubnet = addr.split(SLASH);
            netMask = addrsInSlashnet(Integer.valueOf(addressAndSubnet[1]));
            ipAddress = addressAndSubnet[0];
        }
        String[] addrArray = ipAddress.split("\\.");
        long num = 0;
        for (int i = 0; i < addrArray.length; i++) {
            try {
                if (Integer.parseInt(addrArray[i]) > TWO_FIVE_FIVE)
                    throw new IllegalArgumentException("IP address out of range");
                int power = THREE - i;
                num += Integer.parseInt(addrArray[i]) % TWO_FIVE_SIX * Math.pow(TWO_FIVE_SIX, power);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("Failed to parse IP address %s", addr));
            }
        }
        // throw the ip on the back of the netmask.
        num = netMask << NUMBER_OF_BITS_IN_IP | num;
        return num;
    }

    public static String increment(String ip, long i) {
        return longToIp(ipToLong(ip) + i);
    }

    public static long decrement(String ip, long i) {
        return ipToLong(ip) - i;
    }

    public static int netmaskToSlashnet(String netmask) {
        return SLASHNET_VALUE - ((int) log2((double) (IpAddressUtils.decrement(MAX_IP_ADDRESS, ipToLong(netmask)))) + 1);
    }

    public static int netmaskToSlashnet(long netmask) {
        return SLASHNET_VALUE - ((int) log2((double) (IpAddressUtils.decrement(MAX_IP_ADDRESS, netmask))) + 1);
    }

    private static double log2(double d) {
        return Math.log(d) / Math.log(2.0);
    }

    public static long netmaskToNetSize(long netmask) {
        return ipToLong(IpAddressUtils.MAX_IP_ADDRESS) - netmask + 1;
    }

    public static long netSizeToNetmask(long netSize) {
        return IpAddressUtils.decrement(IpAddressUtils.MAX_IP_ADDRESS, netSize - 1);
    }

    public static int addrsInSlashnet(int slashnet) {
        int delta = SLASHNET_VALUE - slashnet;
        return (int) Math.pow(2, delta);
    }

    public static int slashnetFromAddrs(long addrs) {
        return (int) (SLASHNET_VALUE - log2(addrs));
    }

    public static boolean isIpAddress(String str) {
        String address = str;
        if (str.contains(SLASH)) {
            address = str.split(SLASH)[0];
            String size = str.split(SLASH)[1];
            try {
                int sizeInt = Integer.parseInt(size);
                if (sizeInt < 0 || sizeInt > SLASHNET_VALUE)
                    return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return IP_PATTERN.matcher(address).matches();
    }

    public static String addSlash32ToIpAddress(String ipAddress) {
        String address = ipAddress;
        if (!ipAddress.contains(SLASH))
            address += SLASHNET_32;
        return address;
    }

    public static String removeSlash32FromIpAddress(String ipAddress) {
        String address = ipAddress;
        if (ipAddress.contains(SLASH))
            address = address.replace(SLASHNET_32, EMPTY);

        return address;
    }

    public static String removeSlashnetFromIpAddress(String ipAddress) {
        String address = ipAddress;
        if (ipAddress.contains(SLASH))
            address = address.substring(0, address.indexOf(SLASH));

        return address;
    }
}
