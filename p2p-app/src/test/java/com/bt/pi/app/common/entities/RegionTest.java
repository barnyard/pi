package com.bt.pi.app.common.entities;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class RegionTest {
    private String regionName = "uk";
    private String regionEndpoint = "uk.com";

    private Region region;

    @Before
    public void setup() {
        region = new Region();
    }

    @Test
    public void testRegionName() {
        // setup
        region.setRegionName(regionName);

        // act
        String result = region.getRegionName();

        // assert
        assertThat(result, equalTo(regionName));
    }

    @Test
    public void testRegionEndpoint() {
        // setup
        region.setRegionEndpoint(regionEndpoint);

        // act
        String result = region.getRegionEndpoint();

        // assert
        assertThat(result, equalTo(regionEndpoint));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCodeShouldOnlyBe8BitsLong() {
        // act
        new Region("r", 456, "e", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCodeShouldNotBeNegative() {
        // act
        new Region("r", -1, "e", "");
    }
}
