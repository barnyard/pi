package com.bt.pi.app.instancemanager.handlers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.images.InstanceImageManager;
import com.bt.pi.app.instancemanager.libvirt.DomainNotFoundException;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.app.networkmanager.net.VirtualNetworkBuilder;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

/**
 * @see XenRefreshHandler sets itself as a watcher service and registers running Xen instances with the shared resource
 *      manager.
 */
@Component
public class XenRefreshHandler implements Runnable {
    private static final String ERROR_PROCESSING_INSTANCE_S = "Error processing instance %s";
    private static final String UNABLE_TO_RETRIEVE_INSTANCE_S = "Unable to retrieve instance: %s";
    private static final String UPDATING_INSTANCE_S_HOSTNAME_S_AND_NODE_ID_S = "Updating instance: %s hostname: %s and nodeId: %s";
    private static final int REPEATING_INTERVAL = 60 * 1000;
    private static final int INITIAL_INTERVAL = 5 * 1000;
    private static final Log LOG = LogFactory.getLog(XenRefreshHandler.class);
    private int initialIntervalMillis = INITIAL_INTERVAL;
    private int repeatingIntervalMillis = REPEATING_INTERVAL;
    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private InstanceImageManager instanceImageManager;
    @Resource
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Resource
    private WatcherService watcherService;
    @Resource
    private VirtualNetworkBuilder virtualNetworkBuilder;
    @Resource
    private InstanceManagerApplication instanceManagerApplication;
    @Resource
    private DhtClientFactory dhtClientFactory;
    private String nodeIdFull;

    public XenRefreshHandler() {
        this.piIdBuilder = null;
        this.instanceImageManager = null;
        this.consumedDhtResourceRegistry = null;
        this.watcherService = null;
        this.virtualNetworkBuilder = null;
        this.instanceManagerApplication = null;
        this.dhtClientFactory = null;
        this.nodeIdFull = null;
    }

    public void setInitialIntervalMillis(int intervalMillis) {
        initialIntervalMillis = intervalMillis;
    }

    public void setRepeatingIntervalMillis(int intervalMillis) {
        repeatingIntervalMillis = intervalMillis;
    }

    public void registerWatcher(String aNodeId) {
        LOG.debug(String.format("Registering XenRefreshHandler as a watcher. Initial Interval: %d - Repeating Interval: %d - NodeId: %s", initialIntervalMillis, repeatingIntervalMillis, aNodeId));
        nodeIdFull = aNodeId;
        this.watcherService.replaceTask("XenRefreshHandler", this, initialIntervalMillis, repeatingIntervalMillis);
    }

    @Override
    public void run() {
        processRunningInstances();
        processCrashedInstances();
    }

    private void processRunningInstances() {
        Collection<String> instanceIds = instanceImageManager.getRunningInstances();
        LOG.debug(String.format("Registering already running instances with shared resource manager: %s", instanceIds));
        for (final String instanceId : instanceIds) {
            try {
                final PId instancePiId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
                LOG.debug(String.format("XenRefreshHandler:Registering instance with id %s with shared resource manager", instanceId));

                final String finalHostname = getNodeHostname();

                this.consumedDhtResourceRegistry.registerConsumer(instancePiId, instanceId, Instance.class, new GenericContinuation<Boolean>() {
                    @Override
                    public void handleResult(Boolean result) {
                        Instance instance = consumedDhtResourceRegistry.getCachedEntity(instancePiId);
                        if (null == instance) {
                            LOG.warn(String.format("instance %s not found in ConsumedDhtResourceRegistry", instanceId));
                            return;
                        }
                        if (result) {
                            LOG.debug(String.format("Registered instance %s, now setting up virtual networks", instanceId));
                            virtualNetworkBuilder.setUpVirtualNetworkForInstance(instance.getVlanId(), instanceId);
                            try {
                                long domainId = instanceImageManager.getDomainIdForInstance(instanceId);
                                virtualNetworkBuilder.refreshXenVifOnBridge(instance.getVlanId(), domainId);
                            } catch (DomainNotFoundException e) {
                                LOG.warn(String.format("domain not found: %s", e.getMessage()));
                            }
                        }

                        if (null == instance.getNodeId() || !instance.getNodeId().equals(nodeIdFull) || InstanceState.CRASHED.equals(instance.getState())) {
                            LOG.debug(String.format(UPDATING_INSTANCE_S_HOSTNAME_S_AND_NODE_ID_S, instanceId, finalHostname, nodeIdFull));
                            updateInstanceNodeIdAndHostname(instanceId, instancePiId, nodeIdFull, finalHostname);
                        }

                        LOG.debug(String.format("Heartbeating instance %s to network manager", instanceId));
                        int instanceGlobalAvzCode = piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instance.getInstanceId());
                        PId networkAppIdForInstance = piIdBuilder.getPId(SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName())).forGlobalAvailablityZoneCode(instanceGlobalAvzCode);
                        instanceManagerApplication.newMessageContext().routePiMessageToApplication(networkAppIdForInstance, EntityMethod.UPDATE, instance, NetworkManagerApplication.APPLICATION_NAME);
                    }
                });
            } catch (Throwable t) {
                LOG.error(String.format(ERROR_PROCESSING_INSTANCE_S, instanceId), t);
            }
        }
    }

    private void processCrashedInstances() {
        LOG.debug("processCrashedInstances()");
        Collection<String> instanceIds = instanceImageManager.getCrashedInstances();
        if (instanceIds.size() < 1)
            return;
        LOG.debug(String.format("updating status for crashed instances: %s", instanceIds));
        for (final String instanceId : instanceIds) {
            try {
                final PId instancePiId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
                dhtClientFactory.createWriter().update(instancePiId, null, new UpdateResolvingPiContinuation<Instance>() {
                    @Override
                    public Instance update(Instance existingEntity, Instance requestedEntity) {
                        if (null == existingEntity) {
                            LOG.warn(String.format("instance %s not found in DHT", instanceId));
                            return null;
                        }

                        if (InstanceState.RUNNING.equals(existingEntity.getState())) {
                            existingEntity.setState(InstanceState.CRASHED);
                            return existingEntity;
                        }
                        return null;
                    }

                    @Override
                    public void handleResult(Instance result) {
                        if (null != result)
                            LOG.debug(String.format("Instance state for %s updated to %s", result.getInstanceId(), result.getState()));
                    }
                });
            } catch (Throwable t) {
                LOG.error(String.format(ERROR_PROCESSING_INSTANCE_S, instanceId), t);
            }
        }
    }

    private String getNodeHostname() {
        String hostname = "";

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.error("Unable to retrieve hostname", e);
        }

        return hostname;
    }

    private void updateInstanceNodeIdAndHostname(final String instanceId, final PId instancePiId, final String aNodeId, final String finalHostname) {
        dhtClientFactory.createWriter().update(instancePiId, null, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                if (null == existingEntity) {
                    LOG.warn(String.format(UNABLE_TO_RETRIEVE_INSTANCE_S, instanceId));
                    return null;
                }

                LOG.debug(String.format(UPDATING_INSTANCE_S_HOSTNAME_S_AND_NODE_ID_S, existingEntity.getInstanceId(), finalHostname, aNodeId));
                existingEntity.setNodeId(aNodeId);
                existingEntity.setHostname(finalHostname);
                if (InstanceState.CRASHED.equals(existingEntity.getState()))
                    existingEntity.setState(InstanceState.RUNNING);
                return existingEntity;
            }

            @Override
            public void handleResult(Instance result) {
                if (null == result) {
                    LOG.warn(String.format(UNABLE_TO_RETRIEVE_INSTANCE_S, instanceId));
                    return;
                }
                LOG.debug(String.format("Updated instance: %s hostname: %s and nodeId: %s", result.getInstanceId(), finalHostname, aNodeId));
            }
        });
    }
}
