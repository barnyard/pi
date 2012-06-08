/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonProperty;

import com.bt.pi.core.entity.PiEntityBase;

public abstract class VolumeBase extends PiEntityBase {

    private static final int HASH_MULTIPLE = 37;
    private static final int HASH_INITIAL = 17;
    private String availabilityZone;
    private String ownerId;
    private long createTime;
    private String volumeId;
    private String snapshotId;
    private int regionCode;
    private int availabilityZoneCode;

    @JsonProperty
    private long statusTimestamp;

    public VolumeBase() {
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String aSnapshotId) {
        this.snapshotId = aSnapshotId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String aVolumeId) {
        this.volumeId = aVolumeId;
    }

    public void setOwnerId(String anOwnerId) {
        ownerId = anOwnerId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long aCreateTime) {
        this.createTime = aCreateTime;
    }

    public void setAvailabilityZone(String anAvailabilityZone) {
        availabilityZone = anAvailabilityZone;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setStatusTimestamp(long aStatusTimestamp) {
        this.statusTimestamp = aStatusTimestamp;
    }

    public long getStatusTimestamp() {
        return this.statusTimestamp;
    }

    /**
     * @return the regionCode
     */
    public int getRegionCode() {
        return regionCode;
    }

    /**
     * @param regionCode
     *            the regionCode to set
     */
    public void setRegionCode(int aCode) {
        this.regionCode = aCode;
    }

    /**
     * @return the availabilityZoneCode
     */
    public int getAvailabilityZoneCode() {
        return availabilityZoneCode;
    }

    /**
     * @param availabilityZoneCode
     *            the availabilityZoneCode to set
     */
    public void setAvailabilityZoneCode(int aCode) {
        this.availabilityZoneCode = aCode;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other)
            return true;
        if (!(other instanceof VolumeBase))
            return false;
        VolumeBase castOther = (VolumeBase) other;
        return new EqualsBuilder().append(availabilityZone, castOther.availabilityZone).append(ownerId, castOther.ownerId).append(createTime, castOther.createTime).append(volumeId, castOther.volumeId).append(snapshotId, castOther.snapshotId)
                .append(statusTimestamp, castOther.statusTimestamp).append(regionCode, castOther.regionCode).append(availabilityZoneCode, castOther.availabilityZoneCode).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).append(availabilityZone).append(ownerId).append(createTime).append(volumeId).append(snapshotId).append(statusTimestamp).append(regionCode).append(availabilityZoneCode).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("availabilityZone", availabilityZone).append("ownerId", ownerId).append("createTime", createTime).append("volumeId", volumeId).append("snapshotId", snapshotId)
                .append("statusTimestamp", statusTimestamp).append("regionCode", regionCode).append("availabilityZoneCode", availabilityZoneCode).toString();
    }

    public abstract boolean isDeleted();
}
