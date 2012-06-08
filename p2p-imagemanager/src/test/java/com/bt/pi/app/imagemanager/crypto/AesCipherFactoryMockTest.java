package com.bt.pi.app.imagemanager.crypto;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bt.pi.app.common.UserServiceHelper;
import com.bt.pi.app.common.entities.PiCertificate;
import com.bt.pi.app.common.util.SecurityUtils;
import com.bt.pi.app.imagemanager.xml.Manifest;
import com.bt.pi.app.imagemanager.xml.ManifestBuilder;
import com.bt.pi.app.imagemanager.xml.XMLParser;
import com.bt.pi.app.imagemanager.xml.XPathEvaluator;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Cipher.class)
public class AesCipherFactoryMockTest {
    private Cipher rsaCipher;
    private Cipher aesCipher;
    private AesCipherFactory aesCipherFactory;
    @Mock
    private UserServiceHelper userServiceHelper;
    private byte[] privateKeyBytes = new byte[] { 48, -126 };
    @Mock
    private PiCertificate piCertificate;
    @Mock
    private SecurityUtils securityUtils;

    @Before
    public void setUp() throws KeyStoreException, GeneralSecurityException {
        Security.addProvider(createDefaultProvider());
        rsaCipher = PowerMockito.mock(Cipher.class);
        aesCipher = PowerMockito.mock(Cipher.class);
        aesCipherFactory = new AesCipherFactory() {
            @Override
            protected Cipher createRsaCipher() throws GeneralSecurityException {
                return rsaCipher;
            }

            @Override
            protected Cipher createAesCipher() throws GeneralSecurityException {
                return aesCipher;
            }
        };
        aesCipherFactory.setUserServiceHelper(userServiceHelper);
        aesCipherFactory.setSecurityUtils(securityUtils);
    }

    @Test
    public void testDecrypt() throws Exception {
        // setup
        Manifest manifest = createManifest();

        PrivateKey pk = mock(PrivateKey.class);
        when(securityUtils.getPrivateKeyFromEncoded(privateKeyBytes)).thenReturn(pk);
        when(userServiceHelper.getPiCertificate()).thenReturn(piCertificate);
        when(piCertificate.getPrivateKey()).thenReturn(privateKeyBytes);

        final byte[] decryptedKey = "4f2e".getBytes();
        final byte[] decryptedIv = "ddee".getBytes();
        when(rsaCipher.doFinal((byte[]) Matchers.anyObject())).thenReturn(decryptedKey).thenReturn(decryptedIv);

        // act
        Cipher result = this.aesCipherFactory.createAesCipher(manifest);

        // assert
        verify(rsaCipher).init(Cipher.DECRYPT_MODE, pk);
        verify(aesCipher).init(eq(Cipher.DECRYPT_MODE), argThat(new ArgumentMatcher<SecretKey>() {
            @Override
            public boolean matches(Object argument) {
                SecretKey secretKey = (SecretKey) argument;
                try {
                    return ("AES".equals(secretKey.getAlgorithm()) && Arrays.areEqual(Hex.decodeHex(new String(decryptedKey).toCharArray()), secretKey.getEncoded()));
                } catch (DecoderException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }), argThat(new ArgumentMatcher<IvParameterSpec>() {
            @Override
            public boolean matches(Object argument) {
                IvParameterSpec ivParameterSpec = (IvParameterSpec) argument;
                try {
                    return (Arrays.areEqual(ivParameterSpec.getIV(), Hex.decodeHex(new String(decryptedIv).toCharArray())));
                } catch (DecoderException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }));
        assertEquals(aesCipher, result);
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
