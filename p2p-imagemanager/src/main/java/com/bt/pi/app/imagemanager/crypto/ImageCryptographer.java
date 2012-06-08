/* (c) British Telecommunications plc, 2010, All Rights Reserved */

package com.bt.pi.app.imagemanager.crypto;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.annotation.Resource;
import javax.crypto.Cipher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.imagemanager.xml.Manifest;
import com.bt.pi.app.imagemanager.xml.ManifestBuilder;
import com.bt.pi.sss.client.PisssClient;

@Component
public class ImageCryptographer {
    private static final Log LOG = LogFactory.getLog(ImageCryptographer.class);
    @Resource
    private PisssClient pisssClient;
    @Resource
    private ManifestBuilder manifestBuilder;
    @Resource
    private ManifestVerifier manifestVerifier;
    @Resource
    private ImageAssembler imageAssembler;
    @Resource
    private AesCipherFactory aesCipherFactory;
    @Resource
    private ImageFileDecryptor imageFileDecryptor;

    public ImageCryptographer() {
        this.pisssClient = null;
        this.manifestBuilder = null;
        this.manifestVerifier = null;
        this.imageAssembler = null;
        this.aesCipherFactory = null;
        this.imageFileDecryptor = null;
    }

    public File decrypt(String bucketName, String manifestFileName, User user) {
        LOG.debug(String.format("decrypt(bucket: %s, manifestfile: %s, user: %s)", bucketName, manifestFileName, user.getUsername()));
        Manifest manifest = null;
        try {
            File manifestTmpFile = null;
            File manifestXmlFile = null;
            try {
                manifestTmpFile = File.createTempFile("manifest", null);
                manifestXmlFile = this.pisssClient.getFileFromBucket(bucketName, manifestFileName, user, manifestTmpFile.getAbsolutePath());
                manifest = manifestBuilder.build(manifestXmlFile);
                LOG.debug(String.format("Manifest built: %s", manifest));
            } finally {
                FileUtils.deleteQuietly(manifestXmlFile);
                FileUtils.deleteQuietly(manifestTmpFile);
            }
            boolean isVerified = this.manifestVerifier.verify(manifest, user);
            if (!isVerified) {
                throwImageCryptoException("manifest could not be verified with user cert");
            }
            LOG.debug("manifest verified successfully");
        } catch (IOException e) {
            throwImageCryptoException("IOException received when building the Manifest");
        } catch (GeneralSecurityException e) {
            throwImageCryptoException("CertificateException received when verifying the user cert");
        }

        File encryptedImageFile = null;
        try {
            encryptedImageFile = imageAssembler.assembleParts(manifest, bucketName, user);
        } catch (IOException ioe) {
            String message = "Unable to assemble image parts";
            LOG.error(message, ioe);
            throw new ImageCryptoException(message);
        }
        Cipher aesCipher = this.aesCipherFactory.createAesCipher(manifest);
        return imageFileDecryptor.decryptImage(encryptedImageFile, aesCipher);
    }

    private void throwImageCryptoException(String message) {
        LOG.error(message);
        throw new ImageCryptoException(message);
    }
}
