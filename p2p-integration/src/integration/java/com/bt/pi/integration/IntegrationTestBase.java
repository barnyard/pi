package com.bt.pi.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.AssertionFailedError;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.springframework.context.ApplicationContext;

import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.management.PiSeeder;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.integration.applications.Avz1Application;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.InstanceType;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;

public class IntegrationTestBase {
    private static final Log LOG = LogFactory.getLog(IntegrationTestBase.class);

    @Rule
    public TestName testName = new TestName();

    protected static final String[][] REGIONS = new String[][] { { "jupiter", "0", "jupiter.com" }, { "saturn", "128", "saturn.com" } };
    protected static final String[][] AVAILABILITY_ZONES = new String[][] { { "-1a", "0" }, { "-1b", "128" } };

    protected static final String MANAGEMENT_USERNAME = "user";
    protected static final String PASSWORD = "password";

    protected static final String USER_ID = "jon";
    private static final String SECURITY_GROUP_NAME = "default";
    protected static final String IMAGE_ID = "pmi-ApplePie";
    private static final String DEFAULT_RAMDISK = "pri-RhubarbPie";
    private static final String DEFAULT_KERNEL = "pki-PumpkinPie";
    private static final String DOT_PIRC_DIRECTORY = "tempCerts";

    protected static String secretKey;
    protected static String accessKey;
    protected static String piHost;
    protected static LaunchConfiguration launchConfig;

    public static String localhostStr;

    @BeforeClass
    public static void setupBeforeClass() throws Throwable {
        Security.addProvider(new BouncyCastleProvider());
        FileUtils.deleteQuietly(new File("var"));
        FileUtils.forceMkdir(new File("var/buckets"));
        FileUtils.forceMkdir(new File("var/images"));

        localhostStr = InetAddress.getLocalHost().getHostAddress();
        launchConfig = new LaunchConfiguration(IMAGE_ID);
    }

    protected static void seedSystem(PiSeeder piSeeder, UserManagementService userManagementService) throws Exception {
        piSeeder.configureNumberOfSuperNodes(ReportingApplication.APPLICATION_NAME, 2, 0);
        piSeeder.configureInstanceTypes(InstanceType.DEFAULT.getTypeId() + ";" + InstanceType.XLARGE_HCPU.getTypeId() + ";" + InstanceType.LARGE.getTypeId(), "0;0;1", "0;0;2048", "0;0;10");

        piSeeder.createManagementUser(MANAGEMENT_USERNAME, md5Hash(PASSWORD), "ROLE_OPS");
        userManagementService.createPiCertificate();

        for (int i = 0; i < REGIONS.length; i++) {
            seedRegion(piSeeder, REGIONS[i][0], REGIONS[i][1], REGIONS[i][2], String.format("10.0.0.%s-10.0.0.%s", i * 30 + 1, (i + 1) * 30), String.format("%s-%s", i * 10 + 30, i * 10 + 40), String.format("172.30.250.%s/24;16", i), String.format(
                    "147.149.2.%s", 5 + i), "127.0.0.1", "127.0.0.1", "127.0.0.1", "1");
        }

        createUserAndUseCertsAndEndpointsInPircFile(userManagementService);
    }

    private static void seedRegion(PiSeeder piSeeder, String regionName, String regionCode, String regionEndpoint, String publicIpAddressRangesString, String vlanIdRangesString, String subnetRangesString, String dnsAddress, String apiAppAddresses,
            String storageAppAddresses, String opsAppAddresses, String volumeBackupResources) {
        piSeeder.addRegion(regionName, regionCode, regionEndpoint, regionEndpoint, publicIpAddressRangesString, vlanIdRangesString, subnetRangesString, dnsAddress, apiAppAddresses, storageAppAddresses, opsAppAddresses, volumeBackupResources);

        piSeeder.addTaskProcessingQueuesForRegion(regionName);

        piSeeder.createImage(DEFAULT_RAMDISK, (String) null, (String) null, (String) null, (String) null, "i386", ImagePlatform.linux.toString(), true, MachineType.RAMDISK.toString(), regionName);
        piSeeder.createImage(DEFAULT_KERNEL, (String) null, (String) null, (String) null, (String) null, "i386", ImagePlatform.linux.toString(), true, MachineType.KERNEL.toString(), regionName);
        piSeeder.createImage(IMAGE_ID, DEFAULT_KERNEL, DEFAULT_RAMDISK, (String) null, (String) null, "i386", ImagePlatform.linux.toString(), true, MachineType.MACHINE.toString(), regionName);

        String avz1Name = regionName + AVAILABILITY_ZONES[0][0];

        boolean avz1Added = seedAvailabilityZone(piSeeder, avz1Name, AVAILABILITY_ZONES[0][1], regionCode, "127.0.0.1-127.0.0.2");
        boolean avz1App1RecordAdded = piSeeder.createApplicationRecordForAvailabilityZone(Avz1Application.APPLICATION_NAME, avz1Name, "127.0.0.1");
        LOG.info(String.format("Added availability zone %s: %s; avz1 app rec: %s", avz1Name, avz1Added, avz1App1RecordAdded));

        String avz2Name = regionName + AVAILABILITY_ZONES[1][0];

        boolean avz2Added = seedAvailabilityZone(piSeeder, avz2Name, AVAILABILITY_ZONES[1][1], regionCode, "127.0.0.1-127.0.0.2");
        boolean avz2App1RecordAdded = piSeeder.createApplicationRecordForAvailabilityZone(Avz1Application.APPLICATION_NAME, avz2Name, "127.0.0.1");
        LOG.info(String.format("Added availability zone %s: %s; avz2 app rec: %s", avz2Name, avz2Added, avz2App1RecordAdded));
    }

    private static boolean seedAvailabilityZone(PiSeeder piSeeder, String avzName, String avzCodeWithinRegion, String regionCode, String networkAppAddresses) {
        boolean availabilityZoneAdded = piSeeder.addAvailabilityZone(avzName, avzCodeWithinRegion, regionCode, "available", networkAppAddresses);
        piSeeder.addTaskProcessingQueuesForAvailabilityZone(avzName);

        return availabilityZoneAdded;
    }

    private static String md5Hash(String pass) {
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");
            byte[] data = pass.getBytes();
            m.update(data, 0, data.length);
            BigInteger i = new BigInteger(1, m.digest());
            return String.format("%1$032x", i);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void createUserAndUseCertsAndEndpointsInPircFile(UserManagementService userManagementService) throws Exception {
        userManagementService.createPiCertificate();

        String realName = "I am a test user";
        String emailAddress = "test@user.org";
        byte[] userCerts = userManagementService.createUser(USER_ID, realName, emailAddress);
        File tempCerts = new File(DOT_PIRC_DIRECTORY);
        tempCerts.mkdir();

        String zipFileString = String.format("%s%s%s", tempCerts, File.separator, "zip");
        File file = new File(zipFileString);
        FileUtils.writeByteArrayToFile(file, userCerts);

        ZipFile zipFile = new ZipFile(zipFileString);
        Enumeration<? extends ZipEntry> zipFileEntries = zipFile.entries();
        while (zipFileEntries.hasMoreElements()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ZipEntry zipEntry = zipFileEntries.nextElement();
            IOUtils.copy(zipFile.getInputStream(zipEntry), outputStream);
            FileUtils.writeByteArrayToFile(new File(String.format("%s%s%s", tempCerts, File.separator, zipEntry.getName())), outputStream.toByteArray());
        }

        File pircFile = new File(String.format("%s%spirc", tempCerts, File.separator));
        List<String> pircLines = (List<String>) FileUtils.readLines(pircFile);
        for (String line : pircLines) {
            System.err.println(line);
            if (line.startsWith("export EC2_URL=")) {
                String url = line.substring("export EC2_URL=".length(), line.length());
                piHost = url.substring(0, url.lastIndexOf(":"));
            }
            if (line.startsWith("export EC2_ACCESS_KEY='"))
                accessKey = line.substring("export EC2_ACCESS_KEY='".length(), line.length() - 1);
            if (line.startsWith("export EC2_SECRET_KEY='"))
                secretKey = line.substring("export EC2_SECRET_KEY='".length(), line.length() - 1);
        }
        userManagementService.updateUser(USER_ID, realName, emailAddress, true, null, 30, 30);
    }

    protected static void waitForApplication(ApplicationContext applicationContext, final String applicationName, NodeScope scope, int expectedNumberActiveNodes) throws Exception {
        System.err.println(String.format("waitForApplication(%s)", applicationName));

        KoalaIdFactory koalaIdFactory = (KoalaIdFactory) applicationContext.getBean("koalaIdFactory");
        final PId entityId;
        if (NodeScope.REGION.equals(scope)) {
            entityId = koalaIdFactory.buildPId(RegionScopedApplicationRecord.getUrl(applicationName)).forLocalRegion();
        } else
            entityId = koalaIdFactory.buildPId(AvailabilityZoneScopedApplicationRecord.getUrl(applicationName)).forLocalAvailabilityZone();

        System.err.println(String.format(">>>>> Application Record: %s , Pid: %s", applicationName, entityId.toStringFull()));

        waitForApplicationWithPid(applicationContext, entityId, expectedNumberActiveNodes);
    }

    protected static void waitForApplicationWithPid(ApplicationContext applicationContext, final PId entityId, final int expectedNumberActiveNodes) throws Exception {
        waitForEntity(applicationContext, entityId, new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                boolean retry = true;
                ApplicationRecord applicationRecord = (ApplicationRecord) entity;

                if (applicationRecord == null) {
                    System.err.println(">>>>> Application Record is null for Pid" + entityId.toStringFull());
                }
                if (applicationRecord != null) {
                    System.err.println(">>>>> Number Currently Active Nodes:" + applicationRecord.getNumCurrentlyActiveNodes());
                    System.err.println(">>>>> Application Record:" + applicationRecord);
                }

                if (applicationRecord != null && applicationRecord.getNumCurrentlyActiveNodes() == expectedNumberActiveNodes) {
                    retry = false;
                }
                return retry;
            }
        }, 30, 2 * 1000);
    }

    protected static void waitForApplicationToChangeNodeId(ApplicationContext applicationContext, final PId applicationId, final String currentNodeId) throws Exception {
        System.err.println(String.format("waitForApplicationToChangeNodeId(%s,%s,%s)", applicationContext, applicationId, currentNodeId));
        waitForEntity(applicationContext, applicationId, new EntityChecker() {

            @Override
            public boolean shouldRetry(PiEntity entity) {

                ApplicationRecord applicationRecord = (ApplicationRecord) entity;
                if (applicationRecord == null) {
                    System.err.println(">>>>> Application Record is null for Pid" + applicationId.toStringFull());
                }
                System.err.println(applicationRecord);
                if (applicationRecord.getActiveNodeMap().get("127.0.0.1") != null) {
                    String applicationRecordNodeId = applicationRecord.getActiveNodeMap().get("127.0.0.1").getObject();
                    System.err.println(" APPLICATION RECORD HAS NODE ID: " + applicationRecordNodeId + ", OLD NODE ID: " + currentNodeId);
                    return currentNodeId.equals(applicationRecordNodeId);
                }
                return true;
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
                fail("Exeeded number of retries");
        }
    }

    protected void waitForInstanceToBeCreated(ApplicationContext applicationContext, final String instanceId) throws Exception {
        System.err.println(String.format("waitForInstanceToBeCreated(%s)", instanceId));
        String securityGroupUrl = SecurityGroup.getUrl(USER_ID, SECURITY_GROUP_NAME);
        waitForEntityToBeCreatedInSecurityGroup(applicationContext, instanceId, securityGroupUrl);
    }

    protected void waitForInstanceTypesToBeCreated(ApplicationContext applicationContext) throws Exception {
        PiIdBuilder piIdBuilder = applicationContext.getBean(PiIdBuilder.class);
        PId instanceTypesId = piIdBuilder.getPId(InstanceTypes.URL_STRING);
        waitForEntity(applicationContext, instanceTypesId, new EntityChecker() {

            @Override
            public boolean shouldRetry(PiEntity entity) {
                boolean retry = true;
                InstanceTypes instanceTypes = (InstanceTypes) entity;
                if (instanceTypes != null)
                    System.err.println(instanceTypes);
                if (instanceTypes != null && instanceTypes.getInstanceTypes() != null && instanceTypes.getInstanceTypes().containsKey(InstanceType.LARGE.getTypeId()))
                    return false;
                return retry;
            }
        }, 30, 2 * 1000);
    }

    protected void waitForEntityToBeCreatedInSecurityGroup(ApplicationContext applicationContext, final String instanceId, String securityGroupUrl) throws Exception, InterruptedException {
        PId securityGroupId = ((KoalaIdFactory) applicationContext.getBean("koalaIdFactory")).buildPId(securityGroupUrl).forLocalRegion();
        waitForEntity(applicationContext, securityGroupId, new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                boolean retry = true;
                SecurityGroup sg = (SecurityGroup) entity;
                if (sg != null && sg.getInstances().size() > 0 && sg.getInstances().containsKey(instanceId)) {
                    assertTrue("Security group did not contain requested instance. :(. Security group: " + sg, sg.getInstances().containsKey(instanceId));
                    System.err.println("Instances in the security group: " + sg.getInstances());
                    retry = false;
                }

                return retry;
            }
        }, 30, 2 * 1000);
        Thread.sleep(2 * 1000);
    }

    protected void waitForInstanceToBeRunning(final Jec2 ec2, final String instanceId) throws Exception {
        System.err.println(String.format("waitForInstanceToBeRunning(%s)", instanceId));
        retry(new Retrier() {
            @Override
            public boolean shouldRetry() {
                List<String> instanceIds = new ArrayList<String>();
                instanceIds.add(instanceId);
                try {
                    List<ReservationDescription> instanceList = ec2.describeInstances(instanceIds);
                    System.err.println("Instance List: " + instanceList);
                    if (instanceList.size() == 1) {
                        com.xerox.amazonws.ec2.ReservationDescription.Instance runningInstance = instanceList.get(0).getInstances().get(0);
                        if (InstanceState.RUNNING.toString().equalsIgnoreCase(runningInstance.getState())) {
                            return false;
                        }
                    }
                    return true;
                } catch (EC2Exception e) {
                    e.printStackTrace();
                    return true;
                }

            }
        }, 30, 2 * 1000);
        Thread.sleep(2 * 1000);
    }

    protected void assertContains(String expected, String actual) {
        if (!actual.contains(expected)) {
            throw new AssertionFailedError(String.format("Did not find expected string in actual result\nExpected:\n %s\n\nActual:\n%s", expected, actual));
        }
    }

    interface EntityChecker {
        boolean shouldRetry(PiEntity entity);
    }

    interface Retrier {
        boolean shouldRetry();
    }

}
