package com.bt.pi.app.common.entities;

public class NoFreePublicAddressesAvailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoFreePublicAddressesAvailableException(String message, Exception e) {
        super(message, e);
    }
}
