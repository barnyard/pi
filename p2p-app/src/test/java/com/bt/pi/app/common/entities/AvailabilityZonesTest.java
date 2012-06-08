package com.bt.pi.app.common.entities;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.application.resource.DefaultDhtResourceWatchingStrategy;
import com.bt.pi.core.application.resource.watched.WatchedResource;

public class AvailabilityZonesTest {
    private AvailabilityZones availabilityZones;

    AvailabilityZone availabilityZone;

    @Before
    public void setup() {
        availabilityZones = new AvailabilityZones();
        availabilityZone = new AvailabilityZone("bob", 0, 99, "good");
    }

    @Test
    public void getUrl() throws Exception {
        // act
        String result = availabilityZones.getUrl();

        // assert
        assertThat(result, equalTo("avz:all"));
    }

    @Test
    public void getType() throws Exception {
        // act
        String result = availabilityZones.getType();

        // assert
        assertThat(result, equalTo("AvailabilityZones"));
    }

    @Test
    public void putAndGetByName() throws Exception {
        // setup
        AvailabilityZone availabilityZone = new AvailabilityZone("avail", 133, 99, "status");
        availabilityZones.addAvailabilityZone(availabilityZone);

        // act
        AvailabilityZone result = availabilityZones.getAvailabilityZoneByName("avail");

        // assert
        assertThat(result, equalTo(availabilityZone));
    }

    @Test(expected = AvailabilityZoneNotFoundException.class)
    public void failToGetByUnknownName() throws Exception {
        // setup
        AvailabilityZone availabilityZone1 = new AvailabilityZone("avail", 0x13, 0x99, "status");
        availabilityZones.addAvailabilityZone(availabilityZone1);

        // act
        availabilityZones.getAvailabilityZoneByName("turkey");
    }

    @Test
    public void putAndGetByCode() throws Exception {
        // setup
        AvailabilityZone availabilityZone1 = new AvailabilityZone("avail", 0x13, 0x99, "status");
        AvailabilityZone availabilityZone2 = new AvailabilityZone("noavail", 0x23, 0x99, "status");
        availabilityZones.addAvailabilityZone(availabilityZone1);
        availabilityZones.addAvailabilityZone(availabilityZone2);

        // act
        AvailabilityZone result = availabilityZones.getAvailabilityZoneByGlobalAvailabilityZoneCode(0x9923);

        // assert
        assertThat(result, equalTo(availabilityZone2));
    }

    @Test(expected = AvailabilityZoneNotFoundException.class)
    public void failToGetByUnknownCode() throws Exception {
        // setup
        AvailabilityZone availabilityZone1 = new AvailabilityZone("avail", 0x13, 0x99, "status");
        availabilityZones.addAvailabilityZone(availabilityZone1);

        // act
        availabilityZones.getAvailabilityZoneByGlobalAvailabilityZoneCode(0x2399);
    }

    @Test
    public void shouldBeAnnotatedAsWatchedResourceWithAppropriateStrategyAndIntervalSettings() {
        // act
        WatchedResource res = AvailabilityZones.class.getAnnotation(WatchedResource.class);

        // assert
        assertEquals(DefaultDhtResourceWatchingStrategy.class, res.watchingStrategy());
        assertEquals(10000, res.defaultInitialResourceRefreshIntervalMillis());
        assertEquals(86400000, res.defaultRepeatingResourceRefreshIntervalMillis());
        assertEquals("availabilityzones.subscribe.initial.wait.time.millis", res.initialResourceRefreshIntervalMillisProperty());
        assertEquals("availabilityzones..subscribe.interval.millis", res.repeatingResourceRefreshIntervalMillisProperty());
    }

    @Test(expected = DuplicateAvailabilityZoneException.class)
    public void shouldDetectAvailabilityZoneHasDuplicatedAvailabilityZoneCode() {
        // setup
        AvailabilityZone dup = new AvailabilityZone("bill", 0, 99, "good");
        availabilityZones.addAvailabilityZone(availabilityZone);

        // act && assert
        availabilityZones.addAvailabilityZone(dup);
    }

    @Test(expected = DuplicateAvailabilityZoneException.class)
    public void shouldDetectNonGloballyUniqueAvailabilityZone() {
        // setup
        availabilityZones.addAvailabilityZone(availabilityZone);

        AvailabilityZone dup = new AvailabilityZone("bill", 0, 99, "good");

        // act && assert
        availabilityZones.addAvailabilityZone(dup);
    }

    @Test(expected = DuplicateAvailabilityZoneException.class)
    public void shouldDetectAvailabilityZoneHasDuplicatedAvailabilityZoneName() {
        // setup
        availabilityZone = new AvailabilityZone("bob", 0x01, 0x10, "moo");
        AvailabilityZone dup = new AvailabilityZone("bOb", 0x01, 0x20, "baah");

        availabilityZones.addAvailabilityZone(availabilityZone);

        // act && assert
        availabilityZones.addAvailabilityZone(dup);
    }

    @Test
    public void shouldAddNonDuplicatedAvz() {
        // setup
        AvailabilityZone dup = new AvailabilityZone("bill", 3, 0x33, "bad");
        availabilityZones.addAvailabilityZone(availabilityZone);

        // act
        availabilityZones.addAvailabilityZone(dup);

        // assert
        assertEquals("bill", availabilityZones.getAvailabilityZoneByName("bill").getAvailabilityZoneName());
    }
}
