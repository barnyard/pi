package com.bt.pi.app.common.entities;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.bt.pi.app.common.util.CaseIgnoringMap;
import com.bt.pi.core.application.resource.DefaultDhtResourceWatchingStrategy;
import com.bt.pi.core.application.resource.watched.WatchedResource;
import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.GLOBAL)
@WatchedResource(watchingStrategy = DefaultDhtResourceWatchingStrategy.class, defaultInitialResourceRefreshIntervalMillis = AvailabilityZones.DEFAULT_INITIAL_INTERVAL_MILLIS, defaultRepeatingResourceRefreshIntervalMillis = AvailabilityZones.DEFAULT_REPEATING_INTERVAL_MILLIS, initialResourceRefreshIntervalMillisProperty = AvailabilityZones.INITIAL_INTERVAL_MILLIS_PROPERTY, repeatingResourceRefreshIntervalMillisProperty = AvailabilityZones.REPEATING_INTERVAL_MILLIS_PROPERTY)
public class Regions extends PiEntityBase {
    public static final String URL = String.format("%s:all", ResourceSchemes.REGIONS);
    private Map<String, Region> regions;

    public Regions() {
        regions = new CaseIgnoringMap<Region>(new HashMap<String, Region>());
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return URL;
    }

    public Map<String, Region> getRegions() {
        return regions;
    }

    public void setAvailabilityZones(Map<String, Region> aRegions) {
        this.regions = aRegions;
    }

    public Region getRegion(String regionName) {
        return regions.get(regionName);
    }

    public void addRegion(Region aRegion) {
        checkNotDuplicated(aRegion);
        regions.put(aRegion.getRegionName(), aRegion);
    }

    private void checkNotDuplicated(Region aRegion) {
        for (Region region : regions.values()) {
            if (region.getRegionCode() == aRegion.getRegionCode())
                throw new DuplicateRegionException(String.format("Region with code %s already exists", aRegion.getRegionCode()));
            if (region.getRegionName().equalsIgnoreCase(aRegion.getRegionName()))
                throw new DuplicateRegionException(String.format("Region with name %s already exists", aRegion.getRegionName()));
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Regions))
            return false;
        Regions castOther = (Regions) other;
        return new EqualsBuilder().append(regions, castOther.regions).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(regions).toHashCode();
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.REGIONS.toString();
    }
}
