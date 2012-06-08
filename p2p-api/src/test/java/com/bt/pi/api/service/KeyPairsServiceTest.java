package com.bt.pi.api.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.util.SecurityUtils;

@RunWith(MockitoJUnitRunner.class)
public class KeyPairsServiceTest {
    private String keyAlgorithm = "RSA";
    private int keySize = 2048;

    private String ownerId = "owner";
    private String keyName = "test";
    private List<String> keyNames;

    private User user = new User();
    private KeyPair keyPair;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private UserService userService;

    @Mock
    private UserManagementService userManagementService;

    @InjectMocks
    private KeyPairsServiceImpl keyPairsService = new KeyPairsServiceImpl();

    @Before
    public void setupKeyPairsService() {
        keyPairsService.setKeyAlgorithm(keyAlgorithm);
        keyPairsService.setKeySize(keySize);
    }

    @Before
    public void setupDhtMocks() {
        when(userManagementService.getUser(ownerId)).thenReturn(user);
    }

    @Before
    public void setupSecurityUtils() throws Exception {
        keyPair = KeyPairGenerator.getInstance(keyAlgorithm).generateKeyPair();
        when(securityUtils.getNewKeyPair(keyAlgorithm, keySize)).thenReturn(keyPair);
        when(securityUtils.getPemBytes(keyPair.getPrivate())).thenReturn("private".getBytes());
    }

    @Test
    public void createKeysShouldInsertNewKeysIntoUserRecord() throws Exception {
        // setup

        // act
        keyPairsService.createKeyPair(ownerId, keyName);

        // assert
        verify(userService).addKeyPairToUser(eq(ownerId), isA(com.bt.pi.app.common.entities.KeyPair.class));
    }

    @Test
    public void createKeysReturnsNewKeyPair() throws Exception {
        // act
        com.bt.pi.app.common.entities.KeyPair result = keyPairsService.createKeyPair(ownerId, keyName);

        // assert
        assertThat(result.getKeyName(), equalTo(keyName));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createKeysThrowsExceptionIfNullKeyName() throws Exception {
        // act
        keyPairsService.createKeyPair(ownerId, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createKeysThrowsExceptionIfNullOwnerId() throws Exception {
        // act
        keyPairsService.createKeyPair(null, keyName);
    }

    @Test(expected = RuntimeException.class)
    public void createKeysThrowsRuntimeExceptionForGeneralSecurityException() throws Exception {
        // setup
        when(securityUtils.getNewKeyPair(keyAlgorithm, keySize)).thenThrow(new GeneralSecurityException());

        // act
        keyPairsService.createKeyPair(ownerId, keyName);
    }

    @Test(expected = RuntimeException.class)
    public void createKeysThrowsRuntimeExceptionForIOExceptionWhenGettingPemBytes() throws Exception {
        // setup
        when(securityUtils.getPemBytes(keyPair.getPrivate())).thenThrow(new IOException());

        // act
        keyPairsService.createKeyPair(ownerId, keyName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDescribeKeysThrowsExceptionIfNullOwnerId() {
        // act
        keyPairsService.describeKeyPairs(null, keyNames);
    }

    @Test
    public void testDescribeKeysReturnsAllKeyPairsIfKeyNamesIsNull() {
        // setup
        com.bt.pi.app.common.entities.KeyPair key1 = new com.bt.pi.app.common.entities.KeyPair("test1", "", "");
        com.bt.pi.app.common.entities.KeyPair key2 = new com.bt.pi.app.common.entities.KeyPair("test2", "", "");
        user.getKeyPairs().addAll(Arrays.asList(new com.bt.pi.app.common.entities.KeyPair[] { key1, key2 }));

        // act
        List<com.bt.pi.app.common.entities.KeyPair> result = keyPairsService.describeKeyPairs(ownerId, null);

        // assert
        assertThat(result.size(), equalTo(2));
        assertThat(result.contains(key1), is(true));
        assertThat(result.contains(key2), is(true));
    }

    @Test
    public void testDescribeKeysReturnsAllKeyPairsIfKeyNamesIsEmpty() {
        // setup
        com.bt.pi.app.common.entities.KeyPair key1 = new com.bt.pi.app.common.entities.KeyPair("test1", "", "");
        com.bt.pi.app.common.entities.KeyPair key2 = new com.bt.pi.app.common.entities.KeyPair("test2", "", "");
        user.getKeyPairs().addAll(Arrays.asList(new com.bt.pi.app.common.entities.KeyPair[] { key1, key2 }));

        // act
        List<com.bt.pi.app.common.entities.KeyPair> result = keyPairsService.describeKeyPairs(ownerId, new ArrayList<String>());

        // assert
        assertThat(result.size(), equalTo(2));
        assertThat(result.contains(key1), is(true));
        assertThat(result.contains(key2), is(true));
    }

    @Test
    public void testDescribeKeysReturnsOnlyKeyPairsSpecifiedInRequest() {
        // setup
        com.bt.pi.app.common.entities.KeyPair key1 = new com.bt.pi.app.common.entities.KeyPair("test1", "", "");
        com.bt.pi.app.common.entities.KeyPair key2 = new com.bt.pi.app.common.entities.KeyPair("test2", "", "");
        user.getKeyPairs().addAll(Arrays.asList(new com.bt.pi.app.common.entities.KeyPair[] { key1, key2 }));

        // act
        List<com.bt.pi.app.common.entities.KeyPair> result = keyPairsService.describeKeyPairs(ownerId, Arrays.asList(new String[] { key1.getKeyName() }));

        // assert
        assertThat(result.size(), equalTo(1));
        assertThat(result.contains(key1), is(true));
    }

    @Test
    public void testDescribeKeysReturnsOnlyKeyPairsSpecifiedInRequestAndWhichAreStored() {
        // setup
        com.bt.pi.app.common.entities.KeyPair key1 = new com.bt.pi.app.common.entities.KeyPair("test1", "", "");
        com.bt.pi.app.common.entities.KeyPair key2 = new com.bt.pi.app.common.entities.KeyPair("test2", "", "");
        user.getKeyPairs().addAll(Arrays.asList(new com.bt.pi.app.common.entities.KeyPair[] { key1, key2 }));

        // act
        List<com.bt.pi.app.common.entities.KeyPair> result = keyPairsService.describeKeyPairs(ownerId, Arrays.asList(new String[] { key1.getKeyName(), "spurious" }));

        // assert
        assertThat(result.size(), equalTo(1));
        assertThat(result.contains(key1), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteKeyThrowsExceptionIfOwnerIdIsNull() {
        // act
        keyPairsService.deleteKeyPair(null, keyName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteKeyThrowsExceptionIfKeyNameIsNull() {
        // act
        keyPairsService.deleteKeyPair(ownerId, null);
    }

    @Test
    public void testDeleteKeyDeletesKeyFromUserRecord() {
        // setup
        com.bt.pi.app.common.entities.KeyPair key = new com.bt.pi.app.common.entities.KeyPair(keyName, "", "");
        user.getKeyPairs().add(key);

        // act
        keyPairsService.deleteKeyPair(ownerId, keyName);

        // assert
        verify(userService).removeKeyPairFromUser(eq(ownerId), eq(new com.bt.pi.app.common.entities.KeyPair(keyName)));
    }
}
