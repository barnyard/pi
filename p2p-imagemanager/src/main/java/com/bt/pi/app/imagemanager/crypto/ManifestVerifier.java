package com.bt.pi.app.imagemanager.crypto;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;

import javax.annotation.Resource;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.util.SecurityUtils;
import com.bt.pi.app.imagemanager.xml.Manifest;

@Component
public class ManifestVerifier {
    private static final Log LOG = LogFactory.getLog(ManifestVerifier.class);
    private SecurityUtils securityUtils;

    public ManifestVerifier() {
        this.securityUtils = null;
    }

    public boolean verify(Manifest manifest, User user) throws GeneralSecurityException {
        // Read the manifest file and extract the image components
        LOG.debug(String.format("verify(manifest: %s, user: %s)", manifest.getMachineConfiguration(), user.getUsername()));
        Certificate cert = this.securityUtils.getCertificateFromEncoded(user.getCertificate());
        String manifestSignature = manifest.getSignature();
        String image = manifest.getImage();
        String machineConfiguration = manifest.getMachineConfiguration();
        String verificationString = machineConfiguration + image;

        LOG.debug(String.format("Verifying cert against manifest using signature '%s' " + "verificationString '%s'", manifestSignature, verificationString));
        PublicKey publicKey = cert.getPublicKey();
        Signature signature = createSignature();
        signature.initVerify(publicKey);
        signature.update(verificationString.getBytes());
        try {
            return signature.verify(Hex.decodeHex(manifestSignature.toCharArray()));
        } catch (DecoderException e) {
            String message = "Error decoding hex manifest signature string to byte array";
            LOG.error(message, e);
            throw new ImageCryptoException(message, e);
        }
    }

    protected Signature createSignature() throws GeneralSecurityException {
        return Signature.getInstance("SHA1withRSA");
    }

    @Resource
    public void setSecurityUtils(SecurityUtils aSecurityUtils) {
        this.securityUtils = aSecurityUtils;
    }
}