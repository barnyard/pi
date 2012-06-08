package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.scope.NodeScope;

public class UserAccessKeyTest {

    private static final String ACCESS_KEY = "accesskey";
    private static final String USERNAME = "username";

    @Test
    public void constructUserAccessKey() {
        // setup
        UserAccessKey userAccessKey = new UserAccessKey();

        // act
        userAccessKey.setAccessKey(ACCESS_KEY);
        userAccessKey.setUsername(USERNAME);

        // assert
        assertEquals(USERNAME, userAccessKey.getUsername());
        assertEquals(ACCESS_KEY, userAccessKey.getAccessKey());
        assertEquals("uak:accesskey", userAccessKey.getUrl());
        assertEquals("uak", userAccessKey.getUriScheme());
    }

    @Test
    public void shouldBeBackupable() {
        // assert
        assertNotNull(UserAccessKey.class.getAnnotation(Backupable.class));
    }

    @Test
    public void shouldHaveGlobalEntityScope() {
        // assert
        assertNotNull(UserAccessKey.class.getAnnotation(EntityScope.class));
        EntityScope scope = UserAccessKey.class.getAnnotation(EntityScope.class);
        assertEquals(NodeScope.GLOBAL, scope.scope());
    }
}
