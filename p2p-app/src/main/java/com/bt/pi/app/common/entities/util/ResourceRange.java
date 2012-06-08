package com.bt.pi.app.common.entities.util;

import java.util.Iterator;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ResourceRange implements Comparable<ResourceRange>, Iterable<Long> {
    private static final int HASH_MULTIPLE = 37;
    private static final int HASH_INITIAL = 17;
    private Long min;
    private Long max;
    private int allocationStepSize;

    public ResourceRange() {
        this(null, null);
    }

    public ResourceRange(Long aMin, Long aMax) {
        this(aMin, aMax, 1);
    }

    public ResourceRange(Long aMin, Long aMax, int anAllocationStepSize) {
        this.min = aMin;
        this.max = aMax;
        this.allocationStepSize = anAllocationStepSize;
    }

    public Long getMin() {
        return min;
    }

    public Long getMax() {
        return max;
    }

    public void setMin(Long aMin) {
        this.min = aMin;
    }

    public void setMax(Long aMax) {
        this.max = aMax;
    }

    public int getAllocationStepSize() {
        return allocationStepSize;
    }

    public void setAllocationStepSize(int anAllocationStepSize) {
        this.allocationStepSize = anAllocationStepSize;
    }

    @Override
    public String toString() {
        return "ResourceRange [min=" + min + ", max=" + max + "]";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).append(min).append(max).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ResourceRange))
            return false;
        ResourceRange other = (ResourceRange) obj;
        return new EqualsBuilder().append(min, other.min).isEquals();
    }

    @Override
    public int compareTo(ResourceRange other) {
        return min.compareTo(other.min);
    }

    @Override
    public Iterator<Long> iterator() {
        return new ResourceIterator();
    }

    public class ResourceIterator implements Iterator<Long> {
        private Long current;

        public ResourceIterator() {
            current = min.longValue();
        }

        @Override
        public boolean hasNext() {
            return current + allocationStepSize - 1 <= max;
        }

        @Override
        public Long next() {
            Long res = current;
            current = current + allocationStepSize;
            return res;
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not implemented");
        }

    }

}
