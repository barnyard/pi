package com.bt.pi.app.common.entities;

import java.util.Locale;

public enum SnapshotState {
    PENDING, CREATING, COMPLETE, ERROR, DELETING, DELETED, BURIED;

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.getDefault());
    }
}
