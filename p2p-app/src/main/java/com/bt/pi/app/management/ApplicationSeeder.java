package com.bt.pi.app.management;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;
import com.bt.pi.core.id.PId;

@Component
public class ApplicationSeeder extends SeederBase {
    private static final String APPLICATION_RECORD_FOR_S_S_CREATED = "Application record for %s %s created";
    private static final Log LOG = LogFactory.getLog(ApplicationSeeder.class);

    public ApplicationSeeder() {
        super();
    }

    public boolean createAvailabilityZoneScopedApplicationRecord(String applicationName, Integer regionCode, Integer availabilityZoneCodeWithinRegion, List<String> resources) {
        LOG.info(String.format("Creating application record object for availability zone scoped application %s, with region code %s, avz code %s and resources %s", applicationName, regionCode, availabilityZoneCodeWithinRegion, resources));

        boolean containsAddresses = containsAddresses(resources);

        ApplicationRecord application;
        if (containsAddresses) {
            List<String> addresses = extractAddresses(resources);
            application = new AvailabilityZoneScopedApplicationRecord(applicationName, 1, addresses);
        } else
            application = new AvailabilityZoneScopedApplicationRecord(applicationName, 1, resources);

        PId applicationId;
        if (regionCode != null && availabilityZoneCodeWithinRegion != null)
            applicationId = getPiIdBuilder().getPId(application).forGlobalAvailablityZoneCode(PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(regionCode, availabilityZoneCodeWithinRegion));
        else
            throw new IllegalArgumentException(String.format("Application record for app %s had wrong region and / or avz (%s / %s)", applicationName, regionCode, availabilityZoneCodeWithinRegion));

        return writeApplicationRecord(applicationName, application, applicationId);
    }

    public boolean createRegionScopedApplicationRecord(String applicationName, Integer regionCode, List<String> resources) {
        LOG.info(String.format("Creating application record object for region scoped application %s, with region code %s and resources %s", applicationName, regionCode, resources));

        boolean containsAddresses = containsAddresses(resources);

        ApplicationRecord application;
        if (containsAddresses) {
            List<String> addresses = extractAddresses(resources);
            application = new RegionScopedApplicationRecord(applicationName, 1, addresses);
        } else
            application = new RegionScopedApplicationRecord(applicationName, 1, resources);

        PId applicationId;
        if (regionCode != null)
            applicationId = getPiIdBuilder().getPId(application).forRegion(regionCode);
        else
            throw new IllegalArgumentException(String.format("Application record for app %s had wrong (null) region", applicationName));

        return writeApplicationRecord(applicationName, application, applicationId);
    }

    private boolean writeApplicationRecord(String applicationName, ApplicationRecord application, PId applicationId) {
        LOG.debug(String.format("writeApplicationRecord(%s,%s,%s)", applicationName, application, applicationId));
        boolean applicationRecordWritten = getDhtClientFactory().createBlockingWriter().writeIfAbsent(applicationId, application);

        LOG.info(String.format(APPLICATION_RECORD_FOR_S_S_CREATED, applicationName + "-" + applicationId.toStringFull(), applicationRecordWritten ? "" : NOT));
        return applicationRecordWritten;
    }

    private boolean containsAddresses(List<String> resources) {
        String firstResource = resources.get(0);
        boolean containsAddresses = false;
        if (firstResource.contains(DASH)) {
            containsAddresses = IpAddressUtils.isIpAddress(firstResource.substring(0, firstResource.indexOf(DASH)));
        } else
            containsAddresses = IpAddressUtils.isIpAddress(firstResource);
        return containsAddresses;
    }

    private List<String> extractAddresses(List<String> resources) {
        Set<ResourceRange> addressRanges = new HashSet<ResourceRange>();

        for (String ipAddressBlock : resources) {
            ResourceRange rr;

            if (ipAddressBlock.indexOf(DASH) > -1) {
                String[] addrPair = ipAddressBlock.split(DASH);
                try {
                    long lower = IpAddressUtils.ipToLong(addrPair[0]);
                    long upper = IpAddressUtils.ipToLong(addrPair[1]);
                    rr = new ResourceRange(lower, upper);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            } else {
                long addr = IpAddressUtils.ipToLong(ipAddressBlock);
                rr = new ResourceRange(addr, addr);
            }
            addressRanges.add(rr);
        }
        List<String> addresses = expandAddressRanges(addressRanges);
        return addresses;
    }

    private List<String> expandAddressRanges(Set<ResourceRange> addressRanges) {
        List<String> allAddresses = new ArrayList<String>();
        for (ResourceRange addressRange : addressRanges) {
            Iterator<Long> rangeIt = addressRange.iterator();
            while (rangeIt.hasNext()) {
                allAddresses.add(IpAddressUtils.longToIp(rangeIt.next()));
            }
        }
        return allAddresses;
    }
}
