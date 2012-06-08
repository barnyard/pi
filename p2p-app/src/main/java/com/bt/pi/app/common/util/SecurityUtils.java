package com.bt.pi.app.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.Locale;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.springframework.stereotype.Component;


@Component
public class SecurityUtils {
    private static final Log LOG = LogFactory.getLog(SecurityUtils.class);
    private static final String PROVIDER = "BC";
    private static final int FIVE = 5;

    public SecurityUtils() {
    }

    public KeyPair getNewKeyPair(String keyAlgorithm, int keySize) throws GeneralSecurityException {
        LOG.debug(String.format("getNewKeyPair(%s, %d)", keyAlgorithm, keySize));
        KeyPairGenerator keyGen = null;
        keyGen = KeyPairGenerator.getInstance(keyAlgorithm);
        SecureRandom random = new SecureRandom();
        random.setSeed(System.currentTimeMillis());
        keyGen.initialize(keySize, random);
        KeyPair keyPair = keyGen.generateKeyPair();
        return keyPair;
    }

    public String getFingerPrint(Key key) {
        LOG.debug(String.format("getFingerPrint(%s)", key));
        byte[] fp = HashDigest.SHA1.getMessageDigest().digest(key.getEncoded());
        StringBuffer sb = new StringBuffer();
        for (byte b : fp)
            sb.append(String.format("%02X:", b));
        return sb.substring(0, sb.length() - 1).toLowerCase(Locale.ENGLISH);
    }

    public byte[] getPemBytes(final Object o) throws IOException {
        LOG.debug(String.format("getPemBytes(%s)", o));
        ByteArrayOutputStream pemByteOut = new ByteArrayOutputStream();
        PEMWriter pemOut = new PEMWriter(new OutputStreamWriter(pemByteOut));
        pemOut.writeObject(o);
        pemOut.close();
        return pemByteOut.toByteArray();
    }

    public X509Certificate getNewCertificate(String certDn, String keyAlgorithm, String keySigningAlgorithm, int keySize) throws GeneralSecurityException {
        return getNewCertificate(certDn, keySigningAlgorithm, getNewKeyPair(keyAlgorithm, keySize));
    }

    public X509Certificate getNewCertificate(String certDn, String keySigningAlgorithm, KeyPair keyPair) throws GeneralSecurityException {
        LOG.debug(String.format("getNewCertificate(%s, %s, %s)", certDn, keySigningAlgorithm, keyPair));
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal dnName = new X500Principal(certDn);

        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(dnName);
        certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(true));

        Calendar cal = Calendar.getInstance();
        certGen.setNotBefore(cal.getTime());
        cal.add(Calendar.YEAR, FIVE);
        certGen.setNotAfter(cal.getTime());
        certGen.setSubjectDN(dnName);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm(keySigningAlgorithm);
        return certGen.generate(keyPair.getPrivate(), PROVIDER);
    }

    public PrivateKey getPrivateKeyFromEncoded(byte[] bytes) throws GeneralSecurityException {
        LOG.debug(String.format("getPrivateKeyFromEncoded(%s)", bytes));
        return KeyFactory.getInstance("RSA", PROVIDER).generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    public Certificate getCertificateFromEncoded(byte[] data) throws GeneralSecurityException {
        LOG.debug(String.format("getCertificateFromEncoded(%s)", data));
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return certificateFactory.generateCertificate(new ByteArrayInputStream(data));
    }
}
