/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.app.common.entities.watchers.securitygroup.SecurityGroupResourceWatchingStrategy;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.leased.LeasedAllocatedResource;
import com.bt.pi.core.application.resource.leased.LeasedAllocatedResourceConsumerId;
import com.bt.pi.core.application.resource.watched.WatchedResource;
import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.scope.NodeScope;

@WatchedResource(watchingStrategy = SecurityGroupResourceWatchingStrategy.class)
public class SecurityGroup extends PiEntityBase implements Deletable {
    public static final String SEC_GROUP_ID_FORMAT_STRING = "%s:%s";
    private static final Log LOG = LogFactory.getLog(SecurityGroup.class);
    private static final String SCHEME = "sg";
    private static final int HASH_MULTIPLE = 37;
    private static final int HASH_INITIAL = 17;
    private Long vlanId;
    private Map<String, InstanceAddress> instanceIdToInstanceAddressMap;
    private String networkAddress;
    private String netmask;
    private String dnsAddress;
    private Set<NetworkRule> networkRules;
    private String description;
    private boolean deleted;
    private OwnerIdGroupNamePair ownerIdGroupNamePair;

    public SecurityGroup() {
        this(null, null);
    }

    public SecurityGroup(String anOwnerId, String aGroupName) {
        this(anOwnerId, aGroupName, null, null, null, null, new HashSet<NetworkRule>());
    }

    public SecurityGroup(String anOwnerId, String aGroupName, Long aVlanId, String aNetworkAddress, String aNetmaskAddress, String aDnsAddress, Set<NetworkRule> aNetworkRules) {
        ownerIdGroupNamePair = new OwnerIdGroupNamePair(anOwnerId, aGroupName);
        this.vlanId = aVlanId;
        this.networkAddress = aNetworkAddress;
        this.netmask = aNetmaskAddress;
        this.dnsAddress = aDnsAddress;
        this.instanceIdToInstanceAddressMap = new HashMap<String, InstanceAddress>();
        this.deleted = false;

        setNetworkRules(aNetworkRules);
    }

    public SecurityGroup(SecurityGroup securityGroup) {
        this.description = securityGroup.getDescription();
        this.ownerIdGroupNamePair = securityGroup.getOwnerIdGroupNamePair();
        this.vlanId = securityGroup.getVlanId();
        this.networkAddress = securityGroup.getNetworkAddress();
        this.netmask = securityGroup.getNetmask();
        this.dnsAddress = securityGroup.getDnsAddress();

        this.instanceIdToInstanceAddressMap = new HashMap<String, InstanceAddress>();
        for (Entry<String, InstanceAddress> instanceAddressEntry : securityGroup.getInstances().entrySet()) {
            InstanceAddress instanceAddress = instanceAddressEntry.getValue();
            InstanceAddress clonedInstanceAddress = new InstanceAddress(instanceAddress);
            this.instanceIdToInstanceAddressMap.put(instanceAddressEntry.getKey(), clonedInstanceAddress);
        }
        setNetworkRules(securityGroup.networkRules);
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String aDescription) {
        this.description = aDescription;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean b) {
        deleted = b;
    }

    @JsonIgnore
    @LeasedAllocatedResourceConsumerId
    public String getSecurityGroupId() {
        return String.format(SEC_GROUP_ID_FORMAT_STRING, getOwnerIdGroupNamePair().getOwnerId(), getOwnerIdGroupNamePair().getGroupName());
    }

    @LeasedAllocatedResource(allocationRecordScope = NodeScope.REGION, allocationRecordUri = VlanAllocationIndex.URL)
    public Long getVlanId() {
        return vlanId;
    }

    public String getNetworkAddress() {
        return this.networkAddress;
    }

    @LeasedAllocatedResource(allocationRecordScope = NodeScope.REGION, allocationRecordUri = SubnetAllocationIndex.URL)
    @JsonIgnore
    public long getNetworkAddressLong() {
        return IpAddressUtils.ipToLong(getNetworkAddress());
    }

    public boolean containsInstance(String instanceId) {
        return instanceIdToInstanceAddressMap.containsKey(instanceId);
    }

    public void addInstance(String instanceId, String privateAddress, String publicAddress, String macAddress) {
        instanceIdToInstanceAddressMap.put(instanceId, new InstanceAddress(privateAddress, publicAddress, macAddress));
    }

    public Map<String, InstanceAddress> getInstances() {
        return instanceIdToInstanceAddressMap;
    }

    public void setInstances(Map<String, InstanceAddress> map) {
        this.instanceIdToInstanceAddressMap = map;
    }

    @JsonIgnore
    public int getSlashnet() {
        return IpAddressUtils.netmaskToSlashnet(this.netmask);
    }

    @JsonIgnore
    public String getRouterAddress() {
        if (getNetworkAddress() == null)
            return null;

        return IpAddressUtils.increment(this.getNetworkAddress(), 1);
    }

    @JsonIgnore
    public String getBroadcastAddress() {
        if (networkAddress == null || netmask == null)
            return null;

        long offset = IpAddressUtils.ipToLong(IpAddressUtils.MAX_IP_ADDRESS) - IpAddressUtils.ipToLong(netmask);
        long broadcastAddress = IpAddressUtils.ipToLong(networkAddress) + offset;
        return IpAddressUtils.longToIp(broadcastAddress);
    }

    public String getNetmask() {
        return this.netmask;
    }

    public String getDnsAddress() {
        return this.dnsAddress;
    }

    public Set<NetworkRule> getNetworkRules() {
        return copyNetworkRules(networkRules);
    }

    private Set<NetworkRule> copyNetworkRules(Set<NetworkRule> original) {
        Set<NetworkRule> copy = new HashSet<NetworkRule>();
        if (original != null) {
            for (NetworkRule networkRule : original)
                copy.add(new NetworkRule(networkRule));
        }

        return copy;
    }

    public boolean containsPublicIp(String address) {
        for (InstanceAddress instanceAddress : instanceIdToInstanceAddressMap.values()) {
            if (instanceAddress != null && instanceAddress.getPublicIpAddress() != null && instanceAddress.getPublicIpAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPrivateIpAllocated(String address) {
        for (InstanceAddress instanceAddress : instanceIdToInstanceAddressMap.values()) {
            if (instanceAddress != null && instanceAddress.getPrivateIpAddress() != null && instanceAddress.getPrivateIpAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public List<String> getPrivateAddresses() {
        LOG.debug(String.format("getPrivateIPAddresses"));
        List<String> privateAddressList = new ArrayList<String>();

        long aNetworkAddress = IpAddressUtils.ipToLong(this.getNetworkAddress());
        long aBroadcastAddress = IpAddressUtils.ipToLong(this.getBroadcastAddress());

        LOG.debug(String.format("Looping through %s to %s - 1", this.getRouterAddress(), this.getBroadcastAddress()));

        for (long j = aNetworkAddress + 2; j < aBroadcastAddress; j++) {
            privateAddressList.add(IpAddressUtils.longToIp(j));
        }

        return privateAddressList;
    }

    public void setVlanId(Long aVlanId) {
        this.vlanId = aVlanId;
    }

    public void setNetworkAddress(String aNetworkAddress) {
        this.networkAddress = aNetworkAddress;
    }

    public void setNetmask(String aNetmask) {
        this.netmask = aNetmask;
    }

    public void setDnsAddress(String aDnsAddress) {
        this.dnsAddress = aDnsAddress;
    }

    public void setNetworkRules(Set<NetworkRule> aNetworkRules) {
        networkRules = copyNetworkRules(aNetworkRules);
    }

    public void addNetworkRule(NetworkRule aNetworkRule) {
        networkRules.add(new NetworkRule(aNetworkRule));
    }

    public OwnerIdGroupNamePair getOwnerIdGroupNamePair() {
        return ownerIdGroupNamePair;
    }

    public void setOwnerIdGroupNamePair(OwnerIdGroupNamePair anOwnerIdGroupNamePair) {
        this.ownerIdGroupNamePair = anOwnerIdGroupNamePair;
    }

    public boolean removeNetworkRule(NetworkRule networkRule) {
        boolean ruleRemoved = false;
        if (networkRules != null) {
            ruleRemoved = networkRules.remove(networkRule);
        }
        return ruleRemoved;
    }

    public void removeNetworkRules() {
        if (networkRules != null) {
            networkRules.clear();
        }
    }

    @Override
    public String getUrl() {
        return SecurityGroup.getUrl(getSecurityGroupId());
    }

    public static String getUrl(String entityKey) {
        return String.format(SCHEME + ":%s", entityKey);
    }

    public static String getUrl(String ownerId, String groupName) {
        return getUrl(String.format(SEC_GROUP_ID_FORMAT_STRING, ownerId, groupName));
    }

    @Override
    public String toString() {
        return "SecurityGroup [deleted=" + deleted + ", addresses=" + instanceIdToInstanceAddressMap + ", chainName=" + getSecurityGroupId() + ", dnsAddress=" + dnsAddress + ", netmask=" + netmask + ", networkAddress=" + networkAddress + ", vlanId="
                + vlanId + "]";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).append(deleted).append(instanceIdToInstanceAddressMap).append(getBroadcastAddress()).append(getSecurityGroupId()).append(dnsAddress).append(netmask).append(networkAddress)
                .append(vlanId).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SecurityGroup))
            return false;
        SecurityGroup other = (SecurityGroup) obj;
        return new EqualsBuilder().append(instanceIdToInstanceAddressMap, other.instanceIdToInstanceAddressMap).append(deleted, other.deleted).append(getBroadcastAddress(), other.getBroadcastAddress())
                .append(getSecurityGroupId(), other.getSecurityGroupId()).append(dnsAddress, other.dnsAddress).append(netmask, other.netmask).append(networkAddress, other.networkAddress).append(vlanId, other.vlanId).isEquals();
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }
}
