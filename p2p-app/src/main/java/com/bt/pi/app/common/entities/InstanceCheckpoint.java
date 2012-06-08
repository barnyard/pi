package com.bt.pi.app.common.entities;

public class InstanceCheckpoint {
    private String state;

    public InstanceCheckpoint() {

    }

    public void setState(String aState) {
        this.state = aState;
    }

    public String getState() {
        return state;
    }
}
