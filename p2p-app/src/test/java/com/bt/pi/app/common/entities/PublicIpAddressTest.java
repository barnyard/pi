package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class PublicIpAddressTest {
    private PublicIpAddress publicIpAddress;

    @Before
    public void before() {
        publicIpAddress = new PublicIpAddress("1.2.3.4", "i-abc", "ownerId", "default");
    }

    @Test
    public void testGettersFromConstructor() {
        // assert
        assertEquals("1.2.3.4", publicIpAddress.getIpAddress());
        assertEquals("default", publicIpAddress.getSecurityGroupName());
        assertEquals("i-abc", publicIpAddress.getInstanceId());
        assertEquals("ownerId", publicIpAddress.getOwnerId());
    }

    @Test
    public void testGettersAndSetters() {
        // act
        publicIpAddress.setIpAddress("1.2.3.4");
        publicIpAddress.setSecurityGroupName("default");
        publicIpAddress.setInstanceId("i-abc");
        publicIpAddress.setOwnerId("ownerId");

        // assert
        assertEquals("1.2.3.4", publicIpAddress.getIpAddress());
        assertEquals("default", publicIpAddress.getSecurityGroupName());
        assertEquals("i-abc", publicIpAddress.getInstanceId());
        assertEquals("ownerId", publicIpAddress.getOwnerId());
    }

    @Test
    public void testType() {
        assertEquals("PublicIpAddress", publicIpAddress.getType());
    }

    @Test
    public void testUrl() {
        assertEquals("addr:1.2.3.4", publicIpAddress.getUrl());
    }

    @Test
    public void testToString() {
        assertTrue(publicIpAddress.toString().contains("1.2.3.4"));
        assertTrue(publicIpAddress.toString().contains("i-abc"));
        assertTrue(publicIpAddress.toString().contains("ownerId"));
        assertTrue(publicIpAddress.toString().contains("default"));
    }
}
