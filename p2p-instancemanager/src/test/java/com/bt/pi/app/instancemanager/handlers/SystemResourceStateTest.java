package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import net.sf.ehcache.Cache;

import org.junit.Before;
import org.junit.Test;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.NodeInfo;
import org.springframework.cache.ehcache.EhCacheFactoryBean;

import com.bt.pi.app.common.conf.XenConfigurationParser;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManager;

public class SystemResourceStateTest {
    private SystemResourceState systemResourceState;
    private LibvirtManager libvirtManager;
    private XenConfigurationParser xenConfigurationParser;
    private File mockFile;
    private Domain mockDomain;
    private NodeInfo mockNodeInfo;
    private Cache cache;
    private final static String instanceId = "INSTANCE_ID";
    private int cacheTTLSeconds = 2;
    private DomainInfo domainInfo1;
    private DomainInfo domainInfo2;

    @Before
    public void before() throws Exception {
        setUp(false);
    }

    public void setUp(boolean isMockFile) throws Exception {
        if (isMockFile) {
            mockFile = mock(File.class);
            when(mockFile.exists()).thenReturn(true);
            when(mockFile.getFreeSpace()).thenReturn(1024 * 1024 * 1024L);
            systemResourceState = new SystemResourceState() {
                @Override
                protected File getFile(String instancePath) {
                    return mockFile;
                }
            };
        } else
            systemResourceState = new SystemResourceState();

        libvirtManager = mock(LibvirtManager.class);
        systemResourceState.setLibvirtManager(libvirtManager);

        xenConfigurationParser = mock(XenConfigurationParser.class);
        systemResourceState.setXenConfigurationParser(xenConfigurationParser);

        mockNodeInfo = new NodeInfo();
        mockNodeInfo.cpus = 10;
        mockNodeInfo.memory = 2048 * 1024;

        domainInfo1 = new DomainInfo();
        domainInfo1.nrVirtCpu = 4;

        domainInfo2 = new DomainInfo();
        domainInfo2.nrVirtCpu = 2;

        mockDomain = mock(Domain.class);
        when(mockDomain.getInfo()).thenReturn(domainInfo1).thenReturn(domainInfo2);
        when(mockDomain.getMaxMemory()).thenReturn(768 * 1024L).thenReturn(256 * 1024L);

        setupLibvirtManagerExpectations(Arrays.asList(new Domain[] { mockDomain, mockDomain }));

        EhCacheFactoryBean ehCacheFactoryBean = new EhCacheFactoryBean();
        ehCacheFactoryBean.setCacheName("unittest");
        ehCacheFactoryBean.setTimeToIdle(cacheTTLSeconds);
        ehCacheFactoryBean.setTimeToLive(cacheTTLSeconds);
        ehCacheFactoryBean.afterPropertiesSet();
        this.cache = (Cache) ehCacheFactoryBean.getObject();

        systemResourceState.setCache(cache);
    }

    @Test
    public void shouldCalculateFreeCoresAvailable() throws Exception {
        // act
        int result = systemResourceState.getFreeCores();

        // assert
        assertEquals(4, result);
    }

    @Test
    public void shouldNotReturnNegativeFreeCoresAvailable() throws Exception {
        // setup
        mockNodeInfo.cpus = 4;

        // act
        int result = systemResourceState.getFreeCores();

        // assert
        assertEquals(0, result);
    }

    private void setupLibvirtManagerExpectations(Collection<Domain> domains) {
        when(libvirtManager.getNodeInfo()).thenReturn(mockNodeInfo);
        when(libvirtManager.getAllInstances()).thenReturn(domains);
    }

    @Test
    public void shouldCalculateFreeMemoryAvailable() throws Exception {
        // setup
        // this domain should be skipped.
        Domain mockDomain0 = mock(Domain.class);
        when(mockDomain0.getMaxMemory()).thenReturn(999999999999L * 1024);
        when(mockDomain0.getName()).thenReturn("Domain-0");

        when(xenConfigurationParser.getLongValue("dom0-min-mem")).thenReturn(768L);

        setupLibvirtManagerExpectations(Arrays.asList(new Domain[] { mockDomain0, mockDomain, mockDomain }));

        // act
        long result = systemResourceState.getFreeMemoryInMB();

        // assert
        assertEquals(192, result);
    }

    @Test
    public void shouldNotReturnNegativeFreeMemoryAvailable() throws Exception {
        // setup
        when(xenConfigurationParser.getLongValue("dom0-min-mem")).thenReturn(1088L);

        // act
        long result = systemResourceState.getFreeMemoryInMB();

        // assert
        assertEquals(0, result);
    }

    @Test
    public void shouldUseMaxMemoryValueFromConfigurationIfItIsLessThanRealMaxMemory() throws Exception {
        // setup
        mockNodeInfo.memory = 3072 * 1024;
        when(xenConfigurationParser.getLongValue("dom0-min-mem")).thenReturn(768L);
        when(mockDomain.getMaxMemory()).thenReturn(1024L * 1024);
        systemResourceState.setConfigReservedMem(192L);

        setupLibvirtManagerExpectations(Arrays.asList(mockDomain));

        // act
        long result = systemResourceState.getFreeMemoryInMB();

        // assert
        assertEquals(1024, result);
    }

    @Test
    public void shouldUseMaxCoresValueFromConfigurationIfItIsPresent() throws Exception {
        // setup
        systemResourceState.setMaxCores(20);

        // act
        int result = systemResourceState.getFreeCores();

        // assert
        assertEquals(14, result);
    }

    @Test
    public void shouldMakeDirectoryIfInstancePathDoesNotExist() throws Exception {
        // setup
        setUp(true);
        when(mockFile.exists()).thenReturn(false);
        when(mockFile.mkdirs()).thenReturn(true);
        String instanceDirectory = "/path/to/instances";

        // act
        systemResourceState.setInstancesDirectory(instanceDirectory);

        // assert
        verify(mockFile).mkdirs();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfMakeDirectoryFailsOnInstancePathDoesNotExist() throws Exception {
        // setup
        setUp(true);
        when(mockFile.exists()).thenReturn(false);
        when(mockFile.mkdirs()).thenReturn(false);
        String instanceDirectory = "/path/to/instances";

        // act
        systemResourceState.setInstancesDirectory(instanceDirectory);

        // assert
        verify(mockFile).mkdirs();
    }

    @Test
    public void shouldCalculateFreeDiskAvailable() throws Exception {
        // setup
        setUp(true);
        systemResourceState.setInstancesDirectory("/path/to/instances");

        // act
        long result = systemResourceState.getFreeDiskInMB();

        // assert
        assertEquals(1024, result);
    }

    @Test
    public void shouldNotReturnNegativeFreeDiskAvailable() throws Exception {
        // setup
        setUp(true);
        systemResourceState.setInstancesDirectory("/path/to/instances");
        systemResourceState.reserveResources(instanceId, new InstanceTypeConfiguration("test", 1, 2, 2));

        // act
        long result = systemResourceState.getFreeDiskInMB();

        // assert
        assertEquals(0, result);
    }

    @Test
    public void shouldSubtractResourcesForNewInstance() throws Exception {
        // setup
        setUp(true);
        systemResourceState.setInstancesDirectory("/path/to/instances");

        // act
        systemResourceState.reserveResources(instanceId, new InstanceTypeConfiguration("test", 1, 2, 3));

        // assert
        assertEquals(3, systemResourceState.getFreeCores());
        assertEquals(958, systemResourceState.getFreeMemoryInMB());
        assertEquals(0, systemResourceState.getFreeDiskInMB());
    }

    @Test
    public void shouldReleaseResourcesForTerminatedOrNonProvisionedInstance() throws Exception {
        // setup
        setUp(true);
        systemResourceState.setInstancesDirectory("/path/to/instances");
        systemResourceState.reserveResources(instanceId, new InstanceTypeConfiguration("test", 1, 2, 3));

        // act
        systemResourceState.unreserveResources(instanceId);

        // assert
        assertEquals(4, systemResourceState.getFreeCores());
        assertEquals(960, systemResourceState.getFreeMemoryInMB());
        assertEquals(1024, systemResourceState.getFreeDiskInMB());
    }

    @Test
    public void reservationsShouldExpireIfNotReleased() throws Exception {
        // setup
        when(mockDomain.getInfo()).thenReturn(domainInfo1).thenReturn(domainInfo2).thenReturn(domainInfo1).thenReturn(domainInfo2);

        // act
        systemResourceState.reserveResources(instanceId, new InstanceTypeConfiguration("test", 1, 2, 3));
        assertEquals(3, systemResourceState.getFreeCores());
        Thread.sleep(cacheTTLSeconds * 2 * 1000);

        // assert
        assertEquals(4, systemResourceState.getFreeCores());
    }
}
