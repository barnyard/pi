package com.bt.pi.app.common.net.iptables;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.NetworkProtocol;

public class IpTablesHelperTest {
    private String fromChain;
    private String toChain;
    private String destinationNetwork;
    private String ipAddress;
    private int port;
    private List<String> lines;

    private IpTablesHelper ipTablesHelper;

    @Before
    public void setup() {
        fromChain = "from-chain";
        toChain = "to-chain";
        destinationNetwork = "dest";
        ipAddress = "1.2.3.4";
        port = 2000;
        lines = new ArrayList<String>();

        ipTablesHelper = new IpTablesHelper();
    }

    @Test
    public void testLinesToString() throws Exception {
        // setup
        setupLinesWithData(20);

        // act
        String result = ipTablesHelper.linesToLogFriendlyString(lines);

        // assert
        assertThatResultContainsLines(result, 20);
    }

    private void setupLinesWithData(int number) {
        for (int i = 0; i < number; i++)
            lines.add(String.format("Line number %d", i));
    }

    private void assertThatResultContainsLines(String result, int number) {
        String[] lines = result.split("\n");
        assertThat(lines.length, equalTo(number));
        for (int i = 0; i < lines.length; i++)
            assertThat(lines[i], equalTo(String.format("\tLine number %d", i)));
    }

    @Test
    public void testAddChain() throws Exception {
        // act
        String result = ipTablesHelper.addChain(fromChain);

        // assert
        assertThat(result, equalTo(String.format("-N %s", fromChain)));
    }

    @Test
    public void testGenerateRemoveChainRule() throws Exception {
        // act
        String result = ipTablesHelper.generateRemoveChainRule(fromChain);

        // assert
        assertThat(result, equalTo(String.format("-X %s", fromChain)));
    }

    @Test
    public void testAppendForwardChainToChain() throws Exception {
        // act
        String result = ipTablesHelper.appendForwardChainToChain(fromChain, toChain);

        // assert
        assertThat(result, equalTo(String.format("-A %s -j %s", fromChain, toChain)));
    }

    @Test
    public void testAppendForwardChainToChainWithDestinationNetwork() throws Exception {
        // act
        String result = ipTablesHelper.appendForwardChainToChain(fromChain, destinationNetwork, toChain);

        // assert
        assertThat(result, equalTo(String.format("-A %s -d %s -j %s", fromChain, destinationNetwork, toChain)));
    }

    @Test
    public void testInsertForwardChainToChain() throws Exception {
        // act
        String result = ipTablesHelper.insertChainForwardToChain(fromChain, toChain);

        // assert
        assertThat(result, equalTo(String.format("-I %s -j %s", fromChain, toChain)));
    }

    @Test
    public void testInsertForwardChainToChainWithDestinationNetwork() throws Exception {
        // act
        String result = ipTablesHelper.insertChainForwardToChain(fromChain, destinationNetwork, toChain);

        // assert
        assertThat(result, equalTo(String.format("-I %s -s %s -j %s", fromChain, destinationNetwork, toChain)));
    }

    @Test
    public void testOpenPortRule() throws Exception {
        // act
        String result = ipTablesHelper.createOpenPortRuleInChain(toChain, NetworkProtocol.TCP, ipAddress, port);

        // assert
        assertThat(result, equalTo(String.format("-A %s -d %s -p tcp -m tcp --dport %d -j ACCEPT", toChain, ipAddress, port)));
    }

    @Test
    public void testRemoveChainForwardFromChain() throws Exception {
        // act
        String result = ipTablesHelper.removeChainForwardFromChain(fromChain, toChain);

        // assert
        assertThat(result, equalTo(String.format("-D %s -j %s", fromChain, toChain)));
    }

    @Test
    public void testGenerateFlushChainRule() throws Exception {
        // act
        String result = ipTablesHelper.generateFlushChainRule(fromChain);

        // assert
        assertThat(result, equalTo(String.format("-F %s", fromChain)));
    }
}
