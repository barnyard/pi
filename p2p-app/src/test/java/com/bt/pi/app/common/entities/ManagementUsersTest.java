/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.parser.KoalaJsonParser;

public class ManagementUsersTest {
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";
    private ManagementUsers users;
    private ManagementUser user;

    @Before
    public void doBefore() {
        users = new ManagementUsers();
        user = new ManagementUser();

        user.setPassword(PASSWORD);
        user.setUsername(USERNAME);
        user.getRoles().add(ManagementRoles.ROLE_PROVISIONING);
        user.getRoles().add(ManagementRoles.ROLE_OPS);

        users.getUserMap().put(USERNAME, user);
    }

    @Test
    public void getTypeShouldReturnTheClassName() {
        assertEquals("ManagementUsers", users.getType());
    }

    @Test
    public void getUrlShouldReturnTheUrl() {
        assertEquals("management:users", users.getUrl());
    }

    @Test
    public void getUserMapShouldReturnTheUsers() {
        assertNotNull(users.getUserMap());
    }

    @Test
    public void equalsShouldBeTrueWhenSameObject() {
        assertTrue(users.equals(users));
    }

    @Test
    public void equalsShouldBeTrueWhenObjectsContainSameObjects() {
        // setup
        ManagementUsers users2 = new ManagementUsers();
        users2.getUserMap().put(USERNAME, user);

        // act and assert
        assertTrue(users.equals(users2));
    }

    @Test
    public void equalsShouldBeFalseWhenNull() {
        assertFalse(users.equals(null));
    }

    @Test
    public void equalsShouldBeFalseWhenDifferentClass() {
        assertFalse(users.equals("a different class, like a string"));
    }

    @Test
    public void equalsShouldBeFalseWhenDifferent() {
        // setup
        ManagementUsers users2 = new ManagementUsers();
        users2.getUserMap().put("different", user);

        // act and assert
        assertFalse(users.equals(users2));
    }

    @Test
    public void itShouldBeSerialisable() {
        KoalaJsonParser parser = new KoalaJsonParser();
        String json = parser.getJson(users);
        ManagementUsers result = (ManagementUsers) parser.getObject(json, ManagementUsers.class);

        assertEquals(users, result);
    }

}
