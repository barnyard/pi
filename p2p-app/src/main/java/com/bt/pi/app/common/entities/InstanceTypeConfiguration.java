package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class InstanceTypeConfiguration {
    private String instanceType;
    private int numCores;
    private int memorySizeInMB;
    private int diskSizeInGB;
    private boolean deprecated;

    public InstanceTypeConfiguration() {
        this(null, 0, 0, 0);
    }

    public InstanceTypeConfiguration(String aInstanceType, int aNumCores, int aMemorySizeInMB, int aDiskSizeInGB) {
        setInstanceType(aInstanceType);
        setNumCores(aNumCores);
        setMemorySizeInMB(aMemorySizeInMB);
        setDiskSizeInGB(aDiskSizeInGB);
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String aInstanceType) {
        this.instanceType = aInstanceType;
    }

    public int getNumCores() {
        return numCores;
    }

    public void setNumCores(int aNumCores) {
        this.numCores = aNumCores;
    }

    public int getMemorySizeInMB() {
        return memorySizeInMB;
    }

    public void setMemorySizeInMB(int aMemorySizeInMB) {
        this.memorySizeInMB = aMemorySizeInMB;
    }

    public int getDiskSizeInGB() {
        return diskSizeInGB;
    }

    public void setDiskSizeInGB(int aDiskSizeInGB) {
        this.diskSizeInGB = aDiskSizeInGB;
    }

    public boolean isDeprecated() {
        return this.deprecated;
    }

    public void setDeprecated(boolean b) {
        this.deprecated = b;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof InstanceTypeConfiguration))
            return false;
        InstanceTypeConfiguration castOther = (InstanceTypeConfiguration) other;
        return new EqualsBuilder().append(instanceType, castOther.instanceType).append(numCores, castOther.numCores).append(memorySizeInMB, castOther.memorySizeInMB).append(diskSizeInGB, castOther.diskSizeInGB)
                .append(deprecated, castOther.deprecated).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(instanceType).append(numCores).append(memorySizeInMB).append(diskSizeInGB).append(deprecated).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("instanceType", instanceType).append("numCores", numCores).append("memorySizeInMB", memorySizeInMB).append("diskSizeInGB", diskSizeInGB).append("depecated", deprecated)
                .toString();
    }
}
