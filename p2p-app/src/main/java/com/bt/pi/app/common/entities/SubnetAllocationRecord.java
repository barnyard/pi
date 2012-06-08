package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

public class SubnetAllocationRecord extends HeartbeatTimestampEntityBase {
    private String securityGroupId;
    private Long subnetMask;

    public SubnetAllocationRecord() {
        securityGroupId = null;
        subnetMask = null;
    }

    public SubnetAllocationRecord(String aSecurityGroupId, Long aSubnetMask) {
        securityGroupId = aSecurityGroupId;
        subnetMask = aSubnetMask;
    }

    public String getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(String aSecurityGroupId) {
        this.securityGroupId = aSecurityGroupId;
    }

    public Long getSubnetMask() {
        return subnetMask;
    }

    public void setSubnetMask(Long aSubnetMask) {
        this.subnetMask = aSubnetMask;
    }

    @Override
    @JsonIgnore
    public boolean isConsumedBy(String consumerId) {
        return consumerId.equals(getSecurityGroupId());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("securityGroupId", securityGroupId).append("subnetMask", subnetMask).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof SubnetAllocationRecord))
            return false;
        SubnetAllocationRecord castOther = (SubnetAllocationRecord) other;
        return new EqualsBuilder().append(securityGroupId, castOther.securityGroupId).append(subnetMask, castOther.subnetMask).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(securityGroupId).append(subnetMask).toHashCode();
    }
}
