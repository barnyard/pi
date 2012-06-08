/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.scattergather.PiScatterGatherContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class RebootInstanceServiceHelper extends ServiceHelperBase {
    private static final Log LOG = LogFactory.getLog(RebootInstanceServiceHelper.class);
    @Resource
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;

    public RebootInstanceServiceHelper() {
        scatterGatherContinuationRunner = null;
    }

    public boolean rebootInstances(final String ownerId, Collection<String> instanceIds, final ApiApplicationManager apiApplicationManager) {
        Collection<ScatterGatherContinuationRunnable> runnables = new ArrayList<ScatterGatherContinuationRunnable>();
        // send messages to the nodes that are rebooting.
        for (final String instanceId : instanceIds) {
            LOG.debug("Sending message to reboot: " + instanceId);
            final PId id = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
            final DhtReader dhtReader = getDhtClientFactory().createReader();

            final PiScatterGatherContinuation<Instance> scatterGatherContinuation = new PiScatterGatherContinuation<Instance>(new PiContinuation<Instance>() {
                @Override
                public void handleResult(Instance result) {
                    if (result != null) {
                        PId nodeId = getPiIdBuilder().getNodeIdFromNodeId(result.getNodeId());
                        if (ownerId.equals(result.getUserId())) {
                            result.setRestartRequested(true);
                            apiApplicationManager.newMessageContext().routePiMessageToApplication(nodeId, EntityMethod.UPDATE, result, InstanceManagerApplication.APPLICATION_NAME);
                        }
                    }
                }
            });

            ScatterGatherContinuationRunnable runnable = new ScatterGatherContinuationRunnable(scatterGatherContinuation) {
                @Override
                public void run() {
                    dhtReader.getAnyAsync(id, scatterGatherContinuation);
                }
            };
            runnables.add(runnable);
        }
        scatterGatherContinuationRunner.execute(runnables, 2, TimeUnit.MINUTES);
        return true;
    }
}
