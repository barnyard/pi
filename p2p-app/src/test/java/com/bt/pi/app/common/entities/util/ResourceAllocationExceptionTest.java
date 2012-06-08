package com.bt.pi.app.common.entities.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.pi.app.common.entities.util.ResourceAllocationException;

public class ResourceAllocationExceptionTest {

    @Test
    public void testContstructor() {

        ResourceAllocationException exception = new ResourceAllocationException("wow");

        assertTrue(exception.getMessage().contains("wow"));
    }
}
