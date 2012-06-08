package com.bt.pi.app.imagemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.pi.app.common.UserServiceHelper;
import com.bt.pi.app.common.entities.BucketMetaData;
import com.bt.pi.app.common.entities.CannedAclPolicy;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.PiCertificate;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.UserAccessKey;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.management.ApplicationSeeder;
import com.bt.pi.app.management.QueueSeeder;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.sss.PisssApplicationManager;
import com.bt.pi.sss.entities.ObjectMetaData;

public class ImageManagerApplicationIntegrationTest {
    private static final String SECRET_KEY = "sIL03bvLbAa__Oa7Oe5Ssuhjalg6m-cd0RDwOg";
    private static final String ACCESS_KEY = "Y7HqdJgHC7aYIbnxPRNU1A";
    private static AbstractApplicationContext context;
    private static PiIdBuilder piIdBuilder;
    private static DhtClientFactory dhtClientFactory;
    private static ImageManagerApplication imageManagerApplication;
    private static Properties properties;
    private static KoalaNode koalaNode;
    private static KoalaIdFactory koalaIdFactory;
    private String imageId;
    private static String imageManagerNodeId;
    private static ImageManagerApplicationWatcherManager imageManagerApplicationWatcherManager;
    private static ApplicationSeeder applicationSeeder;
    private static String bucketName = "mytestbucket";
    private static String userName = "fred";
    private static int imageIdIndex = 0;
    private static TaskProcessingQueueHelper taskProcessingQueueHelper;
    private static final int THIRTY_MINUTE = 30 * 60 * 1000;

    @BeforeClass
    public static void beforeClass() throws Exception {
        FileUtils.deleteQuietly(new File("var"));
        FileUtils.forceMkdir(new File("var/images"));
        FileUtils.forceMkdir(new File("var/image_processing"));

        try {
            context = new ClassPathXmlApplicationContext("commonApplicationContext.xml");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        koalaNode = (KoalaNode) context.getBean("koalaNode");
        piIdBuilder = (PiIdBuilder) context.getBean("piIdBuilder");
        dhtClientFactory = (DhtClientFactory) context.getBean("dhtClientFactory");
        imageManagerApplication = (ImageManagerApplication) context.getBean("imageManagerApplication");
        properties = (Properties) context.getBean("properties");
        koalaIdFactory = (KoalaIdFactory) context.getBean("koalaIdFactory");
        applicationSeeder = (ApplicationSeeder) context.getBean("applicationSeeder");
        taskProcessingQueueHelper = (TaskProcessingQueueHelper) context.getBean("taskProcessingQueueHelper");
        imageManagerApplicationWatcherManager = (ImageManagerApplicationWatcherManager) context.getBean("imageManagerApplicationWatcherManager");

        koalaNode.start();

        createPisssApplicationRecord();
        seedPiCertificate();
        createUserInDht();
        seedQueues();
        imageManagerNodeId = imageManagerApplication.getNodeIdFull();
    }

    private static void seedQueues() {
        QueueSeeder queueSeeder = new QueueSeeder();
        queueSeeder.setDhtClientFactory(dhtClientFactory);
        queueSeeder.setPiIdBuilder(piIdBuilder);
        queueSeeder.addQueue(PiQueue.DECRYPT_IMAGE);
    }

    private static void seedPiCertificate() throws Exception {
        UserServiceHelper userServiceHelper = (UserServiceHelper) context.getBean("userServiceHelper");
        PiCertificate piCertificate = userServiceHelper.createPiCertificate();
        byte[] aPrivateKey = new byte[] { 48, -126, 4, -67, 2, 1, 0, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 4, -126, 4, -89, 48, -126, 4, -93, 2, 1, 0, 2, -126, 1, 1, 0, -105, -14, -17, -66, -77, 29, 123, 61, 120, -86, -53, -107,
                -128, -8, -39, 91, -122, -83, -59, -70, 56, 124, 60, 82, -115, -36, -63, -90, -54, 98, -93, -125, -97, -18, 2, -31, -34, -66, -9, 98, 31, 119, 112, 46, -126, 80, -69, -44, 83, 1, -33, -126, 63, -80, 99, -52, 97, -90, 39, 36, 84, -86,
                -112, -23, 3, 32, -68, -68, -44, 45, -58, 10, 35, 10, -4, 96, -91, 32, -15, 64, -11, 20, 115, -72, 83, -50, -28, -8, 36, -71, -1, -7, 92, -116, 45, -45, -96, -45, -17, 49, 35, 94, 127, -7, 62, 10, 58, -39, 51, -87, 12, -23, -101,
                118, -47, -122, 78, -72, 2, -15, 94, 76, 115, -99, -8, -8, 115, -107, -67, 55, -126, 108, 22, 10, 20, 86, 6, 92, -39, -36, 95, -91, -2, -56, -68, 35, 115, 9, -91, -109, -58, -80, -9, -93, -123, -128, -19, 53, -125, 85, -80, -8, 46,
                -122, -53, -78, 86, 103, 95, -33, 84, -34, -83, -60, 81, -84, -51, -71, 113, 48, -73, -66, 40, 70, 27, -121, -91, 122, 45, 9, 2, 89, -89, -116, 53, -86, -65, 83, -71, 70, 53, 47, -1, 47, -91, -117, -97, -60, 115, 23, -122, -44, 62,
                -44, -28, 89, -127, -87, 5, 74, -51, -8, -15, -79, 46, 51, -108, -26, -76, -107, -121, 9, -6, -32, -69, 69, 29, -76, 80, -13, -17, 112, 79, 78, -34, 106, -32, -109, -111, -62, -63, 118, -1, -12, 95, -77, 2, 3, 1, 0, 1, 2, -126, 1, 0,
                115, -19, -84, -114, 113, 55, -81, -4, 33, 97, 37, -68, 37, -49, 54, 126, 71, 13, -77, -118, -75, 103, -53, -38, 44, 83, -34, 7, 115, -25, -73, -37, 71, -100, -98, -28, 87, 62, -103, -99, 106, 102, -124, -41, 103, 35, 83, 122, -43,
                -19, -38, -22, 19, -49, 111, 4, -45, 7, -94, 91, 108, -95, 73, -72, 13, 99, -33, -69, -83, -94, 82, -77, 15, 51, 101, -124, 18, -40, 68, 88, -117, -29, -109, -70, 113, 110, -85, 112, -53, 12, -127, -56, 109, -100, -95, -90, 17, -47,
                64, 111, -36, 13, 80, 84, 7, -54, 100, 69, -84, -51, 112, -123, -106, -48, 27, 97, 126, 19, -108, 99, 52, -49, -35, 114, 12, 46, -6, 119, 121, 60, 92, -57, 125, 87, 32, -56, 90, 78, 111, -94, 24, 122, 52, 68, -55, -42, 112, 33, -59,
                -48, -63, 123, -16, -58, -40, -95, 97, 35, -10, 125, -113, 91, -124, 36, -114, -104, 54, 6, -110, 95, -95, -22, -73, -74, -61, 90, 37, 76, 58, 54, -87, 47, 30, 40, 8, -33, 22, -62, 77, -119, -76, -61, 20, -64, -83, -13, 50, -31, -87,
                99, 13, 117, -109, 5, 80, 106, 39, 23, 58, 102, 4, -68, 14, -30, 92, 0, -49, 94, 69, 36, 34, 112, -15, 22, 122, -70, -7, -104, 78, -59, -119, 127, -88, -37, -108, 117, 117, 76, -110, -48, 57, 34, 60, -103, -99, 46, -40, -7, 82, -63,
                -65, -51, 73, 45, -25, -31, 2, -127, -127, 0, -44, -89, 57, -64, 14, -118, -106, -59, 16, 25, -30, 105, -118, -26, -117, 98, 88, 49, 51, 88, 0, 37, -97, 99, -8, -124, 5, 6, -124, -105, -103, -125, 72, 99, -118, -42, 16, 100, 9, -38,
                61, 84, -120, 38, 111, 125, -81, -107, -35, 118, 68, -75, -103, 71, 34, -23, 58, 81, 11, 76, 118, -74, 59, -22, -93, -25, 68, -58, -55, 10, -29, 115, -117, 68, 68, 87, -67, 88, 84, 82, 53, 37, 118, -27, 123, 88, 23, 48, 75, -62,
                -116, -21, 32, 48, -35, -121, 108, -14, 71, 69, 52, 3, -32, -34, 67, 53, -97, -36, -19, -9, -105, 116, -62, 19, -122, -127, -15, 13, -17, -76, -12, 66, 73, -1, 117, -76, 37, -29, 2, -127, -127, 0, -74, -20, 3, 10, -55, -87, 110,
                -101, -53, -110, -83, -72, 36, -58, -125, 45, -11, 87, 8, -67, -79, -113, -50, 25, 45, 63, -46, 90, -70, 88, 17, -46, 44, 124, -19, 54, 50, -48, -47, 65, 74, -74, 60, 67, -34, -80, -87, 41, 32, 78, 60, 56, 86, -112, -107, 53, -18,
                -70, -84, 2, 88, 91, -103, -83, 53, 71, 30, -47, 11, 125, -23, 110, -2, -40, -78, 93, -117, 91, -33, 87, -63, 21, -39, -127, -4, -4, -89, -12, 45, 63, 74, -18, -67, -68, 25, -63, -54, -117, -99, -20, -66, 11, 90, 81, -121, -4, 77,
                72, -22, 86, 58, -110, 8, -73, -117, -47, 114, -3, 113, -102, 121, -44, -60, -98, 86, -74, -121, -15, 2, -127, -128, 51, -12, -105, 123, -127, 18, 3, 60, 42, 110, -24, -114, 120, -51, 83, 8, -72, 27, 109, 59, -10, -19, 58, 64, 38,
                -101, -70, -50, -104, -34, -95, 55, 30, 28, -109, -13, 49, 22, 0, 2, 62, 49, -59, 1, -1, 3, 106, 62, -25, 88, -39, -8, -76, 118, 88, -27, 58, -58, 74, 72, 104, 72, -91, -30, -14, 32, -77, 1, 14, 101, -122, -92, -40, 69, -39, -100,
                -58, 58, 42, 127, -37, 84, 71, -12, 81, 106, 120, 95, -24, 98, -92, 35, 94, 62, 18, 33, -32, 80, 97, 113, 91, 0, 7, -108, -58, 62, -9, -53, -10, -88, 35, 108, -9, 109, -27, -45, 33, -98, 18, 14, -40, 14, -54, 29, -116, 24, 115, -103,
                2, -127, -128, 110, -48, -26, 58, -25, -42, -52, 90, 119, -26, -95, 117, 120, 90, 6, -24, -107, -60, 39, 88, 124, 52, -119, -128, 57, 40, 123, -16, 89, 9, -73, -86, 35, 39, 127, -79, -96, -15, 94, -125, -10, -106, 22, 70, 107, -89,
                -116, -93, -116, -99, -72, -33, -52, -103, -124, -69, -118, -89, -18, 66, -15, 114, 116, -44, 56, -3, -96, 14, -74, -82, -115, -9, -97, 78, 122, 40, 47, -97, -11, -37, 60, -17, 86, -72, -24, 33, -52, 66, 34, 19, 64, -5, 7, 88, -24,
                37, -67, -27, -3, 67, -118, 18, 104, -94, 18, 6, -24, 111, 47, 0, 20, 53, -102, 48, 79, -11, 16, 123, -72, 18, 4, -110, -64, -106, -56, 35, -122, -111, 2, -127, -127, 0, -66, -76, 120, -78, 58, 105, -23, -22, -113, -58, 12, 67, 114,
                13, -25, 8, 109, 46, 98, -19, 54, -53, 88, 71, -51, -22, 47, -53, -43, 24, -49, -17, -95, 98, 45, -60, -82, 86, 70, -95, -80, -57, 15, -100, -52, 99, -85, 121, -79, 68, -86, -52, -40, -49, -51, 4, -48, -73, 68, -117, -33, 122, 127,
                -56, -116, -64, 110, 93, 25, 84, 99, 32, 69, -120, 104, 123, -86, -42, 37, -71, 48, -7, -66, 122, -41, -111, -16, -101, -95, -86, 51, -12, 46, 23, 10, 127, 111, -20, -31, -128, 35, 42, -117, -11, -84, 37, -18, 101, 96, -116, -19,
                -96, 12, 68, -30, 69, 22, -15, -36, 26, -32, -103, 26, 34, -25, 100, 105, -2 };
        piCertificate.setPrivateKey(aPrivateKey);
        dhtClientFactory.createBlockingWriter().writeIfAbsent(piIdBuilder.getPId(piCertificate), piCertificate);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        FileUtils.deleteQuietly(new File("var"));
        File nodeIdFile = new File(properties.getProperty("nodeIdFile"));
        String nodeId = FileUtils.readFileToString(nodeIdFile);

        File file = new File(String.format("storage%s", nodeId));
        FileUtils.deleteQuietly(file);
        FileUtils.deleteQuietly(nodeIdFile);

        koalaNode.stop();
        context.destroy();
    }

    private static void createPisssApplicationRecord() {
        applicationSeeder.createRegionScopedApplicationRecord(PisssApplicationManager.APPLICATION_NAME, koalaNode.getKoalaIdFactory().getRegion(), Arrays.asList(new String[] { "127.0.0.1" }));
    }

    @Before
    public void before() {
        imageIdIndex++;
        imageId = String.format("img-%06d", imageIdIndex);
    }

    @After
    public void after() {
        String path = String.format("var/images/%s", imageId);
        boolean deleted = FileUtils.deleteQuietly(new File(path));
        System.err.println(String.format("file %s %s deleted", path, deleted ? "" : "NOT"));

        imageManagerApplicationWatcherManager.setInitialQueueWatcherIntervalMillis(THIRTY_MINUTE);
        imageManagerApplicationWatcherManager.setRepeatingQueueWatcherIntervalMillis(THIRTY_MINUTE);
        imageManagerApplicationWatcherManager.setStaleQueueItemMillis(THIRTY_MINUTE);
        imageManagerApplicationWatcherManager.createTaskProcessingQueueWatcher(imageManagerNodeId);
    }

    @Test
    public void testDecryptImageShouldSetImageStateIfImageAlreadyExists() throws Exception {
        // setup
        FileUtils.touch(new File(String.format("var/images/%s", imageId)));
        Image image = storeImageRecord(imageId);

        // act
        // anycast message to image manager
        PubSubMessageContext pubSubMessageContext = imageManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.DECRYPT_IMAGE.getUrl()).forLocalScope(PiTopics.DECRYPT_IMAGE.getNodeScope()), null);
        pubSubMessageContext.sendAnycast(EntityMethod.CREATE, image);

        // assert
        waitForImageState(ImageState.AVAILABLE);
    }

    private void waitForImageState(ImageState expectedState) throws InterruptedException {
        long interval = 1000;
        long tries = 10;
        for (int i = 0; i < tries; i++) {
            Image image = (Image) dhtClientFactory.createBlockingReader().get(piIdBuilder.getPId(Image.getUrl(imageId)));
            if (image.getState().equals(expectedState))
                return;
            Thread.sleep(interval);
        }
        fail(String.format("image state not %s after %d milliseconds", expectedState, interval * tries));
    }

    @Test
    public void testDecryptImageShouldDecryptImage() throws Exception {
        // setup
        Image image = storeImageRecord(imageId);
        copyUploadedImageToBucket();
        createBucketMetaDataInDht();

        // act
        // anycast message to image manager
        PubSubMessageContext pubSubMessageContext = imageManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.DECRYPT_IMAGE.getUrl()).forLocalScope(PiTopics.DECRYPT_IMAGE.getNodeScope()), null);
        pubSubMessageContext.sendAnycast(EntityMethod.CREATE, image);

        // assert
        File actualResultFile = waitForResultFile(String.format("var/images/%s", imageId));
        long check1 = getFileCheckSum(new File("src/integration/resources/test-file"));
        long check2 = getFileCheckSum(actualResultFile);

        assertEquals(check1, check2);
        Thread.sleep(1000);
        assertImageState(ImageState.AVAILABLE);
    }

    @Test
    public void shouldDecryptImageByProcessingQueueItem() throws Exception {
        // setup
        Image image = storeImageRecord(imageId);
        PId decryptImageQueueId = piIdBuilder.getPiQueuePId(PiQueue.DECRYPT_IMAGE).forLocalScope(PiQueue.DECRYPT_IMAGE.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(decryptImageQueueId, image.getUrl());
        wakeUpDecyptQueue();

        // assert
        File actualResultFile = waitForResultFile(String.format("var/images/%s", imageId));
        long check1 = getFileCheckSum(new File("src/integration/resources/test-file"));
        long check2 = getFileCheckSum(actualResultFile);

        assertEquals(check1, check2);
        Thread.sleep(1000);
        assertImageState(ImageState.AVAILABLE);
    }

    @Test
    public void shouldSetImageStatusToFailedWhenQueueRetriesAreExhuasted() throws Exception {
        // setup
        Image image = storeImageRecord(imageId);
        PId decryptImageQueueId = piIdBuilder.getPiQueuePId(PiQueue.DECRYPT_IMAGE).forLocalScope(PiQueue.DECRYPT_IMAGE.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(decryptImageQueueId, image.getUrl(), 0);
        wakeUpDecyptQueue();

        // assert
        waitForImageState(ImageState.FAILED);
    }

    private void wakeUpDecyptQueue() throws Exception {
        Thread.sleep(500);
        imageManagerApplicationWatcherManager.setInitialQueueWatcherIntervalMillis(0);
        imageManagerApplicationWatcherManager.setStaleQueueItemMillis(1);
        imageManagerApplicationWatcherManager.createTaskProcessingQueueWatcher(imageManagerNodeId);
    }

    private void assertImageState(ImageState expectedState) {
        Image image = (Image) dhtClientFactory.createBlockingReader().get(piIdBuilder.getPId(Image.getUrl(imageId)));
        assertEquals(expectedState, image.getState());
    }

    private long getFileCheckSum(File file) throws IOException {
        return FileUtils.checksumCRC32(file);
    }

    private static void createUserInDht() throws Exception {
        User user = new User(userName, ACCESS_KEY, SECRET_KEY);
        user.getBucketNames().add(bucketName);
        user.setCertificate(readCertificate());
        PId id1 = piIdBuilder.getPId(user);
        putPiEntityIntoDht(user, id1);

        UserAccessKey userAccessKey = new UserAccessKey(userName, ACCESS_KEY);
        PId id2 = piIdBuilder.getPId(userAccessKey);
        putPiEntityIntoDht(userAccessKey, id2);
    }

    private static byte[] readCertificate() throws Exception {
        InputStream inStream = new FileInputStream("src/integration/resources/euca2-koalascope-17cfb720-cert.pem");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();
        return cert.getEncoded();
    }

    private void createBucketMetaDataInDht() {
        BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ);
        PId id = piIdBuilder.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())));
        putPiEntityIntoDht(bucketMetaData, id);
    }

    private void copyUploadedImageToBucket() throws Exception {
        File targetDir = new File(String.format("var/buckets/%s", bucketName));
        FileUtils.copyDirectory(new File("src/integration/resources/uploadedImage"), targetDir);

        for (String filename : targetDir.list()) {
            String metadataFile = String.format("%s/%s%s", targetDir.getAbsolutePath(), filename, ObjectMetaData.FILE_SUFFIX);
            String data = String.format("{\"contentType\":\"binary/octet-stream\",\"contentDisposition\":null,\"xAmzMetaHeaders\":null,\"eTag\":null,\"name\":\"%s\"}", filename);
            FileUtils.writeStringToFile(new File(metadataFile), data);
        }
    }

    private File waitForResultFile(String filename) throws InterruptedException {
        long interval = 1000;
        long tries = 60;
        for (int i = 0; i < tries; i++) {
            File result = new File(filename);
            if (result.exists())
                return result;
            Thread.sleep(interval);
        }
        fail(String.format("file %s not created after %d milliseconds", filename, interval * tries));
        return null;
    }

    private Image storeImageRecord(String imageId) {
        Image result = new Image();
        result.setImageId(imageId);
        result.setOwnerId(userName);
        result.setManifestLocation(String.format("%s/test-file.manifest.xml", bucketName));
        result.setState(ImageState.PENDING);
        PId id = piIdBuilder.getPId(result);
        putPiEntityIntoDht(result, id);
        return result;
    }

    private static void putPiEntityIntoDht(PiEntity piEntity, PId id) {
        BlockingDhtWriter dhtWriter = dhtClientFactory.createBlockingWriter();
        dhtWriter.put(id, piEntity);
    }
}
