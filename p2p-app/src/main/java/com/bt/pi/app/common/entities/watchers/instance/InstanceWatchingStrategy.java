package com.bt.pi.app.common.entities.watchers.instance;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.core.application.resource.AbstractResourceWatchingStrategy;
import com.bt.pi.core.application.resource.DefaultDhtResourceRefreshRunner;
import com.bt.pi.core.id.PId;

@Component
@Scope("prototype")
public class InstanceWatchingStrategy extends AbstractResourceWatchingStrategy<PId, Instance> {
    private static final Log LOG = LogFactory.getLog(InstanceWatchingStrategy.class);
    private InstanceRefreshHandler instanceRefreshHandler;

    public InstanceWatchingStrategy() {
        instanceRefreshHandler = null;
    }

    @Resource
    public void setInstanceRefreshHandler(InstanceRefreshHandler aInstanceRefreshHandler) {
        this.instanceRefreshHandler = aInstanceRefreshHandler;
    }

    @Override
    public Runnable getSharedResourceRefreshRunner(PId id) {
        LOG.debug(String.format("getSharedResourceRefreshRunner(%s)", id));
        InstanceRefreshContinuation refreshContinuation = new InstanceRefreshContinuation(instanceRefreshHandler);
        return new DefaultDhtResourceRefreshRunner<Instance>(id, getCachingConsumedResourceRegistry(), getLeasedResourceAllocationRecordHeartbeater(), refreshContinuation);
    }
}
