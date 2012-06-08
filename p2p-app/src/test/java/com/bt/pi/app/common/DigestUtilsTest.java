package com.bt.pi.app.common;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.util.DigestUtils;
import com.bt.pi.app.common.util.HashDigest;

public class DigestUtilsTest {
    private String input;
    private DigestUtils digestUtils;

    @Before
    public void setup() {
        input = "input";
        digestUtils = new DigestUtils();
    }

    @Test
    public void shouldGetDigestBase64() throws Exception {
        // act
        String result = digestUtils.getDigestBase64(input, HashDigest.SHA256, true);

        // assert
        assertNotNull(result);
    }

    @Test
    public void shouldGetDifferentDigestBase64EveryTimeIfRandomizeSetToTrue() throws Exception {
        // setup
        String expected = digestUtils.getDigestBase64(input, HashDigest.SHA256, true);

        // act
        String result = digestUtils.getDigestBase64(input, HashDigest.SHA256, true);

        // assert
        assertThat(result, not(equalTo(expected)));
    }

    @Test
    public void shouldGetSameDigestBase64EveryTimeIfRandomizeSetToFalse() throws Exception {
        // setup
        String expected = digestUtils.getDigestBase64(input, HashDigest.SHA256, false);

        // act
        String result = digestUtils.getDigestBase64(input, HashDigest.SHA256, false);

        // assert
        assertThat(result, equalTo(expected));
    }

    @Test
    public void shouldGetDigestBase62() throws Exception {
        // act
        String result = digestUtils.getDigestBase62(input, HashDigest.MD5, true);

        // assert
        assertNotNull(result);
    }

    @Test
    public void shouldGetDifferentDigestBase62EveryTimeIfRandomizeSetToTrue() throws Exception {
        // setup
        String expected = digestUtils.getDigestBase64(input, HashDigest.MD5, true);

        // act
        String result = digestUtils.getDigestBase64(input, HashDigest.MD5, true);

        // assert
        assertThat(result, not(equalTo(expected)));
    }

    @Test
    public void shouldGetSameDigestBase62EveryTimeIfRandomizeSetToFalse() throws Exception {
        // setup
        String expected = digestUtils.getDigestBase64(input, HashDigest.MD5, false);

        // act
        String result = digestUtils.getDigestBase64(input, HashDigest.MD5, false);

        // assert
        assertThat(result, equalTo(expected));
    }
}
