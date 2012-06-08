package com.bt.pi.app.instancemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.libvirt.Domain;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.handlers.AnycastHandler;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.app.instancemanager.handlers.PauseInstanceTaskQueueWatcherInitiator;
import com.bt.pi.app.instancemanager.handlers.RemoveInstanceFromUserTaskQueueWatcherInitiator;
import com.bt.pi.app.instancemanager.handlers.RunInstanceWatcherManager;
import com.bt.pi.app.instancemanager.handlers.SystemResourceState;
import com.bt.pi.app.instancemanager.handlers.TerminateInstanceTaskQueueWatcherInitiator;
import com.bt.pi.app.instancemanager.handlers.XenRefreshHandler;
import com.bt.pi.app.instancemanager.testing.StubLibvirtConnection;
import com.bt.pi.app.management.QueueSeeder;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.testing.StubCommandExecutor;

public class InstanceManagerApplicationIntegrationTestBase {
    private static final String KERNEL_ID = "pki-PumpkinPie";
    protected static final String USER_NAME = "test";
    protected static final String IMAGE_ID = "pmi-ApplePie";
    protected static final String KEY = "key";
    protected String instancePath;
    protected String instanceImagePath;
    protected static ClassPathXmlApplicationContext context;
    protected static Properties properties;
    protected Instance instance;
    protected PId instancePastryId;
    protected InstanceTypeConfiguration instanceTypeConfiguration;
    protected StubLibvirtConnection stubLibvirtConnection;
    protected StubCommandExecutor stubCommandExecutor;
    protected SystemResourceState systemResourceState;
    protected PiIdBuilder piIdBuilder;
    protected DhtClientFactory dhtClientFactory;
    protected static XenRefreshHandler xenRefreshHandler;
    protected InstanceManagerApplication instanceManagerApplication;
    protected static int instanceIdIndex = 1;
    protected String instanceId;
    protected TaskProcessingQueueHelper taskProcessingQueueHelper;
    protected TerminateInstanceTaskQueueWatcherInitiator terminateInstanceTaskQueueWatcherInitiator;
    protected RemoveInstanceFromUserTaskQueueWatcherInitiator removeInstanceFromUserTaskQueueWatcherInitiator;
    protected RunInstanceWatcherManager runInstanceWatcherManager;
    protected PauseInstanceTaskQueueWatcherInitiator pauseInstanceTaskQueueWatcherInitiator;

    @BeforeClass
    public static void loadCommonApplicationContextAndStartTheNode() {
        context = new ClassPathXmlApplicationContext("commonApplicationContext.xml");

        xenRefreshHandler = (XenRefreshHandler) context.getBean("xenRefreshHandler");
        xenRefreshHandler.setInitialIntervalMillis(1000);
        xenRefreshHandler.setRepeatingIntervalMillis(1000);

        properties = (Properties) context.getBean("properties");
        KoalaNode koalaNode = (KoalaNode) context.getBean("koalaNode");
        koalaNode.start();
        AnycastHandler anycastHandler = (AnycastHandler) context.getBean("anycastHandler");
        anycastHandler.refreshInstanceTypes();
        FileUtils.deleteQuietly(new File("tmp"));
    }

    private void getBeanReferences() {
        systemResourceState = (SystemResourceState) context.getBean("systemResourceState");
        instanceManagerApplication = (InstanceManagerApplication) context.getBean("instanceManagerApplication");
        piIdBuilder = (PiIdBuilder) context.getBean("piIdBuilder");
        dhtClientFactory = (DhtClientFactory) context.getBean("dhtClientFactory");
        stubLibvirtConnection = (StubLibvirtConnection) context.getBean("libvirtConnection");
        stubCommandExecutor = (StubCommandExecutor) context.getBean("stubCommandExecutor");
        taskProcessingQueueHelper = (TaskProcessingQueueHelper) context.getBean("taskProcessingQueueHelper");
        terminateInstanceTaskQueueWatcherInitiator = (TerminateInstanceTaskQueueWatcherInitiator) context.getBean("terminateInstanceTaskQueueWatcherInitiator");
        removeInstanceFromUserTaskQueueWatcherInitiator = context.getBean(RemoveInstanceFromUserTaskQueueWatcherInitiator.class);
        runInstanceWatcherManager = (RunInstanceWatcherManager) context.getBean("runInstanceWatcherManager");
        pauseInstanceTaskQueueWatcherInitiator = context.getBean(PauseInstanceTaskQueueWatcherInitiator.class);
    }

    @Before
    public void resetCommandExecutor() {
        stubCommandExecutor.clearCommands();
    }

    @Before
    public void seedTheDht() throws Exception {
        instanceId = String.format("i-07VO7Y%d", instanceIdIndex);
        System.err.println("instanceId: " + instanceId);
        instanceIdIndex++;

        getBeanReferences();
        seed();
        putImageIntoDht();
        putUserIntoDht();
        putInstanceIntoDht(instanceId);
        putInstanceTypeIntoDht();

        instancePath = String.format("%s/%s/%s", properties.getProperty("instances.directory"), instance.getUserId(), instanceId);
        instanceImagePath = String.format("%s/%s", instancePath, IMAGE_ID);

        instanceManagerApplication.becomeActive();

        AnycastHandler anycastHandler = (AnycastHandler) context.getBean("anycastHandler");
        anycastHandler.refreshInstanceTypes();

        Thread.sleep(3000); // to allow async initialisations etc
    }

    private void seed() {
        QueueSeeder queueSeeder = new QueueSeeder();
        queueSeeder.setPiIdBuilder(piIdBuilder);
        queueSeeder.setDhtClientFactory(dhtClientFactory);
        queueSeeder.addQueue(PiQueue.TERMINATE_INSTANCE);
        queueSeeder.addQueue(PiQueue.RUN_INSTANCE);
        queueSeeder.addQueue(PiQueue.PAUSE_INSTANCE);
        queueSeeder.addQueue(PiQueue.REMOVE_INSTANCE_FROM_USER);
    }

    @After
    public void resetLibvirtConnection() {
        stubLibvirtConnection.reset();
    }

    @AfterClass
    public static void removeStorageFolderAndNodeIdFile() throws Exception {
        File nodeIdFile = new File(properties.getProperty("nodeIdFile"));
        String nodeId = FileUtils.readFileToString(nodeIdFile);

        File file = new File(String.format("storage%s", nodeId));
        if (file.exists()) {
            FileUtils.deleteDirectory(file);
        }

        if (nodeIdFile.exists())
            nodeIdFile.delete();
    }

    protected Instance putInstanceIntoDht(String instanceID) {
        int globalAvailabilityZoneCode = piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instanceID);
        instance = new Instance(instanceID, "test", "default", ImagePlatform.linux);
        instance.setAvailabilityZoneCode(PId.getAvailabilityZoneCodeWithinRegionFromGlobalAvailabilityZoneCode(globalAvailabilityZoneCode));
        instance.setRegionCode(PId.getRegionCodeFromGlobalAvailabilityZoneCode(globalAvailabilityZoneCode));
        instance.setImageId(IMAGE_ID);
        instance.setKeyName(KEY);
        instance.setImageSizeInMB(1024);
        instance.setPrivateMacAddress("D0:0D:12:34:56:78");
        instance.setMemoryInKB("512");
        instance.setVcpus(1);
        instance.setVlanId(getRandomVlanId());
        instance.setInstanceType("m1.small");
        instance.setUserId(USER_NAME);
        instance.setState(InstanceState.PENDING);

        instancePastryId = piIdBuilder.getPIdForEc2AvailabilityZone(instance);
        System.err.println("Instance pastryId: " + instancePastryId + " gAvz code embeded in instanceID: " + globalAvailabilityZoneCode);
        System.err.println("Instance pastryId: " + instancePastryId.toStringFull());
        putPiEntityIntoDht(instance, instancePastryId);

        return instance;
    }

    private int getRandomVlanId() {
        Random r = new Random();
        return r.nextInt(100) + 100;
    }

    protected void putInstanceTypeIntoDht() {
        instanceTypeConfiguration = new InstanceTypeConfiguration(instance.getInstanceType(), 1, 256, 1);
        InstanceTypes instanceTypes = new InstanceTypes();
        instanceTypes.addInstanceType(instanceTypeConfiguration);
        PId instanceTypesId = piIdBuilder.getPId(instanceTypes);
        putPiEntityIntoDht(instanceTypes, instanceTypesId);
    }

    protected void putImageIntoDht() {
        PId imageId = piIdBuilder.getPId(Image.getUrl(IMAGE_ID));
        Image image = new Image();
        image.setArchitecture("i386");
        image.setImageId(IMAGE_ID);
        image.setKernelId(KERNEL_ID);
        image.setRamdiskId("pri-RhubarbPie");
        image.setPublic(true);
        image.setState(ImageState.AVAILABLE);
        image.setMachineType(MachineType.MACHINE);
        putPiEntityIntoDht(image, imageId);
    }

    private void putUserIntoDht() {
        PId userId = piIdBuilder.getPId(User.getUrl(USER_NAME));
        User user = new User();
        user.setUsername(USER_NAME);
        user.getKeyPairs().add(new KeyPair(KEY, "", ""));
        user.addInstance(instanceId);
        putPiEntityIntoDht(user, userId);
    }

    protected void putPiEntityIntoDht(PiEntityBase piEntity, PId id) {
        BlockingDhtWriter dhtWriter = dhtClientFactory.createBlockingWriter();
        dhtWriter.update(id, piEntity, new UpdateResolvingPiContinuation<PiEntityBase>() {
            @Override
            public void handleResult(PiEntityBase result) {
            }

            @Override
            public PiEntityBase update(PiEntityBase existingEntity, PiEntityBase requestedEntity) {
                if (existingEntity != null)
                    requestedEntity.setVersion(existingEntity.getVersion());

                return requestedEntity;
            }
        });
        System.err.println("Value written: " + dhtWriter.getValueWritten() + " to PId" + id);
    }

    protected PiEntityBase getPiEntityFromDht(PId id) {
        return (PiEntityBase) dhtClientFactory.createBlockingReader().get(id);
    }

    protected void assertThatTheRightLibvirtApiIsInvokedForRunInstance() throws Exception {
        int[] domains = stubLibvirtConnection.listDomains();
        assertEquals(1, domains.length);

        Domain domain = stubLibvirtConnection.domainLookupByID(domains[0]);
        assertEquals(domain.getName(), instanceId);

        String domainXml = domain.getXMLDesc(0);
        assertThatDomainXmlContainsNecessaryFields(domainXml);
    }

    protected void assertThatDomainXmlContainsNecessaryFields(String domainXml) {
        String errorMessage = String.format("Instance: %s, XML: %s", instance, domainXml);
        assertTrue(errorMessage, domainXml.contains(String.format("<name>%s</name>", instanceId)));
        assertTrue(errorMessage, domainXml.contains(String.format("<type>%s</type>", instance.getPlatform())));
        assertTrue(errorMessage, domainXml.contains(String.format("<kernel>%s/%s/%s/%s</kernel>", properties.getProperty("instances.directory"), instance.getUserId(), instanceId, KERNEL_ID)));
        assertTrue(errorMessage, domainXml.contains("<cmdline> ro</cmdline>"));
        assertTrue(errorMessage, domainXml.contains("<root>/dev/sda1</root>"));
        assertTrue(errorMessage, domainXml.contains(String.format("<source file='%s/%s/%s/%s'/>", properties.getProperty("instances.directory"), instance.getUserId(), instanceId, instance.getImageId())));
        assertTrue(errorMessage, domainXml.contains(String.format("<source file='%s/%s/%s/ephemeral'/>", properties.getProperty("instances.directory"), instance.getUserId(), instanceId)));
        assertTrue(errorMessage, domainXml.contains(String.format("<source file='%s/%s/%s/swap'/>", properties.getProperty("instances.directory"), instance.getUserId(), instanceId)));
        assertTrue(errorMessage, domainXml.contains("<target dev='sda1'/>"));
        assertTrue(errorMessage, domainXml.contains("<target dev='sda2'/>"));
        assertTrue(errorMessage, domainXml.contains("<target dev='sda3'/>"));
        assertTrue(errorMessage, domainXml.contains("<interface type='bridge'>"));
        assertTrue(errorMessage, domainXml.contains(String.format("<mac address='%s'/>", instance.getPrivateMacAddress())));
        assertTrue(errorMessage, domainXml.contains(String.format("<source bridge='%s'/>", VlanAddressUtils.getBridgeNameForVlan(instance.getVlanId()))));
        assertTrue(errorMessage, domainXml.contains("<script path='/etc/xen/scripts/vif-bridge'/>"));
    }

    protected void assertThatTheRightCommandsAreExecutedForRunInstance() {
        assertThatNetworkCommandsAreExecutedForRunInstance();
        assertThatPlatformBuilderCommandsAreExecutedForRunInstance();

        stubCommandExecutor.logCommandsToStream(System.err);
    }

    protected void assertThatTheRightCommandsAreExecutedForTerminateInstance(String instanceId) {
        assertThatNetworkCommandsAreExecutedForTerminateInstance(instanceId);

        stubCommandExecutor.logCommandsToStream(System.err);
    }

    protected void assertThatPlatformBuilderCommandsAreExecutedForRunInstance() {
        assertThatEmbedKeyCommandIsRun();
        assertThatDDCommandIsRun();
        assertThatMkSwapCommandIsRun();
        assertThatEphemeralDDCommandIsRun();
        assertThatEphemeralMkfsCommandIsRun();
    }

    protected void assertThatEphemeralMkfsCommandIsRun() {
        String[] expectedCommand = new String[] { "/bin/sh", "-c", String.format("nice -n +10 ionice -c3 mkfs.ext3 -F %s/ephemeral >/dev/null 2>&1", instancePath) };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatEphemeralDDCommandIsRun() {
        int ephemeralDiskSize = instance.getImageSizeInMB() - (int) (new File(instanceImagePath).length() / 1024 / 1024);
        String[] expectedCommand = new String[] { "/bin/sh", "-c", String.format("ionice -c3 dd bs=1M count=0 seek=%d if=/dev/zero of=%s/ephemeral 2>/dev/null", ephemeralDiskSize, instancePath) };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatMkSwapCommandIsRun() {
        String[] expectedCommand = new String[] { "/bin/sh", "-c", String.format("mkswap %s/swap >/dev/null", instancePath) };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatDDCommandIsRun() {
        String[] expectedCommand = new String[] { "/bin/sh", "-c", String.format("ionice -c3 dd bs=1M count=0 seek=%d if=/dev/zero of=%s/swap 2>/dev/null", Integer.parseInt(properties.getProperty("CONFIG_NC_SWAP_SIZE", "512")), instancePath) };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatEmbedKeyCommandIsRun() {
        assertTrue(stubCommandExecutor.assertThatEmbedKeyCommandIsRun(instanceImagePath));
    }

    protected void assertThatNetworkCommandsAreExecutedForRunInstance() {
        assertThatVconfigAddVlanCommandIsExecuted();
        assertThatNewBridgeCommandIsExecuted();
        assertThatBridgeIfCommandIsExecuted();
        assertThatBridgeUpCommandIsExecuted();
        assertThatBringInterfaceUpCommandIsExecuted();
    }

    protected void assertThatNetworkCommandsAreExecutedForTerminateInstance(String instanceId) {
        assertThatVconfigRemoveVlanCommandIsExecuted();
        assertThatBridgeDownCommandIsExecuted();
        assertThatBringInterfaceDownCommandIsExecuted();
        assertThatMoveCommandIsExecuted(instanceId);
    }

    private void assertThatMoveCommandIsExecuted(String instanceId) {
        String[] expectedCommand = new String[] { "mv", String.format("%s/%s/%s", properties.getProperty("instances.directory"), instance.getUserId(), instanceId),
                String.format("%s/%s/%s-terminated-%s", properties.getProperty("instances.directory"), instance.getUserId(), instanceId, new SimpleDateFormat("yyyyMMdd").format(new Date())) };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatBringInterfaceUpCommandIsExecuted() {
        String[] expectedCommand = new String[] { "ip", "link", "set", "dev", String.format("%s.%d", properties.getProperty("vnet.private.interface"), instance.getVlanId()), "up", "mtu", "1500" };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatBringInterfaceDownCommandIsExecuted() {
        String[] expectedCommand = new String[] { "ip", "link", "set", "dev", String.format("%s.%d", properties.getProperty("vnet.private.interface"), instance.getVlanId()), "down" };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatBridgeUpCommandIsExecuted() {
        String[] expectedCommand = new String[] { "ip", "link", "set", "dev", String.format("pibr%d", instance.getVlanId()), "up" };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatBridgeDownCommandIsExecuted() {
        String[] expectedCommand = new String[] { "ip", "link", "set", "dev", String.format("pibr%d", instance.getVlanId()), "down" };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatBridgeIfCommandIsExecuted() {
        String[] expectedCommand = new String[] { "brctl", "addif", String.format("pibr%d", instance.getVlanId()), String.format("%s.%d", properties.getProperty("vnet.private.interface"), instance.getVlanId()) };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatNewBridgeCommandIsExecuted() {
        String[] expectedCommand = new String[] { "brctl", "addbr", String.format("pibr%d", instance.getVlanId()) };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatVconfigAddVlanCommandIsExecuted() {
        String[] expectedCommand = new String[] { "vconfig", "add", properties.getProperty("vnet.private.interface"), String.format("%d", instance.getVlanId()) };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatVconfigRemoveVlanCommandIsExecuted() {
        String[] expectedCommand = new String[] { "vconfig", "rem", String.format("%s.%d", properties.getProperty("vnet.private.interface"), instance.getVlanId()) };
        assertTrue(stubCommandExecutor.assertCommand(expectedCommand));
    }

    protected void assertThatSystemResourcesHaveBeenConsumed(SystemState startSystemState) {
        SystemState endSystemState = new SystemState(systemResourceState.getFreeCores(), systemResourceState.getFreeMemoryInMB());

        assertEquals(instanceTypeConfiguration.getNumCores(), startSystemState.cores - endSystemState.cores);
        assertEquals(instanceTypeConfiguration.getMemorySizeInMB(), startSystemState.memory - endSystemState.memory);
    }

    protected void assertThatSystemResourcesHaveBeenReleased(SystemState startSystemState) {
        SystemState endSystemState = new SystemState(systemResourceState.getFreeCores(), systemResourceState.getFreeMemoryInMB());
        assertEquals(instanceTypeConfiguration.getNumCores(), endSystemState.cores - startSystemState.cores);
        assertEquals(instanceTypeConfiguration.getMemorySizeInMB(), endSystemState.memory - startSystemState.memory);
    }

    class SystemState {
        int cores;
        private long memory;

        public SystemState(int cores, long memory) {
            this.cores = cores;
            this.memory = memory;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("cores", cores).append("memory", memory).toString();
        }
    }
}
