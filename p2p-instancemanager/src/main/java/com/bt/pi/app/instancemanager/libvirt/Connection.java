/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.libvirt;

import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;

public interface Connection {

    int[] listDomains() throws LibvirtException;

    Domain domainLookupByID(int i) throws LibvirtException;

    NodeInfo getNodeInfo() throws LibvirtException;

    Domain startDomain(String libvirtXml) throws LibvirtException;

    Domain domainLookupByName(String domainName) throws LibvirtException;

    void shutdown(Domain domain) throws LibvirtException;

    void attachDevice(String domainName, String deviceXml) throws LibvirtException;

    void detachDevice(String domainName, String deviceXml) throws LibvirtException;

    void destroy(Domain domain) throws LibvirtException;

    void pauseInstance(String instanceId) throws LibvirtException;

    void unPauseInstance(String domainName) throws LibvirtException;

    String getDomainXml(String domainName) throws LibvirtException;
}
