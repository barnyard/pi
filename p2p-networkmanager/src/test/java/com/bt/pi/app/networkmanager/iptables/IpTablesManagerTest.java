package com.bt.pi.app.networkmanager.iptables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.NetworkProtocol;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.NetworkRuleType;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.iptables.IpTablesHelper;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

public class IpTablesManagerTest {
    private IpTablesManager ipTablesManager;
    private IpTablesBuilder ipTablesBuilder;
    private IpTablesHelper ipTablesHelper;
    private CommandRunner commandRunner;
    private CommandResult iptablesSaveNatCommandResult;
    private CommandResult iptablesSaveFilterCommandResult;
    private List<SecurityGroup> securityGroups;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private PiIdBuilder piIdBuilder;
    private PId subnetAllocationIndexId;
    private DhtCache dhtCache;
    private SubnetAllocationIndex subnetAllocationIndex;
    private Set<ResourceRange> resourceRanges;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        iptablesSaveNatCommandResult = mock(CommandResult.class);
        when(iptablesSaveNatCommandResult.getOutputLines()).thenReturn(sampleIptablesNatOutput());

        iptablesSaveFilterCommandResult = mock(CommandResult.class);
        when(iptablesSaveFilterCommandResult.getOutputLines()).thenReturn(sampleIptablesFilterOutput());

        commandRunner = mock(CommandRunner.class);
        when(commandRunner.run("iptables-save -t nat")).thenReturn(iptablesSaveNatCommandResult);
        when(commandRunner.run("iptables-save -t filter")).thenReturn(iptablesSaveFilterCommandResult);

        ipTablesHelper = new IpTablesHelper();
        ipTablesBuilder = new IpTablesBuilder();
        ipTablesBuilder.setIpTablesHelper(ipTablesHelper);

        setupSecurityGroups();

        consumedDhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);
        when(consumedDhtResourceRegistry.getByType(isA(Class.class))).thenAnswer(new Answer<List<? extends PiEntity>>() {
            public List<? extends PiEntity> answer(InvocationOnMock invocation) throws Throwable {
                return securityGroups;
            }
        });

        subnetAllocationIndexId = mock(PId.class);
        subnetAllocationIndex = new SubnetAllocationIndex();
        resourceRanges = new HashSet<ResourceRange>();
        subnetAllocationIndex.setResourceRanges(resourceRanges);

        dhtCache = mock(DhtCache.class);
        doAnswer(new GenericContinuationAnswer<SubnetAllocationIndex>(subnetAllocationIndex)).when(dhtCache).get(eq(subnetAllocationIndexId), isA(PiContinuation.class));

        piIdBuilder = mock(PiIdBuilder.class);
        when(this.piIdBuilder.getPId(SubnetAllocationIndex.URL)).thenReturn(this.subnetAllocationIndexId);
        when(subnetAllocationIndexId.forLocalRegion()).thenReturn(subnetAllocationIndexId);

        ipTablesManager = new IpTablesManager();
        ipTablesManager.setIpTablesBuilder(ipTablesBuilder);
        ipTablesManager.setCommandRunner(commandRunner);
        ipTablesManager.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
        ipTablesManager.setIpTablesHelper(ipTablesHelper);
        ipTablesManager.setDhtCache(dhtCache);
        ipTablesManager.setPiIdBuilder(this.piIdBuilder);
    }

    private void setupSecurityGroups() {
        NetworkRule networkRule1 = new NetworkRule();
        networkRule1.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
        networkRule1.setSourceNetworks(new String[] { "0.0.0.0/0" });
        networkRule1.setDestinationSecurityGroupName("default");
        networkRule1.setNetworkProtocol(NetworkProtocol.TCP);
        networkRule1.setPortRangeMin(22);
        networkRule1.setPortRangeMax(22);

        NetworkRule networkRule2 = new NetworkRule();
        networkRule2.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
        networkRule2.setSourceNetworks(new String[] { "1.2.3.4/32" });
        networkRule2.setDestinationSecurityGroupName("default");
        networkRule2.setNetworkProtocol(NetworkProtocol.UDP);
        networkRule2.setPortRangeMin(0);
        networkRule2.setPortRangeMax(0);

        SecurityGroup securityGroup1 = new SecurityGroup("bozo", "default", 10L, "172.0.0.0", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup1.getInstances().put("172.0.0.3", new InstanceAddress("172.0.0.3", null, "aa:aa:aa:aa:aa:aa"));
        securityGroup1.getInstances().put("172.0.0.2", new InstanceAddress("172.0.0.2", "1.2.3.4", "aa:aa:aa:aa:aa:aa"));
        securityGroup1.setNetworkRules(new HashSet<NetworkRule>(Arrays.asList(new NetworkRule[] { networkRule1 })));

        SecurityGroup securityGroup2 = new SecurityGroup("nutter", "default", 11L, "172.0.0.16", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup2.getInstances().put("172.0.0.18", new InstanceAddress("172.0.0.18", "5.6.7.8", "aa:aa:aa:aa:aa:aa"));
        securityGroup2.setNetworkRules(new HashSet<NetworkRule>(Arrays.asList(new NetworkRule[] { networkRule2 })));

        SecurityGroup securityGroup3 = new SecurityGroup("abc", "default", 12L, "172.0.0.32", "255.255.255.240", null, new HashSet<NetworkRule>());
        securityGroup3.getInstances().put("172.0.0.45", new InstanceAddress("172.0.0.45", "9.10.11.12", "d0:0d:4a:1d:08:f7"));

        securityGroups = new ArrayList<SecurityGroup>();
        securityGroups.add(securityGroup1);
        securityGroups.add(securityGroup2);
        securityGroups.add(securityGroup3);
    }

    @Test
    public void shouldRefreshFilterAndNatTables() {
        // setup
        final List<String> filterExpectation = new ArrayList<String>();
        filterExpectation.add("# Generated by iptables-save v1.4.1.1 on Fri Oct 16 12:31:01 2009");
        filterExpectation.add("*filter");
        filterExpectation.add(":INPUT ACCEPT [212801:217224757]");
        filterExpectation.add(":FORWARD ACCEPT [0:0]");
        filterExpectation.add(":OUTPUT ACCEPT [148444:25148602]");
        filterExpectation.add(":aa+aa - [0:0]");
        filterExpectation.add(":aa/aa - [0:0]");
        filterExpectation.add(":aa_aa - [0:0]");
        filterExpectation.add(":pi-chain - [0:0]");
        filterExpectation.add("-A FORWARD -j pi-chain");
        filterExpectation.add("-N FLTR-uywrHnhaMUYSmOrc8AJklA==");
        filterExpectation.add("-A pi-chain -d 172.0.0.0/28 -j FLTR-uywrHnhaMUYSmOrc8AJklA==");
        filterExpectation.add("-A FLTR-uywrHnhaMUYSmOrc8AJklA== -s 0.0.0.0/0 -d 172.0.0.0/28 -p tcp --dport 22:22 -j ACCEPT");
        filterExpectation.add("-N FLTR-3ATzSoWNjSDd/XMl3AMZ/A==");
        filterExpectation.add("-A pi-chain -d 172.0.0.16/28 -j FLTR-3ATzSoWNjSDd/XMl3AMZ/A==");
        filterExpectation.add("-A FLTR-3ATzSoWNjSDd/XMl3AMZ/A== -s 1.2.3.4/32 -d 172.0.0.16/28 -p udp -j ACCEPT");
        filterExpectation.add("-N FLTR-rVfSbRBCndprtbocA7Nsbg==");
        filterExpectation.add("-A pi-chain -d 172.0.0.32/28 -j FLTR-rVfSbRBCndprtbocA7Nsbg==");
        filterExpectation.add("COMMIT");
        filterExpectation.add("# Completed on Fri Oct 16 12:31:01 2009");

        final List<String> natExpectation = new ArrayList<String>();
        natExpectation.add("*nat");
        natExpectation.add(":PREROUTING ACCEPT [0:0]");
        natExpectation.add(":POSTROUTING ACCEPT [2:120]");
        natExpectation.add(":OUTPUT ACCEPT [2:120]");
        natExpectation.add(":bb - [0:0]");
        natExpectation.add("-N PI-PREROUTING");
        natExpectation.add("-A PREROUTING -j PI-PREROUTING");
        natExpectation.add("-N PI-OUTPUT");
        natExpectation.add("-A OUTPUT -j PI-OUTPUT");
        natExpectation.add("-N POST-uywrHnhaMUYSmOrc8AJklA==");
        natExpectation.add("-I POSTROUTING -s 172.0.0.0/28 -j POST-uywrHnhaMUYSmOrc8AJklA==");
        natExpectation.add("-A PI-PREROUTING -d 1.2.3.4 -j DNAT --to 172.0.0.2");
        natExpectation.add("-A PI-OUTPUT -d 1.2.3.4 -j DNAT --to 172.0.0.2");
        natExpectation.add("-A POST-uywrHnhaMUYSmOrc8AJklA== -s 172.0.0.2 -j SNAT --to 1.2.3.4");
        natExpectation.add("-N POST-3ATzSoWNjSDd/XMl3AMZ/A==");
        natExpectation.add("-I POSTROUTING -s 172.0.0.16/28 -j POST-3ATzSoWNjSDd/XMl3AMZ/A==");
        natExpectation.add("-A PI-PREROUTING -d 5.6.7.8 -j DNAT --to 172.0.0.18");
        natExpectation.add("-A PI-OUTPUT -d 5.6.7.8 -j DNAT --to 172.0.0.18");
        natExpectation.add("-A POST-3ATzSoWNjSDd/XMl3AMZ/A== -s 172.0.0.18 -j SNAT --to 5.6.7.8");
        natExpectation.add("-N POST-rVfSbRBCndprtbocA7Nsbg==");
        natExpectation.add("-I POSTROUTING -s 172.0.0.32/28 -j POST-rVfSbRBCndprtbocA7Nsbg==");
        natExpectation.add("-A PI-PREROUTING -d 9.10.11.12 -j DNAT --to 172.0.0.45");
        natExpectation.add("-A PI-OUTPUT -d 9.10.11.12 -j DNAT --to 172.0.0.45");
        natExpectation.add("-A POST-rVfSbRBCndprtbocA7Nsbg== -s 172.0.0.45 -j SNAT --to 9.10.11.12");
        natExpectation.add("COMMIT");
        natExpectation.add("# Completed on Fri Oct 16 18:46:03 2009");

        final String filterExpectationString = "echo '" + collapseList(filterExpectation) + "' | iptables-restore";
        final String natExpectationString = "echo '" + collapseList(natExpectation) + "' | iptables-restore";

        // act
        ipTablesManager.refreshIpTables();

        // assert
        verify(commandRunner, times(2)).runInShell(argThat(new ArgumentMatcher<String>() {
            public boolean matches(Object argument) {
                String arg = (String) argument;
                if (arg.indexOf("*filter") > -1) {
                    assertEquals(779, arg.length());
                    assertEquals(filterExpectationString, arg);
                }
                if (arg.indexOf("*nat") > -1) {
                    assertEquals(1053, arg.length());
                    assertEquals(natExpectationString, arg);
                }
                return true;
            }
        }));
    }

    @Test
    public void shouldRefreshFilterTablesWhenNoPiChain() {
        // setup
        List<String> sampleIptablesFilterOutputWithoutPiChain = sampleIptablesFilterOutput();
        assertTrue(sampleIptablesFilterOutputWithoutPiChain.remove(":pi-chain - [0:0]"));
        assertTrue(sampleIptablesFilterOutputWithoutPiChain.remove("-A FORWARD -j pi-chain"));

        when(iptablesSaveFilterCommandResult.getOutputLines()).thenReturn(sampleIptablesFilterOutputWithoutPiChain);

        final List<String> filterExpectation = new ArrayList<String>();
        filterExpectation.add("# Generated by iptables-save v1.4.1.1 on Fri Oct 16 12:31:01 2009");
        filterExpectation.add("*filter");
        filterExpectation.add(":INPUT ACCEPT [212801:217224757]");
        filterExpectation.add(":FORWARD ACCEPT [0:0]");
        filterExpectation.add(":OUTPUT ACCEPT [148444:25148602]");
        filterExpectation.add(":aa+aa - [0:0]");
        filterExpectation.add(":aa/aa - [0:0]");
        filterExpectation.add(":aa_aa - [0:0]");
        filterExpectation.add("-N pi-chain");
        filterExpectation.add("-A FORWARD -j pi-chain");
        filterExpectation.add("-N FLTR-uywrHnhaMUYSmOrc8AJklA==");
        filterExpectation.add("-A pi-chain -d 172.0.0.0/28 -j FLTR-uywrHnhaMUYSmOrc8AJklA==");
        filterExpectation.add("-A FLTR-uywrHnhaMUYSmOrc8AJklA== -s 0.0.0.0/0 -d 172.0.0.0/28 -p tcp --dport 22:22 -j ACCEPT");
        filterExpectation.add("-N FLTR-3ATzSoWNjSDd/XMl3AMZ/A==");
        filterExpectation.add("-A pi-chain -d 172.0.0.16/28 -j FLTR-3ATzSoWNjSDd/XMl3AMZ/A==");
        filterExpectation.add("-A FLTR-3ATzSoWNjSDd/XMl3AMZ/A== -s 1.2.3.4/32 -d 172.0.0.16/28 -p udp -j ACCEPT");
        filterExpectation.add("-N FLTR-rVfSbRBCndprtbocA7Nsbg==");
        filterExpectation.add("-A pi-chain -d 172.0.0.32/28 -j FLTR-rVfSbRBCndprtbocA7Nsbg==");
        filterExpectation.add("COMMIT");
        filterExpectation.add("# Completed on Fri Oct 16 12:31:01 2009");

        final String filterExpectationString = "echo '" + collapseList(filterExpectation) + "' | iptables-restore";

        // act
        ipTablesManager.refreshIpTables();

        // assert
        verify(commandRunner, times(2)).runInShell(argThat(new ArgumentMatcher<String>() {
            public boolean matches(Object argument) {
                String arg = (String) argument;
                if (arg.indexOf("*filter") > -1) {
                    assertEquals(773, arg.length());
                    assertEquals(filterExpectationString, arg);
                }
                return true;
            }
        }));
    }

    private String collapseList(List<String> filterExpectation) {
        StringBuilder sb = new StringBuilder();
        for (String s : filterExpectation) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    @Test
    @Ignore
    public void realTest() {
        // setup
        CommandRunner commandRunner = new CommandRunner();
        ipTablesManager.setCommandRunner(commandRunner);

        // ipTablesManager.iptablesSaveCommand = "sudo iptables-save";
        // ipTablesManager.iptablesRestoreCommand = "sudo iptables-restore";

        // act
        ipTablesManager.refreshIpTables();
    }

    // private List<String> sampleIptablesOutputShort() {
    // List<String> sb = new ArrayList<String>();
    // sb.add("*nat");
    // sb.add(":PREROUTING ACCEPT [0:0]");
    // sb.add(":POSTROUTING ACCEPT [2:120]");
    // sb.add(":OUTPUT ACCEPT [2:120]");
    // sb.add(":POST-9W0ioE23GzlYxZDnfEkrFw== - [0:0]");
    // sb.add(":bb - [0:0]");
    // sb.add("-A PREROUTING -d 5.6.7.8/32 -j DNAT --to-destination 172.0.0.18");
    // sb.add("-A POSTROUTING -d 172.0.0.16/28 -j POST-9W0ioE23GzlYxZDnfEkrFw==");
    // sb.add("-A OUTPUT -d 5.6.7.8/32 -j DNAT --to-destination 172.0.0.18");
    // sb.add("-A POST-9W0ioE23GzlYxZDnfEkrFw== -d 172.0.0.18/32 -j SNAT --to-source 5.6.7.8");
    // sb.add("COMMIT");
    // sb.add("# Completed on Fri Oct 16 09:48:48 2009");
    // sb.add("# Generated by iptables-save v1.4.1.1 on Fri Oct 16 09:48:48 2009");
    // sb.add("*filter");
    // sb.add(":INPUT ACCEPT [212801:217224757]");
    // sb.add(":FORWARD ACCEPT [0:0]");
    // sb.add(":OUTPUT ACCEPT [148444:25148602]");
    // sb.add(":FLTR-9W0ioE23GzlYxZDnfEkrFw== - [0:0]");
    // sb.add(":aa+aa - [0:0]");
    // sb.add(":aa/aa - [0:0]");
    // sb.add(":aa_aa - [0:0]");
    // sb.add(":pi-chain - [0:0]");
    // sb.add("-A FORWARD -j pi-chain");
    // sb.add("-A FLTR-9W0ioE23GzlYxZDnfEkrFw== -s 1.2.3.4/32 -d 172.0.0.16/28 -p udp -j ACCEPT");
    // sb.add("-A pi-chain -d 172.0.0.16/28 -j FLTR-9W0ioE23GzlYxZDnfEkrFw==");
    // sb.add("COMMIT");
    // sb.add("# Completed on Fri Oct 16 09:48:48 2009");
    // return sb;
    // }

    private List<String> sampleIptablesFilterOutput() {
        List<String> sb = new ArrayList<String>();
        sb.add("# Generated by iptables-save v1.4.1.1 on Fri Oct 16 12:31:01 2009");
        sb.add("*filter");
        sb.add(":INPUT ACCEPT [212801:217224757]");
        sb.add(":FORWARD ACCEPT [0:0]");
        sb.add(":OUTPUT ACCEPT [148444:25148602]");
        sb.add(":FLTR-9W0ioE23GzlYxZDnfEkrFw== - [0:0]");
        sb.add(":FLTR-hgvHy68+HHdL/uFREcfE1Q== - [0:0]");
        sb.add(":FLTR-sOPwadtSCBogG/2OYDBM0w== - [0:0]");
        sb.add(":aa+aa - [0:0]");
        sb.add(":aa/aa - [0:0]");
        sb.add(":aa_aa - [0:0]");
        sb.add(":pi-chain - [0:0]");
        sb.add("-A FORWARD -j pi-chain");
        sb.add("-A FLTR-9W0ioE23GzlYxZDnfEkrFw== -s 1.2.3.4/32 -d 172.0.0.16/28 -p udp -j ACCEPT");
        sb.add("-A FLTR-sOPwadtSCBogG/2OYDBM0w== -d 172.0.0.0/28 -p tcp -m tcp --dport 22 -j ACCEPT");
        sb.add("-A pi-chain -d 172.0.0.0/28 -j FLTR-sOPwadtSCBogG/2OYDBM0w==");
        sb.add("-A pi-chain -d 172.0.0.16/28 -j FLTR-9W0ioE23GzlYxZDnfEkrFw==");
        sb.add("-A pi-chain -d 172.0.0.32/28 -j FLTR-hgvHy68+HHdL/uFREcfE1Q==");
        sb.add("COMMIT");
        sb.add("# Completed on Fri Oct 16 12:31:01 2009");
        return sb;
    }

    private List<String> sampleIptablesNatOutput() {
        List<String> sb = new ArrayList<String>();
        sb.add("*nat");
        sb.add(":PREROUTING ACCEPT [0:0]");
        sb.add(":POSTROUTING ACCEPT [2:120]");
        sb.add(":OUTPUT ACCEPT [2:120]");
        sb.add(":POST-9W0ioE23GzlYxZDnfEkrFw== - [0:0]");
        sb.add(":POST-hgvHy68+HHdL/uFREcfE1Q== - [0:0]");
        sb.add(":POST-sOPwadtSCBogG/2OYDBM0w== - [0:0]");
        sb.add(":bb - [0:0]");
        sb.add("-A PI-PREROUTING -d 1.2.3.4/32 -j DNAT --to-destination 172.0.0.2 ");
        sb.add("-A PI-PREROUTING -d 5.6.7.8/32 -j DNAT --to-destination 172.0.0.18");
        sb.add("-A PI-PREROUTING -d 9.10.11.12/32 -j DNAT --to-destination 172.0.0.45");
        sb.add("-A POSTROUTING -d 172.0.0.32/28 -j POST-hgvHy68+HHdL/uFREcfE1Q==");
        sb.add("-A POSTROUTING -d 172.0.0.16/28 -j POST-9W0ioE23GzlYxZDnfEkrFw==");
        sb.add("-A POSTROUTING -d 172.0.0.0/28 -j POST-sOPwadtSCBogG/2OYDBM0w==");
        sb.add("-A PI-OUTPUT -d 1.2.3.4/32 -j DNAT --to-destination 172.0.0.2");
        sb.add("-A PI-OUTPUT -d 5.6.7.8/32 -j DNAT --to-destination 172.0.0.18");
        sb.add("-A PI-OUTPUT -d 9.10.11.12/32 -j DNAT --to-destination 172.0.0.45");
        sb.add("-A POST-9W0ioE23GzlYxZDnfEkrFw== -d 172.0.0.18/32 -j SNAT --to-source 5.6.7.8");
        sb.add("-A POST-hgvHy68+HHdL/uFREcfE1Q== -d 172.0.0.45/32 -j SNAT --to-source 9.10.11.12");
        sb.add("-A POST-sOPwadtSCBogG/2OYDBM0w== -d 172.0.0.2/32 -j SNAT --to-source 1.2.3.4");
        sb.add("COMMIT");
        sb.add("# Completed on Fri Oct 16 18:46:03 2009");
        return sb;
    }

    @Test
    public void shouldSetDropForSubnetAddressRangeWhenRefreshing() {
        // setup
        final String subnetBaseIp1 = "172.30.2.0";
        long baseAddress1 = IpAddressUtils.ipToLong(subnetBaseIp1);
        int addressesInSubnet1 = 16;
        final int mask1 = 16;
        resourceRanges.add(new ResourceRange(baseAddress1, baseAddress1 + IpAddressUtils.addrsInSlashnet(mask1) - 1, addressesInSubnet1));

        final String subnetBaseIp2 = "192.168.224.0";
        long baseAddress2 = IpAddressUtils.ipToLong(subnetBaseIp2);
        int addressesInSubnet2 = 16;
        final int mask2 = 24;
        resourceRanges.add(new ResourceRange(baseAddress2, baseAddress2 + IpAddressUtils.addrsInSlashnet(mask2) - 1, addressesInSubnet2));

        // act
        this.ipTablesManager.refreshIpTables();

        // assert
        verify(commandRunner).runInShell(argThat(new ArgumentMatcher<String>() {
            public boolean matches(Object argument) {
                String arg = (String) argument;
                return (arg.indexOf("*filter") > -1 && arg.contains(String.format("-A pi-chain -d %s/%d -j DROP", subnetBaseIp1, mask1)) && arg.contains(String.format("-A pi-chain -d %s/%d -j DROP", subnetBaseIp2, mask2)));
            }
        }));
    }
}
