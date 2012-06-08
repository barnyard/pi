package com.bt.pi.app.instancemanager.libvirt;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import com.bt.pi.core.util.template.TemplateHelper;

import freemarker.template.Configuration;

public class LibvirtTemplateGeneratorTest {
    private LibvirtTemplateGenerator libvirtTemplateGenerator = new LibvirtTemplateGenerator();
    private Map<String, Object> model;
    private TemplateHelper templateHelper = new TemplateHelper();

    @Before
    public void setup() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(getClass(), "/");
        setField(templateHelper, "freeMarkerConfiguration", configuration);

        libvirtTemplateGenerator.setTemplateHelper(templateHelper);
    }

    private void setField(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }

    @Before
    public void setupModel() {
        model = new HashMap<String, Object>();
        model.put("NAME", "test");
        model.put("BASEPATH", "/root");
        model.put("MEMORY", "128m");
        model.put("VCPUS", "2");
        model.put("BRIDGEDEV", "virbr0");
        model.put("PRIVMACADDR", "127.0.0.1");
    }

    private void setupLinuxModel() {
        model.put("TYPE", "linux");
        model.put("OS_TYPE", "linux");
        model.put("XEN_ID", "18");
        model.put("SWAPPATH", "/swap");
        model.put("use_ramdisk", "true");
        model.put("use_ephemeral", "false");
        model.put("KERNEL_ID", "kernelId");
        model.put("IMAGE_ID", "imageId");
        model.put("RAMDISK_ID", "ramdiskId");
    }

    private void setupWindowsModel() {
        model.put("use_ephemeral", "true");
        model.put("TYPE", "windows");
        model.put("OS_TYPE", "hvm");
        model.put("XEN_ID", "16");
        model.put("IMAGE_ID", "imageId");
    }

    private void setupOpenSolarisModel() {
        setupLinuxModel();
        model.put("TYPE", "opensolaris");
        model.remove("SWAPPATH");
        model.put("use_ephemeral", "true");
    }

    @Ignore
    @Test
    public void testWindowsLibvirtXmlForFailingParameters() throws Exception {

        model = new HashMap<String, Object>();
        model.put("BRIDGEDEV", "pibr101");
        model.put("VCPUS", "1");
        model.put("OS_TYPE", "hvm");
        model.put("NAME", "i-2599AE7E");
        model.put("MEMORY", "524288");
        model.put("use_ephemeral", "true");
        model.put("BASEPATH", "/opt/pi/var/instances/koala/i-2599AE7E");
        model.put("PRIVMACADDR", "d0:0d:25:99:AE:7E");
        model.put("XEN_ID", "16");
        model.put("TYPE", "windows");
        String buildXml = libvirtTemplateGenerator.buildXml(model);

        System.out.println(buildXml);
    }

    @Test
    public void testLinuxLibvirtXml() throws Exception {
        // setup
        setupLinuxModel();
        String expected = "<domain type='xen' id='18'><name>test</name><os><type>linux</type><kernel>/root/kernelId</kernel><initrd>/root/ramdiskId</initrd>"
                + "<root>/dev/sda1</root><cmdline> ro</cmdline></os><memory>128m</memory><vcpu>2</vcpu><on_crash>preserve</on_crash><devices><disk type='file'><driver name='tap' type='aio' cache='default' /><source file='/root/imageId'/>"
                + "<target dev='sda1'/></disk><disk type='file'><driver name='tap' type='aio' cache='default' /><source file='/swap/swap'/><target dev='sda3'/></disk><interface type='bridge'>"
                + "<source bridge='virbr0'/><mac address='127.0.0.1'/><script path='/etc/xen/scripts/vif-bridge'/></interface></devices></domain>";

        // act
        String result = libvirtTemplateGenerator.buildXml(model);

        // assert
        assertThat(formatResult(result), equalTo(expected));
    }

    @Test
    public void testLinuxLibvirtXmlWithEphemeralDisk() throws Exception {
        setupLinuxModel();
        model.put("use_ephemeral", "true");

        String expected = "<domain type='xen' id='18'><name>test</name><os><type>linux</type><kernel>/root/kernelId</kernel><initrd>/root/ramdiskId</initrd>"
                + "<root>/dev/sda1</root><cmdline> ro</cmdline></os><memory>128m</memory><vcpu>2</vcpu><on_crash>preserve</on_crash><devices><disk type='file'><driver name='tap' type='aio' cache='default' /><source file='/root/imageId'/>"
                + "<target dev='sda1'/></disk><disk type='file'><driver name='tap' type='aio' cache='default' /><source file='/root/ephemeral'/><target dev='sda2'/></disk>"
                + "<disk type='file'><driver name='tap' type='aio' cache='default' /><source file='/swap/swap'/><target dev='sda3'/></disk><interface type='bridge'>"
                + "<source bridge='virbr0'/><mac address='127.0.0.1'/><script path='/etc/xen/scripts/vif-bridge'/></interface></devices></domain>";

        // act
        String result = libvirtTemplateGenerator.buildXml(model);

        // assert
        assertEquals(expected, formatResult(result));
    }

    @Test
    public void testWindowsLibvirtXml() throws Exception {
        // setup
        setupWindowsModel();
        String expected = "<domain type='xen' id='16'><name>test</name><os><type>hvm</type><loader>/usr/lib/xen/boot/hvmloader</loader><boot dev='hd'/>"
                + "</os><memory>128m</memory><vcpu>2</vcpu><on_crash>preserve</on_crash><on_poweroff>destroy</on_poweroff><on_reboot>restart</on_reboot><on_crash>preserve</on_crash><features>"
                + "<acpi/><apic/><pae/></features><clock offset='utc'/><devices><emulator>/usr/lib64/xen/bin/qemu-dm</emulator><interface type='bridge'>"
                + "<source bridge='virbr0'/><mac address='127.0.0.1'/><script path='/etc/xen/scripts/vif-bridge'/></interface><disk type='file' device='disk'>"
                + "<driver name='tap' type='aio' cache='default' /><source file='/root/imageId'/><target dev='hda'/></disk>"
                + "<disk type='file'><driver name='tap' type='aio' cache='default' /><source file='/root/ephemeral'/><target dev='hdb'/></disk>" + "<input type='tablet' bus='usb'/><input type='mouse' bus='ps2'/>"
                + "<graphics type='vnc' listen='0.0.0.0'/><console tty='/dev/pts/0'/></devices></domain>";

        // act
        String result = libvirtTemplateGenerator.buildXml(model);

        // assert
        assertEquals(expected, formatResult(result));
    }

    @Test
    public void testOpenSolarisLibvirtXml() throws Exception {
        // setup
        setupOpenSolarisModel();
        String expected = "<domain type='xen' id='18'><name>test</name><os><type>linux</type><kernel>/root/kernelId</kernel><initrd>/root/ramdiskId</initrd>"
                + "<cmdline>/platform/i86xpv/kernel/amd64/unix -v -B zfs-bootfs=rpool/ROOT/opensolaris,bootpath=/xpvd/xdf\\@51712:a</cmdline></os><memory>"
                + "128m</memory><vcpu>2</vcpu><on_crash>preserve</on_crash><devices><disk type='file'><driver name='tap' type='aio' cache='default' /><source file='/root/imageId'/><target dev='xvda'/></disk><disk type='file'>"
                + "<driver name='tap' type='aio' cache='default' /><source file='/root/ephemeral'/><target dev='xvdb'/></disk><interface type='bridge'><source bridge='virbr0'/><mac address='127.0.0.1'/>"
                + "<script path='/etc/xen/scripts/vif-bridge'/></interface></devices></domain>";

        // act
        String result = libvirtTemplateGenerator.buildXml(model);

        // assert
        assertThat(formatResult(result), equalTo(expected));
    }

    private String formatResult(String result) {
        return result.replaceAll(">[\\s]+<", "><").trim();
    }
}
