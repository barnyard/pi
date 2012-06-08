package com.bt.pi.app.instancemanager.handlers;

import com.bt.pi.app.common.entities.Instance;

public interface TerminateInstanceEventListener {
    void instanceTerminated(Instance instance);
}
