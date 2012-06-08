package com.bt.pi.app.common.entities.watchers.instance;

import com.bt.pi.app.common.entities.Instance;

public interface InstanceRefreshHandler {
    void handleInstanceRefresh(Instance instance);
}
