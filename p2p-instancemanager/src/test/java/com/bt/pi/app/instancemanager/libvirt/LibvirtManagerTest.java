package com.bt.pi.app.instancemanager.libvirt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.os.FileManager;
import com.bt.pi.core.parser.KoalaJsonParser;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtManagerTest {
    private static final String INSTANCE_ID = "i-123";
    @InjectMocks
    private LibvirtManager libvirtManager = new LibvirtManager();
    @Mock
    private LibvirtException mockLibvirtException;
    @Mock
    private Connection mockConnection;
    @Mock
    private LibvirtTemplateGenerator mockLibvirtTemplateGenerator;
    @Mock
    private FileManager mockFileManager;
    @Mock
    private KoalaJsonParser mockKoalaJsonParser;
    private Instance instance = new Instance();
    @Mock
    private Domain mockDomain;

    @Before
    public void setupInstance() {
        instance.setInstanceId(INSTANCE_ID);
        instance.setUserId("koala");
        instance.setKernelId("kernelId");
        instance.setImageId("imageId");
        instance.setRamdiskId("ramdiskId");
    }

    @Before
    public void setupLibvirtException() {
        when(mockLibvirtException.fillInStackTrace()).thenReturn(mockLibvirtException);
        when(mockLibvirtException.getStackTrace()).thenReturn(new StackTraceElement[] {});
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowLibvirtManagerExceptionIfUnableToGetListOfRunningInstances() throws Exception {
        // setup
        when(mockConnection.listDomains()).thenThrow(mockLibvirtException);

        // act
        libvirtManager.getAllInstances();
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowLibvirtManagerExceptionIfUnableToFindTheRunningInstance() throws Exception {
        // setup
        when(mockConnection.listDomains()).thenReturn(new int[] { 1, 2 });
        when(mockConnection.domainLookupByID(1)).thenThrow(mockLibvirtException);

        // act
        libvirtManager.getAllInstances();
    }

    @Test
    public void shouldReturnListOfInstances() throws Exception {
        // setup
        when(mockConnection.listDomains()).thenReturn(new int[] { 1, 2 });
        when(mockConnection.domainLookupByID(1)).thenReturn(mockDomain);
        when(mockConnection.domainLookupByID(2)).thenReturn(mockDomain);

        // act
        Collection<Domain> allRunInstances = this.libvirtManager.getAllInstances();

        // assert
        assertEquals(2, allRunInstances.size());
    }

    @Test
    public void shouldReturnListOfRunningInstances() throws Exception {
        // setup
        DomainInfo info1 = createDomainInfo(DomainState.VIR_DOMAIN_RUNNING);
        DomainInfo info2 = createDomainInfo(DomainState.VIR_DOMAIN_NOSTATE);
        DomainInfo info3 = createDomainInfo(DomainState.VIR_DOMAIN_PAUSED);
        DomainInfo info4 = createDomainInfo(DomainState.VIR_DOMAIN_BLOCKED);
        DomainInfo info5 = createDomainInfo(DomainState.VIR_DOMAIN_CRASHED);
        DomainInfo info6 = createDomainInfo(DomainState.VIR_DOMAIN_SHUTDOWN);
        DomainInfo info7 = createDomainInfo(DomainState.VIR_DOMAIN_SHUTOFF);

        when(mockDomain.getName()).thenReturn("i-ABCD");
        when(mockDomain.getInfo()).thenReturn(info1).thenReturn(info2).thenReturn(info3).thenReturn(info4).thenReturn(info5).thenReturn(info6).thenReturn(info7);

        when(mockConnection.listDomains()).thenReturn(new int[] { 1, 2, 3, 4, 5, 6, 7 });
        when(mockConnection.domainLookupByID(isA(Integer.class))).thenReturn(mockDomain);

        // act
        Collection<String> allRunInstances = this.libvirtManager.getAllRunningInstances();

        // assert
        assertEquals(4, allRunInstances.size());
    }

    @Test
    public void shouldReturnListOfCrashedInstances() throws Exception {
        // setup
        final Domain[] domains = new Domain[] { createMockDomain(1, DomainState.VIR_DOMAIN_RUNNING), createMockDomain(2, DomainState.VIR_DOMAIN_NOSTATE), createMockDomain(3, DomainState.VIR_DOMAIN_PAUSED),
                createMockDomain(4, DomainState.VIR_DOMAIN_BLOCKED), createMockDomain(5, DomainState.VIR_DOMAIN_CRASHED), createMockDomain(6, DomainState.VIR_DOMAIN_SHUTDOWN), createMockDomain(7, DomainState.VIR_DOMAIN_SHUTOFF),
                createMockDomain(8, DomainState.VIR_DOMAIN_CRASHED) };

        when(mockConnection.listDomains()).thenReturn(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 });

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                int i = (Integer) invocation.getArguments()[0];
                return domains[i - 1];
            }
        }).when(mockConnection).domainLookupByID(isA(Integer.class));

        // act
        Collection<String> result = this.libvirtManager.getAllCrashedInstances();

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains("i-5"));
        assertTrue(result.contains("i-8"));
    }

    private Domain createMockDomain(int i, DomainState state) throws LibvirtException {
        Domain result = mock(Domain.class);
        when(result.getName()).thenReturn(String.format("i-%d", i));
        when(result.getInfo()).thenReturn(createDomainInfo(state));
        return result;
    }

    @Test
    public void shouldNotAddDomain0ToListOfRunningInstances() throws LibvirtException {
        // setup
        DomainInfo info = createDomainInfo(DomainState.VIR_DOMAIN_RUNNING);

        Domain domain0 = mock(Domain.class);
        when(domain0.getName()).thenReturn("Domain-0");
        when(domain0.getInfo()).thenReturn(info);

        Domain validDomain = mock(Domain.class);
        when(validDomain.getName()).thenReturn("i-ABCDEFG");
        when(validDomain.getInfo()).thenReturn(info);

        when(mockConnection.listDomains()).thenReturn(new int[] { 0, 1 });
        when(mockConnection.domainLookupByID(0)).thenReturn(domain0);
        when(mockConnection.domainLookupByID(1)).thenReturn(validDomain);

        // act
        Collection<String> allRunningInstances = this.libvirtManager.getAllRunningInstances();

        // assert
        assertEquals(1, allRunningInstances.size());
    }

    private DomainInfo createDomainInfo(DomainState state) {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.state = state;
        return domainInfo;
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowLibvirtManagerExceptionIfUnableToGetNodeInfo() throws Exception {
        // setup
        when(mockConnection.getNodeInfo()).thenThrow(mockLibvirtException);

        // act
        this.libvirtManager.getNodeInfo();
    }

    @Test
    public void shouldReturnNodeInfo() throws Exception {
        // setup
        NodeInfo mockNodeInfo = mock(NodeInfo.class);
        when(mockConnection.getNodeInfo()).thenReturn(mockNodeInfo);

        // act
        NodeInfo nodeInfo = this.libvirtManager.getNodeInfo();

        // assert
        assertEquals(mockNodeInfo, nodeInfo);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnLibvirtXml() {
        // setup
        final String basePath = "/state/partition1/pi/instances/owner/" + INSTANCE_ID;
        String fileContents = "<domain>hello</domain>";

        when(this.mockLibvirtTemplateGenerator.buildXml(isA(Map.class))).thenReturn(fileContents);

        // act
        String libvirtXml = this.libvirtManager.generateLibvirtXml(instance, ImagePlatform.linux, basePath, true, null, null, null, null);

        // assert
        assertEquals(fileContents, libvirtXml);
    }

    @Test
    public void shouldStartNewInstance() throws Exception {
        // setup
        String libvirtXml = "<domain></domain>";
        when(this.mockConnection.startDomain(libvirtXml)).thenReturn(mockDomain);
        DomainInfo domainInfo = createDomainInfo(DomainState.VIR_DOMAIN_BLOCKED);
        when(mockDomain.getInfo()).thenReturn(domainInfo);

        final String basePath = "/state/partition1/pi/instances/owner/" + INSTANCE_ID;

        // act
        this.libvirtManager.startInstance(libvirtXml, INSTANCE_ID, basePath);

        // assert
        verify(this.mockConnection).startDomain(libvirtXml);
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowLibvirtManagerExceptionOnLibvirtExceptionForStartInstance() throws LibvirtException {
        // setup
        String libvirtXml = "<domain></domain>";

        doThrow(mockLibvirtException).when(this.mockConnection).startDomain(libvirtXml);

        // act
        this.libvirtManager.startInstance(libvirtXml, INSTANCE_ID, "basePath");
    }

    @Test
    public void shouldStopInstance() throws Exception {
        // setup

        // act
        this.libvirtManager.stopInstance(mockDomain);

        // assert
        verify(this.mockConnection).shutdown(mockDomain);
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowNewLibvirtManagerExceptionOnAnyLibvirtExceptionForDomainShutdown() throws LibvirtException {
        // setup
        LibvirtException ex = mock(LibvirtException.class);
        doThrow(ex).when(this.mockConnection).shutdown(mockDomain);

        // act
        this.libvirtManager.stopInstance(mockDomain);
    }

    @Test
    public void shouldDestroyInstance() throws Exception {
        // setup

        // act
        this.libvirtManager.destroyInstance(mockDomain);

        // assert
        verify(this.mockConnection).destroy(mockDomain);
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowNewLibvirtManagerExceptionOnAnyLibvirtExceptionForDomainDestroy() throws LibvirtException {
        // setup
        LibvirtException ex = mock(LibvirtException.class);
        doThrow(ex).when(this.mockConnection).destroy(mockDomain);

        // act
        this.libvirtManager.destroyInstance(mockDomain);
    }

    @Test
    public void shouldNotStartInstanceIfItAlreadyExists() throws Exception {
        // setup
        String libvirtXml = "<domain></domain>";
        final String basePath = "/state/partition1/pi/instances/owner/" + INSTANCE_ID;
        when(this.mockConnection.domainLookupByName(INSTANCE_ID)).thenReturn(mockDomain);

        // act
        this.libvirtManager.startInstance(libvirtXml, INSTANCE_ID, basePath);

        // assert
        verify(this.mockConnection, never()).startDomain(libvirtXml);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotPassRamdiskIdIfUseRamdiskIfFalse() {
        // setup
        ImagePlatform platform = ImagePlatform.linux;
        final String basePath = "/state/partition1/pi/instances/owner/" + INSTANCE_ID;
        final String privateMacAddress = "d0:0d:45:1a:09:16";
        final String bridgeDevice = "pibr11";
        final String memory = "1048576";
        final String vcpus = "2";

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Map<String, Object> model = (Map<String, Object>) invocation.getArguments()[0];
                assertEquals(14, model.size());
                assertEquals("linux", model.get("TYPE"));
                assertEquals("linux", model.get("OS_TYPE"));
                assertEquals("true", model.get("use_ephemeral"));
                assertEquals("false", model.get("use_ramdisk"));
                assertEquals(basePath, model.get("BASEPATH"));
                assertEquals(basePath, model.get("SWAPPATH"));
                assertEquals(INSTANCE_ID, model.get("NAME"));
                assertEquals(privateMacAddress, model.get("PRIVMACADDR"));
                assertEquals(bridgeDevice, model.get("BRIDGEDEV"));
                assertEquals(memory, model.get("MEMORY"));
                assertEquals(vcpus, model.get("VCPUS"));
                assertEquals("18", model.get("XEN_ID"));
                assertEquals(instance.getKernelId(), model.get("KERNEL_ID"));
                assertEquals(instance.getImageId(), model.get("IMAGE_ID"));
                assertEquals(null, model.get("RAMDISK_ID"));

                return null;
            }
        }).when(mockLibvirtTemplateGenerator).buildXml(isA(Map.class));

        // act
        this.libvirtManager.generateLibvirtXml(instance, platform, basePath, false, privateMacAddress, bridgeDevice, memory, vcpus);

        // assert
        verify(this.mockLibvirtTemplateGenerator).buildXml(isA(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldPassAllRequiredParametersInMapToStartLinuxInstance() {
        // setup
        ImagePlatform platform = ImagePlatform.linux;
        final String basePath = "/state/partition1/pi/instances/owner/" + INSTANCE_ID;
        final String privateMacAddress = "d0:0d:45:1a:09:16";
        final String bridgeDevice = "pibr11";
        final String memory = "1048576";
        final String vcpus = "2";

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Map<String, Object> model = (Map<String, Object>) invocation.getArguments()[0];
                assertEquals(15, model.size());
                assertEquals("linux", model.get("TYPE"));
                assertEquals("linux", model.get("OS_TYPE"));
                assertEquals("true", model.get("use_ephemeral"));
                assertEquals("true", model.get("use_ramdisk"));
                assertEquals(basePath, model.get("BASEPATH"));
                assertEquals(basePath, model.get("SWAPPATH"));
                assertEquals(INSTANCE_ID, model.get("NAME"));
                assertEquals(privateMacAddress, model.get("PRIVMACADDR"));
                assertEquals(bridgeDevice, model.get("BRIDGEDEV"));
                assertEquals(memory, model.get("MEMORY"));
                assertEquals(vcpus, model.get("VCPUS"));
                assertEquals("18", model.get("XEN_ID"));
                assertEquals(instance.getKernelId(), model.get("KERNEL_ID"));
                assertEquals(instance.getImageId(), model.get("IMAGE_ID"));
                assertEquals(instance.getRamdiskId(), model.get("RAMDISK_ID"));

                return null;
            }
        }).when(mockLibvirtTemplateGenerator).buildXml(isA(Map.class));

        // act
        this.libvirtManager.generateLibvirtXml(instance, platform, basePath, true, privateMacAddress, bridgeDevice, memory, vcpus);

        // assert
        verify(this.mockLibvirtTemplateGenerator).buildXml(isA(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotPassLinuxSpecificParamsIfPlatformIsWindows() {
        // setup
        ImagePlatform platform = ImagePlatform.windows;
        final String basePath = "/state/partition1/pi/instances/owner/" + INSTANCE_ID;
        final String privateMacAddress = "d0:0d:45:1a:09:16";
        final String bridgeDevice = "pibr11";
        final String memory = "1048576";
        final String vcpus = "2";

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Map<String, Object> model = (Map<String, Object>) invocation.getArguments()[0];
                assertNull(model.get("SWAPPATH"));
                assertNull(model.get("ramdisk"));
                assertNull(model.get("KERNEL_ID"));
                return null;
            }
        }).when(mockLibvirtTemplateGenerator).buildXml(isA(Map.class));

        // act
        this.libvirtManager.generateLibvirtXml(instance, platform, basePath, true, privateMacAddress, bridgeDevice, memory, vcpus);

        // assert
        verify(this.mockLibvirtTemplateGenerator).buildXml(isA(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldPassAllRequiredValuesForBuildingAWindowsPlatform() {
        // setup
        ImagePlatform platform = ImagePlatform.windows;
        final String basePath = "/state/partition1/pi/instances/owner/" + INSTANCE_ID;
        final String privateMacAddress = "d0:0d:45:1a:09:16";
        final String bridgeDevice = "pibr11";
        final String memory = "1048576";
        final String vcpus = "2";

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Map<String, Object> model = (Map<String, Object>) invocation.getArguments()[0];
                assertEquals("hvm", model.get("OS_TYPE"));
                assertEquals("windows", model.get("TYPE"));
                assertEquals(INSTANCE_ID, model.get("NAME"));
                assertEquals(memory, model.get("MEMORY"));
                assertEquals(vcpus, model.get("VCPUS"));
                assertEquals(bridgeDevice, model.get("BRIDGEDEV"));
                assertEquals(privateMacAddress, model.get("PRIVMACADDR"));
                assertEquals(basePath, model.get("BASEPATH"));
                assertEquals(instance.getImageId(), model.get("IMAGE_ID"));
                assertEquals("true", model.get("use_ephemeral"));
                return null;
            }
        }).when(mockLibvirtTemplateGenerator).buildXml(isA(Map.class));

        // act
        this.libvirtManager.generateLibvirtXml(instance, platform, basePath, true, privateMacAddress, bridgeDevice, memory, vcpus);

        // assert
        verify(this.mockLibvirtTemplateGenerator).buildXml(isA(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldPassAllRequiredValuesForBuildingAnOpenSolarisPlatform() {
        // setup
        ImagePlatform platform = ImagePlatform.opensolaris;
        final String basePath = "/state/partition1/pi/instances/owner/" + INSTANCE_ID;
        final String privateMacAddress = "d0:0d:45:1a:09:16";
        final String bridgeDevice = "pibr11";
        final String memory = "1048576";
        final String vcpus = "2";

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Map<String, Object> model = (Map<String, Object>) invocation.getArguments()[0];
                assertEquals("linux", model.get("OS_TYPE"));
                assertEquals("opensolaris", model.get("TYPE"));
                assertEquals(INSTANCE_ID, model.get("NAME"));
                assertEquals(memory, model.get("MEMORY"));
                assertEquals(vcpus, model.get("VCPUS"));
                assertEquals(bridgeDevice, model.get("BRIDGEDEV"));
                assertEquals(privateMacAddress, model.get("PRIVMACADDR"));
                assertEquals(basePath, model.get("BASEPATH"));
                assertEquals(basePath, model.get("SWAPPATH"));
                return null;
            }
        }).when(mockLibvirtTemplateGenerator).buildXml(isA(Map.class));

        // act
        this.libvirtManager.generateLibvirtXml(instance, platform, basePath, true, privateMacAddress, bridgeDevice, memory, vcpus);

        // assert
        verify(this.mockLibvirtTemplateGenerator).buildXml(isA(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSaveLibvirtXmlIntoLibvirtFileInTheInstanceDirectory() {
        // setup
        final String basePath = "/state/partition1/pi/instances/owner/" + INSTANCE_ID;
        String fileContents = "<domain>hello</domain>";

        when(this.mockLibvirtTemplateGenerator.buildXml(isA(Map.class))).thenReturn(fileContents);

        // act
        this.libvirtManager.generateLibvirtXml(instance, ImagePlatform.linux, basePath, true, null, null, null, null);

        // assert
        verify(this.mockFileManager).saveFile(basePath + "/libvirt.xml", fileContents);
    }

    @Test
    public void shouldSaveInstanceStateAsJsonInCheckPointFile() throws Exception {
        // setup
        String libvirtXml = "<domain>hello</domain>";
        String jsoned = "{hello}";
        when(this.mockConnection.startDomain(libvirtXml)).thenReturn(mockDomain);
        DomainInfo domainInfo = createDomainInfo(DomainState.VIR_DOMAIN_BLOCKED);
        when(mockDomain.getInfo()).thenReturn(domainInfo);

        final String basePath = "/state/partition1/pi/instances/owner/" + INSTANCE_ID;

        when(this.mockKoalaJsonParser.getJson(Matchers.isA(Object.class))).thenReturn(jsoned);

        // act
        this.libvirtManager.startInstance(libvirtXml, INSTANCE_ID, basePath);

        // assert
        verify(this.mockFileManager).saveFile(basePath + "/instance-checkpoint.json", jsoned);
    }

    @Test
    public void shouldLookupInstanceBasedOnInstanceId() throws LibvirtException {
        // act
        libvirtManager.lookupInstance(INSTANCE_ID);

        // assert
        verify(mockConnection).domainLookupByName(INSTANCE_ID);
    }

    public void shouldReturnNullIfUnableToLookupDomainByName() throws LibvirtException {
        // setup
        when(mockConnection.domainLookupByName(INSTANCE_ID)).thenReturn(null);

        // act
        Domain domain = libvirtManager.lookupInstance(INSTANCE_ID);

        // assert
        assertNull(domain);
    }

    @Test
    public void shouldAttachVolume() throws LibvirtException {
        // setup
        String xml = "<xml/>";

        // act
        libvirtManager.attachVolume(INSTANCE_ID, xml);

        // assert
        verify(this.mockConnection).attachDevice(INSTANCE_ID, xml);
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowIfConnectionThrowsWhenAttachingVolume() throws LibvirtException {
        // setup
        String xml = "<xml/>";
        LibvirtException ex = mock(LibvirtException.class);
        doThrow(ex).when(this.mockConnection).attachDevice(INSTANCE_ID, xml);

        // act
        libvirtManager.attachVolume(INSTANCE_ID, xml);
    }

    @Test
    public void shouldDetachVolume() throws LibvirtException {
        // setup
        String xml = "<xml/>";

        // act
        libvirtManager.detachVolume(INSTANCE_ID, xml);

        // assert
        verify(this.mockConnection).detachDevice(INSTANCE_ID, xml);
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowIfConnectionThrowsWhenDetachingVolume() throws LibvirtException {
        // setup
        String xml = "<xml/>";
        LibvirtException ex = mock(LibvirtException.class);
        doThrow(ex).when(this.mockConnection).detachDevice(INSTANCE_ID, xml);

        // act
        libvirtManager.detachVolume(INSTANCE_ID, xml);
    }

    @Test
    public void shouldRebootInstance() throws LibvirtException {
        // setup

        // act
        libvirtManager.reboot(mockDomain);

        // assert
        verify(mockDomain).reboot(eq(0));
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowIfRebootThrowWhenRebooting() throws LibvirtException {
        // setup
        doThrow(mockLibvirtException).when(mockDomain).reboot(eq(0));

        // act
        libvirtManager.reboot(mockDomain);
    }

    @Test
    public void shouldReturnTrueIfDeviceExistsInDomain() throws Exception {
        // setup
        String devicePath = "/path.to.volume";
        when(mockConnection.domainLookupByName(INSTANCE_ID)).thenReturn(mockDomain);

        String domainXml = "<domain type='xen' id='18'><name>test</name><os><type>linux</type><kernel>/root/kernelId</kernel><initrd>/root/ramdiskId</initrd>"
                + "<root>/dev/sda1</root><cmdline> ro</cmdline></os><memory>128m</memory><vcpu>2</vcpu><devices><disk type='file'><source file='/root/imageId'/>"
                + "<target dev='sda1'/></disk><disk type='file'><source file='/path.to.volume'/><target dev='sda3'/></disk><interface type='bridge'>"
                + "<source bridge='virbr0'/><mac address='127.0.0.1'/><script path='/etc/xen/scripts/vif-bridge'/></interface></devices></domain>";

        when(mockDomain.getXMLDesc(-1)).thenReturn(domainXml);

        // act
        boolean volumeExists = libvirtManager.volumeExists(INSTANCE_ID, devicePath);

        // assert
        assertTrue(volumeExists);
    }

    @Test
    public void shouldNotThrowExceptionIfDeviceDoesntExistInDomain() throws Exception {
        // setup
        String devicePath = "/path.to.volume";
        when(mockConnection.domainLookupByName(INSTANCE_ID)).thenReturn(mockDomain);

        String domainXml = "<domain type='xen' id='18'><name>test</name><os><type>linux</type><kernel>/root/kernelId</kernel><initrd>/root/ramdiskId</initrd>"
                + "<root>/dev/sda1</root><cmdline> ro</cmdline></os><memory>128m</memory><vcpu>2</vcpu><devices><disk type='file'><source file='/root/imageId'/>"
                + "<target dev='sda1'/></disk><disk type='file'><source file='/abc'/><target dev='sda3'/></disk><interface type='bridge'>"
                + "<source bridge='virbr0'/><mac address='127.0.0.1'/><script path='/etc/xen/scripts/vif-bridge'/></interface></devices></domain>";

        when(mockDomain.getXMLDesc(-1)).thenReturn(domainXml);

        // act
        libvirtManager.volumeExists(INSTANCE_ID, devicePath);
    }

    @Test(expected = LibvirtManagerException.class)
    public void shouldThrowExceptionWhenDomainNotFound() {
        // setup
        String devicePath = "/path.to.volume";

        // act
        try {
            libvirtManager.volumeExists(INSTANCE_ID, devicePath);
        } catch (LibvirtManagerException e) {
            assertEquals("Unable to get domain:" + INSTANCE_ID, e.getMessage());
            throw e;
        }
    }

    @Test
    public void shouldPauseInstance() throws LibvirtException {
        // setup

        // act
        libvirtManager.pauseInstance(INSTANCE_ID);

        // assert
        verify(mockConnection).pauseInstance(INSTANCE_ID);
    }

    @Test
    public void shouldUnPauseInstance() throws LibvirtException {
        // setup

        // act
        libvirtManager.unPauseInstance(INSTANCE_ID);

        // assert
        verify(mockConnection).unPauseInstance(INSTANCE_ID);
    }

    @Test
    public void shouldReturnInstanceCrashed() throws Exception {
        // setup
        Domain domain = mock(Domain.class);
        DomainInfo domainInfo = createDomainInfo(DomainState.VIR_DOMAIN_CRASHED);
        when(domain.getInfo()).thenReturn(domainInfo);
        when(mockConnection.domainLookupByName(INSTANCE_ID)).thenReturn(domain);

        // assert
        assertTrue(libvirtManager.isInstanceCrashed(INSTANCE_ID));
    }

    @Test
    public void shouldReturnDomainXml() throws Exception {
        // setup
        String xml = "<xml/>";
        when(mockConnection.getDomainXml(INSTANCE_ID)).thenReturn(xml);

        // act
        String result = libvirtManager.getDomainXml(INSTANCE_ID);

        // assert
        assertEquals(xml, result);
    }
}
