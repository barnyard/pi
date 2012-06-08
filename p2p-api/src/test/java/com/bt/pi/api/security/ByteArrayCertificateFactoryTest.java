package com.bt.pi.api.security;

import static org.junit.Assert.assertEquals;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.api.security.ByteArrayCertificateFactory;

public class ByteArrayCertificateFactoryTest {

    private final ByteArrayCertificateFactory certificateFactory = new ByteArrayCertificateFactory();

    private Certificate certificate;

    @Before
    public void generateCertificate() throws CertificateException {
        certificate = certificateFactory.generateCertificate(CertTestHelper.DATA);
    }

    @Test
    public void shouldGenerateAnX509Certificate() throws CertificateException {
        assertEquals("X.509", certificate.getType());
    }

    @Test
    public void shouldHaveSameEncodedValue() throws CertificateException {
        byte[] encoded = certificate.getEncoded();
        for (int i = 0; i < CertTestHelper.DATA.length; i++) {
            assertEquals(CertTestHelper.DATA[i], encoded[i]);
        }
    }
}
