package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyPairTest {
    @Test
    public void shouldConstructKeyPairDefault() {
        // setup
        KeyPair keyPair = new KeyPair();

        // act
        keyPair.setKeyName("name");
        keyPair.setKeyMaterial("material");
        keyPair.setKeyFingerprint("fingerprint");

        // assert
        assertEquals("name", keyPair.getKeyName());
        assertEquals("material", keyPair.getKeyMaterial());
        assertEquals("fingerprint", keyPair.getKeyFingerprint());
    }

    @Test
    public void shouldConstructKeyPairUsingFields() {
        // act
        KeyPair keyPair = new KeyPair("name", "fingerprint", "material");

        // assert
        assertEquals("name", keyPair.getKeyName());
        assertEquals("material", keyPair.getKeyMaterial());
        assertEquals("fingerprint", keyPair.getKeyFingerprint());
    }

    @Test
    public void shouldConstructKeyPairUsingKeyName() {
        // act
        KeyPair keyPair = new KeyPair("name");

        // assert
        assertEquals("name", keyPair.getKeyName());
        assertNull(keyPair.getKeyMaterial());
        assertNull(keyPair.getKeyFingerprint());
    }

    @Test
    public void testEqualsOnlyComparesOnName() {
        // setup
        KeyPair keyPair1 = new KeyPair("name", "fingerprint", "material");
        KeyPair keyPair2 = new KeyPair("name", "fingerprint2", "material2");

        // assert
        assertTrue(keyPair1.equals(keyPair2));
    }

    @Test
    public void testEqualsFailsIfNameDifferent() {
        // setup
        KeyPair keyPair1 = new KeyPair("name", "fingerprint", "material");
        KeyPair keyPair2 = new KeyPair("name2", "fingerprint", "material");

        // assert
        assertFalse(keyPair1.equals(keyPair2));
    }

    @Test
    public void shouldHaveSameHashCode() {
        // setup
        KeyPair keyPair1 = new KeyPair("name", "fingerprint", "material");
        KeyPair keyPair2 = new KeyPair("name", "fingerprint2", "material2");

        // assert
        assertEquals(keyPair1.hashCode(), keyPair2.hashCode());
    }

    @Test
    public void shouldHaveDifferentHashCodeIfNameIsDifferent() {
        // setup
        KeyPair keyPair1 = new KeyPair("name", "fingerprint", "material");
        KeyPair keyPair2 = new KeyPair("name2", "fingerprint", "material");

        // assert
        assertNotSame(keyPair1.hashCode(), keyPair2.hashCode());
    }
}
