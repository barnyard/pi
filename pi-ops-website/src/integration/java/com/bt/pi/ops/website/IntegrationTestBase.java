package com.bt.pi.ops.website;

import static org.junit.Assert.fail;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.management.JMException;

import junit.framework.AssertionFailedError;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.pi.app.common.UserServiceHelper;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageIndex;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.ManagementRoles;
import com.bt.pi.app.common.entities.ManagementUser;
import com.bt.pi.app.common.entities.ManagementUsers;
import com.bt.pi.app.common.entities.PiCertificate;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.UserAccessKey;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.instancemanager.testing.StubMailSender;
import com.bt.pi.app.management.PiSeeder;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.ragstorooks.testrr.cli.CommandExecutor;

public abstract class IntegrationTestBase {
	protected static final String USERNAME = "Mr Barney Gumble";
	protected static final String REGION = "cringin region";
	protected static final String SECURITY_GROUP_NAME = "sec-group1";
	protected static final String AVAILABILITY_ZONE = "drone zone";
	protected static final String KERNEL_ID = "kmi-1234";
	protected static final String MIS_USER_USERNAME = "misuser";
	protected static final String PROVISIONING_USER_USERNAME = "provisioninguser";
	protected static final String OPS_USER_USERNAME = "opsuser";
	protected static final String PASSWORD = "password";
	protected static ClassPathXmlApplicationContext classPathXmlApplicationContext = null;
	protected static final String SECRET_KEY = "sIL03bvLbAa__Oa7Oe5Ssuhjalg6m-cd0RDwOg";
	protected static final String BAD_SECRET_KEY = "xxxxxxxxxxx__xxxxxxxxxxxxxxxx-xxxxxxxx";
	protected static final String ACCESS_KEY = "Y7HqdJgHC7aYIbnxPRNU1A";
	protected static final String BAD_ACCESS_KEY = "xxxxxxxxxxxxxxxxxxxxxx";
	protected CommandExecutor commandExecutor;
	protected static final String PORT = "9090";
	protected static String s3Url = "http://localhost:" + PORT;

	protected static final String BUCKET_ROOT = "var/buckets";
	protected static final String USER_NAME = "bob";
	protected static final String BASE_URI = "https://localhost:8443/";
	protected String bucketName = "test1";
	protected String objectName = "testFile1";
	protected String testData = "this is a test file\n line 2";
	protected HttpClient opsUserHttpClient;
	protected HttpClient misUserHttpClient;
	protected HttpClient provisioningUserHttpClient;
	protected HttpClient anonymousHttpClient;
	private static KoalaNode koalaNode;
	private static Properties properties;
	protected static PiIdBuilder piIdBuilder;
	protected static DhtClientFactory dhtClientFactory;
	protected static ManagementUser provisioningUser;
	protected static ManagementUser misUser;
	protected static ManagementUser opsUser;
	protected static StubMailSender stubMailSender;

	protected static String instanceId;
	private static AvailabilityZone availabilityZone3;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		FileUtils.deleteQuietly(new File("var"));
		FileUtils.forceMkdir(new File(BUCKET_ROOT));
		try {
			classPathXmlApplicationContext = new ClassPathXmlApplicationContext(new String[] { "commonApplicationContext.xml" });

			ResourceHandler resourceHandler = (ResourceHandler) classPathXmlApplicationContext.getBean("resourceHandler");
			resourceHandler.setResourceBase("src/main/www");

			properties = (Properties) classPathXmlApplicationContext.getBean("properties");
			koalaNode = (KoalaNode) classPathXmlApplicationContext.getBean("koalaNode");
			koalaNode.start();

			piIdBuilder = (PiIdBuilder) classPathXmlApplicationContext.getBean("piIdBuilder");
			dhtClientFactory = (DhtClientFactory) classPathXmlApplicationContext.getBean("dhtClientFactory");
			stubMailSender = classPathXmlApplicationContext.getBean(StubMailSender.class);
			seedInstanceTypes();
			seedRegions();
			seedAvailabilityZones();
			seedUsers();
			seedPiCertificate();
			seedManagementUsers();
			seedHealthApplication();
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	private static void seedInstanceTypes() {
		InstanceTypeConfiguration smallConf = new InstanceTypeConfiguration();
		smallConf.setDiskSizeInGB(10);
		smallConf.setMemorySizeInMB(1024);
		smallConf.setNumCores(1);
		smallConf.setInstanceType("small");

		InstanceTypes instanceTypes = new InstanceTypes();
		instanceTypes.addInstanceType(smallConf);

		BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
		writer.put(piIdBuilder.getPId(InstanceTypes.URL_STRING), instanceTypes);
	}

	private static void seedRegions() {
		Region region1 = new Region();
		region1.setRegionCode(12);
		region1.setRegionEndpoint("http://endpoint");
		region1.setRegionName("ragin' region");

		Region region2 = new Region();
		region2.setRegionCode(23);
		region2.setRegionEndpoint("http://endpoint");
		region2.setRegionName("legion region");

		Region region3 = new Region();
		region3.setRegionCode(99);
		region3.setRegionEndpoint("http://endpoint");
		region3.setRegionName(REGION);

		Regions regions = new Regions();
		regions.addRegion(region1);
		regions.addRegion(region2);
		regions.addRegion(region3);

		BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
		writer.put(piIdBuilder.getRegionsId(), regions);
	}

	private static void seedAvailabilityZones() {
		AvailabilityZone availabilityZone1 = new AvailabilityZone();
		availabilityZone1.setRegionCode(12);
		availabilityZone1.setAvailabilityZoneCodeWithinRegion(23);
		availabilityZone1.setAvailabilityZoneName("lone zone");
		availabilityZone1.setStatus("ok");

		AvailabilityZone availabilityZone2 = new AvailabilityZone();
		availabilityZone2.setRegionCode(23);
		availabilityZone2.setAvailabilityZoneCodeWithinRegion(55);
		availabilityZone2.setAvailabilityZoneName("clone zone");
		availabilityZone2.setStatus("ko");

		availabilityZone3 = new AvailabilityZone();
		availabilityZone3.setRegionCode(99);
		availabilityZone3.setAvailabilityZoneCodeWithinRegion(99);
		availabilityZone3.setAvailabilityZoneName(AVAILABILITY_ZONE);
		availabilityZone3.setStatus("ho ho ho");

		AvailabilityZones availabilityZones = new AvailabilityZones();
		availabilityZones.addAvailabilityZone(availabilityZone1);
		availabilityZones.addAvailabilityZone(availabilityZone2);
		availabilityZones.addAvailabilityZone(availabilityZone3);

		BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
		writer.put(piIdBuilder.getAvailabilityZonesId(), availabilityZones);
	}

	private static void seedManagementUsers() {
		ManagementUsers users = new ManagementUsers();
		misUser = new ManagementUser();
		misUser.setUsername(MIS_USER_USERNAME);
		// password is "password"
		// use `echo -n password | md5sum` to generate
		misUser.setPassword(md5Hash(PASSWORD));
		misUser.getRoles().add(ManagementRoles.ROLE_MIS);
		users.getUserMap().put(misUser.getUsername(), misUser);

		opsUser = new ManagementUser();
		opsUser.setUsername(OPS_USER_USERNAME);
		opsUser.setPassword(md5Hash(PASSWORD));
		opsUser.getRoles().add(ManagementRoles.ROLE_OPS);
		users.getUserMap().put(opsUser.getUsername(), opsUser);

		provisioningUser = new ManagementUser();
		provisioningUser.setUsername(PROVISIONING_USER_USERNAME);
		provisioningUser.setPassword(md5Hash(PASSWORD));
		provisioningUser.getRoles().add(ManagementRoles.ROLE_PROVISIONING);
		users.getUserMap().put(provisioningUser.getUsername(), provisioningUser);

		BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
		writer.put(piIdBuilder.getPId(users), users);
	}

	private static void seedUsers() {
		User[] users = new User[] { buildUser(USERNAME), buildUser("Dr Julius Hibbert"), buildUser("Master Bartholemew Simpson"), buildUser("Apu Nahasapeemapetilon") };

		List<Image> images = new ArrayList<Image>();
		Image kernel = buildKernelImage();
		kernel.setImageId(KERNEL_ID);
		Image ramdisk = buildRamdiskImage();
		ramdisk.setImageId("rmi-1234");
		images.add(buildWindowsImage());
		images.add(buildWindowsImage());
		images.add(buildWindowsImage());
		images.add(buildWindowsImage());
		images.add(kernel);
		images.add(buildKernelImage());
		images.add(ramdisk);
		images.add(buildRamdiskImage());
		images.add(buildLinuxImage(ramdisk, kernel));
		images.add(buildLinuxImage(ramdisk, kernel));
		images.add(buildLinuxImage(ramdisk, kernel));
		images.add(buildLinuxImage(ramdisk, kernel));
		images.add(buildLinuxImage(ramdisk, kernel));
		images.add(buildLinuxImage(ramdisk, kernel));
		images.add(buildLinuxImage(ramdisk, kernel));
		images.add(buildLinuxImage(ramdisk, kernel));

		instanceId = piIdBuilder.generateBase62Ec2Id("i", availabilityZone3.getGlobalAvailabilityZoneCode());

		PId id = piIdBuilder.getPId(SecurityGroup.getUrl(users[0].getUsername(), SECURITY_GROUP_NAME)).forLocalRegion();
		BlockingDhtWriter writer0 = dhtClientFactory.createBlockingWriter();
		SecurityGroup sg = new SecurityGroup(users[0].getUsername(), SECURITY_GROUP_NAME);
		writer0.put(id, sg);

		users[0].addInstance(instanceId);
		users[0].getSecurityGroupIds().add(SECURITY_GROUP_NAME);
		users[0].getSecurityGroupIds().add("sec-group2");
		users[0].getSecurityGroupIds().add("sec-group3");
		users[0].getSecurityGroupIds().add("sec-group4");
		users[0].getVolumeIds().add("vol-2");
		users[0].getVolumeIds().add("vol-3");
		users[0].getVolumeIds().add("vol-5");
		users[0].getVolumeIds().add("vol-1");
		users[0].getVolumeIds().add("vol-4");

		ImageIndex imageIndex = new ImageIndex();

		for (Image image : images) {
			setImageOwnership(image, users[0]);
			BlockingDhtWriter writer1 = dhtClientFactory.createBlockingWriter();
			writer1.put(piIdBuilder.getPId(image), image);
			imageIndex.getImages().add(image.getImageId());
		}

		BlockingDhtWriter imageIndexWriter = dhtClientFactory.createBlockingWriter();
		imageIndexWriter.put(piIdBuilder.getPId(imageIndex).forLocalRegion(), imageIndex);

		for (User user : users) {
			BlockingDhtWriter writer1 = dhtClientFactory.createBlockingWriter();
			writer1.put(piIdBuilder.getPId(user), user);
			BlockingDhtWriter writer2 = dhtClientFactory.createBlockingWriter();
			UserAccessKey key = new UserAccessKey(user.getUsername(), user.getApiAccessKey());
			writer2.put(piIdBuilder.getPId(key), key);
		}

		BlockingDhtWriter writer1 = dhtClientFactory.createBlockingWriter();
		Instance instance = new Instance();
		instance.setInstanceId(instanceId);
		PId pIdForEc2AvailabilityZone = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
		writer1.put(pIdForEc2AvailabilityZone, instance);
	}

	private static void seedHealthApplication() throws JMException {
		PiSeeder piSeeder = (PiSeeder) classPathXmlApplicationContext.getBean("piSeeder");
		piSeeder.configureNumberOfSuperNodes(ReportingApplication.APPLICATION_NAME, 1, 1);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// delete the vare directory
		FileUtils.deleteQuietly(new File("var"));

		// get the node id
		File nodeIdFile = new File(properties.getProperty("nodeIdFile"));
		String nodeId = FileUtils.readFileToString(nodeIdFile);

		// delete the DHT
		File file = new File(String.format("storage%s", nodeId));
		FileUtils.deleteQuietly(file);

		// delete the node id file
		FileUtils.deleteQuietly(nodeIdFile);

		koalaNode.stop();
		classPathXmlApplicationContext.destroy();
		FileUtils.deleteDirectory(new File(BUCKET_ROOT));
	}

	@Before
	public void doBefore() {
		commandExecutor = new CommandExecutor(new ScheduledThreadPoolExecutor(20));
		System.setProperty("javax.net.ssl.trustStore", "src/test/resources/ssl_keystore");
		System.setProperty("javax.net.ssl.trustStorePassword", "badgermonkeyfish");
		opsUserHttpClient = logInClient(OPS_USER_USERNAME, PASSWORD);
		misUserHttpClient = logInClient(MIS_USER_USERNAME, PASSWORD);
		provisioningUserHttpClient = logInClient(PROVISIONING_USER_USERNAME, PASSWORD);
		anonymousHttpClient = new HttpClient();
	}

	private HttpClient logInClient(String username, String password) {
		HttpClient cli = new HttpClient();
		cli.getParams().setAuthenticationPreemptive(true);
		Credentials credentials = new UsernamePasswordCredentials(username, password);
		cli.getState().setCredentials(AuthScope.ANY, credentials);
		return cli;
	}

	private static Image buildWindowsImage() {
		Image img = buildImage();
		img.setPlatform(ImagePlatform.windows);
		return img;
	}

	private static Image buildKernelImage() {
		Image img = buildImage();
		img.setPlatform(ImagePlatform.linux);
		img.setMachineType(MachineType.KERNEL);
		return img;
	}

	private static Image buildRamdiskImage() {
		Image img = buildImage();
		img.setPlatform(ImagePlatform.linux);
		img.setMachineType(MachineType.RAMDISK);
		return img;
	}

	private static Image buildLinuxImage(Image ramdisk, Image kernel) {
		Image img = buildImage();
		img.setPlatform(ImagePlatform.linux);
		img.setKernelId(kernel.getImageId());
		img.setRamdiskId(ramdisk.getImageId());
		return img;
	}

	private static Image buildImage() {
		Image image = new Image();
		image.setImageId(getRandomString("img-"));
		if (Math.random() > 0.5) {
			image.setArchitecture("i386");
		} else {
			image.setArchitecture("x86_64");
		}
		image.setMachineType(MachineType.MACHINE);
		image.setManifestLocation(getRandomString("/") + "/manifest.xml");
		image.setPublic(Math.random() > 0.5);
		if (Math.random() < 0.3) {
			image.setState(ImageState.PENDING);
		} else {
			if (Math.random() > 0.5) {
				image.setState(ImageState.AVAILABLE);
			} else {
				image.setState(ImageState.FAILED);
			}
		}

		return image;
	}

	private static void setImageOwnership(Image image, User user) {
		image.setOwnerId(user.getUsername());
		user.getImageIds().add(image.getImageId());
	}

	private static User buildUser(String name) {
		User user = new User();
		user.setRealName(name);
		user.setUsername(name.replace(" ", "").toLowerCase());
		user.setApiAccessKey(getRandomString("access"));
		user.setApiSecretKey(getRandomString("secret"));
		user.setEmailAddress(name.replace(" ", ".").toLowerCase() + "@somewhere.com");
		user.setEnabled(Math.random() > 0.5);
		user.setExternalRefId("external" + user.getUsername());
		byte[] cert = new byte[256];
		for (int i = 0; i < 256; i++) {
			cert[i] = (byte) (Math.random() * 8);
		}
		user.setCertificate(cert);
		return user;
	}

	private static String getRandomString(String prefix) {
		return prefix + (int) (Math.random() * 10000000);
	}

	protected void runCommand(String command) throws Exception {
		String[] commands = new String[] { "/bin/bash", "-c", command };
		commandExecutor.executeScript(commands, Runtime.getRuntime());
	}

	protected String getXmlFromLines(List<String> lines) {
		StringBuffer buffer = new StringBuffer();
		for (String line : lines) {
			if (line.trim().startsWith("<"))
				buffer.append(line.trim());
		}
		return buffer.toString();
	}

	protected void assertResponse(List<String> lines, String target) {
		for (String line : lines)
			if (line.contains(target))
				return;
		fail();
	}

	protected String findLine(List<String> lines, String target) {
		for (String line : lines)
			if (line.contains(target))
				return line;
		return null;
	}

	protected void assertContains(String expected, String actual) {
		if (!actual.contains(expected)) {
			throw new AssertionFailedError(String.format("Did not find expected string in actual result\nExpected:\n %s\n\nActual:\n%s", expected, actual));
		}
	}

	protected void assertMatches(String expectedRegex, String actual) {
		if (!actual.matches(expectedRegex)) {
			throw new AssertionFailedError(String.format("Actual did not match regex\nRegex:\n %s\n\nActual:\n%s", expectedRegex, actual));
		}
	}

	private static void seedPiCertificate() throws Exception {
		UserServiceHelper userServiceHelper = (UserServiceHelper) classPathXmlApplicationContext.getBean("userServiceHelper");
		PiCertificate piCertificate = userServiceHelper.createPiCertificate();
		byte[] aPrivateKey = new byte[] { 48, -126, 4, -67, 2, 1, 0, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 4, -126, 4, -89, 48, -126, 4, -93, 2, 1, 0, 2, -126, 1, 1, 0, -105, -14,
				-17, -66, -77, 29, 123, 61, 120, -86, -53, -107, -128, -8, -39, 91, -122, -83, -59, -70, 56, 124, 60, 82, -115, -36, -63, -90, -54, 98, -93, -125, -97, -18, 2, -31, -34, -66, -9, 98,
				31, 119, 112, 46, -126, 80, -69, -44, 83, 1, -33, -126, 63, -80, 99, -52, 97, -90, 39, 36, 84, -86, -112, -23, 3, 32, -68, -68, -44, 45, -58, 10, 35, 10, -4, 96, -91, 32, -15, 64,
				-11, 20, 115, -72, 83, -50, -28, -8, 36, -71, -1, -7, 92, -116, 45, -45, -96, -45, -17, 49, 35, 94, 127, -7, 62, 10, 58, -39, 51, -87, 12, -23, -101, 118, -47, -122, 78, -72, 2, -15,
				94, 76, 115, -99, -8, -8, 115, -107, -67, 55, -126, 108, 22, 10, 20, 86, 6, 92, -39, -36, 95, -91, -2, -56, -68, 35, 115, 9, -91, -109, -58, -80, -9, -93, -123, -128, -19, 53, -125,
				85, -80, -8, 46, -122, -53, -78, 86, 103, 95, -33, 84, -34, -83, -60, 81, -84, -51, -71, 113, 48, -73, -66, 40, 70, 27, -121, -91, 122, 45, 9, 2, 89, -89, -116, 53, -86, -65, 83, -71,
				70, 53, 47, -1, 47, -91, -117, -97, -60, 115, 23, -122, -44, 62, -44, -28, 89, -127, -87, 5, 74, -51, -8, -15, -79, 46, 51, -108, -26, -76, -107, -121, 9, -6, -32, -69, 69, 29, -76,
				80, -13, -17, 112, 79, 78, -34, 106, -32, -109, -111, -62, -63, 118, -1, -12, 95, -77, 2, 3, 1, 0, 1, 2, -126, 1, 0, 115, -19, -84, -114, 113, 55, -81, -4, 33, 97, 37, -68, 37, -49,
				54, 126, 71, 13, -77, -118, -75, 103, -53, -38, 44, 83, -34, 7, 115, -25, -73, -37, 71, -100, -98, -28, 87, 62, -103, -99, 106, 102, -124, -41, 103, 35, 83, 122, -43, -19, -38, -22,
				19, -49, 111, 4, -45, 7, -94, 91, 108, -95, 73, -72, 13, 99, -33, -69, -83, -94, 82, -77, 15, 51, 101, -124, 18, -40, 68, 88, -117, -29, -109, -70, 113, 110, -85, 112, -53, 12, -127,
				-56, 109, -100, -95, -90, 17, -47, 64, 111, -36, 13, 80, 84, 7, -54, 100, 69, -84, -51, 112, -123, -106, -48, 27, 97, 126, 19, -108, 99, 52, -49, -35, 114, 12, 46, -6, 119, 121, 60,
				92, -57, 125, 87, 32, -56, 90, 78, 111, -94, 24, 122, 52, 68, -55, -42, 112, 33, -59, -48, -63, 123, -16, -58, -40, -95, 97, 35, -10, 125, -113, 91, -124, 36, -114, -104, 54, 6, -110,
				95, -95, -22, -73, -74, -61, 90, 37, 76, 58, 54, -87, 47, 30, 40, 8, -33, 22, -62, 77, -119, -76, -61, 20, -64, -83, -13, 50, -31, -87, 99, 13, 117, -109, 5, 80, 106, 39, 23, 58, 102,
				4, -68, 14, -30, 92, 0, -49, 94, 69, 36, 34, 112, -15, 22, 122, -70, -7, -104, 78, -59, -119, 127, -88, -37, -108, 117, 117, 76, -110, -48, 57, 34, 60, -103, -99, 46, -40, -7, 82,
				-63, -65, -51, 73, 45, -25, -31, 2, -127, -127, 0, -44, -89, 57, -64, 14, -118, -106, -59, 16, 25, -30, 105, -118, -26, -117, 98, 88, 49, 51, 88, 0, 37, -97, 99, -8, -124, 5, 6, -124,
				-105, -103, -125, 72, 99, -118, -42, 16, 100, 9, -38, 61, 84, -120, 38, 111, 125, -81, -107, -35, 118, 68, -75, -103, 71, 34, -23, 58, 81, 11, 76, 118, -74, 59, -22, -93, -25, 68,
				-58, -55, 10, -29, 115, -117, 68, 68, 87, -67, 88, 84, 82, 53, 37, 118, -27, 123, 88, 23, 48, 75, -62, -116, -21, 32, 48, -35, -121, 108, -14, 71, 69, 52, 3, -32, -34, 67, 53, -97,
				-36, -19, -9, -105, 116, -62, 19, -122, -127, -15, 13, -17, -76, -12, 66, 73, -1, 117, -76, 37, -29, 2, -127, -127, 0, -74, -20, 3, 10, -55, -87, 110, -101, -53, -110, -83, -72, 36,
				-58, -125, 45, -11, 87, 8, -67, -79, -113, -50, 25, 45, 63, -46, 90, -70, 88, 17, -46, 44, 124, -19, 54, 50, -48, -47, 65, 74, -74, 60, 67, -34, -80, -87, 41, 32, 78, 60, 56, 86,
				-112, -107, 53, -18, -70, -84, 2, 88, 91, -103, -83, 53, 71, 30, -47, 11, 125, -23, 110, -2, -40, -78, 93, -117, 91, -33, 87, -63, 21, -39, -127, -4, -4, -89, -12, 45, 63, 74, -18,
				-67, -68, 25, -63, -54, -117, -99, -20, -66, 11, 90, 81, -121, -4, 77, 72, -22, 86, 58, -110, 8, -73, -117, -47, 114, -3, 113, -102, 121, -44, -60, -98, 86, -74, -121, -15, 2, -127,
				-128, 51, -12, -105, 123, -127, 18, 3, 60, 42, 110, -24, -114, 120, -51, 83, 8, -72, 27, 109, 59, -10, -19, 58, 64, 38, -101, -70, -50, -104, -34, -95, 55, 30, 28, -109, -13, 49, 22,
				0, 2, 62, 49, -59, 1, -1, 3, 106, 62, -25, 88, -39, -8, -76, 118, 88, -27, 58, -58, 74, 72, 104, 72, -91, -30, -14, 32, -77, 1, 14, 101, -122, -92, -40, 69, -39, -100, -58, 58, 42,
				127, -37, 84, 71, -12, 81, 106, 120, 95, -24, 98, -92, 35, 94, 62, 18, 33, -32, 80, 97, 113, 91, 0, 7, -108, -58, 62, -9, -53, -10, -88, 35, 108, -9, 109, -27, -45, 33, -98, 18, 14,
				-40, 14, -54, 29, -116, 24, 115, -103, 2, -127, -128, 110, -48, -26, 58, -25, -42, -52, 90, 119, -26, -95, 117, 120, 90, 6, -24, -107, -60, 39, 88, 124, 52, -119, -128, 57, 40, 123,
				-16, 89, 9, -73, -86, 35, 39, 127, -79, -96, -15, 94, -125, -10, -106, 22, 70, 107, -89, -116, -93, -116, -99, -72, -33, -52, -103, -124, -69, -118, -89, -18, 66, -15, 114, 116, -44,
				56, -3, -96, 14, -74, -82, -115, -9, -97, 78, 122, 40, 47, -97, -11, -37, 60, -17, 86, -72, -24, 33, -52, 66, 34, 19, 64, -5, 7, 88, -24, 37, -67, -27, -3, 67, -118, 18, 104, -94, 18,
				6, -24, 111, 47, 0, 20, 53, -102, 48, 79, -11, 16, 123, -72, 18, 4, -110, -64, -106, -56, 35, -122, -111, 2, -127, -127, 0, -66, -76, 120, -78, 58, 105, -23, -22, -113, -58, 12, 67,
				114, 13, -25, 8, 109, 46, 98, -19, 54, -53, 88, 71, -51, -22, 47, -53, -43, 24, -49, -17, -95, 98, 45, -60, -82, 86, 70, -95, -80, -57, 15, -100, -52, 99, -85, 121, -79, 68, -86, -52,
				-40, -49, -51, 4, -48, -73, 68, -117, -33, 122, 127, -56, -116, -64, 110, 93, 25, 84, 99, 32, 69, -120, 104, 123, -86, -42, 37, -71, 48, -7, -66, 122, -41, -111, -16, -101, -95, -86,
				51, -12, 46, 23, 10, 127, 111, -20, -31, -128, 35, 42, -117, -11, -84, 37, -18, 101, 96, -116, -19, -96, 12, 68, -30, 69, 22, -15, -36, 26, -32, -103, 26, 34, -25, 100, 105, -2 };
		piCertificate.setPrivateKey(aPrivateKey);
		dhtClientFactory.createBlockingWriter().writeIfAbsent(piIdBuilder.getPId(piCertificate), piCertificate);
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
