package com.bt.pi.app.common.entities.util;

import org.apache.commons.lang.builder.ToStringBuilder;

public class ResourceAllocation {
    private long allocatedResource;
    private int allocatedStepSize;

    public ResourceAllocation(long anAllocatedResource, int anAllocatedStepSize) {
        super();
        this.allocatedResource = anAllocatedResource;
        this.allocatedStepSize = anAllocatedStepSize;
    }

    public long getAllocatedResource() {
        return allocatedResource;
    }

    public int getAllocatedStepSize() {
        return allocatedStepSize;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("resource", allocatedResource).append("step", allocatedStepSize).toString();
    }
}
