package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.OwnerIdGroupNamePair;

public class OwnerIdGroupNamePairTest {
    private OwnerIdGroupNamePair ownerIdGroupNamePair;

    @Before
    public void before() {
        ownerIdGroupNamePair = new OwnerIdGroupNamePair("user", "loser");
    }

    @Test
    public void testSecurityGroupId() {
        assertEquals("user:loser", ownerIdGroupNamePair.getSecurityGroupId());
    }

    @Test
    public void testToString() {
        assertEquals("user:loser", ownerIdGroupNamePair.toString());
    }
}
