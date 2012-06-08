/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.handlers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.networkmanager.addressing.resolution.NetworkInconsistencyResolver;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.id.PId;

@Component
public class NetworkCleanupHandler {
    private static final Log LOG = LogFactory.getLog(NetworkCleanupHandler.class);
    private NetworkInconsistencyResolver networkInconsistencyResolver;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private IpTablesManager ipTablesManager;

    public NetworkCleanupHandler() {
        networkInconsistencyResolver = null;
        consumedDhtResourceRegistry = null;
        ipTablesManager = null;
    }

    @Resource
    public void setNetworkInconsistencyResolver(NetworkInconsistencyResolver aNetworkInconsistencyResolver) {
        this.networkInconsistencyResolver = aNetworkInconsistencyResolver;
    }

    @Resource
    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    @Resource
    public void setIpTablesManager(IpTablesManager aIpTablesManager) {
        ipTablesManager = aIpTablesManager;
    }

    public void releaseAllSecurityGroups() {
        LOG.debug("Handling network cleanup request for all sec groups");
        List<SecurityGroup> securityGroups = consumedDhtResourceRegistry.getByType(SecurityGroup.class);
        consumedDhtResourceRegistry.clearAll(SecurityGroup.class);
        doPostCleanupRefreshes(securityGroups);
    }

    public void releaseSecurityGroup(PId securityGroupId) {
        LOG.debug(String.format("Handling network cleanup request for sec group %s", securityGroupId.toStringFull()));
        SecurityGroup securityGroup = consumedDhtResourceRegistry.getCachedEntity(securityGroupId);
        consumedDhtResourceRegistry.clearResource(securityGroupId);
        List<SecurityGroup> securityGroups = new ArrayList<SecurityGroup>();
        securityGroups.add(securityGroup);
        doPostCleanupRefreshes(securityGroups);
    }

    private void doPostCleanupRefreshes(List<SecurityGroup> securityGroups) {
        // TODO tear down some / all stuff we currently have running
        // tear down DHCP

        LOG.debug(String.format("doPostCleanupRefreshes(List<SecurityGroup> %s)", securityGroups));
        networkInconsistencyResolver.refreshIpAddressesOnPublicInterfaceAndBridges();
        networkInconsistencyResolver.tearDownNetworkManagerAddressesForGroups(securityGroups);
        ipTablesManager.refreshIpTables();
    }
}
