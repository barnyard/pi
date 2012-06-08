package com.bt.pi.api.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;

import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.api.service.UserNotFoundException;
import com.bt.pi.app.common.entities.User;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceCachingCryptoFacadeWrapperTest {

    private final UserServiceCachingCryptoFacadeWrapper cryptoWrapper = new UserServiceCachingCryptoFacadeWrapper();

    @Mock
    private CryptoFacade cryptoFacade;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private ByteArrayCertificateFactory certificateFactory;
    @Mock
    private CertificateExpiredPredicate expiredPredicate;
    @Mock
    private CertificateTimestampFormatter timestampFormatter;

    @Mock
    private User user;
    @Mock
    private X509Certificate requestCertificate;
    @Mock
    private X509Certificate userCertificate;
    @Mock
    private PublicKey publicKey;

    private static final String USER_ID = "userId";
    private static final String ALIAS = "alias";
    private static final byte[] CERT_BYTE_ARRAY = "certificate_bytes".getBytes();
    private static final ReadableInstant TIMESTAMP = new DateTime(2009, 10, 11, 12, 13, 14, 567);
    private static final String TIMESTAMPED_ALIAS = "alias@timestamp";

    @Before
    public void before() throws Exception {
        cryptoWrapper.setCryptoFacade(cryptoFacade);
        cryptoWrapper.setUserManagementService(userManagementService);
        cryptoWrapper.setCertificateFactory(certificateFactory);
        cryptoWrapper.setExpiredPredicate(expiredPredicate);
        cryptoWrapper.setTimestampFormatter(timestampFormatter);

        when(cryptoFacade.getAlias(requestCertificate)).thenReturn(ALIAS);
        when(cryptoFacade.getCertificate(ALIAS)).thenReturn(userCertificate);

        when(userManagementService.getUser(USER_ID)).thenReturn(user);
        when(user.getCertificate()).thenReturn(CERT_BYTE_ARRAY);
        when(certificateFactory.generateCertificate(CERT_BYTE_ARRAY)).thenReturn(userCertificate);

        when(timestampFormatter.getTimestamp(ALIAS)).thenReturn(TIMESTAMP);
        when(timestampFormatter.addOrUpdateTimestamp(USER_ID)).thenReturn(TIMESTAMPED_ALIAS);
        when(expiredPredicate.isExpired(TIMESTAMP)).thenReturn(false);

        when(userCertificate.getPublicKey()).thenReturn(publicKey);
    }

    @Test
    public void shouldGetAliasFromCryptoFacadeUsingRequestCert() throws Exception {
        cryptoWrapper.getUserCertificate(USER_ID, requestCertificate);
        verify(cryptoFacade).getAlias(requestCertificate);
    }

    @Test
    public void shouldGetCertificateFromCryptoFacadeUsingAlias() throws Exception {
        cryptoWrapper.getUserCertificate(USER_ID, requestCertificate);
        verify(cryptoFacade).getCertificate(ALIAS);
    }

    @Test
    public void shouldReturnUserCertificateFromCryptoFacade() throws Exception {
        assertEquals(userCertificate, cryptoWrapper.getUserCertificate(USER_ID, requestCertificate));
    }

    @Test
    public void shouldGetTimestampFromAliasFromCryptoFacade() throws Exception {
        cryptoWrapper.getUserCertificate(USER_ID, requestCertificate);
        verify(timestampFormatter).getTimestamp(ALIAS);
    }

    @Test
    public void shouldCheckIfTimestampHasExpiredOnCertFromCryptoFacade() throws Exception {
        cryptoWrapper.getUserCertificate(USER_ID, requestCertificate);
        verify(expiredPredicate).isExpired(TIMESTAMP);
    }

    @Test
    public void ifNoAliasFoundForRequestCertThenShouldGetUserFromUserService() throws Exception {
        when(cryptoFacade.getAlias(requestCertificate)).thenReturn(null);
        cryptoWrapper.getUserCertificate(USER_ID, requestCertificate);
        verify(userManagementService).getUser(USER_ID);
    }

    @Test
    public void ifCertExpiredThenShouldGetUserFromUserService() throws Exception {
        when(expiredPredicate.isExpired(TIMESTAMP)).thenReturn(true);
        cryptoWrapper.getUserCertificate(USER_ID, requestCertificate);
        verify(userManagementService).getUser(USER_ID);
    }

    @Test
    public void returnNullIfAliasExistsButNoCertificateInKeyStore() throws Exception {
        when(cryptoFacade.getCertificate(ALIAS)).thenReturn(null);
        assertNull(cryptoWrapper.getUserCertificate(USER_ID, requestCertificate));
    }

    @Test
    public void returnNullIfUserNotFoundInUserService() throws Exception {
        when(cryptoFacade.getAlias(requestCertificate)).thenReturn(null);
        when(userManagementService.getUser(USER_ID)).thenThrow(new UserNotFoundException(null));
        assertNull(cryptoWrapper.getUserCertificate(USER_ID, requestCertificate));
    }

    @Test
    public void returnNullIfUserFromUserServiceHasNoCertificate() throws Exception {
        when(cryptoFacade.getAlias(requestCertificate)).thenReturn(null);
        when(user.getCertificate()).thenReturn(null);
        assertNull(cryptoWrapper.getUserCertificate(USER_ID, requestCertificate));
    }

    @Test
    public void shouldAddTimestampToAliasIfAddingToKeyStore() throws Exception {
        when(expiredPredicate.isExpired(TIMESTAMP)).thenReturn(true);
        cryptoWrapper.getUserCertificate(USER_ID, requestCertificate);
        verify(timestampFormatter).addOrUpdateTimestamp(USER_ID);
    }

    @Test
    public void shouldAddTimestampedCertToKeyStoreIfFetchingFromUserService() throws Exception {
        when(cryptoFacade.getAlias(requestCertificate)).thenReturn(null);
        cryptoWrapper.getUserCertificate(USER_ID, requestCertificate);
        verify(cryptoFacade).addToKeyStore(TIMESTAMPED_ALIAS, userCertificate);
    }

    @Test
    public void shouldDelegateProcessSecurityHeaderToCryptoFacade() throws Exception {
        Document envelope = mock(Document.class);
        cryptoWrapper.processSecurityHeader(envelope);
        verify(cryptoFacade).processSecurityHeader(envelope);
    }
}
