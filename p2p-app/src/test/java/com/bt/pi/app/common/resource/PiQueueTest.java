package com.bt.pi.app.common.resource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.scope.NodeScope;

public class PiQueueTest {

    @Test
    public void testValues() throws Exception {
        assertEquals(18, PiQueue.values().length);
    }

    @Test
    public void testGetUrl() throws Exception {
        assertEquals(ResourceSchemes.QUEUE + ":CREATE_VOLUME", PiQueue.CREATE_VOLUME.getUrl());
    }

    @Test
    public void testToString() throws Exception {
        assertEquals("CREATE_VOLUME", PiQueue.CREATE_VOLUME.toString());
    }

    @Test
    public void testValueOf() throws Exception {
        assertEquals(PiQueue.CREATE_VOLUME, PiQueue.valueOf("CREATE_VOLUME"));
        assertEquals(PiQueue.DELETE_VOLUME, PiQueue.valueOf("DELETE_VOLUME"));
        assertEquals(PiQueue.ATTACH_VOLUME, PiQueue.valueOf("ATTACH_VOLUME"));
        assertEquals(PiQueue.DETACH_VOLUME, PiQueue.valueOf("DETACH_VOLUME"));
        assertEquals(PiQueue.DECRYPT_IMAGE, PiQueue.valueOf("DECRYPT_IMAGE"));
        assertEquals(PiQueue.RUN_INSTANCE, PiQueue.valueOf("RUN_INSTANCE"));
        assertEquals(PiQueue.ASSOCIATE_ADDRESS, PiQueue.valueOf("ASSOCIATE_ADDRESS"));
        assertEquals(PiQueue.DISASSOCIATE_ADDRESS, PiQueue.valueOf("DISASSOCIATE_ADDRESS"));
        assertEquals(PiQueue.UPDATE_SECURITY_GROUP, PiQueue.valueOf("UPDATE_SECURITY_GROUP"));
        assertEquals(PiQueue.REMOVE_SECURITY_GROUP, PiQueue.valueOf("REMOVE_SECURITY_GROUP"));
        assertEquals(PiQueue.TERMINATE_INSTANCE, PiQueue.valueOf("TERMINATE_INSTANCE"));
        assertEquals(PiQueue.CREATE_SNAPSHOT, PiQueue.valueOf("CREATE_SNAPSHOT"));
        assertEquals(PiQueue.REMOVE_SNAPSHOT_FROM_USER, PiQueue.valueOf("REMOVE_SNAPSHOT_FROM_USER"));
        assertEquals(PiQueue.REMOVE_INSTANCE_FROM_USER, PiQueue.valueOf("REMOVE_INSTANCE_FROM_USER"));
        assertEquals(PiQueue.PAUSE_INSTANCE, PiQueue.valueOf("PAUSE_INSTANCE"));
    }

    @Test
    public void testGetPiLocation() {
        // act
        PiLocation result = PiQueue.CREATE_VOLUME.getPiLocation();

        // assert
        assertEquals(PiQueue.CREATE_VOLUME.getUrl(), result.getUrl());
        assertEquals(NodeScope.AVAILABILITY_ZONE, result.getNodeScope());
    }
}
