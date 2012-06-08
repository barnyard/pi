package com.bt.pi.app.instancemanager.handlers;

import com.bt.pi.app.common.entities.InstanceState;

public class InstanceStateTransition {

    private InstanceState previousState;
    private InstanceState nextState;

    public InstanceStateTransition() {
        previousState = null;
        nextState = null;
    }

    public InstanceStateTransition(InstanceState aPreviousState, InstanceState aNextState) {
        super();
        this.previousState = aPreviousState;
        this.nextState = aNextState;
    }

    /**
     * @return the previousState
     */
    public InstanceState getPreviousState() {
        return previousState;
    }

    /**
     * @param previousState
     *            the previousState to set
     */
    public void setPreviousState(InstanceState aPreviousState) {
        this.previousState = aPreviousState;
    }

    /**
     * @return the nextState
     */
    public InstanceState getNextState() {
        return nextState;
    }

    /**
     * @param nextState
     *            the nextState to set
     */
    public void setNextState(InstanceState aNextState) {
        this.nextState = aNextState;
    }

}
