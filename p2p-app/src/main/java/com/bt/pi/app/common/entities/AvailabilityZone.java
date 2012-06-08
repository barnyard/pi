/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.bt.pi.core.id.KoalaIdFactory;

public class AvailabilityZone {
    private static final int HASH_MULTIPLE = 19;
    private static final int HASH_INITIAL = 173;
    private static final String AVAILABILITY_ZONE_CODE = "availabilityZoneCode";

    private String availabilityZoneName;
    private int availabilityZoneCode;
    private int regionCode;
    private String status;

    public AvailabilityZone() {
    }

    public AvailabilityZone(String anAvailabilityZoneName, int anAvailabilityZoneCodeWithinRegion, int aRegionCode, String aStatus) {
        super();

        setAvailabilityZoneName(anAvailabilityZoneName);
        setAvailabilityZoneCodeWithinRegion(anAvailabilityZoneCodeWithinRegion);
        setRegionCode(aRegionCode);
        setStatus(aStatus);
    }

    public String getAvailabilityZoneName() {
        return availabilityZoneName;
    }

    public void setAvailabilityZoneName(String anAvailabilityZoneName) {
        this.availabilityZoneName = anAvailabilityZoneName;
    }

    @JsonProperty(AVAILABILITY_ZONE_CODE)
    public int getAvailabilityZoneCodeWithinRegion() {
        return availabilityZoneCode;
    }

    @JsonProperty(AVAILABILITY_ZONE_CODE)
    public void setAvailabilityZoneCodeWithinRegion(int anAvailabilityZoneCode) {
        if (anAvailabilityZoneCode < 0 || anAvailabilityZoneCode > (int) Math.pow(2, KoalaIdFactory.AVAILABILITY_ZONE_CODE_WITHIN_REGION_SIZE_BITS) - 1)
            throw new IllegalArgumentException("Availability zone code can only be a positive number and 8 bits long");

        this.availabilityZoneCode = anAvailabilityZoneCode;
    }

    @JsonIgnore
    public int getGlobalAvailabilityZoneCode() {
        return regionCode * (int) Math.pow(2, KoalaIdFactory.AVAILABILITY_ZONE_CODE_WITHIN_REGION_SIZE_BITS) + availabilityZoneCode;
    }

    public int getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(int aRegionCode) {
        if (aRegionCode < 0 || aRegionCode > (int) Math.pow(2, KoalaIdFactory.AVAILABILITY_ZONE_CODE_WITHIN_REGION_SIZE_BITS) - 1)
            throw new IllegalArgumentException("Region code can only be a positive number and 8 bits long");
        this.regionCode = aRegionCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String aStatus) {
        this.status = aStatus;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).append(availabilityZoneName).append(availabilityZoneCode).append(regionCode).append(status).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AvailabilityZone other = (AvailabilityZone) obj;
        return new EqualsBuilder().append(availabilityZoneName, other.availabilityZoneName).append(availabilityZoneCode, other.availabilityZoneCode).append(regionCode, other.regionCode).append(status, other.status).isEquals();
    }
}
