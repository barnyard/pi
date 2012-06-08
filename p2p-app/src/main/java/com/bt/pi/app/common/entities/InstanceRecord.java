package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

public class InstanceRecord extends HeartbeatTimestampEntityBase {
    private String instanceId;
    private String ownerId;
    private long createdAt;
    private long updated;

    public InstanceRecord() {
        instanceId = null;
        ownerId = null;
        createdAt = System.currentTimeMillis();
        updated = createdAt;
    }

    public InstanceRecord(String instanceIdentifier, String ownerIdentifier) {
        this();
        instanceId = instanceIdentifier;
        ownerId = ownerIdentifier;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceidentifier) {
        this.instanceId = instanceidentifier;
    }

    @Override
    @JsonIgnore
    public boolean isConsumedBy(String consumerId) {
        if (consumerId == null)
            return instanceId == null && ownerId == null;

        if (consumerId.startsWith("i-"))
            return consumerId.equals(instanceId);
        else
            return consumerId.equals(ownerId);
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String anOwnerId) {
        this.ownerId = anOwnerId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long timestamp) {
        this.createdAt = timestamp;
    }

    public long getUpdatedAt() {
        return updated;
    }

    public void setUpdatedAt(long timestamp) {
        this.updated = timestamp;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("instanceId", instanceId).append("ownerId", ownerId).append("createdAt", createdAt).append("updated", updated).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof InstanceRecord))
            return false;
        InstanceRecord castOther = (InstanceRecord) other;
        return new EqualsBuilder().append(instanceId, castOther.instanceId).append(ownerId, castOther.ownerId).append(createdAt, castOther.createdAt).append(updated, castOther.updated).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(instanceId).append(ownerId).append(createdAt).append(updated).toHashCode();
    }

}
