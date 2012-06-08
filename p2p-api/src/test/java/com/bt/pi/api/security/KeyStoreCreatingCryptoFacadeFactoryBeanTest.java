package com.bt.pi.api.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.Security;
import java.security.cert.Certificate;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

public class KeyStoreCreatingCryptoFacadeFactoryBeanTest {

    private static final String KEY_STORE_LOCATION = "build/" + KeyStoreCreatingCryptoFacadeFactoryBeanTest.class.getSimpleName() + ".p12";
    private static final String ALIAS = "alias";
    private final static String MANUALLY_SET_KEY_STORE_LOCATION = "build/fake.p12";
    private KeyStoreCreatingCryptoFacadeFactoryBean factoryBean;

    @BeforeClass
    public static void setupSecurityProvider() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void before() throws Exception {
        FileUtils.deleteQuietly(new File(KEY_STORE_LOCATION));
        FileUtils.deleteQuietly(new File(MANUALLY_SET_KEY_STORE_LOCATION));
        factoryBean = new KeyStoreCreatingCryptoFacadeFactoryBean();
        factoryBean.setKeyStoreLocation(KEY_STORE_LOCATION);
        factoryBean.setKeyStorePassword("testPassword");
    }

    @Test
    public void getObjectReturnsACryptoBean() throws Exception {
        assertTrue("should return a CryptoFacade", factoryBean.getObject() instanceof CryptoFacade);
    }

    @Test
    public void getObjectTypeReturnsCryptoBean() throws Exception {
        assertEquals(CryptoFacade.class, factoryBean.getObjectType());
    }

    @Test
    public void isSingletonReturnsTrue() throws Exception {
        assertTrue(factoryBean.isSingleton());
    }

    @Test
    public void shouldCreateKeyStoreIfAbsent() throws Exception {
        FileSystemResource keyStore = new FileSystemResource(KEY_STORE_LOCATION);
        assertFalse("keystore should not exist before test", keyStore.exists());

        initializeFactoryBean();

        assertTrue("keystore should exist after test", keyStore.exists());
    }

    @Test
    public void shouldCreateKeyStoreFromSetValues() throws Exception {
        // setup
        FileSystemResource keyStore = new FileSystemResource(MANUALLY_SET_KEY_STORE_LOCATION);
        assertFalse("keystore should not exist before test", keyStore.exists());
        factoryBean.setKeyStoreLocation(MANUALLY_SET_KEY_STORE_LOCATION);
        factoryBean.setKeyStorePassword("bob");

        // act
        initializeFactoryBean();

        // assert
        assertTrue("keystore should exist after test", keyStore.exists());

    }

    @Test
    public void newKeyStoreShouldBeEmpty() throws Exception {
        initializeFactoryBean();

        assertNull("new keystore should be empty", getCryptoFacade().getCertificate(ALIAS));
    }

    @Test
    public void shouldUseExistingKeystoreIfOneExists() throws Exception {
        // create keystore
        initializeFactoryBean();
        Certificate certificate = CertTestHelper.getCertificate();
        getCryptoFacade().addToKeyStore(ALIAS, certificate);

        // reload keystore
        initializeFactoryBean();
        assertEquals("keystore should still have an entry", certificate, getCryptoFacade().getCertificate(ALIAS));
    }

    @Test
    public void shouldOverwriteExistingKeyStoreIfItIsCorrupt() throws Exception {
        // create invalid keystore
        FileUtils.touch(new File(KEY_STORE_LOCATION));

        // load from keystore
        initializeFactoryBean();

        assertNull("keystore should be empty", getCryptoFacade().getCertificate(ALIAS));
    }

    private void initializeFactoryBean() throws Exception {
        factoryBean.afterPropertiesSet();
    }

    private CryptoFacade getCryptoFacade() throws Exception {
        return (CryptoFacade) factoryBean.getObject();
    }
}
