/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.net.utils;

import java.math.BigInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.util.Base62Utils;

// Final coz Emma told me to
public final class VlanAddressUtils {
    private static final Log LOG = LogFactory.getLog(VlanAddressUtils.class);
    private static final String COLON = ":";
    private static final int INSTANCE_ID_LENGTH = 10;
    private static final int MAC_ADDRESS_LENGTH = 10;
    private static final int HEX_RADIX = 16;

    static {
        new VlanAddressUtils(); // for Emma
    }

    private VlanAddressUtils() {
    }

    public static String getBridgeNameForVlan(long vlanId) {
        return String.format("pibr%d", vlanId);
    }

    public static String getMacAddressFromInstanceId(String instanceId) {
        if (instanceId == null || instanceId.length() != INSTANCE_ID_LENGTH || !instanceId.startsWith("i-"))
            throw new IllegalArgumentException(String.format("Invalid instance id %s: must be exactly 10 chars and start with 'i-'", instanceId));
        String hexInstanceId = getHexFromInstanceId(instanceId);
        StringBuilder res = new StringBuilder();
        res.append("00:");
        // d0:0d:07:Xf:cW:ch
        for (int i = 0; i < MAC_ADDRESS_LENGTH; i++) {
            if ((i + 0) % 2 == 0 && i != 0)
                res.append(COLON);

            if (hexInstanceId.length() > i) {
                res.append(hexInstanceId.charAt(i));
            } else {
                res.append("0");
            }

        }
        LOG.debug(String.format("Returning mac address %s for instance %s -- instance hex: %s ", res.toString(), instanceId, hexInstanceId));
        return res.toString();
    }

    private static String getHexFromInstanceId(String instanceId) {
        String instanceNumberHash = instanceId.substring(instanceId.indexOf("-") + 1);

        BigInteger bigInt = Base62Utils.decodeBase62(instanceNumberHash);

        return bigInt.toString(HEX_RADIX);
    }
}
