package com.bt.pi.app.instancemanager.handlers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;

public class RunInstanceContinuation extends PiContinuation<Instance> {
    private static final Log LOG = LogFactory.getLog(RunInstanceContinuation.class);
    private RunInstanceHandler runInstanceHandler;
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;
    private String nodeId;
    private Executor executor;

    public RunInstanceContinuation() {
        super();
        this.runInstanceHandler = null;
        this.piIdBuilder = null;
        this.dhtClientFactory = null;
        this.nodeId = null;
        this.executor = null;
    }

    public void setRunInstanceHandler(RunInstanceHandler aRunInstanceHandler) {
        runInstanceHandler = aRunInstanceHandler;
    }

    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    public void setExecutor(Executor anExecutor) {
        this.executor = anExecutor;
    }

    public void setNodeId(String aNodeId) {
        this.nodeId = aNodeId;
    }

    protected String getNodeId() {
        return nodeId;
    }

    @Override
    public void handleResult(final Instance instance) {
        LOG.debug("Received Instance:" + instance.getUrl() + " from Network Manager");

        executor.execute(new Runnable() {
            public void run() {
                try {
                    DhtWriter dhtWriter = dhtClientFactory.createWriter();
                    dhtWriter.update(piIdBuilder.getPIdForEc2AvailabilityZone(instance), instance, new UpdateResolvingPiContinuation<Instance>() {
                        @Override
                        public Instance update(Instance existingEntity, Instance requestedEntity) {

                            String hostname = "";

                            try {
                                hostname = InetAddress.getLocalHost().getHostName();
                                existingEntity.setHostname(hostname);
                            } catch (UnknownHostException e) {
                                LOG.error("Unable to retrieve hostname", e);
                            }

                            LOG.debug(String.format("Updating instance: %s hostname: %s and nodeId: %s", existingEntity.getInstanceId(), hostname, nodeId));

                            existingEntity.setNodeId(nodeId);
                            existingEntity.setSourceImagePath(instance.getSourceImagePath());
                            existingEntity.setSourceKernelPath(instance.getSourceKernelPath());
                            existingEntity.setSourceRamdiskPath(instance.getSourceRamdiskPath());
                            existingEntity.setKernelId(instance.getKernelId());
                            existingEntity.setRamdiskId(instance.getRamdiskId());
                            existingEntity.setPlatform(instance.getPlatform());
                            return existingEntity;
                        }

                        @Override
                        public void handleResult(Instance result) {
                            LOG.debug(String.format("Run instances state update result: %s", result));
                        }
                    });

                    runInstanceHandler.startInstance(instance);

                } catch (Throwable t) {
                    LOG.error(t.getMessage(), t);
                }
            }
        });
    }
}
