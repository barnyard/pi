package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.pi.app.common.entities.NoFreePublicAddressesAvailableException;

public class NoFreePublicAddressesAvailableExceptionTest {

    @Test
    public void testConstructor() {
        Exception innerException = new Exception();
        NoFreePublicAddressesAvailableException exception = new NoFreePublicAddressesAvailableException("poo", innerException);

        assertTrue(exception.getMessage().contains("poo"));
        assertEquals(innerException, exception.getCause());
    }
}
