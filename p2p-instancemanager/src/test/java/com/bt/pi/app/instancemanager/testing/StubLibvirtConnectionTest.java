package com.bt.pi.app.instancemanager.testing;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.libvirt.Domain;

public class StubLibvirtConnectionTest {
    private StubLibvirtConnection stubLibvirtConnection = new StubLibvirtConnection();

    @Test
    public void testAssertLibvirtCommand() throws Exception {
        // setup
        stubLibvirtConnection.destroy(mock(Domain.class));

        // act
        boolean result = stubLibvirtConnection.assertLibvirtCommand("destroy");

        // assert
        assertTrue(result);
    }

    @Test
    public void testWaitForLibvirtCommand() throws Exception {
        // setup
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(900);
                    stubLibvirtConnection.destroy(mock(Domain.class));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(r).start();

        // act
        boolean result = stubLibvirtConnection.waitForLibvirtCommand("destroy", 20);

        // assert
        assertTrue(result);
    }
}
