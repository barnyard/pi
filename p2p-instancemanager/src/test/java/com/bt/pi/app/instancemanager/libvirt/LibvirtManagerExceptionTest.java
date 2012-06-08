package com.bt.pi.app.instancemanager.libvirt;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.bt.pi.app.instancemanager.libvirt.LibvirtManagerException;

public class LibvirtManagerExceptionTest {

    LibvirtManagerException libvirtManagerException;

    @Test
    public void shouldTestLibvirtManagerExceptionString() {
        // setup
        libvirtManagerException = new LibvirtManagerException("Unable to connect");

        // assert
        assertNotNull(libvirtManagerException);
    }

    @Test
    public void shouldTestLibvirtManagerExceptionWithExceptionAndString() {
        // setup
        libvirtManagerException = new LibvirtManagerException("Unable to connect", new Exception());

        // assert
        assertNotNull(libvirtManagerException);
    }
}
