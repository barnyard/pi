package com.bt.pi.app.networkmanager.handlers;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.PublicIpAddress;

public class QueueTaskUriHelperTest {
    private static final String INSTANCE_ID = "i-123";
    private static final String PUBLIC_IP = "1.2.3.4";
    private static final String SEC_GROUP_NAME = "default";
    private static final String USER_ID = "user-id";
    private PublicIpAddress addr;

    @Before
    public void before() {
        addr = new PublicIpAddress();
        addr.setOwnerId(USER_ID);
        addr.setSecurityGroupName(SEC_GROUP_NAME);
        addr.setIpAddress(PUBLIC_IP);
        addr.setInstanceId(INSTANCE_ID);
    }

    @Test
    public void testAssociateAddressUri() {
        assertEquals("addr:1.2.3.4;sg=user-id:default;inst=i-123", QueueTaskUriHelper.getUriForAssociateAddress(addr));
    }

    @Test
    public void testDisassociateAddressUri() {
        assertEquals("addr:1.2.3.4;sg=user-id:default;inst=i-123", QueueTaskUriHelper.getUriForDisassociateAddress(addr));
    }
}
