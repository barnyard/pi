package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AvailabilityZoneTest {
    @Test
    public void shouldConstructAvailabilityZone() {
        // act
        AvailabilityZone availabilityZone = new AvailabilityZone();
        availabilityZone.setAvailabilityZoneName("avzone");
        availabilityZone.setAvailabilityZoneCodeWithinRegion(123);
        availabilityZone.setRegionCode(234);
        availabilityZone.setStatus("available");

        // assert
        assertEquals("avzone", availabilityZone.getAvailabilityZoneName());
        assertEquals(123, availabilityZone.getAvailabilityZoneCodeWithinRegion());
        assertEquals(234, availabilityZone.getRegionCode());
        assertEquals("available", availabilityZone.getStatus());
    }

    @Test
    public void shouldConstructUsingFields() {
        // act
        AvailabilityZone availabilityZone = new AvailabilityZone("avzone", 123, 234, "available");

        // assert
        assertEquals("avzone", availabilityZone.getAvailabilityZoneName());
        assertEquals(123, availabilityZone.getAvailabilityZoneCodeWithinRegion());
        assertEquals(234, availabilityZone.getRegionCode());
        assertEquals("available", availabilityZone.getStatus());
    }

    @Test
    public void shouldBeEqual() {
        // setup
        AvailabilityZone availabilityZone1 = new AvailabilityZone("avzone", 123, 234, "available");
        AvailabilityZone availabilityZone2 = new AvailabilityZone("avzone", 123, 234, "available");
        AvailabilityZone availabilityZone3 = new AvailabilityZone("avzone", 123, 234, "available");

        // assert
        assertTrue(availabilityZone1.equals(availabilityZone1));
        assertTrue(availabilityZone1.equals(availabilityZone2));
        assertTrue(availabilityZone1.equals(availabilityZone3));
        assertTrue(availabilityZone2.equals(availabilityZone1));
        assertTrue(availabilityZone2.equals(availabilityZone2));
        assertTrue(availabilityZone2.equals(availabilityZone3));
        assertTrue(availabilityZone3.equals(availabilityZone1));
        assertTrue(availabilityZone3.equals(availabilityZone2));
        assertTrue(availabilityZone3.equals(availabilityZone3));
    }

    @Test
    public void shouldHaveSameHashCode() {
        // setup
        AvailabilityZone availabilityZone1 = new AvailabilityZone("avzone", 123, 234, "available");
        AvailabilityZone availabilityZone2 = new AvailabilityZone("avzone", 123, 234, "available");
        AvailabilityZone availabilityZone3 = new AvailabilityZone("avzone", 123, 234, "available");

        // assert
        assertEquals(availabilityZone1.hashCode(), availabilityZone2.hashCode());
        assertEquals(availabilityZone1.hashCode(), availabilityZone3.hashCode());
        assertEquals(availabilityZone2.hashCode(), availabilityZone3.hashCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAvzCodeShouldOnlyBe8BitsLong() {
        // act
        new AvailabilityZone("z", 456, 234, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegionCodeShouldOnlyBe8BitsLong() {
        // act
        new AvailabilityZone("z", 123, 456, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAvzCodeShouldNotBeNegative() {
        // act
        new AvailabilityZone("z", -1, 234, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegionCodeShouldNotBeNegative() {
        // act
        new AvailabilityZone("z", 123, -1, "");
    }

    @Test
    public void shouldGetUniqueAvailabilityZoneCode() {
        assertEquals(0x1234, new AvailabilityZone("avzone", 0x34, 0x12, "available").getGlobalAvailabilityZoneCode());
    }
}
