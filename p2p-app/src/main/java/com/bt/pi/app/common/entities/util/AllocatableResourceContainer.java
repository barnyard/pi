package com.bt.pi.app.common.entities.util;

public interface AllocatableResourceContainer {

    void setMostRecentlyAllocatedResource(Long mostRecentlyAllocatedResource);

    Long getMostRecentlyAllocatedResource();
}
