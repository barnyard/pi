package com.bt.pi.app.instancemanager.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManagerException;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.LoggingContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.NodeStartedEvent;

@Component
public class AnycastHandler extends AbstractHandler implements ApplicationListener<NodeStartedEvent> {
    private static final String FOUND_ENOUGH_RESOURCES_FOR_INSTANCE_S_RESERVING = "Found enough resources for instance %s, reserving...";
    private static final Log LOG = LogFactory.getLog(AnycastHandler.class);
    private static final int ONE_THOUSAND_TWENTY_FOUR = 1024;
    private static final long INSTANCE_TYPES_REFRESH_DELAY_MILLIS = 10 * 60 * 1000;
    private boolean started;

    @Resource
    private RunInstanceContinuationHandler runInstanceContinuationHandler;
    private SystemResourceState systemResourceState;
    private PId instanceTypesId;
    private InstanceTypes instanceTypes;

    public AnycastHandler() {
        systemResourceState = null;
        instanceTypesId = null;
        instanceTypes = null;
        runInstanceContinuationHandler = null;
    }

    @Resource
    public void setSystemResourceState(SystemResourceState aSystemResourceState) {
        this.systemResourceState = aSystemResourceState;
    }

    public boolean handleAnycast(final PubSubMessageContext pubSubMessageContext, final PiEntity data, final String nodeIdFull) {
        LOG.debug(String.format("handleAnycast(%s, %s, %s)", pubSubMessageContext, data, nodeIdFull));

        PId runInstancePId = getKoalaIdFactory().buildPId(PiTopics.RUN_INSTANCE.getPiLocation()).forLocalAvailabilityZone();
        LOG.debug("RunInstance topic PId: " + runInstancePId);
        if (!(runInstancePId.equals(pubSubMessageContext.getTopicPId()))) {
            LOG.debug("Received anycast message but unable to handle topic:" + pubSubMessageContext.getTopicPId());
            return false;
        }
        LOG.debug("Received RUN_INSTANCE message on RUN_INSTANCE topic. Instance: " + data);
        Reservation reservation;

        if (data instanceof Reservation) {
            reservation = (Reservation) data;
            int noOfInstancesAccepted = handleReservation(reservation, pubSubMessageContext, nodeIdFull);
            return noOfInstancesAccepted > 0;
        }

        if (data instanceof Instance) {
            return handleInstance((Instance) data, pubSubMessageContext, nodeIdFull);
        }

        return false;
    }

    private int handleReservation(final Reservation reservation, final PubSubMessageContext pubSubMessageContext, final String nodeIdFull) {
        LOG.debug(String.format("handleReservation(%s, %s, %s)", reservation, pubSubMessageContext, nodeIdFull));
        LOG.debug(String.format("Getting configuration for instance type %s, type id %s", reservation.getInstanceType(), instanceTypesId));
        InstanceTypeConfiguration requiredResourcesPerInstance = instanceTypes.getInstanceTypeConfiguration(reservation.getInstanceType());

        Set<String> setOfInstanceIds = reservation.getInstanceIds();
        LOG.info(String.format("Reservation of type %s received to spin up %d instances with ids: %s", reservation.getInstanceType(), setOfInstanceIds.size(), setOfInstanceIds.toString()));
        Collection<String> instancesToRun = removeSomeInstancesFromReservation(requiredResourcesPerInstance, setOfInstanceIds);
        if (!instancesToRun.isEmpty()) {
            try {
                handleReservationInstanceIds(instancesToRun, requiredResourcesPerInstance, pubSubMessageContext, nodeIdFull);
            } catch (Throwable t) {
                LOG.warn("Exception handling reservation", t);
                // if there is an exception we:
                // 1. Un reserve resources
                for (String instanceId : setOfInstanceIds) {
                    systemResourceState.unreserveResources(instanceId);
                }

                // 2. Re - anycast whole message
                return -1;
            }
            anycastRemainingInstanceIds(reservation, pubSubMessageContext);
        }
        return instancesToRun.size();

    }

    private boolean handleInstance(final Instance instance, PubSubMessageContext pubSubMessageContext, String nodeIdFull) {
        InstanceTypeConfiguration requiredResourcesPerInstance = instanceTypes.getInstanceTypeConfiguration(instance.getInstanceType());
        if (hasEnoughResources(requiredResourcesPerInstance)) {
            LOG.debug(String.format(FOUND_ENOUGH_RESOURCES_FOR_INSTANCE_S_RESERVING, instance.getInstanceId()));
            try {
                systemResourceState.reserveResources(instance.getInstanceId(), requiredResourcesPerInstance);
                handleReservationInstanceIds(Arrays.asList(new String[] { instance.getInstanceId() }), requiredResourcesPerInstance, pubSubMessageContext, nodeIdFull);
                return true;
            } catch (Throwable t) {
                LOG.warn("Exception handling instance", t);
                systemResourceState.unreserveResources(instance.getInstanceId());
                return false;
            }
        }
        return false;
    }

    private void anycastRemainingInstanceIds(final Reservation reservation, final PubSubMessageContext pubSubMessageContext) {
        if (!reservation.getInstanceIds().isEmpty()) {
            LOG.debug("Sending out anycast message for reservation because of insufficient resources on local node");
            getTaskExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    pubSubMessageContext.sendAnycast(EntityMethod.CREATE, reservation);
                }
            });
        }
    }

    private Collection<String> removeSomeInstancesFromReservation(final InstanceTypeConfiguration requiredResourcesPerInstance, final Set<String> instanceIdsReceived) {
        LOG.debug(String.format("removeSomeInstancesFromReservation(%s, %s)", requiredResourcesPerInstance, instanceIdsReceived));
        Collection<String> result = new ArrayList<String>();
        String[] instanceIds = new String[instanceIdsReceived.size()];
        instanceIds = instanceIdsReceived.toArray(instanceIds);
        for (int i = 0; i < instanceIds.length; i++) {
            if (requiredResourcesPerInstance == null) {
                LOG.warn("InstanceTypeConfiguration was null - ignoring request.");
                break;
            }
            if (hasEnoughResources(requiredResourcesPerInstance)) {
                LOG.debug(String.format(FOUND_ENOUGH_RESOURCES_FOR_INSTANCE_S_RESERVING, instanceIds[i]));
                systemResourceState.reserveResources(instanceIds[i], requiredResourcesPerInstance);
                instanceIdsReceived.remove(instanceIds[i]);
                result.add(instanceIds[i]);
            } else {
                LOG.info(String.format("Not enough resources to spin up an instance. Requested resources: %d cores, %dMB memory, %dMB disk.", requiredResourcesPerInstance.getNumCores(), requiredResourcesPerInstance.getMemorySizeInMB(),
                        requiredResourcesPerInstance.getDiskSizeInGB() * ONE_THOUSAND_TWENTY_FOUR));
                break;
            }
        }
        return result;
    }

    protected boolean hasEnoughResources(final InstanceTypeConfiguration requiredResourcesPerInstance) {
        try {
            LOG.debug(String.format("hasEnoughResources(InstanceTypeConfiguration %s) - Current SystemResourceState %s", requiredResourcesPerInstance, systemResourceState));
            int freeCores = systemResourceState.getFreeCores();
            long freeDiskInMB = systemResourceState.getFreeDiskInMB();
            long freeMemoryInMB = systemResourceState.getFreeMemoryInMB();
            LOG.debug(String.format("Remaining resources: %d cores, %dMB memory, %dMB disk", freeCores, freeMemoryInMB, freeDiskInMB));

            boolean result = (freeCores >= requiredResourcesPerInstance.getNumCores()) && (freeDiskInMB >= (1L * requiredResourcesPerInstance.getDiskSizeInGB() * ONE_THOUSAND_TWENTY_FOUR))
                    && (freeMemoryInMB >= requiredResourcesPerInstance.getMemorySizeInMB());
            LOG.debug("returning " + result);
            return result;
        } catch (LibvirtManagerException e) {
            LOG.warn(String.format("Unable to calculate remaining resources due to libvirt failure: %s", e));
        }

        return false;
    }

    protected void handleReservationInstanceIds(final Collection<String> instancesToRun, final InstanceTypeConfiguration instanceTypeConfiguration, final PubSubMessageContext pubSubMessageContext, final String nodeIdFull) {
        LOG.debug(String.format("handleReservationInstanceIds(%s, %s, %s, %s)", instancesToRun, instanceTypeConfiguration, pubSubMessageContext, nodeIdFull));
        for (final String instanceId : instancesToRun) {
            PId queueId = getPiIdBuilder().getPId(PiQueue.RUN_INSTANCE.getUrl()).forLocalScope(PiQueue.RUN_INSTANCE.getNodeScope());
            getTaskProcessingQueueHelper().setNodeIdOnUrl(queueId, Instance.getUrl(instanceId), nodeIdFull, new TaskProcessingQueueContinuation() {
                @Override
                public void receiveResult(String uri, String nodeId) {
                    final PId instancePiId = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
                    getDhtClientFactory().createWriter().update(instancePiId, new UpdateResolvingPiContinuation<Instance>() {
                        @Override
                        public Instance update(Instance existingEntity, Instance requestedEntity) {
                            existingEntity.setMemoryInKB(String.valueOf(instanceTypeConfiguration.getMemorySizeInMB() * ONE_THOUSAND_TWENTY_FOUR));
                            existingEntity.setImageSizeInMB(instanceTypeConfiguration.getDiskSizeInGB() * ONE_THOUSAND_TWENTY_FOUR);
                            existingEntity.setVcpus(instanceTypeConfiguration.getNumCores());
                            existingEntity.setInstanceType(instanceTypeConfiguration.getInstanceType());
                            return existingEntity;
                        }

                        @Override
                        public void handleResult(Instance result) {
                            LOG.debug("handleReservationInstanceIds-handleResult received: " + result);
                            if (null == result.getState() || (result.getState().equals(InstanceState.PENDING) && null == result.getNodeId())) {
                                int instanceGlobalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(result.getInstanceId());
                                PId networkManagerId = getPiIdBuilder().getPId(SecurityGroup.getUrl(result.getUserId(), result.getSecurityGroupName())).forGlobalAvailablityZoneCode(instanceGlobalAvzCode);
                                pubSubMessageContext.routePiMessageToApplication(networkManagerId, EntityMethod.CREATE, result, NetworkManagerApplication.APPLICATION_NAME, runInstanceContinuationHandler.getContinuation(nodeIdFull));
                                registerInstanceWithSharedResourceManager(instancePiId, result);
                            } else {
                                systemResourceState.unreserveResources(result.getInstanceId());
                            }
                        }
                    });
                }
            });
        }
    }

    public void deRegisterInstanceWithSharedResourceManager(Instance instance) {
        LOG.debug(String.format("DeRegistering instance with id %s with shared resource manager", instance.getInstanceId()));
        PId instancePastryId = getPiIdBuilder().getPIdForEc2AvailabilityZone(instance);
        getConsumedDhtResourceRegistry().deregisterConsumer(instancePastryId, instance.getInstanceId());
    }

    private void registerInstanceWithSharedResourceManager(final PId instancePiId, Instance instance) {
        LOG.debug(String.format("Registering instance with id %s with shared resource manager", instance.getInstanceId()));
        getConsumedDhtResourceRegistry().registerConsumer(instancePiId, instance.getInstanceId(), Instance.class, new LoggingContinuation<Boolean>());
    }

    @Scheduled(fixedDelay = INSTANCE_TYPES_REFRESH_DELAY_MILLIS)
    public void refreshInstanceTypes() {
        LOG.debug("refreshInstanceTypes()");
        if (null == instanceTypesId)
            return;
        if (!started)
            return;
        getInstanceTypesCache().get(instanceTypesId, new PiContinuation<InstanceTypes>() {
            @Override
            public void handleResult(InstanceTypes result) {
                instanceTypes = result;
            }
        });
    }

    @PostConstruct
    @DependsOn({ "piIdBuilder", "generalCache" })
    public void setupInstanceTypesId() {
        LOG.debug("setupInstanceTypesId()");
        instanceTypesId = getPiIdBuilder().getPId(InstanceTypes.URL_STRING);
    }

    protected InstanceTypes getInstanceTypes() {
        return instanceTypes;
    }

    @Override
    public void onApplicationEvent(NodeStartedEvent event) {
        LOG.debug(String.format("onApplicationEvent(%s)", event));
        started = true;
        refreshInstanceTypes();
    }
}
