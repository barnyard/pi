package com.bt.pi.app.common.entities;

public class DuplicateAvailabilityZoneException extends RuntimeException {
    private static final long serialVersionUID = 4874424971254939040L;

    public DuplicateAvailabilityZoneException(String message) {
        super(message);
    }
}
