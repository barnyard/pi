package com.bt.pi.app.common.entities.util;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.util.ResourceAllocation;

public class ResourceAllocationTest {
    private ResourceAllocation resourceAllocation;

    @Before
    public void before() {
        resourceAllocation = new ResourceAllocation(2, 3);
    }

    @Test
    public void shouldGenToString() {
        // act
        String res = resourceAllocation.toString();

        // assert
        assertTrue(res.contains("[resource=2,step=3]"));
    }
}
