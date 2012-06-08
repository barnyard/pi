package com.bt.pi.app.instancemanager.images;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.instancemanager.images.PlatformBuilderException;

public class PlatformBuilderExceptionTest {

    private PlatformBuilderException platformBuilderException;

    @Before
    public void setUp() throws Exception {
        platformBuilderException = new PlatformBuilderException("Unable to build platform", new Exception());
    }

    @Test
    public void testPlatformBuilderException() {
        // assert
        assertEquals("Unable to build platform", platformBuilderException.getMessage());
    }

}