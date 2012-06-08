package com.bt.pi.app.volumemanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public abstract class RemoveResourceFromUserTaskProcessingQueueContinuationBase<T extends PiEntity> implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(RemoveResourceFromUserTaskProcessingQueueContinuationBase.class);

    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private TaskProcessingQueueHelper taskProcessingQueueHelper;

    public RemoveResourceFromUserTaskProcessingQueueContinuationBase() {
        piIdBuilder = null;
        dhtClientFactory = null;
        taskProcessingQueueHelper = null;
    }

    protected PiIdBuilder getPiIdBuilder() {
        return piIdBuilder;
    }

    @Override
    public void receiveResult(final String uri, String nodeId) {
        LOG.debug(String.format("Received queue item for removing resource from user: Url: %s, NodeId: %s", uri, nodeId));

        PiQueue queue = getPiQueueForResource();
        final PId removeResourceFromUserQueueId = piIdBuilder.getPiQueuePId(queue).forLocalScope(queue.getNodeScope());
        taskProcessingQueueHelper.setNodeIdOnUrl(removeResourceFromUserQueueId, uri, nodeId);

        final PId resourcePastryId = getResourcePastryId(uri);
        dhtClientFactory.createReader().getAsync(resourcePastryId, new PiContinuation<T>() {
            @Override
            public void handleResult(final T result) {
                final String ownerId = getOwnerId(result);
                PId userId = piIdBuilder.getPId(User.getUrl(ownerId));

                dhtClientFactory.createWriter().update(userId, new UpdateResolvingPiContinuation<User>() {
                    @Override
                    public User update(User existingEntity, User requestedEntity) {
                        if (null == existingEntity) {
                            LOG.error("Unable to find user:" + ownerId);
                            return null;
                        }
                        LOG.debug(String.format("Removing resource id: %s from User: %s", getResourceId(result), existingEntity.getUsername()));
                        if (removeResourceFromUser(existingEntity, result))
                            return existingEntity;

                        return null;
                    }

                    @Override
                    public void handleResult(User user) {
                        LOG.debug(String.format("Resource %s removed from user %s", getResourceId(result), ownerId));
                        dhtClientFactory.createWriter().update(resourcePastryId, new UpdateResolvingPiContinuation<T>() {
                            @Override
                            public T update(T existingEntity, T requestedEntity) {
                                if (null == existingEntity) {
                                    LOG.error("Unable to find resource: " + uri);
                                    return null;
                                }
                                if (setResourceStatusToBuried(existingEntity))
                                    return existingEntity;

                                return null;
                            }

                            @Override
                            public void handleResult(T result) {
                                if (result != null) {
                                    LOG.debug(String.format("Resource %s status set to buried", getResourceId(result)));
                                    taskProcessingQueueHelper.removeUrlFromQueue(removeResourceFromUserQueueId, result.getUrl());
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    protected abstract String getResourceId(T resource);

    protected abstract PId getResourcePastryId(String uri);

    protected abstract PiQueue getPiQueueForResource();

    protected abstract String getOwnerId(T result);

    protected abstract boolean removeResourceFromUser(User user, T resource);

    protected abstract boolean setResourceStatusToBuried(T resource);
}
