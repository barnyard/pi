package com.bt.pi.app.common.util;

import java.math.BigInteger;

public class Base62Utils {
    private static final String BASE62_DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final BigInteger SIXTY_TWO = new BigInteger("62");

    protected Base62Utils() {
    }

    public static String encodeToBase62(byte[] bytes) {
        BigInteger value = new BigInteger(bytes);
        return encodeToBase62(value);
    }

    public static String encodeToBase62(BigInteger num) {
        BigInteger value = BigInteger.ZERO.add(num);
        String result = "";
        while (!value.equals(BigInteger.ZERO)) {
            result = BASE62_DIGITS.charAt(value.mod(SIXTY_TWO).intValue()) + result;
            value = value.divide(SIXTY_TWO);
        }
        return result;
    }

    public static BigInteger decodeBase62(String base62String) {
        BigInteger result = BigInteger.ZERO;
        // work from the back
        for (int i = 0; i < base62String.length(); i++) {
            char c = base62String.charAt(base62String.length() - 1 - i);
            BigInteger currentHexValue = new BigInteger(String.valueOf(BASE62_DIGITS.indexOf(c))).multiply(SIXTY_TWO.pow(i));
            result = result.add(currentHexValue);
        }
        return result;
    }
}
