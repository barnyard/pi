package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

public class VlanAllocationRecord extends HeartbeatTimestampEntityBase {
    private String securityGroupId;

    public VlanAllocationRecord() {
        securityGroupId = null;
    }

    public VlanAllocationRecord(String aSecurityGroupId) {
        this.securityGroupId = aSecurityGroupId;
    }

    public String getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(String aSecurityGroupId) {
        this.securityGroupId = aSecurityGroupId;
    }

    @Override
    @JsonIgnore
    public boolean isConsumedBy(String consumerId) {
        return consumerId.equals(getSecurityGroupId());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("securityGroupId", securityGroupId).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof VlanAllocationRecord))
            return false;
        VlanAllocationRecord castOther = (VlanAllocationRecord) other;
        return new EqualsBuilder().append(securityGroupId, castOther.securityGroupId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(securityGroupId).toHashCode();
    }
}
