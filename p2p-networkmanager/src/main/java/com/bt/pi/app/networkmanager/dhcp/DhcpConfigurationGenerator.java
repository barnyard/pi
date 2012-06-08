package com.bt.pi.app.networkmanager.dhcp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.util.template.TemplateHelper;

@Component
public class DhcpConfigurationGenerator {
    private static final String TWO_THOUSAND_FOUR_HUNDRED = "2400";
    private static final String DEFAULT_DEFAULT_LEASE_TIME = TWO_THOUSAND_FOUR_HUNDRED;
    private static final String DEFAULT_MAX_LEASE_TIME = TWO_THOUSAND_FOUR_HUNDRED;
    private static final Log LOG = LogFactory.getLog(DhcpConfigurationGenerator.class);
    private static final String DEFAULT_MTU = "1492";
    private String defaultLeaseTime = DEFAULT_DEFAULT_LEASE_TIME;
    private String maxLeaseTime = DEFAULT_MAX_LEASE_TIME;
    private String mtu = DEFAULT_MTU;
    private TemplateHelper templateHelper;

    public DhcpConfigurationGenerator() {
        templateHelper = null;
    }

    @Resource
    public void setTemplateHelper(TemplateHelper aTemplateHelper) {
        this.templateHelper = aTemplateHelper;
    }

    @Property(key = "dhcp.default.lease.time", defaultValue = DEFAULT_DEFAULT_LEASE_TIME)
    public void setDefaultLeaseTime(String value) {
        this.defaultLeaseTime = value;
    }

    @Property(key = "dhcp.max.lease.time", defaultValue = DEFAULT_MAX_LEASE_TIME)
    public void setMaxLeaseTime(String value) {
        this.maxLeaseTime = value;
    }

    public String generate(String templateFilePath, Collection<SecurityGroup> securityGroups) {
        LOG.debug(String.format("Generating dhcp config from %s for %d sec groups", templateFilePath, securityGroups.size()));

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("securityGroups", securityGroups);
        model.put("defaultLeaseTime", defaultLeaseTime);
        model.put("maxLeaseTime", maxLeaseTime);
        model.put("mtu", mtu);

        return templateHelper.generate(templateFilePath, model);
    }

    @Property(key = "dhcp.mtu", defaultValue = DEFAULT_MTU)
    public void setMtu(String value) {
        this.mtu = value;
    }
}
