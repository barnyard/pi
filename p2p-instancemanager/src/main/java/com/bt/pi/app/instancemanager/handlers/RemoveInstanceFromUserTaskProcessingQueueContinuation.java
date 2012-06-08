package com.bt.pi.app.instancemanager.handlers;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.watchers.RemoveResourceFromUserTaskProcessingQueueContinuationBase;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.id.PId;

@Component
public class RemoveInstanceFromUserTaskProcessingQueueContinuation extends RemoveResourceFromUserTaskProcessingQueueContinuationBase<Instance> {

    public RemoveInstanceFromUserTaskProcessingQueueContinuation() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected String getOwnerId(Instance result) {
        return result.getUserId();
    }

    @Override
    protected PiQueue getPiQueueForResource() {
        return PiQueue.REMOVE_INSTANCE_FROM_USER;
    }

    @Override
    protected String getResourceId(Instance resource) {
        return resource.getInstanceId();
    }

    @Override
    protected PId getResourcePastryId(String uri) {
        return getPiIdBuilder().getPIdForEc2AvailabilityZone(uri);
    }

    @Override
    protected boolean removeResourceFromUser(User user, Instance resource) {
        return user.removeInstance(resource.getInstanceId());
    }

    @Override
    protected boolean setResourceStatusToBuried(Instance resource) {
        return false;
    }

    @Override
    protected void handleResultForUser(String uri, PId removeResourceFromUserQueueId, PId resourcePastryId, Instance result, String ownerId) {
        removeTaskProcessingQueue(removeResourceFromUserQueueId, uri);
    }
}