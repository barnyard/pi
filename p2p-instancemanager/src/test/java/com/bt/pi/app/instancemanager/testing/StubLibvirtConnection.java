package com.bt.pi.app.instancemanager.testing;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;

import com.bt.pi.app.instancemanager.libvirt.Connection;
import com.bt.pi.app.instancemanager.libvirt.DomainNotFoundException;
import com.bt.pi.core.conf.Property;

public class StubLibvirtConnection implements Connection {
    private static final int FOUR_OH_NINE_SIX = 4096;
    private static final int ONE_OH_TWO_FOUR = 1024;
    private static final long ONE_OH_TWO_FOUR_LONG = 1024L;
    private static final String LIBVIRT_CONNECTION_STRING = "libvirt.connection.string";
    private static final String DEFAULT_LIBVIRT_CONNECTION_STRING = "test:///";
    private static final int CPU = 1;
    private static final long MAX_MEM = 256 * 1024;
    private List<String> executeCommands;
    private String libvirtConnectionString = DEFAULT_LIBVIRT_CONNECTION_STRING;
    private Map<Integer, Domain> domains;
    private int count;

    public StubLibvirtConnection() {
        executeCommands = Collections.synchronizedList(new ArrayList<String>());
        domains = new HashMap<Integer, Domain>();
    }

    public boolean assertLibvirtCommand(final String target) throws InterruptedException {
        synchronized (executeCommands) {
            for (String command : executeCommands) {
                if (command.startsWith(target)) {
                    System.err.println(command);
                    return true;
                }
            }
        }
        System.err.println("no LibVirt " + target);
        return false;
    }

    public boolean waitForLibvirtCommand(final String target, final int maxCount) throws InterruptedException {
        int count = 0;
        while (count < maxCount) {
            synchronized (executeCommands) {
                for (String command : executeCommands) {
                    // System.err.println(String.format("checking %s against %s", target, command));
                    if (command.startsWith(target)) {
                        System.err.println(command);
                        return true;
                    }
                }
            }
            Thread.sleep(100);
            count++;
        }
        System.err.println("no LibVirt " + target);
        return false;
    }

    public String getLibvirtConnectionString() {
        return libvirtConnectionString;
    }

    @Override
    public Domain domainLookupByID(int i) throws LibvirtException {
        addCommand("domainLookupByID:" + i);
        return domains.get(i);
    }

    private void addCommand(String command) {
        synchronized (executeCommands) {
            executeCommands.add(command);
        }
    }

    @Override
    public Domain domainLookupByName(String domainName) throws LibvirtException {
        System.err.println("Looking up domain by name:" + domainName);
        addCommand("domainLookupByName" + domainName);
        for (Domain domain : domains.values()) {
            if (domainName.equals(domain.getName())) {
                System.err.println("returning " + domain);
                return domain;
            }
        }
        System.err.println("returning null");
        return null;
    }

    @Override
    public NodeInfo getNodeInfo() throws LibvirtException {
        addCommand("getNodeInfo");
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.memory = FOUR_OH_NINE_SIX * ONE_OH_TWO_FOUR * ONE_OH_TWO_FOUR_LONG;
        nodeInfo.cpus = ONE_OH_TWO_FOUR;
        return nodeInfo;
    }

    @Override
    public int[] listDomains() throws LibvirtException {
        addCommand("listDomains");
        int[] domainIds = new int[domains.size()];
        int i = 0;
        for (Integer domainId : domains.keySet())
            domainIds[i++] = domainId;

        return domainIds;
    }

    @Override
    public void shutdown(Domain domain) throws LibvirtException {
        if (domain == null)
            return;

        addCommand("shutdown");
        System.err.println("Shutting down domain with id: " + domain.getID());
        domains.remove(domain.getID());
    }

    @Override
    public void destroy(Domain domain) throws LibvirtException {
        if (domain == null)
            return;

        addCommand("destroy");
        System.err.println("Destroying domain with id: " + domain.getID());
        domains.remove(domain.getID());
    }

    @Override
    public Domain startDomain(String aLibvirtXml) throws LibvirtException {
        addCommand("startDomain" + aLibvirtXml);

        String domainName = aLibvirtXml.substring(aLibvirtXml.indexOf("<name>") + 6, aLibvirtXml.indexOf("</name>"));
        String memory = aLibvirtXml.indexOf("<memory>") > 0 ? aLibvirtXml.substring(aLibvirtXml.indexOf("<memory>") + 8, aLibvirtXml.indexOf("</memory>")) : null;
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.nrVirtCpu = CPU;
        domainInfo.state = DomainState.VIR_DOMAIN_RUNNING;

        Domain mockDomain = mock(Domain.class);
        when(mockDomain.getID()).thenReturn(++count);
        when(mockDomain.getName()).thenReturn(domainName);
        when(mockDomain.getXMLDesc(anyInt())).thenReturn(aLibvirtXml);
        when(mockDomain.getInfo()).thenReturn(domainInfo);
        when(mockDomain.getMaxMemory()).thenReturn(StringUtils.isEmpty(memory) ? MAX_MEM : Long.parseLong(memory));

        System.err.println("Adding domain with id: " + mockDomain.getID() + " and name " + mockDomain.getName());
        domains.put(count, mockDomain);

        return mockDomain;
    }

    public Domain crashDomain(String aLibvirtXml) throws LibvirtException {
        addCommand("startDomain" + aLibvirtXml);
        String domainName = aLibvirtXml.substring(aLibvirtXml.indexOf("<name>") + 6, aLibvirtXml.indexOf("</name>"));
        String memory = aLibvirtXml.indexOf("<memory>") > 0 ? aLibvirtXml.substring(aLibvirtXml.indexOf("<memory>") + 8, aLibvirtXml.indexOf("</memory>")) : null;
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.nrVirtCpu = CPU;
        domainInfo.state = DomainState.VIR_DOMAIN_CRASHED;

        Domain mockDomain = mock(Domain.class);
        when(mockDomain.getID()).thenReturn(++count);
        when(mockDomain.getName()).thenReturn(domainName);
        when(mockDomain.getXMLDesc(anyInt())).thenReturn(aLibvirtXml);
        when(mockDomain.getInfo()).thenReturn(domainInfo);
        when(mockDomain.getMaxMemory()).thenReturn(StringUtils.isEmpty(memory) ? MAX_MEM : Long.parseLong(memory));

        System.err.println("Adding domain with id: " + mockDomain.getID());
        domains.put(count, mockDomain);

        return mockDomain;
    }

    public void crashARunningDomain(String instanceId) throws LibvirtException {
        for (Domain domain : domains.values()) {
            if (domain.getName().equals(instanceId)) {
                System.err.println("About to crash " + instanceId);
                DomainInfo domainInfo = new DomainInfo();
                domainInfo.nrVirtCpu = CPU;
                domainInfo.state = DomainState.VIR_DOMAIN_CRASHED;
                when(domain.getInfo()).thenReturn(domainInfo);
            }
        }
    }

    @Property(key = LIBVIRT_CONNECTION_STRING, defaultValue = DEFAULT_LIBVIRT_CONNECTION_STRING)
    public void setLibvirtConnectionString(String value) {
        this.libvirtConnectionString = value;
    }

    public void reset() {
        domains.clear();
        synchronized (executeCommands) {
            executeCommands.clear();
        }
        count = 0;
    }

    @Override
    public void attachDevice(String domainName, String deviceXml) throws LibvirtException {
        addCommand(String.format("attachDevice(%s, %s)", domainName, deviceXml));
        Domain domain = domainLookupByName(domainName);
        if (null != domain) {
            String xml = domain.getXMLDesc(1);
            System.err.println("##### " + xml);
            String newXml = xml + deviceXml;
            System.err.println("###### " + newXml);
            when(domain.getXMLDesc(anyInt())).thenReturn(newXml);
        }
    }

    @Override
    public void detachDevice(String domainName, String deviceXml) throws LibvirtException {
        addCommand(String.format("detachDevice(%s, %s)", domainName, deviceXml));
        Domain domain = domainLookupByName(domainName);
        if (null != domain) {
            String xml = domain.getXMLDesc(1);
            System.err.println("##### " + xml);
            int start = xml.lastIndexOf("<disk");
            int end = xml.lastIndexOf("</disk>") + "</disk>".length();
            if (start > -1 && end > -1) {
                String newXml = xml.substring(0, start) + xml.substring(end);
                System.err.println("###### " + newXml);
                when(domain.getXMLDesc(anyInt())).thenReturn(newXml);
            }
        } else
            throw new DomainNotFoundException();
    }

    @Override
    public void pauseInstance(String instanceId) throws LibvirtException {
        addCommand(String.format("pauseInstance(%s)", instanceId));
        Domain domain = domainLookupByName(instanceId);
        if (null == domain)
            throw new DomainNotFoundException();

        DomainInfo domainInfo = new DomainInfo();
        domainInfo.state = DomainState.VIR_DOMAIN_PAUSED;
        when(domain.getInfo()).thenReturn(domainInfo);
    }

    @Override
    public void unPauseInstance(String instanceId) throws LibvirtException {
        addCommand(String.format("unPauseInstance(%s)", instanceId));
        Domain domain = domainLookupByName(instanceId);
        if (null == domain)
            throw new DomainNotFoundException();

        DomainInfo domainInfo = new DomainInfo();
        domainInfo.state = DomainState.VIR_DOMAIN_RUNNING;
        when(domain.getInfo()).thenReturn(domainInfo);
    }

    @Override
    public String getDomainXml(String domainName) throws LibvirtException {
        System.err.println(String.format("%s getDomainXml(%s)", new Date(), domainName));
        Domain domain = this.domainLookupByName(domainName);
        return domain.getXMLDesc(0);
    }
}
