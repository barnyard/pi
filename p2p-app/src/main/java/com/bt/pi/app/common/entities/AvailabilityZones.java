package com.bt.pi.app.common.entities;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.bt.pi.core.application.resource.DefaultDhtResourceWatchingStrategy;
import com.bt.pi.core.application.resource.watched.WatchedResource;
import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.GLOBAL)
@WatchedResource(watchingStrategy = DefaultDhtResourceWatchingStrategy.class, defaultInitialResourceRefreshIntervalMillis = AvailabilityZones.DEFAULT_INITIAL_INTERVAL_MILLIS, defaultRepeatingResourceRefreshIntervalMillis = AvailabilityZones.DEFAULT_REPEATING_INTERVAL_MILLIS, initialResourceRefreshIntervalMillisProperty = AvailabilityZones.INITIAL_INTERVAL_MILLIS_PROPERTY, repeatingResourceRefreshIntervalMillisProperty = AvailabilityZones.REPEATING_INTERVAL_MILLIS_PROPERTY)
public class AvailabilityZones extends PiEntityBase {
    public static final String URL = String.format("%s:all", ResourceSchemes.AVAILABILITY_ZONES);
    protected static final String INITIAL_INTERVAL_MILLIS_PROPERTY = "availabilityzones.subscribe.initial.wait.time.millis";
    protected static final String REPEATING_INTERVAL_MILLIS_PROPERTY = "availabilityzones..subscribe.interval.millis";
    protected static final long DEFAULT_INITIAL_INTERVAL_MILLIS = 10000;
    protected static final long DEFAULT_REPEATING_INTERVAL_MILLIS = 86400000;
    private Map<String, AvailabilityZone> availabilityZones;

    public AvailabilityZones() {
        availabilityZones = new HashMap<String, AvailabilityZone>();
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return URL;
    }

    public Map<String, AvailabilityZone> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Map<String, AvailabilityZone> aAvailabilityZones) {
        this.availabilityZones = aAvailabilityZones;
    }

    public AvailabilityZone getAvailabilityZoneByName(String availabilityZoneName) {
        AvailabilityZone res = availabilityZones.get(availabilityZoneName);
        if (res == null)
            throw new AvailabilityZoneNotFoundException(String.format("Availability zone not found for name %s", availabilityZoneName));
        return res;
    }

    public AvailabilityZone getAvailabilityZoneByGlobalAvailabilityZoneCode(int globalAvailabiltyZoneCode) {
        for (AvailabilityZone currentAvailabilityZone : availabilityZones.values()) {
            if (globalAvailabiltyZoneCode == currentAvailabilityZone.getGlobalAvailabilityZoneCode())
                return currentAvailabilityZone;
        }
        throw new AvailabilityZoneNotFoundException(String.format("Availability zone not found for code %s", globalAvailabiltyZoneCode));
    }

    public void addAvailabilityZone(AvailabilityZone availabilityZone) {
        checkNotDuplicated(availabilityZone);
        availabilityZones.put(availabilityZone.getAvailabilityZoneName(), availabilityZone);
    }

    private void checkNotDuplicated(AvailabilityZone aZone) {
        for (AvailabilityZone currentAvailabilityZone : availabilityZones.values()) {
            if (currentAvailabilityZone.getGlobalAvailabilityZoneCode() == aZone.getGlobalAvailabilityZoneCode()) {
                throw new DuplicateAvailabilityZoneException(String.format("Availability zone with unique zone code of %s (region suffix + avz code) already exists", currentAvailabilityZone.getGlobalAvailabilityZoneCode()));
            }
            if (currentAvailabilityZone.getAvailabilityZoneName().equalsIgnoreCase(aZone.getAvailabilityZoneName())) {
                throw new DuplicateAvailabilityZoneException(String.format("Availability zone with name %s already exists", aZone.getAvailabilityZoneName()));
            }
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AvailabilityZones))
            return false;
        AvailabilityZones castOther = (AvailabilityZones) other;
        return new EqualsBuilder().append(availabilityZones, castOther.availabilityZones).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(availabilityZones).toHashCode();
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.AVAILABILITY_ZONES.toString();
    }
}
