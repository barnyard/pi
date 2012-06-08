package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.UserAccessKey;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {
    private static final String ACCESS_KEY = "accesskey";
    private static final String SECRET_KEY = "secretkey";
    private String username = "piUser";
    private String instanceId = "i-ABCD1234";
    @Mock
    private PId userId;
    @Mock
    private PId accessKeyId;
    private User user;
    @Mock
    private PiIdBuilder piIdBuilder;
    @InjectMocks
    private UserService userService = new UserService();
    @Mock
    private BlockingDhtCache userBlockingCache;
    private String imageId = "img-123";
    private String groupName = "default";
    @Mock
    private BlockingDhtCache instanceTypeCache;

    private String snapshotId = "snap-12345678";
    @Mock
    private PId instanceTypesId;
    private InstanceTypes instanceTypes;

    @Before
    public void before() throws Exception {
        user = new User(username, ACCESS_KEY, SECRET_KEY);

        when(piIdBuilder.getPId(User.getUrl(username))).thenReturn(userId);
        when(piIdBuilder.getPId(UserAccessKey.getUrl(ACCESS_KEY))).thenReturn(accessKeyId);
        when(piIdBuilder.getPId(isA(UserAccessKey.class))).thenReturn(accessKeyId);
        when(piIdBuilder.getPId(isA(User.class))).thenReturn(userId);

        when(userBlockingCache.get(userId)).thenReturn(user);
        when(userBlockingCache.writeIfAbsent(eq(userId), isA(User.class))).thenReturn(true);

        setupInstanceTypes();
    }

    private void setupInstanceTypes() {
        instanceTypes = new InstanceTypes();

        when(piIdBuilder.getPId(InstanceTypes.URL_STRING)).thenReturn(instanceTypesId);

        when(instanceTypeCache.get(instanceTypesId)).thenReturn(instanceTypes);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddInstanceToUser() {
        // setup
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userService.addInstanceToUser(username, instanceId);

        // assert
        verify(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        assertEquals(1, user.getInstanceIds().length);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddInstancesToUser() {
        // setup
        Collection<String> instanceIds = new HashSet<String>();
        instanceIds.add("i-123");
        instanceIds.add("i-456");
        instanceIds.add("i-789");

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        String instanceType = "biggest";

        // act
        userService.addInstancesToUser(username, instanceIds, instanceType);

        // assert
        verify(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        assertEquals(3, user.getInstanceIds().length);
        assertEquals(3, user.getInstanceTypes().length);
        assertEquals(instanceType, user.getInstanceTypes()[0]);
        assertEquals(instanceType, user.getInstanceTypes()[1]);
        assertEquals(instanceType, user.getInstanceTypes()[2]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddVolumeToUser() {
        // setup
        userService.setMaxVolumesPerUser(10);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        String volumeId = "vol-123";

        // act
        userService.addVolumeToUser(username, volumeId);

        // assert
        verify(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        assertEquals(1, user.getVolumeIds().size());
        assertTrue(user.getVolumeIds().contains(volumeId));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testAddVolumeToUserWhenUserIsOverLimit() {
        // setup
        userService.setMaxVolumesPerUser(-1);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        String volumeId = "vol-123";

        // act
        userService.addVolumeToUser(username, volumeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddImageToUser() {
        // setup
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        userService.setMaxPrivateImagesPerUser(1);

        // act
        userService.addImageToUser(username, imageId);

        // assert
        verify(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        assertEquals(1, user.getImageIds().size());
        assertTrue(user.getImageIds().contains(imageId));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testAddImageToUserWhenUserIsOverLimit() {
        // setup
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        userService.setMaxPrivateImagesPerUser(0);

        // act
        userService.addImageToUser(username, imageId);

        // assert
        verify(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        assertEquals(1, user.getImageIds().size());
        assertTrue(user.getImageIds().contains(imageId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddSecurityGroupToUser() {
        // setup
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userService.addSecurityGroupToUser(username, groupName);

        // assert
        verify(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        assertEquals(1, user.getSecurityGroupIds().size());
        assertTrue(user.getSecurityGroupIds().contains(groupName));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRemoveInstanceFromUser() {
        // setup
        user.addInstance(instanceId);
        user.addInstance("i-345");
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }

        }).when(userBlockingCache).update(isA(PId.class), isA(UpdateResolver.class));

        // act
        userService.removeInstanceFromUser(username, instanceId);

        // assert
        assertEquals(1, user.getInstanceIds().length);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRemoveImageFromUser() {
        // setup
        user.getImageIds().add(imageId);
        user.getImageIds().add("img-345");
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }

        }).when(userBlockingCache).update(isA(PId.class), isA(UpdateResolver.class));

        // act
        userService.removeImageFromUser(username, imageId);

        // assert
        assertEquals(1, user.getImageIds().size());
        assertTrue(user.getImageIds().contains("img-345"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRemoveSecurityGroupFromUser() {
        // setup
        user.getSecurityGroupIds().add(groupName);
        user.getSecurityGroupIds().add("fred");
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }

        }).when(userBlockingCache).update(isA(PId.class), isA(UpdateResolver.class));

        // act
        userService.removeSecurityGroupFromUser(username, groupName);

        // assert
        assertEquals(1, user.getSecurityGroupIds().size());
        assertTrue(user.getSecurityGroupIds().contains("fred"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddKeyPairToUser() {
        // setup
        KeyPair keyPair = mock(KeyPair.class);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        this.userService.addKeyPairToUser(username, keyPair);

        // assert
        assertTrue(user.getKeyPairs().contains(keyPair));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testAddKeyPairToUserAlreadyExists() {
        // setup
        KeyPair keyPair = mock(KeyPair.class);
        String keyName = "newKey";
        when(keyPair.getKeyName()).thenReturn(keyName);
        user.getKeyPairs().add(keyPair);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        try {
            this.userService.addKeyPairToUser(username, keyPair);
        } catch (IllegalStateException e) {
            // assert
            assertEquals(String.format("Key pair %s already exists for user %s", keyName, username), e.getMessage());
            assertTrue(user.getKeyPairs().contains(keyPair));
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveKeyPairFromUser() {
        // setup
        KeyPair keyPair = mock(KeyPair.class);
        String keyName = "newKey";
        when(keyPair.getKeyName()).thenReturn(keyName);
        user.getKeyPairs().add(keyPair);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        this.userService.removeKeyPairFromUser(username, keyPair);

        // assert
        assertEquals(0, user.getKeyPairs().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveKeyPairFromUserNotFound() {
        // setup
        KeyPair keyPair = mock(KeyPair.class);
        String keyName = "newKey";
        when(keyPair.getKeyName()).thenReturn(keyName);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                Object result = resolver.update(user, user);
                assertNull(result);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        this.userService.removeKeyPairFromUser(username, keyPair);

        // assert
        verify(userBlockingCache).update(isA(PId.class), isA(UpdateResolver.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddSnapshotToUser() {
        // setup
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userService.addSnapshotToUser(username, snapshotId);

        // assert
        verify(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        assertEquals(1, user.getSnapshotIds().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveSnapshotFromUserr() {
        // setup
        user.getSnapshotIds().add(snapshotId);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, null);
                return null;
            }
        }).when(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));

        // act
        userService.removeSnapshotFromUser(username, snapshotId);

        // assert
        verify(userBlockingCache).update(eq(userId), isA(UpdateResolver.class));
        assertEquals(0, user.getSnapshotIds().size());
    }

    @Test
    public void shouldReturnMaxCores() {
        // setup

        // act
        int result = userService.getMaxCores(username);

        // assert
        assertEquals(user.getMaxCores(), result);
    }

    @Test
    public void shouldReturnCurrentCores() {
        // setup
        String instanceType1 = "x.large";
        InstanceTypeConfiguration instanceTypeAConfiguration1 = new InstanceTypeConfiguration(instanceType1, 3, 1, 1);
        instanceTypes.addInstanceType(instanceTypeAConfiguration1);
        String instanceType2 = "m.medium";
        InstanceTypeConfiguration instanceTypeAConfiguration2 = new InstanceTypeConfiguration(instanceType2, 2, 1, 1);
        instanceTypes.addInstanceType(instanceTypeAConfiguration2);
        user.addInstance("1111", instanceType1);
        user.addInstance("2222", instanceType2);

        // act
        int result = userService.getCurrentCores(username);

        // assert
        assertEquals(5, result);
    }

    @Test
    public void shouldReturnCurrentCoresCountingOneForAnyUnknown() {
        // setup
        String instanceType1 = "x.large";
        InstanceTypeConfiguration instanceTypeAConfiguration1 = new InstanceTypeConfiguration(instanceType1, 8, 1, 1);
        instanceTypes.addInstanceType(instanceTypeAConfiguration1);
        String instanceType2 = "m.medium";
        InstanceTypeConfiguration instanceTypeAConfiguration2 = new InstanceTypeConfiguration(instanceType2, 7, 1, 1);
        instanceTypes.addInstanceType(instanceTypeAConfiguration2);
        user.addInstance("1111", instanceType1);
        user.addInstance("2222", instanceType2);
        user.addInstance("3333");
        user.addInstance("3334", "bogus");

        // act
        int result = userService.getCurrentCores(username);

        // assert
        assertEquals(17, result);
    }
}