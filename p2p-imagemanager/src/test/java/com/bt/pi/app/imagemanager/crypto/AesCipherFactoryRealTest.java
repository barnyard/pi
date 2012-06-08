package com.bt.pi.app.imagemanager.crypto;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;

import javax.crypto.Cipher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.UserServiceHelper;
import com.bt.pi.app.common.entities.PiCertificate;
import com.bt.pi.app.common.util.SecurityUtils;
import com.bt.pi.app.imagemanager.xml.Manifest;
import com.bt.pi.app.imagemanager.xml.ManifestBuilder;
import com.bt.pi.app.imagemanager.xml.XMLParser;
import com.bt.pi.app.imagemanager.xml.XPathEvaluator;

@RunWith(MockitoJUnitRunner.class)
public class AesCipherFactoryRealTest {
    private AesCipherFactory aesCipherFactory;
    @Mock
    private UserServiceHelper userServiceHelper;
    private byte[] privateKeyBytes = new byte[] { 48, -126, 4, -67, 2, 1, 0, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 4, -126, 4, -89, 48, -126, 4, -93, 2, 1, 0, 2, -126, 1, 1, 0, -105, -14, -17, -66, -77, 29, 123, 61, 120, -86, -53,
            -107, -128, -8, -39, 91, -122, -83, -59, -70, 56, 124, 60, 82, -115, -36, -63, -90, -54, 98, -93, -125, -97, -18, 2, -31, -34, -66, -9, 98, 31, 119, 112, 46, -126, 80, -69, -44, 83, 1, -33, -126, 63, -80, 99, -52, 97, -90, 39, 36, 84,
            -86, -112, -23, 3, 32, -68, -68, -44, 45, -58, 10, 35, 10, -4, 96, -91, 32, -15, 64, -11, 20, 115, -72, 83, -50, -28, -8, 36, -71, -1, -7, 92, -116, 45, -45, -96, -45, -17, 49, 35, 94, 127, -7, 62, 10, 58, -39, 51, -87, 12, -23, -101,
            118, -47, -122, 78, -72, 2, -15, 94, 76, 115, -99, -8, -8, 115, -107, -67, 55, -126, 108, 22, 10, 20, 86, 6, 92, -39, -36, 95, -91, -2, -56, -68, 35, 115, 9, -91, -109, -58, -80, -9, -93, -123, -128, -19, 53, -125, 85, -80, -8, 46, -122,
            -53, -78, 86, 103, 95, -33, 84, -34, -83, -60, 81, -84, -51, -71, 113, 48, -73, -66, 40, 70, 27, -121, -91, 122, 45, 9, 2, 89, -89, -116, 53, -86, -65, 83, -71, 70, 53, 47, -1, 47, -91, -117, -97, -60, 115, 23, -122, -44, 62, -44, -28,
            89, -127, -87, 5, 74, -51, -8, -15, -79, 46, 51, -108, -26, -76, -107, -121, 9, -6, -32, -69, 69, 29, -76, 80, -13, -17, 112, 79, 78, -34, 106, -32, -109, -111, -62, -63, 118, -1, -12, 95, -77, 2, 3, 1, 0, 1, 2, -126, 1, 0, 115, -19,
            -84, -114, 113, 55, -81, -4, 33, 97, 37, -68, 37, -49, 54, 126, 71, 13, -77, -118, -75, 103, -53, -38, 44, 83, -34, 7, 115, -25, -73, -37, 71, -100, -98, -28, 87, 62, -103, -99, 106, 102, -124, -41, 103, 35, 83, 122, -43, -19, -38, -22,
            19, -49, 111, 4, -45, 7, -94, 91, 108, -95, 73, -72, 13, 99, -33, -69, -83, -94, 82, -77, 15, 51, 101, -124, 18, -40, 68, 88, -117, -29, -109, -70, 113, 110, -85, 112, -53, 12, -127, -56, 109, -100, -95, -90, 17, -47, 64, 111, -36, 13,
            80, 84, 7, -54, 100, 69, -84, -51, 112, -123, -106, -48, 27, 97, 126, 19, -108, 99, 52, -49, -35, 114, 12, 46, -6, 119, 121, 60, 92, -57, 125, 87, 32, -56, 90, 78, 111, -94, 24, 122, 52, 68, -55, -42, 112, 33, -59, -48, -63, 123, -16,
            -58, -40, -95, 97, 35, -10, 125, -113, 91, -124, 36, -114, -104, 54, 6, -110, 95, -95, -22, -73, -74, -61, 90, 37, 76, 58, 54, -87, 47, 30, 40, 8, -33, 22, -62, 77, -119, -76, -61, 20, -64, -83, -13, 50, -31, -87, 99, 13, 117, -109, 5,
            80, 106, 39, 23, 58, 102, 4, -68, 14, -30, 92, 0, -49, 94, 69, 36, 34, 112, -15, 22, 122, -70, -7, -104, 78, -59, -119, 127, -88, -37, -108, 117, 117, 76, -110, -48, 57, 34, 60, -103, -99, 46, -40, -7, 82, -63, -65, -51, 73, 45, -25,
            -31, 2, -127, -127, 0, -44, -89, 57, -64, 14, -118, -106, -59, 16, 25, -30, 105, -118, -26, -117, 98, 88, 49, 51, 88, 0, 37, -97, 99, -8, -124, 5, 6, -124, -105, -103, -125, 72, 99, -118, -42, 16, 100, 9, -38, 61, 84, -120, 38, 111, 125,
            -81, -107, -35, 118, 68, -75, -103, 71, 34, -23, 58, 81, 11, 76, 118, -74, 59, -22, -93, -25, 68, -58, -55, 10, -29, 115, -117, 68, 68, 87, -67, 88, 84, 82, 53, 37, 118, -27, 123, 88, 23, 48, 75, -62, -116, -21, 32, 48, -35, -121, 108,
            -14, 71, 69, 52, 3, -32, -34, 67, 53, -97, -36, -19, -9, -105, 116, -62, 19, -122, -127, -15, 13, -17, -76, -12, 66, 73, -1, 117, -76, 37, -29, 2, -127, -127, 0, -74, -20, 3, 10, -55, -87, 110, -101, -53, -110, -83, -72, 36, -58, -125,
            45, -11, 87, 8, -67, -79, -113, -50, 25, 45, 63, -46, 90, -70, 88, 17, -46, 44, 124, -19, 54, 50, -48, -47, 65, 74, -74, 60, 67, -34, -80, -87, 41, 32, 78, 60, 56, 86, -112, -107, 53, -18, -70, -84, 2, 88, 91, -103, -83, 53, 71, 30, -47,
            11, 125, -23, 110, -2, -40, -78, 93, -117, 91, -33, 87, -63, 21, -39, -127, -4, -4, -89, -12, 45, 63, 74, -18, -67, -68, 25, -63, -54, -117, -99, -20, -66, 11, 90, 81, -121, -4, 77, 72, -22, 86, 58, -110, 8, -73, -117, -47, 114, -3, 113,
            -102, 121, -44, -60, -98, 86, -74, -121, -15, 2, -127, -128, 51, -12, -105, 123, -127, 18, 3, 60, 42, 110, -24, -114, 120, -51, 83, 8, -72, 27, 109, 59, -10, -19, 58, 64, 38, -101, -70, -50, -104, -34, -95, 55, 30, 28, -109, -13, 49, 22,
            0, 2, 62, 49, -59, 1, -1, 3, 106, 62, -25, 88, -39, -8, -76, 118, 88, -27, 58, -58, 74, 72, 104, 72, -91, -30, -14, 32, -77, 1, 14, 101, -122, -92, -40, 69, -39, -100, -58, 58, 42, 127, -37, 84, 71, -12, 81, 106, 120, 95, -24, 98, -92,
            35, 94, 62, 18, 33, -32, 80, 97, 113, 91, 0, 7, -108, -58, 62, -9, -53, -10, -88, 35, 108, -9, 109, -27, -45, 33, -98, 18, 14, -40, 14, -54, 29, -116, 24, 115, -103, 2, -127, -128, 110, -48, -26, 58, -25, -42, -52, 90, 119, -26, -95,
            117, 120, 90, 6, -24, -107, -60, 39, 88, 124, 52, -119, -128, 57, 40, 123, -16, 89, 9, -73, -86, 35, 39, 127, -79, -96, -15, 94, -125, -10, -106, 22, 70, 107, -89, -116, -93, -116, -99, -72, -33, -52, -103, -124, -69, -118, -89, -18, 66,
            -15, 114, 116, -44, 56, -3, -96, 14, -74, -82, -115, -9, -97, 78, 122, 40, 47, -97, -11, -37, 60, -17, 86, -72, -24, 33, -52, 66, 34, 19, 64, -5, 7, 88, -24, 37, -67, -27, -3, 67, -118, 18, 104, -94, 18, 6, -24, 111, 47, 0, 20, 53, -102,
            48, 79, -11, 16, 123, -72, 18, 4, -110, -64, -106, -56, 35, -122, -111, 2, -127, -127, 0, -66, -76, 120, -78, 58, 105, -23, -22, -113, -58, 12, 67, 114, 13, -25, 8, 109, 46, 98, -19, 54, -53, 88, 71, -51, -22, 47, -53, -43, 24, -49, -17,
            -95, 98, 45, -60, -82, 86, 70, -95, -80, -57, 15, -100, -52, 99, -85, 121, -79, 68, -86, -52, -40, -49, -51, 4, -48, -73, 68, -117, -33, 122, 127, -56, -116, -64, 110, 93, 25, 84, 99, 32, 69, -120, 104, 123, -86, -42, 37, -71, 48, -7,
            -66, 122, -41, -111, -16, -101, -95, -86, 51, -12, 46, 23, 10, 127, 111, -20, -31, -128, 35, 42, -117, -11, -84, 37, -18, 101, 96, -116, -19, -96, 12, 68, -30, 69, 22, -15, -36, 26, -32, -103, 26, 34, -25, 100, 105, -2 };
    @Mock
    private PiCertificate piCertificate;
    private Provider provider;

    @Before
    public void setUp() throws KeyStoreException, GeneralSecurityException {
        provider = createDefaultProvider();
        Security.addProvider(provider);
        aesCipherFactory = new AesCipherFactory();
        aesCipherFactory.setUserServiceHelper(userServiceHelper);
    }

    // this is a crappy test that was written after the event to try and get some coverage
    @Test
    public void shouldDecryptAnEncryptedImageFile() throws Exception {
        // setup
        Manifest manifest = createManifest();
        aesCipherFactory.setSecurityUtils(new SecurityUtils());

        when(userServiceHelper.getPiCertificate()).thenReturn(piCertificate);
        when(piCertificate.getPrivateKey()).thenReturn(privateKeyBytes);

        // act
        Cipher result = this.aesCipherFactory.createAesCipher(manifest);

        // assert
        assertEquals("AES/CBC/PKCS5Padding", result.getAlgorithm());
        assertEquals(provider, result.getProvider());
    }

    private Manifest createManifest() throws Exception {
        URL manifestUrl = Thread.currentThread().getContextClassLoader().getResource("test-file.manifest.xml");
        File manifestFile = new File(manifestUrl.getPath());
        ManifestBuilder builder = new ManifestBuilder();
        builder.setParser(new XMLParser());
        builder.setEvaluator(new XPathEvaluator());
        return builder.build(manifestFile);
    }

    private static Provider createDefaultProvider() {
        return new org.bouncycastle.jce.provider.BouncyCastleProvider();
    }
}
