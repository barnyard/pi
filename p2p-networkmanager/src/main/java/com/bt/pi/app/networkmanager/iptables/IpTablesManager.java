/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.iptables;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.iptables.IpTablesHelper;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.util.common.CommandRunner;

@Component
public class IpTablesManager implements ApplicationListener<NodeStartedEvent> {
    protected static final String PI_CHAIN = "pi-chain";
    protected static final String PI_CHAIN_FORWARD = "-A FORWARD -j " + PI_CHAIN;
    protected static final String PI_CHAIN_DEFINITION = ":" + PI_CHAIN + " - [0:0]";
    protected static final String PI_CHAIN_CREATE = "-N " + PI_CHAIN;
    private static final String FILTER = "filter";
    private static final String NAT = "nat";
    private static final Log LOG = LogFactory.getLog(IpTablesManager.class);
    private static final String LINE_OF_EQUALS = "==================================================================";
    private IpTablesBuilder ipTablesBuilder;
    private IpTablesHelper ipTablesHelper;
    private String iptablesSaveCommand;
    private String iptablesRestoreCommand;
    private CommandRunner commandRunner;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private PiIdBuilder piIdBuilder;
    private DhtCache dhtCache;
    private AtomicBoolean nodeStarted;

    public IpTablesManager() {
        this.ipTablesBuilder = null;
        this.ipTablesHelper = null;
        this.commandRunner = null;
        this.iptablesSaveCommand = "iptables-save";
        this.iptablesRestoreCommand = "iptables-restore";
        this.consumedDhtResourceRegistry = null;
        this.piIdBuilder = null;
        this.dhtCache = null;
        this.nodeStarted = new AtomicBoolean(false);
    }

    @Resource
    public synchronized void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public synchronized void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    @Resource
    public synchronized void setIpTablesBuilder(IpTablesBuilder aIpTablesBuilder) {
        this.ipTablesBuilder = aIpTablesBuilder;
    }

    @Resource
    public synchronized void setIpTablesHelper(IpTablesHelper aIpTablesHelper) {
        this.ipTablesHelper = aIpTablesHelper;
    }

    @Resource
    public synchronized void setCommandRunner(CommandRunner runner) {
        this.commandRunner = runner;
    }

    @Resource(name = "generalCache")
    public synchronized void setDhtCache(DhtCache aDhtCache) {
        this.dhtCache = aDhtCache;
    }

    public synchronized void refreshIpTables() {
        List<SecurityGroup> securityGroups = (List<SecurityGroup>) consumedDhtResourceRegistry.getByType(SecurityGroup.class);
        LOG.info(String.format("Refreshing iptables with %d group(s)", securityGroups.size()));

        final List<String> iptablesSaveNatLines = getLines(NAT);
        final List<String> iptablesSaveFilterLines = getLines(FILTER);

        final List<String> nonPiIptablesNatLines = ipTablesBuilder.removePiChainsAndRules(iptablesSaveNatLines);
        final List<String> nonPiIptablesFilterLines = ipTablesBuilder.removePiChainsAndRules(iptablesSaveFilterLines, Arrays.asList(new String[] { PI_CHAIN_DEFINITION, PI_CHAIN_FORWARD }));

        final List<String> natRules = ipTablesBuilder.generateNatTable(securityGroups, Arrays.asList(new String[0]));
        applyRules(NAT, natRules, nonPiIptablesNatLines);

        final List<String> filterRules = ipTablesBuilder.generateFilterTable(securityGroups, Arrays.asList(new String[0]));
        if (!nonPiIptablesFilterLines.contains(PI_CHAIN_FORWARD))
            filterRules.add(0, PI_CHAIN_FORWARD);
        if (!nonPiIptablesFilterLines.contains(PI_CHAIN_DEFINITION))
            filterRules.add(0, PI_CHAIN_CREATE);

        dhtCache.get(piIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion(), new PiContinuation<SubnetAllocationIndex>() {
            @Override
            public void handleResult(SubnetAllocationIndex subnetAllocationIndex) {
                if (null != subnetAllocationIndex) {
                    Set<ResourceRange> resourceRanges = subnetAllocationIndex.getResourceRanges();
                    for (ResourceRange rr : resourceRanges) {
                        long minBaseIpAddressAsLong = rr.getMin();
                        String ipAddress = IpAddressUtils.longToIp(minBaseIpAddressAsLong);
                        long maxBaseIpAddressAsLong = rr.getMax();
                        long numAddrs = maxBaseIpAddressAsLong - minBaseIpAddressAsLong + 1;
                        String rule = String.format("-A %s -d %s/%d -j DROP", PI_CHAIN, ipAddress, IpAddressUtils.slashnetFromAddrs(numAddrs));
                        filterRules.add(rule);
                    }
                } else {
                    LOG.warn(String.format("Cached subnet allocation index record was not found by ip tables manager"));
                }
                applyRules(FILTER, filterRules, nonPiIptablesFilterLines);
            }
        });
    }

    private synchronized void applyRules(String tableName, List<String> rules, List<String> existingLines) {
        LOG.debug(String.format("applyRules(%s, %s, %s)", tableName, rules, existingLines));
        logLines("before edit", tableName, existingLines);
        int i = 0;
        for (String line : existingLines) {
            if ("COMMIT".equals(line)) {
                break;
            }
            i++;
        }

        for (int j = rules.size() - 1; j >= 0; j--)
            existingLines.add(i, rules.get(j));

        logLines("after edit", tableName, existingLines);
        commandRunner.runInShell(String.format("echo '%s'", ipTablesHelper.linesToString(existingLines)) + " | " + String.format(iptablesRestoreCommand));
    }

    protected synchronized List<String> getLines(String section) {
        LOG.debug(String.format("getLines(%s)", section));
        return this.commandRunner.run(iptablesSaveCommand + " -t " + section).getOutputLines();
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

    @Override
    public void onApplicationEvent(NodeStartedEvent event) {
        LOG.debug(String.format("onApplicationEvent(%s)", event));
        nodeStarted.set(true);
    }
}
