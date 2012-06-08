package com.bt.pi.app.imagemanager.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.util.SecurityUtils;
import com.bt.pi.app.imagemanager.xml.Manifest;

@RunWith(MockitoJUnitRunner.class)
public class ManifestVerifierTest {
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private Certificate cert;
    @Mock
    private Manifest manifest;
    @Mock
    private User user;
    private ManifestVerifier manifestVerifier;
    private MySignature mockSignature;
    private String signature = "4adaf84301a4d1a79969d4941e0acc7d3e4a1ac953a9651e8c6c62db5a463b707ef5b49733605c7425c826586b2bd0efa67fb71d979b3d8384c6e45493d78e564106fcf9e887d7f639152323e735fd3c7e5ef662f41c2ff3f395bf2836fef3feba1080f3276aaad8d91c139df760b72c72dbcb7f11e362ce5c6206e14a202ed6214a2e3e791ece25b4c4e3fd57bcc72d78625c565d652d581d880fafcd2ed0ef88d5c3a8c2c913443ff1aa66e362bfc0661c34458eff50bc42827fb8005bd6d9c609eac76a37d7f7c586077c9e0434964b090dd84d8428287ca72c830c17cced71940f3c7c7ba5b05b685826620220c49ca52d767febbaab9f5e26d0effdd38b";
    private byte[] encodedCert = "asdf".getBytes();

    @Before
    public void setUp() throws Exception {
        mockSignature = new MySignature("");
        manifestVerifier = new ManifestVerifier() {
            @Override
            protected Signature createSignature() throws GeneralSecurityException {
                return mockSignature;
            }
        };
        manifestVerifier.setSecurityUtils(securityUtils);
        when(user.getCertificate()).thenReturn(encodedCert);
        when(securityUtils.getCertificateFromEncoded(encodedCert)).thenReturn(cert);
        when(manifest.getSignature()).thenReturn(signature);
        when(manifest.getImage()).thenReturn("anImage");
        when(manifest.getMachineConfiguration()).thenReturn("aMachineConfiguration");
    }

    @Test
    public void shouldReturnTrueWhenVerifyingAManifestForAValidUser() throws GeneralSecurityException {
        // setup
        mockSignature.setVerified(true);

        // act/assert
        assertTrue(manifestVerifier.verify(manifest, user));
    }

    @Test
    public void shouldReturnFalseWhenVerifyingAManifestForAnInvalidUser() throws GeneralSecurityException {
        // setup
        mockSignature.setVerified(false);

        // act/assert
        assertFalse(manifestVerifier.verify(manifest, user));
    }

    @Test
    public void testCreateSignature() throws GeneralSecurityException {
        // setup
        ManifestVerifier mv = new ManifestVerifier();

        // act
        Signature result = mv.createSignature();

        // assert
        assertEquals("SHA1withRSA", result.getAlgorithm());
    }

    // I'd liked to have used PowerMock for this but couldn't get it to work :-(
    private class MySignature extends Signature {
        private boolean verified;

        protected MySignature(String algorithm) {
            super(algorithm);
        }

        @Override
        protected Object engineGetParameter(String param) throws InvalidParameterException {
            return null;
        }

        @Override
        protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        }

        @Override
        protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        }

        @Override
        protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
        }

        @Override
        protected byte[] engineSign() throws SignatureException {
            return null;
        }

        @Override
        protected void engineUpdate(byte b) throws SignatureException {
        }

        @Override
        protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        }

        @Override
        protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
            return this.verified;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }
    }
}
