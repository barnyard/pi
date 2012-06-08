/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZoneNotFoundException;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;

@Component
public class AvailabilityZonesAndRegionsServiceImpl extends ServiceBaseImpl implements AvailabilityZonesAndRegionsService {
    private static final Log LOG = LogFactory.getLog(AvailabilityZonesAndRegionsServiceImpl.class);

    public AvailabilityZonesAndRegionsServiceImpl() {
    }

    public List<AvailabilityZone> describeAvailabilityZones(List<String> availabilityZoneNames) {
        List<AvailabilityZone> result = new ArrayList<AvailabilityZone>();
        AvailabilityZones availabilityZones = getApiApplicationManager().getAvailabilityZonesRecord();
        if (availabilityZoneNames == null || availabilityZoneNames.size() == 0)
            result.addAll(availabilityZones.getAvailabilityZones().values());
        else {
            for (String availabilityZoneName : availabilityZoneNames) {
                AvailabilityZone availabilityZone = null;
                try {
                    availabilityZone = availabilityZones.getAvailabilityZoneByName(availabilityZoneName);
                } catch (AvailabilityZoneNotFoundException e) {
                    LOG.debug(String.format("Availability zone %s did not exist", availabilityZoneName));
                }
                if (availabilityZone != null)
                    result.add(availabilityZone);
            }
        }
        return result;
    }

    public List<Region> describeRegions(List<String> regionNames) {
        List<Region> result = new ArrayList<Region>();
        Regions regions = getApiApplicationManager().getRegions();
        if (regionNames == null || regionNames.size() == 0)
            result.addAll(regions.getRegions().values());
        else {
            for (String regionName : regionNames) {
                Region region = regions.getRegion(regionName);
                if (region != null)
                    result.add(region);
            }
        }
        return result;
    }

}
