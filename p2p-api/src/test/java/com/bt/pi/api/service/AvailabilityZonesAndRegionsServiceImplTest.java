package com.bt.pi.api.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;

@RunWith(MockitoJUnitRunner.class)
public class AvailabilityZonesAndRegionsServiceImplTest {
    @InjectMocks
    private AvailabilityZonesAndRegionsServiceImpl availabilityZonesAndRegionsServiceImpl = new AvailabilityZonesAndRegionsServiceImpl();
    private AvailabilityZones availabilityZones;
    private Regions regions;
    @Mock
    private ApiApplicationManager apiApplicationManager;

    @Before
    public void before() {
        availabilityZones = new AvailabilityZones();
        availabilityZones.addAvailabilityZone(new AvailabilityZone("1", 1, 10, "s"));
        availabilityZones.addAvailabilityZone(new AvailabilityZone("2", 2, 10, "s"));
        availabilityZones.addAvailabilityZone(new AvailabilityZone("3", 3, 10, "s"));

        regions = new Regions();
        regions.addRegion(new Region("r1", 1, "re1", ""));
        regions.addRegion(new Region("r2", 2, "re2", ""));
        regions.addRegion(new Region("r3", 3, "re3", ""));

        when(apiApplicationManager.getAvailabilityZonesRecord()).thenReturn(availabilityZones);
        when(apiApplicationManager.getRegions()).thenReturn(regions);
    }

    @Test
    public void describeAvailabilityZonesShouldReturnAllZonesIfListIsNull() {
        // setup

        // act
        List<AvailabilityZone> describeAvailabilityZones = availabilityZonesAndRegionsServiceImpl.describeAvailabilityZones(null);

        // assert
        assertThat(describeAvailabilityZones.size(), equalTo(availabilityZones.getAvailabilityZones().size()));
        assertThat(describeAvailabilityZones.containsAll(availabilityZones.getAvailabilityZones().values()), is(true));
    }

    @Test
    public void describeAvailabilityZonesShouldReturnAllZonesIfListIsEmpty() {
        // setup

        // act
        List<AvailabilityZone> describeAvailabilityZones = availabilityZonesAndRegionsServiceImpl.describeAvailabilityZones(new ArrayList<String>());

        // assert
        assertThat(describeAvailabilityZones.size(), equalTo(availabilityZones.getAvailabilityZones().size()));
        assertThat(describeAvailabilityZones.containsAll(availabilityZones.getAvailabilityZones().values()), is(true));
    }

    @Test
    public void describeAvailabilityZonesShouldReturnOnlyRequestedZones() {
        // setup

        // act
        List<AvailabilityZone> describeAvailabilityZones = availabilityZonesAndRegionsServiceImpl.describeAvailabilityZones(Arrays.asList(new String[] { "1", "3" }));

        // assert
        assertThat(describeAvailabilityZones.size(), equalTo(2));
        assertThat(describeAvailabilityZones.contains(availabilityZones.getAvailabilityZones().get("1")), is(true));
        assertThat(describeAvailabilityZones.contains(availabilityZones.getAvailabilityZones().get("3")), is(true));
    }

    @Test
    public void describeAvailabilityZonesShouldReturnOnlyRequestedZonesAndNotPukeAtNonExistentZones() {
        // setup

        // act
        List<AvailabilityZone> describeAvailabilityZones = availabilityZonesAndRegionsServiceImpl.describeAvailabilityZones(Arrays.asList(new String[] { "1", "4", "3" }));

        // assert
        assertThat(describeAvailabilityZones.size(), equalTo(2));
        assertThat(describeAvailabilityZones.contains(availabilityZones.getAvailabilityZones().get("1")), is(true));
        assertThat(describeAvailabilityZones.contains(availabilityZones.getAvailabilityZones().get("3")), is(true));
    }

    @Test
    public void describeRegionsShouldReturnAllRegionsIfListIsNull() {
        // setup

        // act
        List<Region> describeRegions = availabilityZonesAndRegionsServiceImpl.describeRegions(null);

        // assert
        assertThat(describeRegions.size(), equalTo(regions.getRegions().size()));
        assertThat(describeRegions.containsAll(regions.getRegions().values()), is(true));
    }

    @Test
    public void describeRegionsShouldReturnAllRegionsIfListIsEmpty() {
        // setup

        // act
        List<Region> describeRegions = availabilityZonesAndRegionsServiceImpl.describeRegions(new ArrayList<String>());

        // assert
        assertThat(describeRegions.size(), equalTo(regions.getRegions().size()));
        assertThat(describeRegions.containsAll(regions.getRegions().values()), is(true));
    }

    @Test
    public void describeRegionsShouldReturnOnlyRequestedRegions() {
        // setup

        // act
        List<Region> describeRegions = availabilityZonesAndRegionsServiceImpl.describeRegions(Arrays.asList(new String[] { "r1", "r3" }));

        // assert
        assertThat(describeRegions.size(), equalTo(2));
        assertThat(describeRegions.contains(regions.getRegions().get("r1")), is(true));
        assertThat(describeRegions.contains(regions.getRegions().get("r3")), is(true));
    }

    @Test
    public void describeRegionsShouldReturnOnlyRequestedRegionsAndNotPukeAtNonExistentZones() {
        // setup

        // act
        List<Region> describeRegions = availabilityZonesAndRegionsServiceImpl.describeRegions(Arrays.asList(new String[] { "r1", "r4", "r3" }));

        // assert
        assertThat(describeRegions.size(), equalTo(2));
        assertThat(describeRegions.contains(regions.getRegions().get("r1")), is(true));
        assertThat(describeRegions.contains(regions.getRegions().get("r3")), is(true));
    }
}
