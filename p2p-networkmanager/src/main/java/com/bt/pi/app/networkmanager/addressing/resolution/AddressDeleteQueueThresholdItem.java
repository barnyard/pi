package com.bt.pi.app.networkmanager.addressing.resolution;

public class AddressDeleteQueueThresholdItem extends AddressDeleteQueueItem {
    public AddressDeleteQueueThresholdItem(int aPriority) {
        super(aPriority);
    }

    @Override
    public void delete() {
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return String.format("threshold queue item, priority: %d", getPriority());
    }
}
