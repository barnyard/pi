/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.bt.pi.core.entity.Deletable;

public class Snapshot extends VolumeBase implements Deletable {
    public static final int BURIED_TIME = 6 * 60 * 60 * 1000;
    public static final String SCHEME = "snap";
    private static final int TEN = 10;
    private static final int HASH_MULTIPLE = 31;
    private static final int HASH_INITIAL = 7;

    @JsonProperty
    private SnapshotState status;
    private long startTime;
    private double progress;
    private String description;

    public Snapshot() {
    }

    public Snapshot(String aSnapshotId, String aVolumeId, SnapshotState aStatus, long aStartTime, double aProgress, String aDescription, String anOwnerId) {
        startTime = aStartTime;
        progress = aProgress;
        description = aDescription;
        setSnapshotId(aSnapshotId);
        setVolumeId(aVolumeId);
        setStatus(aStatus);
        setOwnerId(anOwnerId);
    }

    // This is jsonignored since otherwise the status timestamp gets set when deserializing.
    // We annotate status with @JsonProperty above instead.
    @JsonIgnore
    public void setStatus(SnapshotState aStatus) {
        this.status = aStatus;
        setStatusTimestamp(System.currentTimeMillis());
    }

    public SnapshotState getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String aDescription) {
        this.description = aDescription;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double aProgress) {
        this.progress = aProgress;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long aStartTime) {
        this.startTime = aStartTime;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return Snapshot.getUrl(getSnapshotId());
    }

    public static String getUrl(String entityKey) {
        return String.format(SCHEME + ":%s", entityKey);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Snapshot))
            return false;
        Snapshot castOther = (Snapshot) other;
        return new EqualsBuilder().appendSuper(super.equals(other)).append(status, castOther.status).append(startTime, castOther.startTime).append(progress, castOther.progress).append(description, castOther.description).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).appendSuper(super.hashCode()).append(status).append(startTime).append(progress).append(description).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("status", status).append("startTime", startTime).append("progress", progress).append("description", description).toString();
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }

    @Override
    @JsonIgnore
    public boolean isDeleted() {
        return this.getStatus().equals(SnapshotState.BURIED) || (SnapshotState.DELETED.equals(status) && (getStatusTimestamp() + (TEN * BURIED_TIME)) < System.currentTimeMillis());
    }

    @Override
    @JsonIgnore
    public void setDeleted(boolean b) {
        if (b)
            setStatus(SnapshotState.BURIED);

    }
}
