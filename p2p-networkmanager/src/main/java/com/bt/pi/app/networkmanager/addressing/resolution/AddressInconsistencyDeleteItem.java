package com.bt.pi.app.networkmanager.addressing.resolution;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.net.NetworkCommandRunner;

public class AddressInconsistencyDeleteItem extends AddressDeleteQueueItem {
    private static final Log LOG = LogFactory.getLog(AddressInconsistencyDeleteItem.class);
    private String address;
    private String iface;
    private NetworkCommandRunner networkCommandRunner;

    public AddressInconsistencyDeleteItem(int priority, String anAddress, String anIface, NetworkCommandRunner aNetworkCommandRunner) {
        super(priority);
        this.address = anAddress;
        this.iface = anIface;
        networkCommandRunner = aNetworkCommandRunner;
    }

    @Override
    public void delete() {
        LOG.debug(String.format("Removing IP address %s as part of refresh", address));
        networkCommandRunner.ipAddressDelete(address, iface);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(address).append(iface).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AddressInconsistencyDeleteItem))
            return false;
        AddressInconsistencyDeleteItem castOther = (AddressInconsistencyDeleteItem) other;
        return new EqualsBuilder().append(address, castOther.address).append(iface, castOther.iface).isEquals();
    }

    @Override
    public String toString() {
        return String.format("AddressDeleteItem[%s, %s], priority: %d", address, iface, getPriority());
    }
}
