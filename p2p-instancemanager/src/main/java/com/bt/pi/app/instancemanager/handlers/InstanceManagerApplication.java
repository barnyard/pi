/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.handlers;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.AbstractPiCloudApplication;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.watchers.instance.InstanceRefreshHandler;
import com.bt.pi.app.instancemanager.images.InstanceImageManager;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.scope.NodeScope;

@Component
public class InstanceManagerApplication extends AbstractPiCloudApplication implements InstanceRefreshHandler, TerminateInstanceEventListener, ApplicationListener<NodeStartedEvent> {
    public static final String APPLICATION_NAME = "pi-instance-manager";
    private static final int TWO = 2;
    private static final Log LOG = LogFactory.getLog(InstanceManagerApplication.class);
    private static final int INSTANCE_TYPES_REFRESH_INTERVAL_MILLIS = 30000;
    private static final String DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION = "2";
    private static final String DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET = "1";
    @Resource(type = AlwaysOnApplicationActivator.class)
    private AlwaysOnApplicationActivator applicationActivator;
    private InstanceImageManager instanceImageManager;
    private AtomicBoolean nodeStarted;
    private String instancesDirectory;
    private String imagePath;
    @Resource
    private AnycastHandler anycastHandler;
    @Resource
    private DeliverHandler deliverHandler;
    @Resource
    private XenRefreshHandler xenRefreshHandler;
    @Resource
    private InstanceManagerApplicationQueueWatcherInitiator instanceManagerApplicationQueueWatcherInitiator;
    @Resource
    private InstanceManagerApplicationSubscriptionHelper instanceManagerApplicationSubscriptionHelper;
    private int queueWatchingApplicationsPerRegion = Integer.parseInt(DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION);
    private int queueWatchingApplicationsOffset = Integer.parseInt(DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET);
    private AtomicBoolean shouldRetainFailedInstanceArtifacts;

    public InstanceManagerApplication() {
        super(APPLICATION_NAME);
        this.nodeStarted = new AtomicBoolean(false);
        this.shouldRetainFailedInstanceArtifacts = new AtomicBoolean();
        this.instanceImageManager = null;
        this.anycastHandler = null;
        this.deliverHandler = null;
        this.xenRefreshHandler = null;
        this.instanceManagerApplicationQueueWatcherInitiator = null;
        this.instanceManagerApplicationSubscriptionHelper = null;
    }

    @Property(key = "instance.manager.queue.watching.applications.per.region", defaultValue = DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION)
    public void setQueueWatchingApplicationsPerRegion(int value) {
        this.queueWatchingApplicationsPerRegion = value;
    }

    @Property(key = "instance.manager.queue.watching.applications.offset", defaultValue = DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET)
    public void setQueueWatchinApplicationsOffset(int value) {
        this.queueWatchingApplicationsOffset = value;
    }

    @Resource
    public void setInstanceImageManager(InstanceImageManager aInstanceImageManager) {
        this.instanceImageManager = aInstanceImageManager;
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return this.applicationActivator;
    }

    @Override
    public void deliver(final PId id, final ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("deliver(%s)", receivedMessageContext));
        this.deliverHandler.deliver(id, receivedMessageContext);
    }

    // TODO: Remove node id from instance and remove instance as a shared entity
    void terminateInstance(Instance instance) {
        LOG.debug(String.format("terminateInstance(%s)", instance.getInstanceId()));
        this.deliverHandler.terminateInstance(instance);
    }

    void destroyInstance(Instance instance) {
        this.deliverHandler.destroyInstance(instance);
    }

    @Override
    protected void onApplicationStarting() {
        manageQueueWatcher();
        this.xenRefreshHandler.registerWatcher(getNodeIdFull());
    }

    private void manageQueueWatcher() {
        if (iAmAQueueWatchingApplication(this.queueWatchingApplicationsPerRegion, this.queueWatchingApplicationsOffset, NodeScope.AVAILABILITY_ZONE)) {
            instanceManagerApplicationQueueWatcherInitiator.initialiseWatchers(getNodeIdFull());
        } else {
            instanceManagerApplicationQueueWatcherInitiator.removeWatchers();
        }
    }

    @Override
    public boolean becomeActive() {
        LOG.debug("Starting application");

        try {
            checkInstanceAndImageDirectories();
        } catch (IllegalArgumentException ex) {
            LOG.error("Cannot find instance and/or image directories", ex);
            return false;
        }

        xenRefreshHandler.run();

        checkCapacityAndSubscribeUnSubscribe();

        return true;
    }

    @Scheduled(fixedDelay = INSTANCE_TYPES_REFRESH_INTERVAL_MILLIS)
    public void checkCapacityAndSubscribeUnSubscribe() {
        if (nodeStarted.get()) {
            instanceManagerApplicationSubscriptionHelper.checkCapacityAndSubscribeUnSubscribe();
        }
    }

    @Override
    public void becomePassive() {
    }

    private void checkInstanceAndImageDirectories() {
        checkInstanceDirectoryAndCreateIfNotExisting();
        checkImageDirectory();
    }

    @Property(key = "instances.directory")
    public void setInstanceDirectory(String value) {
        this.instancesDirectory = value;
    }

    @Property(key = "image.path", defaultValue = "var/images")
    public void setImagePath(String value) {
        this.imagePath = value;
    }

    private void checkInstanceDirectoryAndCreateIfNotExisting() {
        instanceImageManager.createDirectoryIfItDoesNotExist(this.instancesDirectory);
    }

    private void checkImageDirectory() {
        if (!instanceImageManager.doesDirectoryExist(this.imagePath))
            throw new IllegalArgumentException("Image path does not exist!");
    }

    @Override
    public boolean handleAnycast(final PubSubMessageContext pubSubMessageContext, final EntityMethod entityMethod, final PiEntity data) {
        LOG.debug(String.format("handleAnycast(%s, %s)", pubSubMessageContext, data));
        boolean result = this.anycastHandler.handleAnycast(pubSubMessageContext, data, getNodeIdFull());
        if (result) {
            checkCapacityAndSubscribeUnSubscribe();
        }
        return result;
    }

    @Override
    public void handleInstanceRefresh(Instance instance) {
        if (instance == null) {
            LOG.warn("Instance refreshed with a null instance, ignoring");
            return;
        }

        long interval = Instance.DEFAULT_REPEATING_INTERVAL_MILLIS;

        LOG.debug(String.format("instance %s, Current millis: %d, last heartbeat: %d, buried interval:%d", instance.getInstanceId(), System.currentTimeMillis(), instance.getLastHeartbeatTimestamp(), interval));

        if (instance.getState() == InstanceState.TERMINATED || instance.getState() == InstanceState.SHUTTING_DOWN) {
            // if instance is terminate or shutting down but still seems around, or the instance has crashed, in which
            // case we destroy it and archive it in the crashed folder.
            if (instance.getLastHeartbeatTimestamp() + (interval * TWO) < System.currentTimeMillis()) {
                LOG.debug(String.format("Destroying Instance. Instance %s is dead. Last heartbeat was at %d, state is %s", instance.getInstanceId(), instance.getLastHeartbeatTimestamp(), instance.getState()));
                destroyInstance(instance);
            } else {
                LOG.debug(String.format("Terminating Instance. Instance %s is dead. Last heartbeat was at %d, state is %s", instance.getInstanceId(), instance.getLastHeartbeatTimestamp(), instance.getState()));
                terminateInstance(instance);
            }
        } else {
            if (instance.isBuried() && instance.getState().ordinal() < InstanceState.FAILED.ordinal())
                LOG.info("Zombie instance detected: " + instance + " expecting ZombieinstanceChecker to report this.");
            if (instance.getState().ordinal() < InstanceState.SHUTTING_DOWN.ordinal())
                checkInstanceStateOnXen(instance);
        }
    }

    private void checkInstanceStateOnXen(final Instance instance) {
        LOG.debug(String.format("checkInstanceStateOnXen: %s state is %s in DHT", instance.getInstanceId(), instance.getState()));

        if (instance.isPending()) {
            LOG.info(String.format("%s instance is in pending state in DHT", instance.getInstanceId()));
            return;
        }

        if (instanceImageManager.isInstanceRunning(instance)) {
            getDhtClientFactory().createWriter().update(getPiIdBuilder().getPIdForEc2AvailabilityZone(instance), new UpdateResolvingPiContinuation<Instance>() {
                @Override
                public Instance update(Instance existingEntity, Instance requestedEntity) {
                    existingEntity.heartbeat();
                    return existingEntity;
                }

                @Override
                public void handleResult(Instance result) {
                    LOG.debug(String.format("Instance %s heartbeat complete", instance.getInstanceId()));
                    sendHeartbeatToNetworkManager();
                }

                @Override
                public void handleException(Exception e) {
                    sendHeartbeatToNetworkManager();
                    super.handleException(e);
                }

                private void sendHeartbeatToNetworkManager() {
                    LOG.debug(String.format("Sending heartbeat to network manager for instance %s", instance.getInstanceId()));
                    int instanceGlobalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(instance.getInstanceId());
                    PId networkManagerIdForInstance = getPiIdBuilder().getPId(SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName())).forGlobalAvailablityZoneCode(instanceGlobalAvzCode);
                    newMessageContext().routePiMessageToApplication(networkManagerIdForInstance, EntityMethod.UPDATE, instance, NetworkManagerApplication.APPLICATION_NAME);
                }
            });
        } else if (shouldRetainFailedInstanceArtifacts.get()) {
            LOG.warn(String.format("Instance %s was not found in xen but the DHT has it as running. We are preserving artifacts. ", instance));
        } else
            terminateInstance(instance);
    }

    @Property(key = "retain.failed.instance.artifacts", defaultValue = "true")
    public void setRetainFaileInstanceArtifacts(boolean shouldRetain) {
        shouldRetainFailedInstanceArtifacts.set(shouldRetain);
    }

    @Property(key = "instancemanager.activation.check.period.secs", defaultValue = DEFAULT_ACTIVATION_CHECK_PERIOD_SECS)
    public void setInstanceManagerActivationCheckPeriodSecs(int value) {
        super.setActivationCheckPeriodSecs(value);
    }

    @Property(key = "instancemanager.start.timeout.millis", defaultValue = DEFAULT_START_TIMEOUT_MILLIS)
    public void setInstanceManagerStartTimeoutMillis(long value) {
        super.setStartTimeoutMillis(value);
    }

    @Override
    public void instanceTerminated(Instance instance) {
        LOG.debug(String.format("instanceTerminated(%s)", instance));
        instanceManagerApplicationSubscriptionHelper.checkCapacityAndSubscribeUnSubscribe();
        anycastHandler.deRegisterInstanceWithSharedResourceManager(instance);
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
        LOG.warn(String.format("OH NOOOOOOOOS! Application: %s Node: %s has left the ring and we were supposed to do something!", InstanceManagerApplication.APPLICATION_NAME, nodeId));
        manageQueueWatcher();
    }

    @Override
    public void onApplicationEvent(NodeStartedEvent event) {
        nodeStarted.set(true);
    }

    @Override
    public void handleNodeArrival(String nodeId) {
        manageQueueWatcher();
    }
}
