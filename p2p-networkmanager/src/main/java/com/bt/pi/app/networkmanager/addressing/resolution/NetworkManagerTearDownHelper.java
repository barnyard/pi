package com.bt.pi.app.networkmanager.addressing.resolution;

import java.util.Collection;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.networkmanager.net.VirtualNetworkBuilder;

@Component
public class NetworkManagerTearDownHelper {
    private static final Log LOG = LogFactory.getLog(NetworkManagerTearDownHelper.class);

    @Resource
    private VirtualNetworkBuilder virtualNetworkBuilder;

    public NetworkManagerTearDownHelper() {
        virtualNetworkBuilder = null;
    }

    public void setVirtualNetworkBuilder(VirtualNetworkBuilder aVirtualNetworkBuilder) {
        this.virtualNetworkBuilder = aVirtualNetworkBuilder;
    }

    public void removeAllAddressesFromSecurityGroups(Collection<SecurityGroup> groups) {
        LOG.debug(String.format("removeAllAddressesFromSecurityGroups(%s)", groups));
        for (SecurityGroup securityGroup : groups) {
            virtualNetworkBuilder.tearDownVirtualNetworkForSecurityGroup(securityGroup);
        }
    }
}
