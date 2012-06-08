/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

@Component
public class SecurityGroupNetworkUpdateHandler {
    private static final Log LOG = LogFactory.getLog(SecurityGroupNetworkUpdateHandler.class);
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private IpTablesManager ipTablesManager;
    private PiIdBuilder piIdBuilder;

    public SecurityGroupNetworkUpdateHandler() {
        consumedDhtResourceRegistry = null;
        ipTablesManager = null;
        piIdBuilder = null;
    }

    @Resource
    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    @Resource
    public void setIpTablesManager(IpTablesManager anIpTablesManager) {
        this.ipTablesManager = anIpTablesManager;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    public void handle(EntityMethod entityMethod, SecurityGroup securityGroup) {
        LOG.debug(String.format("handle(%s, %s)", entityMethod, securityGroup));
        if (entityMethod.equals(EntityMethod.DELETE)) {
            cleanUpSecurityGroupAsIfRemoved(securityGroup);
        } else
            refreshSharedResourceManager(securityGroup);
    }

    private void cleanUpSecurityGroupAsIfRemoved(final SecurityGroup securityGroup) {
        final PId securityGroupId = piIdBuilder.getPId(securityGroup).forLocalRegion();
        consumedDhtResourceRegistry.clearResource(securityGroupId);
        refreshIpTables();
    }

    private void refreshSharedResourceManager(final SecurityGroup securityGroup) {
        consumedDhtResourceRegistry.refresh(piIdBuilder.getPId(securityGroup).forLocalRegion(), new GenericContinuation<PiEntity>() {
            @Override
            public void handleException(Exception e) {
                LOG.warn(String.format("Error refreshing security group: %s in SharedResourceManager, updating network rules with cached entry", securityGroup), e);
                refreshIpTables();
            }

            @Override
            public void handleResult(PiEntity piEntity) {
                LOG.debug(String.format("Refreshed security group: %s in SharedResourceManager, updating network rules", securityGroup));
                refreshIpTables();
            }
        });
    }

    private void refreshIpTables() {
        ipTablesManager.refreshIpTables();
    }
}
