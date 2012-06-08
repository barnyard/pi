package com.bt.pi.app.networkmanager;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.mockito.exceptions.verification.WantedButNotInvoked;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import rice.pastry.PastryNode;

import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.VlanAllocationIndex;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.management.PiSeeder;
import com.bt.pi.app.networkmanager.handlers.NetworkCleanupHandler;
import com.bt.pi.app.networkmanager.handlers.SecurityGroupNetworkUpdateHandler;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.testing.StubCommandExecutor;

public abstract class IntegrationTestBase {
    protected static final String NEW_RING_FOR_EACH_TEST = "new.ring.for.each.integration.test";
    @Rule
    public TestName testName = new TestName();
    private static final String AVAILABILITY_ZONE_NAME = "c1";
    private static final String REGION_NAME = "UK";
    private static final String REGION_ENDPOINT = "uk.com";
    protected static final int CLUSTER_START_PORT = 5050;
    private static final int NUMBER_OF_NODES_PER_AVAILABILITY_ZONE = 2;
    private static final int REGION_CODE = 99;
    private static final int AVAILABILITY_ZONE = 66;
    private static final String COMMON_APP_CTX = "commonApplicationContext.xml";
    protected static int numberOfNodes = NUMBER_OF_NODES_PER_AVAILABILITY_ZONE;
    protected static SortedMap<String, AbstractApplicationContext> applicationContexts;
    protected static final String VALID_PUBLIC_ADDRESS_1 = "10.0.0.1";
    protected static final String VALID_PUBLIC_ADDRESS_2 = "10.0.0.2";
    protected NetworkManagerApplication currentNetworkManagerApplication;
    protected PastryNode currentPastryNode;
    protected DhtClientFactory currentDhtClientFactory;
    protected PiIdBuilder currentPiIdBuilder;
    protected ConsumedDhtResourceRegistry currentConsumedDhtResourceRegistry;
    protected ApplicationContext currentApplicationContext;
    protected TaskProcessingQueueHelper currentTaskProcessingQueueHelper;
    protected SecurityGroupUpdateTaskQueueWatcherInitiator currentSecurityGroupUpdateTaskQueueWatcherInitiator;
    protected SecurityGroupDeleteTaskQueueWatcherInitiator currentSecurityGroupDeleteTaskQueueWatcherInitiator;
    protected AssociateAddressTaskQueueWatcherInitiator currentAssociateAddressTaskQueueWatcherInitiator;
    protected DisassociateAddressTaskQueueWatcherInitiator currentDisassociateAddressTaskQueueWatcherInitiator;
    protected InstanceNetworkManagerTeardownTaskQueueWatcherInitiator currentInstanceNetworkManagerTeardownTaskQueueWatcherInitiator;
    protected ApplicationRegistry currentApplicationRegistry;
    protected KoalaNode currentKoalaNode;
    protected ConsumedDhtResourceRegistry currentSpiedOnConsumedDhtResourceRegistry;
    private static boolean newRingForEachTest = null != System.getProperty(NEW_RING_FOR_EACH_TEST, null);

    public static void beforeClassBase(int numberOfNodes) throws Exception {
        IntegrationTestBase.numberOfNodes = numberOfNodes;
        if (!newRingForEachTest)
            startCluster();
    }

    @Before
    public void beforeBase() throws Exception {
        System.err.println("#### running " + testName.getMethodName());
        if (newRingForEachTest)
            startCluster();
    }

    @After
    public void afterBase() throws Exception {
        if (newRingForEachTest)
            tearDownArtifacts();
    }

    @AfterClass
    public static void afterClassBase() throws Exception {
        if (!newRingForEachTest)
            tearDownArtifacts();
    }

    public static void startCluster() throws Exception {
        deleteNodeIdFiles();
        deleteStorageDirectories();
        BeanPropertiesMunger.setDoMunging(true);
        applicationContexts = new TreeMap<String, AbstractApplicationContext>();

        int groovyPort = 20000;
        int port = CLUSTER_START_PORT;
        int bootstrapPort = port;
        BeanPropertiesMunger.setRegionAndAvailabilityZone(REGION_CODE, AVAILABILITY_ZONE);

        for (int k = 0; k < numberOfNodes; k++) {
            BeanPropertiesMunger.setPortAndBootstrapPort(port, k == 0 ? CLUSTER_START_PORT : bootstrapPort);
            BeanPropertiesMunger.setApplicationPorts(groovyPort++);
            startNewNode(port++);
            BeanPropertiesMunger.resetRegionAndAvailabilityZone();
            Thread.sleep(10 * 1000);
        }

        System.err.println("Printing out leafsets for all nodes");
        printNodeLeafsetSizes();

        System.err.println("All nodes are now up, seeding the system");
        seedSystem((PiSeeder) getBeanFromApplicationContext(CLUSTER_START_PORT, "piSeeder"));

        waitForAllApplicationsToBecomeActiveInAllContexts();
    }

    public static void tearDownArtifacts() throws Exception {
        BeanPropertiesMunger.setDoMunging(false);
        for (Entry<String, AbstractApplicationContext> entry : applicationContexts.entrySet()) {
            AbstractApplicationContext context = entry.getValue();
            context.destroy();

            File nodeIdFile = new File(String.format("nodeIdFile%s", entry.getKey()));
            String nodeId = FileUtils.readFileToString(nodeIdFile);
            File file = new File(String.format("storage%s", nodeId));
            if (file.exists()) {
                FileUtils.deleteDirectory(file);
            }

            if (nodeIdFile.exists())
                nodeIdFile.delete();
        }

        deleteNodeIdFiles();
        deleteStorageDirectories();
        FileUtils.deleteQuietly(new File("var"));
        Thread.sleep(5000); // allow time to "die down"
    }

    private static void deleteNodeIdFiles() throws Exception {
        @SuppressWarnings("unchecked")
        Collection<File> files = FileUtils.listFiles(new File("."), FileFilterUtils.prefixFileFilter("nodeIdFile"), null);
        for (File f : files)
            FileUtils.deleteQuietly(f);
    }

    private static void deleteStorageDirectories() throws Exception {
        File root = new File(".");
        String[] list = root.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("storage");
            }
        });
        for (String s : list) {
            File d = new File(s);
            if (d.isDirectory())
                FileUtils.deleteQuietly(d);
        }
    }

    protected static void seedSystem(PiSeeder piSeeder) {
        System.err.println("Seeding system.");
        piSeeder.configureRegions(String.format("%s", REGION_NAME), String.format("%s", REGION_CODE), String.format("%s", REGION_ENDPOINT), String.format("%s", REGION_ENDPOINT));
        piSeeder.configureAvailabilityZones(AVAILABILITY_ZONE_NAME, Integer.toString(AVAILABILITY_ZONE), String.format("%s", REGION_CODE), "UP");
        piSeeder.createApplicationRecordForAvailabilityZone(NetworkManagerApplication.APPLICATION_NAME, AVAILABILITY_ZONE_NAME, "1");
        piSeeder.addTaskProcessingQueue("ASSOCIATE_ADDRESS");
        piSeeder.addTaskProcessingQueue("DISASSOCIATE_ADDRESS");
        piSeeder.addTaskProcessingQueue("UPDATE_SECURITY_GROUP");
        piSeeder.addTaskProcessingQueue("REMOVE_SECURITY_GROUP");
        piSeeder.addTaskProcessingQueue("INSTANCE_NETWORK_MANAGER_TEARDOWN");
    }

    protected static void seedSubnets() {
        PiIdBuilder piIdBuilder = (PiIdBuilder) getBeanFromApplicationContext(CLUSTER_START_PORT, "piIdBuilder");
        DhtClientFactory dhtClientFactory = (DhtClientFactory) getBeanFromApplicationContext(CLUSTER_START_PORT, "dhtClientFactory");
        SubnetAllocationIndex subnetIndex = new SubnetAllocationIndex();
        Set<ResourceRange> aSubnetRanges = new TreeSet<ResourceRange>();
        aSubnetRanges.add(new ResourceRange(IpAddressUtils.ipToLong("172.30.0.0"), IpAddressUtils.ipToLong("172.30.255.255"), 16));
        subnetIndex.setResourceRanges(aSubnetRanges);
        subnetIndex.setDnsAddress("147.149.2.5");
        subnetIndex.setInactiveResourceConsumerTimeoutSec(3600L);
        BlockingDhtWriter blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(piIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion(), subnetIndex, new UpdateResolver<SubnetAllocationIndex>() {
            @Override
            public SubnetAllocationIndex update(SubnetAllocationIndex existingEntity, SubnetAllocationIndex requestedEntity) {
                if (null != existingEntity)
                    requestedEntity.setVersion(existingEntity.getVersion() + 1);
                return requestedEntity;
            }
        });
    }

    protected static void seedVlans() {
        PiIdBuilder piIdBuilder = (PiIdBuilder) getBeanFromApplicationContext(CLUSTER_START_PORT, "piIdBuilder");
        DhtClientFactory dhtClientFactory = (DhtClientFactory) getBeanFromApplicationContext(CLUSTER_START_PORT, "dhtClientFactory");
        VlanAllocationIndex vlanIndex = new VlanAllocationIndex();
        Set<ResourceRange> aVlanRanges = new TreeSet<ResourceRange>();
        aVlanRanges.add(new ResourceRange(1L, 2L));
        vlanIndex.setResourceRanges(aVlanRanges);
        vlanIndex.setInactiveResourceConsumerTimeoutSec(3600L);
        BlockingDhtWriter blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(piIdBuilder.getPId(VlanAllocationIndex.URL).forLocalRegion(), vlanIndex, new UpdateResolver<VlanAllocationIndex>() {
            @Override
            public VlanAllocationIndex update(VlanAllocationIndex existingEntity, VlanAllocationIndex requestedEntity) {
                if (null != existingEntity)
                    requestedEntity.setVersion(existingEntity.getVersion() + 1);
                return requestedEntity;
            }
        });
    }

    protected static void seedPublicIpAddresses() {
        PiIdBuilder piIdBuilder = (PiIdBuilder) getBeanFromApplicationContext(CLUSTER_START_PORT, "piIdBuilder");
        DhtClientFactory dhtClientFactory = (DhtClientFactory) getBeanFromApplicationContext(CLUSTER_START_PORT, "dhtClientFactory");
        PublicIpAllocationIndex seededIndex = new PublicIpAllocationIndex();
        Set<ResourceRange> aIpAddressRanges = new TreeSet<ResourceRange>();
        aIpAddressRanges.add(new ResourceRange(IpAddressUtils.ipToLong(VALID_PUBLIC_ADDRESS_1), IpAddressUtils.ipToLong(VALID_PUBLIC_ADDRESS_2)));
        seededIndex.setResourceRanges(aIpAddressRanges);
        seededIndex.setInactiveResourceConsumerTimeoutSec(3600L);
        BlockingDhtWriter blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion(), seededIndex, new UpdateResolver<PublicIpAllocationIndex>() {
            @Override
            public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                if (null != existingEntity)
                    requestedEntity.setVersion(existingEntity.getVersion() + 1);
                return requestedEntity;
            }
        });
    }

    private static void printNodeLeafsetSizes() {
        for (ApplicationContext applicationContext : applicationContexts.values())
            System.err.println(String.format("Leafset size for node %s: %d", ((KoalaNode) applicationContext.getBean("koalaNode")).getPastryNode().getNodeId().toStringFull(), ((KoalaNode) applicationContext.getBean("koalaNode")).getLeafNodeHandles()
                    .size()));
    }

    private static void waitForAllApplicationsToBecomeActiveInAllContexts() throws Exception {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            System.err.println(String.format("Waiting for applications to become active in region %d, avz %d, port %d", ((KoalaIdFactory) applicationContext.getBean("koalaIdFactory")).getRegion(), ((KoalaIdFactory) applicationContext
                    .getBean("koalaIdFactory")).getAvailabilityZoneWithinRegion(), ((KoalaNode) applicationContext.getBean("koalaNode")).getPort()));

            waitForAllApplicationsToBecomeActiveInContext(applicationContext);
        }
    }

    private static void waitForAllApplicationsToBecomeActiveInContext(ApplicationContext applicationContext) throws Exception {
        waitForApplication(applicationContext, NetworkManagerApplication.APPLICATION_NAME, NodeScope.AVAILABILITY_ZONE);
    }

    protected static void waitForApplication(ApplicationContext applicationContext, String applicationName, NodeScope scope) throws Exception {
        System.err.println(String.format("waitForApplication(%s)", applicationName));

        KoalaIdFactory koalaIdFactory = (KoalaIdFactory) applicationContext.getBean("koalaIdFactory");
        PId entityId = null;
        if (NodeScope.REGION.equals(scope)) {
            entityId = koalaIdFactory.buildPId(RegionScopedApplicationRecord.getUrl(applicationName)).forLocalRegion();
        } else
            entityId = koalaIdFactory.buildPId(AvailabilityZoneScopedApplicationRecord.getUrl(applicationName)).forLocalAvailabilityZone();
        waitForEntity(applicationContext, entityId, new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                boolean retry = true;
                ApplicationRecord applicationRecord = (ApplicationRecord) entity;
                if (applicationRecord != null && applicationRecord.getNumCurrentlyActiveNodes() > 0) {
                    retry = false;
                }
                return retry;
            }
        }, 30, 2 * 1000);
    }

    protected static void waitForEntity(final ApplicationContext applicationContext, final PId entityId, final EntityChecker checker, int retries, long retryInterval) throws Exception {
        retry(new Retrier() {
            @Override
            public boolean shouldRetry() {
                DhtClientFactory dhtClientFactory = (DhtClientFactory) applicationContext.getBean("dhtClientFactory");
                BlockingDhtReader dhtReader = dhtClientFactory.createBlockingReader();
                return checker.shouldRetry(dhtReader.get(entityId));
            }
        }, retries, retryInterval);
    }

    protected static void retry(Retrier retrier, int retries, long retryInterval) throws Exception {
        if (retrier.shouldRetry()) {
            System.err.println("Retries left: " + retries);
            if (retries > 0) {
                Thread.sleep(retryInterval);
                retry(retrier, retries - 1, retryInterval);
            } else
                fail("Exceeded number of retries");
        }
    }

    private static Object getBeanFromApplicationContext(int port, String beanName) {
        return applicationContexts.get(String.format("%d", port)).getBean(beanName);
    }

    private static void startNewNode(int port) {
        System.err.println(String.format("Starting new node with port %d", port));

        AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext(COMMON_APP_CTX);

        String key = String.format("%d", port);
        System.err.println(String.format("Storing application context in map with key %s", key));
        applicationContexts.put(key, applicationContext);

        System.err.println(String.format("Starting up node on port %s", port));
        KoalaNode koalaNode = applicationContext.getBean(KoalaNode.class);
        koalaNode.start();
    }

    protected void verifyCalledEventually(int timeoutSecs, Runnable r) {
        for (int i = 0; i < timeoutSecs; i++) {
            try {
                r.run();
                System.err.println("Verified invocation after " + i + " sec");
                return; // succeeded
            } catch (Throwable t) {
                if (t instanceof java.lang.AssertionError || t instanceof WantedButNotInvoked || t instanceof org.mockito.exceptions.verification.ArgumentsAreDifferent || t instanceof org.mockito.exceptions.verification.junit.ArgumentsAreDifferent) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ee) {
                        throw new RuntimeException(ee);
                    }
                } else {
                    System.err.println(t.getClass().getName() + ": ");
                    t.printStackTrace();
                    throw new RuntimeException(t);
                }
            }
        }
        throw new RuntimeException("verifyCalledEventually failed after " + timeoutSecs + " seconds");
    }

    protected void changeContextToNode(String liveNodePort) {
        System.err.println("changeContextToNode " + liveNodePort);
        currentApplicationContext = applicationContexts.get(liveNodePort);
        currentNetworkManagerApplication = (NetworkManagerApplication) currentApplicationContext.getBean("networkManagerApplication");
        currentKoalaNode = (KoalaNode) currentApplicationContext.getBean("koalaNode");
        currentPastryNode = currentKoalaNode.getPastryNode();
        currentDhtClientFactory = (DhtClientFactory) currentApplicationContext.getBean("dhtClientFactory");
        currentPiIdBuilder = (PiIdBuilder) currentApplicationContext.getBean("piIdBuilder");
        currentTaskProcessingQueueHelper = (TaskProcessingQueueHelper) currentApplicationContext.getBean("taskProcessingQueueHelper");
        currentSecurityGroupUpdateTaskQueueWatcherInitiator = (SecurityGroupUpdateTaskQueueWatcherInitiator) currentApplicationContext.getBean("securityGroupUpdateTaskQueueWatcherInitiator");
        currentSecurityGroupDeleteTaskQueueWatcherInitiator = (SecurityGroupDeleteTaskQueueWatcherInitiator) currentApplicationContext.getBean("securityGroupDeleteTaskQueueWatcherInitiator");
        currentAssociateAddressTaskQueueWatcherInitiator = (AssociateAddressTaskQueueWatcherInitiator) currentApplicationContext.getBean("associateAddressTaskQueueWatcherInitiator");
        currentDisassociateAddressTaskQueueWatcherInitiator = (DisassociateAddressTaskQueueWatcherInitiator) currentApplicationContext.getBean("disassociateAddressTaskQueueWatcherInitiator");
        currentInstanceNetworkManagerTeardownTaskQueueWatcherInitiator = (InstanceNetworkManagerTeardownTaskQueueWatcherInitiator) currentApplicationContext.getBean("instanceNetworkManagerTeardownTaskQueueWatcherInitiator");
        currentApplicationRegistry = (ApplicationRegistry) currentApplicationContext.getBean("applicationRegistry");

        currentConsumedDhtResourceRegistry = (ConsumedDhtResourceRegistry) currentApplicationContext.getBean("consumedDhtResourceRegistry");
        currentSpiedOnConsumedDhtResourceRegistry = spy(currentConsumedDhtResourceRegistry);
        SecurityGroupNetworkUpdateHandler securityGroupNetworkUpdateHandler = (SecurityGroupNetworkUpdateHandler) currentApplicationContext.getBean("securityGroupNetworkUpdateHandler");
        securityGroupNetworkUpdateHandler.setConsumedDhtResourceRegistry(currentSpiedOnConsumedDhtResourceRegistry);
        currentConsumedDhtResourceRegistry = currentSpiedOnConsumedDhtResourceRegistry;

        NetworkCleanupHandler networkCleanupHandler = currentApplicationContext.getBean(NetworkCleanupHandler.class);
        networkCleanupHandler.setConsumedDhtResourceRegistry(currentSpiedOnConsumedDhtResourceRegistry);
    }

    protected String getLiveNetworkAppNodePort() {
        for (Entry<String, AbstractApplicationContext> entry : applicationContexts.entrySet()) {
            NetworkManagerApplication networkManagerApplication = (NetworkManagerApplication) entry.getValue().getBean("networkManagerApplication");
            ApplicationStatus networkApplicationStatus = networkManagerApplication.getApplicationActivator().getApplicationStatus(networkManagerApplication.getApplicationName());
            KoalaNode koalaNode = (KoalaNode) entry.getValue().getBean("koalaNode");
            PastryNode pastryNode = koalaNode.getPastryNode();
            System.err.println(String.format("Node %s(%s) with app %s has the status %s", pastryNode.getNodeId().toStringFull(), entry.getKey(), networkManagerApplication, networkApplicationStatus));
            if (ApplicationStatus.ACTIVE == networkApplicationStatus) {
                return entry.getKey();
            }
        }
        return null;
    }

    protected void stopNode(String port) {
        AbstractApplicationContext applicationContext = applicationContexts.remove(port);
        if (null == applicationContext)
            return;
        applicationContext.destroy();
    }

    protected void verifyCommand(String toMatch) {
        StubCommandExecutor stubCommandExecutor = (StubCommandExecutor) currentApplicationContext.getBean("stubCommandExecutor");
        assertTrue(stubCommandExecutor.assertCommand(toMatch.split(" ")));
    }

    protected void waitForLiveNetworkManager() throws InterruptedException {
        int liveNetworkManagerCount = getLiveNetworkManagerCount();
        for (int i = 0; i < 50 && liveNetworkManagerCount < 1; i++) {
            Thread.sleep(400L);
            liveNetworkManagerCount = getLiveNetworkManagerCount();
        }
        if (liveNetworkManagerCount < 1)
            fail("no live Network Managers found");
    }

    protected int getLiveNetworkManagerCount() {
        int liveNetworkManagerCount = 0;
        for (Entry<String, AbstractApplicationContext> entry : applicationContexts.entrySet()) {
            NetworkManagerApplication networkManagerApplication = (NetworkManagerApplication) entry.getValue().getBean("networkManagerApplication");
            ApplicationStatus networkApplicationStatus = networkManagerApplication.getApplicationActivator().getApplicationStatus(networkManagerApplication.getApplicationName());
            KoalaNode koalaNode = (KoalaNode) entry.getValue().getBean("koalaNode");
            PastryNode pastryNode = koalaNode.getPastryNode();
            System.err.println(String.format("Node %s(%s) with app %s has the status %s", pastryNode.getNodeId().toStringFull(), entry.getKey(), networkManagerApplication, networkApplicationStatus));
            if (ApplicationStatus.ACTIVE == networkApplicationStatus) {
                liveNetworkManagerCount++;
            }
        }
        return liveNetworkManagerCount;
    }

    interface EntityChecker {
        boolean shouldRetry(PiEntity entity);
    }

    interface Retrier {
        boolean shouldRetry();
    }
}
