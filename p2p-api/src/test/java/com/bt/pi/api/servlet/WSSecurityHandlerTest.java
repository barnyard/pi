package com.bt.pi.api.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Vector;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;

import com.bt.pi.api.security.UserServiceCachingCryptoFacadeWrapper;
import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.app.common.entities.User;

@RunWith(MockitoJUnitRunner.class)
public class WSSecurityHandlerTest {

    @InjectMocks
    private final WSSecurityHandler wsSecurityHandler = new WSSecurityHandler();

    private static final String USER_ID = "userId";

    @Mock
    private UserServiceCachingCryptoFacadeWrapper cryptoWrapper;

    @Mock
    private Document envelope;

    @Mock
    private WSSecurityEngineResult signatureSecurityResult;

    @Mock
    private UserManagementService userManagementService;

    @Mock
    private Principal principal;

    @Mock
    private Certificate requestCertificate;

    @Mock
    private Certificate userCertificate;

    @Mock
    private PublicKey publicKey;

    @Mock
    private User user;

    private Vector<WSSecurityEngineResult> wsSecurityEngineResults;

    @Before
    public void before() throws Exception {

        wsSecurityEngineResults = new Vector<WSSecurityEngineResult>();
        wsSecurityEngineResults.add(new WSSecurityEngineResult(WSConstants.TS, new Object()));
        wsSecurityEngineResults.add(new WSSecurityEngineResult(WSConstants.BST, new Object()));
        wsSecurityEngineResults.add(signatureSecurityResult);

        when(signatureSecurityResult.get(WSSecurityEngineResult.TAG_ACTION)).thenReturn(WSConstants.SIGN);
        when(signatureSecurityResult.get(WSSecurityEngineResult.TAG_PRINCIPAL)).thenReturn(principal);
        when(signatureSecurityResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE)).thenReturn(requestCertificate);

        when(principal.getName()).thenReturn("a=b,c=d,O=" + USER_ID + ",e=f");

        when(cryptoWrapper.processSecurityHeader(envelope)).thenReturn(wsSecurityEngineResults);

        when(cryptoWrapper.getUserCertificate(USER_ID, requestCertificate)).thenReturn(userCertificate);
        when(userCertificate.getPublicKey()).thenReturn(publicKey);
        when(userManagementService.getUser(USER_ID)).thenReturn(user);
        when(user.isEnabled()).thenReturn(true);
    }

    @Test
    public void shouldProcessSecurityHeader() throws Exception {
        wsSecurityHandler.processEnvelope(envelope);
        verify(cryptoWrapper).processSecurityHeader(envelope);
    }

    @Test
    public void shouldVerifyRequestCertificateAgainstUserPublicKey() throws Exception {
        wsSecurityHandler.processEnvelope(envelope);
        verify(requestCertificate).verify(publicKey);
    }

    @Test
    public void shouldThrowIfSecurityEngineResultsAreNull() throws Exception {
        when(cryptoWrapper.processSecurityHeader(envelope)).thenReturn(null);
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("incorrect number of WS-Security headers"));
        }
    }

    @Test
    public void shouldThrowIfSecurityEngineResultsHaveLessThanThreeResults() throws Exception {
        wsSecurityEngineResults.clear();
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("incorrect number of WS-Security headers"));
        }
    }

    @Test
    public void shouldThrowIfSecurityEngineResultsHaveMoreThanThreeResults() throws Exception {
        wsSecurityEngineResults.add(new WSSecurityEngineResult(0, new Object()));
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("incorrect number of WS-Security headers"));
        }
    }

    @Test
    public void shouldThrowIfSecurityEngineResultsDoNotContainTsHeader() throws Exception {
        blankOutResultAtPosition(0);
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("missing WS-Security header(s): TS"));
        }
    }

    @Test
    public void shouldThrowIfSecurityEngineResultsDoNotContainBstHeader() throws Exception {
        blankOutResultAtPosition(1);
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("missing WS-Security header(s): BST"));
        }
    }

    @Test
    public void shouldThrowIfSecurityEngineResultsDoNotContainSignHeader() throws Exception {
        blankOutResultAtPosition(2);
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("missing WS-Security header(s): SIGN"));
        }
    }

    @Test
    public void shouldThrowIfSecurityEngineResultsDoNotMultipleHeaders() throws Exception {
        blankOutResultAtPosition(0);
        blankOutResultAtPosition(1);
        blankOutResultAtPosition(2);
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("missing WS-Security header(s): BST, TS, SIGN"));
        }
    }

    @Test
    public void shouldThrowIfHeadersDoNotContainAPrincipal() throws Exception {
        when(signatureSecurityResult.get(WSSecurityEngineResult.TAG_PRINCIPAL)).thenReturn(null);
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("unable to find principal in WS-Security headers"));
        }
    }

    @Test
    public void shouldThrowIfPrincipalNameDoesNotContainAUserId() throws Exception {
        when(principal.getName()).thenReturn("a=b,c=d,e=f");
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("unable to determine userId from principal name 'a=b,c=d,e=f'"));
        }
    }

    @Test
    public void principalNameContainingASpaceShouldStillWork() throws Exception {
        when(principal.getName()).thenReturn("a=b,c=d, O=" + USER_ID + ",e=f");
        wsSecurityHandler.processEnvelope(envelope);
    }

    @Test
    public void shouldThrowIfHeadersDoNotContainACertificate() throws Exception {
        when(signatureSecurityResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE)).thenReturn(null);
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("unable to find certificate in WS-Security headers"));
        }
    }

    @Test
    public void shouldThrowIfCryptoWrapperDoesNotReturnACertificate() throws Exception {
        when(cryptoWrapper.getUserCertificate(USER_ID, requestCertificate)).thenReturn(null);
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertThat(e.getMessage(), containsString("unable to get user certificate from cryptoWrapper"));
        }
    }

    @Test
    public void shouldThrowWrappedExceptionFromVerify() throws Exception {
        CertificateException certificateException = new CertificateException();
        doThrow(certificateException).when(requestCertificate).verify(publicKey);
        try {
            wsSecurityHandler.processEnvelope(envelope);
            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {
            assertTrue("should have wrapped certificateException", e.getCause() == certificateException);
        }
    }

    @Test
    public void shouldThrowExceptionWhenUserIsDisabled() throws Exception {

        when(user.isEnabled()).thenReturn(false);
        try {
            // act
            wsSecurityHandler.processEnvelope(envelope);

            fail("Should have thrown WSSecurityHandlerException");
        } catch (WSSecurityHandlerException e) {

            assertEquals("User null is not enabled", e.getMessage());
        }
    }

    private void blankOutResultAtPosition(int index) {
        wsSecurityEngineResults.remove(index);
        wsSecurityEngineResults.add(index, new WSSecurityEngineResult(0, new Object()));
    }
}
