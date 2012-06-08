package com.bt.pi.app.imagemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.Continuation;
import rice.p2p.commonapi.Id;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.imagemanager.reporting.ImageReportEntity;
import com.bt.pi.app.imagemanager.reporting.ImageReportEntityCollection;
import com.bt.pi.core.application.storage.LocalStorageScanningHandlerBase;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

@Component
public class LocalStorageImageHandler extends LocalStorageScanningHandlerBase {

    private static final Log LOG = LogFactory.getLog(LocalStorageImageHandler.class);
    private static final String IMAGE_ENTITY_TYPE = new Image().getType();
    private static final long DISPATCH_DELAY_SECONDS = 60 * 5; // five mins
    private long dispatchDelaySeconds = DISPATCH_DELAY_SECONDS;

    private List<ImageReportEntity> imageReportEntities;

    @Resource
    private DhtClientFactory dhtClientFactory;

    @Resource
    private KoalaIdFactory koalaIdFactory;

    @Resource
    private ScheduledExecutorService scheduledExecutorService;

    public LocalStorageImageHandler() {
        dhtClientFactory = null;
        imageReportEntities = new ArrayList<ImageReportEntity>();
        koalaIdFactory = null;

    }

    @Override
    protected void doHandle(final Id id, KoalaGCPastMetadata metadata) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("doHandle(%s, %s)", id.toStringFull(), metadata));

        dhtClientFactory.createReader().getAsync(koalaIdFactory.convertToPId(id), new Continuation<Image, Exception>() {
            @Override
            public void receiveResult(Image result) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Received result: " + result);
                }
                synchronized (imageReportEntities) {
                    ImageReportEntity imageReportEntity = new ImageReportEntity(result);
                    if (!imageReportEntities.contains(imageReportEntity))
                        imageReportEntities.add(imageReportEntity);
                }
            }

            @Override
            public void receiveException(Exception exception) {
                LOG.warn("Exception while reading image with ID: " + koalaIdFactory.convertToPId(id).toStringFull(), exception);
            }

        });
    }

    protected String getEntityType() {
        return IMAGE_ENTITY_TYPE;
    }

    public void checkAndReportToSupernodes() {
        LOG.debug(String.format("checkAndDispatchToSupernode()"));
        synchronized (imageReportEntities) {
            if (imageReportEntities.isEmpty())
                return;
            try {
                ImageReportEntityCollection imageReportEntityCollection = new ImageReportEntityCollection();
                List<ImageReportEntity> copy = new ArrayList<ImageReportEntity>(imageReportEntities);
                imageReportEntityCollection.setEntities(copy);
                getReportingApplication().sendReportingUpdateToASuperNode(imageReportEntityCollection);
                imageReportEntities.clear();
            } catch (Throwable t) {
                LOG.error(t);
            }
        }
    }

    @Property(key = "local.storage.image.handler.report.seconds", defaultValue = "" + DISPATCH_DELAY_SECONDS)
    public void setDispatchDelayMillis(long aDispatchDelay) {
        dispatchDelaySeconds = aDispatchDelay;
    }

    protected List<ImageReportEntity> getImageReportEntities() {
        return imageReportEntities;
    }

    @PostConstruct
    public void scheduleReportToSupernodes() {
        LOG.debug(String.format("Scheduling local storage image handler report to run every %s seconds", dispatchDelaySeconds));
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                checkAndReportToSupernodes();
            }
        }, 0, dispatchDelaySeconds, TimeUnit.SECONDS);
    }
}
