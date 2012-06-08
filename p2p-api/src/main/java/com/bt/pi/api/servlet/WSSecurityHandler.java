/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.servlet;

import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import com.bt.pi.api.security.UserServiceCachingCryptoFacadeWrapper;
import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.api.service.UserNotFoundException;
import com.bt.pi.app.common.entities.User;

/**
 * Processes the SOAP request and does some WS-security validation.
 */
@Component
public class WSSecurityHandler {
    private static final int EXPECTED_SECURITY_HEADER_COUNT = 3;
    private static final String USER_S_IS_NOT_ENABLED = "User %s is not enabled";
    private static final Log LOG = LogFactory.getLog(WSSecurityHandler.class);

    private static final int TWO = 2;
    private static final int THREE = 3;

    private UserServiceCachingCryptoFacadeWrapper cryptoWrapper;

    private UserManagementService userManagementService;

    public WSSecurityHandler() {
        cryptoWrapper = null;
        userManagementService = null;
    }

    @Resource
    public void setUserManagementService(UserManagementService aUserManagementService) {
        this.userManagementService = aUserManagementService;
    }

    @Resource
    public void setCryptoWrapper(UserServiceCachingCryptoFacadeWrapper aCryptoWrapper) {
        cryptoWrapper = aCryptoWrapper;
    }

    public String processEnvelope(Document envelope) {
        Collection<WSSecurityEngineResult> wsSecurityEngineResults = processSecurityHeader(envelope);
        LOG.debug(String.format("wsSecurityEngineResults: %s", wsSecurityEngineResults));

        if (wsSecurityEngineResults == null || wsSecurityEngineResults.size() != EXPECTED_SECURITY_HEADER_COUNT)
            throw new WSSecurityHandlerException("incorrect number of WS-Security headers");

        checkActions(wsSecurityEngineResults);

        Principal principal = getPrincipal(wsSecurityEngineResults);
        String userId = getUserId(principal.getName());
        LOG.debug("userId=" + userId);
        try {
            User user = userManagementService.getUser(userId);
            if (!user.isEnabled()) {
                LOG.debug(String.format(USER_S_IS_NOT_ENABLED, user.getUsername()));
                throw new WSSecurityHandlerException(String.format(USER_S_IS_NOT_ENABLED, user.getUsername()), HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (UserNotFoundException e) {
            throw new WSSecurityHandlerException(String.format("User %s not found", userId));
        }

        Certificate requestCertificate = getCertificate(wsSecurityEngineResults);
        checkCertificate(userId, requestCertificate);
        return userId;
    }

    private Collection<WSSecurityEngineResult> processSecurityHeader(Document envelope) {
        try {
            return cryptoWrapper.processSecurityHeader(envelope);
        } catch (WSSecurityException e) {
            LOG.error("Security stuff barfed. When given this envelope:  " + envelope, e);
            throw new WSSecurityHandlerException(e);
        }
    }

    private void checkActions(Collection<WSSecurityEngineResult> wsSecurityEngineResults) {
        LOG.debug("checking actions");
        Map<Integer, String> actions = new HashMap<Integer, String>();
        actions.put(WSConstants.SIGN, "SIGN");
        actions.put(WSConstants.BST, "BST");
        actions.put(WSConstants.TS, "TS");

        for (WSSecurityEngineResult wsSecurityEngineResult : wsSecurityEngineResults)
            actions.remove(wsSecurityEngineResult.get(WSSecurityEngineResult.TAG_ACTION));

        if (!actions.isEmpty())
            throw new WSSecurityHandlerException("missing WS-Security header(s): " + StringUtils.join(actions.values(), ", "));
    }

    private Principal getPrincipal(Collection<WSSecurityEngineResult> wsSecurityEngineResults) {
        LOG.debug("getting principal");
        Principal principal = null;
        for (WSSecurityEngineResult wsSecurityEngineResult : wsSecurityEngineResults) {
            principal = (Principal) wsSecurityEngineResult.get(WSSecurityEngineResult.TAG_PRINCIPAL);
            if (principal != null)
                break;
        }
        if (principal == null)
            throw new WSSecurityHandlerException("unable to find principal in WS-Security headers");
        return principal;
    }

    private String getUserId(String principalName) {
        LOG.debug(String.format("getUserId(\"%s\")", principalName));
        String[] parts = principalName.split(",");
        for (String part : parts) {
            if (part.startsWith("O="))
                return part.substring(TWO);
            else if (part.startsWith(" O="))
                return part.substring(THREE);
        }
        throw new WSSecurityHandlerException(String.format("unable to determine userId from principal name '%s'", principalName));
    }

    private Certificate getCertificate(Collection<WSSecurityEngineResult> wsSecurityEngineResults) {
        LOG.debug("getting certificate");
        Certificate certificate = null;
        for (WSSecurityEngineResult wsSecurityEngineResult : wsSecurityEngineResults) {
            certificate = (Certificate) wsSecurityEngineResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
            if (certificate != null)
                break;
        }
        if (certificate == null)
            throw new WSSecurityHandlerException("unable to find certificate in WS-Security headers");
        return certificate;
    }

    private void checkCertificate(String userId, Certificate requestCertificate) {
        LOG.debug("checking certificate");

        Certificate userCertificate = cryptoWrapper.getUserCertificate(userId, requestCertificate);
        if (userCertificate == null)
            throw new WSSecurityHandlerException(String.format("unable to get user certificate from cryptoWrapper for userId '%s' and requestCertificate '%s'", userId, requestCertificate));

        try {
            LOG.debug("Verifying request certificate against user's public key");
            requestCertificate.verify(userCertificate.getPublicKey());
            LOG.debug("Request certificate has verified OK against user's public key");
        } catch (GeneralSecurityException e) {
            throw new WSSecurityHandlerException(e);
        }
    }

}
