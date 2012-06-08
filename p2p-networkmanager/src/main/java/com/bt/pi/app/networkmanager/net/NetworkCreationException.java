/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.net;

public class NetworkCreationException extends RuntimeException {
    private static final long serialVersionUID = -3108752903669563236L;

    public NetworkCreationException(String message, Exception e) {
        super(message, e);
    }

    public NetworkCreationException(String message) {
        super(message);
    }
}
