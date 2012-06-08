package com.bt.pi.integration;

import static com.bt.pi.app.common.net.utils.IpAddressUtils.ipToLong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.ws.rs.core.Response.Status;

import junit.framework.AssertionFailedError;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.util.ReflectionUtils;

import com.bt.pi.api.service.ApiApplicationManager;
import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceActivityState;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.ManagementRoles;
import com.bt.pi.app.common.entities.NodeVolumeBackupRecord;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.instancemanager.handlers.AnycastHandler;
import com.bt.pi.app.instancemanager.handlers.DetachVolumeHandler;
import com.bt.pi.app.instancemanager.handlers.RunningInstanceInteractionHandler;
import com.bt.pi.app.instancemanager.handlers.VolumeBackupManagerApplication;
import com.bt.pi.app.instancemanager.testing.StubLibvirtConnection;
import com.bt.pi.app.instancemanager.testing.StubMailSender;
import com.bt.pi.app.management.PiSeeder;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.app.volumemanager.VolumeManagerApplication;
import com.bt.pi.core.Main;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.LoggingContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.testing.StubCommandExecutor;
import com.bt.pi.integration.util.StubVolumeBackupHandler;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;
import com.bt.pi.ops.website.entities.ReadOnlyManagementUser;
import com.bt.pi.ops.website.entities.ReadOnlyManagementUsers;
import com.bt.pi.sss.PisssApplicationManager;
import com.ragstorooks.testrr.cli.CommandExecutor;
import com.xerox.amazonws.ec2.AddressInfo;
import com.xerox.amazonws.ec2.AttachmentInfo;
import com.xerox.amazonws.ec2.AvailabilityZone;
import com.xerox.amazonws.ec2.ConsoleOutput;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.GroupDescription;
import com.xerox.amazonws.ec2.ImageDescription;
import com.xerox.amazonws.ec2.InstanceType;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.KeyPairInfo;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.RegionInfo;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.SnapshotInfo;
import com.xerox.amazonws.ec2.VolumeInfo;
import com.xerox.amazonws.ec2.GroupDescription.IpPermission;

public class GivenASingleNodeCloudTest {
    private static final org.apache.commons.logging.Log LOG = LogFactory.getLog(GivenASingleNodeCloudTest.class);
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";
    private static final String DEFAULT_RAMDISK = "pri-RhubarbPie";
    private static final String DEFAULT_KERNEL = "pki-PumpkinPie";
    private static final String DOT_PIRC_DIRECTORY = "tempCerts";
    private static final String DEFAULT_DELETE_COMMAND = "nice -n +10 ionice -c3 rm %s/%s";
    private static final String DEFAULT_RSYNC_COMMAND = "nice -n +10 ionice -c3 cp %s %s";
    private static final String DEFAULT_SNAPSHOT_FOLDER = "var/snapshots";
    private static final String DEFAULT_NFS_VOLUMES_DIRECTORY = "var/volumes/remote";
    private static final String DEFAULT_LOCAL_VOLUMES_DIRECTORY = "var/volumes/local";

    @Rule
    public TestName testName = new TestName();

    private static final String DEFAULT = "default";
    private static final String IMAGE_ID = "pmi-ApplePie";
    private static final String SECURITY_GROUP_NAME = DEFAULT;
    private static final String USERID = "jon";
    private static final String USER_REAL_NAME = "I am a test user";
    private static final String USER_EMAIL = "test@user.org";
    private static AbstractApplicationContext applicationContext = null;
    private static final String UK_REGION_NAME = "UK";
    private static final String UK_REGION_ENDPOINT = "uk.com";
    private static final int UK_REGION_CODE = 2;
    private static final String US_REGION_NAME = "US";
    private static final String US_REGION_ENDPOINT = "us.com";
    private static final int US_REGION_CODE = 1;
    private static String secretKey;
    private static String accessKey;
    private static String timKayBaseCommand;
    private static Jec2 ec2;
    private static PiSeeder piSeeder;
    private static DhtClientFactory dhtClientFactory;
    private static KoalaIdFactory koalaIdFactory;
    private static PiIdBuilder piIdBuilder;
    private static LaunchConfiguration launchConfig;
    private static StubLibvirtConnection stubLibvirtConnection;
    private static StubCommandExecutor stubCommandExecutor;
    private static VolumeBackupManagerApplication volumeBackupManagerApplication;
    private static StubVolumeBackupHandler stubVolumeBackupHandler;
    private static DetachVolumeHandler detachVolumeHandler;

    private CommandExecutor commandExecutor;

    final String TEXT_HTML = "text/html";
    final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    final String APPLICATION_JSON = "application/json";
    final String APPLICATION_XML = "application/xml";
    private String archiveRoot = "var/buckets_archive";
    private String emptyBucketName = "emptyBucket";
    private String nonEmptyBucketName = "nonEmptyBucket";
    private File archiveDir = new File(archiveRoot);
    private String secgroup1 = "secgroup1";
    private String secgroup2 = "secgroup2";
    private static StubMailSender stubMailSender;
    private String instanceId;

    @BeforeClass
    public static void setupBeforeClass() throws Throwable {
        Security.addProvider(new BouncyCastleProvider());
        FileUtils.deleteQuietly(new File("var"));
        FileUtils.forceMkdir(new File("var/buckets"));
        FileUtils.forceMkdir(new File("var/images"));
        FileUtils.forceMkdir(new File("var/image_processing"));
        FileUtils.forceMkdir(new File("var/storage_archive"));

        try {
            String localhostStr = InetAddress.getLocalHost().getHostAddress();
            System.err.println(localhostStr);
            Main nodeStarter = new Main();
            applicationContext = nodeStarter.init(new String[] { "-p5050", "-a^" + localhostStr, "-b" + localhostStr + ":5050", "-x applicationContext-p2p-app-e2e.xml" }, false);

            stubLibvirtConnection = (StubLibvirtConnection) applicationContext.getBean("libvirtConnection");
            stubCommandExecutor = (StubCommandExecutor) applicationContext.getBean("stubCommandExecutor");
            volumeBackupManagerApplication = applicationContext.getBean(VolumeBackupManagerApplication.class);
            stubVolumeBackupHandler = applicationContext.getBean(StubVolumeBackupHandler.class);
            detachVolumeHandler = applicationContext.getBean(DetachVolumeHandler.class);

            dhtClientFactory = (DhtClientFactory) applicationContext.getBean("dhtClientFactory");
            piIdBuilder = (PiIdBuilder) applicationContext.getBean("piIdBuilder");
            piSeeder = (PiSeeder) applicationContext.getBean("piSeeder");
            launchConfig = new LaunchConfiguration(IMAGE_ID);

            koalaIdFactory = (KoalaIdFactory) applicationContext.getBean("koalaIdFactory");
            koalaIdFactory.setRegion(US_REGION_CODE);
            koalaIdFactory.setAvailabilityZone(99);

            // set path for static www content
            ResourceHandler resourceHandler = (ResourceHandler) applicationContext.getBean("resourceHandler");
            resourceHandler.setResourceBase("build/www");

            nodeStarter.start();

            prepareSystem();
            ec2 = createUserAndUseCertsAndEndpointsInPircFile(USERID, DOT_PIRC_DIRECTORY);

            timKayBaseCommand = "touch ~/.awsrc;unset http_proxy;AWS_HOST=localhost AWS_PORT=9090 AWS_ACCESS_KEY_ID=" + accessKey + " AWS_SECRET_ACCESS_KEY=" + secretKey + " etc/timkay-aws/aws --simple --insecure-aws";

            stubMailSender = applicationContext.getBean(StubMailSender.class);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }

        Thread.sleep(30000);
    }

    @Before
    public void setUp() throws Exception {
        System.err.println("######################################################");
        System.err.println("running test " + testName.getMethodName());
        System.err.println("######################################################");
        stubLibvirtConnection.reset();
        commandExecutor = new CommandExecutor(new ScheduledThreadPoolExecutor(20));
        volumeBackupManagerApplication.setVolumeBackupCooldownPeriodSecs(86400);
        stubMailSender.reset();
    }

    @After
    public void tearDown() throws Exception {
        // clean up archive folder.
        if (archiveDir.exists())
            FileUtils.deleteQuietly(archiveDir);

        // clean up
        if (null != instanceId) {
            ec2.terminateInstances(new String[] { instanceId });
            waitForInstanceToBeTerminated(instanceId);
        }
        volumeBackupManagerApplication.setVolumeBackupCooldownPeriodSecs(86400);
    }

    @AfterClass
    public static void teardownAfterClass() throws IOException {
        applicationContext.destroy();
        FileUtils.deleteDirectory(new File(DOT_PIRC_DIRECTORY));
        File file = new File("nodeIdFile");
        String nodeId = FileUtils.readFileToString(file);
        File storageFile = new File(String.format("storage%s", nodeId));
        if (storageFile.exists()) {
            FileUtils.deleteDirectory(storageFile);
        }
        if (file.exists())
            file.delete();
        FileUtils.deleteQuietly(new File("/tmp/test-file.manifest.xml"));
        FileUtils.deleteQuietly(new File("/tmp/test-file.part.0"));
        FileUtils.deleteQuietly(new File("/tmp/test-file.part.1"));
    }

    @Test
    public void shouldBundleImageAndUploadBundleAndRegisterImageAndRunInstanceAndDeregisterImageAndDeleteBundle() throws Exception {
        // setup
        String javaHome = System.getProperty("java.home", "/usr");
        String amitoolsCommandBase = String.format("unset http_proxy; JAVA_HOME=%s EC2_AMITOOL_HOME=etc/ec2-ami-tools-1.3-26357 etc/ec2-ami-tools-1.3-26357/bin/", javaHome);
        String s3Url = "http://localhost:9090";

        // bundle image
        runCommand(String.format("%s%s --debug --batch --image %s --cert %s --privatekey %s --user 000000000000 --ec2cert %s --kernel %s --ramdisk %s", amitoolsCommandBase, "ec2-bundle-image", "src/integration/resources/test-file",
                getUserCertPath(), getUserPrivateKeyPath(), getEc2CertPath(), DEFAULT_KERNEL, DEFAULT_RAMDISK));

        String manifestFile = "/tmp/test-file.manifest.xml";
        String bucketName = "mytestbucket";
        // act
        runCommand(String.format("%s%s --url %s --debug --bucket %s --manifest %s --access-key %s --secret-key %s", amitoolsCommandBase, "ec2-upload-bundle", s3Url, bucketName, manifestFile, accessKey, secretKey));

        // assert
        assertBucketExists(true, bucketName);
        assertObjectExists(true, bucketName, "test-file.part.0", FileUtils.readFileToString(new File("/tmp/test-file.part.0")), "binary/octet-stream");
        assertObjectExists(true, bucketName, "test-file.part.1", FileUtils.readFileToString(new File("/tmp/test-file.part.1")), "binary/octet-stream");

        // act
        String imageId = ec2.registerImage(String.format("%s/test-file.manifest.xml", bucketName));
        System.err.println("Image Id: " + imageId);

        // assert
        List<ImageDescription> images = ec2.describeImages(new String[] {});
        assertEquals(4, images.size());
        assertImageIdIsInList(IMAGE_ID, images);
        assertImageIdIsInList(imageId, images, bucketName + "/test-file.manifest.xml", null);

        waitForImageState(imageId, ImageState.AVAILABLE);

        // now run the bugger
        instanceId = runInstance();

        // not act or assert.. just waiting.
        waitForInstanceToBeCreated(instanceId);

        waitForInstanceToBeRunning(instanceId);

        // act
        ec2.deregisterImage(imageId);

        // assert
        images = ec2.describeImages(new String[] {});
        assertEquals(3, images.size());
        assertImageIdIsInList(IMAGE_ID, images);

        // act
        runCommand(String.format("%s%s --yes --clear --url %s --debug --bucket %s --manifest %s --access-key %s --secret-key %s", amitoolsCommandBase, "ec2-delete-bundle", s3Url, bucketName, manifestFile, accessKey, secretKey));

        // assert
        assertBucketExists(false, bucketName);
    }

    private void waitForImageState(String imageId, ImageState target) throws Exception {
        int count = 0;
        int delay = 1000;
        int no = 20;
        while (count < no) {
            List<ImageDescription> images = ec2.describeImages(new String[] { imageId });
            System.err.println("image state:" + images.get(0).getImageState());
            if (images.get(0).getImageState().equals(target.toString()))
                return;
            Thread.sleep(delay);
        }
        fail("image not in " + target + " status after " + (delay * no) + " millis");
    }

    private String getUserPrivateKeyPath() {
        File pircDir = new File(DOT_PIRC_DIRECTORY);
        for (File f : pircDir.listFiles())
            if (f.getAbsolutePath().endsWith("-pk.pem"))
                return f.getAbsolutePath();
        return null;
    }

    private Object getUserCertPath() {
        File pircDir = new File(DOT_PIRC_DIRECTORY);
        for (File f : pircDir.listFiles())
            if (f.getAbsolutePath().endsWith("-cert.pem") && !f.getAbsolutePath().contains("cloud-cert.pem"))
                return f.getAbsolutePath();
        return null;
    }

    private Object getEc2CertPath() {
        File pircDir = new File(DOT_PIRC_DIRECTORY);
        for (File f : pircDir.listFiles())
            if (f.getAbsolutePath().endsWith("cloud-cert.pem"))
                return f.getAbsolutePath();
        return null;
    }

    private void assertImageIdIsInList(String imageId, List<ImageDescription> images) {
        assertImageIdIsInList(imageId, images, null, null);
    }

    private void assertImageIdIsInList(String imageId, List<ImageDescription> images, String location, ImageState expectedState) {
        for (ImageDescription imageDescription : images)
            if (imageDescription.getImageId().equals(imageId)) {
                if (location == null)
                    return;
                if (imageDescription.getImageLocation().equals(location)) {
                    if (null == expectedState)
                        return;
                    if (imageDescription.getImageState().equals(expectedState.toString()))
                        return;
                }
            }
        fail(String.format("expected image %s %s %s not found in %s", imageId, null == location ? "" : "with location " + location, null == expectedState ? "" : "with state " + expectedState.toString(), images));
    }

    @Test
    public void shouldAllocateAssociateDescribeDisassociateAndDeallocateElasticAddress() throws Exception {
        // allocate
        String allocatedAddress = ec2.allocateAddress();
        assertTrue(ipToLong("10.0.0.1") <= ipToLong(allocatedAddress));
        assertTrue(ipToLong("10.0.0.6") >= ipToLong(allocatedAddress));

        // describe
        List<AddressInfo> describedAddressesOnAllocation = ec2.describeAddresses(new ArrayList<String>());
        assertEquals(1, describedAddressesOnAllocation.size());
        assertEquals(allocatedAddress, describedAddressesOnAllocation.get(0).getPublicIp());
        assertEquals("", describedAddressesOnAllocation.get(0).getInstanceId());

        // run instance
        instanceId = runInstance();
        waitForInstanceToBeRunning(instanceId);

        // associate
        ec2.associateAddress(instanceId, allocatedAddress);
        Thread.sleep(5000);

        // describe
        List<AddressInfo> describedAddressesOnAssociation = ec2.describeAddresses(new ArrayList<String>());
        assertEquals(1, describedAddressesOnAssociation.size());
        assertEquals(allocatedAddress, describedAddressesOnAssociation.get(0).getPublicIp());
        assertEquals(instanceId, describedAddressesOnAssociation.get(0).getInstanceId());

        // disassociate
        ec2.disassociateAddress(allocatedAddress);
        Thread.sleep(5000);

        // describe
        List<AddressInfo> describedAddressesOnDisassociation = ec2.describeAddresses(new ArrayList<String>());
        assertEquals(1, describedAddressesOnDisassociation.size());
        assertEquals(allocatedAddress, describedAddressesOnDisassociation.get(0).getPublicIp());
        assertEquals("", describedAddressesOnDisassociation.get(0).getInstanceId());

        // deallocate
        ec2.releaseAddress(allocatedAddress);
        assertEquals(0, ec2.describeAddresses(null).size());
    }

    @Test
    public void shouldCreateAndAttachAndDetachAndDeleteAVolume() throws Exception {
        // setup
        String size = "2";
        String snapshotId = null;
        String zone = "c1";

        // act
        VolumeInfo result = ec2.createVolume(size, snapshotId, zone);

        // assert
        assertEquals("creating", result.getStatus());
        assertTime(result.getCreateTime());
        String volumeId = result.getVolumeId();

        // now use API to list the vol
        waitForVolumeStatus(volumeId, VolumeState.AVAILABLE);
        List<VolumeInfo> describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("available"));

        assertCommand(String.format("nice -n +10 ionice -c3 dd if=/dev/zero of=var/volumes/remote/%s count=0 seek=%d bs=1M", volumeId, Integer.parseInt(size) * 1024));

        // run an instance
        instanceId = runInstance();
        waitForInstanceToBeRunning(instanceId);
        Thread.sleep(2000);

        // use API to attach volume to instance
        String device = "/dev/sdb";
        AttachmentInfo attachmentInfo = ec2.attachVolume(volumeId, instanceId, device);
        assertEquals("attaching", attachmentInfo.getStatus());

        // spy on stub libvirt to see it attached
        waitForLibvirtCommand("attachDevice(");

        assertCommand(String.format("nice -n +10 ionice -c3 cp var/volumes/remote/%s var/volumes/local/%s", volumeId, volumeId));

        // now use API to list the vol
        Thread.sleep(2000);
        describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("in-use"));
        assertEquals(device, describeVolumes.get(0).getAttachmentInfo().get(0).getDevice());
        assertEquals(instanceId, describeVolumes.get(0).getAttachmentInfo().get(0).getInstanceId());
        assertTime(describeVolumes.get(0).getAttachmentInfo().get(0).getAttachTime());

        // use API to detach
        AttachmentInfo detachVolume = ec2.detachVolume(volumeId, null, null, false);
        assertEquals("detaching", detachVolume.getStatus());

        // spy on stub libvirt to see it detached
        Thread.sleep(2000);
        waitForLibvirtCommand("detachDevice(");
        Thread.sleep(1500);
        assertCommand(String.format("cp var/volumes/local/%s var/volumes/remote/%s", volumeId, volumeId));
        assertCommand(String.format("rm var/volumes/local/%s", volumeId));

        // now use API to list the vol
        Thread.sleep(2000);
        describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("available"));

        // use API to delete the vol
        ec2.deleteVolume(volumeId);

        waitForVolumeStatus(volumeId, VolumeState.DELETED);

        // check disk to see it deleted
        assertCommand(String.format("nice -n +10 ionice -c3 rm var/volumes/remote/%s", volumeId));
        // now use API to list the vol
        Thread.sleep(2000);
        describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("deleted"));
        assertEquals(0, describeVolumes.get(0).getAttachmentInfo().size());
    }

    @Test
    public void shouldBackupAttachedVolumes() throws Exception {
        // setup
        volumeBackupManagerApplication.setVolumeBackupCooldownPeriodSecs(10); // reset in @after method

        String size = "2";
        String snapshotId = null;
        String zone = "c1";
        VolumeInfo result = ec2.createVolume(size, snapshotId, zone);
        String volumeId = result.getVolumeId();
        waitForVolumeStatus(volumeId, VolumeState.AVAILABLE);

        // run an instance
        instanceId = runInstance();
        waitForInstanceToBeRunning(instanceId);
        Thread.sleep(2000);

        // use API to attach volume to instance
        String device = "/dev/sdb";
        ec2.attachVolume(volumeId, instanceId, device);
        waitForLibvirtCommand("attachDevice(");
        waitForInstanceInRegistryToHaveVolume(instanceId);

        // act
        long startTimestamp = System.currentTimeMillis();
        StubVolumeBackupHandler stubVolumeBackupHandler = applicationContext.getBean(StubVolumeBackupHandler.class);
        boolean becomeActive = volumeBackupManagerApplication.becomeActive();

        // assert
        assertTrue(becomeActive);
        waitForUpdateOnNodeBackupRecordTimestamp(volumeBackupManagerApplication.getNodeIdFull(), startTimestamp);
        assertCommand(String.format("nice -n +10 ionice -c3 cp %s tmp/%s", stubVolumeBackupHandler.getAbsoluteLocalVolumeFilename(volumeId), volumeId));
        assertCommand(String.format("nice -n +10 ionice -c3 mv tmp/%s var/volumes/remote/%s", volumeId, volumeId));
    }

    @Test
    public void shouldNotBackupAttachedVolumesIfDetachIsInProgress() throws Exception {
        // setup
        volumeBackupManagerApplication.setVolumeBackupCooldownPeriodSecs(10); // reset in @after method

        String size = "2";
        String snapshotId = null;
        String zone = "c1";
        VolumeInfo result = ec2.createVolume(size, snapshotId, zone);
        String volumeId = result.getVolumeId();
        waitForVolumeStatus(volumeId, VolumeState.AVAILABLE);

        // run an instance
        instanceId = runInstance();
        waitForInstanceToBeRunning(instanceId);
        Thread.sleep(2000);

        // use API to attach volume to instance
        String device = "/dev/sdb";
        ec2.attachVolume(volumeId, instanceId, device);
        waitForLibvirtCommand("attachDevice(");
        waitForInstanceInRegistryToHaveVolume(instanceId);

        assertTrue(detachVolumeHandler.acquireLock(volumeId, 10, TimeUnit.MILLISECONDS));

        // act
        long startTimestamp = System.currentTimeMillis();
        boolean becomeActive = volumeBackupManagerApplication.becomeActive();

        // assert
        assertTrue(becomeActive);
        assertThatBackupDidNotOccur(startTimestamp, volumeId);
    }

    @Test
    public void shouldNotDetachVolumeIfBackupIsInProgress() throws Exception {
        // setup
        volumeBackupManagerApplication.setVolumeBackupCooldownPeriodSecs(10); // reset in @after method

        String size = "2";
        String snapshotId = null;
        String zone = "c1";
        VolumeInfo result = ec2.createVolume(size, snapshotId, zone);
        String volumeId = result.getVolumeId();
        waitForVolumeStatus(volumeId, VolumeState.AVAILABLE);

        // run an instance
        instanceId = runInstance();
        waitForInstanceToBeRunning(instanceId);
        Thread.sleep(2000);

        // use API to attach volume to instance
        String device = "/dev/sdb";
        ec2.attachVolume(volumeId, instanceId, device);
        waitForLibvirtCommand("attachDevice(");
        waitForInstanceInRegistryToHaveVolume(instanceId);

        assertTrue(detachVolumeHandler.acquireLock(volumeId, 10, TimeUnit.MILLISECONDS));

        // act
        AttachmentInfo detachVolume = ec2.detachVolume(volumeId, null, null, false);
        assertEquals("detaching", detachVolume.getStatus());

        Thread.sleep(2000);
        assertCommandDidNotExecute(String.format("cp var/volumes/local/%s var/volumes/remote/%s", volumeId, volumeId));
        assertCommandDidNotExecute(String.format("rm var/volumes/local/%s", volumeId));
    }

    private void assertThatBackupDidNotOccur(long startTimestamp, String volumeId) throws Exception {
        waitForUpdateOnNodeBackupRecordTimestamp(volumeBackupManagerApplication.getNodeIdFull(), startTimestamp);
        assertCommandDidNotExecute(String.format("nice -n +10 ionice -c3 cp %s tmp/%s", stubVolumeBackupHandler.getAbsoluteLocalVolumeFilename(volumeId), volumeId));
        assertCommandDidNotExecute(String.format("nice -n +10 ionice -c3 mv tmp/%s var/volumes/remote/%s", volumeId, volumeId));
    }

    private void waitForInstanceInRegistryToHaveVolume(String instanceId) throws Exception {
        PId instancePId = piIdBuilder.getPId(Instance.getUrl(instanceId)).forLocalAvailabilityZone();
        ConsumedDhtResourceRegistry consumedDhtResourceRegistry = applicationContext.getBean(ConsumedDhtResourceRegistry.class);

        Instance instance = null;
        for (int i = 0; i < 5; i++) {
            instance = (Instance) consumedDhtResourceRegistry.getCachedEntity(instancePId);
            if (instance.getBlockDeviceMappings().size() > 0)
                return;

            consumedDhtResourceRegistry.refresh(instancePId, new LoggingContinuation<Instance>());
            Thread.sleep(2000);
        }
        fail("Instance in consumedDhtResourceRegistry has no attached volumes!!");
    }

    private void waitForUpdateOnNodeBackupRecordTimestamp(String nodeId, final long startTimestamp) throws Exception {
        PId entityId = piIdBuilder.getPId(NodeVolumeBackupRecord.getUrl(nodeId));

        EntityChecker checker = new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                NodeVolumeBackupRecord nodeVolumeBackupRecord = (NodeVolumeBackupRecord) entity;
                return nodeVolumeBackupRecord == null || nodeVolumeBackupRecord.getLastBackup() <= startTimestamp;
            }
        };
        waitForEntity(entityId, checker, new AtomicInteger(10), 2000);
    }

    @Test
    public void shouldForceDetachVolumeFromCrashedInstance() throws Exception {
        // setup
        String size = "2";
        String snapshotId = null;
        String zone = "c1";

        // act
        VolumeInfo result = ec2.createVolume(size, snapshotId, zone);

        // assert
        assertEquals("creating", result.getStatus());
        assertTime(result.getCreateTime());
        String volumeId = result.getVolumeId();

        // now use API to list the vol
        waitForVolumeStatus(volumeId, VolumeState.AVAILABLE);
        List<VolumeInfo> describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("available"));

        assertCommand(String.format("nice -n +10 ionice -c3 dd if=/dev/zero of=var/volumes/remote/%s count=0 seek=%d bs=1M", volumeId, Integer.parseInt(size) * 1024));

        // run an instance
        instanceId = runInstance();
        waitForInstanceToBeRunning(instanceId);
        Thread.sleep(2000);

        // use API to attach volume to instance
        String device = "/dev/sdb";
        AttachmentInfo attachmentInfo = ec2.attachVolume(volumeId, instanceId, device);
        assertEquals("attaching", attachmentInfo.getStatus());

        // spy on stub libvirt to see it attached
        waitForLibvirtCommand("attachDevice(");

        assertCommand(String.format("nice -n +10 ionice -c3 cp var/volumes/remote/%s var/volumes/local/%s", volumeId, volumeId));

        // now use API to list the vol
        Thread.sleep(2000);
        describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("in-use"));
        assertEquals(device, describeVolumes.get(0).getAttachmentInfo().get(0).getDevice());
        assertEquals(instanceId, describeVolumes.get(0).getAttachmentInfo().get(0).getInstanceId());
        assertTime(describeVolumes.get(0).getAttachmentInfo().get(0).getAttachTime());

        // manually "crash" the instance
        stubLibvirtConnection.reset();

        // use API to detach
        AttachmentInfo detachVolume = ec2.detachVolume(volumeId, null, null, true); // force
        assertEquals("detaching", detachVolume.getStatus());

        // spy on stub libvirt to see it detached
        waitForLibvirtCommand("domainLookupByName");
        Thread.sleep(1500);
        assertCommand(String.format("cp var/volumes/local/%s var/volumes/remote/%s", volumeId, volumeId));
        assertCommand(String.format("rm var/volumes/local/%s", volumeId));

        // now use API to list the vol
        Thread.sleep(2000);
        describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("available"));

        // use API to delete the vol
        ec2.deleteVolume(volumeId);

        waitForVolumeStatus(volumeId, VolumeState.DELETED);

        // check disk to see it deleted
        assertCommand(String.format("nice -n +10 ionice -c3 rm var/volumes/remote/%s", volumeId));
        // now use API to list the vol
        Thread.sleep(2000);
        describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("deleted"));
        assertEquals(0, describeVolumes.get(0).getAttachmentInfo().size());
    }

    @Test
    public void shouldDetachAVolumeAndDisassociateAddressWhenInstanceIsTerminated() throws Exception {
        LOG.info("Create and attach a volume");
        AttachmentInfo attachmentInfo = createAndAttachVolume();
        LOG.info("Allocating address");

        // allocate
        String allocatedAddress = ec2.allocateAddress();
        System.err.println("Allocated address: " + allocatedAddress);
        assertTrue(ipToLong("10.0.0.1") <= ipToLong(allocatedAddress));
        assertTrue(ipToLong("10.0.0.14") >= ipToLong(allocatedAddress));
        // describe
        List<AddressInfo> describedAddressesOnAllocation = ec2.describeAddresses(new ArrayList<String>());
        assertEquals(1, describedAddressesOnAllocation.size());
        assertEquals(allocatedAddress, describedAddressesOnAllocation.get(0).getPublicIp());
        assertEquals("", describedAddressesOnAllocation.get(0).getInstanceId());

        LOG.info("Terminating instance " + attachmentInfo.getInstanceId());
        ec2.terminateInstances(Arrays.asList(attachmentInfo.getInstanceId()));

        waitForInstanceToBeTerminated(attachmentInfo.getInstanceId());

        String volumeId = attachmentInfo.getVolumeId();

        // now use API to list the vol
        System.err.println("shouldDetachAVolumeWhenInstanceIsTerminated: get volume information and assert that it's available");
        waitForVolumeStatus(volumeId, VolumeState.AVAILABLE);
        LOG.info("Volume in available state, checking for file system cleanup commands");
        assertCommand(String.format("cp var/volumes/local/%s var/volumes/remote/%s", volumeId, volumeId));
        assertCommand(String.format("rm var/volumes/local/%s", volumeId));
        deleteVolume(attachmentInfo.getVolumeId());
        // Check that the address has been disassociated from the instance
        waitForAddressToDisassociate(allocatedAddress);
    }

    private void waitForAddressToDisassociate(String allocatedAddress) throws EC2Exception, InterruptedException {
        int count = 0;
        int delay = 500;
        int no = 20;
        while (count < no) {

            List<AddressInfo> describedAddressesOnDisassociation = ec2.describeAddresses(new ArrayList<String>());
            System.err.println(new KoalaJsonParser().getJson(describedAddressesOnDisassociation));
            if ("".equals(describedAddressesOnDisassociation.get(0).getInstanceId()) && allocatedAddress.equals(describedAddressesOnDisassociation.get(0).getPublicIp())) {
                return;
            }

            count++;
            Thread.sleep(delay);
        }
        fail("address " + allocatedAddress + " has not disassociated after " + (delay * no) + " millis");
    }

    private void waitForVolumeStatus(String volumeId, VolumeState volumeState) throws Exception {
        int count = 0;
        int delay = 500;
        int no = 20;
        while (count < no) {
            List<VolumeInfo> describeVolumes = ec2.describeVolumes(new String[] { volumeId });
            if (describeVolumes.size() > 0) {
                System.err.println(describeVolumes.get(0));
                if (volumeState.toString().equals(describeVolumes.get(0).getStatus()))
                    return;
            }
            count++;
            Thread.sleep(delay);
        }
        fail("volume not in " + volumeState + " status after " + (delay * no) + " millis");
    }

    @Test
    public void shouldCreateAndDeleteSnapshotOnAttachedVolume() throws Exception {
        String volumeId = createAndAttachVolume().getVolumeId();

        SnapshotInfo snapshotInfo = createSnapshot(volumeId, getLocalVolumeFilename(volumeId));
        deleteSnapshot(snapshotInfo);
        detachVolume(volumeId);
        deleteVolume(volumeId);
    }

    @Test
    public void shouldCreateAndDeleteSnapshotOnAvailableVolume() throws Exception {
        String volumeId = createVolume(null);

        SnapshotInfo snapshotInfo = createSnapshot(volumeId, getVolumeFilename(volumeId));
        deleteSnapshot(snapshotInfo);
        deleteVolume(volumeId);
    }

    private void deleteSnapshot(SnapshotInfo snapshotInfo) throws EC2Exception, Exception {
        ec2.deleteSnapshot(snapshotInfo.getSnapshotId());
        waitForSnapshotStatus(snapshotInfo.getSnapshotId(), SnapshotState.DELETED);
        assertCommand(String.format(DEFAULT_DELETE_COMMAND, DEFAULT_SNAPSHOT_FOLDER, snapshotInfo.getSnapshotId()));
    }

    private SnapshotInfo createSnapshot(String volumeId, String volumeFilename) throws EC2Exception, Exception {
        LOG.warn(String.format("Create snapshot for volume %s", volumeId));
        Date methodStartTime = new Date();
        SnapshotInfo snapshotInfo = ec2.createSnapshot(volumeId);
        System.err.println("SNAPSHOT INFO: " + snapshotInfo);
        snapshotInfo = waitForSnapshotStatus(snapshotInfo.getSnapshotId(), SnapshotState.COMPLETE);
        assertCommandSilently(String.format(DEFAULT_RSYNC_COMMAND, volumeFilename, getSnapshotFilename(snapshotInfo.getSnapshotId())));
        assertEquals(volumeId, snapshotInfo.getVolumeId());
        assertTrue(snapshotInfo.getStartTime().getTime().after(methodStartTime));
        assertEquals("100.0", snapshotInfo.getProgress());
        return snapshotInfo;
    }

    @Test
    public void shouldCreateVolumeFromSnapshot() throws Exception {
        String volumeId = createVolume(null);
        LOG.warn(String.format("Create snapshot for volume %s", volumeId));
        SnapshotInfo snapshotInfo = ec2.createSnapshot(volumeId);
        System.err.println("SNAPSHOT INFO: " + snapshotInfo);
        waitForSnapshotStatus(snapshotInfo.getSnapshotId(), SnapshotState.COMPLETE);
        deleteVolume(volumeId);
        volumeId = createVolume(snapshotInfo.getSnapshotId());
    }

    private String getVolumeFilename(String volumeId) {
        return String.format("%s/%s", DEFAULT_NFS_VOLUMES_DIRECTORY, volumeId);
    }

    private String getLocalVolumeFilename(String volumeId) {
        return String.format("%s/%s", new File(DEFAULT_LOCAL_VOLUMES_DIRECTORY).getAbsolutePath(), volumeId);
    }

    private String getSnapshotFilename(String snapshotId) {
        return String.format("%s/%s", DEFAULT_SNAPSHOT_FOLDER, snapshotId);
    }

    private String runInstance() throws Exception {
        ReservationDescription reservation = ec2.runInstances(launchConfig);
        return reservation.getInstances().get(0).getInstanceId();
    }

    private AttachmentInfo createAndAttachVolume() throws Exception {
        String volumeId = createVolume(null);
        List<VolumeInfo> describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("available"));

        // run an instance
        instanceId = runInstance();
        waitForInstanceToBeRunning(instanceId);
        Thread.sleep(2000);

        // use API to attach volume to instance
        String device = "/dev/sdb";
        AttachmentInfo attachmentInfo = ec2.attachVolume(volumeId, instanceId, device);
        assertEquals("attaching", attachmentInfo.getStatus());

        // spy on stub libvirt to see it attached
        waitForLibvirtCommand("attachDevice(");

        // now use API to list the vol
        Thread.sleep(2000);
        describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("in-use"));

        return attachmentInfo;
    }

    private String createVolume(String snapshotId) throws Exception {
        // setup
        String size = "2";
        String zone = "c1";

        // act
        VolumeInfo result = ec2.createVolume(size, snapshotId, zone);

        // assert
        assertEquals("creating", result.getStatus());
        assertTime(result.getCreateTime());
        String volumeId = result.getVolumeId();

        // now use API to list the vol
        waitForVolumeStatus(volumeId, VolumeState.AVAILABLE);

        List<VolumeInfo> describeVolumes = ec2.describeVolumes(new String[] { volumeId });
        System.err.println("Volume created: \n" + new KoalaJsonParser().getJson(describeVolumes));
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(volumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals(VolumeState.AVAILABLE.toString()));

        return volumeId;
    }

    private void detachVolume(String aVolumeId) throws Exception {
        AttachmentInfo detachVolume = ec2.detachVolume(aVolumeId, null, null, false);
        assertEquals("detaching", detachVolume.getStatus());

        // spy on stub libvirt to see it detached
        waitForLibvirtCommand("detachDevice(");
        Thread.sleep(1500);
        assertCommand(String.format("cp var/volumes/local/%s var/volumes/remote/%s", aVolumeId, aVolumeId));
        assertCommand(String.format("rm var/volumes/local/%s", aVolumeId));

        // now use API to list the vol
        Thread.sleep(2000);
        List<VolumeInfo> describeVolumes = ec2.describeVolumes(new String[] { aVolumeId });
        assertEquals(1, describeVolumes.size());
        assertTrue(describeVolumes.get(0).getVolumeId().equals(aVolumeId));
        assertTrue(describeVolumes.get(0).getStatus().equals("available"));

    }

    private void deleteVolume(String volumeId) throws Exception {

        // use API to delete the vol
        ec2.deleteVolume(volumeId);

        waitForVolumeStatus(volumeId, VolumeState.DELETED);

    }

    private SnapshotInfo waitForSnapshotStatus(String snapshotId, SnapshotState snapshotStatus) throws Exception {
        int count = 0;
        int delay = 500;
        int no = 20;
        while (count < no) {
            List<SnapshotInfo> describeSnapshots = ec2.describeSnapshots(new String[] { snapshotId });
            if (describeSnapshots.size() > 0) {

                System.err.println(describeSnapshots.get(0));
                if (snapshotStatus.toString().equals(describeSnapshots.get(0).getStatus()))
                    return describeSnapshots.get(0);
            } else if (snapshotStatus.equals(SnapshotState.DELETED)) {
                // When the snapshot status is deleted the entity will disappear from DHT
                if (describeSnapshots.size() == 0)
                    return null;
            }
            count++;
            Thread.sleep(delay);
        }
        fail("snapshot not in " + snapshotStatus + " status after " + (delay * no) + " millis");
        return null;
    }

    private void assertCommand(String target) {
        assertCommand(target.split(" "));
    }

    private void assertCommandSilently(String target) {
        assertTrue(stubCommandExecutor.assertCommandSilently(target.split(" ")));
    }

    private void assertCommand(String[] arrayTarget) {
        assertTrue(stubCommandExecutor.assertCommandSilently(arrayTarget));
    }

    private void assertCommandDidNotExecute(String target) {
        assertCommandDidNotExecute(target.split(" "));
    }

    private void assertCommandDidNotExecute(String[] arrayTarget) {
        assertFalse(stubCommandExecutor.assertCommandSilently(arrayTarget));
    }

    private void assertTime(Calendar createTime) {
        System.err.println(String.format("assertTime(%s)", createTime.getTime()));
        long now = System.currentTimeMillis();
        System.err.println(now);
        long then = createTime.getTimeInMillis();
        System.err.println(then);
        assertTrue(Math.abs(now - then) < 20000);
    }

    private void waitForLibvirtCommand(final String target) throws InterruptedException {
        assertTrue(stubLibvirtConnection.waitForLibvirtCommand(target, 80));
    }

    @Test(expected = EC2Exception.class)
    public void shouldThrowExceptionIfNoCapacityLeftInSecurityGroup() throws Exception {
        // setup
        PId sid = piIdBuilder.getPId("sg:jon:default").forLocalRegion();
        SecurityGroup sg = (SecurityGroup) dhtClientFactory.createBlockingReader().get(sid);
        sg.setNetmask("255.255.255.250");
        for (int i = 0; i < 5; i++) {
            sg.getInstances().put(String.format("i-abc%d", i), null);
        }

        dhtClientFactory.createBlockingWriter().put(sid, sg);

        // act
        try {
            ec2.runInstances(launchConfig);
        } catch (EC2Exception ex) {
            SecurityGroup sg1 = (SecurityGroup) dhtClientFactory.createBlockingReader().get(sid);
            sg1.getInstances().clear();
            dhtClientFactory.createBlockingWriter().put(sid, sg1);
            System.err.println("After SG cleanup:>>>" + sg1);
            throw ex;
        }
    }

    @Test
    public void shouldStartUpAnInstance() throws Exception {
        // setup

        // act
        instanceId = runInstance();

        // not act or assert.. just waiting.
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);
    }

    @Test
    public void shouldPerformInstanceValidationLifecycle() throws Exception {
        // setup
        instanceId = runInstance();
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        clobberInstanceActivityTimestamp(instanceId, InstanceActivityState.GREEN, 10000);

        String message = waitForValidationEmail();
        assertNotNull("email message not received", message);
        System.err.println(message);

        // use the link in the email to validate the users
        System.setProperty("javax.net.ssl.trustStore", "src/integration/resources/ssl_keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "badgermonkeyfish");

        int start = message.indexOf("href=\"") + "href=\"".length();
        int end = message.indexOf("\"", start);
        String getAddress = message.substring(start, end);
        System.err.println(getAddress);

        final HttpClient cli = new HttpClient();

        GetMethod getMethod = new GetMethod(getAddress);
        getMethod.setRequestHeader("accept", TEXT_HTML);

        // make sure we can pull static content
        assertEquals(200, cli.executeMethod(getMethod));
        String getResponse = getMethod.getResponseBodyAsString();
        System.err.println(getResponse);

        List<String> instanceIdsFromGet = getInstanceIds(getResponse);
        assertTrue(instanceIdsFromGet.contains(instanceId));

        String postPath = "https://localhost:8443" + getPostPath(getResponse);
        PostMethod postMethod = new PostMethod(postPath);
        postMethod.addParameter("validate_" + instanceId, "true");

        assertEquals(200, cli.executeMethod(postMethod));
        String postResponse = postMethod.getResponseBodyAsString();
        System.err.println(postResponse);

        Date lastValidated = getLastValidated(instanceId);
        assertTrue(lastValidated.getTime() > System.currentTimeMillis() - 10000);

        assertInstanceActivityState(instanceId, InstanceActivityState.GREEN);
    }

    @Test
    public void shouldPauseAmberInstance() throws Exception {
        // setup
        instanceId = runInstance();
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        // act
        clobberInstanceActivityTimestamp(instanceId, InstanceActivityState.AMBER, 10000);

        // assert
        waitForInstanceToBePaused(instanceId);
        assertInstanceActivityState(instanceId, InstanceActivityState.RED);
    }

    @Test
    public void shouldBeAbleToValidateAmberInstance() throws Exception {
        // setup
        instanceId = runInstance();
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        clobberInstanceActivityTimestamp(instanceId, InstanceActivityState.AMBER, -1);

        // act
        System.setProperty("javax.net.ssl.trustStore", "src/integration/resources/ssl_keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "badgermonkeyfish");
        String userPid = piIdBuilder.getPId(User.getUrl(USERID)).toStringFull();
        String postPath = "https://localhost:8443/users/" + USERID + "/instancevalidation/" + userPid;
        PostMethod postMethod = new PostMethod(postPath);
        postMethod.addParameter("validate_" + instanceId, "true");

        final HttpClient cli = new HttpClient();
        assertEquals(200, cli.executeMethod(postMethod));
        String postResponse = postMethod.getResponseBodyAsString();
        System.err.println(postResponse);

        Date lastValidated = getLastValidated(instanceId);
        assertTrue(lastValidated.getTime() > System.currentTimeMillis() - 5000);

        assertInstanceActivityState(instanceId, InstanceActivityState.GREEN);
    }

    @Test
    public void shouldBeAbleToValidateRedInstance() throws Exception {
        // setup
        instanceId = runInstance();
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        clobberInstanceActivityTimestamp(instanceId, InstanceActivityState.RED, -1);

        // act
        System.setProperty("javax.net.ssl.trustStore", "src/integration/resources/ssl_keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "badgermonkeyfish");
        String userPid = piIdBuilder.getPId(User.getUrl(USERID)).toStringFull();
        String postPath = "https://localhost:8443/users/" + USERID + "/instancevalidation/" + userPid;
        PostMethod postMethod = new PostMethod(postPath);
        postMethod.addParameter("validate_" + instanceId, "true");

        final HttpClient cli = new HttpClient();
        assertEquals(200, cli.executeMethod(postMethod));
        String postResponse = postMethod.getResponseBodyAsString();
        System.err.println(postResponse);

        Date lastValidated = getLastValidated(instanceId);
        assertTrue(lastValidated.getTime() > System.currentTimeMillis() - 5000);

        assertInstanceActivityState(instanceId, InstanceActivityState.GREEN);

        waitForInstanceToBeUnPaused(instanceId);
    }

    @Test
    public void shouldTerminateRedInstance() throws Exception {
        // setup
        instanceId = runInstance();
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        // act
        clobberInstanceActivityTimestamp(instanceId, InstanceActivityState.RED, 10000);

        // assert
        waitForInstanceToBeTerminated(instanceId, 40);
    }

    private void waitForInstanceToBeUnPaused(String instanceId) throws Exception {
        assertTrue(stubLibvirtConnection.waitForLibvirtCommand(String.format("unPauseInstance(%s)", instanceId), 20));
    }

    private void waitForInstanceToBePaused(String instanceId) throws Exception {
        assertTrue(stubLibvirtConnection.waitForLibvirtCommand(String.format("pauseInstance(%s)", instanceId), 400));
    }

    private void assertInstanceActivityState(String instanceId, InstanceActivityState expected) {
        PId id = koalaIdFactory.buildPId(Instance.getUrl(instanceId)).forLocalAvailabilityZone();
        Instance instance = (Instance) dhtClientFactory.createBlockingReader().get(id);
        assertEquals(expected, instance.getInstanceActivityState());
    }

    private Date getLastValidated(String instanceId) {
        PId id = koalaIdFactory.buildPId(Instance.getUrl(instanceId)).forLocalAvailabilityZone();
        Instance instance = (Instance) dhtClientFactory.createBlockingReader().get(id);
        return new Date(instance.getInstanceActivityStateChangeTimestamp());
    }

    private List<String> getInstanceIds(String body) {
        List<String> result = new ArrayList<String>();
        for (String line : body.split("\n")) {
            if (line.contains("<td>i-")) {
                int start = line.indexOf("<td>i-") + 4;
                int end = line.indexOf("<", start);
                result.add(line.substring(start, end));
            }
        }
        return result;
    }

    private String getPostPath(String body) {
        int start = body.indexOf("<form action=\"") + "<form action=\"".length();
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    private void clobberInstanceActivityTimestamp(final String instanceId, final InstanceActivityState newState, final long newTimestamp) {
        int globalAvailabilityZoneCode = piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instanceId);
        PId id = piIdBuilder.getPId(Instance.getUrl(instanceId)).forGlobalAvailablityZoneCode(globalAvailabilityZoneCode);
        dhtClientFactory.createBlockingWriter().update(id, null, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                if (null == existingEntity) {
                    fail("instance " + instanceId + " not found in DHT");
                    return null;
                }
                existingEntity.setInstanceActivityState(newState);
                if (newTimestamp > 0)
                    setField(existingEntity, "instanceActivityStateChangeTimestamp", newTimestamp);
                return existingEntity;
            }

            @Override
            public void handleResult(Instance result) {
                System.err.println(String.format("instance %s activityState: %s, timestamp: %s", result.getInstanceId(), result.getInstanceActivityState(), new Date(result.getInstanceActivityStateChangeTimestamp())));
            }
        });
    }

    private void setField(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }

    private String waitForValidationEmail() {
        String message = null;
        int count = 0;
        while (null == (message = stubMailSender.getLastMessage()) && count < 75) {
            try {
                Thread.sleep(1000);
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return message;
    }

    @Test
    public void shouldStartUpAXLargeInstance() throws Exception {
        // setup
        launchConfig.setInstanceType(InstanceType.XLARGE_HCPU);

        // act
        // make a call to ec2 run instance
        instanceId = runInstance();

        // not act or assert.. just waiting.
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);
    }

    @Test(expected = com.xerox.amazonws.ec2.EC2Exception.class)
    public void shouldRespectUserInstanceLimit() throws Exception {
        try {
            // setup
            instanceId = runInstance();
            waitForInstanceToBeCreated(instanceId);
            waitForInstanceToBeRunning(instanceId);

            // act - modify user to set max instances
            setMaxInstancesForUser(USERID, 1);
            ec2.runInstances(launchConfig);

            // assert
        } catch (com.xerox.amazonws.ec2.EC2Exception e) {
            System.err.println(e);
            assertTrue(e.getMessage().contains(String.format("Client error : Unable to run instances as user %s currently has", USERID)));
            assertTrue(e.getMessage().contains("instances when the maximum is 1."));
            throw e;
        } finally {
            setMaxInstancesForUser(USERID, 50);
        }
    }

    @Test(expected = com.xerox.amazonws.ec2.EC2Exception.class)
    public void shouldRespectUserCoreLimit() throws Exception {
        try {
            // setup
            instanceId = runInstance();
            waitForInstanceToBeCreated(instanceId);
            waitForInstanceToBeRunning(instanceId);

            // act - modify user to set max instances
            setMaxCoresForUser(USERID, 1);
            ec2.runInstances(launchConfig);

            // assert
        } catch (com.xerox.amazonws.ec2.EC2Exception e) {
            System.err.println(e);
            assertTrue(e.getMessage().contains(String.format("Client error : Unable to run instances as user %s currently has", USERID)));
            assertTrue(e.getMessage().contains("cores when the maximum is 1."));
            throw e;
        } finally {
            setMaxCoresForUser(USERID, 50);
        }
    }

    private static void setMaxInstancesForUser(String username, int i) {
        UserManagementService userManagementService = (UserManagementService) applicationContext.getBean(UserManagementService.class);
        userManagementService.updateUser(username, USER_REAL_NAME, USER_EMAIL, true, null, i, -1);
    }

    private static void setMaxCoresForUser(String username, int i) {
        UserManagementService userManagementService = (UserManagementService) applicationContext.getBean(UserManagementService.class);
        userManagementService.updateUser(username, USER_REAL_NAME, USER_EMAIL, true, null, -1, i);
    }

    @Test
    public void shouldShutdownAnInstance() throws Exception {
        // setup
        instanceId = runInstance();

        // not act or assert.. just waiting.
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        // act
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);
        ec2.terminateInstances(instanceIds);

        // assert
        waitForInstanceToBeTerminated(instanceId);
    }

    @Test
    public void shouldShutdownACrashedInstance() throws Exception {
        // setup
        instanceId = runInstance();

        // not act or assert.. just waiting.
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        // crash it
        stubLibvirtConnection.crashARunningDomain(instanceId);
        waitForInstanceToBeCrashed(instanceId);

        // act
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);
        ec2.terminateInstances(instanceIds);

        // assert
        waitForInstanceToBeTerminated(instanceId);
    }

    @Test
    public void shouldRebootAnInstance() throws Exception {
        // setup
        instanceId = runInstance();

        // not act or assert.. just waiting.
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);
        when(stubLibvirtConnection.domainLookupByID(1).getName()).thenReturn(instanceId);

        // act
        ec2.rebootInstances(instanceIds);

        Thread.sleep(2000);

        // assert
        Domain rebootedInstance = stubLibvirtConnection.domainLookupByName(instanceId);
        verify(rebootedInstance).reboot(eq(0));
    }

    @Test
    public void shouldRebootACrashedInstance() throws Exception {
        // setup
        instanceId = runInstance();

        // not act or assert.. just waiting.
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        stubLibvirtConnection.crashARunningDomain(instanceId);
        waitForInstanceToBeCrashed(instanceId);

        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);
        when(stubLibvirtConnection.domainLookupByID(1).getName()).thenReturn(instanceId);

        // act
        ec2.rebootInstances(instanceIds);

        Thread.sleep(2000);

        // assert
        Domain rebootedInstance = stubLibvirtConnection.domainLookupByName(instanceId);
        verify(rebootedInstance).reboot(eq(0));
    }

    @Test
    public void shouldGetConsoleOutput() throws Exception {
        // start an instance
        // make a call to ec2 run instance
        RunningInstanceInteractionHandler runningInstanceHandler = (RunningInstanceInteractionHandler) applicationContext.getBean("runningInstanceInteractionHandler");
        runningInstanceHandler.setConsoleOutputDirectory(".");
        String OutputFormat = "%s-Test.log";
        String consoleOutput = "sweeet";
        runningInstanceHandler.setConsoleOutputFileNameFormat(OutputFormat);

        instanceId = runInstance();
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        File f = new File("./" + String.format(OutputFormat, instanceId));
        FileUtils.writeStringToFile(f, consoleOutput);

        // act
        ConsoleOutput console = ec2.getConsoleOutput(instanceId);

        // assert
        assertEquals(instanceId, console.getInstanceId());
        assertEquals(consoleOutput, console.getOutput());

        f.delete();
    }

    @Test
    public void testSimpleEc2Dim() throws Exception {
        // act
        List<ImageDescription> result = ec2.describeImages(new String[] { IMAGE_ID });

        // assert
        assertEquals(1, result.size());
        assertEquals(IMAGE_ID, result.get(0).getImageId());
    }

    @Test
    public void shouldDeleteSecurityGroup() throws Exception {
        // add new security group and wait for it..
        String createdSecurityGroupName = "newSecurityGroup";
        ec2.createSecurityGroup(createdSecurityGroupName, "shouldDeleteSecurityGroup");
        // wait for group to show up
        waitForSecurityGroupToBeCreated(USERID, createdSecurityGroupName);
        // do a describe security group to make sure it is there
        List<String> groupNames = new ArrayList<String>();
        groupNames.add(createdSecurityGroupName);
        groupNames.add("default");
        List<GroupDescription> securityGroups = ec2.describeSecurityGroups(groupNames);

        // assert group is there
        System.err.println("SecGroups:" + securityGroups);
        System.err.println("Group returned from describe: " + securityGroups.get(0));
        System.err.println("Group returned from describe: " + securityGroups.get(1));
        assertEquals(2, securityGroups.size());
        assertNotNull(securityGroups.get(0));
        assertNotNull(securityGroups.get(1));

        // act
        ec2.deleteSecurityGroup(createdSecurityGroupName);

        // wait for security group to be deleted
        waitForSecurityGroupToBeDeleted(USERID, createdSecurityGroupName);

        // assert deleted
        List<GroupDescription> groupsAfterDelete = ec2.describeSecurityGroups(groupNames);
        assertEquals(1, groupsAfterDelete.size());
        assertEquals(DEFAULT, groupsAfterDelete.get(0).getName());
    }

    @Test
    public void shouldAlterSecurityGroup() throws Exception {
        // setup
        String tcpProtocol = "TCP";
        int fromPort = 80;
        int toPort = 99;
        String ip = "1.2.3.4/5";
        List<String> groupNames = new ArrayList<String>();
        groupNames.add(DEFAULT);
        List<GroupDescription> securityGroups = ec2.describeSecurityGroups(groupNames);

        // check that the group has no rules.
        System.err.println("Group returned from describe: " + securityGroups.get(0));
        System.err.println("Group permisions: " + securityGroups.get(0).getPermissions());
        assertEquals(1, securityGroups.size());
        assertEquals(0, securityGroups.get(0).getPermissions().size());

        // act
        ec2.authorizeSecurityGroupIngress(DEFAULT, tcpProtocol, fromPort, toPort, ip);

        // wait
        waitForSecurityGroupToBeUpdated(DEFAULT, 1);

        // check that our rule is there
        securityGroups = ec2.describeSecurityGroups(groupNames);
        assertEquals(1, securityGroups.size());
        assertEquals(1, securityGroups.get(0).getPermissions().size());
        IpPermission networkRule = securityGroups.get(0).getPermissions().get(0);
        assertEquals(tcpProtocol, networkRule.getProtocol());
        assertEquals(fromPort, networkRule.getFromPort());
        assertEquals(toPort, networkRule.getToPort());
        assertEquals(ip, networkRule.getIpRanges().get(0));

        // delete act
        ec2.revokeSecurityGroupIngress(DEFAULT, tcpProtocol, fromPort, toPort, ip);

        // wait
        waitForSecurityGroupToBeUpdated(DEFAULT, 0);

        // assert
        securityGroups = ec2.describeSecurityGroups(groupNames);
        assertEquals(1, securityGroups.size());
        assertEquals(DEFAULT, securityGroups.get(0).getName());
        assertEquals(0, securityGroups.get(0).getPermissions().size());
    }

    @Test
    public void testEc2DescribeRegions() throws Exception {
        // act
        List<RegionInfo> regions = ec2.describeRegions(null);

        // assert
        assertEquals(2, regions.size());
        assertThatRegionsContainsRegion(regions, US_REGION_NAME, US_REGION_ENDPOINT);
        assertThatRegionsContainsRegion(regions, UK_REGION_NAME, UK_REGION_ENDPOINT);
    }

    @Test
    public void testEc2DescribeAvailabilityZones() throws Exception {
        // act
        List<AvailabilityZone> availabilityZones = ec2.describeAvailabilityZones(null);

        // assert
        assertEquals(2, availabilityZones.size());
        assertThatAvailabilityZonesContainsAvailabilityZone(availabilityZones, "c1", "UP");
        assertThatAvailabilityZonesContainsAvailabilityZone(availabilityZones, "c2", "UP");
    }

    @Test
    public void pisssTest() throws Exception {
        // setup
        String bucketName = "test1";
        String objectName = "testFile1";
        String testData = "this is a test file\n line 2";

        // act & assert
        createBucket(bucketName);
        assertBucketExists(true, bucketName);

        createObject(bucketName, objectName, testData);
        assertObjectExists(true, bucketName, objectName, testData);

        deleteObject(bucketName, objectName);
        assertObjectExists(false, bucketName, objectName, null);

        deleteBucket(bucketName);
        assertBucketExists(false, bucketName);
    }

    @Test
    public void pisssTestHttps() throws Exception {
        // setup
        String actualTimKayBaseCommand = timKayBaseCommand;
        timKayBaseCommand = "unset http_proxy;AWS_HOST=localhost AWS_PORT=8883 AWS_ACCESS_KEY_ID=" + accessKey + " AWS_SECRET_ACCESS_KEY=" + secretKey + " etc/timkay-aws/aws_modified_for_https";

        String bucketName = "test1";
        String objectName = "testFile1";
        String testData = "this is a test file\n line 2";

        // act & assert
        createBucket(bucketName);
        assertBucketExists(true, bucketName);

        createObject(bucketName, objectName, testData);
        assertObjectExists(true, bucketName, objectName, testData);

        deleteObject(bucketName, objectName);
        assertObjectExists(false, bucketName, objectName, null);

        deleteBucket(bucketName);
        assertBucketExists(false, bucketName);

        // reset
        timKayBaseCommand = actualTimKayBaseCommand;
    }

    @Test
    public void shouldDescribeImagesViaHttps() throws Exception {
        // setup
        String actualTimKayBaseCommand = timKayBaseCommand;
        timKayBaseCommand = "unset http_proxy;AWS_HOST=localhost AWS_PORT=4443 AWS_ACCESS_KEY_ID=" + accessKey + " AWS_SECRET_ACCESS_KEY=" + secretKey + " etc/timkay-aws/aws_modified_for_https";

        // act
        String command = String.format("%s -vvv  dim", timKayBaseCommand);
        runCommand(command);

        // assert
        assertResponse(commandExecutor.getOutputLines(), DEFAULT_KERNEL);
        assertResponse(commandExecutor.getOutputLines(), DEFAULT_RAMDISK);

        // reset
        timKayBaseCommand = actualTimKayBaseCommand;
    }

    @Test
    public void testKeyPairs() throws Exception {
        List<KeyPairInfo> keyPairs = ec2.describeKeyPairs(new ArrayList<String>());
        assertEquals(0, keyPairs.size());

        ec2.createKeyPair("test1");
        ec2.createKeyPair("test2");

        // test for all key pairs
        keyPairs = ec2.describeKeyPairs(new ArrayList<String>());
        assertEquals(2, keyPairs.size());
        assertTrue(keyPairs.get(0).getKeyName().equals("test1") || keyPairs.get(1).getKeyName().equals("test1"));
        assertTrue(keyPairs.get(0).getKeyName().equals("test2") || keyPairs.get(1).getKeyName().equals("test2"));
        assertNotNull(keyPairs.get(0).getKeyFingerprint());
        assertNotNull(keyPairs.get(1).getKeyFingerprint());

        // test for one key pair
        keyPairs = ec2.describeKeyPairs(Arrays.asList(new String[] { "test1" }));
        assertEquals(1, keyPairs.size());
        assertEquals("test1", keyPairs.get(0).getKeyName());
        assertNotNull(keyPairs.get(0).getKeyFingerprint());

        // test for the other key pair
        keyPairs = ec2.describeKeyPairs(Arrays.asList(new String[] { "test2" }));
        assertEquals(1, keyPairs.size());
        assertEquals("test2", keyPairs.get(0).getKeyName());
        assertNotNull(keyPairs.get(0).getKeyFingerprint());

        ec2.deleteKeyPair("test2");
        keyPairs = ec2.describeKeyPairs(new ArrayList<String>());
        assertEquals(1, keyPairs.size());
        assertEquals("test1", keyPairs.get(0).getKeyName());

        ec2.deleteKeyPair("test1");
        keyPairs = ec2.describeKeyPairs(new ArrayList<String>());
        assertEquals(0, keyPairs.size());
    }

    @Test
    public void shouldGetInstanceTypesWithoutAuthentication() throws HttpException, IOException {
        // setup
        String instanceTypesUrl = "https://localhost:8443/dhtrecords/instancetypes.json";
        System.setProperty("javax.net.ssl.trustStore", "src/integration/resources/ssl_keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "badgermonkeyfish");

        HttpClient httpClient = new HttpClient();
        // act
        GetMethod getMethod = new GetMethod(instanceTypesUrl);
        httpClient.executeMethod(getMethod);

        // assert
        assertEquals(200, getMethod.getStatusCode());
    }

    @Test
    public void testOpsWebsite() throws HttpException, IOException {
        // setup
        System.setProperty("javax.net.ssl.trustStore", "src/integration/resources/ssl_keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "badgermonkeyfish");

        final String BASE_URI = "https://localhost:8443/";
        final String STATIC_RESOURCE_URI = BASE_URI + "default.css";
        final String RESOURCE_URI = BASE_URI + "users";
        final String USER_URI = RESOURCE_URI + "/mayordiamondjoequimby";
        final String PUT_POST_CONTENT = "username=mayordiamondjoequimby&realname=Mayor%20Diamond%20Joe%20Quimby&email=mayor.diamond.joe.quimby@somewhere.com&enabled=checkbox";
        final HttpClient cli = new HttpClient();
        final Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);

        cli.getParams().setAuthenticationPreemptive(true);
        cli.getState().setCredentials(AuthScope.ANY, credentials);

        GetMethod getStaticResource = new GetMethod(STATIC_RESOURCE_URI);
        GetMethod getUser = new GetMethod(RESOURCE_URI + "/view" + "/mayordiamondjoequimby");
        getUser.setRequestHeader("accept", TEXT_HTML);
        PostMethod postUser = new PostMethod(RESOURCE_URI);
        postUser.setRequestEntity(new StringRequestEntity(PUT_POST_CONTENT, APPLICATION_FORM_URLENCODED, "UTF-8"));
        PostMethod postUserCert = new PostMethod(USER_URI + "/certificate");
        DeleteMethod deleteUser = new DeleteMethod(USER_URI);
        PutMethod putUser = new PutMethod(USER_URI);
        putUser.setRequestEntity(new StringRequestEntity(PUT_POST_CONTENT.replace("@somewhere.com", "@springfield.com"), APPLICATION_FORM_URLENCODED, "UTF-8"));

        // make sure we can pull static content
        assertEquals(200, cli.executeMethod(getStaticResource));

        // make sure the user doesn't exist
        assertEquals(404, cli.executeMethod(getUser));

        // create the user
        assertEquals(201, cli.executeMethod(postUser));
        assertContains("users/mayordiamondjoequimby", postUser.getResponseHeaders("Location")[0].getValue());

        // check the user was created and can be viewed
        assertEquals(200, cli.executeMethod(getUser));
        assertContains("Real name:</td><td>Mayor Diamond Joe Quimby", getUser.getResponseBodyAsString());
        assertContains("Username:</td><td>mayordiamondjoequimby", getUser.getResponseBodyAsString());
        assertContains("Email address:</td><td>mayor.diamond.joe.quimby@somewhere.com", getUser.getResponseBodyAsString());

        // check the user can be accessed using json
        getUser = new GetMethod(USER_URI);
        getUser.setRequestHeader("accept", APPLICATION_JSON);
        assertEquals(200, cli.executeMethod(getUser));
        assertContains("\"realName\":\"Mayor Diamond Joe Quimby\"", getUser.getResponseBodyAsString());
        assertContains("\"username\":\"mayordiamondjoequimby\"", getUser.getResponseBodyAsString());

        // check the user can be accessed using xml
        getUser.setRequestHeader("accept", APPLICATION_XML);
        assertEquals(200, cli.executeMethod(getUser));
        assertContains("<realName>Mayor Diamond Joe Quimby</realName>", getUser.getResponseBodyAsString());
        assertContains("<username>mayordiamondjoequimby</username", getUser.getResponseBodyAsString());

        // check the user's certificate can be downloaded
        assertEquals(200, cli.executeMethod(postUserCert));
        assertEquals("application/zip", postUserCert.getResponseHeaders("Content-Type")[0].getValue());

        // make a change to the user
        assertEquals(200, cli.executeMethod(putUser));

        // check that the change worked
        getUser = new GetMethod(RESOURCE_URI + "/view" + "/mayordiamondjoequimby");
        getUser.setRequestHeader("accept", TEXT_HTML);
        assertEquals(200, cli.executeMethod(getUser));
        assertContains("Real name:</td><td>Mayor Diamond Joe Quimby", getUser.getResponseBodyAsString());
        assertContains("Username:</td><td>mayordiamondjoequimby", getUser.getResponseBodyAsString());
        assertContains("Email address:</td><td>mayor.diamond.joe.quimby@springfield.com", getUser.getResponseBodyAsString());

        // delete the user
        assertEquals(204, cli.executeMethod(deleteUser));

        // check the user was deleted
        assertEquals(404, cli.executeMethod(getUser));
    }

    @Test
    public void shouldCleanUserResourcesWhenUserIsDeleted() throws Exception {
        try {
            // setup
            String userId = createATestUser();
            initialiseBucketScenario(userId);
            String imageId = initialiseImagesScenario(userId);
            initialiseSecurityGroupScenario(userId);
            List<String> volumeIds = initialiseVolumesScenario();
            List<SnapshotInfo> snapshots = initialiseSnapshotsScenario(volumeIds);
            deleteUser(userId);
            assertBucketScenario(userId);
            assertSecurityGroupScenario(userId);
            System.err.println("wait for image to be deleted");
            waitForImageToBeDeleted(imageId);
            System.err.println("Assert that all the volumes are deleted");
            assertVolumeScenario(volumeIds);
            System.err.println("Assert that all the snapshots are deleted");
            assertSnapshotScenario(snapshots);
        } finally {
            restoreOriginalTestUser();
        }
    }

    @Test
    public void shouldDisableAccessToPisssAndAPIForDisabledUsers() throws Exception {
        try {
            String userId = createATestUser();
            disableUser(userId);
            // 1. Test that the user can't access the API.
            try {
                List<ReservationDescription> instances = ec2.describeInstances(new String[] {});
                fail("API access should be disabled");

            } catch (Exception e) {
                assertTrue(e instanceof EC2Exception);
                assertTrue(e.getMessage().contains("is not enabled"));
            }
            // 2. Test that user can't access PI-SSS
            String command = String.format("%s -vvv get", timKayBaseCommand);
            String[] commands = new String[] { "/bin/bash", "-c", command };
            commandExecutor.executeScript(commands, Runtime.getRuntime());

            System.err.println(commandExecutor.getErrorLines());
            // PI-SSS should return unauthorized access
            assertResponse(commandExecutor.getErrorLines(), "401 Unauthorized");

        } finally {
            restoreOriginalTestUser();
        }
    }

    private void disableUser(String username) throws Exception {
        final String TEXT_HTML = "text/html";
        final String BASE_URI = "https://localhost:8443/";

        final String RESOURCE_URI = BASE_URI + "users";
        final String DISABLE_USER_URI = RESOURCE_URI + "/disable/" + username;

        final HttpClient cli = new HttpClient();
        final Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);

        cli.getParams().setAuthenticationPreemptive(true);
        cli.getState().setCredentials(AuthScope.ANY, credentials);
        PostMethod disableUser = new PostMethod(DISABLE_USER_URI);
        GetMethod getUser = new GetMethod(RESOURCE_URI + "/view/" + username);
        getUser.setRequestHeader("accept", TEXT_HTML);
        // print user
        cli.executeMethod(getUser);

        // delete the user
        assertEquals(200, cli.executeMethod(disableUser));

        // check the user was disabled
        assertEquals(200, cli.executeMethod(getUser));
        System.err.println(getUser.getResponseBodyAsString());
        assertTrue(getUser.getResponseBodyAsString().contains("disabled"));
    }

    private void initialiseBucketScenario(String userId) throws Exception {
        // create buckets, one will be empty, the other will contain two objects.
        System.err.println("create buckets, one will be empty, the other will contain two object");
        createBucket(emptyBucketName);
        assertBucketExists(true, emptyBucketName);
        createBucket(nonEmptyBucketName);
        assertBucketExists(true, nonEmptyBucketName);
        String testData = "onetwothreefourfive\n This is the second line \n this is the third line";
        createObject(nonEmptyBucketName, "object1", testData);
        assertObjectExists(true, nonEmptyBucketName, "object1", testData);
        createObject(nonEmptyBucketName, "object2", testData);
        assertObjectExists(true, nonEmptyBucketName, "object2", testData);
        // wait for the buckets to be stored in the user entity.
        final PId userIdToCheck = piIdBuilder.getPId(User.getUrl(userId));

        waitForEntity(userIdToCheck, new EntityChecker() {

            @Override
            public boolean shouldRetry(PiEntity entity) {
                boolean retry = true;
                final User user = (User) entity;
                System.err.println(new KoalaJsonParser().getJson(user));
                if (user != null && !user.isDeleted()) {
                    if (user.getBucketNames().contains(emptyBucketName) && user.getBucketNames().contains(nonEmptyBucketName)) {
                        retry = false;
                    }
                }
                return retry;
            }
        }, new AtomicInteger(10), 2 * 1000);
    }

    private void assertBucketScenario(String userId) throws Exception {
        waitForUserToBeDeleted(userId);
        // assert all buckets have been deleted
        System.err.println("Assert all buckets have been deleted");
        waitForBucketToBeDeleted(emptyBucketName, new AtomicInteger(10), 2 * 1000);
        waitForBucketToBeDeleted(nonEmptyBucketName, new AtomicInteger(10), 2 * 1000);

        // assert that an archived directory was created for non empty bucket
        System.err.println("assert that an archived directory was created for non empty bucket");
        boolean hasCopyCommand = false;

        String archiveCommandString = "[nice, -n, +10, ionice, -c3, cp, -r, var/buckets/nonEmptyBucket, var/buckets_archive/nonEmptyBucket-";
        System.err.println("archive command String: " + archiveCommandString);
        for (String[] command : stubCommandExecutor.getCommands()) {
            String commandString = Arrays.toString(command);
            if (commandString.startsWith(archiveCommandString)) {
                System.err.println("command string: " + commandString);
                hasCopyCommand = true;
            }
        }
        assertTrue(hasCopyCommand);
    }

    private void restoreOriginalTestUser() throws Exception {
        UserManagementService userManagementService = (UserManagementService) applicationContext.getBean(UserManagementService.class);
        userManagementService.deleteUser(USERID);
        ec2 = createUserAndUseCertsAndEndpointsInPircFile(USERID, DOT_PIRC_DIRECTORY);
        timKayBaseCommand = "touch ~/.awsrc;unset http_proxy;AWS_HOST=localhost AWS_PORT=9090 AWS_ACCESS_KEY_ID=" + accessKey + " AWS_SECRET_ACCESS_KEY=" + secretKey + " etc/timkay-aws/aws --simple --insecure-aws";
    }

    private void assertSecurityGroupScenario(String userId) throws Exception {
        waitForSecurityGroupToBeDeleted(userId, secgroup1);
        waitForSecurityGroupToBeDeleted(userId, secgroup2);
    }

    private void initialiseSecurityGroupScenario(String userId) throws EC2Exception, Exception {
        // now add some security groups.
        System.err.println("Adding secgroup1");
        ec2.createSecurityGroup(secgroup1, "First security group");
        waitForSecurityGroupToBeCreated(userId, secgroup1);
        System.err.println("Creating secgroup2");
        ec2.createSecurityGroup(secgroup2, "Second security group");
        waitForSecurityGroupToBeCreated(userId, secgroup2);
    }

    private String initialiseImagesScenario(String userId) throws Exception {
        String javaHome = System.getProperty("java.home", "/usr");
        String amitoolsCommandBase = String.format("unset http_proxy; JAVA_HOME=%s EC2_AMITOOL_HOME=etc/ec2-ami-tools-1.3-26357 etc/ec2-ami-tools-1.3-26357/bin/", javaHome);
        String s3Url = "http://localhost:9090";

        // bundle image
        runCommand(String.format("%s%s --debug --batch --image %s --cert %s --privatekey %s --user 000000000000 --ec2cert %s --kernel %s --ramdisk %s", amitoolsCommandBase, "ec2-bundle-image", "src/integration/resources/test-file",
                getUserCertPath(), getUserPrivateKeyPath(), getEc2CertPath(), DEFAULT_KERNEL, DEFAULT_RAMDISK));

        String manifestFile = "/tmp/test-file.manifest.xml";
        String bucketName = "mytestbucket";
        // act
        runCommand(String.format("%s%s --url %s --debug --bucket %s --manifest %s --access-key %s --secret-key %s", amitoolsCommandBase, "ec2-upload-bundle", s3Url, bucketName, manifestFile, accessKey, secretKey));

        // assert
        assertBucketExists(true, bucketName);
        assertObjectExists(true, bucketName, "test-file.part.0", FileUtils.readFileToString(new File("/tmp/test-file.part.0")), "binary/octet-stream");
        assertObjectExists(true, bucketName, "test-file.part.1", FileUtils.readFileToString(new File("/tmp/test-file.part.1")), "binary/octet-stream");

        // act
        String imageId = ec2.registerImage(String.format("%s/test-file.manifest.xml", bucketName));
        System.err.println("Image Id: " + imageId);

        // assert
        List<ImageDescription> images = ec2.describeImages(new String[] {});
        assertEquals(4, images.size());
        assertImageIdIsInList(IMAGE_ID, images);
        assertImageIdIsInList(imageId, images, bucketName + "/test-file.manifest.xml", null);
        System.err.println("wait for image to become available");
        waitForImageState(imageId, ImageState.AVAILABLE);
        PId userIdToCheck = piIdBuilder.getPId(User.getUrl(userId));
        User user = (User) dhtClientFactory.createBlockingReader().get(userIdToCheck);
        System.err.println(new KoalaJsonParser().getJson(user));
        return imageId;
    }

    private List<SnapshotInfo> initialiseSnapshotsScenario(List<String> volumeIds) throws Exception {
        List<SnapshotInfo> snapshots = new ArrayList<SnapshotInfo>();
        for (String volumeId : volumeIds) {
            snapshots.add(createSnapshot(volumeId, getVolumeFilename(volumeId)));
        }
        System.err.println("Snapshot scenario initilialised: " + new KoalaJsonParser().getJson(snapshots));
        return snapshots;
    }

    private List<String> initialiseVolumesScenario() throws Exception {
        return Arrays.asList(createVolume(null), createVolume(null));
    }

    private void assertSnapshotScenario(List<SnapshotInfo> snapshots) throws Exception {
        for (SnapshotInfo snapshot : snapshots) {
            waitForSnapshotStatus(snapshot.getSnapshotId(), SnapshotState.DELETED);
        }
    }

    private void assertVolumeScenario(List<String> volumeIds) throws Exception {
        for (String volumeId : volumeIds) {
            waitForVolumeStatus(volumeId, VolumeState.DELETED);
        }
    }

    private void waitForImageToBeDeleted(String imageId) throws Exception {
        PId imagePid = piIdBuilder.getPId(Image.getUrl(imageId));
        waitForEntity(imagePid, new EntityChecker() {

            @Override
            public boolean shouldRetry(PiEntity entity) {

                Image image = (Image) entity;
                System.err.println(new KoalaJsonParser().getJson(image));
                if (image.isDeleted())
                    return false;
                return true;
            }
        }, new AtomicInteger(30), 2 * 1000);
    }

    @Test
    public void shouldPauseAndUnPauseInstanceFromOpsWebsite() throws Exception {
        // setup
        // make a call to ec2 run instance
        instanceId = runInstance();

        // not act or assert.. just waiting.
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        String pauseInstanceUrl = String.format("https://localhost:8443/instances/%s/pause", instanceId);

        // act to pause
        HttpClient httpClient = logInClient();

        PostMethod postMethod = new PostMethod(pauseInstanceUrl);
        httpClient.executeMethod(postMethod);

        System.out.println("Post response code:" + postMethod.getStatusCode());

        // assert
        Thread.sleep(10 * 1000);
        Domain pausedInstance = stubLibvirtConnection.domainLookupByName(instanceId);
        assertEquals(DomainState.VIR_DOMAIN_PAUSED, pausedInstance.getInfo().state);

        // act to unpause
        String unPauseInstanceUrl = String.format("https://localhost:8443/instances/%s/unpause", instanceId);
        postMethod = new PostMethod(unPauseInstanceUrl);
        httpClient.executeMethod(postMethod);

        System.out.println("Post response code:" + postMethod.getStatusCode());

        // assert
        Thread.sleep(10 * 1000);
        Domain unpausedInstance = stubLibvirtConnection.domainLookupByName(instanceId);
        assertEquals(DomainState.VIR_DOMAIN_RUNNING, unpausedInstance.getInfo().state);

        // act to pause an unknown instance
        String unKnownInstanceUrl = String.format("https://localhost:8443/instances/i-notthere/unpause");
        postMethod = new PostMethod(unKnownInstanceUrl);
        httpClient.executeMethod(postMethod);

        System.out.println("Post response code:" + postMethod.getStatusCode());

        // assert
        Thread.sleep(10 * 1000);
        assertNotSame(Status.OK, postMethod.getStatusCode());
    }

    @Test
    public void shouldPauseAndUnPauseInstanceWithIpAddressFromOpsWebsite() throws Exception {
        // setup
        // make a call to ec2 run instance
        instanceId = runInstance();

        // not act or assert.. just waiting.
        waitForInstanceToBeCreated(instanceId);
        waitForInstanceToBeRunning(instanceId);

        runCommand(timKayBaseCommand + " din " + instanceId);
        List<String> outputLines = commandExecutor.getOutputLines();
        System.err.println(outputLines);

        Matcher matcher = Pattern.compile(String.format("^%s\\s+running\\s+(\\S+).*?$", instanceId)).matcher(outputLines.get(0));
        System.err.println(matcher.matches());
        System.err.println(matcher.group(0));
        String ipAddress = matcher.group(1);

        String pauseInstanceUrl = "https://localhost:8443/instances/pause";

        // act to pause
        HttpClient httpClient = logInClient();

        PostMethod postMethod = new PostMethod(pauseInstanceUrl);
        postMethod.addParameter("ipAddress", ipAddress);
        httpClient.executeMethod(postMethod);

        System.out.println("Post response code:" + postMethod.getStatusCode());

        // assert
        Thread.sleep(10 * 1000);
        Domain pausedInstance = stubLibvirtConnection.domainLookupByName(instanceId);
        assertEquals(DomainState.VIR_DOMAIN_PAUSED, pausedInstance.getInfo().state);

        // act to unpause
        String unPauseInstanceUrl = "https://localhost:8443/instances/unpause";
        postMethod = new PostMethod(unPauseInstanceUrl);
        postMethod.addParameter("ipAddress", ipAddress);
        httpClient.executeMethod(postMethod);

        System.out.println("Post response code:" + postMethod.getStatusCode());

        // assert
        Thread.sleep(10 * 1000);
        Domain unpausedInstance = stubLibvirtConnection.domainLookupByName(instanceId);
        assertEquals(DomainState.VIR_DOMAIN_RUNNING, unpausedInstance.getInfo().state);
    }

    @Test
    public void shouldProvideCRUDForManagementUserFromOpsWebsite() throws HttpException, IOException, InterruptedException {
        // setup
        String managementUsersUrl = "https://localhost:8443/managementusers";
        String johnDoeUrl = String.format("%s/johndoe", managementUsersUrl);

        HttpClient httpClient = logInClient();

        // adding johndoe
        PostMethod postMethod = new PostMethod(managementUsersUrl);
        postMethod.addParameter("username", "johndoe");
        postMethod.addParameter("password", StringUtils.reverse("johndoe"));
        postMethod.addParameter("roles", ManagementRoles.ROLE_MIS.name());
        httpClient.executeMethod(postMethod);

        LOG.debug("Post successful:" + postMethod.getStatusCode());

        // assert
        ReadOnlyManagementUser johnDoeUser = getJohnDoe(johnDoeUrl, httpClient);
        assertEquals("johndoe", johnDoeUser.getUsername());
        assertEquals("ROLE_MIS", johnDoeUser.getRoles());

        // update johndoe
        PutMethod putMethod = new PutMethod(johnDoeUrl);
        putMethod.setRequestEntity(new StringRequestEntity("username=johndoe&password=pass&roles=ROLE_OPS", APPLICATION_FORM_URLENCODED, "UTF-8"));
        httpClient.executeMethod(putMethod);

        LOG.debug("Update successful:" + putMethod.getStatusCode());

        // assert
        johnDoeUser = getJohnDoe(johnDoeUrl, httpClient);
        assertEquals("johndoe", johnDoeUser.getUsername());
        assertEquals("ROLE_OPS", johnDoeUser.getRoles());

        // delete johndoe
        DeleteMethod deleteMethod = new DeleteMethod(johnDoeUrl);
        httpClient.executeMethod(deleteMethod);

        LOG.debug("Delete successful:" + deleteMethod.getStatusCode());

        // get all users and make sure that johndoe is not there
        GetMethod getMethod = new GetMethod(managementUsersUrl);
        httpClient.executeMethod(getMethod);

        assertEquals(200, getMethod.getStatusCode());
        ReadOnlyManagementUsers allManagementUsers = (ReadOnlyManagementUsers) new KoalaJsonParser().getObject(getMethod.getResponseBodyAsString(), ReadOnlyManagementUsers.class);

        // assert
        assertEquals(1, allManagementUsers.getManagementUsers().size());
    }

    private ReadOnlyManagementUser getJohnDoe(String johnDoeUrl, HttpClient httpClient) throws IOException, HttpException {
        GetMethod getMethod = new GetMethod(johnDoeUrl);
        httpClient.executeMethod(getMethod);

        String responseBody = getMethod.getResponseBodyAsString();
        System.out.println("----- GetMethod response:" + responseBody);

        assertEquals(200, getMethod.getStatusCode());
        return (ReadOnlyManagementUser) new KoalaJsonParser().getObject(responseBody, ReadOnlyManagementUser.class);
    }

    private HttpClient logInClient() {
        System.setProperty("javax.net.ssl.trustStore", "src/integration/resources/ssl_keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "badgermonkeyfish");
        HttpClient cli = new HttpClient();
        cli.getParams().setAuthenticationPreemptive(true);
        Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
        cli.getState().setCredentials(AuthScope.ANY, credentials);

        return cli;
    }

    private String createATestUser() throws Exception {
        System.setProperty("javax.net.ssl.trustStore", "src/integration/resources/ssl_keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "badgermonkeyfish");
        String userId = "testUser";
        File tempCerts = new File(DOT_PIRC_DIRECTORY);

        FileUtils.deleteDirectory(tempCerts);
        System.err.println("Create test User and Certificates in Pirc file");
        ec2 = createUserAndUseCertsAndEndpointsInPircFile(userId, DOT_PIRC_DIRECTORY);
        timKayBaseCommand = "touch ~/.awsrc;unset http_proxy;AWS_HOST=localhost AWS_PORT=9090 AWS_ACCESS_KEY_ID=" + accessKey + " AWS_SECRET_ACCESS_KEY=" + secretKey + " etc/timkay-aws/aws --simple --insecure-aws";
        return userId;
    }

    private void deleteUser(String username) throws Exception {
        final String TEXT_HTML = "text/html";
        final String BASE_URI = "https://localhost:8443/";

        final String RESOURCE_URI = BASE_URI + "users";
        final String USER_URI = RESOURCE_URI + "/" + username;

        final HttpClient cli = new HttpClient();
        final Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);

        cli.getParams().setAuthenticationPreemptive(true);
        cli.getState().setCredentials(AuthScope.ANY, credentials);
        DeleteMethod deleteUser = new DeleteMethod(USER_URI);
        GetMethod getUser = new GetMethod(RESOURCE_URI + "/view/" + username);
        getUser.setRequestHeader("accept", TEXT_HTML);
        // print user
        cli.executeMethod(getUser);

        // delete the user
        assertEquals(204, cli.executeMethod(deleteUser));

        // check the user was deleted
        assertEquals(404, cli.executeMethod(getUser));

    }

    private void waitForBucketToBeDeleted(String bucketName, final AtomicInteger retries, final long retryInterval) throws Exception {
        boolean shouldRetry = true;
        try {
            assertBucketExists(false, bucketName);
            shouldRetry = false;
        } catch (Throwable t) {

        }
        if (retries.get() > 0 && shouldRetry) {
            Thread.sleep(retryInterval);
            System.err.println("Retries left: " + retries);
            waitForBucketToBeDeleted(bucketName, new AtomicInteger(retries.decrementAndGet()), retryInterval);
        } else if (retries.get() <= 0 && shouldRetry) {
            fail("Exeeded number of retries waiting for bucket to be deleted: " + bucketName);
        }
    }

    protected void assertContains(String expected, String actual) {
        if (!actual.contains(expected)) {
            throw new AssertionFailedError(String.format("Did not find expected string in actual result\nExpected:\n %s\n\nActual:\n%s", expected, actual));
        }
    }

    private void assertBucketExists(boolean bucketExists, String bucketName) throws Exception {
        String command = String.format("%s -vvv get %s", timKayBaseCommand, bucketName);
        String[] commands = new String[] { "/bin/bash", "-c", command };
        commandExecutor.executeScript(commands, Runtime.getRuntime());

        System.err.println(commandExecutor.getErrorLines());

        if (bucketExists)
            assertResponse(commandExecutor.getErrorLines(), "200 OK");
        else
            assertResponse(commandExecutor.getErrorLines(), "404 Not Found");
    }

    private void createObject(String bucketName, String objectName, String testData) throws Exception {
        String tmpFilePath = writeToTmpFile(testData);
        String command = String.format("%s -vvv put \"Content-Type: text/plain\" %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFilePath);
        runCommand(command);
    }

    private void deleteObject(String bucketName, String objectName) throws Exception {
        String command = String.format("%s -vvv delete %s/%s", timKayBaseCommand, bucketName, objectName);
        runCommand(command);
    }

    private void createBucket(String bucketName) throws Exception {
        String command = String.format("%s -vvv mkdir %s", timKayBaseCommand, bucketName);
        runCommand(command);
    }

    private void deleteBucket(String bucketName) throws Exception {
        String command = String.format("%s -vvv delete %s", timKayBaseCommand, bucketName);
        runCommand(command);
    }

    private void assertObjectExists(boolean objectExists, String bucketName, String objectName, String testData) throws Exception {
        assertObjectExists(objectExists, bucketName, objectName, testData, "text/plain");
    }

    private void assertObjectExists(boolean objectExists, String bucketName, String objectName, String testData, String contentType) throws Exception {
        File tmpFile = File.createTempFile("unittesting", null);
        tmpFile.deleteOnExit();
        String command = String.format("%s -vvv get %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFile.getAbsolutePath());
        runCommand(command);
        if (objectExists) {
            assertResponse(commandExecutor.getErrorLines(), contentType);
            assertEquals(testData, FileUtils.readFileToString(tmpFile));
        } else
            assertResponse(commandExecutor.getErrorLines(), "404 Not Found");
    }

    private void assertResponse(List<String> lines, String target) {
        for (String line : lines)
            if (line.contains(target))
                return;
        System.err.println("Output lines:");
        System.err.println(ArrayUtils.toString(lines));
        System.err.println("Error lines:");
        System.err.println(ArrayUtils.toString(commandExecutor.getErrorLines()));
        fail();
    }

    private String writeToTmpFile(String data) throws Exception {
        File tmpFile = File.createTempFile("unittesting", null);
        tmpFile.deleteOnExit();
        FileUtils.writeStringToFile(tmpFile, data);
        return tmpFile.getAbsolutePath();
    }

    private void assertThatAvailabilityZonesContainsAvailabilityZone(List<AvailabilityZone> availabilityZones, String availabilityZoneName, String status) {
        for (AvailabilityZone availabilityZone : availabilityZones) {
            if (availabilityZone.getName().equals(availabilityZoneName) && availabilityZone.getState().equals(status))
                return;
        }

        fail(String.format("%s with status %s not found in %s", availabilityZoneName, status, availabilityZones));
    }

    private void assertThatRegionsContainsRegion(List<RegionInfo> regions, String regionName, String regionEndpoint) {
        for (RegionInfo region : regions) {
            if (region.getName().equals(regionName) && region.getUrl().equals(regionEndpoint))
                return;
        }

        fail(String.format("%s with endpoint %s not found in %s", regionName, regionEndpoint, regions));
    }

    private static void prepareSystem() throws Exception {
        System.err.println("prepareSystem");
        addSystemConfiguration();
        addApplications();

        waitForApplication(NetworkManagerApplication.APPLICATION_NAME, NodeScope.AVAILABILITY_ZONE);
        waitForApplication(ApiApplicationManager.APPLICATION_NAME, NodeScope.REGION);
        waitForApplication(PisssApplicationManager.APPLICATION_NAME, NodeScope.REGION);
        waitForApplication(OpsWebsiteApplicationManager.APPLICATION_NAME, NodeScope.REGION);

        AnycastHandler anycastHandler = (AnycastHandler) applicationContext.getBean("anycastHandler");
        anycastHandler.refreshInstanceTypes();
    }

    @SuppressWarnings("unchecked")
    private static Jec2 createUserAndUseCertsAndEndpointsInPircFile(String userId, String pircDirectory) throws Exception {
        System.err.println(String.format("createUserAndUseCertsAndEndpointsInPircFile(%s,%s)", userId, pircDirectory));
        UserManagementService userManagementService = (UserManagementService) applicationContext.getBean(UserManagementService.class);
        userManagementService.createPiCertificate();

        byte[] userCerts = userManagementService.createUser(userId, USER_REAL_NAME, USER_EMAIL);
        File tempCerts = new File(pircDirectory);
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
        String piHost = null;// , pisssHost = null;
        int piPort = 0;// , pisssPort = 0;
        for (String line : pircLines) {
            System.err.println(line);
            if (line.startsWith("export EC2_URL=")) {
                String url = line.substring("export EC2_URL=".length(), line.length());
                piHost = url.substring(0, url.lastIndexOf(":"));
                piPort = Integer.parseInt(url.substring(piHost.length() + 1));
            }

            if (line.startsWith("export EC2_ACCESS_KEY='"))
                accessKey = line.substring("export EC2_ACCESS_KEY='".length(), line.length() - 1);
            if (line.startsWith("export EC2_SECRET_KEY='"))
                secretKey = line.substring("export EC2_SECRET_KEY='".length(), line.length() - 1);
        }

        setMaxInstancesForUser(userId, 30);
        setMaxCoresForUser(userId, 50);
        return new Jec2(accessKey, secretKey, false, piHost, piPort);
    }

    private void waitForSecurityGroupToBeUpdated(final String securityGroupName, final Integer networkRules) throws Exception {
        System.err.println("waitForSecurityGroup");
        PId securityGroupIdToCheck = piIdBuilder.getPId(SecurityGroup.getUrl(USERID, securityGroupName)).forLocalRegion();
        waitForEntity(securityGroupIdToCheck, new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                boolean retry = true;
                SecurityGroup sg = (SecurityGroup) entity;
                if (sg != null && sg.getDescription() != null && sg.getSecurityGroupId().contains(securityGroupName) && sg.getNetworkRules().size() == networkRules.intValue()) {
                    retry = false;
                }
                return retry;
            }
        }, new AtomicInteger(10), 2 * 1000);
    }

    private void waitForSecurityGroupToBeCreated(String userId, final String securityGroupName) throws Exception {
        System.err.println("waitForSecurityGroup");
        PId securityGroupIdToCheck = piIdBuilder.getPId(SecurityGroup.getUrl(userId, securityGroupName)).forLocalRegion();
        waitForEntity(securityGroupIdToCheck, new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                boolean retry = true;
                SecurityGroup sg = (SecurityGroup) entity;
                if (sg != null && sg.getDescription() != null && sg.getSecurityGroupId().contains(securityGroupName)) {
                    retry = false;
                }
                return retry;
            }
        }, new AtomicInteger(10), 2 * 1000);
    }

    private void waitForSecurityGroupToBeDeleted(String userId, final String securityGroupName) throws Exception {
        System.err.println("waitForSecurityGroup");
        PId securityGroupIdToCheck = piIdBuilder.getPId(SecurityGroup.getUrl(userId, securityGroupName)).forLocalRegion();
        waitForEntity(securityGroupIdToCheck, new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                boolean retry = true;
                SecurityGroup sg = (SecurityGroup) entity;
                if (sg == null || sg.isDeleted()) {
                    retry = false;
                }
                return retry;
            }
        }, new AtomicInteger(10), 2 * 1000);
    }

    private void waitForUserToBeDeleted(final String userName) throws Exception {
        System.err.println("waitForUserToBeDeleted");
        PId userIdToCheck = piIdBuilder.getPId(User.getUrl(userName));
        waitForEntity(userIdToCheck, new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                boolean retry = true;
                User user = (User) entity;
                System.err.println(new KoalaJsonParser().getJson(user));
                if (user == null || user.isDeleted()) {
                    retry = false;
                }
                return retry;
            }
        }, new AtomicInteger(10), 2 * 1000);
    }

    private void waitForInstanceToBeTerminated(String instanceId) throws Exception {
        waitForInstanceToBeTerminated(instanceId, 10);
    }

    private void waitForInstanceToBeTerminated(String instanceId, int retries) throws Exception {
        System.err.println(String.format("waitForInstanceToBeTerminated(%s, %d)", instanceId, retries));
        PId instanceIdToCheck = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        waitForEntity(instanceIdToCheck, new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                Instance instance = (Instance) entity;
                if (instance != null && instance.getState() != null && instance.getState().equals(InstanceState.TERMINATED)) {
                    return false;
                }
                return true;
            }
        }, new AtomicInteger(retries), 2 * 1000);
    }

    // I will make these waits prettier later. :D
    private void waitForInstanceToBeCreated(final String instanceId) throws Exception {
        System.err.println(String.format("waitForInstanceToBeCreated(%s)", instanceId));
        String securityGroupUrl = SecurityGroup.getUrl(USERID, SECURITY_GROUP_NAME);
        PId securityGroupId = koalaIdFactory.buildPId(securityGroupUrl).forLocalRegion();
        waitForEntity(securityGroupId, new EntityChecker() {
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
        }, new AtomicInteger(30), 2 * 1000);
        Thread.sleep(2 * 1000);
    }

    private void waitForInstanceToBeRunning(final String instanceId) throws Exception {
        System.err.println(String.format("waitForInstanceToBeRunning(%s)", instanceId));
        waitForInstanceToBeInAState(instanceId, InstanceState.RUNNING);
    }

    private void waitForInstanceToBeCrashed(final String instanceId) throws Exception {
        System.err.println(String.format("waitForInstanceToBeCrashed(%s)", instanceId));
        waitForInstanceToBeInAState(instanceId, InstanceState.CRASHED);
    }

    private void waitForInstanceToBeInAState(final String instanceId, final InstanceState instanceState) throws Exception {
        String securityGroupUrl = SecurityGroup.getUrl(USERID, SECURITY_GROUP_NAME);
        PId securityGroupId = koalaIdFactory.buildPId(securityGroupUrl).forLocalRegion();
        waitForEntity(securityGroupId, new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                List<String> instanceIds = new ArrayList<String>();
                instanceIds.add(instanceId);
                try {
                    List<ReservationDescription> instanceList = ec2.describeInstances(instanceIds);
                    if (instanceList.size() == 1) {
                        com.xerox.amazonws.ec2.ReservationDescription.Instance runninInstance = instanceList.get(0).getInstances().get(0);
                        System.err.println(String.format("%s: %s", runninInstance.getInstanceId(), runninInstance.getState()));
                        if (instanceState.toString().equalsIgnoreCase(runninInstance.getState())) {
                            return false;
                        }
                    }
                    return true;
                } catch (EC2Exception e) {
                    e.printStackTrace();
                    return true;
                }
            }
        }, new AtomicInteger(35), 2 * 1000);
    }

    private static void waitForApplication(String applicationName, NodeScope scope) throws Exception {
        System.err.println(String.format("waitForApplication(%s)", applicationName));
        PId entityId = null;
        if (NodeScope.REGION.equals(scope)) {
            entityId = koalaIdFactory.buildPId(RegionScopedApplicationRecord.getUrl(applicationName)).forLocalRegion();
        } else
            entityId = koalaIdFactory.buildPId(AvailabilityZoneScopedApplicationRecord.getUrl(applicationName)).forLocalAvailabilityZone();
        waitForEntity(entityId, new EntityChecker() {
            @Override
            public boolean shouldRetry(PiEntity entity) {
                boolean retry = true;
                ApplicationRecord applicationRecord = (ApplicationRecord) entity;
                if (applicationRecord != null && applicationRecord.getNumCurrentlyActiveNodes() > 0) {
                    retry = false;
                }
                return retry;
            }
        }, new AtomicInteger(30), 2 * 1000);
    }

    private static void waitForEntity(final PId entityId, final EntityChecker checker, final AtomicInteger retries, long retryInterval) throws Exception {
        BlockingDhtReader dhtReader = dhtClientFactory.createBlockingReader();
        boolean shouldRetry = checker.shouldRetry(dhtReader.get(entityId));
        if (retries.get() > 0 && shouldRetry) {
            Thread.sleep(retryInterval);
            System.err.println("Retries left: " + retries);
            waitForEntity(entityId, checker, new AtomicInteger(retries.decrementAndGet()), retryInterval);
        } else if (retries.get() <= 0 && shouldRetry) {
            fail("Exeeded number of retries waiting for entity with id: " + entityId.toStringFull());
        }
    }

    private static void addApplications() {
        piSeeder.createApplicationRecordForAvailabilityZone(NetworkManagerApplication.APPLICATION_NAME, "c1", "1");
        piSeeder.createApplicationRecordForRegion(ApiApplicationManager.APPLICATION_NAME, US_REGION_NAME, "1.2.3.4");
        piSeeder.createApplicationRecordForRegion(VolumeManagerApplication.APPLICATION_NAME, US_REGION_NAME, "1");
        piSeeder.createApplicationRecordForRegion(PisssApplicationManager.APPLICATION_NAME, US_REGION_NAME, "127.0.0.1");
        piSeeder.createApplicationRecordForRegion(OpsWebsiteApplicationManager.APPLICATION_NAME, US_REGION_NAME, "127.0.0.1");
        piSeeder.createApplicationRecordForRegion(VolumeBackupManagerApplication.APPLICATION_NAME, US_REGION_NAME, "1");
    }

    private static void addSystemConfiguration() {
        piSeeder.configureInstanceTypes(InstanceType.DEFAULT.getTypeId() + ";" + InstanceType.XLARGE_HCPU.getTypeId(), "1;4", "0;0", "0;0");
        piSeeder.configureRegions(String.format("%s;%s", US_REGION_NAME, UK_REGION_NAME), String.format("%s;%s", US_REGION_CODE, UK_REGION_CODE), String.format("%s;%s", US_REGION_ENDPOINT, UK_REGION_ENDPOINT), String.format("%s;%s",
                US_REGION_ENDPOINT, UK_REGION_ENDPOINT));
        piSeeder.configureAvailabilityZones("c1;c2", "99;121", String.format("%s;%s", US_REGION_CODE, UK_REGION_CODE), "UP;UP");
        piSeeder.createPublicAddressAllocationIndex("10.0.0.1-10.0.0.14", US_REGION_NAME);
        piSeeder.createVlanAllocationIndex("30-35", US_REGION_NAME);
        piSeeder.createSubnetAllocationIndex("172.30.250.0/24;16", "147.149.2.5", US_REGION_NAME);
        piSeeder.addTaskProcessingQueue("CREATE_VOLUME");
        piSeeder.addTaskProcessingQueue("ATTACH_VOLUME");
        piSeeder.addTaskProcessingQueue("DETACH_VOLUME");
        piSeeder.addTaskProcessingQueue("DECRYPT_IMAGE");
        piSeeder.addTaskProcessingQueue("RUN_INSTANCE");
        piSeeder.addTaskProcessingQueue("PAUSE_INSTANCE");
        piSeeder.addTaskProcessingQueue("ASSOCIATE_ADDRESS");
        piSeeder.addTaskProcessingQueue("DISASSOCIATE_ADDRESS");
        // creating management user with password of 'password'
        piSeeder.createManagementUser(USERNAME, md5Hash(PASSWORD), "ROLE_OPS");
        writeImage();
    }

    private static void writeImage() {
        piSeeder.createImage(DEFAULT_RAMDISK, (String) null, (String) null, (String) null, (String) null, "i386", ImagePlatform.linux.toString(), true, MachineType.RAMDISK.toString(), US_REGION_NAME);
        piSeeder.createImage(DEFAULT_KERNEL, (String) null, (String) null, (String) null, (String) null, "i386", ImagePlatform.linux.toString(), true, MachineType.KERNEL.toString(), US_REGION_NAME);
        piSeeder.createImage(IMAGE_ID, DEFAULT_KERNEL, DEFAULT_RAMDISK, (String) null, (String) null, "i386", ImagePlatform.linux.toString(), true, MachineType.MACHINE.toString(), US_REGION_NAME);
    }

    private void runCommand(String command) throws Exception {
        String[] commands = new String[] { "/bin/bash", "-c", command };
        commandExecutor.executeScript(commands, Runtime.getRuntime());
    }

    protected static String md5Hash(String pass) {
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
}

interface EntityChecker {
    boolean shouldRetry(PiEntity entity);
}
