/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.api.service;

public class CertificateGenerationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CertificateGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
