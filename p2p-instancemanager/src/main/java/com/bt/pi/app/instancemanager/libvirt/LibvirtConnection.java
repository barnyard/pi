/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.libvirt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;

@Component
public class LibvirtConnection implements Connection {
    private static final String LIBVIRT_CONNECTION_STRING = "libvirt.connection.string";
    private static final String DEFAULT_LIBVIRT_CONNECTION_STRING = "xen:///";
    private static final Log LOG = LogFactory.getLog(LibvirtConnection.class);
    private Connect connect;
    private String libvirtConnectionString = DEFAULT_LIBVIRT_CONNECTION_STRING;

    public LibvirtConnection() {
    }

    @Override
    public Domain domainLookupByID(int i) throws LibvirtException {
        LOG.debug("domainLookupByID(" + i + ")");
        init();
        return this.connect.domainLookupByID(i);
    }

    @Override
    public int[] listDomains() throws LibvirtException {
        LOG.debug("listDomains()");
        init();
        return this.connect.listDomains();
    }

    @Override
    public NodeInfo getNodeInfo() throws LibvirtException {
        LOG.debug("getNodeInfo()");
        init();
        return this.connect.nodeInfo();
    }

    @Override
    public Domain startDomain(String libvirtXml) throws LibvirtException {
        LOG.debug(String.format("startDomain(%s)", libvirtXml));
        init();
        return this.connect.domainCreateLinux(libvirtXml, 0);
    }

    @Override
    public Domain domainLookupByName(String domainName) throws LibvirtException {
        LOG.debug(String.format("domainLookupByName(%s)", domainName));
        init();
        return this.connect.domainLookupByName(domainName);
    }

    @Override
    public void shutdown(Domain domain) throws LibvirtException {
        LOG.debug(String.format("shutdownDomain(%s)", domain.getName()));
        init();
        domain.shutdown();
    }

    @Override
    public void destroy(Domain domain) throws LibvirtException {
        LOG.debug(String.format("destroy(%s)", domain.getName()));
        init();
        domain.destroy();
    }

    @Override
    public void attachDevice(String domainName, String deviceXml) throws LibvirtException {
        LOG.debug(String.format("attachDevice(%s, %s)", domainName, deviceXml));
        init();
        Domain domain = this.domainLookupByName(domainName);
        if (null == domain)
            throw new DomainNotFoundException();
        domain.attachDevice(deviceXml);
    }

    @Override
    public void detachDevice(String domainName, String deviceXml) throws LibvirtException {
        LOG.debug(String.format("detachDevice(%s, %s)", domainName, deviceXml));
        Domain domain = this.domainLookupByName(domainName);
        if (null == domain)
            throw new DomainNotFoundException();
        domain.detachDevice(deviceXml);
    }

    @Override
    public void pauseInstance(String domainName) throws LibvirtException {
        LOG.debug(String.format("pauseInstance(%s)", domainName));
        Domain domain = this.domainLookupByName(domainName);
        if (null == domain) {
            throw new DomainNotFoundException();
        }

        domain.suspend();
    }

    @Override
    public void unPauseInstance(String domainName) throws LibvirtException {
        LOG.debug(String.format("unPauseInstance(%s)", domainName));
        Domain domain = this.domainLookupByName(domainName);
        if (null == domain) {
            throw new DomainNotFoundException();
        }

        domain.resume();
    }

    @Property(key = LIBVIRT_CONNECTION_STRING, defaultValue = DEFAULT_LIBVIRT_CONNECTION_STRING)
    public void setLibvirtConnectionString(String value) {
        this.libvirtConnectionString = value;
    }

    private synchronized void init() throws LibvirtException {
        if (null == connect)
            this.connect = newConnection(this.libvirtConnectionString);
    }

    protected Connect newConnection(String connectionString) throws LibvirtException {
        return new Connect(connectionString);
    }

    @Override
    public String getDomainXml(String domainName) throws LibvirtException {
        LOG.debug(String.format("getDomainXml(%s)", domainName));
        Domain domain = this.domainLookupByName(domainName);
        if (null == domain) {
            throw new DomainNotFoundException();
        }
        return domain.getXMLDesc(0);
    }
}
