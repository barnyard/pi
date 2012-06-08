package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.bt.pi.app.common.images.platform.ImagePlatform;

public class ConsoleOutputTest {

    @Test
    public void shouldConstruct() {
        // setup
        // act
        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.setOutput("output");
        consoleOutput.setTimestamp(1l);
        // assert
        assertEquals("output", consoleOutput.getOutput());
        assertEquals(1l, consoleOutput.getTimestamp());
        assertNull(consoleOutput.getUrl());
        assertEquals(ConsoleOutput.class.getSimpleName(), consoleOutput.getType());
    }

    @Test
    public void shouldConstructWithalternateConstructor() {

        // act
        ConsoleOutput consoleOutput = new ConsoleOutput("laptop", ImagePlatform.linux);

        // assert
        assertEquals("laptop", consoleOutput.getInstanceId());
        assertNull(consoleOutput.getOutput());
        assertEquals(-1, consoleOutput.getTimestamp());
        assertEquals(ImagePlatform.linux, consoleOutput.getImagePlatform());
    }

    @Test
    public void shouldConstructUsingFields() {
        // setup
        // act
        ConsoleOutput consoleOutput = new ConsoleOutput("output", 1l, "bob", ImagePlatform.windows);
        // assert
        assertEquals("output", consoleOutput.getOutput());
        assertEquals(1l, consoleOutput.getTimestamp());
        assertEquals("bob", consoleOutput.getInstanceId());
        assertEquals(ImagePlatform.windows, consoleOutput.getImagePlatform());
    }

}
