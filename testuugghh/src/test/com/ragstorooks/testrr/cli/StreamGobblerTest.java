package com.ragstorooks.testrr.cli;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

public class StreamGobblerTest {
    private StreamGobbler streamGobbler;
    private InputStream inputStream;

    @Before
    public void before() throws Exception {
        byte[] byteArray = "abc".getBytes("UTF-8");
        inputStream = new ByteArrayInputStream(byteArray);

        streamGobbler = new StreamGobbler(inputStream, null);
    }

    /**
     * Given an input stream, consume it and log its contents
     */
    @Test
    public void shouldConsumeAndLogGivenStream() throws Exception {
        // act
        streamGobbler.run();

        // assert
        assertEquals(0, inputStream.available());
    }
}
