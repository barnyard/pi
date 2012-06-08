package com.bt.pi.app.common.net;

import org.apache.commons.lang.builder.ToStringBuilder;

public class SubnetAllocationResult {
    private long subnetBaseAddress;
    private long subnetMask;
    private String dnsAddress;

    public SubnetAllocationResult(long aSubnetBaseAddress, long aSubnetMask, String aDnsAddress) {
        super();
        this.subnetBaseAddress = aSubnetBaseAddress;
        this.subnetMask = aSubnetMask;
        this.dnsAddress = aDnsAddress;
    }

    public long getSubnetBaseAddress() {
        return subnetBaseAddress;
    }

    public long getSubnetMask() {
        return subnetMask;
    }

    public String getDnsAddress() {
        return dnsAddress;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("subnetBaseAddress", subnetBaseAddress).append("subnetMask", subnetMask).append("dnsAddress", dnsAddress).toString();
    }
}
