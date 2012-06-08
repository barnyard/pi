/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.security;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.components.crypto.Crypto;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.ws.soap.security.wss4j.support.CryptoFactoryBean;

/**
 * Creates a {@link CryptoFacade}, creating an empty keystore file if it doesn't already exist.
 */
@Component
@DependsOn("securityProvider")
@Scope("prototype")
public class KeyStoreCreatingCryptoFacadeFactoryBean implements FactoryBean<CryptoFacade>, InitializingBean {

    private static final Log LOG = LogFactory.getLog(KeyStoreCreatingCryptoFacadeFactoryBean.class);

    private static final String KEY_STORE_PROVIDER = "BC";
    private static final String KEY_STORE_TYPE = "pkcs12";
    private WSSecurityEngine wsSecurityEngine;

    private CryptoFacade cryptoFacade;

    public KeyStoreCreatingCryptoFacadeFactoryBean() {
        cryptoFacade = new CryptoFacade();
    }

    public void setKeyStoreLocation(String aKeyStoreLocation) {
        cryptoFacade.setKeyStoreLocation(new FileSystemResource(aKeyStoreLocation));
    }

    public void setKeyStorePassword(String aKeyStorePassword) {
        cryptoFacade.setKeyStorePassword(aKeyStorePassword);
    }

    @javax.annotation.Resource
    public void setWsSecurityEngine(WSSecurityEngine aWSSecurityEngine) {
        wsSecurityEngine = aWSSecurityEngine;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        FileSystemResource location = cryptoFacade.getKeyStoreLocation();
        String password = cryptoFacade.getKeyStorePassword();
        if (location.exists()) {
            try {
                createCryptoFactory(location, password);
            } catch (RuntimeException e) { // must catch RuntimeException here, as it's what's thrown
                LOG.warn(String.format("Recovering from exception loading KeyStore from '%s'", location), e);
                createEmptyCryptoFactory(location, password);
            }
        } else {
            createEmptyCryptoFactory(location, password);
        }
    }

    private void createEmptyCryptoFactory(FileSystemResource location, String password) throws Exception {
        createEmptyKeyStore(location.getPath(), password.toCharArray());
        createCryptoFactory(location, password);
    }

    private void createEmptyKeyStore(String filename, char[] password) throws Exception {
        // create empty keystore
        LOG.debug(String.format("Creating new keystore at '%s'", filename));
        KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE, KEY_STORE_PROVIDER);
        ks.load(null, password);
        OutputStream outputStream = new FileOutputStream(filename);
        try {
            ks.store(outputStream, password);
        } finally {
            outputStream.close();
        }
    }

    private void createCryptoFactory(Resource location, String password) throws Exception {
        LOG.debug(String.format("Creating CryptoFacade with KeyStore at '%s'", location));
        CryptoFactoryBean cryptoFactory = new CryptoFactoryBean();
        cryptoFactory.setKeyStoreLocation(location);
        cryptoFactory.setKeyStorePassword(password);
        cryptoFactory.setKeyStoreType(KEY_STORE_TYPE);
        cryptoFactory.setKeyStoreProvider(KEY_STORE_PROVIDER);
        cryptoFactory.afterPropertiesSet();
        cryptoFacade.setCrypto((Crypto) cryptoFactory.getObject());
        cryptoFacade.setWsSecurityEngine(wsSecurityEngine);
    }

    @Override
    public CryptoFacade getObject() throws Exception {
        return cryptoFacade;
    }

    @Override
    public Class<CryptoFacade> getObjectType() {
        return CryptoFacade.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
