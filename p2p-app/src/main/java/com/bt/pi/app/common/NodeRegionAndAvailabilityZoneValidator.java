package com.bt.pi.app.common;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.NodeStartedEvent;

@Component
public class NodeRegionAndAvailabilityZoneValidator implements ApplicationListener<NodeStartedEvent> {
    private static final Log LOG = LogFactory.getLog(NodeRegionAndAvailabilityZoneValidator.class);

    private KoalaIdFactory koalaIdFactory;
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;

    public NodeRegionAndAvailabilityZoneValidator() {
        koalaIdFactory = null;
        piIdBuilder = null;
        dhtClientFactory = null;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory aKoalaIdFactory) {
        this.koalaIdFactory = aKoalaIdFactory;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Override
    public void onApplicationEvent(NodeStartedEvent event) {
        final int regionCode = koalaIdFactory.getRegion();
        final int availabilityZoneCode = koalaIdFactory.getAvailabilityZoneWithinRegion();

        PId regionsId = piIdBuilder.getRegionsId();
        PId availabilityZonesId = piIdBuilder.getAvailabilityZonesId();

        dhtClientFactory.createReader().getAsync(regionsId, new RegionAndAvailabilityZoneContinuation<Regions>("region", regionCode));
        dhtClientFactory.createReader().getAsync(availabilityZonesId, new RegionAndAvailabilityZoneContinuation<AvailabilityZones>("availability zone", availabilityZoneCode));
    }

    static class RegionAndAvailabilityZoneContinuation<T> extends GenericContinuation<T> {
        private String entityDescriptionString;
        private int code;

        public RegionAndAvailabilityZoneContinuation(String anEntityDescriptionString, int aCode) {
            entityDescriptionString = anEntityDescriptionString;
            code = aCode;
        }

        @Override
        public void handleException(Exception exception) {
            LOG.error(String.format("Error retrieving %ss entity from the dht", entityDescriptionString), exception);
        }

        @Override
        public void handleResult(T result) {
            if (result == null) {
                LOG.warn(String.format("%s entity does not exist in the dht, OK if this is the very first node that has started. Ensure the seeding process occurs!", entityDescriptionString));
                return;
            }

            if (result instanceof Regions) {
                for (Region region : ((Regions) result).getRegions().values()) {
                    if (region.getRegionCode() == code) {
                        LOG.info(String.format("Node has started in region %s, code %d", region.getRegionName(), code));
                        return;
                    }

                }
            } else if (result instanceof AvailabilityZones) {
                for (AvailabilityZone availabilityZone : ((AvailabilityZones) result).getAvailabilityZones().values()) {
                    if (availabilityZone.getAvailabilityZoneCodeWithinRegion() == code) {
                        LOG.info(String.format("Node has started in availability zone %s, code %d", availabilityZone.getAvailabilityZoneName(), code));
                        return;
                    }
                }
            } else {
                LOG.warn(String.format("Unknown object type: %s, throw it away", result.getClass()));
            }

            LOG.error(String.format("Node has started with %s code %d, which does not exist in the dht!", entityDescriptionString, code));
        }
    }
}
