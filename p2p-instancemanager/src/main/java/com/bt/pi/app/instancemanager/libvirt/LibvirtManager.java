/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.libvirt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.Error;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceCheckpoint;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.os.FileManager;
import com.bt.pi.core.parser.KoalaJsonParser;

@Component
public class LibvirtManager {
    private static final String UNEXPECTED_EXCEPTION_WHILE_PARSING_THE_LIST_OF_XEN_S_RUNNING_INSTANCES = "Unexpected exception while parsing the list of xen's running instances";
    private static final String UNABLE_TO_GET_DOMAIN = "Unable to get domain:";
    private static final int FLAG_NOT_USED = -1;
    private static final String HVM_OS_TYPE = "hvm";
    private static final String LINUX_OS_TYPE = "linux";
    private static final String INSTANCE_STATE_WITH_ID_S_S = "Instance state with id %s: %s";
    private static final String INSTANCE_S_DOES_NOT_EXIST = "Instance %s does not exist";
    private static final String SIXTEEN = "16";
    private static final String EIGHTEEN = "18";
    private static final String PLATFORM_TYPE = "TYPE";
    private static final String USE_RAMDISK = "use_ramdisk";
    private static final String SWAPPATH = "SWAPPATH";
    private static final String TRUE = "true";
    private static final String MEMORY = "MEMORY";
    private static final String BRIDGEDEV = "BRIDGEDEV";
    private static final String PRIVMACADDR = "PRIVMACADDR";
    private static final String NAME = "NAME";
    private static final String BASEPATH = "BASEPATH";
    private static final String OS_TYPE = "OS_TYPE";
    private static final String XEN_ID = "XEN_ID";
    private static final String KERNEL_ID = "KERNEL_ID";
    private static final String IMAGE_ID = "IMAGE_ID";
    private static final String RAMDISK_ID = "RAMDISK_ID";

    private static final Log LOG = LogFactory.getLog(LibvirtManager.class);

    private Connection connection;
    private LibvirtTemplateGenerator libvirtTemplateGenerator;
    private FileManager fileManager;
    private KoalaJsonParser koalaJsonParser;

    public LibvirtManager() {
        connection = null;
        libvirtTemplateGenerator = null;
        fileManager = null;
        koalaJsonParser = null;
    }

    @Resource(name = "libvirtConnection")
    public void setConnection(Connection aConnection) {
        this.connection = aConnection;
    }

    @Resource
    public void setLibvirtTemplateGenerator(LibvirtTemplateGenerator aLibvirtTemplateGenerator) {
        this.libvirtTemplateGenerator = aLibvirtTemplateGenerator;
    }

    @Resource
    public void setFileManager(FileManager aFileManager) {
        fileManager = aFileManager;
    }

    @Resource
    public void setKoalaJsonParser(KoalaJsonParser aKoalaJsonParser) {
        koalaJsonParser = aKoalaJsonParser;
    }

    public Collection<Domain> getAllInstances() {
        Collection<Domain> result = new ArrayList<Domain>();
        int[] domains;

        try {
            domains = this.connection.listDomains();
            for (int i = 0; i < domains.length; i++) {
                Domain domain = this.connection.domainLookupByID(domains[i]);
                result.add(domain);
            }
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to get list of domains");
        }
        return result;
    }

    public Collection<String> getAllRunningInstances() {
        Collection<Domain> libvirtInstances = getAllInstances();
        Collection<String> runningInstances = new ArrayList<String>();
        for (Domain domain : libvirtInstances) {
            try {
                if (notASystemDomain(domain) && isDomainRunning(domain))
                    runningInstances.add(domain.getName());
            } catch (LibvirtException e) {
                LOG.warn(UNEXPECTED_EXCEPTION_WHILE_PARSING_THE_LIST_OF_XEN_S_RUNNING_INSTANCES, e);
            }
        }
        return runningInstances;
    }

    public Collection<String> getAllCrashedInstances() {
        LOG.debug("getAllCrashedInstances()");
        Collection<Domain> libvirtInstances = getAllInstances();
        Collection<String> result = new ArrayList<String>();
        for (Domain domain : libvirtInstances) {
            try {
                if (notASystemDomain(domain) && isDomainCrashed(domain))
                    result.add(domain.getName());
            } catch (LibvirtException e) {
                LOG.warn(UNEXPECTED_EXCEPTION_WHILE_PARSING_THE_LIST_OF_XEN_S_RUNNING_INSTANCES, e);
            }
        }
        return result;
    }

    private boolean isDomainCrashed(Domain domain) throws LibvirtException {
        return isDomainInStates(domain, DomainState.VIR_DOMAIN_CRASHED);
    }

    private boolean notASystemDomain(Domain domain) throws LibvirtException {
        return !domain.getName().startsWith("Domain-");
    }

    public NodeInfo getNodeInfo() {
        try {
            return connection.getNodeInfo();
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to get the node info");
        }
        return null;
    }

    public String generateLibvirtXml(Instance instance, ImagePlatform platform, String basePath, boolean useRamdisk, String privateMacAddress, String bridgeDevice, String memory, String vcpus) {
        String instanceId = instance.getInstanceId();
        String kernelId = instance.getKernelId();
        String imageId = instance.getImageId();

        Map<String, Object> model = new HashMap<String, Object>();

        model.put(PLATFORM_TYPE, platform.toString());
        model.put("use_ephemeral", TRUE);
        model.put(BASEPATH, basePath);
        model.put(NAME, instanceId);
        model.put(PRIVMACADDR, privateMacAddress);
        model.put(BRIDGEDEV, bridgeDevice);
        model.put(MEMORY, memory);
        model.put("VCPUS", vcpus);
        model.put(IMAGE_ID, imageId);

        switch (platform) {
        case windows:
            model.put(OS_TYPE, HVM_OS_TYPE);
            model.put(XEN_ID, SIXTEEN);
            break;
        case opensolaris:
            model.put(OS_TYPE, LINUX_OS_TYPE);
            model.put(SWAPPATH, basePath);
            model.put(USE_RAMDISK, String.valueOf(useRamdisk));
            model.put(XEN_ID, EIGHTEEN);
            model.put(KERNEL_ID, kernelId);
            break;
        default:
            model.put(OS_TYPE, LINUX_OS_TYPE);
            model.put(SWAPPATH, basePath);
            model.put(USE_RAMDISK, String.valueOf(useRamdisk));
            model.put(XEN_ID, EIGHTEEN);
            model.put(KERNEL_ID, kernelId);

            if (useRamdisk) {
                model.put(RAMDISK_ID, instance.getRamdiskId());
            }

            break;
        }

        String libvirtXml = this.libvirtTemplateGenerator.buildXml(model);
        saveLibvirtXml(basePath, libvirtXml);
        return libvirtXml;
    }

    public void startInstance(String libvirtXml, String instanceId, String basePath) {
        if (!existsInstance(instanceId)) {
            LOG.debug(String.format("Starting up instance %s", instanceId));
            Domain newInstance = startDomain(libvirtXml);
            createInstanceCheckpoint(newInstance, basePath);
        } else {
            LOG.info(String.format("instance %s already exists, no need to start a new one", instanceId));
        }
    }

    public Domain lookupInstance(String instanceId) {
        try {
            return this.connection.domainLookupByName(instanceId);
        } catch (LibvirtException e) {
            if (e.getMessage().startsWith("Domain not found:")) {
                LOG.info(String.format(INSTANCE_S_DOES_NOT_EXIST, instanceId));
            } else {
                LOG.warn(String.format("Unable to lookup instance %s", instanceId), e);
            }
        }

        return null;
    }

    public boolean existsInstance(String instanceId) {
        try {
            if (this.connection.domainLookupByName(instanceId) != null)
                return true;
        } catch (LibvirtException e) {
            LOG.debug(String.format(INSTANCE_S_DOES_NOT_EXIST, instanceId));
        }

        return false;
    }

    public boolean isInstanceRunning(String instanceId) {
        return isInstanceInStates(instanceId, DomainState.VIR_DOMAIN_BLOCKED, DomainState.VIR_DOMAIN_RUNNING, DomainState.VIR_DOMAIN_NOSTATE, DomainState.VIR_DOMAIN_PAUSED);
    }

    private boolean isDomainRunning(Domain domain) throws LibvirtException {
        return isDomainInStates(domain, DomainState.VIR_DOMAIN_RUNNING, DomainState.VIR_DOMAIN_BLOCKED, DomainState.VIR_DOMAIN_NOSTATE, DomainState.VIR_DOMAIN_PAUSED);
    }

    public void destroyInstance(Domain domain) {
        try {
            this.connection.destroy(domain);
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to destroy instance");
        }
    }

    public void stopInstance(Domain domain) {
        try {
            this.connection.shutdown(domain);
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to shutdown instance");
        }
    }

    public void reboot(Domain domain) {
        try {
            domain.reboot(0);
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to reboot instance");
        }
    }

    private void createInstanceCheckpoint(Domain newInstance, String basePath) {
        try {
            InstanceCheckpoint checkpoint = new InstanceCheckpoint();
            DomainInfo domainInfo = newInstance.getInfo();
            checkpoint.setState(domainInfo.state.toString());
            String checkPointJsoned = this.koalaJsonParser.getJson(checkpoint);
            this.fileManager.saveFile(basePath + "/instance-checkpoint.json", checkPointJsoned);
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to create instance checkpoint in:" + basePath);
        }
    }

    private void saveLibvirtXml(String basePath, String libvirtXml) {
        this.fileManager.saveFile(String.format("%s/libvirt.xml", basePath), libvirtXml);
    }

    public void pauseInstance(String instanceId) {
        try {
            this.connection.pauseInstance(instanceId);
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to pause instance:" + instanceId);
        }
    }

    public void unPauseInstance(String instanceId) {
        try {
            this.connection.unPauseInstance(instanceId);
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to unpause instance:" + instanceId);
        }
    }

    private Domain startDomain(String libvirtXml) {
        try {
            return this.connection.startDomain(libvirtXml);
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to start an instance");
        }
        return null;
    }

    public void attachVolume(String instanceId, String libvirtXml) {
        LOG.debug(String.format("attachVolume(%s, %s)", instanceId, libvirtXml));
        try {
            this.connection.attachDevice(instanceId, libvirtXml);
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to attach a volume");
        }
    }

    public void detachVolume(String instanceId, String libvirtXml) {
        LOG.debug(String.format("detachVolume(%s, %s)", instanceId, libvirtXml));
        try {
            this.connection.detachDevice(instanceId, libvirtXml);
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to detach a volume");
        }
    }

    public boolean volumeExists(String instanceId, String devicePath) {
        LOG.debug(String.format("volumeExists(%s, %s)", instanceId, devicePath));
        try {
            Domain domain = this.connection.domainLookupByName(instanceId);
            if (null == domain)
                throw new LibvirtManagerException(UNABLE_TO_GET_DOMAIN + instanceId);
            String domainXml = domain.getXMLDesc(FLAG_NOT_USED);
            if (domainXml.contains(devicePath)) {
                LOG.warn(String.format("Device %s still attached to Instance %s. Domain Xml: %s", devicePath, instanceId, domainXml));
                return true;
            }
        } catch (LibvirtException e) {
            processLibvirtException(e, UNABLE_TO_GET_DOMAIN + instanceId);
        }

        return false;
    }

    private void processLibvirtException(LibvirtException e, String message) {
        StringBuffer buf = new StringBuffer(message);
        Error error = e.getError();
        if (null != error) {
            String sep = ", ";
            buf.append(sep + String.format("error code   : %s", error.getCode()));
            buf.append(sep + String.format("error level  : %s", error.getLevel()));
            buf.append(sep + String.format("error message: %s", error.getMessage()));
            buf.append(sep + String.format("error int1   : %s", error.getInt1()));
            buf.append(sep + String.format("error int2   : %s", error.getInt2()));
            buf.append(sep + String.format("error str1   : %s", error.getStr1()));
            buf.append(sep + String.format("error str2   : %s", error.getStr2()));
            buf.append(sep + String.format("error str3   : %s", error.getStr3()));
        }
        LOG.error(buf.toString(), e);
        throw new LibvirtManagerException(message, e);
    }

    public boolean isInstanceCrashed(String instanceId) {
        return isInstanceInStates(instanceId, DomainState.VIR_DOMAIN_CRASHED);
    }

    private boolean isInstanceInStates(String instanceId, DomainState... domainStates) {
        try {
            Domain domain = connection.domainLookupByName(instanceId);
            if (domain != null && domain.getInfo() != null) {
                if (LOG.isDebugEnabled())
                    LOG.debug(String.format(INSTANCE_STATE_WITH_ID_S_S, instanceId, domain.getInfo().state.toString()));
                return isDomainInStates(domain, domainStates);
            } else
                LOG.debug(String.format(INSTANCE_STATE_WITH_ID_S_S, instanceId, null));
        } catch (LibvirtException e) {
            LOG.debug(String.format(INSTANCE_S_DOES_NOT_EXIST, instanceId));
        }

        return false;
    }

    private boolean isDomainInStates(Domain domain, DomainState... domainStates) throws LibvirtException {
        DomainState state = domain.getInfo().state;
        boolean result = false;
        for (DomainState domainState : domainStates) {
            result = result || domainState.equals(state);
        }
        return result;
    }

    public String getDomainXml(String instanceId) {
        LOG.debug(String.format("getDomainXml(%s)", instanceId));
        try {
            return this.connection.getDomainXml(instanceId);
        } catch (LibvirtException e) {
            processLibvirtException(e, "Unable to get domain XML:" + instanceId);
        }
        return null;
    }
}
