package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class HeartbeatTimestampEntityBase implements HeartbeatTimestampResource {
    private static final Log LOG = LogFactory.getLog(HeartbeatTimestampEntityBase.class);
    private Long lastHeartbeatTimestamp;

    public HeartbeatTimestampEntityBase() {
        lastHeartbeatTimestamp = null;
    }

    @Override
    public Long getLastHeartbeatTimestamp() {
        return lastHeartbeatTimestamp;
    }

    public void setLastHeartbeatTimestamp(Long aLastHeartbeatTimestamp) {
        this.lastHeartbeatTimestamp = aLastHeartbeatTimestamp;
    }

    @Override
    public void heartbeat() {
        LOG.debug(String.format("heartbeat() for %s", getClass().getSimpleName()));
        setLastHeartbeatTimestamp(System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("lastHeartbeatTimestamp", lastHeartbeatTimestamp).toString();
    }
}
