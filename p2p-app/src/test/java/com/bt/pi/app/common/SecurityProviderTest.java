package com.bt.pi.app.common;

import static org.junit.Assert.assertNotNull;

import java.security.Security;

import org.junit.Test;

public class SecurityProviderTest {
    @Test
    public void shouldAddProvider() throws Exception {
        // setup
        SecurityProvider securityProvider = new SecurityProvider();

        // act
        securityProvider.addProvider();

        // assert
        assertNotNull(Security.getProvider("BC"));
    }
}
