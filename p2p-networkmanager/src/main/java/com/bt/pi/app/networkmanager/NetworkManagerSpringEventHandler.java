package com.bt.pi.app.networkmanager;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import rice.p2p.commonapi.Id;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.networkmanager.handlers.NetworkCleanupHandler;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.ApplicationRecordRefreshedEvent;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.id.PId;

@Component
public class NetworkManagerSpringEventHandler implements ApplicationListener<ApplicationEvent> {
    private static final Log LOG = LogFactory.getLog(NetworkManagerSpringEventHandler.class);
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private NetworkManagerApplication networkManagerApplication;
    private NetworkCleanupHandler networkCleanupHandler;
    private PiIdBuilder piIdBuilder;

    public NetworkManagerSpringEventHandler() {
        consumedDhtResourceRegistry = null;
        networkManagerApplication = null;
        piIdBuilder = null;
        networkCleanupHandler = null;
    }

    @Resource
    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setNetworkManagerApplication(NetworkManagerApplication aNetworkManagerApplication) {
        this.networkManagerApplication = aNetworkManagerApplication;
    }

    @Resource
    public void setNetworkCleanupHandler(NetworkCleanupHandler aNetworkCleanupHandler) {
        this.networkCleanupHandler = aNetworkCleanupHandler;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof ApplicationRecordRefreshedEvent)) {
            LOG.debug(String.format("Ignoring irrelevant Spring application context event: %s", event.getClass()));
            return;
        }

        ApplicationRecordRefreshedEvent applicationRecordRefreshedEvent = (ApplicationRecordRefreshedEvent) event;
        ApplicationRecord applicationRecord = applicationRecordRefreshedEvent.getApplicationRecord();
        if (!NetworkManagerApplication.APPLICATION_NAME.equals(applicationRecord.getApplicationName())) {
            LOG.debug(String.format("Ignoring irrelevant Spring application record refresh event as it was for app %s", applicationRecord.getApplicationName()));
            return;
        }

        if (applicationRecord.getNumCurrentlyActiveNodes() < 1) {
            LOG.debug(String.format("Doing nothing as < 1 active nodes"));
            return;
        }

        checkCurrentNodeIsStillNearestForOwnedGroups(applicationRecord);
    }

    private void checkCurrentNodeIsStillNearestForOwnedGroups(ApplicationRecord applicationRecord) {
        LOG.debug(String.format("Processing app record updated Spring application event for app record %s", applicationRecord));
        List<SecurityGroup> securityGroups = consumedDhtResourceRegistry.getByType(SecurityGroup.class);
        for (SecurityGroup securityGroup : securityGroups) {
            PId networkManagerAppIdForSecGroup = piIdBuilder.getPId(securityGroup).forLocalAvailabilityZone();
            // TODO: Id.build should not be used. We need to look at hoe we get rid of all of the references.
            Id nearestActiveNodeId = applicationRecord.getClosestActiveNodeId(rice.pastry.Id.build(networkManagerAppIdForSecGroup.getIdAsHex()));
            Id currentNodeId = networkManagerApplication.getNodeId();
            if (currentNodeId.equals(nearestActiveNodeId)) {
                LOG.debug(String.format("This node (%s) is still the nearest active node to net manager app slot %s for sec group %s", currentNodeId.toStringFull(), networkManagerAppIdForSecGroup, securityGroup.getSecurityGroupId()));
            } else {
                LOG.info(String.format("Node (%s) RELEASING sec group %s as the nearest active network app node is now %s", currentNodeId.toStringFull(), securityGroup.getSecurityGroupId(), nearestActiveNodeId));
                PId securityGroupId = piIdBuilder.getPId(securityGroup).forLocalRegion();
                networkCleanupHandler.releaseSecurityGroup(securityGroupId);
            }
        }
    }
}
