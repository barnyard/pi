/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.servlet;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.api.service.UserNotFoundException;
import com.bt.pi.app.common.entities.User;

/**
 * Using the request, check that given request parameters are present and are in a certain format then retrieve the
 * secret key from the user collection and use that to sign the request and validate that the signature sent on the
 * request matches.
 */
@Component
public class AwsQuerySecurityHandler {
    private static final String USER_S_IS_NOT_ENABLED = "User %s is not enabled";
    private static final Log LOG = LogFactory.getLog(AwsQuerySecurityHandler.class);
    private static final String REQUESTS_MUST_INCLUDE_EITHER_TIMESTAMP_OR_EXPIRES_BUT_CANNOT_CONTAIN_BOTH = "Requests must include either Timestamp or Expires, but cannot contain both";
    private static final String PARAMETER_NOT_IN_CORRECT_FORMAT = "Parameter %s not in correct format";
    private static final String MISSING_ARGUMENT = "Missing argument %s";
    private static final String PATTERN_TIMESTAMP = "^\\d{4}-\\d{2}-\\d{2}[A-Z]\\d{2}%3A\\d{2}%3A\\d{2}[A-Z]$";
    private static final String ALTERNATE_PATTERN_TIMESTAMP = "^\\d{4}-\\d{2}-\\d{2}[T]\\d{2}%3A\\d{2}%3A\\d{2}[.]{0,1}\\d{3,}[Z]{0,1}$";
    private static final String PATTERN_CAMAL_CASE = "^[A-Z][a-zA-Z]*$";
    private static final String UTF8 = "UTF-8";
    private static final String SIGNATURE_VERSION = "SignatureVersion";
    private static final String SIGNATURE_METHOD = "SignatureMethod";
    private static final String SIGNATURE = "Signature";
    private static final String EXPIRES = "Expires";
    private static final String VERSION = "Version";
    private static final String ACTION = "Action";
    private static final String TIMESTAMP = "Timestamp";
    private static final String TIMESTAMP_OR_EXPIRES = TIMESTAMP + " or " + EXPIRES;
    private static final String AWS_ACCESS_KEY_ID = "AWSAccessKeyId";
    private static final String PLUS = "\\+";
    private static final String SPACE = " ";

    private UserManagementService userManagementService;

    public AwsQuerySecurityHandler() {
        userManagementService = null;
    }

    @Resource
    public void setUserManagementService(UserManagementService aUserManagementService) {
        userManagementService = aUserManagementService;
    }

    // TODO: make this return the userid
    public String validate(Map<String, String> parameters, String host, String path, String method) {
        LOG.debug(String.format("validate(%s, %s, %s, %s)", parameters, host, path, method));
        User user;
        try {
            validateRequiredParameters(parameters);
            validateParameterPatterns(parameters);
            String apiAccessKey = parameters.get(AWS_ACCESS_KEY_ID);
            user = userManagementService.getUserByApiAccessKey(apiAccessKey);
            if (!user.isEnabled()) {
                LOG.debug(String.format(USER_S_IS_NOT_ENABLED, user.getUsername()));
                throw new WSSecurityHandlerException(String.format(USER_S_IS_NOT_ENABLED, user.getUsername()), HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (IllegalArgumentException e) {
            throw new WSSecurityHandlerException(e.getMessage());
        } catch (UserNotFoundException e) {
            throw new WSSecurityHandlerException(e.getMessage());
        }
        String canonicalSubject = createCanonicalSubject(parameters, host, path, method);
        String signature;
        try {
            signature = URLDecoder.decode(parameters.get(SIGNATURE), UTF8);
        } catch (UnsupportedEncodingException ex) {
            throw new WSSecurityHandlerException(ex.getMessage());
        }
        validateSignature(parameters.get(SIGNATURE_METHOD), signature, user.getApiSecretKey(), canonicalSubject);

        return user.getUsername();
    }

    private void validateRequiredParameters(Map<String, String> parameters) {
        LOG.debug(String.format("validateRequiredParameters(%s)", parameters));
        checkParameter(ACTION, parameters);
        checkParameter(AWS_ACCESS_KEY_ID, parameters);
        checkParameter(VERSION, parameters);
        validateVersion(parameters.get(VERSION));
        checkParameter(SIGNATURE, parameters);
        checkParameter(SIGNATURE_METHOD, parameters);
        checkParameter(SIGNATURE_VERSION, parameters);

        if (parameters.containsKey(TIMESTAMP) && parameters.containsKey(EXPIRES))
            throw new IllegalArgumentException(REQUESTS_MUST_INCLUDE_EITHER_TIMESTAMP_OR_EXPIRES_BUT_CANNOT_CONTAIN_BOTH);
        if (!parameters.containsKey(TIMESTAMP) && !parameters.containsKey(EXPIRES))
            throw new IllegalArgumentException(String.format(MISSING_ARGUMENT, TIMESTAMP_OR_EXPIRES));
    }

    private void validateVersion(String version) {
        LOG.debug(String.format("validateVersion(%s)", version));
        for (String validVersion : RestQueryFilter.SUPPORTED_VERSIONS)
            if (validVersion.equals(version))
                return;
        throw new IllegalArgumentException(String.format("version %s is not supported", version));
    }

    private void checkParameter(String name, Map<String, String> parameters) {
        if (!parameters.containsKey(name))
            throw new IllegalArgumentException(String.format(MISSING_ARGUMENT, name));
    }

    private void validateParameterPatterns(Map<String, String> parameters) {
        LOG.debug(String.format("validateParameterPatterns(%s)", parameters));
        if (!((String) parameters.get(ACTION)).matches(PATTERN_CAMAL_CASE))
            throw new IllegalArgumentException(String.format(PARAMETER_NOT_IN_CORRECT_FORMAT, ACTION));
        if (parameters.containsKey(TIMESTAMP) && !(parameters.get(TIMESTAMP).matches(PATTERN_TIMESTAMP) || parameters.get(TIMESTAMP).matches(ALTERNATE_PATTERN_TIMESTAMP)))
            throw new IllegalArgumentException(String.format(PARAMETER_NOT_IN_CORRECT_FORMAT, TIMESTAMP));
        if (parameters.containsKey(EXPIRES) && !(parameters.get(EXPIRES).matches(PATTERN_TIMESTAMP) || parameters.get(EXPIRES).matches(ALTERNATE_PATTERN_TIMESTAMP)))
            throw new IllegalArgumentException(String.format(PARAMETER_NOT_IN_CORRECT_FORMAT, EXPIRES));
    }

    protected String createCanonicalSubject(Map<String, String> parameters, String host, String path, String method) {
        LOG.debug(String.format("createCanonicalSubject(%s, %s, %s, %s)", parameters, host, path, method));
        StringBuilder canonicalSubject = new StringBuilder();
        canonicalSubject.append(String.format("%s\n%s\n%s\n", method, host, path));

        NavigableSet<String> sortedKeys = new TreeSet<String>();
        sortedKeys.addAll(parameters.keySet());

        String key = sortedKeys.pollFirst();
        // String value = URLEncoder.encode(parameters.get(key).replaceAll(PLUS,
        // SPACE), UTF8);
        String value = parameters.get(key).replaceAll(PLUS, SPACE);
        canonicalSubject.append(String.format("%s=%s", key, value));
        while (true) {
            key = sortedKeys.pollFirst();
            if (key == null)
                break;
            // We don't want to sign the signature, nor do we want to sign a
            // null value
            if (!key.equals(SIGNATURE) && null != parameters.get(key)) {
                // value =
                // URLEncoder.encode(parameters.get(key).replaceAll(PLUS,
                // SPACE), UTF8);
                value = parameters.get(key).replaceAll(PLUS, SPACE);
                canonicalSubject.append(String.format("&%s=%s", key, value));
            }
        }
        return canonicalSubject.toString();
    }

    private void validateSignature(String algorithm, String signature, String awsSecretKey, String canonicalSubject) {
        LOG.debug(String.format("validateSignature(%s, %s, %s, %s)", algorithm, signature, awsSecretKey, canonicalSubject));
        try {
            byte[] accessKey = awsSecretKey.getBytes();
            LOG.debug(String.format("Creating signing key from access key %s and algorithm %s", awsSecretKey, algorithm));
            SecretKeySpec keySpec = new SecretKeySpec(accessKey, algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(keySpec);
            byte[] signedBytes;
            try {
                signedBytes = mac.doFinal(canonicalSubject.getBytes(UTF8));
            } catch (UnsupportedEncodingException e) {
                signedBytes = mac.doFinal(canonicalSubject.getBytes());
            }
            String b64 = new String(Base64.encodeBase64(signedBytes));

            LOG.debug(String.format("Comparing generated signature %s against signature from request %s", b64, signature));
            if (!signature.equals(b64))
                throw new WSSecurityHandlerException("Invalid signature");

        } catch (NoSuchAlgorithmException ex) {
            throw new WSSecurityHandlerException(ex.getMessage());
        } catch (InvalidKeyException ex) {
            throw new WSSecurityHandlerException(ex.getMessage());
        }
    }
}
