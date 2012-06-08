package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Region {
    private static final int ZERO = 0;
    private static final int TWO_FIVE_FIVE = 255;

    private String regionName;
    private int regionCode;
    private String regionEndpoint;
    private String pisssEndpoint;

    public Region() {
    }

    public Region(String aRegionName, int aRegionCode, String aRegionEndpoint, String aPisssEndpoint) {
        setRegionName(aRegionName);
        setRegionCode(aRegionCode);
        setRegionEndpoint(aRegionEndpoint);
        setPisssEndpoint(aPisssEndpoint);
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String aRegionName) {
        this.regionName = aRegionName;
    }

    public int getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(int aRegionCode) {
        if (aRegionCode < ZERO || aRegionCode > TWO_FIVE_FIVE)
            throw new IllegalArgumentException("Code can only be a positive number and 8 bits long");

        this.regionCode = aRegionCode;
    }

    public String getRegionEndpoint() {
        return regionEndpoint;
    }

    public void setRegionEndpoint(String aRegionEndpoint) {
        this.regionEndpoint = aRegionEndpoint;
    }

    public String getPisssEndpoint() {
        return pisssEndpoint;
    }

    public void setPisssEndpoint(String aPisssEndpoint) {
        this.pisssEndpoint = aPisssEndpoint;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Region))
            return false;
        Region castOther = (Region) other;
        return new EqualsBuilder().append(regionName, castOther.regionName).append(regionCode, castOther.regionCode).append(regionEndpoint, castOther.regionEndpoint).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(regionName).append(regionCode).append(regionEndpoint).toHashCode();
    }
}
