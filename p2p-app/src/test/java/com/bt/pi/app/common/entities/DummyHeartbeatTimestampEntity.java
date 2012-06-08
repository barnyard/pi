package com.bt.pi.app.common.entities;

import org.codehaus.jackson.annotate.JsonIgnore;

public class DummyHeartbeatTimestampEntity extends HeartbeatTimestampEntityBase {
    private String id;

    public DummyHeartbeatTimestampEntity() {
    }

    public DummyHeartbeatTimestampEntity(String aId) {
        id = aId;
    }

    public String getId() {
        return id;
    }

    public void setId(String aId) {
        this.id = aId;
    }

    @Override
    @JsonIgnore
    public boolean isConsumedBy(String consumerId) {
        return consumerId.equals(id);
    }
}
