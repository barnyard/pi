package com.bt.pi.app.instancemanager.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.continuation.scattergather.UpdateResolvingPiScatterGatherContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

@Component
public class TerminateInstanceServiceHelper extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(TerminateInstanceServiceHelper.class);
    private static final int TERMINATE_INSTANCE_RETRIES = 5;
    private static final long SIXTY = 60;
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;
    private MessageContextFactory messageContextFactory;
    private TaskProcessingQueueHelper taskProcessingQueueHelper;

    public TerminateInstanceServiceHelper() {
        taskProcessingQueueHelper = null;
        this.scatterGatherContinuationRunner = null;
        this.messageContextFactory = null;
    }

    @Resource
    public void setScatterGatherContinuationRunner(ScatterGatherContinuationRunner aScatterGatherContinuationRunner) {
        this.scatterGatherContinuationRunner = aScatterGatherContinuationRunner;
    }

    @Resource(type = ReportingApplication.class)
    public void setMessageContextFactory(MessageContextFactory aMessageContextFactory) {
        this.messageContextFactory = aMessageContextFactory;
    }

    @Resource
    public void setTaskProcessingQueueHelper(TaskProcessingQueueHelper aTaskProcessingQueueHelper) {
        this.taskProcessingQueueHelper = aTaskProcessingQueueHelper;
    }

    public Map<String, InstanceStateTransition> terminateInstance(String ownerId, Collection<String> instanceIds) {
        LOG.debug(String.format("Terminating instances %s", instanceIds));

        final List<PId> ids = new ArrayList<PId>();

        for (final String instanceId : instanceIds) {
            ids.add(getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId)));
            // TODO:we could make this faster with a list implementation
            int globalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(instanceId);
            PId runInstanceQueueId = getPiIdBuilder().getPId(PiQueue.RUN_INSTANCE.getUrl()).forGlobalAvailablityZoneCode(globalAvzCode);
            PId terminateInstanceQueueId = getPiIdBuilder().getPId(PiQueue.TERMINATE_INSTANCE.getUrl()).forGlobalAvailablityZoneCode(globalAvzCode);
            PId instanceNetworkManagerTeardownId = getPiIdBuilder().getPId(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN.getUrl()).forGlobalAvailablityZoneCode(globalAvzCode);

            taskProcessingQueueHelper.removeUrlFromQueue(runInstanceQueueId, Instance.getUrl(instanceId));
            taskProcessingQueueHelper.addUrlToQueue(terminateInstanceQueueId, Instance.getUrl(instanceId), TERMINATE_INSTANCE_RETRIES);
            taskProcessingQueueHelper.addUrlToQueue(instanceNetworkManagerTeardownId, Instance.getUrl(instanceId), TERMINATE_INSTANCE_RETRIES);
        }

        Map<String, InstanceStateTransition> instanceStatusMap = new ConcurrentHashMap<String, InstanceStateTransition>();

        scatterWriter(ids, new TerminateInstancesContinuation(getPiIdBuilder(), messageContextFactory, instanceStatusMap));

        return instanceStatusMap;
    }

    public Map<String, InstanceStateTransition> terminateBuriedInstance(Collection<String> instanceIds) {
        LOG.debug(String.format("terminateBuriedInstance(%s)", instanceIds));

        List<PId> ids = new ArrayList<PId>();
        for (final String instanceId : instanceIds) {
            ids.add(getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId)));
        }
        Map<String, InstanceStateTransition> instanceStatusMap = new ConcurrentHashMap<String, InstanceStateTransition>();
        scatterWriter(ids, new TerminateBuriedInstancesContinuation(getPiIdBuilder(), messageContextFactory, instanceStatusMap));
        return instanceStatusMap;
    }

    protected <T extends PiEntity> void scatterWriter(final List<PId> ids, final UpdateResolvingPiContinuation<T> continuation) {
        LOG.debug(String.format("scatterWriter(%s, %s)", ids, continuation));
        List<ScatterGatherContinuationRunnable> runnables = new ArrayList<ScatterGatherContinuationRunnable>();
        for (final PId id : ids) {
            final UpdateResolvingPiScatterGatherContinuation<T> updateResolvingPiScatterGatherContinuation = new UpdateResolvingPiScatterGatherContinuation<T>(continuation);
            runnables.add(new ScatterGatherContinuationRunnable(updateResolvingPiScatterGatherContinuation) {
                @Override
                public void run() {
                    getDhtClientFactory().createWriter().update(id, updateResolvingPiScatterGatherContinuation);
                }
            });
        }
        scatterGatherContinuationRunner.execute(runnables, SIXTY, TimeUnit.SECONDS);
    }
}
