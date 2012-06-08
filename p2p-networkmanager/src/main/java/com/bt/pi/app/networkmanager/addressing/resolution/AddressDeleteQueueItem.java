package com.bt.pi.app.networkmanager.addressing.resolution;

public abstract class AddressDeleteQueueItem implements Comparable<AddressDeleteQueueItem> {
    private int priority;

    public AddressDeleteQueueItem() {
    }

    protected AddressDeleteQueueItem(int aPriority) {
        this.priority = aPriority;
    }

    public synchronized void incrementPriority() {
        priority++;
    }

    public synchronized int getPriority() {
        return priority;
    }

    public abstract void delete();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    @Override
    public int compareTo(AddressDeleteQueueItem o) {
        if (o.equals(this))
            return 0;

        int difference = o.getPriority() - getPriority();
        if (difference != 0)
            return difference;

        if (this instanceof AddressDeleteQueueThresholdItem)
            return -1;

        return 1;
    }
}
