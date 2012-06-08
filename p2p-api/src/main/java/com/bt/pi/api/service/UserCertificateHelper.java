package com.bt.pi.api.service;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.api.utils.CertificateUtils;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;

@Component
public class UserCertificateHelper {

    private static final Log LOG = LogFactory.getLog(UserCertificateHelper.class);

    private BlockingDhtCache blockingDhtCache;

    public UserCertificateHelper() {
        blockingDhtCache = null;
    }

    @Resource(name = "userBlockingCache")
    public void setUserCache(BlockingDhtCache aBlockingDhtCache) {
        this.blockingDhtCache = aBlockingDhtCache;
    }

    /**
     * Returns true if the user was created, false otherwise.
     * 
     * @param blockingDhtCache
     * @param userPiId
     * @param username
     * @param certificateAsByteArray
     * @return
     * @throws GeneralSecurityException
     */
    public boolean addCertificateToUser(PId userPiId, final String username, byte[] certificateAsByteArray) throws GeneralSecurityException {
        LOG.debug(String.format("addCertificateToUser(%s, %s)", username, certificateAsByteArray));
        final Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certificateAsByteArray));

        final CertificateUtils certificateUtils = new CertificateUtils();
        final AtomicBoolean isNewUser = new AtomicBoolean(false);
        blockingDhtCache.update(userPiId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                try {
                    if (null != existingEntity) {
                        LOG.debug(String.format("adding certificate to existing user '%s'", username));
                        if (!certificateUtils.areEqual(existingEntity.getCertificate(), certificate.getEncoded())) {
                            existingEntity.setCertificate(certificate.getEncoded());
                            return existingEntity;
                        } else
                            return null;
                    } else {
                        LOG.debug(String.format("creating new user '%s' and adding certificate", username));
                        User user = new User();
                        user.setUsername(username);
                        user.setCertificate(certificate.getEncoded());
                        isNewUser.getAndSet(true);
                        return user;
                    }
                } catch (CertificateEncodingException ex) {
                    LOG.error(ex);
                    return null;
                }
            }
        });
        return isNewUser.get();
    }

    /**
     * Returns true if the certificate was updated successfully.
     * 
     * @param userId
     * @param user
     * @return
     */
    public boolean updateUserCertificate(PId userId, final User user) {
        LOG.debug(String.format("updateUserCertificate(%s,%s)", userId, user));
        final AtomicBoolean certificateUpdated = new AtomicBoolean(false);
        blockingDhtCache.update(userId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                existingEntity.setCertificate(user.getCertificate());
                certificateUpdated.set(true);
                return existingEntity;
            }
        });
        return certificateUpdated.get();
    }

}
