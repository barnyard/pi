/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.api.service.UserNotFoundException;
import com.bt.pi.app.common.entities.User;

/**
 * A {@link CryptoFacade} wrapper treats the wrapped {@link CryptoFacade} as a cache for certificates from the
 * {@link UserManagementService}.
 */
@Component
public class UserServiceCachingCryptoFacadeWrapper {

    private static final String UNABLE_TO_DELETE_FROM_KEYSTORE = "Unable to delete key with alias '%s' from keystore";
    private static final String COULD_NOT_CACHE_MESSAGE = "Could not cache certificate for effectiveUserId '%s' in keystore";
    private static final Log LOG = LogFactory.getLog(UserServiceCachingCryptoFacadeWrapper.class);

    private UserManagementService userManagementService;
    private CryptoFacade cryptoFacade;
    private ByteArrayCertificateFactory certificateFactory;
    private CertificateExpiredPredicate expiredPredicate;
    private CertificateTimestampFormatter timestampFormatter;

    public UserServiceCachingCryptoFacadeWrapper() {
        userManagementService = null;
        cryptoFacade = null;
        certificateFactory = null;
        expiredPredicate = null;
        timestampFormatter = null;
    }

    @Resource
    public void setUserManagementService(UserManagementService aUserManagementService) {
        this.userManagementService = aUserManagementService;
    }

    @Resource
    public void setCertificateFactory(ByteArrayCertificateFactory aCertificateFactory) {
        this.certificateFactory = aCertificateFactory;
    }

    @Resource
    public void setCryptoFacade(CryptoFacade aCryptoFacade) {
        this.cryptoFacade = aCryptoFacade;
    }

    @Resource
    public void setExpiredPredicate(CertificateExpiredPredicate certificateExpiredPredicate) {
        this.expiredPredicate = certificateExpiredPredicate;
    }

    @Resource
    public void setTimestampFormatter(CertificateTimestampFormatter aTimestampFormatter) {
        this.timestampFormatter = aTimestampFormatter;
    }

    public Certificate getUserCertificate(String userId, Certificate requestCertificate) {
        String alias = cryptoFacade.getAlias(requestCertificate);

        if (alias != null) {
            LOG.debug(String.format("Found alias '%s' for request certificate from userId '%s'", alias, userId));
            if (expiredPredicate.isExpired(timestampFormatter.getTimestamp(alias))) {
                LOG.debug(String.format("Expiring certificate with alias '%s' from cache", alias));
                try {
                    cryptoFacade.deleteFromKeyStore(alias);
                } catch (GeneralSecurityException e) {
                    LOG.warn(String.format(UNABLE_TO_DELETE_FROM_KEYSTORE, alias), e);
                } catch (IOException e) {
                    LOG.warn(String.format(UNABLE_TO_DELETE_FROM_KEYSTORE, alias), e);
                }
            } else {
                LOG.debug(String.format("Using certificate with alias '%s' from cache", alias));
                return cryptoFacade.getCertificate(alias);
            }
        }

        LOG.debug("Did not find an alias for the request's certificate in the local keystore, checking in UserService");
        Certificate userCertificate = getCertificateFromUserService(userId);

        if (userCertificate != null) {
            try {
                alias = timestampFormatter.addOrUpdateTimestamp(userId);
                LOG.debug(String.format("Adding certificate from userService to local keystore with alias '%s'", alias));
                cryptoFacade.addToKeyStore(alias, userCertificate);
            } catch (GeneralSecurityException e) {
                LOG.debug(String.format(COULD_NOT_CACHE_MESSAGE, userId), e);
            } catch (IOException e) {
                LOG.debug(String.format(COULD_NOT_CACHE_MESSAGE, userId), e);
            }
        }
        return userCertificate;
    }

    private Certificate getCertificateFromUserService(String effectiveUserId) {
        User user;
        try {
            user = userManagementService.getUser(effectiveUserId);
        } catch (UserNotFoundException e) {
            LOG.debug("Could not find User " + effectiveUserId);
            return null;
        }
        try {
            final byte[] certificate_bytes = user.getCertificate();
            if (certificate_bytes != null) {
                return certificateFactory.generateCertificate(certificate_bytes);
            }
            return null;
        } catch (CertificateException e) {
            LOG.warn("Could not create certificate from data against " + user, e);
            return null;
        }
    }

    public Collection<WSSecurityEngineResult> processSecurityHeader(Document envelope) throws WSSecurityException {
        return Collections.unmodifiableCollection(cryptoFacade.processSecurityHeader(envelope));
    }
}
