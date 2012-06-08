package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IllegalStateExceptionTest {

    @Test
    public void testException() {
        IllegalStateException foo = new IllegalStateException("bob");

        assertEquals("bob", foo.getMessage());
    }
}
