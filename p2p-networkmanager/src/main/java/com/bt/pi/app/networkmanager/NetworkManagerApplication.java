package com.bt.pi.app.networkmanager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import rice.p2p.commonapi.Id;

import com.bt.pi.app.common.AbstractManagedAddressingPiApplication;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.networkmanager.addressing.resolution.AddressDeleteQueue;
import com.bt.pi.app.networkmanager.handlers.NetworkCleanupHandler;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageForwardAction;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedSharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

@Component
public class NetworkManagerApplication extends AbstractManagedAddressingPiApplication {
    public static final String APPLICATION_NAME = "pi-network-manager";
    private static final String DEFAULT_START_TIMEOUT_MILLIS = "15000";
    private static final String DEFAULT_ACTIVATION_CHECK_PERIOD_SECS = "30";
    private static final Log LOG = LogFactory.getLog(NetworkManagerApplication.class);
    private static final String DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION = "4";
    private static final String DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET = "2";
    private static final long TEN = 10;
    private static final int SECOND = 1000;
    private static final int SIXTY = 60;
    private static final long MINUTE = SIXTY * SECOND;
    private static final long REFRESH_INTERVAL_MILLIS = TEN * MINUTE;
    @Resource
    private NetworkResourceWatcherInitiator networkResourceWatcherInitiator;
    @Resource
    private NetworkCleanupHandler networkCleanupHandler;
    @Resource
    private AssociateAddressTaskQueueWatcherInitiator associateAddressTaskQueueWatcherInitiator;
    @Resource
    private DisassociateAddressTaskQueueWatcherInitiator disassociateAddressTaskQueueWatcherInitiator;
    @Resource
    private InstanceNetworkManagerTeardownTaskQueueWatcherInitiator instanceNetworkManagerTeardownTaskQueueWatcherInitiator;
    @Resource
    private NetworkManagerAppDeliveredMessageDispatcher networkManagerAppDeliveredMessageDispatcher;
    private int queueWatchingApplicationsPerRegion = Integer.parseInt(DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION);
    private int queueWatchingApplicationsOffset = Integer.parseInt(DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET);
    @Resource
    private SecurityGroupUpdateTaskQueueWatcherInitiator securityGroupUpdateTaskQueueWatcherInitiator;
    @Resource
    private SecurityGroupDeleteTaskQueueWatcherInitiator securityGroupDeleteTaskQueueWatcherInitiator;
    @Resource(type = AvailabilityZoneScopedSharedRecordConditionalApplicationActivator.class)
    private AvailabilityZoneScopedSharedRecordConditionalApplicationActivator applicationActivator;
    @Resource
    private IpTablesManager ipTablesManager;
    @Resource
    private AddressDeleteQueue addressDeleteQueue;
    private AtomicBoolean active;

    public NetworkManagerApplication() {
        super(APPLICATION_NAME);
        networkResourceWatcherInitiator = null;
        networkCleanupHandler = null;
        associateAddressTaskQueueWatcherInitiator = null;
        disassociateAddressTaskQueueWatcherInitiator = null;
        networkManagerAppDeliveredMessageDispatcher = null;
        securityGroupDeleteTaskQueueWatcherInitiator = null;
        securityGroupUpdateTaskQueueWatcherInitiator = null;
        instanceNetworkManagerTeardownTaskQueueWatcherInitiator = null;
        applicationActivator = null;
        ipTablesManager = null;
        addressDeleteQueue = null;
        this.active = new AtomicBoolean(false);
    }

    @Property(key = "network.manager.queue.watching.applications.per.region", defaultValue = DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION)
    public void setQueueWatchingApplicationsPerRegion(int value) {
        this.queueWatchingApplicationsPerRegion = value;
    }

    @Property(key = "network.manager.queue.watching.applications.offset", defaultValue = DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET)
    public void setQueueWatchinApplicationsOffset(int value) {
        this.queueWatchingApplicationsOffset = value;
    }

    @Override
    public SharedRecordConditionalApplicationActivator getActivatorFromApplication() {
        return applicationActivator;
    }

    @Override
    protected void onApplicationStarting() {
        LOG.info(String.format("Application %s creating and initialising queue watcher", getApplicationName()));
        manageQueueWatcher();
    }

    private void manageQueueWatcher() {
        if (iAmAQueueWatchingApplication(this.queueWatchingApplicationsPerRegion, this.queueWatchingApplicationsOffset, NodeScope.REGION)) {
            associateAddressTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(getNodeId().toStringFull());
            disassociateAddressTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(getNodeId().toStringFull());
            securityGroupUpdateTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(getNodeId().toStringFull());
            securityGroupDeleteTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(getNodeId().toStringFull());
            instanceNetworkManagerTeardownTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(getNodeId().toStringFull());
        } else {
            associateAddressTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
            disassociateAddressTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
            securityGroupUpdateTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
            securityGroupDeleteTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
            instanceNetworkManagerTeardownTaskQueueWatcherInitiator.removeTaskProcessingQueueWatcher();
        }
    }

    @Override
    public boolean becomeActive() {
        LOG.debug(String.format("becomeActive()"));
        if (!callSuperBecomeActive()) {
            LOG.debug(String.format("Become active failed in base"));
            return false;
        }

        ApplicationRecord cachedApplicationRecord = ((SharedRecordConditionalApplicationActivator) getApplicationActivator()).getApplicationRegistry().getCachedApplicationRecord(this.getApplicationName());
        int requiredActiveApps = 1;
        if (cachedApplicationRecord != null) {
            requiredActiveApps = cachedApplicationRecord.getRequiredActive();
            LOG.debug(String.format("Setting required number of active apps to %s from cached app record %s", requiredActiveApps, cachedApplicationRecord));
        } else {
            LOG.warn(String.format("No cached app record found for %s - using default of 1 required active app when initializing watchers", getApplicationName()));
        }

        LOG.info(String.format("Network manager app subscribing to network managers in region topic"));
        subscribe(PiTopics.NETWORK_MANAGERS_IN_REGION.getPiLocation(), this);

        networkResourceWatcherInitiator.initiateWatchers(requiredActiveApps);
        active.set(true);
        return true;
    }

    @Scheduled(fixedDelay = REFRESH_INTERVAL_MILLIS)
    public void refreshIpTablesAtRegularIntervals() {
        if (active.get())
            ipTablesManager.refreshIpTables();
    }

    protected boolean callSuperBecomeActive() {
        return super.becomeActive();
    }

    @Override
    public void becomePassive() {
        LOG.debug(String.format("becomePassive()"));
        active.set(false);
        super.becomePassive();

        LOG.info(String.format("Network manager app unsubscribing from network managers in region topic"));
        unsubscribe(PiTopics.NETWORK_MANAGERS_IN_REGION.getPiLocation(), this);

        networkCleanupHandler.releaseAllSecurityGroups();
    }

    @Override
    protected void onApplicationShuttingDown() {
        LOG.debug(String.format("onApplicationShuttingDown()"));
        super.onApplicationShuttingDown();
        try {
            networkCleanupHandler.releaseAllSecurityGroups();
            addressDeleteQueue.removeAllAddressesInQueueOnShuttingDown();
        } catch (NullPointerException npe) {
            LOG.error("Something went wrong.", npe);
        }
    }

    @Override
    public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("deliver(Id- %s, RecievedMessageContext - %s)", id, receivedMessageContext));
        handleReceivedMessage(receivedMessageContext, receivedMessageContext.getMethod(), receivedMessageContext.getReceivedEntity());
    }

    @Override
    public void deliver(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity piEntity) {
        LOG.debug(String.format("deliver(%s, %s, %s)", pubSubMessageContext, entityMethod, piEntity));
        handleReceivedMessage(pubSubMessageContext, entityMethod, piEntity);
    }

    private void handleReceivedMessage(MessageContext messageContext, EntityMethod method, PiEntity entity) {
        ApplicationStatus applicationStatus = getApplicationActivator().getApplicationStatus(this.getApplicationName());
        if (applicationStatus.equals(ApplicationStatus.ACTIVE)) {
            networkManagerAppDeliveredMessageDispatcher.dispatchToHandler(messageContext, method, entity);
        } else {
            LOG.warn(String.format("Holy bread crumb, message %s with method %s delivered to non-live app!", entity.getType(), method));
            // TODO: may what to do something other than ignore, which relies on client retrying... do we want to
            // forward, redirect, or try to handle?
        }
    }

    @Override
    public MessageForwardAction forwardPiMessage(boolean isForThisNode, Id destinationNodeId, PiEntity payload) {
        MessageForwardAction messageForwardAction = new MessageForwardAction(true);

        if (!isForThisNode) {
            LOG.info(String.format("Network app forwarding message %s as it is not for this node", payload));
            return messageForwardAction;
        }

        Id closestActiveNodeId = ((SharedRecordConditionalApplicationActivator) getApplicationActivator()).getClosestActiveApplicationNodeId(getApplicationName(), destinationNodeId);
        if (closestActiveNodeId == null) {
            LOG.info(String.format("Forward doing nothing as zero active apps"));
            return messageForwardAction;
        }

        if (!closestActiveNodeId.equals(destinationNodeId))
            messageForwardAction = new MessageForwardAction(true, closestActiveNodeId);

        return messageForwardAction;
    }

    @Property(key = "networkmanager.activation.check.period.secs", defaultValue = DEFAULT_ACTIVATION_CHECK_PERIOD_SECS)
    public void setNetworkManagerActivationCheckPeriodSecs(int value) {
        super.setActivationCheckPeriodSecs(value);
    }

    @Property(key = "networkmanager.start.timeout.millis", defaultValue = DEFAULT_START_TIMEOUT_MILLIS)
    public void setNetworkManagerStartTimeoutMillis(long value) {
        super.setStartTimeoutMillis(value);
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
        removeNodeIdFromApplicationRecord(nodeId);
        // TODO: should this be removing node id from queue items like the other apps do?
        manageQueueWatcher();
    }

    @Override
    public void handleNodeArrival(String nodeId) {
        manageQueueWatcher();
    }

    @Override
    public List<String> getPreferablyExcludedApplications() {
        return Arrays.asList("pi-sss-manager", "pi-api-manager");
    }
}
