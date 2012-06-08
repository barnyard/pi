package com.bt.pi.app.common.entities.util;

import java.util.Collection;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.KoalaIdFactory;

@Component
public class QueueOwnerRemovalHelper {
    private static final Log LOG = LogFactory.getLog(QueueOwnerRemovalHelper.class);
    private DhtClientFactory dhtClientFactory;
    private KoalaIdFactory koalaIdFactory;

    public QueueOwnerRemovalHelper() {
        dhtClientFactory = null;
        koalaIdFactory = null;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory aKoalaIdFactory) {
        koalaIdFactory = aKoalaIdFactory;
    }

    public void removeNodeIdFromAllQueues(final Collection<PiLocation> queueLocations, QueueOwnerRemovalContinuation continuation) {
        if (continuation.getOwner() != null && queueLocations != null) {
            for (PiLocation piLocation : queueLocations) {
                DhtWriter writer = dhtClientFactory.createWriter();
                writer.update(koalaIdFactory.buildPId(piLocation.getUrl()).forLocalScope(piLocation.getNodeScope()), continuation);
            }
        } else
            LOG.warn(String.format("Not removing owner from queues. Owner: %s. Queue locations: %s.", continuation.getOwner(), queueLocations));
    }

}
