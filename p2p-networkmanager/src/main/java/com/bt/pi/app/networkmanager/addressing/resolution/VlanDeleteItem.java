package com.bt.pi.app.networkmanager.addressing.resolution;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.net.NetworkCommandRunner;

public class VlanDeleteItem extends AddressDeleteQueueItem {
    private static final Log LOG = LogFactory.getLog(VlanDeleteItem.class);

    private long vlanId;
    private String iface;
    private NetworkCommandRunner networkCommandRunner;

    public VlanDeleteItem(long aVlanId, String anIface, NetworkCommandRunner aNetworkCommandRunner) {
        this.vlanId = aVlanId;
        this.iface = anIface;
        networkCommandRunner = aNetworkCommandRunner;
    }

    @Override
    public void delete() {
        LOG.debug(String.format("Removing vlan id %d from iface %s as part of refresh", vlanId, iface));
        networkCommandRunner.removeManagedNetwork(vlanId, iface);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(vlanId).append(iface).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof VlanDeleteItem))
            return false;
        VlanDeleteItem castOther = (VlanDeleteItem) other;
        return new EqualsBuilder().append(vlanId, castOther.vlanId).append(iface, castOther.iface).isEquals();
    }

    @Override
    public String toString() {
        return String.format("VlanDeleteItem[%d, %s], priority: %d", vlanId, iface, getPriority());
    }
}