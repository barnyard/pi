package com.bt.pi.app.common.entities;

public class AvailabilityZoneNotFoundException extends IllegalArgumentException {

    private static final long serialVersionUID = -1156804552126685350L;

    public AvailabilityZoneNotFoundException(String message) {
        super(message);
    }
}
