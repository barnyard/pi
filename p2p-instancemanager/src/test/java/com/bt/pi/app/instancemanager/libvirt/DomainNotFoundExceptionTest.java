package com.bt.pi.app.instancemanager.libvirt;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class DomainNotFoundExceptionTest {

    @Test
    public void testThatEmmaIsNotASlapper() {
        assertNotNull(new DomainNotFoundException());
    }
}
