package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class ResourceSchemesTest {

    @Test
    public void testEnum() {
        System.err.println(Arrays.toString(ResourceSchemes.values()));
        assertEquals(ResourceSchemes.IMG, ResourceSchemes.valueOf("IMG"));
        assertEquals("img", ResourceSchemes.IMG.toString());

        assertEquals(ResourceSchemes.AVZ_APP, ResourceSchemes.valueOf("AVZ_APP"));
        assertEquals("avzapp", ResourceSchemes.AVZ_APP.toString());

        assertEquals(ResourceSchemes.REGION_APP, ResourceSchemes.valueOf("REGION_APP"));
        assertEquals("regionapp", ResourceSchemes.REGION_APP.toString());

        assertEquals(ResourceSchemes.GLOBAL_APP, ResourceSchemes.valueOf("GLOBAL_APP"));
        assertEquals("globalapp", ResourceSchemes.GLOBAL_APP.toString());
    }
}
