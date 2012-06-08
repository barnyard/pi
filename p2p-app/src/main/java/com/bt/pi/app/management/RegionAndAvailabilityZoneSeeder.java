package com.bt.pi.app.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.id.PId;

@Component
public class RegionAndAvailabilityZoneSeeder extends SeederBase {
    private static final Log LOG = LogFactory.getLog(RegionAndAvailabilityZoneSeeder.class);

    public RegionAndAvailabilityZoneSeeder() {
        super();
    }

    public boolean configureRegions(String regionNames, String regionCodes, String regionEndpoints, String regionPisssEndpoints) {
        LOG.info(String.format("Creating %s regions, with endpoints %s", regionNames, regionEndpoints));

        String[] regionNamesArray = regionNames.split(SEMICOLON);
        String[] regionCodesArray = regionCodes.split(SEMICOLON);
        String[] regionEndpointsArray = regionEndpoints.split(SEMICOLON);
        String[] regionPisssEndpointsArray = regionPisssEndpoints.split(SEMICOLON);

        if (!(regionNamesArray.length == regionEndpointsArray.length && regionNamesArray.length == regionCodesArray.length && regionNamesArray.length == regionPisssEndpointsArray.length))
            return false;

        Regions regions = new Regions();
        for (int i = 0; i < regionNamesArray.length; i++) {
            Region region = new Region(regionNamesArray[i], Integer.parseInt(regionCodesArray[i]), regionEndpointsArray[i], regionPisssEndpointsArray[i]);
            regions.addRegion(region);
        }

        PId id = getPiIdBuilder().getRegionsId();
        boolean recordWritten = getDhtClientFactory().createBlockingWriter().writeIfAbsent(id, regions);
        LOG.info(String.format("Regions record %s created", recordWritten ? "" : NOT));
        return recordWritten;
    }

    public boolean configureAvailabilityZones(String availabilityZoneNames, String availabilityZoneCodes, String regionCodes, String statuses) {
        LOG.info(String.format("Creating %s availabilityZones, with codes %s, regions %s and statuses %s", availabilityZoneNames, availabilityZoneCodes, regionCodes, statuses));

        String[] availabilityZoneNamesArray = availabilityZoneNames.split(SEMICOLON);
        String[] availabilityZoneCodesArray = availabilityZoneCodes.split(SEMICOLON);
        String[] regionCodeStringsArray = regionCodes.split(SEMICOLON);
        String[] statusesArray = statuses.split(SEMICOLON);

        if (!(availabilityZoneNamesArray.length == availabilityZoneCodesArray.length && availabilityZoneNamesArray.length == regionCodeStringsArray.length && availabilityZoneNamesArray.length == statusesArray.length)) {
            LOG.info(String.format("Not all parameter arrays were of the same length"));
            return false;
        }

        int[] regionCodesArray = new int[regionCodeStringsArray.length];
        for (int i = 0; i < regionCodeStringsArray.length; i++)
            regionCodesArray[i] = Integer.parseInt(regionCodeStringsArray[i]);

        AvailabilityZones availabilityZones = new AvailabilityZones();
        for (int i = 0; i < availabilityZoneNamesArray.length; i++) {
            AvailabilityZone availabilityZone = new AvailabilityZone(availabilityZoneNamesArray[i], Integer.parseInt(availabilityZoneCodesArray[i]), regionCodesArray[i], statusesArray[i]);
            availabilityZones.addAvailabilityZone(availabilityZone);
        }

        PId id = getPiIdBuilder().getAvailabilityZonesId();
        boolean recordWritten = getDhtClientFactory().createBlockingWriter().writeIfAbsent(id, availabilityZones);
        LOG.info(String.format("Availability Zones record %s created", recordWritten ? "" : NOT));
        return recordWritten;
    }

    public boolean addRegion(final String regionName, final String regionCode, final String regionEndpoint, final String regionPisssEndpoint) {
        LOG.info(String.format("Adding region %s with code %s and endpoint %s", regionName, regionCode, regionEndpoint));

        PId id = getPiIdBuilder().getRegionsId();
        BlockingDhtWriter writer = getDhtClientFactory().createBlockingWriter();
        writer.update(id, null, new UpdateResolver<Regions>() {
            @Override
            public Regions update(Regions existingEntity, Regions requestedEntity) {
                Regions regions = existingEntity;
                if (regions == null) {
                    LOG.info(String.format("NO regions record found, creating a new one"));
                    regions = new Regions();
                }

                Region region = new Region(regionName, Integer.parseInt(regionCode), regionEndpoint, regionPisssEndpoint);
                regions.addRegion(region);
                return regions;
            }
        });
        boolean recordWritten = writer.getValueWritten() != null;
        LOG.info(String.format("Region %s %s added", regionName, recordWritten ? "" : NOT));
        return recordWritten;
    }

    public boolean addAvailabilityZone(final String availabilityZoneName, final String availabilityZoneCodeWithinRegion, final String regionCode, final String availabilityZoneStatus) {
        LOG.info(String.format("Adding availability zone code %s with code %s in region %s, status %s", availabilityZoneName, availabilityZoneCodeWithinRegion, regionCode, availabilityZoneStatus));

        PId id = getPiIdBuilder().getAvailabilityZonesId();
        BlockingDhtWriter writer = getDhtClientFactory().createBlockingWriter();
        writer.update(id, null, new UpdateResolver<AvailabilityZones>() {
            @Override
            public AvailabilityZones update(AvailabilityZones existingEntity, AvailabilityZones requestedEntity) {
                AvailabilityZones avzs = existingEntity;
                if (avzs == null) {
                    LOG.info(String.format("NO availability zones record found, creating a new one"));
                    avzs = new AvailabilityZones();
                }

                AvailabilityZone avz = new AvailabilityZone(availabilityZoneName, Integer.parseInt(availabilityZoneCodeWithinRegion), Integer.parseInt(regionCode), availabilityZoneStatus);
                avzs.addAvailabilityZone(avz);
                return avzs;
            }
        });
        boolean recordWritten = writer.getValueWritten() != null;
        LOG.info(String.format("Availability zone %s %s created", availabilityZoneName, recordWritten ? "" : NOT));
        return recordWritten;
    }
}
