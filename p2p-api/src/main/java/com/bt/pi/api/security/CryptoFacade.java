/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.security;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Collection;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.springframework.core.io.FileSystemResource;
import org.w3c.dom.Document;

import com.bt.pi.core.conf.Property;

/**
 * Facade around {@link Crypto} and the associated {@link KeyStore} location and password. Please use this in preference
 * to using {@link Crypto} instances directly.
 * 
 */
public class CryptoFacade {

    private static final String UNCHECKED = "unchecked";

    private static final Log LOG = LogFactory.getLog(CryptoFacade.class);

    private Crypto crypto;
    private FileSystemResource keyStoreLocation;
    private String keyStorePassword;
    private WSSecurityEngine wsSecurityEngine;

    /*
     * Do not create directly, use a {@link KeyStoreCreatingCryptoFacadeFactoryBean} instead.
     */
    CryptoFacade() {
        crypto = null;
        keyStoreLocation = null;
        keyStorePassword = null;
        wsSecurityEngine = null;
    }

    public void setCrypto(Crypto aCrypto) {
        this.crypto = aCrypto;
    }

    @Property(key = "pi.keystore.password", defaultValue = "password")
    public void setKeyStorePassword(String aKeyStorePassword) {
        this.keyStorePassword = aKeyStorePassword;
    }

    @Property(key = "pi.keystore.location", defaultValue = "var/run/piusers.p12")
    public void setKeyStoreLocation(FileSystemResource aKeyStoreLocation) {
        this.keyStoreLocation = aKeyStoreLocation;
    }

    public void setWsSecurityEngine(WSSecurityEngine aWsSecurityEngine) {
        this.wsSecurityEngine = aWsSecurityEngine;
    }

    public FileSystemResource getKeyStoreLocation() {
        return keyStoreLocation;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     * Add a {@link Certificate} to the underlying {@link KeyStore} in memory and on disk.
     */
    public synchronized void addToKeyStore(String alias, Certificate certificate) throws GeneralSecurityException, IOException {
        LOG.debug(String.format("Adding certificate to keystore with alias '%s'", alias));
        getKeyStore().setCertificateEntry(alias, certificate);
        saveKeyStore();
    }

    /**
     * Delete an alias from the underlying {@link KeyStore} in memory and on disk.
     */
    public synchronized void deleteFromKeyStore(String alias) throws GeneralSecurityException, IOException {
        LOG.debug(String.format("Deleting certificate from keystore with alias '%s'", alias));
        getKeyStore().deleteEntry(alias);
        saveKeyStore();
    }

    private void saveKeyStore() throws GeneralSecurityException, IOException {
        String path = keyStoreLocation.getPath();
        LOG.debug(String.format("Saving keystore to file at '%s'", path));
        OutputStream outputStream = new FileOutputStream(path);
        try {
            crypto.getKeyStore().store(outputStream, keyStorePassword.toCharArray());
        } finally {
            outputStream.close();
        }
    }

    /**
     * Returns the {@link Certificate} for the given alias
     */
    public Certificate getCertificate(String alias) {
        try {
            return crypto.getKeyStore().getCertificate(alias);
        } catch (KeyStoreException e) {
            // no way to recover
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the alias matching a given {@link Certificate}.
     * 
     * @param certificate
     * @return
     */
    public String getAlias(Certificate certificate) {
        try {
            return crypto.getKeyStore().getCertificateAlias(certificate);
        } catch (KeyStoreException e) {
            // no way to recover
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all aliases in the {@link KeyStore}.
     */
    @SuppressWarnings(UNCHECKED)
    public Collection<String> getAliases() {
        try {
            return EnumerationUtils.toList(crypto.getKeyStore().aliases());
        } catch (KeyStoreException e) {
            // no way to recover
            throw new RuntimeException(e);
        }
    }

    /**
     * Does WS-Security processing of an envelope using the wrapped {@link Crypto} instance.
     */
    @SuppressWarnings(UNCHECKED)
    public Collection<WSSecurityEngineResult> processSecurityHeader(Document envelope) throws WSSecurityException {
        return wsSecurityEngine.processSecurityHeader(envelope, null, null, crypto);
    }

    private KeyStore getKeyStore() {
        return crypto.getKeyStore();
    }

}
