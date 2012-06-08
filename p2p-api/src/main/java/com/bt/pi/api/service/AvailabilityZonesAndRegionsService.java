/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.List;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.Region;

/**
 * Interface for API calls concerning availability zones and regions
 * - Describe Availability Zones
 * - Describe Regions
 *
 */
public interface AvailabilityZonesAndRegionsService {

	List<AvailabilityZone> describeAvailabilityZones(List<String> availabilityZones);
	
	List<Region> describeRegions(List<String> regions);
	
}
