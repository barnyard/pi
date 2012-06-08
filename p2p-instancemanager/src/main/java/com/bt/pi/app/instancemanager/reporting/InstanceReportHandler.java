package com.bt.pi.app.instancemanager.reporting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.reporter.EhCacheReportableEntityStoreBase;
import com.bt.pi.core.application.reporter.ReportHandler;
import com.bt.pi.core.application.reporter.ReportableEntityStore;
import com.bt.pi.core.application.reporter.TimeBasedReportableEntityStore;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.entity.PiEntityCollection;

@Component
public class InstanceReportHandler extends ReportHandler<InstanceReportEntity> {
    private static final String DEFAULT_TIME_TO_LIVE_SECS = "3600";
    private static final String DEFAULT_INSTANCE_PUBLISH_DELAY_SECONDS = "900";
    private static final Log LOG = LogFactory.getLog(InstanceReportHandler.class);

    public InstanceReportHandler() {
        super(Arrays.asList(new String[] { new InstanceReportEntityCollection().getType() }));
    }

    @SuppressWarnings("unchecked")
    @Property(key = "instance.report.store.time.to.live.secs", defaultValue = DEFAULT_TIME_TO_LIVE_SECS)
    public void setTimeToLive(int value) {
        ReportableEntityStore<InstanceReportEntity> existingReportableEntityStore = getReportableEntityStore();

        Collection<InstanceReportEntity> allEntities = new ArrayList<InstanceReportEntity>();
        if (existingReportableEntityStore != null)
            allEntities.addAll(existingReportableEntityStore.getAllEntities());

        if (existingReportableEntityStore instanceof EhCacheReportableEntityStoreBase<?>)
            ((EhCacheReportableEntityStoreBase<InstanceReportEntity>) existingReportableEntityStore).removeCache();

        TimeBasedReportableEntityStore<InstanceReportEntity> newReportableEntityStore = new TimeBasedReportableEntityStore<InstanceReportEntity>("instance.report.store" + UUID.randomUUID().toString(), value, value);
        newReportableEntityStore.addAll(allEntities);
        setReportableEntityStore(newReportableEntityStore);
    }

    @Property(key = "instance.report.broadcastwindowsize", defaultValue = DEFAULT_BROADCAST_WINDOW)
    @Override
    public void setBroadcastWindowSize(int windowSize) {
        super.setBroadcastWindowSize(windowSize);
    }

    @Property(key = "instance.report.publishintervalsize", defaultValue = DEFAULT_INSTANCE_PUBLISH_DELAY_SECONDS)
    @Override
    protected void setPublishIntervalSeconds(int aPublishIntervalSeconds) {
        LOG.debug(String.format("setPublishIntervalSeconds(%d)", aPublishIntervalSeconds));
        super.setPublishIntervalSeconds(aPublishIntervalSeconds);
    }

    @Override
    protected PiEntityCollection<InstanceReportEntity> getPiEntityCollection() {
        return new InstanceReportEntityCollection();
    }
}
