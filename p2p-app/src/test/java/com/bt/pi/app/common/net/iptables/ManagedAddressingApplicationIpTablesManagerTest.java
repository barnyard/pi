package com.bt.pi.app.common.net.iptables;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

public class ManagedAddressingApplicationIpTablesManagerTest {
    private static final String PI_APP_TEST = "pi-app-test";
    private String ipAddress;
    private int port;
    private List<String> existingIpTables;
    private CommandResult commandResult;
    private CommandRunner commandRunner;
    private IpTablesHelper ipTablesHelper;

    private ManagedAddressingApplicationIpTablesManager ipTablesManager;

    @Before
    public void setup() {
        setupCommandRunner();
        ipAddress = "1.2.3.4";
        port = 8773;
        ipTablesHelper = new IpTablesHelper();

        ipTablesManager = new ManagedAddressingApplicationIpTablesManager();
        ipTablesManager.setCommandRunner(commandRunner);
        ipTablesManager.setIpTablesHelper(ipTablesHelper);
    }

    private void setupCommandRunner() {
        setupExistingIpTables();

        commandResult = mock(CommandResult.class);
        when(commandResult.getOutputLines()).thenReturn(existingIpTables);

        commandRunner = mock(CommandRunner.class);
        when(commandRunner.run("iptables-save -t filter")).thenReturn(commandResult);
    }

    private void setupExistingIpTables() {
        existingIpTables = new ArrayList<String>();
        existingIpTables.addAll(sampleIptablesFilterOutput());
    }

    @Test
    public void enablePiAppTestChain() throws Exception {
        // setup
        List<String> expectedIpTables = sampleIpTablesWithPiAppTestChain();
        final String expected = String.format("echo '%s' | iptables-restore", collapseList(expectedIpTables));

        // act
        ipTablesManager.enablePiAppChainForApplication(PI_APP_TEST, ipAddress, port);

        // assert
        verify(commandRunner).runInShell(argThat(new ArgumentMatcher<String>() {
            public boolean matches(Object argument) {
                return ((String) argument).equals(expected);
            }
        }));
    }

    @Test
    public void enablePiAppTestChainWithExistingPiAppTestChainHasOnlyOneEntryForChain() throws Exception {
        // setup
        existingIpTables.clear();
        existingIpTables.addAll(sampleIpTablesWithPiAppTestChain());
        List<String> expectedIpTables = sampleIpTablesWithPiAppTestChain();
        final String expected = String.format("echo '%s' | iptables-restore", collapseList(expectedIpTables));

        // act
        ipTablesManager.enablePiAppChainForApplication(PI_APP_TEST, ipAddress, port);

        // assert
        verify(commandRunner).runInShell(argThat(new ArgumentMatcher<String>() {
            public boolean matches(Object argument) {
                return ((String) argument).equals(expected);
            }
        }));
    }

    @Test
    public void disablePiAppTestChainWithExistingPiAppTestChain() throws Exception {
        // setup
        existingIpTables.clear();
        existingIpTables.addAll(sampleIpTablesWithPiAppTestChain());
        List<String> expectedIpTables = sampleIpTablesWithoutPiAppTestChain();
        final String expected = String.format("echo '%s' | iptables-restore", collapseList(expectedIpTables));

        // act
        ipTablesManager.disablePiAppChainForApplication(PI_APP_TEST);

        // assert
        verify(commandRunner).runInShell(argThat(new ArgumentMatcher<String>() {
            public boolean matches(Object argument) {
                return ((String) argument).equals(expected);
            }
        }));
    }

    @Test
    public void disablePiAppTestChainWithoutExistingPiAppTestChain() throws Exception {
        // setup
        List<String> expectedIpTables = sampleIpTablesWithoutPiAppTestChain();
        final String expected = String.format("echo '%s' | iptables-restore", collapseList(expectedIpTables));

        // act
        ipTablesManager.disablePiAppChainForApplication(PI_APP_TEST);

        // assert
        verify(commandRunner).runInShell(argThat(new ArgumentMatcher<String>() {
            public boolean matches(Object argument) {
                return ((String) argument).equals(expected);
            }
        }));
    }

    private List<String> sampleIpTablesWithPiAppTestChain() {
        List<String> expectedIpTables = sampeIpTablesFilterOutputWithoutCommit();
        addPiAppTestChainEntries(expectedIpTables);
        addCommitToSampleIpTables(expectedIpTables);
        return expectedIpTables;
    }

    private List<String> sampleIpTablesWithoutPiAppTestChain() {
        List<String> expectedIpTables = sampeIpTablesFilterOutputWithoutCommit();
        addCommitToSampleIpTables(expectedIpTables);
        return expectedIpTables;
    }

    private void addPiAppTestChainEntries(List<String> expectedIpTables) {
        expectedIpTables.add(expectedIpTables.size(), "-N pi-app-test");
        expectedIpTables.add(expectedIpTables.size(), "-A pi-app -j pi-app-test");
        expectedIpTables.add(expectedIpTables.size(), "-A pi-app-test -d 1.2.3.4 -p tcp -m tcp --dport 8773 -j ACCEPT");
    }

    private String collapseList(List<String> filterExpectation) {
        StringBuilder sb = new StringBuilder();
        for (String s : filterExpectation) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    private List<String> sampleIptablesFilterOutput() {
        List<String> sb = sampeIpTablesFilterOutputWithoutCommit();
        addCommitToSampleIpTables(sb);
        return sb;
    }

    private void addCommitToSampleIpTables(List<String> sb) {
        sb.add("COMMIT");
        sb.add("# Completed on Fri Oct 16 12:31:01 2009");
    }

    private List<String> sampeIpTablesFilterOutputWithoutCommit() {
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
        return sb;
    }
}
