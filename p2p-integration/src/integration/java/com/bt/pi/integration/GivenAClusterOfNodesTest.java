package com.bt.pi.integration;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.util.ReflectionUtils;

import com.bt.pi.api.service.ApiApplicationManager;
import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.handlers.AnycastHandler;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.app.instancemanager.reporting.InstanceReportEntity;
import com.bt.pi.app.instancemanager.reporting.InstanceReportEntityCollection;
import com.bt.pi.app.instancemanager.testing.StubLibvirtConnection;
import com.bt.pi.app.management.PiSeeder;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.app.volumemanager.AttachVolumeTaskQueueWatcherInitiator;
import com.bt.pi.app.volumemanager.VolumeManagerApplication;
import com.bt.pi.core.Main;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.application.activation.TimeStampedPair;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.testing.StubCommandExecutor;
import com.bt.pi.integration.applications.Avz1Application;
import com.bt.pi.integration.applications.RegionScopedTestApplication;
import com.bt.pi.integration.applications.TestApplication;
import com.bt.pi.integration.util.BeanPropertiesMunger;
import com.bt.pi.integration.util.StubIntegrationCommandRunner;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;
import com.bt.pi.ops.website.entities.AvailableResources;
import com.bt.pi.sss.PisssApplicationManager;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.ImageDescription;
import com.xerox.amazonws.ec2.InstanceType;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.VolumeInfo;

public class GivenAClusterOfNodesTest extends IntegrationTestBase {
    // THIS MUST MATCH max.cores PROPERTY.
    private static final int CORES_PER_NODE = 1024;
    private static final String LOCALHOST = "127.0.0.1";
    private final static Log LOG = LogFactory.getLog(GivenAClusterOfNodesTest.class);
    public static final int CLUSTER_START_PORT = 5050;

    // ****** NODE IDs have been chosen to get a specific distribution in id space. BE VERY CAREFUL IF
    // CHANGING ****
    private static final String[] NODE_IDS_IN_AVAILABILITYZONE = new String[] { "000000000000000000000000000000000000", "580000000000000000000000000000000000", "AF0000000000000000000000000000000000" };

    protected static SortedMap<String, AbstractApplicationContext> applicationContexts;
    private AvailableResources availableResources;
    private Jec2 ec2;
    private long repeatingQueueWatcherIntervalMillis;
    private int staleQueueItemMillis;
    private long initialQueueWatcherIntervalMillis;

    @BeforeClass
    public static void startCluster() throws Exception {
        LOG.info("-------------------------------------" + new Date() + "--------------------------------");
        PrintStream sysout = System.out;
        PrintStream syserr = System.err;

        BeanPropertiesMunger.setDoMunging(true);
        applicationContexts = new TreeMap<String, AbstractApplicationContext>();

        int apiPort = 8773;
        int apiHttpsPort = 4443;
        int pisssHttpPort = 9090;
        int pisssHttpsPort = 8883;
        int opsPort = 8443;
        int groovyPort = 20000;
        int port = CLUSTER_START_PORT;

        for (int i = 0; i < REGIONS.length; i++) {
            for (int j = 0; j < AVAILABILITY_ZONES.length; j++) {
                int bootstrapPort = port;
                BeanPropertiesMunger.setRegionAndAvailabilityZone(Integer.parseInt(REGIONS[i][1]), Integer.parseInt(AVAILABILITY_ZONES[j][1]));

                for (int k = 0; k < NODE_IDS_IN_AVAILABILITYZONE.length; k++) {
                    MDC.put("NODE_ID", port);

                    String nodeId = String.format("%02X%02X%s", Integer.parseInt(REGIONS[i][1]), Integer.parseInt(AVAILABILITY_ZONES[j][1]), NODE_IDS_IN_AVAILABILITYZONE[k]);
                    System.err.println("Generating node id: " + nodeId);

                    BeanPropertiesMunger.setNodeId(nodeId);
                    BeanPropertiesMunger.setPortAndBootstrapPort(port, k == 0 ? CLUSTER_START_PORT : bootstrapPort);
                    BeanPropertiesMunger.setApplicationPorts(apiPort++, apiHttpsPort++, pisssHttpPort++, pisssHttpsPort++, opsPort++, groovyPort++);
                    startNewNode(port++);
                    // BeanPropertiesMunger.resetRegionAndAvailabilityZone();

                    if (i == 0 && j == 0 && k == 0) {
                        System.setOut(sysout);
                        System.setErr(syserr);
                    }
                    Thread.sleep(10 * 1000);
                }
            }
        }

        System.err.println("Printing out leafsets for all nodes");
        printNodeLeafsetSizes();

        System.err.println("All nodes are now up, seeding the system");
        seedSystem((PiSeeder) getBeanFromApplicationContext(CLUSTER_START_PORT, "piSeeder"), (UserManagementService) getBeanFromApplicationContext(CLUSTER_START_PORT, "userManagementService"));
        seedTestApplications((PiSeeder) getBeanFromApplicationContext(CLUSTER_START_PORT, "piSeeder"), applicationContexts.get(String.format("%d", CLUSTER_START_PORT)));
        refreshInstanceTypesInAnycastHandlerAndSubscribeToRunInstance();
        waitForAllApplicationsToBecomeActiveInAllContexts();
        System.setProperty("javax.net.ssl.trustStore", "src/integration/resources/ssl_keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "badgermonkeyfish");
    }

    @AfterClass
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

            FileUtils.deleteDirectory(new File("tempCerts"));
        }

        FileUtils.deleteQuietly(new File("var"));
    }

    @Before
    public void logTestName() throws Exception {
        LOG.info("running test " + testName.getMethodName());
        System.err.println("running test " + testName.getMethodName());
    }

    @Before
    public void clearStubLibvirtConnection() {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            StubLibvirtConnection stubLibvirtConnection = (StubLibvirtConnection) applicationContext.getBean("libvirtConnection");
            stubLibvirtConnection.reset();
        }
    }

    @Before
    public void storeRefreshIntervals() throws Exception {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            AttachVolumeTaskQueueWatcherInitiator taskInitiator = applicationContext.getBean(AttachVolumeTaskQueueWatcherInitiator.class);
            initialQueueWatcherIntervalMillis = taskInitiator.getInitialQueueWatcherIntervalMillis();
            staleQueueItemMillis = taskInitiator.getStaleQueueItemMillis();
            repeatingQueueWatcherIntervalMillis = taskInitiator.getRepeatingQueueWatcherIntervalMillis();
        }
    }

    @After
    public void restoreRefreshIntervals() throws Exception {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            AttachVolumeTaskQueueWatcherInitiator taskInitiator = applicationContext.getBean(AttachVolumeTaskQueueWatcherInitiator.class);
            taskInitiator.setInitialQueueWatcherIntervalMillis(initialQueueWatcherIntervalMillis);
            taskInitiator.setStaleQueueItemMillis(staleQueueItemMillis);
            taskInitiator.setRepeatingQueueWatcherIntervalMillis(repeatingQueueWatcherIntervalMillis);
        }
    }

    @After
    public void resetSecurityGroup() {
        launchConfig.setSecurityGroup(Arrays.asList("default"));
    }

    @After
    public void resetCommandRunner() {
        setDelayOnStubIntegrationCommandRunners(0);
        setFailureOnStubIntegrationCommandRunners(false);
    }

    private static void printNodeLeafsetSizes() {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            System.err.println(String.format("Leafset size for node with port %d and id %s: %d", ((KoalaNode) applicationContext.getBean("koalaNode")).getPort(), ((KoalaNode) applicationContext.getBean("koalaNode")).getPastryNode().getNodeId()
                    .toStringFull(), ((KoalaNode) applicationContext.getBean("koalaNode")).getLeafNodeHandles().size()));
        }

    }

    private static void waitForAllApplicationsToBecomeActiveInAllContexts() throws Exception {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            System.err.println(String.format("Waiting for applications to become active in region %d, avz %d, port %d", ((KoalaIdFactory) applicationContext.getBean("koalaIdFactory")).getRegion(), ((KoalaIdFactory) applicationContext
                    .getBean("koalaIdFactory")).getAvailabilityZoneWithinRegion(), ((KoalaNode) applicationContext.getBean("koalaNode")).getPort()));

            int regionCode = ((KoalaIdFactory) applicationContext.getBean("koalaIdFactory")).getRegion();
            int availabilityZoneWithinRegion = ((KoalaIdFactory) applicationContext.getBean("koalaIdFactory")).getAvailabilityZoneWithinRegion();
            int port = ((KoalaNode) applicationContext.getBean("koalaNode")).getPort();

            System.err.println(String.format("Waiting for applications to become active in region %d, avz %d, port %d", regionCode, availabilityZoneWithinRegion, port));

            waitForAllApplicationsToBecomeActiveInContext(applicationContext);
        }
    }

    private static void waitForAllApplicationsToBecomeActiveInContext(ApplicationContext applicationContext) throws Exception {
        waitForApplication(applicationContext, ApiApplicationManager.APPLICATION_NAME, NodeScope.REGION, 1);
        waitForApplication(applicationContext, PisssApplicationManager.APPLICATION_NAME, NodeScope.REGION, 1);
        waitForApplication(applicationContext, OpsWebsiteApplicationManager.APPLICATION_NAME, NodeScope.REGION, 1);
        waitForApplication(applicationContext, NetworkManagerApplication.APPLICATION_NAME, NodeScope.AVAILABILITY_ZONE, 2);

        Map<String, TestApplication> testApplications = applicationContext.getBeansOfType(TestApplication.class);
        for (TestApplication testApplication : testApplications.values()) {
            if (testApplication instanceof RegionScopedTestApplication)
                waitForApplication(applicationContext, testApplication.getApplicationName(), NodeScope.REGION, 1);
            else
                waitForApplication(applicationContext, testApplication.getApplicationName(), NodeScope.AVAILABILITY_ZONE, 1);
        }

    }

    private static void seedTestApplications(PiSeeder piSeeder, ApplicationContext applicationContext) {
        Map<String, TestApplication> testApplications = applicationContext.getBeansOfType(TestApplication.class);
        for (TestApplication testApplication : testApplications.values()) {
            for (String[] region : REGIONS) {
                if (testApplication instanceof RegionScopedTestApplication) {
                    boolean testAppRecordAdded = piSeeder.createApplicationRecordForRegion(testApplication.getApplicationName(), region[0], LOCALHOST);
                    System.err.println(String.format("Added Region %s ; %s - application record created: %s", region[0], testApplication.getApplicationName(), testAppRecordAdded));
                } else {
                    for (String[] avz : AVAILABILITY_ZONES) {
                        boolean testAppRecordAdded = piSeeder.createApplicationRecordForAvailabilityZone(testApplication.getApplicationName(), region[0] + avz[0], LOCALHOST);
                        System.err.println(String.format("Added avz %s ; %s - application record created: %s", region[0] + avz[0], testApplication.getApplicationName(), testAppRecordAdded));
                    }
                }
            }
        }
    }

    private static void refreshInstanceTypesInAnycastHandlerAndSubscribeToRunInstance() {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            InstanceManagerApplication instanceManagerApplication = (InstanceManagerApplication) applicationContext.getBean("instanceManagerApplication");
            instanceManagerApplication.checkCapacityAndSubscribeUnSubscribe();

            AnycastHandler anycastHandler = (AnycastHandler) applicationContext.getBean("anycastHandler");
            anycastHandler.refreshInstanceTypes();
        }
    }

    private static Object getBeanFromApplicationContext(int port, String beanName) {
        return applicationContexts.get(String.format("%d", port)).getBean(beanName);
    }

    private static void startNewNode(int port) {
        System.err.println(String.format("Starting new node with port %d", port));

        String[] args = new String[] { String.format("-a^%s", localhostStr), "-x applicationContext-p2p-app-e2e.xml" };
        Main nodeStarter = new Main();
        AbstractApplicationContext applicationContext = nodeStarter.init(args, false);

        String key = String.format("%d", port);
        System.err.println(String.format("Storing application context in map with key %s", key));
        applicationContexts.put(key, applicationContext);

        System.err.println(String.format("Starting up node on port %s", port));
        nodeStarter.start();
    }

    @Ignore
    @Test
    public void printOutRegionsAndAvailabilityZonesOfTheNodes() throws Exception {
        for (Entry<String, AbstractApplicationContext> entry : applicationContexts.entrySet()) {
            AbstractApplicationContext context = entry.getValue();

            String nodeId = FileUtils.readFileToString(new File(String.format("nodeIdFile%d", ((KoalaNode) context.getBean("koalaNode")).getPort())));
            int region = ((KoalaIdFactory) context.getBean("koalaIdFactory")).getRegion();
            int avz = ((KoalaIdFactory) context.getBean("koalaIdFactory")).getAvailabilityZoneWithinRegion();

            System.err.println(String.format("MapKey: %s, Region: %d, Avz: %d, NodeId: %s", entry.getKey(), region, avz, nodeId));
        }
    }

    @Test
    public void runInstance() throws Exception {
        ec2 = new Jec2(accessKey, secretKey, false, piHost, getApiPortForRegion("jupiter"));

        String region = "jupiter";
        String zone = getAvailabilityZoneNameWhereApiManagerIsRunning(region).equals("-1a") ? "-1b" : "-1a";
        ApplicationContext applicationContext = getApplicationContext(region, zone);
        launchConfig.setAvailabilityZone(region + zone);

        // act
        ReservationDescription result = ec2.runInstances(launchConfig);
        String instanceId = result.getInstances().get(0).getInstanceId();

        waitForInstanceToBeCreated(applicationContext, instanceId);
        waitForInstanceToBeRunning(ec2, instanceId);

        // assert
        DhtClientFactory dhtClientFactory = (DhtClientFactory) applicationContext.getBean("dhtClientFactory");
        PiIdBuilder piIdBuilder = (PiIdBuilder) applicationContext.getBean("piIdBuilder");
        Instance instance = (Instance) dhtClientFactory.createBlockingReader().get(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId)));
        assertEquals(InstanceState.RUNNING, instance.getState());
    }

    @Test
    public void attachVolumeWorksWhenSerialExecutorCommandIsDelayed() throws Exception {
        // setup
        ec2 = new Jec2(accessKey, secretKey, false, piHost, getApiPortForRegion("jupiter"));

        String region = "jupiter";
        String zone = getAvailabilityZoneNameWhereApiManagerIsRunning(region).equals("-1a") ? "-1b" : "-1a";
        launchConfig.setAvailabilityZone(region + zone);

        ec2.createSecurityGroup("attach", "test security group");
        launchConfig.setSecurityGroup(Arrays.asList("attach"));

        // act
        ReservationDescription result = ec2.runInstances(launchConfig);
        String instanceId = result.getInstances().get(0).getInstanceId();

        waitForInstanceToBeRunning(ec2, instanceId);

        String size = "2";
        String snapshotId = null;
        VolumeInfo volume = ec2.createVolume(size, snapshotId, region + zone);
        waitForVolumeStatus(volume.getVolumeId(), VolumeState.AVAILABLE, 20);

        setDelayOnStubIntegrationCommandRunners(6);

        String device = "/dev/sdb";
        ec2.attachVolume(volume.getVolumeId(), instanceId, device);

        updateQueueWatchersToPoll(2);
        waitForVolumeStatus(volume.getVolumeId(), VolumeState.IN_USE, 30);

        assertEquals(1, numberOfTimesAttachVolumeCopyCommandWasExecuted(volume.getVolumeId()));
    }

    @Test
    public void attachVolumeWorksWhenRetriedOffAQueue() throws Exception {
        // setup
        ec2 = new Jec2(accessKey, secretKey, false, piHost, getApiPortForRegion("jupiter"));

        String region = "jupiter";
        String zone = getAvailabilityZoneNameWhereApiManagerIsRunning(region).equals("-1a") ? "-1b" : "-1a";
        ApplicationContext applicationContext = getApplicationContext(region, zone);
        launchConfig.setAvailabilityZone(region + zone);

        ec2.createSecurityGroup("attachQueue", "test security group");
        launchConfig.setSecurityGroup(Arrays.asList("attachQueue"));

        // act
        ReservationDescription result = ec2.runInstances(launchConfig);
        String instanceId = result.getInstances().get(0).getInstanceId();

        waitForInstanceToBeRunning(ec2, instanceId);

        String size = "2";
        String snapshotId = null;
        final VolumeInfo volume = ec2.createVolume(size, snapshotId, region + zone);
        waitForVolumeStatus(volume.getVolumeId(), VolumeState.AVAILABLE, 20);

        setFailureOnStubIntegrationCommandRunners(true);

        String device = "/dev/sdb";
        ec2.attachVolume(volume.getVolumeId(), instanceId, device);

        updateQueueWatchersToPoll(2);

        retry(new Retrier() {
            @Override
            public boolean shouldRetry() {
                int count = numberOfTimesAttachVolumeCopyCommandWasExecuted(volume.getVolumeId());
                return count < 1;
            }
        }, 10, 500);

        setFailureOnStubIntegrationCommandRunners(false);

        waitForVolumeStatus(volume.getVolumeId(), VolumeState.IN_USE, 20);

        assertTrue(numberOfTimesAttachVolumeCopyCommandWasExecuted(volume.getVolumeId()) >= 2);
    }

    private int numberOfTimesAttachVolumeCopyCommandWasExecuted(String volumeId) {
        int count = 0;
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            StubCommandExecutor stubCommandExecutor = applicationContext.getBean(StubCommandExecutor.class);
            Collection<String[]> commands = new ArrayList<String[]>(stubCommandExecutor.getCommands());
            for (String[] command : commands) {
                if (ArrayUtils.isEquals(command, String.format("nice -n +10 ionice -c3 cp var/volumes/remote/%s var/volumes/local/%s", volumeId, volumeId).split(" ")))
                    count++;
            }
        }
        return count;
    }

    private void updateQueueWatchersToPoll(int numberOfSeconds) throws Exception {
        Method method = BeanUtils.findMethod(VolumeManagerApplication.class, "iAmAQueueWatchingApplication", int.class, int.class, NodeScope.class);
        ReflectionUtils.makeAccessible(method);
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            boolean amIAQueueWatchingApplication = (Boolean) method.invoke(applicationContext.getBean(VolumeManagerApplication.class), 2, 3, NodeScope.AVAILABILITY_ZONE);
            if (!amIAQueueWatchingApplication)
                continue;

            AttachVolumeTaskQueueWatcherInitiator taskInitiator = applicationContext.getBean(AttachVolumeTaskQueueWatcherInitiator.class);
            taskInitiator.setInitialQueueWatcherIntervalMillis(0);
            taskInitiator.setStaleQueueItemMillis(1);
            taskInitiator.setRepeatingQueueWatcherIntervalMillis(numberOfSeconds);
            taskInitiator.createTaskProcessingQueueWatcher(applicationContext.getBean(KoalaNode.class).getPastryNode().getNodeId().toStringFull());
        }
    }

    private void setFailureOnStubIntegrationCommandRunners(boolean failure) {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            StubIntegrationCommandRunner commandRunner = applicationContext.getBean(StubIntegrationCommandRunner.class);
            commandRunner.setFailure(failure);
        }
    }

    private void setDelayOnStubIntegrationCommandRunners(int delaySeconds) {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            StubIntegrationCommandRunner commandRunner = applicationContext.getBean(StubIntegrationCommandRunner.class);
            commandRunner.setDelaySeconds(delaySeconds);
        }
    }

    private void waitForVolumeStatus(String volumeId, VolumeState volumeState, int numberOfHalfSecondDelays) throws Exception {
        int count = 0;
        int delay = 500;
        while (count < numberOfHalfSecondDelays) {
            List<VolumeInfo> describeVolumes = ec2.describeVolumes(new String[] { volumeId });
            if (describeVolumes.size() > 0) {
                System.err.println(describeVolumes.get(0));
                if (volumeState.toString().equals(describeVolumes.get(0).getStatus()))
                    return;
            }
            count++;
            Thread.sleep(delay);
        }
        fail("volume not in " + volumeState + " status after " + (delay * numberOfHalfSecondDelays) + " millis");
    }

    @Test
    public void instancesInSameSecurityGroupShouldBeManagedBySameNetworkManager() throws Exception {
        // setup
        Jec2 ec2 = new Jec2(accessKey, secretKey, false, piHost, getApiPortForRegion("jupiter"));
        Map<String, List<String>> instanceIds = new HashMap<String, List<String>>();
        String region = "jupiter";

        String zone = getAvailabilityZoneNameWhereApiManagerIsRunning(region).equals("-1a") ? "-1b" : "-1a";
        ApplicationContext applicationContext = getApplicationContext(region, zone);
        System.err.println(String.format("LaunchConfig - setting availability zone: %s", region + zone));
        launchConfig.setAvailabilityZone(region + zone);

        // ****** security group names have been chosen to get a specific distribution in id space. BE VERY CAREFUL IF
        // CHANGING ****
        ec2.createSecurityGroup("test1", "test 1 security group");
        ec2.createSecurityGroup("test2", "test 2 security group");
        ec2.createSecurityGroup("test_3", "test_3 security group");

        // act
        launchConfig.setSecurityGroup(Arrays.asList("test1"));
        createInstances(ec2, instanceIds, applicationContext);
        launchConfig.setSecurityGroup(Arrays.asList("test2"));
        createInstances(ec2, instanceIds, applicationContext);
        launchConfig.setSecurityGroup(Arrays.asList("test_3"));
        createInstances(ec2, instanceIds, applicationContext);

        List<String> nodeIds = getActiveNetworkManagersInAvailabilityZone(region, "-1a", NetworkManagerApplication.APPLICATION_NAME);

        assertEquals(2, nodeIds.size());
        nodeIds.addAll(getActiveNetworkManagersInAvailabilityZone(region, "-1b", NetworkManagerApplication.APPLICATION_NAME));
        assertEquals(4, nodeIds.size());
        int networkManagersHavingSecurityGroups = 0;
        for (String nodeId : nodeIds) {
            ApplicationContext networkManagerApplicationContext = getApplicationContextForNodeId(nodeId);
            System.err.println(String.format("Looking at Network Manager Security Group for node id %s", nodeId));
            if (assertThatActiveNetworkManagerContainsAllInstancesOfSecurityGroup(networkManagerApplicationContext, instanceIds))
                networkManagersHavingSecurityGroups++;
        }
        assertTrue(instanceIds.isEmpty());
        assertEquals(2, networkManagersHavingSecurityGroups);
    }

    private boolean assertThatActiveNetworkManagerContainsAllInstancesOfSecurityGroup(ApplicationContext applicationContext, Map<String, List<String>> instanceIds) {
        boolean networkManagerOwnsSecurityGroup = false;
        PiIdBuilder piIdBuilder = applicationContext.getBean(PiIdBuilder.class);
        ConsumedDhtResourceRegistry consumedDhtResourceRegistry = applicationContext.getBean(ConsumedDhtResourceRegistry.class);
        for (String securityGroupName : new ArrayList<String>(instanceIds.keySet())) {
            PId securityGroupRecordId = piIdBuilder.getPId(SecurityGroup.getUrl(USER_ID, securityGroupName)).forLocalRegion();
            Set<String> consumers = consumedDhtResourceRegistry.getAllConsumers(securityGroupRecordId);

            if (consumers == null || consumers.isEmpty()) {
                System.err.println(String.format("Network Manager is not managing security group %s", securityGroupName));
            } else {
                networkManagerOwnsSecurityGroup = true;
                System.err.println(String.format("Network Manager looking at security group %s", securityGroupName));
                List<String> instanceIdsForSecurityGroup = instanceIds.get(securityGroupName);
                assertEquals(instanceIdsForSecurityGroup.size(), consumers.size());
                for (String instanceId : instanceIdsForSecurityGroup) {
                    assertTrue(String.format("Instance %s should be contained in consumers", instanceId), consumers.contains(instanceId));
                }
                instanceIds.remove(securityGroupName);
            }
        }

        return networkManagerOwnsSecurityGroup;
    }

    private void createInstances(Jec2 ec2, Map<String, List<String>> instanceIds, ApplicationContext applicationContext) throws EC2Exception, Exception {
        String securityGroup = launchConfig.getSecurityGroup().get(0);
        String securityGroupUrl = SecurityGroup.getUrl(USER_ID, securityGroup);
        instanceIds.put(securityGroup, new ArrayList<String>());
        for (int i = 0; i < 4; i++) {
            ReservationDescription result = ec2.runInstances(launchConfig);
            String instanceId = result.getInstances().get(0).getInstanceId();
            instanceIds.get(securityGroup).add(instanceId);

            waitForEntityToBeCreatedInSecurityGroup(applicationContext, instanceId, securityGroupUrl);
            waitForInstanceToBeRunning(ec2, instanceId);
        }
        assertEquals(4, instanceIds.get(securityGroup).size());
    }

    @Test
    public void reportInstanceInformation() throws Exception {
        // setup

        Jec2 ec2 = new Jec2(accessKey, secretKey, false, piHost, getApiPortForRegion("jupiter"));
        launchConfig.setAvailabilityZone("jupiter-1a");
        ec2.createSecurityGroup("report", "report");
        launchConfig.setSecurityGroup(Arrays.asList("report"));
        ReservationDescription instance1 = ec2.runInstances(launchConfig);
        ReservationDescription instance2 = ec2.runInstances(launchConfig);
        String instanceId1 = instance1.getInstances().get(0).getInstanceId();
        String instanceId2 = instance2.getInstances().get(0).getInstanceId();

        waitForInstanceToBeRunning(ec2, instanceId1);
        waitForInstanceToBeRunning(ec2, instanceId2);
        Thread.sleep(20000);

        // act
        HttpClient httpClient = logInClient();
        GetMethod getMethod = new GetMethod(String.format("https://localhost:%d/availabilityzones/jupiter-1a/instances/running", getOpsWebsitePortForRegion("jupiter")));
        int responseCode = httpClient.executeMethod(getMethod);

        // assert
        InstanceReportEntityCollection result = (InstanceReportEntityCollection) new KoalaJsonParser().getObject(getMethod.getResponseBodyAsString(), InstanceReportEntityCollection.class);
        Iterator<InstanceReportEntity> iterator = result.getEntities().iterator();
        boolean hasInstanceId1 = false, hasInstanceId2 = false;
        while (iterator.hasNext()) {
            InstanceReportEntity instanceReportEntity = iterator.next();
            hasInstanceId1 |= instanceReportEntity.getInstanceId().equals(instanceId1);
            hasInstanceId2 |= instanceReportEntity.getInstanceId().equals(instanceId2);
        }

        assertEquals(200, responseCode);
        assertTrue("Only " + result.getEntities().size() + " entities in result", result.getEntities().size() >= 2);
        assertTrue("Could not find instance id " + instanceId1, hasInstanceId1);
        assertTrue("Could not find instance id " + instanceId2, hasInstanceId2);

        // kill one of the existing instances
        ec2.terminateInstances(new String[] { instanceId2 });

        Thread.sleep(30000);
        // updateCacheTimeToLiveOnZoneNodes("jupiter", "-1a", 120);
        Thread.sleep(20000);

        // act
        httpClient = logInClient();
        getMethod = new GetMethod(String.format("https://localhost:%d/availabilityzones/jupiter-1a/instances/running", getOpsWebsitePortForRegion("jupiter")));
        responseCode = httpClient.executeMethod(getMethod);

        // assert
        result = (InstanceReportEntityCollection) new KoalaJsonParser().getObject(getMethod.getResponseBodyAsString(), InstanceReportEntityCollection.class);
        iterator = result.getEntities().iterator();
        hasInstanceId1 = false;
        hasInstanceId2 = false;
        while (iterator.hasNext()) {
            InstanceReportEntity instanceReportEntity = iterator.next();
            hasInstanceId1 |= instanceReportEntity.getInstanceId().equals(instanceId1);
            hasInstanceId2 |= instanceReportEntity.getInstanceId().equals(instanceId2);
        }

        assertEquals(200, responseCode);
        assertTrue("Only " + result.getEntities().size() + "entities in result", result.getEntities().size() >= 1);
        assertTrue("Could not find instance id " + instanceId1, hasInstanceId1);
        assertFalse("Should not find instance id " + instanceId2, hasInstanceId2);
    }

    @Test
    public void preferablyExcludedApplicationsShouldNotLoadOnTheSameNode() throws Exception {
        // setup
        System.err.println("Started test");
        Thread.sleep(5000);

        assertThatNoNodeIsRunningAnyTwoApplications();
    }

    private void assertThatNoNodeIsRunningAnyTwoApplications() {
        String test1NodeId = getApplicationRecord("testApplication11", NodeScope.AVAILABILITY_ZONE).getActiveNodeMap().get(LOCALHOST).getObject();
        String test2NodeId = getApplicationRecord("testApplication12", NodeScope.AVAILABILITY_ZONE).getActiveNodeMap().get(LOCALHOST).getObject();
        String test3NodeId = getApplicationRecord("testApplication13", NodeScope.AVAILABILITY_ZONE).getActiveNodeMap().get(LOCALHOST).getObject();

        Set<String> nodeIds = new HashSet<String>(Arrays.asList(new String[] { test1NodeId, test2NodeId, test3NodeId }));
        assertEquals(3, nodeIds.size());
    }

    @Test
    public void preferablyExcludedAppsShouldStillLoadIfTheNumberOfNodesIsLessThanNumberOfApps() throws Exception {
        // setup
        String[] applicationNames = new String[] { "testApplication1", "testApplication2", "testApplication3", "testApplication4", "testApplication5", "testApplication6", "testApplication7" };

        List<ApplicationRecord> testAppRecords = new ArrayList<ApplicationRecord>();
        for (String applicationName : applicationNames) {
            ApplicationRecord testApplicationRecord = getApplicationRecord(applicationName, NodeScope.REGION);
            testAppRecords.add(testApplicationRecord);
            System.err.println(testApplicationRecord);
        }

        // act and assert
        for (ApplicationRecord testApplicationRecord : testAppRecords) {
            String testNodeId = testApplicationRecord.getActiveNodeMap().get(LOCALHOST).getObject();
            System.err.println(testNodeId);
            assertNotNull(testNodeId);
        }
    }

    @Test
    public void shouldEventuallyMoveApplicationToAnotherNodeWhenSettingApplicationRecordToNull() throws Exception {
        int regionCode = Integer.parseInt(REGIONS[0][1]);
        int avz1Code = Integer.parseInt(AVAILABILITY_ZONES[0][1]);
        AbstractApplicationContext applicationContext = applicationContexts.entrySet().iterator().next().getValue();
        ApplicationRecord avz1AppRecord = getApplicationRecord(Avz1Application.APPLICATION_NAME, NodeScope.AVAILABILITY_ZONE, regionCode, avz1Code);
        String test1NodeId = avz1AppRecord.getActiveNodeMap().get(LOCALHOST).getObject();
        PId app1Avz1Pid = getPIdForApplication(applicationContext, Avz1Application.APPLICATION_NAME, NodeScope.AVAILABILITY_ZONE, regionCode, avz1Code);
        avz1AppRecord.removeActiveNode(test1NodeId);
        updateApplicationRecord(avz1AppRecord, app1Avz1Pid);
        waitForApplicationToChangeNodeId(applicationContexts.entrySet().iterator().next().getValue(), app1Avz1Pid, test1NodeId);

    }

    @Test
    public void shouldStartAvailabilityZoneScopedApplicationIntoTwoDifferentAvailabilityZones() throws Exception {
        // setup
        int regionCode = Integer.parseInt(REGIONS[0][1]);
        int avz1Code = Integer.parseInt(AVAILABILITY_ZONES[0][1]);
        int avz2Code = Integer.parseInt(AVAILABILITY_ZONES[1][1]);

        AbstractApplicationContext applicationContext = applicationContexts.entrySet().iterator().next().getValue();

        PId app1Avz1Pid = getPIdForApplication(applicationContext, Avz1Application.APPLICATION_NAME, NodeScope.AVAILABILITY_ZONE, regionCode, avz1Code);
        PId app1Avz2Pid = getPIdForApplication(applicationContext, Avz1Application.APPLICATION_NAME, NodeScope.AVAILABILITY_ZONE, regionCode, avz2Code);

        waitForApplicationWithPid(applicationContext, app1Avz1Pid, 1);
        waitForApplicationWithPid(applicationContext, app1Avz2Pid, 1);

        ApplicationRecord avz1ApplicationRecord1 = getApplicationRecord(Avz1Application.APPLICATION_NAME, NodeScope.AVAILABILITY_ZONE, regionCode, avz1Code);
        ApplicationRecord avz2ApplicationRecord1 = getApplicationRecord(Avz1Application.APPLICATION_NAME, NodeScope.AVAILABILITY_ZONE, regionCode, avz2Code);

        System.err.println(avz1ApplicationRecord1);
        System.err.println(avz2ApplicationRecord1);

        assertThat(avz1ApplicationRecord1.getNumCurrentlyActiveNodes(), is(1));
        assertThat(avz2ApplicationRecord1.getNumCurrentlyActiveNodes(), is(1));
    }

    @Test
    public void shouldChangeImagePlatform() throws Exception {
        // setup
        // use existing image: IMAGE_ID
        // assert that the image platform is set to linux
        Jec2 ec2 = new Jec2(accessKey, secretKey, false, piHost, getApiPortForRegion("jupiter"));
        launchConfig.setAvailabilityZone("jupiter-1a");
        List<ImageDescription> images = ec2.describeImages(new String[] { IMAGE_ID });
        assertEquals("linux", images.get(0).getPlatform().toString());

        // act
        // connect to the website
        HttpClient httpClient = logInClient();
        PostMethod postMethod = new PostMethod(String.format("https://localhost:%d/images/%s/platform", getOpsWebsitePortForRegion("jupiter"), IMAGE_ID));
        postMethod.addParameter("image_platform", "windows");

        int rc = httpClient.executeMethod(postMethod);
        System.err.println(rc);
        System.err.println(postMethod.getResponseBodyAsString());

        // assert
        // suspect that supernode caching might be slowing down the changing result
        int count = 0;
        String result = null;
        while (!"windows".equals(result) && count < 30) {
            Thread.sleep(2000);
            System.err.print(".");
            List<ImageDescription> res = ec2.describeImages(new String[] { IMAGE_ID });
            result = res.get(0).getPlatform().toString();
            count++;
        }
        System.err.println();
        assertEquals("windows", result);
    }

    @Test
    public void shouldConfigureANewSuperNodeApplicationFromOpsWebsite() throws HttpException, IOException {
        // setup
        String randomApplicationName = "pi-random-app";
        AbstractApplicationContext applicationContext = applicationContexts.entrySet().iterator().next().getValue();
        KoalaIdFactory koalaIdFactory = applicationContext.getBean(KoalaIdFactory.class);
        DhtClientFactory dhtClientFactory = applicationContext.getBean(DhtClientFactory.class);

        // setup http POST request
        HttpClient httpClient = logInClient();
        PostMethod postMethod = new PostMethod(String.format("https://localhost:%d/supernodes", getOpsWebsitePortForRegion("jupiter")));
        postMethod.addParameter("superNodeApplication", randomApplicationName);
        postMethod.addParameter("offset", "3");
        postMethod.addParameter("number", "16");

        // act
        httpClient.executeMethod(postMethod);

        // assert
        PId id = koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL);
        SuperNodeApplicationCheckPoints result = (SuperNodeApplicationCheckPoints) dhtClientFactory.createBlockingReader().get(id);
        assertEquals(16, result.getNumberOfSuperNodesPerApplication().get(randomApplicationName).intValue());
        assertEquals(3, result.getOffsetPerApplication().get(randomApplicationName).intValue());
    }

    @Test
    public void shouldConfigureAnExistingSuperNodeApplicationFromOpsWebsite() throws HttpException, IOException {
        // setup
        AbstractApplicationContext applicationContext = applicationContexts.entrySet().iterator().next().getValue();
        KoalaIdFactory koalaIdFactory = applicationContext.getBean(KoalaIdFactory.class);
        DhtClientFactory dhtClientFactory = applicationContext.getBean(DhtClientFactory.class);

        // setup http POST request
        HttpClient httpClient = logInClient();
        PostMethod postMethod = new PostMethod(String.format("https://localhost:%d/supernodes", getOpsWebsitePortForRegion("jupiter")));
        postMethod.addParameter("superNodeApplication", ReportingApplication.APPLICATION_NAME);
        postMethod.addParameter("offset", "2");
        postMethod.addParameter("number", "4");

        // act
        httpClient.executeMethod(postMethod);

        // assert
        PId id = koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL);
        SuperNodeApplicationCheckPoints result = (SuperNodeApplicationCheckPoints) dhtClientFactory.createBlockingReader().get(id);
        assertEquals(4, result.getNumberOfSuperNodesPerApplication().get(ReportingApplication.APPLICATION_NAME).intValue());
        assertEquals(2, result.getOffsetPerApplication().get(ReportingApplication.APPLICATION_NAME).intValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDeactivateApplicationFromWebsite() throws Exception {
        HttpClient httpClient = logInClient();
        GetMethod getMethod = new GetMethod(String.format("https://localhost:%d/applications/list", getOpsWebsitePortForRegion("jupiter")));
        httpClient.executeMethod(getMethod);
        // KoalaJsonParser is returning a list of linkedHashMap objects
        List<Map> applicationRecords = (List<Map>) new KoalaJsonParser().getObject(getMethod.getResponseBodyAsString(), List.class);
        System.err.println(applicationRecords);
        // find an active application
        String applicationName = null;
        String nodeId = null;
        PId applicationId = null;

        for (Map applicationRecordInfo : applicationRecords) {
            Map activeNodeMap = (Map) applicationRecordInfo.get("activeNodeMap");
            NodeScope nodeScope = NodeScope.valueOf((String) applicationRecordInfo.get("nodeScope"));
            if (activeNodeMap.get(LOCALHOST) != null && nodeScope.equals(NodeScope.REGION)) {
                applicationName = (String) applicationRecordInfo.get("applicationName");
                nodeId = (String) ((Map) (activeNodeMap).get(LOCALHOST)).get("object");
                applicationId = getPIdForApplication(applicationContexts.entrySet().iterator().next().getValue(), applicationName, nodeScope, Integer.parseInt((String) applicationRecordInfo.get("value")), 0);

                assertNotNull(applicationName);
                assertNotNull(nodeId);
                // Call deactivate method
                getMethod = new GetMethod(String.format("https://localhost:%d/applications/deactivate/" + applicationName + "/" + nodeId, getOpsWebsitePortForRegion("jupiter")));
                httpClient.executeMethod(getMethod);
                waitForApplicationToChangeNodeId(applicationContexts.entrySet().iterator().next().getValue(), applicationId, nodeId);
            }
        }
    }

    @Test
    public void shouldReturnAvailableResources() throws Exception {
        final String region = "jupiter";
        final int expectedNumberOfCores = NODE_IDS_IN_AVAILABILITYZONE.length * CORES_PER_NODE;
        final String zone = getAvailabilityZoneNameWhereApiManagerIsRunning(region).equals("-1a") ? "-1b" : "-1a";
        waitForInstanceTypesToBeCreated(getApplicationContext(region, zone));
        final HttpClient httpClient = logInClient();

        retry(new Retrier() {
            @Override
            public boolean shouldRetry() {
                try {
                    AvailableResources tempAvailableResources = getAvailableResources(region, zone, httpClient);
                    System.err.println("#########: " + tempAvailableResources.getFreeCores());
                    if (tempAvailableResources.getFreeCores() == expectedNumberOfCores) {
                        availableResources = tempAvailableResources;
                        return false;
                    }
                } catch (Exception e) {
                    LOG.info(e);
                }
                return true;
            }
        }, 30, 2 * 1000);
        System.err.println(new KoalaJsonParser().getJson(availableResources));

        Jec2 ec2 = new Jec2(accessKey, secretKey, false, piHost, getApiPortForRegion(region));

        ApplicationContext applicationContext = getApplicationContext(region, zone);
        launchConfig.setAvailabilityZone(region + zone);
        launchConfig.setInstanceType(InstanceType.LARGE);

        ec2.createSecurityGroup("resources", "resources");
        launchConfig.setSecurityGroup(Arrays.asList("resources"));

        // act
        ReservationDescription result = ec2.runInstances(launchConfig);
        String instanceId = result.getInstances().get(0).getInstanceId();

        waitForInstanceToBeRunning(ec2, instanceId);

        // assert
        retry(new Retrier() {
            @Override
            public boolean shouldRetry() {
                try {
                    AvailableResources availableResourcesAfter = getAvailableResources(region, zone, httpClient);
                    System.err.println(new KoalaJsonParser().getJson(availableResourcesAfter));
                    if (availableResourcesAfter.getFreeCores() == (availableResources.getFreeCores() - 1)) {
                        assertEquals(new Long(availableResources.getFreeMemoryInMB() - 2048), availableResourcesAfter.getFreeMemoryInMB());
                        return false;
                    }
                } catch (Exception e) {
                    LOG.info(e);
                }
                return true;
            }
        }, 30, 2 * 1000);
    }

    private AvailableResources getAvailableResources(final String region, final String zone, final HttpClient httpClient) throws IOException, HttpException {
        GetMethod getMethod = new GetMethod(String.format("https://localhost:%d/resources/%s%s", getOpsWebsitePortForRegion(region), region, zone));
        httpClient.executeMethod(getMethod);
        final AvailableResources availableResources = (AvailableResources) new KoalaJsonParser().getObject(getMethod.getResponseBodyAsString(), AvailableResources.class);
        return availableResources;
    }

    private PId getPIdForApplication(AbstractApplicationContext applicationContext, String applicationName, NodeScope nodeScope, int regionCode, int availabilityZoneInRegion) {

        KoalaIdFactory koalaIdFactory = applicationContext.getBean(KoalaIdFactory.class);

        PId appPid = null;
        if (nodeScope.equals(NodeScope.AVAILABILITY_ZONE))
            appPid = koalaIdFactory.buildPId(AvailabilityZoneScopedApplicationRecord.getUrl(applicationName)).forGlobalAvailablityZoneCode(PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(regionCode, availabilityZoneInRegion));
        else if (nodeScope.equals(NodeScope.REGION))
            appPid = koalaIdFactory.buildPId(RegionScopedApplicationRecord.getUrl(applicationName)).forRegion(regionCode);

        return appPid;
    }

    private ApplicationRecord getApplicationRecord(String applicationName, NodeScope nodeScope, int regionCode, int availabilityZoneInRegion) {
        AbstractApplicationContext applicationContext = applicationContexts.entrySet().iterator().next().getValue();

        KoalaIdFactory koalaIdFactory = applicationContext.getBean(KoalaIdFactory.class);

        PId appPid = null;
        if (nodeScope.equals(NodeScope.AVAILABILITY_ZONE))
            appPid = koalaIdFactory.buildPId(AvailabilityZoneScopedApplicationRecord.getUrl(applicationName)).forGlobalAvailablityZoneCode(PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(regionCode, availabilityZoneInRegion));

        DhtClientFactory dhtClientFactory = applicationContext.getBean(DhtClientFactory.class);
        ApplicationRecord applicationRecord = (ApplicationRecord) dhtClientFactory.createBlockingReader().get(appPid);
        return applicationRecord;
    }

    private ApplicationRecord getApplicationRecord(String applicationName, NodeScope nodeScope) {
        AbstractApplicationContext applicationContext = applicationContexts.entrySet().iterator().next().getValue();

        KoalaIdFactory koalaIdFactory = applicationContext.getBean(KoalaIdFactory.class);

        PId appPid = null;

        if (nodeScope.equals(NodeScope.REGION))
            appPid = koalaIdFactory.buildPId(RegionScopedApplicationRecord.getUrl(applicationName)).forLocalRegion();
        else if (nodeScope.equals(NodeScope.AVAILABILITY_ZONE))
            appPid = koalaIdFactory.buildPId(AvailabilityZoneScopedApplicationRecord.getUrl(applicationName)).forLocalAvailabilityZone();

        DhtClientFactory dhtClientFactory = applicationContext.getBean(DhtClientFactory.class);
        ApplicationRecord applicationRecord = (ApplicationRecord) dhtClientFactory.createBlockingReader().get(appPid);
        return applicationRecord;
    }

    private void updateApplicationRecord(ApplicationRecord applicationRecord, PId applicationId) {
        AbstractApplicationContext applicationContext = applicationContexts.entrySet().iterator().next().getValue();
        DhtClientFactory dhtClientFactory = applicationContext.getBean(DhtClientFactory.class);
        BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
        writer.update(applicationId, applicationRecord, new UpdateResolver<ApplicationRecord>() {

            @Override
            public ApplicationRecord update(ApplicationRecord existingEntity, ApplicationRecord requestedEntity) {
                if (existingEntity == null) {
                    fail("Application Record was not found");
                    return null;
                }

                return requestedEntity;
            }

        });
    }

    private HttpClient logInClient() {
        HttpClient cli = new HttpClient();
        cli.getParams().setAuthenticationPreemptive(true);
        Credentials credentials = new UsernamePasswordCredentials(MANAGEMENT_USERNAME, PASSWORD);
        cli.getState().setCredentials(AuthScope.ANY, credentials);
        return cli;
    }

    private Integer getApiPortForRegion(String regionName) {
        String nodeId = getNodeIdForActiveApiManagerInRegion(regionName);

        int port = 8773;
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            if (isApplicationContextWithinRegion(applicationContext, regionName) && nodeId.equals(((KoalaNode) applicationContext.getBean("koalaNode")).getPastryNode().getNodeId().toStringFull())) {
                System.err.println(String.format("Api port for region %s is %d", regionName, port));
                return port;
            }
            port++;
        }
        System.err.println("Could not find active api manager for region " + regionName);
        return null;
    }

    private Integer getOpsWebsitePortForRegion(String regionName) {
        String nodeId = getNodeIdForActiveOpsWebsiteManagerInRegion(regionName);

        int port = 8443;
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            if (isApplicationContextWithinRegion(applicationContext, regionName) && nodeId.equals(((KoalaNode) applicationContext.getBean("koalaNode")).getPastryNode().getNodeId().toStringFull())) {
                System.err.println(String.format("Ops website port for region %s is %d", regionName, port));
                return port;
            }
            port++;
        }
        System.err.println("Could not find active ops website manager for region " + regionName);
        return null;
    }

    private String getAvailabilityZoneNameWhereApiManagerIsRunning(String regionName) {
        String nodeId = getNodeIdForActiveApiManagerInRegion(regionName);
        String result = null;

        for (ApplicationContext applicationContext : applicationContexts.values()) {
            if (isApplicationContextWithinRegion(applicationContext, regionName) && nodeId.equals(((KoalaNode) applicationContext.getBean("koalaNode")).getPastryNode().getNodeId().toStringFull())) {
                result = getAvailabilityZoneNameFromAvailabilityZoneCode(String.format("%d", ((KoalaIdFactory) applicationContext.getBean("koalaIdFactory")).getAvailabilityZoneWithinRegion()));
                break;
            }
        }
        return result;
    }

    private String getNodeIdForActiveApiManagerInRegion(String regionName) {
        return getNodeIdForActiveApplicationInRegion(regionName, ApiApplicationManager.APPLICATION_NAME);
    }

    private String getNodeIdForActiveOpsWebsiteManagerInRegion(String regionName) {
        return getNodeIdForActiveApplicationInRegion(regionName, OpsWebsiteApplicationManager.APPLICATION_NAME);
    }

    private String getNodeIdForActiveApplicationInRegion(String regionName, String applicationName) {
        DhtClientFactory dhtClientFactory = (DhtClientFactory) applicationContexts.get(applicationContexts.firstKey()).getBean("dhtClientFactory");
        PiIdBuilder piIdBuilder = (PiIdBuilder) applicationContexts.get(applicationContexts.firstKey()).getBean("piIdBuilder");
        ApplicationRecord applicationRecord = (ApplicationRecord) dhtClientFactory.createBlockingReader().get(piIdBuilder.getPId(RegionScopedApplicationRecord.getUrl(applicationName)).forRegion(getRegionCodeFromRegionName(regionName)));
        String nodeId = applicationRecord.getActiveNodeMap().values().iterator().next().getObject();
        return nodeId;
    }

    private String getNodeIdForActiveApplicationInAvailabilityZone(String regionName, String availabilityZoneName, String applicationName) {
        DhtClientFactory dhtClientFactory = (DhtClientFactory) applicationContexts.get(applicationContexts.firstKey()).getBean("dhtClientFactory");
        PiIdBuilder piIdBuilder = (PiIdBuilder) applicationContexts.get(applicationContexts.firstKey()).getBean("piIdBuilder");
        int avzCode = PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(getRegionCodeFromRegionName(regionName), getAvailabilityZoneCodeFromAvailabilityZoneName(availabilityZoneName));
        ApplicationRecord applicationRecord = (ApplicationRecord) dhtClientFactory.createBlockingReader().get(piIdBuilder.getPId(AvailabilityZoneScopedApplicationRecord.getUrl(applicationName)).forGlobalAvailablityZoneCode(avzCode));

        String nodeId = applicationRecord.getActiveNodeMap().values().iterator().next().getObject();
        return nodeId;
    }

    private List<String> getActiveNetworkManagersInAvailabilityZone(String regionName, String availabilityZoneName, String applicationName) {
        DhtClientFactory dhtClientFactory = (DhtClientFactory) applicationContexts.get(applicationContexts.firstKey()).getBean("dhtClientFactory");
        PiIdBuilder piIdBuilder = (PiIdBuilder) applicationContexts.get(applicationContexts.firstKey()).getBean("piIdBuilder");
        int avzCode = PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(getRegionCodeFromRegionName(regionName), getAvailabilityZoneCodeFromAvailabilityZoneName(availabilityZoneName));
        ApplicationRecord applicationRecord = (ApplicationRecord) dhtClientFactory.createBlockingReader().get(piIdBuilder.getPId(AvailabilityZoneScopedApplicationRecord.getUrl(applicationName)).forGlobalAvailablityZoneCode(avzCode));
        List<String> nodeIds = new ArrayList<String>();
        Iterator<TimeStampedPair<String>> it = applicationRecord.getActiveNodeMap().values().iterator();
        while (it.hasNext()) {
            nodeIds.add(it.next().getObject());
        }

        return nodeIds;

    }

    private boolean isApplicationContextWithinRegion(ApplicationContext applicationContext, String regionName) {
        int regionCode = getRegionCodeFromRegionName(regionName);
        return ((KoalaIdFactory) applicationContext.getBean("koalaIdFactory")).getRegion() == regionCode;
    }

    private ApplicationContext getApplicationContext(String regionName, String availabilityZoneName) {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            int regionCode = getRegionCodeFromRegionName(regionName);
            int avzCode = getAvailabilityZoneCodeFromAvailabilityZoneName(availabilityZoneName);
            KoalaIdFactory koalaIdFactory = (KoalaIdFactory) applicationContext.getBean("koalaIdFactory");
            if (koalaIdFactory.getRegion() == regionCode && koalaIdFactory.getAvailabilityZoneWithinRegion() == avzCode)
                return applicationContext;
        }
        throw new IllegalArgumentException(String.format("No app context for region %s, avz %s", regionName, availabilityZoneName));
    }

    private ApplicationContext getApplicationContextForNodeId(String nodeId) {
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            KoalaNode koalaNode = applicationContext.getBean(KoalaNode.class);
            if (nodeId.equals(koalaNode.getPastryNode().getNodeId().toStringFull()))
                return applicationContext;
        }
        throw new IllegalArgumentException(String.format("No applicationContext for nodeid %s", nodeId));
    }

    private String getAvailabilityZoneNameFromAvailabilityZoneCode(String availabilityZoneCode) {
        for (String[] availabilityZone : AVAILABILITY_ZONES) {
            if (availabilityZone[1].equals(availabilityZoneCode))
                return availabilityZone[0];
        }
        throw new IllegalArgumentException("Unknown avz code: " + availabilityZoneCode);
    }

    private int getAvailabilityZoneCodeFromAvailabilityZoneName(String availabilityZoneName) {
        for (String[] availabilityZone : AVAILABILITY_ZONES) {
            if (availabilityZone[0].equals(availabilityZoneName))
                return Integer.parseInt(availabilityZone[1]);
        }
        throw new IllegalArgumentException("Unknown avz name: " + availabilityZoneName);
    }

    private int getRegionCodeFromRegionName(String regionName) {
        for (String[] region : REGIONS) {
            if (region[0].equals(regionName))
                return Integer.parseInt(region[1]);
        }
        throw new IllegalArgumentException("Unknown region name: " + regionName);
    }
}
