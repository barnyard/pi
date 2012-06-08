package com.bt.pi.api.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Collection;

import javax.security.auth.callback.CallbackHandler;

import org.apache.commons.io.FileUtils;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.components.crypto.Crypto;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.w3c.dom.Document;

public class CryptoFacadeTest {

    private static final String ALIAS = "alias";
    private final static String KEY_STORE_LOCATION = "build/" + CryptoFacadeTest.class.getSimpleName() + ".p12";
    private final static String KEY_STORE_PASSWORD = "password";

    private CryptoFacade cryptoFacade;
    private static final Certificate CERTIFICATE = CertTestHelper.getCertificate();

    private WSSecurityEngine wsSecurityEngine;

    @BeforeClass
    public static void setupSecurityProvider() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Create CryptoFacade using FactoryBean so that it has a real crypto, which can't be mocked easily due to having
     * exceptional numbers of final methods
     */
    @Before
    public void before() throws Exception {
        FileUtils.deleteQuietly(new File(KEY_STORE_LOCATION));
        createCryptoFacade();
    }

    @Test
    public void testKeyStorePasswordProperty() {
        cryptoFacade.setKeyStorePassword("newPassword");
        assertEquals("newPassword", cryptoFacade.getKeyStorePassword());
    }

    @Test
    public void testKeyStoreLocationProperty() {
        FileSystemResource keyStoreLocation = new FileSystemResource("/tmp/foo");
        cryptoFacade.setKeyStoreLocation(keyStoreLocation);
        assertEquals(keyStoreLocation, cryptoFacade.getKeyStoreLocation());
    }

    @Test
    public void addToKeyStoreIntegrationTest() throws Exception {

        cryptoFacade.addToKeyStore(ALIAS, CERTIFICATE);

        // reload from disk
        createCryptoFacade();

        assertEquals("Should be a certificate for alias", CERTIFICATE, cryptoFacade.getCertificate(ALIAS));
    }

    @Test
    public void deleteFromKeyStoreIntegrationTest() throws Exception {
        cryptoFacade.addToKeyStore(ALIAS, CERTIFICATE);
        assertEquals("Should be a certificate for alias", CERTIFICATE, cryptoFacade.getCertificate(ALIAS));

        cryptoFacade.deleteFromKeyStore(ALIAS);

        // reload from disk
        createCryptoFacade();
        assertNull("Should be no certificates for alias", cryptoFacade.getCertificate(ALIAS));
    }

    @Test
    public void shouldReturnCorrectCertificateForAlias() throws Exception {
        cryptoFacade.addToKeyStore(ALIAS, CERTIFICATE);
        assertEquals(CERTIFICATE, cryptoFacade.getCertificate(ALIAS));
    }

    @Test
    public void shouldReturnCorrectAliasForCertificate() throws Exception {
        cryptoFacade.addToKeyStore(ALIAS, CERTIFICATE);
        assertEquals(ALIAS, cryptoFacade.getAlias(CERTIFICATE));
    }

    @Test
    public void getAliases() throws Exception {
        cryptoFacade.addToKeyStore("foo", CERTIFICATE);
        cryptoFacade.addToKeyStore("bar", CERTIFICATE);

        Collection<String> aliases = cryptoFacade.getAliases();

        assertThat(aliases, hasItems("foo", "bar"));
    }

    public void processSecurityHeadersShouldDelegateToWSSecurityEngine() throws Exception {
        Document envelope = mock(Document.class);
        cryptoFacade.processSecurityHeader(envelope);
        verify(wsSecurityEngine).processSecurityHeader(eq(envelope), isA(String.class), isA(CallbackHandler.class), isA(Crypto.class));
    }

    private void createCryptoFacade() throws Exception {
        KeyStoreCreatingCryptoFacadeFactoryBean factoryBean = new KeyStoreCreatingCryptoFacadeFactoryBean();
        factoryBean.setKeyStoreLocation(KEY_STORE_LOCATION);
        factoryBean.setKeyStorePassword(KEY_STORE_PASSWORD);
        wsSecurityEngine = mock(WSSecurityEngine.class);
        factoryBean.setWsSecurityEngine(wsSecurityEngine);
        factoryBean.afterPropertiesSet();
        cryptoFacade = (CryptoFacade) factoryBean.getObject();
    }

}
