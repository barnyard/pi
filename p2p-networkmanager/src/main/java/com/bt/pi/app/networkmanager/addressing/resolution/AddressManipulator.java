package com.bt.pi.app.networkmanager.addressing.resolution;

import java.util.Map;
import java.util.Set;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.net.utils.IpAddressUtils;

abstract class AddressManipulator {
    private Map<String, String> addressesOnIface;
    private Map<String, SecurityGroup> addressesInGroups;
    private Set<ResourceRange> cachedIPRanges;

    public AddressManipulator(Map<String, String> aAddressesOnIface, Map<String, SecurityGroup> aAddressesInGroups, Set<ResourceRange> aCachedIPRanges) {
        addressesOnIface = aAddressesOnIface;
        addressesInGroups = aAddressesInGroups;
        cachedIPRanges = aCachedIPRanges;
    }

    public Map<String, String> getAddressesOnIface() {
        return addressesOnIface;
    }

    public Map<String, SecurityGroup> getAddressesInGroups() {
        return addressesInGroups;
    }

    public Set<ResourceRange> getCachedIPRanges() {
        return cachedIPRanges;
    }

    public Long getAddressAsLong(String address) {
        String addr = address.split("/")[0];
        return IpAddressUtils.ipToLong(addr);
    }

    public abstract void addIpAddress(String address, SecurityGroup securityGroup);

    @Override
    public String toString() {
        return String.format("addressManipulator[[addressesOnIface:%s],[addressesInGroups:%s],[cachedIPRanges:%s]]", addressesOnIface, addressesInGroups, cachedIPRanges);
    }
}