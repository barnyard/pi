/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class NetworkRule {
    private static final int HASH_MULTIPLE = 37;
    private static final int HASH_INITIAL = 19;
    private NetworkRuleType networkRuleType;
    private OwnerIdGroupNamePair[] ownerIdGroupNamePair;
    private String[] sourceNetworks;
    private String destinationSecurityGroupName;
    private NetworkProtocol networkProtocol;
    private int portRangeMin;
    private int portRangeMax;

    public NetworkRule() {
        this.networkRuleType = null;
        this.ownerIdGroupNamePair = new OwnerIdGroupNamePair[0];
        this.sourceNetworks = new String[0];
        this.destinationSecurityGroupName = null;
        this.networkProtocol = null;
        this.portRangeMin = 0;
        this.portRangeMax = 0;
    }

    public NetworkRule(NetworkRule other) {
        this.networkRuleType = other.networkRuleType;

        this.ownerIdGroupNamePair = new OwnerIdGroupNamePair[0];
        if (other.ownerIdGroupNamePair != null) {
            this.ownerIdGroupNamePair = new OwnerIdGroupNamePair[other.ownerIdGroupNamePair.length];
            for (int i = 0; i < other.ownerIdGroupNamePair.length; i++)
                this.ownerIdGroupNamePair[i] = other.ownerIdGroupNamePair[i];
        }

        this.sourceNetworks = new String[0];
        if (other.sourceNetworks != null) {
            this.sourceNetworks = new String[other.sourceNetworks.length];
            for (int i = 0; i < other.sourceNetworks.length; i++)
                this.sourceNetworks[i] = other.sourceNetworks[i];
        }

        this.destinationSecurityGroupName = other.destinationSecurityGroupName;
        this.networkProtocol = other.networkProtocol;
        this.portRangeMin = other.portRangeMin;
        this.portRangeMax = other.portRangeMax;
    }

    public NetworkRuleType getNetworkRuleType() {
        return networkRuleType;
    }

    public void setNetworkRuleType(NetworkRuleType aNetworkRuleType) {
        this.networkRuleType = aNetworkRuleType;
    }

    public OwnerIdGroupNamePair[] getOwnerIdGroupNamePair() {
        return ownerIdGroupNamePair;
    }

    public void setOwnerIdGroupNamePair(OwnerIdGroupNamePair[] anOwnerIdGroupNamePair) {
        this.ownerIdGroupNamePair = anOwnerIdGroupNamePair;
    }

    public String[] getSourceNetworks() {
        return sourceNetworks;
    }

    public void setSourceNetworks(String[] aSourceNetworks) {
        this.sourceNetworks = aSourceNetworks;
    }

    public String getDestinationSecurityGroupName() {
        return destinationSecurityGroupName;
    }

    public void setDestinationSecurityGroupName(String aDestinationSecurityGroupName) {
        this.destinationSecurityGroupName = aDestinationSecurityGroupName;
    }

    public NetworkProtocol getNetworkProtocol() {
        return networkProtocol;
    }

    public void setNetworkProtocol(NetworkProtocol aNetworkProtocol) {
        this.networkProtocol = aNetworkProtocol;
    }

    public int getPortRangeMin() {
        return portRangeMin;
    }

    public void setPortRangeMin(int aPortRangeMin) {
        this.portRangeMin = aPortRangeMin;
    }

    public int getPortRangeMax() {
        return portRangeMax;
    }

    public void setPortRangeMax(int aPortRangeMax) {
        this.portRangeMax = aPortRangeMax;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof NetworkRule))
            return false;
        NetworkRule other = (NetworkRule) obj;
        return new EqualsBuilder().append(networkRuleType, other.networkRuleType).append(ownerIdGroupNamePair, other.ownerIdGroupNamePair).append(sourceNetworks, other.sourceNetworks).append(destinationSecurityGroupName,
                other.destinationSecurityGroupName).append(networkProtocol, other.networkProtocol).append(portRangeMin, other.portRangeMin).append(portRangeMax, other.portRangeMax).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).append(networkRuleType).append(ownerIdGroupNamePair).append(sourceNetworks).append(destinationSecurityGroupName).append(networkProtocol).append(portRangeMin).append(portRangeMax)
                .toHashCode();
    }
}