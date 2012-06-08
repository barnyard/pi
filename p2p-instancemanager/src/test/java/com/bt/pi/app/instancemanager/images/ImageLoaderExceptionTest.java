package com.bt.pi.app.instancemanager.images;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

public class ImageLoaderExceptionTest {

    ImageLoaderException imageLoaderException;

    @Test
    public void shouldSetTheExceptionMessage() {
        // setup
        String message = "Unable to open file";

        // act
        imageLoaderException = new ImageLoaderException(message);

        // assert
        assertEquals(message, imageLoaderException.getMessage());
    }

    @Test
    public void shouldSetTheExceptionMessageWithTheExceptionObject() {
        // setup
        String message = "Unable to open file";

        // act
        imageLoaderException = new ImageLoaderException(message, new IOException("internal error"));

        // assert
        assertEquals(message, imageLoaderException.getMessage());
        assertEquals("internal error", imageLoaderException.getCause().getMessage());
    }
}
