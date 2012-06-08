package com.bt.pi.app.common;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.util.Base62Utils;

public class Base62UtilsTest {
    private ArrayList<BigInteger> numbersToTest = new ArrayList<BigInteger>();

    @Before
    public void before() {
        numbersToTest.add(new BigInteger(String.valueOf(System.currentTimeMillis() * new Random().nextInt(40234))));
        numbersToTest.add(new BigInteger("53690189897925"));
        numbersToTest.add(new BigInteger("4294967295"));
    }

    @Test
    public void testRoundTrip() {
        for (int i = 0; i < numbersToTest.size(); i++) {
            BigInteger num = numbersToTest.get(i);

            // act
            String str = Base62Utils.encodeToBase62(num);
            System.err.println("Num : " + num + " encoded: " + str);
            BigInteger result = Base62Utils.decodeBase62(str);
            System.err.println("Str : " + str + " decoded: " + result);

            assertEquals(num, result);
        }
    }
}
