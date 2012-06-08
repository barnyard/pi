package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DescribeAvailabilityZonesDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeAvailabilityZonesResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeAvailabilityZonesSetType;
import com.amazonaws.ec2.doc.x20081201.DescribeAvailabilityZonesType;
import com.bt.pi.api.service.AvailabilityZonesAndRegionsService;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.Region;

public class DescribeAvailabilityZonesHandlerTest extends AbstractHandlerTest {

    private DescribeAvailabilityZonesHandler describeAvailabilityZonesHandler;
    private AvailabilityZonesAndRegionsService availabilityZonesAndRegionsService;

    @Before
    public void setUp() throws Exception {
        super.before();

        Region region1 = new Region("region1", 1, "endpoint", "");
        Region region2 = new Region("region2", 2, "endpoint", "");
        Region region3 = new Region("region3", 3, "endpoint", "");

        List<Region> regionsList = new ArrayList<Region>();
        regionsList.add(region1);
        regionsList.add(region2);
        regionsList.add(region3);

        List<String> availabilityZoneNames = new ArrayList<String>();
        availabilityZoneNames.add("zone1");
        availabilityZoneNames.add("zone2");

        List<AvailabilityZone> value = new ArrayList<AvailabilityZone>();
        value.add(new AvailabilityZone("zone1", 123, 1, "available"));
        value.add(new AvailabilityZone("zone2", 123, 2, "available"));

        availabilityZonesAndRegionsService = mock(AvailabilityZonesAndRegionsService.class);
        when(availabilityZonesAndRegionsService.describeAvailabilityZones(availabilityZoneNames)).thenReturn(value);
        when(availabilityZonesAndRegionsService.describeAvailabilityZones(new ArrayList<String>())).thenReturn(value);
        when(availabilityZonesAndRegionsService.describeRegions(null)).thenReturn(regionsList);

        this.describeAvailabilityZonesHandler = new DescribeAvailabilityZonesHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        describeAvailabilityZonesHandler.setAvailabilityZonesAndRegionsService(availabilityZonesAndRegionsService);
    }

    @Test
    public void testDescribeAvailabilityZones() {
        // setup
        DescribeAvailabilityZonesDocument requestDocument = DescribeAvailabilityZonesDocument.Factory.newInstance();
        DescribeAvailabilityZonesType addNewDescribeAvailabilityZones = requestDocument.addNewDescribeAvailabilityZones();
        DescribeAvailabilityZonesSetType addNewAvailabilityZoneSet = addNewDescribeAvailabilityZones.addNewAvailabilityZoneSet();
        addNewAvailabilityZoneSet.addNewItem().setZoneName("zone1");
        addNewAvailabilityZoneSet.addNewItem().setZoneName("zone2");

        // act
        DescribeAvailabilityZonesResponseDocument result = this.describeAvailabilityZonesHandler.describeAvailabilityZones(requestDocument);

        // assert
        assertEquals(2, result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray().length);
        assertEquals("zone1", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(0).getZoneName());
        assertEquals("region1", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(0).getRegionName());
        assertEquals("available", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(0).getZoneState());
        assertEquals("zone2", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(1).getZoneName());
        assertEquals("region2", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(1).getRegionName());
        assertEquals("available", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(1).getZoneState());
    }

    @Test
    public void testDescribeAvailableZonesWithNoZoneNamesGiven() {
        // setup
        DescribeAvailabilityZonesDocument requestDocument = DescribeAvailabilityZonesDocument.Factory.newInstance();
        DescribeAvailabilityZonesType addNewDescribeAvailabilityZones = requestDocument.addNewDescribeAvailabilityZones();
        addNewDescribeAvailabilityZones.addNewAvailabilityZoneSet();

        // act
        DescribeAvailabilityZonesResponseDocument result = this.describeAvailabilityZonesHandler.describeAvailabilityZones(requestDocument);

        // assert
        assertEquals(2, result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray().length);
        assertEquals("zone1", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(0).getZoneName());
        assertEquals("region1", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(0).getRegionName());
        assertEquals("available", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(0).getZoneState());
        assertEquals("zone2", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(1).getZoneName());
        assertEquals("region2", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(1).getRegionName());
        assertEquals("available", result.getDescribeAvailabilityZonesResponse().getAvailabilityZoneInfo().getItemArray(1).getZoneState());
    }
}
