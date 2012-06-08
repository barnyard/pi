/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.security;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import org.springframework.stereotype.Component;

/**
 * Exists to allow for mocking of CertificateFactory final methods. Also as a convenience for converting byte arrays
 * into certs without needing to create an InputStream from them first.
 */
@Component
public class ByteArrayCertificateFactory {

    private CertificateFactory certificateFactory;

    public ByteArrayCertificateFactory() {
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            // No way to recover
            throw new RuntimeException(e);
        }
    }

    public Certificate generateCertificate(byte[] data) throws CertificateException {
        return certificateFactory.generateCertificate(new ByteArrayInputStream(data));
    }
}
