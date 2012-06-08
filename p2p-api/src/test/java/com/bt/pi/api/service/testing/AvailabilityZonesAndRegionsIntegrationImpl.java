package com.bt.pi.api.service.testing;

import java.util.ArrayList;
import java.util.List;

import com.bt.pi.api.service.AvailabilityZonesAndRegionsService;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.Region;

public class AvailabilityZonesAndRegionsIntegrationImpl implements AvailabilityZonesAndRegionsService {

    public List<Region> describeRegions(List<String> regions) {
        List<Region> listOfRegions = new ArrayList<Region>();
        listOfRegions.add(new Region("US_EAST", 1, "", ""));
        if (regions == null || regions.size() == 0) {
            listOfRegions.add(new Region("US_WEST", 2, "", ""));
            listOfRegions.add(new Region("UK", 3, "", ""));
        }
        return listOfRegions;
    }

    public List<AvailabilityZone> describeAvailabilityZones(List<String> availabilityZones) {
        List<AvailabilityZone> value = new ArrayList<AvailabilityZone>();
        value.add(new AvailabilityZone("zone1", 123, 1, "available"));
        value.add(new AvailabilityZone("zone2", 135, 2, "available"));
        return value;
    }
}
