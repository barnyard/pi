/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.handlers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.conf.XenConfigurationParser;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManager;
import com.bt.pi.core.conf.Property;

@Component
public class SystemResourceState {
    private static final String UNABLE_TO_GET_NODE_INFORMATION_FOR_INSTANCE_IN_XEN_WITH_ID_S = "Unable to get node information for instance in Xen with id: %s";
    private static final int SIXTY_FOUR = 64;
    private static final long ONE_THOUSAND_TWENTY_FOUR = 1024;
    private static final String UNCHECKED = "unchecked";
    private static final Log LOG = LogFactory.getLog(SystemResourceState.class);

    private XenConfigurationParser xenConfigurationParser;
    private LibvirtManager libvirtManager;
    private File instancesDirectory;
    private int configMaxCores;
    private String xenConfigPath;
    private long configReservedMem;
    private Cache cache;

    private final class ReservedResources {
        private int reservedCores;
        private long reservedDiskInMB;
        private long reservedMemoryInMB;

        private ReservedResources(int aReservedCores, long aReservedDiskInMB, long aReservedMemoryInMB) {
            this.reservedCores = aReservedCores;
            this.reservedDiskInMB = aReservedDiskInMB;
            this.reservedMemoryInMB = aReservedMemoryInMB;
        }
    }

    public SystemResourceState() {
        xenConfigurationParser = null;
        libvirtManager = null;
        instancesDirectory = null;
        cache = null;
    }

    @Resource(name = "systemResourceStateCache")
    public void setCache(Cache aCache) {
        this.cache = aCache;
    }

    @Resource
    public void setLibvirtManager(LibvirtManager aLibvirtManager) {
        libvirtManager = aLibvirtManager;
    }

    @Resource
    public void setXenConfigurationParser(XenConfigurationParser aXenConfigurationParser) {
        xenConfigurationParser = aXenConfigurationParser;
    }

    @Property(key = "instances.directory", defaultValue = "/state/partition1/pi/instances")
    public void setInstancesDirectory(String anInstancesDirectory) {
        instancesDirectory = getFile(anInstancesDirectory);
        if (!instancesDirectory.exists()) {
            if (!instancesDirectory.mkdirs())
                throw new IllegalArgumentException(String.format("Instance directory %s does not exist, unable to create either", anInstancesDirectory));
        }
    }

    // TODO: Ignore dom0 and sys- domains while calculating
    public synchronized int getFreeCores() {
        int freeCores = calculateFreeCores();
        return Math.max(0, freeCores - getTotalReservedCores());
    }

    @SuppressWarnings(UNCHECKED)
    private int getTotalReservedCores() {
        int result = 0;
        List<String> keysWithExpiryCheck = cache.getKeysWithExpiryCheck();
        for (String instanceId : keysWithExpiryCheck) {
            ReservedResources reservedResources = (ReservedResources) cache.get(instanceId).getObjectValue();
            result += reservedResources.reservedCores;
        }
        return result;
    }

    public long getFreeDiskInMB() {
        return Math.max(0, calculateDisk() - getTotalReservedDiskInMB());
    }

    @SuppressWarnings(UNCHECKED)
    private long getTotalReservedDiskInMB() {
        long result = 0;
        List<String> keysWithExpiryCheck = cache.getKeysWithExpiryCheck();
        for (String instanceId : keysWithExpiryCheck) {
            ReservedResources reservedResources = (ReservedResources) cache.get(instanceId).getObjectValue();
            result += reservedResources.reservedDiskInMB;
        }
        return result;
    }

    public long getFreeMemoryInMB() {
        try {
            return Math.max(0, calculateFreeMemory() - getTotalReservedMemoryInMB());
        } catch (IOException e) {
            LOG.error("Error reading xen configuration file", e);
            return 0;
        }
    }

    @SuppressWarnings(UNCHECKED)
    private long getTotalReservedMemoryInMB() {
        long result = 0;
        List<String> keysWithExpiryCheck = cache.getKeysWithExpiryCheck();
        for (String instanceId : keysWithExpiryCheck) {
            ReservedResources reservedResources = (ReservedResources) cache.get(instanceId).getObjectValue();
            result += reservedResources.reservedMemoryInMB;
        }
        return result;
    }

    @Property(key = "max.cores")
    public void setMaxCores(int value) {
        this.configMaxCores = value;
    }

    private int calculateFreeCores() {
        LOG.debug(String.format("Max cores in config file = %d", configMaxCores));
        return getFreeCores(getUsedCores());
    }

    private int getFreeCores(int usedCpus) {
        int freeCpus = 0;
        if (configMaxCores > 0) {
            freeCpus = configMaxCores - usedCpus;
            LOG.debug(String.format("Using ConfigMaxCores - FreeCpus: %d", freeCpus));
        } else {
            NodeInfo nodeInfo = libvirtManager.getNodeInfo();
            freeCpus = nodeInfo.cpus - usedCpus;
            LOG.debug("Not using ConfigMaxCores - FreeCpus:" + freeCpus);
        }

        LOG.debug(String.format("Free cores available in Xen: %d", freeCpus));
        return freeCpus;
    }

    private int getUsedCores() {
        int usedCpus = 0;
        Collection<Domain> allRunInstances = libvirtManager.getAllInstances();
        for (Domain domain : allRunInstances) {
            try {
                int nrVirtCpu = domain.getInfo().nrVirtCpu;
                LOG.debug(String.format("%d CPUs used by Domain %s", nrVirtCpu, domain.getName()));
                usedCpus += nrVirtCpu;
            } catch (LibvirtException e) {
                LOG.warn(String.format(UNABLE_TO_GET_NODE_INFORMATION_FOR_INSTANCE_IN_XEN_WITH_ID_S, domain), e);
            }
        }
        return usedCpus;
    }

    @Property(key = "xen.config.path")
    public void setXenConfigPath(String value) {
        this.xenConfigPath = value;
    }

    @Property(key = "xen.reserved.mem.mb")
    public void setConfigReservedMem(long value) {
        this.configReservedMem = value;
    }

    private long calculateFreeMemory() throws IOException {
        NodeInfo nodeInfo = libvirtManager.getNodeInfo();
        long nodeMemoryInMB = nodeInfo.memory / ONE_THOUSAND_TWENTY_FOUR;

        xenConfigurationParser.init(this.xenConfigPath);
        long xenMinMemoryInMB = xenConfigurationParser.getLongValue("dom0-min-mem");

        LOG.debug(String.format("Available Node memory (from libvirt): %dMB, Memory reserved for dom0 in xen config: %dMB, Memory reserved for Xen in p2p-app-config: %dMB", nodeMemoryInMB, configReservedMem, xenMinMemoryInMB));

        long usedMemory = 0;
        Collection<Domain> allRunInstances = libvirtManager.getAllInstances();
        for (Domain domain : allRunInstances) {
            try {
                if (StringUtils.isBlank(domain.getName()) || !domain.getName().toLowerCase(Locale.UK).contains("domain"))
                    usedMemory += domain.getMaxMemory() / ONE_THOUSAND_TWENTY_FOUR;
            } catch (LibvirtException e) {
                LOG.warn(String.format(UNABLE_TO_GET_NODE_INFORMATION_FOR_INSTANCE_IN_XEN_WITH_ID_S, domain), e);
            }
        }

        // deduct 64M as an extra buffer for xen
        long freeMemoryInMB = nodeMemoryInMB - usedMemory - xenMinMemoryInMB - configReservedMem - SIXTY_FOUR;

        LOG.debug(String.format("Free memory available in xen: %d", freeMemoryInMB));
        return freeMemoryInMB;
    }

    public long calculateDisk() {
        long freeSpaceInMB = instancesDirectory.getFreeSpace() / ONE_THOUSAND_TWENTY_FOUR / ONE_THOUSAND_TWENTY_FOUR;
        LOG.debug(String.format("Free Disk available: %dMB", freeSpaceInMB));
        return freeSpaceInMB;
    }

    protected File getFile(String instancePath) {
        return new File(instancePath);
    }

    public void reserveResources(String instanceId, InstanceTypeConfiguration instanceTypeConfiguration) {
        LOG.debug(String.format("Reserving resources for %s, current reserved resources are: Cores: %d, Disk: %dMB, Memory: %dMB", instanceTypeConfiguration, getTotalReservedCores(), getTotalReservedDiskInMB(), getTotalReservedMemoryInMB()));
        ReservedResources reservedResources = new ReservedResources(instanceTypeConfiguration.getNumCores(), 1L * instanceTypeConfiguration.getDiskSizeInGB() * ONE_THOUSAND_TWENTY_FOUR, instanceTypeConfiguration.getMemorySizeInMB());
        this.cache.put(new Element(instanceId, reservedResources));
    }

    public void unreserveResources(String instanceId) {
        LOG.debug(String.format("Releasing reservation resources for %s, current reserved resources are: Cores: %d, Disk: %dMB, Memory: %dMB", instanceId, getTotalReservedCores(), getTotalReservedDiskInMB(), getTotalReservedMemoryInMB()));
        this.cache.remove(instanceId);
    }
}
