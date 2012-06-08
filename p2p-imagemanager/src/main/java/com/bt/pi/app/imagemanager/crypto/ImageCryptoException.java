/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.imagemanager.crypto;

public class ImageCryptoException extends RuntimeException {
    private static final long serialVersionUID = 2983613373012453988L;

    public ImageCryptoException(String message) {
        super(message);
    }

    public ImageCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
