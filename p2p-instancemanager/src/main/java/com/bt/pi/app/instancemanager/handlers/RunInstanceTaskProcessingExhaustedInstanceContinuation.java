package com.bt.pi.app.instancemanager.handlers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueRetriesExhaustedContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@Component
public class RunInstanceTaskProcessingExhaustedInstanceContinuation implements TaskProcessingQueueRetriesExhaustedContinuation {
    private static final Log LOG = LogFactory.getLog(RunInstanceTaskProcessingQueueContinuation.class);
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private TaskProcessingQueueHelper taskProcessingQueueHelper;

    public RunInstanceTaskProcessingExhaustedInstanceContinuation() {
        this.dhtClientFactory = null;
        this.piIdBuilder = null;
        this.taskProcessingQueueHelper = null;
    }

    @Override
    public void receiveResult(String uri, String nodeId) {
        LOG.debug(String.format("receiveResult(%s, %s)", uri, nodeId));
        // mark instance as terminated... if not started ;)
        PId id = piIdBuilder.getPIdForEc2AvailabilityZone(uri);
        DhtWriter writer = dhtClientFactory.createWriter();
        final List<Instance> instanceList = new ArrayList<Instance>();
        writer.update(id, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                if (null == existingEntity)
                    return null;
                switch (existingEntity.getState()) {
                case RUNNING:
                    return null;
                case TERMINATED:
                case FAILED:
                    instanceList.add(existingEntity);
                    return null;
                default:
                    existingEntity.setState(InstanceState.FAILED);
                    instanceList.add(existingEntity);
                    return existingEntity;
                }
            };

            @Override
            public void handleResult(Instance result) {
                if (instanceList.size() > 0)
                    removeInstanceFromUser(instanceList.get(0));
            }
        });
    }

    private void removeInstanceFromUser(final Instance instance) {
        LOG.debug(String.format("removeInstanceFromUser(%s, %s)", instance.getInstanceId(), instance.getUserId()));
        PId id = piIdBuilder.getPId(User.getUrl(instance.getUserId()));
        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(id, new UpdateResolvingPiContinuation<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                if (null == existingEntity)
                    return null;
                if (existingEntity.terminateInstance(instance.getInstanceId()))
                    return existingEntity;
                return null;
            }

            @Override
            public void handleResult(User result) {
                if (null == result)
                    return;
                taskProcessingQueueHelper.addUrlToQueue(piIdBuilder.getPiQueuePId(PiQueue.REMOVE_INSTANCE_FROM_USER).forLocalScope(PiQueue.REMOVE_INSTANCE_FROM_USER.getNodeScope()), instance.getUrl(),
                        TerminateInstanceHandler.REMOVE_INSTANCE_FROM_USER_ENTITY_TASK_RETRIES);
            }
        });
    }
}
