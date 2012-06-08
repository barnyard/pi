package com.bt.pi.app.common.entities.util;

public interface AllocatableResource<T extends Comparable<T>> {

    void setMostRecentlyAllocatedResource(T resourceIndex);

    T getMostRecentlyAllocatedResource();
}
