/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.images;

public class ImageLoaderException extends RuntimeException {

    private static final long serialVersionUID = 3486962493808570197L;

    public ImageLoaderException(String message) {
        super(message);
    }

    public ImageLoaderException(String message, Exception e) {
        super(message, e);
    }
}
