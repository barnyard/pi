package com.bt.pi.app.networkmanager.dhcp;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.core.util.template.TemplateHelper;

import freemarker.template.Configuration;

public class DhcpConfigurationGeneratorTest {
    private DhcpConfigurationGenerator dhcpConfigurationGenerator = new DhcpConfigurationGenerator();
    private TemplateHelper templateHelper = new TemplateHelper();
    private List<SecurityGroup> securityGroups;
    private SecurityGroup securityGroup1;
    private String defaultLeaseTime = "1200";
    private String maxLeaseTime = "2400";
    private String mtu = "1492";

    @Before
    public void before() {
        Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(getClass(), "/");
        setField(templateHelper, "freeMarkerConfiguration", configuration);

        this.dhcpConfigurationGenerator.setTemplateHelper(templateHelper);

        dhcpConfigurationGenerator.setDefaultLeaseTime(defaultLeaseTime);
        dhcpConfigurationGenerator.setMaxLeaseTime(maxLeaseTime);
        dhcpConfigurationGenerator.setMtu(mtu);

        securityGroups = new ArrayList<SecurityGroup>();
        securityGroup1 = new SecurityGroup("testuser", "default", 100L, "172.0.0.0", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroups.add(securityGroup1);
        SecurityGroup securityGroup2 = new SecurityGroup("moo", "default", 101L, "172.0.0.16", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroups.add(securityGroup2);

        securityGroup1.getInstances().put("instance-11", new InstanceAddress("172.0.0.2", null, "aa:bb:cc:dd:ee:ff"));
        securityGroup1.getInstances().put("instance-12", new InstanceAddress("172.0.0.3", null, "00:11:22:33:44:55"));

        securityGroup2.getInstances().put("instance-21", new InstanceAddress("172.0.0.18", null, "d0:0d:d0:0d:d0:0d"));
    }

    private void setField(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }

    /**
     * Should generate config string with no security groups
     */
    @Test
    public void shouldGenerateConfigStringFromTemplateWithNoSecGroups() {
        // setup
        StringBuffer buf = new StringBuffer();
        buf.append("# automatically generated config file for DHCP server\n");
        buf.append("default-lease-time " + defaultLeaseTime + ";\n");
        buf.append("max-lease-time " + maxLeaseTime + ";\n");
        buf.append("ddns-update-style none;\n");
        buf.append("\n");
        buf.append("shared-network pi {\n");
        buf.append("}");
        String expectedOutput = buf.toString();

        // act
        String res = dhcpConfigurationGenerator.generate("pi-dhcp.conf.ftl", new ArrayList<SecurityGroup>());

        // assert
        assertEquals(expectedOutput, res);
    }

    /**
     * Should generate config string with security groups
     */
    @Test
    public void shouldGenerateConfigStringFromTemplateWithSecGroups() {
        // setup
        StringBuffer buf = new StringBuffer();
        buf.append("# automatically generated config file for DHCP server\n");
        buf.append("default-lease-time " + defaultLeaseTime + ";\n");
        buf.append("max-lease-time " + maxLeaseTime + ";\n");
        buf.append("ddns-update-style none;\n");
        buf.append("\n");
        buf.append("shared-network pi {\n");
        buf.append("\n");
        buf.append("# security group testuser/default\n");
        buf.append("subnet 172.0.0.0 netmask 255.255.255.240 {\n");
        buf.append("\toption subnet-mask 255.255.255.240;\n");
        buf.append("\toption broadcast-address 172.0.0.15;\n");
        buf.append("\toption domain-name-servers 147.149.2.5;\n");
        buf.append("\toption routers 172.0.0.1;\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-11\n");
        buf.append("host node-172.0.0.2 {\n");
        buf.append("\thardware ethernet aa:bb:cc:dd:ee:ff;\n");
        buf.append("\tfixed-address 172.0.0.2;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-12\n");
        buf.append("host node-172.0.0.3 {\n");
        buf.append("\thardware ethernet 00:11:22:33:44:55;\n");
        buf.append("\tfixed-address 172.0.0.3;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# security group moo/default\n");
        buf.append("subnet 172.0.0.16 netmask 255.255.255.240 {\n");
        buf.append("\toption subnet-mask 255.255.255.240;\n");
        buf.append("\toption broadcast-address 172.0.0.31;\n");
        buf.append("\toption domain-name-servers 147.149.2.5;\n");
        buf.append("\toption routers 172.0.0.17;\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-21\n");
        buf.append("host node-172.0.0.18 {\n");
        buf.append("\thardware ethernet d0:0d:d0:0d:d0:0d;\n");
        buf.append("\tfixed-address 172.0.0.18;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("}");
        String expectedOutput = buf.toString();

        // act
        String res = dhcpConfigurationGenerator.generate("pi-dhcp.conf.ftl", securityGroups);

        // assert
        assertEquals(expectedOutput, res);
    }

    @Test
    public void shouldIgnoreNullInstanceAddressRecords() {
        // setup
        securityGroup1.getInstances().put("fred", null);
        StringBuffer buf = new StringBuffer();
        buf.append("# automatically generated config file for DHCP server\n");
        buf.append("default-lease-time " + defaultLeaseTime + ";\n");
        buf.append("max-lease-time " + maxLeaseTime + ";\n");
        buf.append("ddns-update-style none;\n");
        buf.append("\n");
        buf.append("shared-network pi {\n");
        buf.append("\n");
        buf.append("# security group testuser/default\n");
        buf.append("subnet 172.0.0.0 netmask 255.255.255.240 {\n");
        buf.append("\toption subnet-mask 255.255.255.240;\n");
        buf.append("\toption broadcast-address 172.0.0.15;\n");
        buf.append("\toption domain-name-servers 147.149.2.5;\n");
        buf.append("\toption routers 172.0.0.1;\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-11\n");
        buf.append("host node-172.0.0.2 {\n");
        buf.append("\thardware ethernet aa:bb:cc:dd:ee:ff;\n");
        buf.append("\tfixed-address 172.0.0.2;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-12\n");
        buf.append("host node-172.0.0.3 {\n");
        buf.append("\thardware ethernet 00:11:22:33:44:55;\n");
        buf.append("\tfixed-address 172.0.0.3;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# security group moo/default\n");
        buf.append("subnet 172.0.0.16 netmask 255.255.255.240 {\n");
        buf.append("\toption subnet-mask 255.255.255.240;\n");
        buf.append("\toption broadcast-address 172.0.0.31;\n");
        buf.append("\toption domain-name-servers 147.149.2.5;\n");
        buf.append("\toption routers 172.0.0.17;\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-21\n");
        buf.append("host node-172.0.0.18 {\n");
        buf.append("\thardware ethernet d0:0d:d0:0d:d0:0d;\n");
        buf.append("\tfixed-address 172.0.0.18;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("}");
        String expectedOutput = buf.toString();

        // act
        String res = dhcpConfigurationGenerator.generate("pi-dhcp.conf.ftl", securityGroups);

        // assert
        assertEquals(expectedOutput, res);
    }

    @Test
    public void shouldIgnoreInstanceAddressRecordsWithNoPrivateAddress() {
        // setup
        securityGroup1.getInstances().put("fred", new InstanceAddress(null, null, "oldmacdonald"));
        StringBuffer buf = new StringBuffer();
        buf.append("# automatically generated config file for DHCP server\n");
        buf.append("default-lease-time " + defaultLeaseTime + ";\n");
        buf.append("max-lease-time " + maxLeaseTime + ";\n");
        buf.append("ddns-update-style none;\n");
        buf.append("\n");
        buf.append("shared-network pi {\n");
        buf.append("\n");
        buf.append("# security group testuser/default\n");
        buf.append("subnet 172.0.0.0 netmask 255.255.255.240 {\n");
        buf.append("\toption subnet-mask 255.255.255.240;\n");
        buf.append("\toption broadcast-address 172.0.0.15;\n");
        buf.append("\toption domain-name-servers 147.149.2.5;\n");
        buf.append("\toption routers 172.0.0.1;\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-11\n");
        buf.append("host node-172.0.0.2 {\n");
        buf.append("\thardware ethernet aa:bb:cc:dd:ee:ff;\n");
        buf.append("\tfixed-address 172.0.0.2;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-12\n");
        buf.append("host node-172.0.0.3 {\n");
        buf.append("\thardware ethernet 00:11:22:33:44:55;\n");
        buf.append("\tfixed-address 172.0.0.3;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# security group moo/default\n");
        buf.append("subnet 172.0.0.16 netmask 255.255.255.240 {\n");
        buf.append("\toption subnet-mask 255.255.255.240;\n");
        buf.append("\toption broadcast-address 172.0.0.31;\n");
        buf.append("\toption domain-name-servers 147.149.2.5;\n");
        buf.append("\toption routers 172.0.0.17;\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-21\n");
        buf.append("host node-172.0.0.18 {\n");
        buf.append("\thardware ethernet d0:0d:d0:0d:d0:0d;\n");
        buf.append("\tfixed-address 172.0.0.18;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("}");
        String expectedOutput = buf.toString();

        // act
        String res = dhcpConfigurationGenerator.generate("pi-dhcp.conf.ftl", securityGroups);

        // assert
        assertEquals(expectedOutput, res);
    }

    @Test
    public void shouldIgnoreInstanceAddressRecordsWithNoMacAddress() {
        // setup
        securityGroup1.getInstances().put("fred", new InstanceAddress("1.2.3.4", null, null));
        StringBuffer buf = new StringBuffer();
        buf.append("# automatically generated config file for DHCP server\n");
        buf.append("default-lease-time " + defaultLeaseTime + ";\n");
        buf.append("max-lease-time " + maxLeaseTime + ";\n");
        buf.append("ddns-update-style none;\n");
        buf.append("\n");
        buf.append("shared-network pi {\n");
        buf.append("\n");
        buf.append("# security group testuser/default\n");
        buf.append("subnet 172.0.0.0 netmask 255.255.255.240 {\n");
        buf.append("\toption subnet-mask 255.255.255.240;\n");
        buf.append("\toption broadcast-address 172.0.0.15;\n");
        buf.append("\toption domain-name-servers 147.149.2.5;\n");
        buf.append("\toption routers 172.0.0.1;\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-11\n");
        buf.append("host node-172.0.0.2 {\n");
        buf.append("\thardware ethernet aa:bb:cc:dd:ee:ff;\n");
        buf.append("\tfixed-address 172.0.0.2;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-12\n");
        buf.append("host node-172.0.0.3 {\n");
        buf.append("\thardware ethernet 00:11:22:33:44:55;\n");
        buf.append("\tfixed-address 172.0.0.3;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# security group moo/default\n");
        buf.append("subnet 172.0.0.16 netmask 255.255.255.240 {\n");
        buf.append("\toption subnet-mask 255.255.255.240;\n");
        buf.append("\toption broadcast-address 172.0.0.31;\n");
        buf.append("\toption domain-name-servers 147.149.2.5;\n");
        buf.append("\toption routers 172.0.0.17;\n");
        buf.append("}\n");
        buf.append("\n");
        buf.append("# instance instance-21\n");
        buf.append("host node-172.0.0.18 {\n");
        buf.append("\thardware ethernet d0:0d:d0:0d:d0:0d;\n");
        buf.append("\tfixed-address 172.0.0.18;\n");
        buf.append("\toption interface-mtu " + mtu + ";\n");
        buf.append("}\n");
        buf.append("}");
        String expectedOutput = buf.toString();

        // act
        String res = dhcpConfigurationGenerator.generate("pi-dhcp.conf.ftl", securityGroups);

        // assert
        assertEquals(expectedOutput, res);
    }

    @Test
    public void shouldIgnoreSecurityGroupWithNoNetworkAddress() {
        // setup
        securityGroups = new ArrayList<SecurityGroup>();
        securityGroups.add(securityGroup1);
        securityGroup1.setNetworkAddress(null);
        StringBuffer buf = new StringBuffer();
        buf.append("# automatically generated config file for DHCP server\n");
        buf.append("default-lease-time " + defaultLeaseTime + ";\n");
        buf.append("max-lease-time " + maxLeaseTime + ";\n");
        buf.append("ddns-update-style none;\n");
        buf.append("\n");
        buf.append("shared-network pi {\n");
        buf.append("\n");
        buf.append("}");
        String expectedOutput = buf.toString();

        // act
        String res = dhcpConfigurationGenerator.generate("pi-dhcp.conf.ftl", securityGroups);

        // assert
        assertEquals(expectedOutput, res);
    }
}
