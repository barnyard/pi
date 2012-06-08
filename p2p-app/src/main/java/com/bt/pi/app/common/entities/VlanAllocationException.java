/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

public class VlanAllocationException extends RuntimeException {
    private static final long serialVersionUID = -3108752903669563236L;

    public VlanAllocationException(Exception e) {
        super(e);
    }

    public VlanAllocationException(String message) {
        super(message);
    }
}
