/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.util.SecurityUtils;
import com.bt.pi.core.conf.Property;

@Component
public class KeyPairsServiceImpl extends ServiceBaseImpl implements KeyPairsService {
    private static final Log LOG = LogFactory.getLog(KeyPairsServiceImpl.class);

    private static final int THREE = 3;
    private static final int FOUR = 4;

    private int keySize;
    private String keyAlgorithm;

    @Resource
    private SecurityUtils securityUtils;
    @Resource
    private UserService userService;
    @Resource
    private UserManagementService userManagementService;

    public KeyPairsServiceImpl() {
        securityUtils = null;
        userService = null;
        userManagementService = null;
    }

    @Property(key = "security.key.generator.algorithm", defaultValue = "RSA")
    public void setKeyAlgorithm(String value) {
        keyAlgorithm = value;
    }

    @Property(key = "security.key.generator.size", defaultValue = "2048")
    public void setKeySize(int value) {
        keySize = value;
    }

    public KeyPair createKeyPair(final String ownerId, final String keyName) {
        LOG.info(String.format("createKeyPair(%s, %s", ownerId, keyName));

        if (ownerId == null || keyName == null)
            throw new IllegalArgumentException("KeyPair generation error. Owner id and key name must be specified.");

        java.security.KeyPair keyPair = null;
        try {
            keyPair = securityUtils.getNewKeyPair(keyAlgorithm, keySize);
        } catch (GeneralSecurityException e) {
            String message = String.format("Error generating new key pair %s for user %s", keyName, ownerId);
            LOG.warn(message, e);
            throw new RuntimeException(message);
        }

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        byte[] keyType = "ssh-rsa".getBytes();
        byte[] expBlob = publicKey.getPublicExponent().toByteArray();
        byte[] modBlob = publicKey.getModulus().toByteArray();
        byte[] authKeyBlob = new byte[THREE * FOUR + keyType.length + expBlob.length + modBlob.length];

        byte[] lenArray = BigInteger.valueOf(keyType.length).toByteArray();
        System.arraycopy(lenArray, 0, authKeyBlob, FOUR - lenArray.length, lenArray.length);
        System.arraycopy(keyType, 0, authKeyBlob, FOUR, keyType.length);

        lenArray = BigInteger.valueOf(expBlob.length).toByteArray();
        System.arraycopy(lenArray, 0, authKeyBlob, FOUR + keyType.length + FOUR - lenArray.length, lenArray.length);
        System.arraycopy(expBlob, 0, authKeyBlob, FOUR + (FOUR + keyType.length), expBlob.length);

        lenArray = BigInteger.valueOf(modBlob.length).toByteArray();
        System.arraycopy(lenArray, 0, authKeyBlob, FOUR + expBlob.length + FOUR + keyType.length + FOUR - lenArray.length, lenArray.length);
        System.arraycopy(modBlob, 0, authKeyBlob, FOUR + (FOUR + expBlob.length + (FOUR + keyType.length)), modBlob.length);

        String authKeyString = String.format("%s %s %s@pi", new String(keyType), new String(Base64.encode(authKeyBlob)), ownerId);
        String fingerPrint = securityUtils.getFingerPrint(keyPair.getPrivate());

        String privateKey = null;
        try {
            privateKey = new String(securityUtils.getPemBytes(keyPair.getPrivate()));
        } catch (IOException e) {
            String message = String.format("Error getting pem while generating new key pair %s for user %s", keyName, ownerId);
            LOG.warn(message, e);
            throw new RuntimeException(message);
        }

        final KeyPair newKeyPair = new KeyPair(keyName, fingerPrint, authKeyString);

        userService.addKeyPairToUser(ownerId, newKeyPair);

        return new KeyPair(keyName, fingerPrint, privateKey);
    }

    public boolean deleteKeyPair(final String ownerId, final String keyName) {
        LOG.info(String.format("deleteKeyPair(%s,%s)", ownerId, keyName));

        if (ownerId == null || keyName == null)
            throw new IllegalArgumentException("Cannot delete key pair. Owner id and key name must be specified.");

        userService.removeKeyPairFromUser(ownerId, new KeyPair(keyName));

        return true;
    }

    public List<KeyPair> describeKeyPairs(String ownerId, List<String> keyNames) {
        LOG.info(String.format("describeKeyPairs(%s,%s)", ownerId, keyNames));

        if (ownerId == null)
            throw new IllegalArgumentException("Owner id must be specified");

        User user = userManagementService.getUser(ownerId);

        if (keyNames != null && !keyNames.isEmpty()) {
            List<KeyPair> result = new ArrayList<KeyPair>();
            for (String keyName : keyNames) {
                KeyPair keyPair = user.getKeyPair(keyName);
                if (keyPair != null)
                    result.add(keyPair);
            }
            return result;
        }
        return new ArrayList<KeyPair>(user.getKeyPairs());
    }
}
