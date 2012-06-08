package com.bt.pi.app.management;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.VlanAllocationIndex;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.id.PId;

@Component
public class NetworkSeeder extends SeederBase {

    private static final Log LOG = LogFactory.getLog(NetworkSeeder.class);

    public NetworkSeeder() {
        super();
    }

    public boolean createPublicAddressAllocationIndex(String publicIpAddressRangesString, int regionCode) {
        LOG.info(String.format("Creating public IP address allocation index record for addresses %s in region %s...", publicIpAddressRangesString, regionCode));
        PId publicAddressIndexId = getPiIdBuilder().getPId(PublicIpAllocationIndex.URL).forRegion(regionCode);

        Set<ResourceRange> ipAddressRanges = new HashSet<ResourceRange>();
        StringTokenizer st = new StringTokenizer(publicIpAddressRangesString);
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            ResourceRange rr;

            if (next.indexOf(DASH) > -1) {
                String[] addrPair = next.split(DASH);
                try {
                    long lower = IpAddressUtils.ipToLong(addrPair[0]);
                    long upper = IpAddressUtils.ipToLong(addrPair[1]);
                    rr = new ResourceRange(lower, upper);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            } else {
                long addr = IpAddressUtils.ipToLong(next);
                rr = new ResourceRange(addr, addr);
            }
            ipAddressRanges.add(rr);
        }

        PublicIpAllocationIndex publicIpAllocationIndex = new PublicIpAllocationIndex();
        publicIpAllocationIndex.setResourceRanges(ipAddressRanges);
        publicIpAllocationIndex.setInactiveResourceConsumerTimeoutSec(DEFAULT_INACTIVE_RESOURCE_CONSUMER_FOR_PUBLIC_IP_ALLOCATION_INDEX_TIMEOUT_SEC);

        boolean publicIpAllocationIndexWritten = getDhtClientFactory().createBlockingWriter().writeIfAbsent(publicAddressIndexId, publicIpAllocationIndex);

        LOG.info(String.format("Public IP address allocation index record %s created", publicIpAllocationIndexWritten ? "" : NOT));
        return publicIpAllocationIndexWritten;
    }

    public boolean createVlanAllocationIndex(String vlanIdRangesString, int regionCode) {
        LOG.info(String.format("Creating VLAN allocation index record for vlan ranges %s in region %s...", vlanIdRangesString, regionCode));

        PId vlanAllocationIndexId = getPiIdBuilder().getPId(VlanAllocationIndex.URL).forRegion(regionCode);

        // TODO: some basic validation
        Set<ResourceRange> vlanIdRanges = new HashSet<ResourceRange>();
        StringTokenizer st = new StringTokenizer(vlanIdRangesString);
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            ResourceRange rr;

            if (next.indexOf(DASH) > -1) {
                String[] addrPair = next.split(DASH);
                try {
                    long lower = Long.parseLong(addrPair[0]);
                    long upper = Long.parseLong(addrPair[1]);
                    rr = new ResourceRange(lower, upper);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            } else {
                long addr = Long.parseLong(next);
                rr = new ResourceRange(addr, addr);
            }
            vlanIdRanges.add(rr);
        }

        VlanAllocationIndex vlanAllocationIndex = new VlanAllocationIndex();
        vlanAllocationIndex.setResourceRanges(vlanIdRanges);
        vlanAllocationIndex.setInactiveResourceConsumerTimeoutSec(DEFAULT_INACTIVE_RESOURCE_CONSUMER_TIMEOUT_SEC);

        boolean vlanAllocationIndexWritten = getDhtClientFactory().createBlockingWriter().writeIfAbsent(vlanAllocationIndexId, vlanAllocationIndex);

        LOG.info(String.format("VLAN allocation index record %s created", vlanAllocationIndexWritten ? "" : NOT));
        return vlanAllocationIndexWritten;
    }

    public boolean createSubnetAllocationIndex(String networksString, String dnsAddress, int regionCode) {
        LOG.info(String.format("Creating subnet allocation index record for nets %s, using dns addr %s in region %s...", networksString, dnsAddress, regionCode));
        PId publicAddressIndexId = getPiIdBuilder().getPId(SubnetAllocationIndex.URL).forRegion(regionCode);

        Set<ResourceRange> subnetRanges = new HashSet<ResourceRange>();
        StringTokenizer st = new StringTokenizer(networksString);
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            ResourceRange rr;

            String[] firstPair = next.split(SLASH);
            try {
                long baseAddress = IpAddressUtils.ipToLong(firstPair[0]);
                String[] secondPair = firstPair[1].split(SEMICOLON);
                int numAddrs = IpAddressUtils.addrsInSlashnet(Integer.parseInt(secondPair[0]));
                int addrsPerAllocation = Integer.parseInt(secondPair[1]);
                rr = new ResourceRange(baseAddress, baseAddress + numAddrs - 1, addrsPerAllocation);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            subnetRanges.add(rr);
        }

        // validate dns addr
        IpAddressUtils.ipToLong(dnsAddress);

        SubnetAllocationIndex subnetAllocationIndex = new SubnetAllocationIndex();
        subnetAllocationIndex.setResourceRanges(subnetRanges);
        subnetAllocationIndex.setDnsAddress(dnsAddress);
        subnetAllocationIndex.setInactiveResourceConsumerTimeoutSec(DEFAULT_INACTIVE_RESOURCE_CONSUMER_TIMEOUT_SEC);

        boolean subnetAllocationIndexWritten = getDhtClientFactory().createBlockingWriter().writeIfAbsent(publicAddressIndexId, subnetAllocationIndex);

        LOG.info(String.format("Subnet allocation index record %s created", subnetAllocationIndexWritten ? "" : NOT));
        return subnetAllocationIndexWritten;
    }

}
