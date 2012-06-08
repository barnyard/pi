package com.bt.pi.app.instancemanager.libvirt;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtConnectionTest {

    @Mock
    private Connect mockConnect;
    @Mock
    private Domain domain;
    private String domainName = "adomu";
    private String libvirtXml = "<xml></xml>";
    private String deviceXml = "<xml/>";

    class MyLibvirtConnection extends LibvirtConnection {
        @Override
        protected Connect newConnection(String connectionString) throws LibvirtException {
            return mockConnect;
        }
    }

    private MyLibvirtConnection libvirtConnection;

    @Before
    public void setUp() throws Exception {
        libvirtConnection = new MyLibvirtConnection();
        when(mockConnect.domainLookupByName(domainName)).thenReturn(domain);
    }

    @Test
    public void shouldListDomains() throws LibvirtException {
        // act
        libvirtConnection.listDomains();

        // assert
        verify(mockConnect).listDomains();
    }

    @Test
    public void shouldGetNodeInfo() throws LibvirtException {
        // act
        libvirtConnection.getNodeInfo();

        // assert
        verify(mockConnect).nodeInfo();
    }

    @Test
    public void shouldStartDomain() throws LibvirtException {
        // setup

        // act
        libvirtConnection.startDomain(libvirtXml);

        // assert
        verify(mockConnect).domainCreateLinux(libvirtXml, 0);
    }

    @Test
    public void shouldLookupDomainByName() throws LibvirtException {
        // setup

        // act
        libvirtConnection.domainLookupByName(domainName);

        // assert
        verify(mockConnect).domainLookupByName(domainName);
    }

    @Test
    public void shouldShutdownDomain() throws Exception {
        // setup

        // act
        libvirtConnection.shutdown(domain);

        // assert
        verify(domain).shutdown();
    }

    @Test
    public void shouldDestroyDomain() throws Exception {
        // setup

        // act
        libvirtConnection.destroy(domain);

        // assert
        verify(domain).destroy();
    }

    @Test
    public void shouldLookupDomainById() throws Exception {
        // setup
        int id = 1;

        // act
        libvirtConnection.domainLookupByID(id);

        // assert
        verify(mockConnect).domainLookupByID(id);
    }

    @Test
    public void shouldAttachDevice() throws Exception {
        // setup

        // act
        libvirtConnection.attachDevice(domainName, deviceXml);

        // assert
        verify(domain).attachDevice(deviceXml);
    }

    @Test
    public void shouldReturnDomXml() throws Exception {
        // setup
        when(domain.getXMLDesc(0)).thenReturn(libvirtXml);

        // act
        String result = libvirtConnection.getDomainXml(domainName);

        // assert
        assertEquals(libvirtXml, result);
    }

    @Test(expected = DomainNotFoundException.class)
    public void shouldThrowIfDomainNotFoundWhenAttachingDevice() throws Exception {
        // setup
        when(mockConnect.domainLookupByName(domainName)).thenReturn(null);

        // act
        libvirtConnection.attachDevice(domainName, deviceXml);
    }

    @Test
    public void shouldDetachDevice() throws Exception {
        // setup

        // act
        libvirtConnection.detachDevice(domainName, deviceXml);

        // assert
        verify(domain).detachDevice(deviceXml);
    }

    @Test(expected = DomainNotFoundException.class)
    public void shouldThrowIfDomainNotFoundWhenDetachingDevice() throws Exception {
        // setup
        when(mockConnect.domainLookupByName(domainName)).thenReturn(null);

        // act
        libvirtConnection.detachDevice(domainName, deviceXml);
    }

    @Test
    public void shouldPauseInstance() throws LibvirtException {
        // setup

        // act
        libvirtConnection.pauseInstance(domainName);

        // assert
        verify(domain).suspend();
    }

    @Test
    public void shouldUnPauseInstance() throws LibvirtException {
        // setup

        // act
        libvirtConnection.unPauseInstance(domainName);

        // assert
        verify(domain).resume();
    }

    int connectionCreationCount;

    @Test
    public void shouldBeAbleToDealWithSimultaneousCalls() throws Exception {
        // setup
        connectionCreationCount = 0;
        LibvirtConnection testLibvirtConnection = new LibvirtConnection() {
            @Override
            protected Connect newConnection(String connectionString) throws LibvirtException {
                connectionCreationCount++;
                return mock(Connect.class);
            }
        };

        Thread thread1 = new Thread(new LibvirtCaller(testLibvirtConnection));
        Thread thread2 = new Thread(new LibvirtCaller(testLibvirtConnection));

        // act
        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // assert
        assertEquals(1, connectionCreationCount);
    }

    class LibvirtCaller implements Runnable {
        private int runs = 100;
        private LibvirtConnection libvConnection;

        LibvirtCaller(LibvirtConnection connection) {
            libvConnection = connection;
        }

        @Override
        public void run() {
            while (runs > 0) {
                runs--;
                try {
                    libvConnection.listDomains();
                } catch (LibvirtException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
