package com.bt.pi.app.common.net.iptables;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.NetworkProtocol;
import com.bt.pi.core.util.common.CommandRunner;

@Component
public class ManagedAddressingApplicationIpTablesManager {
    private static final String LINE_OF_EQUALS = "==================================================================";
    private static final String PI_APP_CHAIN = "pi-app";
    private static final String IPTABLES_SAVE = "iptables-save";
    private static final String IPTABLES_RESTORE = "iptables-restore";
    private static final String FILTER = "filter";
    private static final Log LOG = LogFactory.getLog(ManagedAddressingApplicationIpTablesManager.class);

    private IpTablesHelper ipTablesHelper;
    private CommandRunner commandRunner;

    public ManagedAddressingApplicationIpTablesManager() {
        ipTablesHelper = null;
        commandRunner = null;
    }

    @Resource
    public synchronized void setIpTablesHelper(IpTablesHelper anIpTablesHelper) {
        ipTablesHelper = anIpTablesHelper;
    }

    @Resource
    public void setCommandRunner(CommandRunner aCommandRunner) {
        commandRunner = aCommandRunner;
    }

    public synchronized void enablePiAppChainForApplication(String chainNameForApplication, String appIpAddress, int appPort) {
        List<String> lines = new ArrayList<String>();
        lines.add(ipTablesHelper.addChain(chainNameForApplication));
        lines.add(ipTablesHelper.appendForwardChainToChain(PI_APP_CHAIN, chainNameForApplication));
        lines.add(ipTablesHelper.createOpenPortRuleInChain(chainNameForApplication, NetworkProtocol.TCP, appIpAddress, appPort));

        refreshIpTables(removePiAppEntriesForApplicationFromIpTables(getCurrentFilterIpTables(), chainNameForApplication), lines);
    }

    public synchronized void disablePiAppChainForApplication(String chainNameForApplication) {
        refreshIpTables(removePiAppEntriesForApplicationFromIpTables(getCurrentFilterIpTables(), chainNameForApplication), new ArrayList<String>());
    }

    private List<String> removePiAppEntriesForApplicationFromIpTables(List<String> iptables, String chainNameForApplication) {
        List<String> result = new ArrayList<String>();
        for (String line : iptables)
            if (!line.contains(chainNameForApplication))
                result.add(line);
        return result;
    }

    private List<String> getCurrentFilterIpTables() {
        return commandRunner.run(String.format("%s -t %s", IPTABLES_SAVE, FILTER)).getOutputLines();
    }

    private void refreshIpTables(List<String> existingLines, List<String> newLines) {
        commandRunner.runInShell(String.format("echo '%s'", ipTablesHelper.linesToString(editLines(existingLines, newLines))) + " | " + String.format(IPTABLES_RESTORE));
    }

    private List<String> editLines(List<String> existingLines, List<String> newLines) {
        logLines("before edit", FILTER, existingLines);
        int i = 0;
        for (; i < existingLines.size(); i++)
            if ("COMMIT".equals(existingLines.get(i)))
                break;
        for (int j = newLines.size() - 1; j >= 0; j--)
            existingLines.add(i, newLines.get(j));

        logLines("after edit", FILTER, existingLines);
        return existingLines;
    }

    private void logLines(String where, String tableName, List<String> lines) {
        if (!LOG.isDebugEnabled())
            return;
        LOG.debug(String.format("==== Table %s: %s", tableName, LINE_OF_EQUALS));
        for (int i = 0; i < lines.size(); i++) {
            LOG.debug(String.format("%s (%d): %s", where, i, lines.get(i)));
        }
        LOG.debug(LINE_OF_EQUALS);
    }
}
