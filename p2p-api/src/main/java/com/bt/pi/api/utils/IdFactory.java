/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.utils;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

/*
 * Class to generate new ids for various entities 
 */
@Component
public class IdFactory {
    private static final Log LOG = LogFactory.getLog(IdFactory.class);
    private static final String DEFAULT_RETRIES = "10";
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private PiIdBuilder piIdBuilder;
    private int retries = Integer.parseInt(DEFAULT_RETRIES);

    public IdFactory() {
        this.dhtClientFactory = null;
        this.piIdBuilder = null;
    }

    @Property(key = "id.factory.retries", defaultValue = DEFAULT_RETRIES)
    public void setRetries(int value) {
        this.retries = value;
    }

    public String createNewVolumeId(int globalAvailabilityZoneCode) {
        LOG.debug("createNewVolumeId()");
        return createNewId("vol", globalAvailabilityZoneCode, new UrlFactory() {
            @Override
            public String getUri(String s) {
                return Volume.getUrl(s);
            }

            @Override
            public PId getIdFromUrl(String url) {
                return piIdBuilder.getPIdForEc2AvailabilityZone(url);
            }
        });
    }

    public String createNewSnapshotId(int globalAvailabilityZoneCode) {
        LOG.debug("createNewSnapshotId()");
        return createNewId(Snapshot.SCHEME, globalAvailabilityZoneCode, new UrlFactory() {
            @Override
            public String getUri(String s) {
                return Snapshot.getUrl(s);
            }

            @Override
            public PId getIdFromUrl(String url) {
                return piIdBuilder.getPIdForEc2AvailabilityZone(url);
            }
        });
    }

    public String createNewReservationId() {
        LOG.debug("createNewReservationId()");
        return createNewId("r", null, new UrlFactory() {
            @Override
            public String getUri(String s) {
                return Reservation.getUrl(s);
            }

            @Override
            public PId getIdFromUrl(String url) {
                return piIdBuilder.getPId(url);
            }
        });
    }

    public String createNewInstanceId(int globalAvailabilityZoneCode) {
        LOG.debug("createNewInstanceId()");
        return createNewId("i", globalAvailabilityZoneCode, new UrlFactory() {
            @Override
            public String getUri(String s) {
                return Instance.getUrl(s);
            }

            @Override
            public PId getIdFromUrl(String url) {
                return piIdBuilder.getPIdForEc2AvailabilityZone(url);
            }
        });
    }

    public String createNewImageId() {
        LOG.debug("createNewImageId()");
        return createNewId("pmi", null, new ImageUrlFactory(piIdBuilder));
    }

    public String createNewKernelId() {
        LOG.debug("createNewKernelId()");
        return createNewId("pki", null, new ImageUrlFactory(piIdBuilder));
    }

    public String createNewRamdiskId() {
        LOG.debug("createNewRamdiskId()");
        return createNewId("pri", null, new ImageUrlFactory(piIdBuilder));
    }

    private String createNewId(String prefix, Integer globalAvailabilityZoneCode, UrlFactory urlFactoryCallback) {
        LOG.debug(String.format("createNewId(%s, %s)", prefix, urlFactoryCallback));
        for (int i = 0; i < retries; i++) {
            String result;
            if (globalAvailabilityZoneCode != null)
                result = piIdBuilder.generateBase62Ec2Id(prefix, globalAvailabilityZoneCode);
            else
                result = piIdBuilder.generateStandardEc2Id(prefix);

            String url = urlFactoryCallback.getUri(result);

            LOG.debug(String.format("url: %s", url));
            PId dhtRecordId = urlFactoryCallback.getIdFromUrl(url);

            PiEntity piEntity = this.dhtClientFactory.createBlockingReader().get(dhtRecordId);
            if (null == piEntity)
                return result;
            if (piEntity instanceof Deletable) {
                Deletable deletable = (Deletable) piEntity;
                if (deletable.isDeleted())
                    return result;
            }
        }
        throw new RetriesExhaustedException("unable to generate unique id");
    }

}
