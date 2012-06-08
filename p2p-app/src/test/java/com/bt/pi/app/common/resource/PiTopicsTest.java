package com.bt.pi.app.common.resource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.scope.NodeScope;

public class PiTopicsTest {
    @Test
    public void testGetUrl() {
        assertEquals(ResourceSchemes.TOPIC + ":CREATE_VOLUME", PiTopics.CREATE_VOLUME.getUrl());
    }

    @Test
    public void testGetNodeScope() {
        assertEquals(NodeScope.AVAILABILITY_ZONE, PiTopics.ATTACH_VOLUME.getNodeScope());
        assertEquals(NodeScope.AVAILABILITY_ZONE, PiTopics.CREATE_VOLUME.getNodeScope());
    }

    @Test
    public void testGetLocation() {
        assertEquals(new PiLocation(PiTopics.CREATE_VOLUME.getUrl(), PiTopics.CREATE_VOLUME.getNodeScope()), PiTopics.CREATE_VOLUME.getPiLocation());
    }

    @Test
    public void testValues() {
        assertEquals(9, PiTopics.values().length);
    }

    @Test
    public void testValueOf() {
        assertEquals(PiTopics.CREATE_VOLUME, PiTopics.valueOf("CREATE_VOLUME"));
    }
}
