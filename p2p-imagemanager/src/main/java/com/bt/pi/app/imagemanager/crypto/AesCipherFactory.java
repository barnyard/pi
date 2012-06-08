/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.imagemanager.crypto;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;

import javax.annotation.Resource;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.UserServiceHelper;
import com.bt.pi.app.common.util.SecurityUtils;
import com.bt.pi.app.imagemanager.xml.Manifest;

@Component
public class AesCipherFactory {
    private static final Log LOG = LogFactory.getLog(AesCipherFactory.class);
    private UserServiceHelper userServiceHelper;
    private SecurityUtils securityUtils;

    public AesCipherFactory() {
        this.userServiceHelper = null;
        this.securityUtils = null;
    }

    public Cipher createAesCipher(Manifest manifest) {
        LOG.debug(String.format("createAesCipher(%s)", manifest));
        PrivateKey pk;
        // Decrypt key and IV
        byte[] key;
        byte[] iv;
        try {
            pk = this.securityUtils.getPrivateKeyFromEncoded(this.userServiceHelper.getPiCertificate().getPrivateKey());
            LOG.debug(String.format("Private Key: %s", pk));
            Cipher rsaCipher = createRsaCipher();
            rsaCipher.init(Cipher.DECRYPT_MODE, pk);

            key = decryptManifestString(rsaCipher, manifest.getEncryptedKey());
            iv = decryptManifestString(rsaCipher, manifest.getEncryptedIV());

        } catch (GeneralSecurityException e) {
            throw createImageCryptoException("Error reading PI private key", e);
        }

        // Unencrypt image
        IvParameterSpec salt = new IvParameterSpec(iv);
        SecretKey keySpec = new SecretKeySpec(key, "AES");
        try {
            Cipher aesCipher = createAesCipher();
            aesCipher.init(Cipher.DECRYPT_MODE, keySpec, salt);
            return aesCipher;
        } catch (GeneralSecurityException e) {
            throw createImageCryptoException("GeneralSecurityException raised when decrypting the manifest", e);
        }
    }

    protected Cipher createAesCipher() throws GeneralSecurityException {
        return Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
    }

    protected Cipher createRsaCipher() throws GeneralSecurityException {
        return Cipher.getInstance("RSA/ECB/PKCS1Padding");
    }

    protected byte[] decryptManifestString(Cipher rsaCipher, String inputString) {
        try {
            String s = new String(rsaCipher.doFinal(hexToBytes(inputString)));
            return hexToBytes(s);
        } catch (IllegalBlockSizeException e) {
            throw createImageCryptoException("IllegalBlockSizeException raised when decrypting the iv and private key", e);
        } catch (BadPaddingException e) {
            throw createImageCryptoException("BadPaddingException raised when decrypting the iv and private key", e);
        }
    }

    private byte[] hexToBytes(String s) {
        try {
            return Hex.decodeHex(s.toCharArray());
        } catch (DecoderException e) {
            throw createImageCryptoException(String.format("error converting hex string %s to byte array", s), e);
        }
    }

    @Resource
    public void setUserServiceHelper(UserServiceHelper aUserServiceHelper) {
        this.userServiceHelper = aUserServiceHelper;
    }

    @Resource
    public void setSecurityUtils(SecurityUtils aSecurityUtils) {
        this.securityUtils = aSecurityUtils;
    }

    private ImageCryptoException createImageCryptoException(String message, Exception e) {
        LOG.error(message, e);
        return new ImageCryptoException(message, e);
    }
}
