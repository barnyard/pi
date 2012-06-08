package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.pi.app.common.entities.VlanAllocationException;

public class VlanAllocationExceptionTest {

    @Test
    public void testContstructor() {

        VlanAllocationException exception = new VlanAllocationException("wow");

        assertTrue(exception.getMessage().contains("wow"));
    }
}
