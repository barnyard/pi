package com.bt.pi.app.networkmanager.addressing.resolution;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;

@Component
public class AddressDeleteQueue {
    private static final int SKIP_PRIORITY_LEVEL = 10;
    private static final Log LOG = LogFactory.getLog(AddressDeleteQueue.class);
    private static final String ADDRESS_DELETE_SCHEDULER_INTERVAL_SECONDS = "address.delete.scheduler.interval.seconds";
    private static final String ONE_MINUTE = "60";
    private static final String ADDRESS_DELETE_PRIORITY_THRESHOLD = "address.delete.priority.threshold";
    private static final String DEFAULT_PRIORITY_THRESHOLD = "5";
    private static final String ADDRESS_DELETE_PRIORITY_BATCH_SIZE = "address.delete.priority.batch.size";
    private static final String DEFAULT_PRIORITY_BATCH_SIZE = "50";

    private AtomicInteger priorityThreshold;
    private AtomicInteger batchSize;
    private PriorityBlockingQueue<AddressDeleteQueueItem> deleteQueue;
    private ConcurrentHashMap<AddressDeleteQueueItem, AddressDeleteQueueItem> addressDeleteQueueHash;
    private ScheduledExecutorService scheduledExecutorService;
    private long addressDeleteQueueIntervalSeconds;
    private ScheduledFuture<?> scheduleWithFixedDelay;

    public AddressDeleteQueue() {
        addressDeleteQueueIntervalSeconds = Integer.parseInt(ONE_MINUTE);
        deleteQueue = new PriorityBlockingQueue<AddressDeleteQueueItem>();
        addressDeleteQueueHash = new ConcurrentHashMap<AddressDeleteQueueItem, AddressDeleteQueueItem>();
        priorityThreshold = new AtomicInteger(0);
        batchSize = new AtomicInteger(Integer.parseInt(DEFAULT_PRIORITY_BATCH_SIZE));
        scheduledExecutorService = null;
        scheduleWithFixedDelay = null;
    }

    @Resource
    public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
        scheduledExecutorService = aScheduledExecutorService;
    }

    Collection<AddressDeleteQueueItem> getDeleteQueue() {
        return deleteQueue;
    }

    @Property(key = ADDRESS_DELETE_PRIORITY_THRESHOLD, defaultValue = DEFAULT_PRIORITY_THRESHOLD)
    public void setPriorityThreshold(int value) {
        priorityThreshold.set(value);
    }

    @Property(key = ADDRESS_DELETE_PRIORITY_BATCH_SIZE, defaultValue = DEFAULT_PRIORITY_BATCH_SIZE)
    public void setBatchSize(int value) {
        batchSize.set(value);
    }

    @Property(key = ADDRESS_DELETE_SCHEDULER_INTERVAL_SECONDS, defaultValue = ONE_MINUTE)
    public void setAddressDeleteSchedulerIntervalSeconds(int value) {
        addressDeleteQueueIntervalSeconds = value;
    }

    public int getPriorityThreshold() {
        return priorityThreshold.get();
    }

    @PostConstruct
    public void startAddressDeleteScheduledTask() {
        LOG.debug(String.format("Scheduling address delete thread to run every %d seconds", addressDeleteQueueIntervalSeconds));
        scheduleWithFixedDelay = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                deleteAddressItems();
            }
        }, 0, addressDeleteQueueIntervalSeconds, TimeUnit.SECONDS);
    }

    public void removeAllAddressesInQueueOnShuttingDown() {
        LOG.debug("Removing all addresses in queue on application shutting down");
        if (null != scheduleWithFixedDelay)
            scheduleWithFixedDelay.cancel(true);

        AddressDeleteQueueItem queueItem = deleteQueue.peek();
        if (null != queueItem) {
            setBatchSize(deleteQueue.size());
            setPriorityThreshold(queueItem.getPriority() - SKIP_PRIORITY_LEVEL);
            LOG.debug(String.format("Batch size: %d and Priority threshold: %d", batchSize.get(), priorityThreshold.get()));
            deleteAddressItems();
        }
    }

    public boolean add(AddressDeleteQueueItem e) {
        LOG.debug(String.format("add(%s)", e));
        boolean added = false;
        if (addressDeleteQueueHash.containsKey(e)) {
            AddressDeleteQueueItem address = addressDeleteQueueHash.get(e);
            LOG.trace(String.format("removing %s to increment and readd. Remove result: %s", e, deleteQueue.remove(address)));
            if (address != null) {
                address.incrementPriority();
                LOG.trace("Readding address: " + address);
                added = deleteQueue.add(address);
            }
        } else {
            LOG.trace("Adding " + e + " to hash and queue.");
            AddressDeleteQueueItem address = addressDeleteQueueHash.putIfAbsent(e, e);
            added = deleteQueue.add(address == null ? e : address);
        }
        LOG.trace("Adding successful: " + added);
        return added;
    }

    public void deleteAddressItems() {
        LOG.debug("Checking for AddressItems to delete. Current priorityThreshold: " + priorityThreshold.get());
        boolean lastPriorityExceedsLimit = true;
        for (int i = 0; i < batchSize.get() && lastPriorityExceedsLimit; i++) {
            AddressDeleteQueueItem item = deleteQueue.peek();
            if (item != null) {
                lastPriorityExceedsLimit = checkAddressAndDeleteIfNecessary(item);
            } else {
                LOG.trace("Queue is currenlty empty.");
                lastPriorityExceedsLimit = false;
            }
        }
    }

    private boolean checkAddressAndDeleteIfNecessary(AddressDeleteQueueItem item) {
        boolean lastPriorityExceedsLimit;
        lastPriorityExceedsLimit = item.getPriority() > priorityThreshold.get();
        if (lastPriorityExceedsLimit) {
            LOG.trace(String.format("Delele queue contains an item %s above the specified priority %s.", deleteQueue.peek(), priorityThreshold));
            LOG.trace(String.format("Removing item %s successful %s.", item, addressDeleteQueueHash.remove(item)));
            deleteQueue.remove(item);
            try {
                item.delete();
            } catch (Throwable t) {
                LOG.warn(String.format("Failed to delete address %s", item));
            }

        } else
            LOG.trace("No items over specified priority");
        return lastPriorityExceedsLimit;
    }
}
