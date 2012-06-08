package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.ec2.doc.x20081201.DescribeRegionsDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeRegionsResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeRegionsSetType;
import com.amazonaws.ec2.doc.x20081201.DescribeRegionsType;
import com.bt.pi.api.service.AvailabilityZonesAndRegionsService;
import com.bt.pi.app.common.entities.Region;

public class DescribeRegionsHandlerTest extends AbstractHandlerTest {
    private static final String US_EAST = "us_east";
    private static final String US_EAST_COM = "us_east.com";
    private static final String US_WEST = "us_west";
    private static final String US_WEST_COM = "us_west.com";

    private DescribeRegionsHandler handler;
    private AvailabilityZonesAndRegionsService availabilityZonesAndRegions;
    private List<Region> listOfRegions;
    private DescribeRegionsDocument describeRegionsDocument;
    private DescribeRegionsSetType regionsSetType;

    @Before
    public void setup() {
        listOfRegions = new ArrayList<Region>(Arrays.asList(new Region[] { new Region(US_EAST, 1, US_EAST_COM, "pisss." + US_EAST) }));
        availabilityZonesAndRegions = mock(AvailabilityZonesAndRegionsService.class);

        handler = new DescribeRegionsHandler();
        handler.setAvailabilityZonesAndRegions(availabilityZonesAndRegions);
    }

    @Before
    public void setupDescribeRegionsDocument() {
        describeRegionsDocument = DescribeRegionsDocument.Factory.newInstance();
        DescribeRegionsType describeRegionsType = describeRegionsDocument.addNewDescribeRegions();
        regionsSetType = describeRegionsType.addNewRegionSet();
    }

    @Test
    public void testDescribeRegions20081201() {
        // setup
        List<String> regions = Arrays.asList(new String[] { US_EAST });
        when(availabilityZonesAndRegions.describeRegions(regions)).thenReturn(listOfRegions);

        regionsSetType.addNewItem().setRegionName(US_EAST);

        // act
        DescribeRegionsResponseDocument result = handler.describeRegions(describeRegionsDocument);

        // assert
        assertEquals(1, result.getDescribeRegionsResponse().getRegionInfo().getItemArray().length);
        assertEquals(US_EAST_COM, result.getDescribeRegionsResponse().getRegionInfo().getItemArray(0).getRegionEndpoint());
        assertEquals(US_EAST, result.getDescribeRegionsResponse().getRegionInfo().getItemArray(0).getRegionName());
    }

    @Test
    public void testDescribeRegionsEmpty20081201() {
        // setup
        when(availabilityZonesAndRegions.describeRegions(new ArrayList<String>())).thenReturn(listOfRegions);

        // act
        DescribeRegionsResponseDocument result = handler.describeRegions(describeRegionsDocument);

        // assert
        assertEquals(1, result.getDescribeRegionsResponse().getRegionInfo().getItemArray().length, 1);
        assertEquals(US_EAST_COM, result.getDescribeRegionsResponse().getRegionInfo().getItemArray(0).getRegionEndpoint());
        assertEquals(US_EAST, result.getDescribeRegionsResponse().getRegionInfo().getItemArray(0).getRegionName());
    }

    @Test
    public void testDescribeRegionsMultiple20081201() {
        // setup
        listOfRegions.add(new Region(US_WEST, 2, US_WEST_COM, "pisss." + US_WEST_COM));
        List<String> regions = Arrays.asList(new String[] { US_EAST });
        when(availabilityZonesAndRegions.describeRegions(regions)).thenReturn(listOfRegions);

        regionsSetType.addNewItem().setRegionName(US_EAST);

        // act
        DescribeRegionsResponseDocument result = handler.describeRegions(describeRegionsDocument);

        // assert
        assertEquals(2, result.getDescribeRegionsResponse().getRegionInfo().sizeOfItemArray(), 2);
        assertEquals(US_EAST_COM, result.getDescribeRegionsResponse().getRegionInfo().getItemArray(0).getRegionEndpoint());
        assertEquals(US_EAST, result.getDescribeRegionsResponse().getRegionInfo().getItemArray(0).getRegionName());
        assertEquals(US_WEST_COM, result.getDescribeRegionsResponse().getRegionInfo().getItemArray(1).getRegionEndpoint());
        assertEquals(US_WEST, result.getDescribeRegionsResponse().getRegionInfo().getItemArray(1).getRegionName());
    }

}
