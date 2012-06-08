package com.bt.pi.app.common.net.iptables;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.NetworkProtocol;

@Component
public class IpTablesHelper {
    public IpTablesHelper() {
    }

    public String linesToLogFriendlyString(List<String> lines) {
        return linesToString(lines, "\t%s\n");
    }

    public String linesToString(List<String> lines) {
        return linesToString(lines, "%s\n");
    }

    private String linesToString(List<String> lines, String format) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines)
            sb.append(String.format(format, s));
        return sb.toString();
    }

    public String addChain(String chainName) {
        return String.format("-N %s", chainName);
    }

    public String generateRemoveChainRule(String chainName) {
        return String.format("-X %s", chainName);
    }

    public String appendForwardChainToChain(String fromChain, String toChain) {
        return String.format("-A %s -j %s", fromChain, toChain);
    }

    public String appendForwardChainToChain(String fromChain, String destinationNetwork, String toChain) {
        return String.format("-A %s -d %s -j %s", fromChain, destinationNetwork, toChain);
    }

    public String insertChainForwardToChain(String fromChain, String toChain) {
        return String.format("-I %s -j %s", fromChain, toChain);
    }

    public String insertChainForwardToChain(String fromChain, String destinationNetwork, String toChain) {
        return String.format("-I %s -s %s -j %s", fromChain, destinationNetwork, toChain);
    }

    public String createOpenPortRuleInChain(String chainName, NetworkProtocol protocol, String ipAddress, int port) {
        String prot = protocol.toString().toLowerCase(Locale.ENGLISH);
        return String.format("-A %s -d %s -p %s -m %s --dport %d -j ACCEPT", chainName, ipAddress, prot, prot, port);
    }

    public String removeChainForwardFromChain(String fromChain, String toChain) {
        return String.format("-D %s -j %s", fromChain, toChain);
    }

    public String generateFlushChainRule(String chainName) {
        return String.format("-F %s", chainName);
    }
}
