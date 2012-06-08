package com.bt.pi.app.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bt.pi.app.common.util.SecurityUtils;

public class SecurityUtilsTest {
    private String certDn;
    private String keySigningAlgorithm;
    private String keyAlgorithm;
    private int keySize;

    private SecurityUtils securityUtils;

    private byte[] certAsByteArray = new byte[] { 48, -126, 3, -111, 48, -126, 2, 121, -96, 3, 2, 1, 2, 2, 6, 1, 34, -88, -124, 126, -8, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 13, 5, 0, 48, 127, 49, 11, 48, 9, 6, 3, 85, 4, 6, 19, 2, 85, 83,
            49, 11, 48, 9, 6, 3, 85, 4, 8, 19, 2, 67, 65, 49, 22, 48, 20, 6, 3, 85, 4, 7, 19, 13, 83, 97, 110, 116, 97, 32, 66, 97, 114, 98, 97, 114, 97, 49, 25, 48, 23, 6, 3, 85, 4, 10, 19, 16, 107, 111, 97, 108, 97, 45, 114, 111, 98, 117, 115,
            116, 110, 101, 115, 115, 49, 19, 48, 17, 6, 3, 85, 4, 11, 19, 10, 69, 117, 99, 97, 108, 121, 112, 116, 117, 115, 49, 27, 48, 25, 6, 3, 85, 4, 3, 19, 18, 119, 119, 119, 46, 101, 117, 99, 97, 108, 121, 112, 116, 117, 115, 46, 99, 111, 109,
            48, 30, 23, 13, 48, 57, 48, 55, 50, 51, 49, 54, 52, 57, 51, 49, 90, 23, 13, 49, 52, 48, 55, 50, 51, 49, 54, 52, 57, 51, 49, 90, 48, 127, 49, 11, 48, 9, 6, 3, 85, 4, 6, 19, 2, 85, 83, 49, 11, 48, 9, 6, 3, 85, 4, 8, 19, 2, 67, 65, 49, 22,
            48, 20, 6, 3, 85, 4, 7, 19, 13, 83, 97, 110, 116, 97, 32, 66, 97, 114, 98, 97, 114, 97, 49, 25, 48, 23, 6, 3, 85, 4, 10, 19, 16, 107, 111, 97, 108, 97, 45, 114, 111, 98, 117, 115, 116, 110, 101, 115, 115, 49, 19, 48, 17, 6, 3, 85, 4, 11,
            19, 10, 69, 117, 99, 97, 108, 121, 112, 116, 117, 115, 49, 27, 48, 25, 6, 3, 85, 4, 3, 19, 18, 119, 119, 119, 46, 101, 117, 99, 97, 108, 121, 112, 116, 117, 115, 46, 99, 111, 109, 48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9,
            13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126, 1, 10, 2, -126, 1, 1, 0, -102, 86, -42, -105, -103, -13, -125, -58, -35, 41, -113, 16, -107, -20, -24, -39, 41, -109, 81, -69, -30, -53, 118, 22, -51, -124, -127, 30, -28, 29, -120, 64,
            -86, 79, -11, -48, -27, -7, 93, -116, 28, 25, 113, -29, -84, 111, -25, 108, -99, 118, 91, -26, -13, 40, -119, -6, -20, -126, 29, 70, 101, 83, -108, 1, 3, 36, -67, 9, -49, 125, 121, 54, 38, 52, 36, 39, 118, 40, -108, 36, -85, -116, 67,
            -80, -45, 79, -94, -63, 125, 48, -126, -69, -5, 65, 16, -116, -44, -32, 113, 61, -78, -18, 94, -62, 39, 90, -98, -121, 56, -5, -114, 36, -38, 116, 19, 12, -111, 78, 12, 112, 72, 42, 116, -40, 99, -36, -55, -44, 79, 121, 38, -17, -61,
            -88, 126, 6, 28, -123, 27, -89, 8, 22, 124, 32, -42, 63, 125, 59, -26, -60, -42, 13, 68, -73, -53, -71, 91, -122, 64, -2, 57, 124, 81, 28, 26, -86, -14, -61, -68, 114, 115, -28, -68, 51, -104, 27, 55, 48, 111, -3, 33, -44, -119, 88, -44,
            -6, -72, -72, -20, -23, -114, 30, 80, 79, -93, 21, 92, 124, -56, -120, 38, -43, 94, -97, 4, 45, 64, -47, 69, 48, -60, 54, -84, -14, -23, 56, -106, 80, 38, -70, -60, -79, 105, -22, -120, -19, 95, 5, 103, 120, 99, -93, 10, 97, -37, 4,
            -121, 87, 114, 26, -34, -70, 121, 98, 75, -82, 20, 8, -32, 16, 110, -42, -116, 10, 45, -71, 2, 3, 1, 0, 1, -93, 19, 48, 17, 48, 15, 6, 3, 85, 29, 19, 1, 1, -1, 4, 5, 48, 3, 1, 1, -1, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 13, 5,
            0, 3, -126, 1, 1, 0, 109, -122, 104, 79, 83, 0, -120, -108, -76, -64, -73, 105, -37, -68, 57, -16, 110, 40, -90, 6, 0, -18, 41, -122, 51, -44, 22, -35, 1, -123, 115, 77, 31, -87, -18, -12, 5, -34, -102, -70, 112, 123, -125, -68, -93,
            -58, 36, 102, -113, 34, 42, 16, -4, -126, -43, 54, 90, 116, -117, 86, 85, -49, -78, 34, 77, -70, 40, 60, 68, -122, -53, 28, -64, -103, -2, 2, -103, 25, 93, -116, -93, 36, 10, -91, 109, -27, -15, 87, 112, 47, 53, 125, 24, 102, 91, -69, 6,
            -58, 90, -96, -19, -29, -2, -53, -105, 15, 80, -123, -120, -44, 58, -115, 21, -26, -5, 104, 110, -62, -87, -81, 96, 109, -41, 3, 24, 13, 121, -119, 78, 2, -36, 0, 125, -125, -2, -109, -106, 32, 17, 41, -35, 101, 97, -9, 87, -1, -12, 5,
            45, -85, 80, 67, 84, 55, -14, -99, 50, -4, -94, -12, -7, -104, -27, -31, -122, -36, 23, 2, -14, -88, -60, -101, 75, 4, 85, -94, 74, 4, 99, -3, 42, 114, 11, 14, -31, -106, 46, -34, -14, 71, -127, 27, 54, -107, -59, 114, -42, -48, -56, 51,
            110, -74, -99, 56, -65, 55, -55, -85, 86, 22, -27, -59, -9, -105, -77, -62, 75, 53, 124, -13, 57, -58, 17, -128, -112, -108, 125, -121, -97, 87, -44, 88, -65, 88, -55, 76, -92, -70, -47, 58, 83, 106, -9, -39, 67, 80, -128, 8, 103, 125,
            108, 39, 75, 0, 105, -125 };

    @BeforeClass
    public static void setupSecurityProvider() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void setup() {
        certDn = "CN=www.needs.to.change.com, OU=Pi, O=%s, L=London, ST=UK, C=UK";
        keySigningAlgorithm = "SHA512WithRSA";
        keyAlgorithm = "RSA";
        keySize = 2048;

        securityUtils = new SecurityUtils();
    }

    @Test
    public void shouldGetKeyPair() throws Exception {
        // act
        KeyPair result = securityUtils.getNewKeyPair("RSA", 512);

        // assert
        assertNotNull(result);
        assertNotNull(result.getPrivate());
        assertNotNull(result.getPublic());
    }

    @Test
    public void shouldGetFingerPrint() throws Exception {
        Key key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();

        // act
        String result = securityUtils.getFingerPrint(key);

        // assert
        assertNotNull(result);
    }

    @Test
    public void shouldGetPemBytes() throws Exception {
        // setup
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certAsByteArray));

        // act
        byte[] result = securityUtils.getPemBytes(certificate);

        // assert
        assertNotNull(result);
    }

    @Test
    public void shouldGetNewCertificate() throws Exception {
        // act
        Certificate certificate = securityUtils.getNewCertificate(certDn, keyAlgorithm, keySigningAlgorithm, keySize);

        // assert
        assertNotNull(certificate);
    }

    @Test(expected = GeneralSecurityException.class)
    public void shouldGetNewCertificateShouldThrowRuntimeExceptionIfKeyAlgorithmIsInvalid() throws Exception {
        // act
        securityUtils.getNewCertificate(certDn, "weird", keySigningAlgorithm, keySize);
    }

    @Test
    public void testGetPrivateKeyFromEncoded() throws GeneralSecurityException {
        // setup
        PrivateKey privateKey = this.securityUtils.getNewKeyPair("RSA", 512).getPrivate();
        byte[] bytes = privateKey.getEncoded();

        // act
        PrivateKey result = this.securityUtils.getPrivateKeyFromEncoded(bytes);

        // assert
        assertEquals(privateKey, result);
    }

    @Test
    public void shouldGenerateAnX509Certificate() throws GeneralSecurityException {
        // act
        Certificate certificate = securityUtils.getCertificateFromEncoded(certAsByteArray);

        // assert
        assertEquals("X.509", certificate.getType());
    }

    @Test
    public void shouldHaveSameEncodedValue() throws GeneralSecurityException {
        // act
        Certificate certificate = securityUtils.getCertificateFromEncoded(certAsByteArray);

        // assert
        byte[] encoded = certificate.getEncoded();
        for (int i = 0; i < certAsByteArray.length; i++) {
            assertEquals(certAsByteArray[i], encoded[i]);
        }
    }
}
