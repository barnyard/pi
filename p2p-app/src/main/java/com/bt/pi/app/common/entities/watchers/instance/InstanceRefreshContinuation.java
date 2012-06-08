package com.bt.pi.app.common.entities.watchers.instance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.core.continuation.PiContinuation;

public class InstanceRefreshContinuation extends PiContinuation<Instance> {
    private static final Log LOG = LogFactory.getLog(InstanceRefreshContinuation.class);
    private InstanceRefreshHandler instanceRefreshHandler;

    public InstanceRefreshContinuation(InstanceRefreshHandler aInstanceRefreshHandler) {
        instanceRefreshHandler = aInstanceRefreshHandler;
    }

    @Override
    public void handleResult(Instance result) {
        LOG.debug(String.format("Received result: %s", result));
        instanceRefreshHandler.handleInstanceRefresh(result);
    }
}
