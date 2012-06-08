package com.bt.pi.app.instancemanager.testing;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.id.PId;

public class StubNetworkManagerApplication extends NetworkManagerApplication {
    @Override
    public boolean becomeActive() {
        return true;
    }

    @Override
    public void becomePassive() {
    }

    @Override
    public void deliver(PId aId, ReceivedMessageContext receivedMessageContext) {
        Instance instance = (Instance) receivedMessageContext.getReceivedEntity();
        // instance.setVlanId(101);

        receivedMessageContext.sendResponse(EntityResponseCode.OK, instance);
    }
}
