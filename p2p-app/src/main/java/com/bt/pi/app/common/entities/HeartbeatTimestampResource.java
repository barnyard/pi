package com.bt.pi.app.common.entities;

public interface HeartbeatTimestampResource {
    void heartbeat();

    Long getLastHeartbeatTimestamp();

    boolean isConsumedBy(String consumerId);
}
