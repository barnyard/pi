/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.iptables;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.p2p.util.Base64;

import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.NetworkProtocol;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.net.iptables.IpTablesHelper;

@Component
public class IpTablesBuilder {
    private static final String SKIPPING_SECURITY_GROUP_S_AS_IT_IS_NOT_POPULATED = "Skipping Security group: %s as it is not populated";
    private static final String A_S_D_S_J_DNAT_TO_S = "-A %s -d %s -j DNAT --to %s";
    private static final String OUTPUT = "OUTPUT";
    private static final String PREROUTING = "PREROUTING";
    private static final String DASH_A = "-A";
    private static final String S_S = "%s%s";
    private static final String PI_PREROUTING = "PI-PREROUTING";
    private static final String PI_OUTPUT = "PI-OUTPUT";
    private static final String POST_PREFIX = "POST-";
    private static final String FLTR_PREFIX = "FLTR-";
    private static final String S_SLASH_S = "%s/%s";
    private static final String POSTROUTING = "POSTROUTING";
    private static final String PI_CHAIN = "pi-chain";
    private static final Log LOG = LogFactory.getLog(IpTablesBuilder.class);
    private List<String> lines;

    private IpTablesHelper ipTablesHelper;

    public IpTablesBuilder() {
        lines = null;
        ipTablesHelper = null;
    }

    @Resource
    public void setIpTablesHelper(IpTablesHelper anIpTablesHelper) {
        ipTablesHelper = anIpTablesHelper;
    }

    public synchronized List<String> generateFilterTable(List<SecurityGroup> securityGroups, List<String> existingChains) {

        LOG.debug(String.format("generateFilterTable(%s, %s)", securityGroups, existingChains));
        lines = new ArrayList<String>();

        // if (!existingChains.contains(PI_CHAIN))
        // addChain(PI_CHAIN);
        // addForwardAllToChain(PI_CHAIN);

        for (SecurityGroup securityGroup : securityGroups) {
            if (!validateSecurityGroup(securityGroup)) {
                LOG.debug(String.format(SKIPPING_SECURITY_GROUP_S_AS_IT_IS_NOT_POPULATED, securityGroup));
                continue;
            }

            LOG.debug(String.format("Processing Security group: %s for filter rules.", securityGroup));
            String filterChainName = getChainNameForSecurityGroup(FLTR_PREFIX, securityGroup.getSecurityGroupId());
            String destinationNetwork = String.format(S_SLASH_S, securityGroup.getNetworkAddress(), securityGroup.getSlashnet());

            if (!existingChains.contains(filterChainName))
                lines.add(ipTablesHelper.addChain(filterChainName));

            lines.add(ipTablesHelper.appendForwardChainToChain(PI_CHAIN, destinationNetwork, filterChainName));

            // TODO: Handle case where rule name specificed instead of network

            for (NetworkRule networkRule : securityGroup.getNetworkRules()) {
                for (String sourceNetwork : networkRule.getSourceNetworks()) {
                    addFilterRule(filterChainName, sourceNetwork, destinationNetwork, networkRule.getNetworkProtocol(), networkRule.getPortRangeMin(), networkRule.getPortRangeMax());
                }
            }
        }
        logFlushRules("Generated filter iptables:\n%s", lines);
        return lines;
    }

    public synchronized List<String> generateNatTable(List<SecurityGroup> securityGroups, List<String> existingChains) {

        LOG.debug(String.format("generateNatTable(%s, %s)", securityGroups, existingChains));
        lines = new ArrayList<String>();

        if (!existingChains.contains(PI_PREROUTING)) {
            lines.add(ipTablesHelper.addChain(PI_PREROUTING));
            lines.add(ipTablesHelper.appendForwardChainToChain(PREROUTING, PI_PREROUTING));
        }
        if (!existingChains.contains(PI_OUTPUT)) {
            lines.add(ipTablesHelper.addChain(PI_OUTPUT));
            lines.add(ipTablesHelper.appendForwardChainToChain(OUTPUT, PI_OUTPUT));
        }

        for (SecurityGroup securityGroup : securityGroups) {
            if (!validateSecurityGroup(securityGroup)) {
                LOG.debug(String.format(SKIPPING_SECURITY_GROUP_S_AS_IT_IS_NOT_POPULATED, securityGroup));
                continue;
            }
            LOG.debug(String.format("Processing Security group: %s for nat rules.", securityGroup));
            String postChainName = getChainNameForSecurityGroup(POST_PREFIX, securityGroup.getSecurityGroupId());
            String destinationNetwork = String.format(S_SLASH_S, securityGroup.getNetworkAddress(), securityGroup.getSlashnet());

            if (!existingChains.contains(postChainName))
                lines.add(ipTablesHelper.addChain(postChainName));

            lines.add(ipTablesHelper.insertChainForwardToChain(POSTROUTING, destinationNetwork, postChainName));

            // TODO: Handle case where rule name specified instead of network

            for (Entry<String, InstanceAddress> addressEntry : securityGroup.getInstances().entrySet()) {
                LOG.debug(String.format("Processing addressEntry: %s for nat rules.", addressEntry));
                InstanceAddress address = addressEntry.getValue();
                if (null == address) {
                    LOG.warn(String.format("null entry in instance table for instance %s", addressEntry.getKey()));
                    continue;
                }
                if (address.getPublicIpAddress() != null && address.getPrivateIpAddress() != null) {
                    addNatRule(postChainName, address.getPublicIpAddress(), address.getPrivateIpAddress());
                } else {
                    LOG.debug(String.format("Skipping NAT rule within group %s as one or both addresses are null (%s / %s)", securityGroup.getSecurityGroupId(), address.getPublicIpAddress(), address.getPrivateIpAddress()));
                }
            }
        }
        logFlushRules("Generated nat iptables:\n%s", lines);

        return lines;
    }

    private boolean validateSecurityGroup(SecurityGroup securityGroup) {
        if (null == securityGroup.getNetmask() || null == securityGroup.getVlanId())
            return false;

        return true;
    }

    protected String getChainNameForSecurityGroup(String prefix, String securityGroupId) {
        return String.format(S_S, prefix, getMd5DigestHex(securityGroupId));
    }

    public List<String> generateFilterFlushRules(String[] chainNames, List<String> newChains) {
        LOG.debug(String.format("generateFilterFlushRules(%s, %s)", chainNames, newChains));
        List<String> res = new ArrayList<String>();
        for (String chainName : chainNames) {
            if (chainName.equals(PI_CHAIN)) {
                res.add("-D FORWARD -j " + PI_CHAIN);
                res.add(ipTablesHelper.generateFlushChainRule(chainName));
            } else if (chainName.startsWith(FLTR_PREFIX)) {
                res.add(ipTablesHelper.generateFlushChainRule(chainName));
            } else {
                LOG.debug(String.format("Skipping non-filter chain name: %s", chainName));
            }
        }
        for (String chainName : chainNames) {
            if (!newChains.contains(chainName) && chainName.startsWith(FLTR_PREFIX)) {
                res.add(ipTablesHelper.generateRemoveChainRule(chainName));
            }
        }
        logFlushRules("Generated chain flush rules:\n%s", res);
        return res;
    }

    public List<String> generateNatFlushRules(String[] chainNames, String[] existingPiRules, List<String> newChains) {
        LOG.debug(String.format("generateNatFlushRules(%s, %s, %s)", chainNames, existingPiRules, newChains));
        List<String> res = new ArrayList<String>();
        for (String rule : existingPiRules) {
            boolean isNatRule = rule.contains("SNAT") || rule.contains("DNAT");
            boolean isPostroutingRule = rule.contains(POSTROUTING) && rule.contains(POST_PREFIX);
            if (rule.startsWith(DASH_A) && (isNatRule || isPostroutingRule)) {
                res.add(rule.replaceFirst(DASH_A, "-D"));
            }
        }

        for (String chainName : chainNames) {
            if (chainName.startsWith(POST_PREFIX)) {
                res.add(ipTablesHelper.generateFlushChainRule(chainName));
                // NOTE: for the moment, we comment this out as it causes issues when run within a single commit.
                // The 'right' way to do this is to rewrite all our chains and rules from scratch, rather than
                // incrementally
                // if (!newChains.contains(chainName))
                // res.add(generateRemoveChainRule(chainName));
            } else {
                LOG.debug(String.format("Skipping non-nat chain name: %s", chainName));
            }
        }
        logFlushRules("Generated nat flush rules:\n%s", res);
        return res;
    }

    private void logFlushRules(String textString, List<String> res) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format(textString, ipTablesHelper.linesToLogFriendlyString(res)));
    }

    protected String getMd5DigestHex(String message) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        messageDigest.reset();
        messageDigest.update(message.getBytes());
        byte[] messageDigestBytes = messageDigest.digest();

        return Base64.encodeBytes(messageDigestBytes);
    }

    // private void addForwardAllToChain(String toChain) {
    // lines.add(String.format("-A FORWARD -j %s", toChain));
    // }

    protected void addFilterRule(String destChainName, String sourceNetwork, String destinationNetwork, NetworkProtocol networkProtocol, int portRangeMin, int portRangeMax) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("-A %s", destChainName));
        sb.append(String.format(" -s %s -d %s", sourceNetwork, destinationNetwork));

        if (networkProtocol != null)
            sb.append(String.format(" -p %s", networkProtocol.toString().toLowerCase(Locale.US)));

        if (portRangeMin > 0 && portRangeMax > 0) {
            if (NetworkProtocol.TCP.equals(networkProtocol) || NetworkProtocol.UDP.equals(networkProtocol)) {
                sb.append(String.format(" --dport %d:%d", portRangeMin, portRangeMax));
            }
        }

        sb.append(" -j ACCEPT");
        lines.add(sb.toString());
    }

    private void addNatRule(String postRoutingChainName, String publicAddress, String privateAddress) {
        lines.add(String.format(A_S_D_S_J_DNAT_TO_S, PI_PREROUTING, publicAddress, privateAddress));
        lines.add(String.format(A_S_D_S_J_DNAT_TO_S, PI_OUTPUT, publicAddress, privateAddress));
        lines.add(String.format("-A %s -s %s -j SNAT --to %s", postRoutingChainName, privateAddress, publicAddress));
    }

    protected List<String> removePiChainsAndRules(List<String> rules) {
        LOG.debug(String.format("removePiChainsAndRules(%s)", rules));
        return removePiChainsAndRules(rules, new ArrayList<String>());
    }

    protected List<String> removePiChainsAndRules(List<String> rules, List<String> exceptions) {
        LOG.debug(String.format("removePiChainsAndRules(%s, %s)", rules, exceptions));
        List<String> result = new ArrayList<String>();
        for (String rule : rules) {
            boolean isFilterPiChainRule = rule.contains(PI_CHAIN) || rule.contains(FLTR_PREFIX);
            boolean isNatPiChainRule = rule.contains(PI_PREROUTING) || rule.contains(POST_PREFIX) || rule.contains(PI_OUTPUT);
            if (exceptions.contains(rule) || (!isNatPiChainRule && !isFilterPiChainRule)) {
                result.add(rule);
            }
        }
        return result;
    }
}
