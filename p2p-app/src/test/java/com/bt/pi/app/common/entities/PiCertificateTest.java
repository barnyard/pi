package com.bt.pi.app.common.entities;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.parser.KoalaJsonParser;

public class PiCertificateTest {
    private PiCertificate piCert;
    private byte[] certificateBytes;
    private byte[] privateKeyBytes;
    private byte[] publicKeyBytes;

    @Before
    public void setup() {
        piCert = new PiCertificate();
        certificateBytes = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        privateKeyBytes = new byte[] { 8, 7, 6, 5 };
        publicKeyBytes = new byte[] { 4, 3, 2, 1 };
    }

    @Test
    public void testMultiArgumentConstructor() throws Exception {
        // setup
        piCert = new PiCertificate(certificateBytes, privateKeyBytes, publicKeyBytes);

        // assert
        assertThat(piCert.getCertificate(), equalTo(certificateBytes));
        assertThat(piCert.getPrivateKey(), equalTo(privateKeyBytes));
        assertThat(piCert.getPublicKey(), equalTo(publicKeyBytes));
    }

    @Test
    public void testGetType() throws Exception {
        // act
        String result = piCert.getType();

        // assert
        assertThat(result, equalTo("PiCertificate"));
    }

    @Test
    public void testGetUrl() throws Exception {
        // act
        String result = piCert.getUrl();

        // assert
        assertThat(result, equalTo("pcrt:all"));
    }

    @Test
    public void testGetCertificate() throws Exception {
        // setup
        piCert.setCertificate(certificateBytes);

        // act
        byte[] result = piCert.getCertificate();

        // assert
        assertThat(result, equalTo(certificateBytes));
    }

    @Test
    public void testGetPrivateKey() throws Exception {
        // setup
        piCert.setPrivateKey(privateKeyBytes);

        // act
        byte[] result = piCert.getPrivateKey();

        // assert
        assertThat(result, equalTo(privateKeyBytes));
    }

    @Test
    public void testGetPublicKey() throws Exception {
        // setup
        piCert.setPublicKey(publicKeyBytes);

        // act
        byte[] result = piCert.getPublicKey();

        // assert
        assertThat(result, equalTo(publicKeyBytes));
    }

    @Test
    public void testSerializeAndDeserialize() throws Exception {
        // setup
        piCert = new PiCertificate(certificateBytes, privateKeyBytes, publicKeyBytes);
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();

        // act
        String json = koalaJsonParser.getJson(piCert);

        // assert
        assertNotNull(json);
    }
}
