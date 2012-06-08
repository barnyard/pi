package com.bt.pi.app.instancemanager.libvirt;

public class DomainNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 7473977759431404165L;

    public DomainNotFoundException() {
        super();
    }

    public DomainNotFoundException(String message) {
        super(message);
    }
}
