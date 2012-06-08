/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.libvirt;

public class LibvirtManagerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LibvirtManagerException(String message) {
        super(message);
    }

    public LibvirtManagerException(String message, Exception e) {
        super(message, e);
    }
}
