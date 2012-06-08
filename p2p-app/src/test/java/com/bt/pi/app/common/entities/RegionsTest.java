package com.bt.pi.app.common.entities;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.application.resource.DefaultDhtResourceWatchingStrategy;
import com.bt.pi.core.application.resource.watched.WatchedResource;

public class RegionsTest {
    private Regions regions;

    @Before
    public void setup() {
        regions = new Regions();
    }

    @Test
    public void getUrl() throws Exception {
        // act
        String result = regions.getUrl();

        // assert
        assertThat(result, equalTo("rgn:all"));
    }

    @Test
    public void getType() throws Exception {
        // act
        String result = regions.getType();

        // assert
        assertThat(result, equalTo("Regions"));
    }

    @Test
    public void putAndGet() throws Exception {
        // setup
        Region region = new Region("uk", 1, "uk.com", "");
        regions.addRegion(region);

        // act
        Region result = regions.getRegion("uk");

        // assert
        assertThat(result, equalTo(region));
    }

    @Test
    public void shouldBeAnnotatedAsWatchedResourceWithAppropriateStrategyAndIntervalSettings() {
        // act
        WatchedResource res = Regions.class.getAnnotation(WatchedResource.class);

        // assert
        assertEquals(DefaultDhtResourceWatchingStrategy.class, res.watchingStrategy());
        assertEquals(10000, res.defaultInitialResourceRefreshIntervalMillis());
        assertEquals(86400000, res.defaultRepeatingResourceRefreshIntervalMillis());
        assertEquals("availabilityzones.subscribe.initial.wait.time.millis", res.initialResourceRefreshIntervalMillisProperty());
        assertEquals("availabilityzones..subscribe.interval.millis", res.repeatingResourceRefreshIntervalMillisProperty());
    }

    @Test(expected = DuplicateRegionException.class)
    public void shouldDetectRegionHasDuplicatedRegionCode() {
        // setup
        Region region = new Region("bob", 1, "endpoint1", null);
        Region dup = new Region("bill", 1, "hello", null);
        regions.addRegion(region);

        // act
        regions.addRegion(dup);
    }

    @Test(expected = DuplicateRegionException.class)
    public void shouldDetectRegionHasDuplicatedRegionName() {
        // setup
        Region region = new Region("bob", 1, "endpoint1", null);
        Region dup = new Region("bOb", 2, "hello", null);
        regions.addRegion(region);

        // act && assert
        regions.addRegion(dup);
    }
}
