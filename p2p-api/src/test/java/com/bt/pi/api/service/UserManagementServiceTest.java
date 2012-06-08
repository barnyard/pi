package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.management.JMException;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.UserServiceHelper;
import com.bt.pi.app.common.entities.PiCertificate;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.UserAccessKey;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class UserManagementServiceTest {
    private byte[] certAsByteArray = new byte[] { 48, -126, 3, -111, 48, -126, 2, 121, -96, 3, 2, 1, 2, 2, 6, 1, 34, -88, -124, 126, -8, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 13, 5, 0, 48, 127, 49, 11, 48, 9, 6, 3, 85, 4, 6, 19, 2, 85, 83,
            49, 11, 48, 9, 6, 3, 85, 4, 8, 19, 2, 67, 65, 49, 22, 48, 20, 6, 3, 85, 4, 7, 19, 13, 83, 97, 110, 116, 97, 32, 66, 97, 114, 98, 97, 114, 97, 49, 25, 48, 23, 6, 3, 85, 4, 10, 19, 16, 107, 111, 97, 108, 97, 45, 114, 111, 98, 117, 115,
            116, 110, 101, 115, 115, 49, 19, 48, 17, 6, 3, 85, 4, 11, 19, 10, 69, 117, 99, 97, 108, 121, 112, 116, 117, 115, 49, 27, 48, 25, 6, 3, 85, 4, 3, 19, 18, 119, 119, 119, 46, 101, 117, 99, 97, 108, 121, 112, 116, 117, 115, 46, 99, 111, 109,
            48, 30, 23, 13, 48, 57, 48, 55, 50, 51, 49, 54, 52, 57, 51, 49, 90, 23, 13, 49, 52, 48, 55, 50, 51, 49, 54, 52, 57, 51, 49, 90, 48, 127, 49, 11, 48, 9, 6, 3, 85, 4, 6, 19, 2, 85, 83, 49, 11, 48, 9, 6, 3, 85, 4, 8, 19, 2, 67, 65, 49, 22,
            48, 20, 6, 3, 85, 4, 7, 19, 13, 83, 97, 110, 116, 97, 32, 66, 97, 114, 98, 97, 114, 97, 49, 25, 48, 23, 6, 3, 85, 4, 10, 19, 16, 107, 111, 97, 108, 97, 45, 114, 111, 98, 117, 115, 116, 110, 101, 115, 115, 49, 19, 48, 17, 6, 3, 85, 4, 11,
            19, 10, 69, 117, 99, 97, 108, 121, 112, 116, 117, 115, 49, 27, 48, 25, 6, 3, 85, 4, 3, 19, 18, 119, 119, 119, 46, 101, 117, 99, 97, 108, 121, 112, 116, 117, 115, 46, 99, 111, 109, 48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9,
            13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126, 1, 10, 2, -126, 1, 1, 0, -102, 86, -42, -105, -103, -13, -125, -58, -35, 41, -113, 16, -107, -20, -24, -39, 41, -109, 81, -69, -30, -53, 118, 22, -51, -124, -127, 30, -28, 29, -120, 64,
            -86, 79, -11, -48, -27, -7, 93, -116, 28, 25, 113, -29, -84, 111, -25, 108, -99, 118, 91, -26, -13, 40, -119, -6, -20, -126, 29, 70, 101, 83, -108, 1, 3, 36, -67, 9, -49, 125, 121, 54, 38, 52, 36, 39, 118, 40, -108, 36, -85, -116, 67,
            -80, -45, 79, -94, -63, 125, 48, -126, -69, -5, 65, 16, -116, -44, -32, 113, 61, -78, -18, 94, -62, 39, 90, -98, -121, 56, -5, -114, 36, -38, 116, 19, 12, -111, 78, 12, 112, 72, 42, 116, -40, 99, -36, -55, -44, 79, 121, 38, -17, -61,
            -88, 126, 6, 28, -123, 27, -89, 8, 22, 124, 32, -42, 63, 125, 59, -26, -60, -42, 13, 68, -73, -53, -71, 91, -122, 64, -2, 57, 124, 81, 28, 26, -86, -14, -61, -68, 114, 115, -28, -68, 51, -104, 27, 55, 48, 111, -3, 33, -44, -119, 88, -44,
            -6, -72, -72, -20, -23, -114, 30, 80, 79, -93, 21, 92, 124, -56, -120, 38, -43, 94, -97, 4, 45, 64, -47, 69, 48, -60, 54, -84, -14, -23, 56, -106, 80, 38, -70, -60, -79, 105, -22, -120, -19, 95, 5, 103, 120, 99, -93, 10, 97, -37, 4,
            -121, 87, 114, 26, -34, -70, 121, 98, 75, -82, 20, 8, -32, 16, 110, -42, -116, 10, 45, -71, 2, 3, 1, 0, 1, -93, 19, 48, 17, 48, 15, 6, 3, 85, 29, 19, 1, 1, -1, 4, 5, 48, 3, 1, 1, -1, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 13, 5,
            0, 3, -126, 1, 1, 0, 109, -122, 104, 79, 83, 0, -120, -108, -76, -64, -73, 105, -37, -68, 57, -16, 110, 40, -90, 6, 0, -18, 41, -122, 51, -44, 22, -35, 1, -123, 115, 77, 31, -87, -18, -12, 5, -34, -102, -70, 112, 123, -125, -68, -93,
            -58, 36, 102, -113, 34, 42, 16, -4, -126, -43, 54, 90, 116, -117, 86, 85, -49, -78, 34, 77, -70, 40, 60, 68, -122, -53, 28, -64, -103, -2, 2, -103, 25, 93, -116, -93, 36, 10, -91, 109, -27, -15, 87, 112, 47, 53, 125, 24, 102, 91, -69, 6,
            -58, 90, -96, -19, -29, -2, -53, -105, 15, 80, -123, -120, -44, 58, -115, 21, -26, -5, 104, 110, -62, -87, -81, 96, 109, -41, 3, 24, 13, 121, -119, 78, 2, -36, 0, 125, -125, -2, -109, -106, 32, 17, 41, -35, 101, 97, -9, 87, -1, -12, 5,
            45, -85, 80, 67, 84, 55, -14, -99, 50, -4, -94, -12, -7, -104, -27, -31, -122, -36, 23, 2, -14, -88, -60, -101, 75, 4, 85, -94, 74, 4, 99, -3, 42, 114, 11, 14, -31, -106, 46, -34, -14, 71, -127, 27, 54, -107, -59, 114, -42, -48, -56, 51,
            110, -74, -99, 56, -65, 55, -55, -85, 86, 22, -27, -59, -9, -105, -77, -62, 75, 53, 124, -13, 57, -58, 17, -128, -112, -108, 125, -121, -97, 87, -44, 88, -65, 88, -55, 76, -92, -70, -47, 58, 83, 106, -9, -39, 67, 80, -128, 8, 103, 125,
            108, 39, 75, 0, 105, -125 };

    private String username = "user";
    private String accessKey = "access";
    private String secretKey = "secret";
    private String realName = "real";
    private String emailAddress = "email";
    private String externalRefId = "external ref";

    private User user;
    private UserAccessKey userAccessKey;
    private Answer<Object> genericAnswerUpdateHandleResult;

    @Mock
    private PId userId;
    @Mock
    private PId accessKeyId;
    @Mock
    private PId piCertificateId;
    @Mock
    private PiCertificate piCertificate;
    @Mock
    private UserCertificateHelper userCertificateHelper;
    @Mock
    private SecurityGroupService securityGroupService;
    @Mock
    private UserServiceHelper userServiceHelper;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private BlockingDhtCache blockingDhtCache;
    @Mock
    private DeleteUserHelper deleteUserHelper;

    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private BlockingDhtReader blockingDhtReader;

    @InjectMocks
    private UserManagementService userManagementService = new UserManagementService();

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        user = new User(username, accessKey, secretKey);
        userAccessKey = new UserAccessKey(username, accessKey);

        when(piIdBuilder.getPId(User.getUrl(username))).thenReturn(userId);
        when(piIdBuilder.getPId(UserAccessKey.getUrl(accessKey))).thenReturn(accessKeyId);
        when(piIdBuilder.getPId(isA(UserAccessKey.class))).thenReturn(accessKeyId);
        when(piIdBuilder.getPId(isA(User.class))).thenReturn(userId);
        when(piIdBuilder.getPId(isA(PiCertificate.class))).thenReturn(piCertificateId);

        when(blockingDhtCache.get(userId)).thenReturn(user);
        when(blockingDhtCache.writeIfAbsent(eq(userId), isA(User.class))).thenReturn(true);

        when(userCertificateHelper.addCertificateToUser(eq(userId), eq(username), isA(byte[].class))).thenReturn(true);
        when(userServiceHelper.createPiCertificate()).thenReturn(piCertificate);
        when(dhtClientFactory.createBlockingReader()).thenReturn(blockingDhtReader);
        when(blockingDhtReader.get(userId)).thenReturn(user);
        doAnswer(new Answer<Object>() {

            @SuppressWarnings("rawtypes")
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver updateResolver = (UpdateResolver) invocation.getArguments()[1];
                updateResolver.update(user, null);
                return null;
            }
        }).when(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));
    }

    @Before
    public void setupGenericAnswer() {
        genericAnswerUpdateHandleResult = new Answer<Object>() {
            @SuppressWarnings("unchecked")
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation updater = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object entity = updater.update(null, null);
                assertNotNull(entity);
                updater.handleResult(entity);
                return null;
            }
        };
    }

    @Test
    public void shouldAddCertificateWithBaseEncodedString() throws Exception {
        String encodedCertificate = Base64.encodeBase64String(certAsByteArray);
        when(userServiceHelper.decodeBase64(encodedCertificate)).thenReturn(certAsByteArray);

        // act
        userManagementService.addCertificateToUser(user.getUsername(), encodedCertificate);

        // assert

        verify(securityGroupService).createSecurityGroup(user.getUsername(), "default", "default");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddUser() {
        // setup
        doAnswer(genericAnswerUpdateHandleResult).when(blockingDhtCache).update(eq(accessKeyId), isA(UpdateResolvingPiContinuation.class));

        // act
        userManagementService.addUser(user.getUsername(), user.getApiAccessKey(), user.getApiSecretKey());

        // assert
        verify(blockingDhtCache).writeIfAbsent(eq(accessKeyId), isA(UserAccessKey.class));
        verify(securityGroupService).createSecurityGroup(user.getUsername(), "default", "default");
    }

    @Test
    public void createUserShouldReturnCertificateZip() throws Exception {
        // setup
        byte[] bytes = new byte[] { 1, 2 };
        when(userServiceHelper.setUserCertAndKeysAndGetX509Zip(isA(User.class))).thenReturn(bytes);

        // act
        byte[] result = userManagementService.createUser(username, realName, emailAddress);

        // assert
        assertEquals(bytes, result);
    }

    @Test
    public void createUserShouldAddUserEntityIntoDht() throws Exception {
        // act
        userManagementService.createUser(username, realName, emailAddress);

        // assert
        verify(blockingDhtCache).writeIfAbsent(eq(userId), argThat(new ArgumentMatcher<User>() {

            @Override
            public boolean matches(Object argument) {
                User user = (User) argument;
                return username.equals(user.getUsername()) && realName.equals(user.getRealName()) && emailAddress.equals(user.getEmailAddress()) && user.isEnabled();
            }
        }));
    }

    @Test
    public void createUserShouldSetKeysAndCertsInUser() throws Exception {
        // act
        userManagementService.createUser(username, realName, emailAddress);

        // assert
        verify(userServiceHelper).setUserCertAndKeysAndGetX509Zip(argThat(new ArgumentMatcher<User>() {
            @Override
            public boolean matches(Object argument) {
                User user = (User) argument;
                return username.equals(user.getUsername()) && realName.equals(user.getRealName()) && emailAddress.equals(user.getEmailAddress()) && user.isEnabled();
            }
        }));
    }

    @Test(expected = JMException.class)
    public void createUserShouldThrowJMExceptionIfException() throws Exception {
        // setup
        doThrow(new RuntimeException()).when(userServiceHelper).setUserCertAndKeysAndGetX509Zip(isA(User.class));

        // act
        userManagementService.createUser(username, realName, emailAddress);
    }

    @Test
    public void createUserCertificateShouldReturnCertificateZip() throws Exception {
        // setup
        byte[] bytes = new byte[] { 1, 2 };
        when(userServiceHelper.setUserCertAndGetX509Zip(isA(User.class))).thenReturn(bytes);
        when(userCertificateHelper.updateUserCertificate(eq(userId), eq(user))).thenReturn(true);

        // act
        byte[] result = userManagementService.createUserCertificate(username);

        // assert
        assertEquals(bytes, result);
    }

    @Test(expected = JMException.class)
    public void createUserCertificateShouldThrowExceptionWhenUnableToUpdate() throws Exception {
        // setup
        byte[] bytes = new byte[] { 1, 2 };
        when(userServiceHelper.setUserCertAndGetX509Zip(isA(User.class))).thenReturn(bytes);
        when(userCertificateHelper.updateUserCertificate(eq(userId), eq(user))).thenReturn(false);

        // act
        userManagementService.createUserCertificate(username);
    }

    @Test(expected = JMException.class)
    public void createUserCertificateShouldThrowExceptionWhenCertificateCreationFails() throws Exception {
        // setup
        when(userServiceHelper.setUserCertAndGetX509Zip(isA(User.class))).thenThrow(new CertificateGenerationException("hello", null));

        // act
        userManagementService.createUserCertificate(username);
    }

    @Test(expected = JMException.class)
    public void createUserCertificateShouldThrowJMExceptionIfException() throws Exception {
        // setup
        doThrow(new RuntimeException()).when(userServiceHelper).setUserCertAndGetX509Zip(isA(User.class));

        // act
        userManagementService.createUserCertificate(username);
    }

    @Test
    public void createPiCertificateShouldAddCertificateInDht() throws Exception {
        // setup
        when(blockingDhtCache.writeIfAbsent(piCertificateId, piCertificate)).thenReturn(true);

        // act
        boolean result = userManagementService.createPiCertificate();

        // assert
        assertTrue(result);
        verify(blockingDhtCache).writeIfAbsent(eq(piCertificateId), isA(PiCertificate.class));
    }

    @Test
    public void createPiCertificateShouldReturnFalseIfAlreadyInDht() throws Exception {
        // setup
        when(blockingDhtCache.writeIfAbsent(piCertificateId, piCertificate)).thenReturn(false);

        // act
        boolean result = userManagementService.createPiCertificate();

        // assert
        assertFalse(result);
    }

    @Test(expected = JMException.class)
    public void createPiCertificateShouldThrowJMExceptionIfException() throws Exception {
        // setup
        doThrow(new RuntimeException()).when(userServiceHelper).createPiCertificate();

        // act
        userManagementService.createPiCertificate();
    }

    @Test
    public void addUserShouldGenerateNewApiAndSecretKeyIfNotGiven() {
        // setup
        user = new User();
        user.setUsername(username);

        // act
        userManagementService.addUser(user);

        // assert
        verify(userServiceHelper).generateAccessKey(username);
        verify(userServiceHelper).generateSecretKey(username);
    }

    @Test
    public void addUserShouldSetNewApiAndSecretKeyIfNotGiven() {
        // setup
        user = new User();
        user.setUsername(username);

        when(userServiceHelper.generateAccessKey(username)).thenReturn("abc");
        when(userServiceHelper.generateSecretKey(username)).thenReturn("xyz");

        // act
        userManagementService.addUser(user);

        // assert
        assertEquals("abc", user.getApiAccessKey());
        assertEquals("xyz", user.getApiSecretKey());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateUserShouldChangeTheUsersDetails() {
        // setup
        final String blah = "blah";
        final int maxInstances = 23;
        final int maxCores = 55;
        user.setRealName(blah);
        user.setEmailAddress(blah);
        user.setEnabled(true);
        user.setExternalRefId(blah);

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userManagementService.updateUser(username, realName, emailAddress, false, externalRefId, maxInstances, maxCores);

        // assert
        verify(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));
        assertEquals(realName, user.getRealName());
        assertEquals(emailAddress, user.getEmailAddress());
        assertEquals(false, user.isEnabled());
        assertEquals(externalRefId, user.getExternalRefId());
        assertEquals(maxInstances, user.getMaxInstances());
        assertEquals(maxCores, user.getMaxCores());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateUserShouldIgnoreNullValuesForRealNameAndEmailAddressAndEnabledFlag() {
        // setup
        final int maxInstances = 10;
        final int maxCores = 15;
        user.setRealName(realName);
        user.setEmailAddress(emailAddress);
        user.setEnabled(true);
        user.setExternalRefId(externalRefId);

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userManagementService.updateUser(username, null, null, null, null, maxInstances, maxCores);

        // assert
        verify(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));
        assertEquals(realName, user.getRealName());
        assertEquals(emailAddress, user.getEmailAddress());
        assertEquals(true, user.isEnabled());
        assertEquals(externalRefId, user.getExternalRefId());
        assertEquals(maxInstances, user.getMaxInstances());
        assertEquals(maxCores, user.getMaxCores());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateUserShouldIgnoreNegativeMaxCores() {
        // setup
        final String blah = "blah";
        final int maxInstances = 23;
        final int maxCores = 55;
        user.setRealName(blah);
        user.setEmailAddress(blah);
        user.setEnabled(true);
        user.setExternalRefId(blah);
        user.setMaxCores(maxCores);

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userManagementService.updateUser(username, realName, emailAddress, false, externalRefId, maxInstances, -1);

        // assert
        assertEquals(maxCores, user.getMaxCores());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateUserShouldSetNullMaxCoresToDefault() {
        // setup
        final String blah = "blah";
        final int maxInstances = 23;
        final int maxCores = 55;
        user.setRealName(blah);
        user.setEmailAddress(blah);
        user.setEnabled(true);
        user.setExternalRefId(blah);
        user.setMaxCores(maxCores);

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userManagementService.updateUser(username, realName, emailAddress, false, externalRefId, maxInstances, null);

        // assert
        assertEquals(8, user.getMaxCores());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateUserShouldIgnoreNegativeMaxInstances() {
        // setup
        final String blah = "blah";
        final int maxInstances = 23;
        final int maxCores = 55;
        user.setRealName(blah);
        user.setEmailAddress(blah);
        user.setEnabled(true);
        user.setExternalRefId(blah);
        user.setMaxInstances(maxInstances);

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userManagementService.updateUser(username, realName, emailAddress, false, externalRefId, -1, maxCores);

        // assert
        assertEquals(maxInstances, user.getMaxInstances());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateUserShouldSetNullMaxInstancesToDefault() {
        // setup
        final String blah = "blah";
        final int maxInstances = 23;
        final int maxCores = 55;
        user.setRealName(blah);
        user.setEmailAddress(blah);
        user.setEnabled(true);
        user.setExternalRefId(blah);
        user.setMaxCores(maxCores);
        user.setMaxInstances(maxInstances);

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userManagementService.updateUser(username, realName, emailAddress, false, externalRefId, null, maxCores);

        // assert
        assertEquals(5, user.getMaxInstances());
    }

    @Test(expected = CertificateGenerationException.class)
    public void updateUserCertificateShouldThrowWhenCatchingIOException() throws GeneralSecurityException, IOException {
        // setup
        when(userServiceHelper.setUserCertAndGetX509Zip(isA(User.class))).thenThrow(new IOException());

        // act
        userManagementService.updateUserCertificate(username);
    }

    @Test(expected = CertificateGenerationException.class)
    public void updateUserCertificateShouldThrowWhenCatchingGeneralSecurityException() throws GeneralSecurityException, IOException {
        // setup
        when(userServiceHelper.setUserCertAndGetX509Zip(isA(User.class))).thenThrow(new GeneralSecurityException());

        // act
        userManagementService.updateUserCertificate(username);
    }

    @Test
    public void testGetUser() {
        // act
        User result = userManagementService.getUser(username);

        // asssert
        assertEquals(username, result.getUsername());
    }

    @Test(expected = UserNotFoundException.class)
    public void testGetUserNotFound() {
        // setup
        when(blockingDhtCache.get(userId)).thenReturn(null);

        // act
        userManagementService.getUser(username);
    }

    @Test(expected = EntityMismatchException.class)
    public void testGetUserNotUserType() {
        // setup
        when(blockingDhtCache.get(userId)).thenReturn(new Snapshot());

        // act
        userManagementService.getUser(username);
    }

    @Test
    public void shouldGetUserByApiAccessKey() {
        // setup
        when(blockingDhtCache.get(accessKeyId)).thenReturn(userAccessKey);

        // act
        User user = userManagementService.getUserByApiAccessKey(accessKey);

        // assert
        assertEquals(username, user.getUsername());
        assertEquals(secretKey, user.getApiSecretKey());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deleteUserShouldSetTheUserToDeleted() {
        user.setDeleted(false);

        // setup
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userManagementService.deleteUser(username);

        // assert
        verify(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));
        assertTrue(user.isDeleted());
    }

    @Test
    public void deleteUserShouldCleanupResources() {
        // setup
        user.setDeleted(false);

        // act
        userManagementService.deleteUser(username);

        // assert
        verify(deleteUserHelper).cleanupUserResources(user);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSetEnabledFlagToFalse() {
        // setup
        user.setEnabled(true);
        // act
        userManagementService.setUserEnabledFlag(username, false);
        // assert
        verify(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));
        assertFalse(user.isEnabled());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSetEnabledFlagToTrue() {
        // setup
        user.setEnabled(false);
        // act
        userManagementService.setUserEnabledFlag(username, true);
        // assert
        verify(blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));
        assertTrue(user.isEnabled());

    }

}
