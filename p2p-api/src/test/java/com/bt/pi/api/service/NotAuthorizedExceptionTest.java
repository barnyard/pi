package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class NotAuthorizedExceptionTest {

    private NotAuthorizedException notAuthorizedException;

    @Before
    public void before() {
        notAuthorizedException = new NotAuthorizedException("oboe");
    }

    @Test
    public void testMessageSet() {
        assertEquals("oboe", notAuthorizedException.getMessage());
    }
}
