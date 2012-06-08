/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.servlet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.api.service.UserNotFoundException;
import com.bt.pi.app.common.entities.User;

public class AwsQuerySecurityHandlerTest {

    private static final Log LOG = LogFactory.getLog(AwsQuerySecurityHandlerTest.class);

    private static final String SIGNATURE = "Signature";
    private static final String EXPIRES = "Expires";
    private static final String ACTION = "Action";
    private static final String TIMESTAMP = "Timestamp";
    private static final String AWS_ACCESS_KEY_ID = "AWSAccessKeyId";
    private static final String VERSION = "Version";
    private static final String SIGNATURE_METHOD = "SignatureMethod";
    private static final String SIGNATURE_VERSION = "SignatureVersion";
    private static final String ADDRESSING_TYPE = "AddressingType";
    private static final String IMAGE_ID = "ImageId";
    private static final String KEY_NAME = "KeyName";
    private static final String INSTANCE_TYPE = "InstanceType";
    private static final String MAX_COUNT = "MaxCount";
    private static final String REQUEST_RUN_INSTANCES = "RunInstances";
    private static final String REQUEST_ADDRESSING_TYPE = "public";
    private static final String REQUEST_TIMESTAMP = "2009-10-13T15%3A27%3A44Z";
    private static final String REQUEST_ACCESS_KEY = "Y7HqdJgHC7aYIbnxPRNU1A";
    private static final String REQUEST_VERSION = "2008-12-01";
    private static final String REQUEST_SIGNATURE = "SZh1ElKB%2F8Iv0U6k2g1eyRhlCqE%3D";
    private static final String REQUEST_IMAGE_ID = "emi-123";
    private static final String REQUEST_INSTANCE_TYPE = "m1.small";
    private static final String REQUEST_SIGNATURE_METHOD = "HmacSHA1";
    private static final String REQUEST_SECRET_KEY = "sIL03bvLbAa__Oa7Oe5Ssuhjalg6m-cd0RDwOg";
    private static final String MIN_COUNT = "MinCount";
    private static final String SECURITY_GROUP = "SecurityGroup.1";
    private static final String HOST = "localhost";
    private static final String PATH = "/";
    private static final String METHOD = "GET";
    private static final String DEFAULT = "default";
    private static final String PUBLIC = "public";

    private AwsQuerySecurityHandler querySecurityHandler;
    private Map<String, String> parameters;
    private UserManagementService userManagementService;

    @Before
    public void before() {
        querySecurityHandler = new AwsQuerySecurityHandler();
        User user = new User("Jon", REQUEST_ACCESS_KEY, REQUEST_SECRET_KEY);

        userManagementService = mock(UserManagementService.class);
        when(userManagementService.getUserByApiAccessKey(REQUEST_ACCESS_KEY)).thenReturn(user);
        querySecurityHandler.setUserManagementService(userManagementService);

        parameters = new HashMap<String, String>();
        parameters.put(ACTION, REQUEST_RUN_INSTANCES);
        parameters.put(EXPIRES, REQUEST_TIMESTAMP);
        parameters.put(AWS_ACCESS_KEY_ID, REQUEST_ACCESS_KEY);
        parameters.put(VERSION, REQUEST_VERSION);
        parameters.put(SIGNATURE, REQUEST_SIGNATURE);
        parameters.put(SIGNATURE_METHOD, REQUEST_SIGNATURE_METHOD);
        parameters.put(SIGNATURE_VERSION, "2");
        parameters.put(ADDRESSING_TYPE, PUBLIC);
        parameters.put(IMAGE_ID, REQUEST_IMAGE_ID);
        parameters.put(INSTANCE_TYPE, REQUEST_INSTANCE_TYPE);
        parameters.put(KEY_NAME, DEFAULT);
        parameters.put(MAX_COUNT, "1");
        parameters.put(MIN_COUNT, "1");
        parameters.put(SECURITY_GROUP, DEFAULT);

    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorWhenBothTimestampAndExpiresPresent() {
        // setup
        parameters.put(TIMESTAMP, "2006-07-07T15:04:56Z");
        parameters.put(EXPIRES, "2006-07-07T15:04:56Z");
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(true, ex.getMessage().equals("Requests must include either Timestamp or Expires, but cannot contain both"));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorWithoutTimestamp() {
        // setup
        parameters.remove(EXPIRES);
        parameters.remove(TIMESTAMP);
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(true, ex.getMessage().contains(TIMESTAMP));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorOnInvalidTimestampPattern() {
        // setup
        parameters.remove(TIMESTAMP);
        parameters.put(TIMESTAMP, "Time for some action");
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            LOG.debug(ex.getMessage());
            assertEquals(true, ex.getMessage().contains(TIMESTAMP));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorWithoutExpiresAndWithoutTimestamp() {
        // setup
        parameters.remove(EXPIRES);
        parameters.remove(TIMESTAMP);
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(true, ex.getMessage().contains(EXPIRES));
            assertEquals(true, ex.getMessage().contains(TIMESTAMP));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorOnInvalidExpiresPattern() {
        // setup
        parameters.remove(EXPIRES);
        parameters.put(EXPIRES, "Time for some action");
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(true, ex.getMessage().contains(EXPIRES));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorWithoutAction() {
        // setup
        parameters.remove(ACTION);
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (IllegalArgumentException ex) {
            // assert
            assertEquals(true, ex.getMessage().contains(ACTION));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorOnInvalidActionPattern() {
        // setup
        parameters.remove(ACTION);
        parameters.put(ACTION, "Time for some action");
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(true, ex.getMessage().contains(ACTION));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorWithoutAwsSecretKey() {
        // setup
        parameters.remove(AWS_ACCESS_KEY_ID);
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(true, ex.getMessage().contains(AWS_ACCESS_KEY_ID));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorWithoutVersion() {
        // setup
        parameters.remove(VERSION);
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(true, ex.getMessage().contains(VERSION));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorOnInvalidVersion() {
        // setup
        String invalidVersion = "2008-09-03";
        parameters.put(VERSION, invalidVersion);

        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(String.format("version %s is not supported", invalidVersion), ex.getMessage());
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorWithoutSignature() {
        // setup
        parameters.remove(SIGNATURE);
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(true, ex.getMessage().contains(SIGNATURE));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorWithoutSignatureMethod() {
        // setup
        parameters.remove(SIGNATURE_METHOD);
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(true, ex.getMessage().contains(SIGNATURE_METHOD));
            throw ex;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorWithoutSignatureVersion() {
        // setup
        parameters.remove(SIGNATURE_VERSION);
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals(true, ex.getMessage().contains(SIGNATURE_VERSION));
            throw ex;
        }
    }

    @Test
    public void shouldReturnStringToSign() throws Throwable {
        // setup
        parameters.remove(TIMESTAMP);
        parameters.put(EXPIRES, REQUEST_TIMESTAMP);
        String stringThatShouldBeReturned = "GET\n" + "localhost\n" + "/\n" + "AWSAccessKeyId=" + REQUEST_ACCESS_KEY + "&Action=" + REQUEST_RUN_INSTANCES + "&AddressingType=" + REQUEST_ADDRESSING_TYPE + "&Expires="
                + REQUEST_TIMESTAMP.replace(":", "%3A") + "&ImageId=" + REQUEST_IMAGE_ID + "&InstanceType=m1.small" + "&KeyName=default" + "&MaxCount=1" + "&MinCount=1" + "&SecurityGroup.1=default" + "&SignatureMethod=" + REQUEST_SIGNATURE_METHOD
                + "&SignatureVersion=2" + "&Version=" + REQUEST_VERSION;
        // act
        String stringToSign = querySecurityHandler.createCanonicalSubject(parameters, HOST, PATH, METHOD);
        // assert
        assertEquals(stringThatShouldBeReturned, stringToSign);
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldErrorWithInvalidSignature() {
        // setup
        // Note: if the new timestamp format is not good a different error will be thrown.
        parameters.remove(TIMESTAMP);
        parameters.put(EXPIRES, "2009-10-13T15%3A27%3A44.000Z");
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException ex) {
            // assert
            assertEquals("Invalid signature", ex.getMessage());
            throw ex;
        }
    }

    @Test
    public void shouldReturnUsernameOnValidSignature() {
        // act
        String username = querySecurityHandler.validate(parameters, HOST, PATH, METHOD);

        // assert
        assertEquals("Jon", username);
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldThrowWhenUserNotFound() {
        // setup
        when(userManagementService.getUserByApiAccessKey(REQUEST_ACCESS_KEY)).thenThrow(new UserNotFoundException("User not found"));

        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException e) {
            // assert
            assertEquals("User not found", e.getMessage());
            throw e;
        }
    }

    @Test(expected = WSSecurityHandlerException.class)
    public void shouldNotAuthenticateDisabledUsers() throws Exception {
        // setup
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("username");
        when(user.isEnabled()).thenReturn(false);
        when(userManagementService.getUserByApiAccessKey(REQUEST_ACCESS_KEY)).thenReturn(user);
        // act
        try {
            querySecurityHandler.validate(parameters, HOST, PATH, METHOD);
        } catch (WSSecurityHandlerException e) {
            // assert
            assertEquals("User username is not enabled", e.getMessage());
            throw e;
        }

    }
}
