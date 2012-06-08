package com.bt.pi.app.instancemanager.reporting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.bt.pi.core.application.reporter.EhCacheReportableEntityStoreBase;
import com.bt.pi.core.application.reporter.ReportHandler;
import com.bt.pi.core.application.reporter.ReportableEntityStore;
import com.bt.pi.core.application.reporter.TimeBasedSizeBoundReportableEntityStore;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.entity.PiEntityCollection;

@Component
public class ZombieInstanceReportHandler extends ReportHandler<InstanceReportEntity> {
    private static final String DEFAULT_SIZE = "1000";
    private static final String DEFAULT_TIME_TO_LIVE_SECS = "18000"; // 5 hours in seconds
    private static final String DEFAULT_INSTANCE_PUBLISH_DELAY_SECONDS = "900";

    private int timeToLive;
    private int size;

    public ZombieInstanceReportHandler() {
        super(Arrays.asList(new String[] { new ZombieInstanceReportEntityCollection().getType() }));

        timeToLive = Integer.parseInt(DEFAULT_TIME_TO_LIVE_SECS);
        size = Integer.parseInt(DEFAULT_SIZE);
    }

    @Property(key = "zombie.instance.report.store.time.to.live.secs", defaultValue = DEFAULT_TIME_TO_LIVE_SECS)
    public void setTimeToLive(int value) {
        timeToLive = value;
        updateReportableEntityStore();
    }

    @Property(key = "zombie.instance.report.store.size", defaultValue = DEFAULT_SIZE)
    public void setSize(int value) {
        size = value;
        updateReportableEntityStore();
    }

    @SuppressWarnings("unchecked")
    private void updateReportableEntityStore() {
        ReportableEntityStore<InstanceReportEntity> existingReportableEntityStore = getReportableEntityStore();
        Collection<InstanceReportEntity> allEntities = new ArrayList<InstanceReportEntity>();
        if (existingReportableEntityStore != null)
            allEntities.addAll(existingReportableEntityStore.getAllEntities());

        if (existingReportableEntityStore instanceof EhCacheReportableEntityStoreBase<?>)
            ((EhCacheReportableEntityStoreBase<InstanceReportEntity>) existingReportableEntityStore).removeCache();

        TimeBasedSizeBoundReportableEntityStore<InstanceReportEntity> newReportableEntityStore = new TimeBasedSizeBoundReportableEntityStore<InstanceReportEntity>("zombie.instance.report.store", timeToLive, timeToLive, size);
        newReportableEntityStore.addAll(allEntities);
        setReportableEntityStore(newReportableEntityStore);
    }

    @Property(key = "zombie.instance.report.broadcastwindowsize", defaultValue = DEFAULT_BROADCAST_WINDOW)
    @Override
    public void setBroadcastWindowSize(int windowSize) {
        super.setBroadcastWindowSize(windowSize);
    }

    @Property(key = "zombie.instance.report.publishintervalsize", defaultValue = DEFAULT_INSTANCE_PUBLISH_DELAY_SECONDS)
    @Override
    protected void setPublishIntervalSeconds(int aPublishIntervalSeconds) {
        super.setPublishIntervalSeconds(aPublishIntervalSeconds);
    }

    @Override
    protected PiEntityCollection<InstanceReportEntity> getPiEntityCollection() {
        return new ZombieInstanceReportEntityCollection();
    }
}
