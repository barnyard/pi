package com.bt.pi.app.networkmanager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public class TestNetworkManagerApplication extends NetworkManagerApplication {

    private BlockingQueue<PiEntity> deliveredMessages = new LinkedBlockingQueue<PiEntity>();

    @Override
    public void deliver(PId aId, ReceivedMessageContext receivedMessageContext) {
        System.err.println("deliver " + receivedMessageContext.getReceivedEntity());
        deliveredMessages.add(receivedMessageContext.getReceivedEntity());
        super.deliver(aId, receivedMessageContext);
    }

    public BlockingQueue<PiEntity> getDeliveredMessages() {
        return deliveredMessages;
    }

    public void clearDeliveredQueue() {
        deliveredMessages.clear();
    }
}

