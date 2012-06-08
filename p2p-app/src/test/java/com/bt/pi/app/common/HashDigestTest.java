package com.bt.pi.app.common;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.security.MessageDigest;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bt.pi.app.common.util.HashDigest;

public class HashDigestTest {
    @BeforeClass
    public static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void shouldTestThatMessageDigestsAreOk() throws Exception {
        for (HashDigest hashDigest : HashDigest.values()) {
            // act
            MessageDigest messageDigest = hashDigest.getMessageDigest();

            // assert
            assertNotNull(hashDigest.toString(), messageDigest);
            assertThat(messageDigest.getAlgorithm(), equalTo(hashDigest.toString()));
        }
    }
}
