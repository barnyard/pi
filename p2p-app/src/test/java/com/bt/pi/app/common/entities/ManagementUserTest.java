/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ManagementUserTest {
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";
    private ManagementUser user;

    @Before
    public void doBefore() {
        user = new ManagementUser();
    }

    @Test
    public void usernameMustBeSettableAndGettable() {
        user.setUsername(USERNAME);

        assertEquals(USERNAME, user.getUsername());
    }

    @Test
    public void rolesMustBeGettable() {
        assertNotNull(user.getRoles());
    }

    @Test
    public void passwordShouldBeSetableAndGettable() {
        user.setPassword(PASSWORD);

        assertEquals(PASSWORD, user.getPassword());
    }

    @Test
    public void equalsShouldBeTrueWhenSameObject() {
        assertTrue(user.equals(user));
    }

    @Test
    public void equalsShouldBeTrueWhenObjectsContainSameObjects() {
        // setup
        ManagementUser user2 = new ManagementUser();

        // act and assert
        assertTrue(user.equals(user2));
    }

    @Test
    public void equalsShouldBeFalseWhenNull() {
        assertFalse(user.equals(null));
    }

    @Test
    public void equalsShouldBeFalseWhenDifferentClass() {
        assertFalse(user.equals("a different class, like a string"));
    }

    @Test
    public void equalsShouldBeFalseWhenDifferent() {
        // setup
        ManagementUser user2 = new ManagementUser();
        user2.setUsername("different");

        // act and assert
        assertFalse(user.equals(user2));
    }

}
