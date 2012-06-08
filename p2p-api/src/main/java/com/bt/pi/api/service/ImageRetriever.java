package com.bt.pi.api.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.imagemanager.reporting.ImageReportEntity;
import com.bt.pi.app.imagemanager.reporting.ImageReportEntityCollection;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@Component
public class ImageRetriever {
    private static final int BLOCKING_CONTINUATION_TIMEOUT_SECONDS = 30;

    private static final Log LOG = LogFactory.getLog(ImageRetriever.class);
    @Resource
    private KoalaIdFactory koalaIdFactory;
    @Resource(name = "generalBlockingCache")
    private BlockingDhtCache blockingDhtCache;
    @Resource
    private ApiApplicationManager apiApplicationManager;
    @Resource
    private PiIdBuilder piIdBuilder;

    public ImageRetriever() {
        blockingDhtCache = null;
        koalaIdFactory = null;
        apiApplicationManager = null;
    }

    public Set<ImageReportEntity> retrieveImagesFromSupernodes(List<String> imageIds) {
        LOG.debug("retrieveImagesFromSupernodes()");
        final Set<ImageReportEntity> allImages = new HashSet<ImageReportEntity>();
        List<String> supernodeIds = getAllSupernodeIds();
        if (supernodeIds.size() > 0) {
            final CountDownLatch latch = new CountDownLatch(supernodeIds.size());
            for (String supernodeId : supernodeIds) {
                MessageContext messageContext = apiApplicationManager.newMessageContext();
                messageContext.routePiMessageToApplication(koalaIdFactory.buildPIdFromHexString(supernodeId), EntityMethod.GET, buildReportEntityCollection(imageIds), ReportingApplication.APPLICATION_NAME,
                        new PiContinuation<ImageReportEntityCollection>() {
                            @Override
                            public void handleResult(ImageReportEntityCollection result) {
                                LOG.debug("Received result: " + result.getEntities().size());
                                allImages.addAll(result.getEntities());
                                latch.countDown();
                            }
                        });
            }
            try {
                latch.await(BLOCKING_CONTINUATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.error("Interrupted exception while waiting for latch", e);
                Thread.interrupted();
            }
        }
        return allImages;
    }

    private ImageReportEntityCollection buildReportEntityCollection(List<String> imageIds) {
        ImageReportEntityCollection collection = new ImageReportEntityCollection();
        if (!CollectionUtils.isEmpty(imageIds)) {
            List<ImageReportEntity> entities = new ArrayList<ImageReportEntity>();
            for (String imageId : imageIds) {
                Image image = new Image();
                image.setImageId(imageId);
                entities.add(new ImageReportEntity(image));
            }
            collection.setEntities(entities);
        }
        return collection;
    }

    private AvailabilityZones getAvailabilityZonesFromDht() {
        PId availabilityZonesPId = piIdBuilder.getAvailabilityZonesId();
        return blockingDhtCache.get(availabilityZonesPId);
    }

    protected List<String> getAllSupernodeIds() {
        LOG.debug("getAllSupernodeIds()");
        List<String> supernodeIds = new ArrayList<String>();
        for (AvailabilityZone avz : getAvailabilityZonesFromDht().getAvailabilityZones().values()) {
            String supernodeId = getSuperNodeApplicationId(avz.getRegionCode(), avz.getAvailabilityZoneCodeWithinRegion());
            if (supernodeId != null)
                supernodeIds.add(supernodeId);
        }
        return supernodeIds;
    }

    private String getSuperNodeApplicationId(int region, int availabilityZone) {
        LOG.debug(String.format("getSuperNodeApplicationId(%d, %d)", region, availabilityZone));
        SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints = (SuperNodeApplicationCheckPoints) blockingDhtCache.get(koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL));
        if (superNodeApplicationCheckPoints == null)
            return null;
        return superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, region, availabilityZone);
    }
}
