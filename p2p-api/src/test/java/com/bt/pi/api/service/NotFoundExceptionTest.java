package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NotFoundExceptionTest {

    @Test
    public void testMessageSetThroughConstructor() {
        assertEquals("bob", new NotFoundException("bob").getMessage());
    }
}
