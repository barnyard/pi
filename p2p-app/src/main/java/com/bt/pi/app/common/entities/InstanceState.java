package com.bt.pi.app.common.entities;

public enum InstanceState {

    PENDING("pending", 0), RUNNING("running", 16), CRASHED("crashed", 24), SHUTTING_DOWN("shutting-down", 32), FAILED(InstanceState.TERMINATED_DISPLAY_NAME, 48), TERMINATED(InstanceState.TERMINATED_DISPLAY_NAME, 48);

    private static final String TERMINATED_DISPLAY_NAME = "terminated";
    private final String name;
    private final int code;

    private InstanceState(String aName, int codeNum) {
        this.name = aName;
        this.code = codeNum;
    }

    public String getDisplayName() {
        return name;
    }

    public int getCode() {
        return code;
    }
}
