package com.bt.pi.app.common.entities;

import java.util.Locale;

public enum VolumeState {
    CREATING, AVAILABLE, AVAILABLE_SNAPSHOTTING, ATTACHING, IN_USE, IN_USE_SNAPSHOTTING, DETACHING, FORCE_DETACHING, DELETING, DELETED, FAILED, BURIED;

    private static final String HYPHEN = "-";
    private static final String UNDERSCORE = "_";

    @Override
    public String toString() {
        return name().replaceAll(UNDERSCORE, HYPHEN).toLowerCase(Locale.getDefault());
    }

    public static VolumeState getValue(String value) {
        return valueOf(value.replaceAll(HYPHEN, UNDERSCORE).toUpperCase(Locale.getDefault()));
    }
}
