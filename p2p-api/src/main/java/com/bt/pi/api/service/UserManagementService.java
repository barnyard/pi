package com.bt.pi.api.service;

import java.security.GeneralSecurityException;

import javax.annotation.Resource;
import javax.management.JMException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.UserServiceHelper;
import com.bt.pi.app.common.entities.PiCertificate;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.UserAccessKey;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

@ManagedResource(description = "The service layer for user interactions within the DHT", objectName = "bean:name=userService")
@Component
public class UserManagementService {
    private static final String USER_TYPE = "User";

    private static final Log LOG = LogFactory.getLog(UserManagementService.class);

    private static final String CREATE_USER = "Create a new user and return that user's certificate";
    private static final String ADD_USER_INTO_DHT = "Add a user into the DHT";
    private static final String USERNAME = "username";
    private static final String ACCESS_KEY = "accessKey";
    private static final String ACCESS_KEY_DESC = "API Access Key";
    private static final String SECRET_KEY = "secretKey";
    private static final String SECRET_KEY_DESC = "API Secret Key";
    private static final String ENCODED_CERTIFICATE = "encodedCertificate";
    private static final String ENCODED_CERTIFICATE_DESC = "Base64 encoded String of the byte array that is the certificate";
    private static final String CREATE_NEW_CERTIFICATE_FOR_USER = "Create a new certificate for user and return it";
    private static final String EMAIL_ADDRESS = "emailAddress";
    private static final String EMAIL_ADDRESS_DESC = "User's email address";
    private static final String REAL_NAME = "realName";
    private static final String REAL_NAME_DESC = "User's real name";
    private static final String DEFAULT = "default";
    private static final String ERROR_CREATING_KEYS_AND_CERTIFICATE_FOR_USER_S = "Error creating keys and certificate for user %s";
    private static final String CANNOT_FIND_USER = "Cannot find user %s";
    private static final String TYPE_MISMATCH = "Expected %s but got %s";
    private static final String USER_NOT_FOUND = "User not found";

    @Resource
    private UserServiceHelper userServiceHelper;
    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource(name = "userBlockingCache")
    private BlockingDhtCache blockingDhtCache;

    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private SecurityGroupService securityGroupService;
    @Resource
    private UserCertificateHelper userCertificateHelper;

    @Resource
    private DeleteUserHelper deleteUserHelper;

    public UserManagementService() {
        userServiceHelper = null;
        piIdBuilder = null;
        blockingDhtCache = null;
        securityGroupService = null;
        userCertificateHelper = null;

    }

    @ManagedOperation(description = ADD_USER_INTO_DHT)
    @ManagedOperationParameters({ @ManagedOperationParameter(name = USERNAME, description = USERNAME), @ManagedOperationParameter(name = ACCESS_KEY, description = ACCESS_KEY_DESC),
            @ManagedOperationParameter(name = SECRET_KEY, description = SECRET_KEY_DESC) })
    public void addUser(String username, String accessKey, String secretKey) {
        LOG.debug(String.format("addUser(%s, %s, %s)", username, accessKey, secretKey));
        User user = new User(username, accessKey, secretKey);
        addUser(user);
    }

    @ManagedOperation(description = ADD_USER_INTO_DHT)
    @ManagedOperationParameters({ @ManagedOperationParameter(name = USERNAME, description = USERNAME), @ManagedOperationParameter(name = ACCESS_KEY, description = ACCESS_KEY_DESC),
            @ManagedOperationParameter(name = SECRET_KEY, description = SECRET_KEY_DESC), @ManagedOperationParameter(name = ENCODED_CERTIFICATE, description = ENCODED_CERTIFICATE_DESC) })
    public void addUser(String username, String accessKey, String secretKey, String encodedCertificate) throws GeneralSecurityException {
        LOG.debug(String.format("addUser(%s, %s, %s, %s)", username, accessKey, secretKey, encodedCertificate));
        User user = new User(username, accessKey, secretKey);
        user.setCertificate(userServiceHelper.decodeBase64(encodedCertificate));
        addUser(user);
    }

    public boolean addUser(final User user) {
        LOG.debug(String.format("addUser(%s)", user));
        if (null == user.getApiAccessKey() || user.getApiAccessKey().isEmpty())
            user.setApiAccessKey(userServiceHelper.generateAccessKey(user.getUsername()));
        if (null == user.getApiSecretKey() || user.getApiSecretKey().isEmpty())
            user.setApiSecretKey(userServiceHelper.generateSecretKey(user.getUsername()));

        final PId userPiId = piIdBuilder.getPId(user);
        final UserAccessKey userAccessKey = new UserAccessKey(user.getUsername(), user.getApiAccessKey());
        final PId accessKeyId = piIdBuilder.getPId(userAccessKey);
        if (blockingDhtCache.writeIfAbsent(userPiId, user)) {
            LOG.info(String.format("Creating UserAccessKey index for user '%s'", user.getUsername()));
            blockingDhtCache.writeIfAbsent(accessKeyId, userAccessKey);
            securityGroupService.createSecurityGroup(user.getUsername(), DEFAULT, DEFAULT);

            return true;
        }
        return false;
    }

    @ManagedOperation(description = "Add a Base64 encoded string of a certificate to a user, if the user does not exist, the user is created")
    @ManagedOperationParameters({ @ManagedOperationParameter(name = USERNAME, description = USERNAME), @ManagedOperationParameter(name = ENCODED_CERTIFICATE, description = ENCODED_CERTIFICATE_DESC) })
    public void addCertificateToUser(String username, String encodedCertificate) throws GeneralSecurityException {
        LOG.debug(String.format("addBase64EncodedCertificateToUser(%s, %s)", username, encodedCertificate));
        byte[] certificateAsByteArray = userServiceHelper.decodeBase64(encodedCertificate);
        boolean isNewUser = userCertificateHelper.addCertificateToUser(piIdBuilder.getPId(User.getUrl(username)), username, certificateAsByteArray);

        if (isNewUser) {
            LOG.debug("New user was created");
            securityGroupService.createSecurityGroup(username, DEFAULT, DEFAULT);
        }
    }

    @ManagedOperation(description = CREATE_USER)
    @ManagedOperationParameters({ @ManagedOperationParameter(name = USERNAME, description = USERNAME), @ManagedOperationParameter(name = REAL_NAME, description = REAL_NAME_DESC),
            @ManagedOperationParameter(name = EMAIL_ADDRESS, description = EMAIL_ADDRESS_DESC) })
    public byte[] createUser(String username, String realName, String emailAddress) throws JMException {
        try {
            LOG.debug(String.format("createUser(%s, %s, %s)", username, realName, emailAddress));

            User user = new User(username, realName, emailAddress, true);
            byte[] certificateZip = userServiceHelper.setUserCertAndKeysAndGetX509Zip(user);

            if (addUser(user))
                return certificateZip;
            else
                throw new JMException(String.format("User %s already exists", username));
        } catch (Throwable t) {
            LOG.error("Exception: !!!!!!!!!!" + t);
            String message = String.format(ERROR_CREATING_KEYS_AND_CERTIFICATE_FOR_USER_S, username);
            LOG.error(message, t);
            throw new JMException(message);
        }
    }

    @ManagedOperation(description = CREATE_NEW_CERTIFICATE_FOR_USER)
    @ManagedOperationParameters({ @ManagedOperationParameter(name = USERNAME, description = USERNAME) })
    public byte[] createUserCertificate(String username) throws JMException {
        LOG.debug(String.format("createUserCertificate(%s)", username));
        try {
            PId userId = piIdBuilder.getPId(User.getUrl(username));
            final User user = (User) blockingDhtCache.get(userId);
            byte[] certificateZip = userServiceHelper.setUserCertAndGetX509Zip(user);
            if (userCertificateHelper.updateUserCertificate(userId, user)) {
                return certificateZip;
            } else
                throw new JMException("Unable to update user with new certificate.");
        } catch (Throwable t) {
            String message = String.format(ERROR_CREATING_KEYS_AND_CERTIFICATE_FOR_USER_S, username);
            LOG.error(message, t);
            throw new JMException(message);
        }
    }

    @ManagedOperation(description = "Create Pi's security certificate")
    @ManagedOperationParameters({})
    public boolean createPiCertificate() throws JMException {
        LOG.debug("createPiCertificate()");
        try {
            PiCertificate piCertificate = userServiceHelper.createPiCertificate();
            return blockingDhtCache.writeIfAbsent(piIdBuilder.getPId(piCertificate), piCertificate);
        } catch (Throwable t) {
            String message = "Error creating pi's security certificate";
            LOG.error(message, t);

            LOG.error("pooh !!!!! --- " + t);
            throw new JMException(message);
        }
    }

    public byte[] updateUserCertificate(String username) {
        PId userId = piIdBuilder.getPId(User.getUrl(username));
        final User user = (User) blockingDhtCache.get(userId);
        byte[] certificateZip;
        try {
            certificateZip = userServiceHelper.setUserCertAndGetX509Zip(user);
        } catch (Throwable t) {
            throw new CertificateGenerationException(ERROR_CREATING_KEYS_AND_CERTIFICATE_FOR_USER_S, t);
        }
        if (userCertificateHelper.updateUserCertificate(userId, user)) {
            return certificateZip;
        } else
            throw new CertificateGenerationException(ERROR_CREATING_KEYS_AND_CERTIFICATE_FOR_USER_S, null);
    }

    public void updateUser(final String username, final String realName, final String emailAddress, final Boolean enabled, final String externalRefId, final Integer maxInstances, final Integer maxCores) {
        LOG.debug(String.format("Updating user %s", username));

        PId userId = piIdBuilder.getPId(User.getUrl(username));

        blockingDhtCache.update(userId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                if (null != existingEntity && !existingEntity.isDeleted()) {

                    validateArguments(realName, emailAddress, enabled, externalRefId, maxInstances, maxCores, existingEntity);

                    return existingEntity;
                } else {
                    return null;
                }
            }

        });
    }

    private void validateArguments(final String realName, final String emailAddress, final Boolean enabled, final String externalRefId, final Integer maxInstances, final Integer maxCores, User existingEntity) {
        if (null != enabled)
            existingEntity.setEnabled(enabled);

        if (!StringUtils.isEmpty(realName))
            existingEntity.setRealName(realName);

        if (!StringUtils.isEmpty(emailAddress))
            existingEntity.setEmailAddress(emailAddress);

        if (!StringUtils.isEmpty(externalRefId))
            existingEntity.setExternalRefId(externalRefId);

        if (null == maxInstances || maxInstances > -1)
            existingEntity.setMaxInstances(maxInstances);

        if (null == maxCores || maxCores > -1)
            existingEntity.setMaxCores(maxCores);
    }

    public User getUser(String username) {
        LOG.debug(String.format("getUser(%s)", username));
        PId id = piIdBuilder.getPId(User.getUrl(username));
        PiEntity piEntity = blockingDhtCache.get(id);
        if (null == piEntity)
            throw new UserNotFoundException(String.format(CANNOT_FIND_USER, username));
        if (!(piEntity instanceof User))
            throw new EntityMismatchException(String.format(TYPE_MISMATCH, USER_TYPE, piEntity.getClass()));
        return (User) piEntity;
    }

    private User getUserFromDht(String username) {
        LOG.debug(String.format("getUserFromDht(%s)", username));
        PId id = piIdBuilder.getPId(User.getUrl(username));

        PiEntity piEntity = dhtClientFactory.createBlockingReader().get(id);
        if (null == piEntity)
            throw new UserNotFoundException(String.format(CANNOT_FIND_USER, username));
        if (!(piEntity instanceof User))
            throw new EntityMismatchException(String.format(TYPE_MISMATCH, USER_TYPE, piEntity.getClass()));
        return (User) piEntity;
    }

    public User getUserByApiAccessKey(String accessKey) {
        LOG.debug(String.format("getUserByAccessKey(%s)", accessKey));
        PId id = piIdBuilder.getPId(UserAccessKey.getUrl(accessKey));
        PiEntity piEntity = blockingDhtCache.get(id);
        User user;
        if (null == piEntity)
            throw new UserNotFoundException(USER_NOT_FOUND);
        if (piEntity instanceof UserAccessKey) {
            UserAccessKey userAccessKey = (UserAccessKey) piEntity;
            user = getUser(userAccessKey.getUsername());
        } else {
            throw new EntityMismatchException(String.format(TYPE_MISMATCH, "UserAccessKey", piEntity.getClass()));
        }
        return user;
    }

    public void deleteUser(final String username) {
        LOG.debug("Deleting user " + username);

        PId userId = piIdBuilder.getPId(User.getUrl(username));
        User user = getUserFromDht(username);

        deleteUserHelper.cleanupUserResources(user);
        blockingDhtCache.update(userId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                if (null != existingEntity && !existingEntity.isDeleted()) {
                    existingEntity.setDeleted(true);
                    return existingEntity;
                } else {
                    return null;
                }
            }
        });
    }

    public void setUserEnabledFlag(final String username, final boolean enabled) {
        LOG.debug("Disabling user: " + username);
        PId userId = piIdBuilder.getPId(User.getUrl(username));
        blockingDhtCache.update(userId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                if (null != existingEntity && !existingEntity.isDeleted()) {
                    existingEntity.setEnabled(enabled);
                    return existingEntity;
                } else {
                    return null;
                }
            }
        });

    }
}
