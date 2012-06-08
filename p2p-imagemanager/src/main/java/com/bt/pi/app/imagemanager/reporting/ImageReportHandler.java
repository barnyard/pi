package com.bt.pi.app.imagemanager.reporting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.instancemanager.reporting.InstanceReportEntity;
import com.bt.pi.core.application.reporter.EhCacheReportableEntityStoreBase;
import com.bt.pi.core.application.reporter.ReportHandler;
import com.bt.pi.core.application.reporter.ReportableEntityStore;
import com.bt.pi.core.application.reporter.TimeBasedSizeBoundReportableEntityStore;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.entity.PiEntityCollection;

@Component
public class ImageReportHandler extends ReportHandler<ImageReportEntity> {
    private static final Log LOG = LogFactory.getLog(ImageReportHandler.class);
    private static final String DEFAULT_SIZE = "1000";
    private static final String DEFAULT_TIME_TO_LIVE_SECS = "18000"; // 5 hours in seconds
    private static final String DEFAULT_IMAGE_PUBLISH_DELAY_SECONDS = "900";
    private int timeToLive;
    private int size;

    public ImageReportHandler() {
        super(Arrays.asList(new String[] { new ImageReportEntityCollection().getType() }));
    }

    @Override
    protected PiEntityCollection<ImageReportEntity> getPiEntityCollection() {
        return new ImageReportEntityCollection();
    }

    @Property(key = "image.report.store.time.to.live.secs", defaultValue = DEFAULT_TIME_TO_LIVE_SECS)
    public void setTimeToLive(int value) {
        timeToLive = value;
        updateReportableEntityStore();
    }

    @Property(key = "image.report.store.size", defaultValue = DEFAULT_SIZE)
    public void setSize(int value) {
        size = value;
        updateReportableEntityStore();
    }

    @Property(key = "image.report.publishintervalsize", defaultValue = DEFAULT_IMAGE_PUBLISH_DELAY_SECONDS)
    @Override
    protected void setPublishIntervalSeconds(int aPublishIntervalSeconds) {
        LOG.debug(String.format("setPublishIntervalSeconds(%d)", aPublishIntervalSeconds));
        super.setPublishIntervalSeconds(aPublishIntervalSeconds);
    }

    @SuppressWarnings("unchecked")
    protected void updateReportableEntityStore() {
        LOG.info("updateReportableEntityStore()");
        ReportableEntityStore<ImageReportEntity> existingReportableEntityStore = getReportableEntityStore();
        Collection<ImageReportEntity> allEntities = new ArrayList<ImageReportEntity>();
        if (existingReportableEntityStore != null)
            allEntities.addAll(existingReportableEntityStore.getAllEntities());

        if (existingReportableEntityStore instanceof EhCacheReportableEntityStoreBase<?>)
            ((EhCacheReportableEntityStoreBase<InstanceReportEntity>) existingReportableEntityStore).removeCache();
        LOG.debug("Creating new reportable entity store");
        TimeBasedSizeBoundReportableEntityStore<ImageReportEntity> newReportableEntityStore = new TimeBasedSizeBoundReportableEntityStore<ImageReportEntity>("image.report.store" + UUID.randomUUID().toString(), timeToLive, timeToLive, size);
        newReportableEntityStore.addAll(allEntities);
        setReportableEntityStore(newReportableEntityStore);
    }

    @Override
    public PiEntityCollection<ImageReportEntity> getEntities(PiEntityCollection piEntityCollection) {
        ImageReportEntityCollection collection = (ImageReportEntityCollection) piEntityCollection;
        Collection<ImageReportEntity> allEntities = getReportableEntityStore().getAllEntities();
        allEntities.retainAll(collection.getEntities());
        collection.setEntities(allEntities);
        return collection;
    }

}
