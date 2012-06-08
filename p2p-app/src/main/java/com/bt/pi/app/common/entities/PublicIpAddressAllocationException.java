package com.bt.pi.app.common.entities;

public class PublicIpAddressAllocationException extends RuntimeException {
    private static final long serialVersionUID = -3092131204753324395L;

    public PublicIpAddressAllocationException(String message) {
        super(message);
    }
}
