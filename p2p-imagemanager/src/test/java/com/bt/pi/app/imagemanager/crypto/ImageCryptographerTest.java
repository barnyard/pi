package com.bt.pi.app.imagemanager.crypto;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.imagemanager.xml.Manifest;
import com.bt.pi.app.imagemanager.xml.ManifestBuilder;
import com.bt.pi.sss.client.PisssClient;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { Cipher.class })
@PowerMockIgnore( { "org.apache.commons.logging.*", "org.apache.log4j.*" })
public class ImageCryptographerTest {
    @InjectMocks
    private ImageCryptographer ic = new ImageCryptographer();
    @Mock
    private PisssClient pisssClient;
    @Mock
    private ManifestBuilder mockManifestBuilder;
    @Mock
    private File mockManifestFile;
    @Mock
    private Manifest mockManifest;
    @Mock
    private ImageAssembler mockImageAssembler;
    @Mock
    private File encryptedImageFile;
    @Mock
    private AesCipherFactory aesCipherFactory;
    @Mock
    private User user;
    @Mock
    private ManifestVerifier manifestVerifier;
    @Mock
    private File mockDecryptedImageFile;
    @Mock
    private ImageFileDecryptor imageFileDecryptor;

    private Cipher aesCipher;
    private String bucketName = "test-bucket";
    private String objectName = "manifest.xml";

    @Before
    public void setUp() throws Exception {
        aesCipher = PowerMockito.mock(Cipher.class);
        when(pisssClient.getFileFromBucket(eq(bucketName), eq(objectName), eq(user), isA(String.class))).thenReturn(mockManifestFile);
        when(mockManifestBuilder.build(isA(File.class))).thenReturn(mockManifest);
    }

    @Test
    public void shouldDecryptImageWithValidUserAndManifest() throws Exception {
        setupExpectationsForValidUserCertAndManifest();

        // act
        File result = ic.decrypt(bucketName, objectName, user);

        // assert
        verify(imageFileDecryptor).decryptImage(eq(encryptedImageFile), eq(aesCipher));
        assertEquals(mockDecryptedImageFile, result);
    }

    @Test(expected = ImageCryptoException.class)
    public void shouldThrowImageCryptoExceptionWhenDecryptingImageAndManifestCannotBeVerified() throws Exception {
        // setup
        when(manifestVerifier.verify(mockManifest, user)).thenReturn(false);

        // act
        ic.decrypt(bucketName, objectName, user);
    }

    @Test(expected = ImageCryptoException.class)
    public void shouldThrowImageCryptoExceptionWhenDecryptingImageAndManifestVerificationThrowsGeneralSecurityException() throws Exception {
        // setup
        when(manifestVerifier.verify(mockManifest, user)).thenThrow(new GeneralSecurityException("security exception"));

        // act
        ic.decrypt(bucketName, objectName, user);
    }

    @Test(expected = ImageCryptoException.class)
    public void shouldThrowImageCryptoExceptionWhenDecryptingImageAndImagePartsCannotBeAssembled() throws Exception {
        // setup
        when(manifestVerifier.verify(mockManifest, user)).thenReturn(true);
        when(mockImageAssembler.assembleParts(mockManifest, bucketName, user)).thenThrow(new IOException("IO Exception"));

        // act
        ic.decrypt(bucketName, objectName, user);
    }

    private void setupExpectationsForValidUserCertAndManifest() throws GeneralSecurityException, IOException {
        byte[] encodedCert = "asdf".getBytes();
        when(user.getCertificate()).thenReturn(encodedCert);
        when(manifestVerifier.verify(mockManifest, user)).thenReturn(true);

        when(mockImageAssembler.assembleParts(mockManifest, bucketName, user)).thenReturn(encryptedImageFile);
        when(aesCipherFactory.createAesCipher(mockManifest)).thenReturn(aesCipher);
        when(imageFileDecryptor.decryptImage(encryptedImageFile, aesCipher)).thenReturn(mockDecryptedImageFile);
    }
}
