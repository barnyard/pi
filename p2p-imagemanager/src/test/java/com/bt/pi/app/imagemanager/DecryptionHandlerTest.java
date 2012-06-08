package com.bt.pi.app.imagemanager;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.imagemanager.crypto.ImageCryptographer;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class DecryptionHandlerTest {
    @InjectMocks
    private DecryptionHandler decryptionHandler = new DecryptionHandler();
    @Mock
    private Image image;
    private String imagesPath = System.getProperty("java.io.tmpdir") + "/unittesting/images";
    @Mock
    private ImageCryptographer imageCryptographer;
    private String imageId = "img-123";
    @Mock
    private ImageManagerApplication imageManager;
    private String nodeId = "123456";
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    private String reservationUrl;
    private ThreadPoolTaskExecutor taskExecutor;
    private String userName = "fred";
    private String bucketName = "bucketName";
    private String manifestName = "manifest.xml";
    private String manifestFileLocation = bucketName + "/" + manifestName;
    @Mock
    private DhtCache dhtCache;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private PId userPastryId;
    @Mock
    private User user;
    @Mock
    private PId imagePastryId;
    @Mock
    private ImageHelper imageHelper;
    @Mock
    private PId decryptImageQueueId;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws IOException {
        FileUtils.deleteQuietly(new File(this.imagesPath));
        FileUtils.forceMkdir(new File(this.imagesPath));

        decryptionHandler.setImagesPath(imagesPath);

        when(piIdBuilder.getPId(User.getUrl(userName))).thenReturn(userPastryId);
        when(piIdBuilder.getPiQueuePId(PiQueue.DECRYPT_IMAGE)).thenReturn(decryptImageQueueId);
        when(decryptImageQueueId.forLocalScope(PiQueue.DECRYPT_IMAGE.getNodeScope())).thenReturn(decryptImageQueueId);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[1];
                continuation.handleResult(user);
                return null;
            }
        }).when(dhtCache).getReadThrough(eq(userPastryId), isA(PiContinuation.class));

        when(image.getImageId()).thenReturn(this.imageId);
        when(image.getUrl()).thenReturn(this.reservationUrl);
        when(image.getOwnerId()).thenReturn(this.userName);
        when(image.getManifestLocation()).thenReturn(this.manifestFileLocation);

        when(this.imageManager.getNodeIdFull()).thenReturn(nodeId);

        this.taskExecutor = new ThreadPoolTaskExecutor();
        this.taskExecutor.initialize();
        this.decryptionHandler.setThreadPoolTaskExecutor(this.taskExecutor);
        when(piIdBuilder.getPId(Image.getUrl(imageId))).thenReturn(imagePastryId);
    }

    @After
    public void after() {
        FileUtils.deleteQuietly(new File(this.imagesPath));
    }

    @Test
    public void decryptShouldMarkTaskProcessingQueueWithNodeId() throws Exception {
        // setup
        FileUtils.touch(new File(this.imagesPath + "/" + this.imageId));

        // act
        decryptionHandler.decrypt(image, imageManager);
        Thread.sleep(100);

        // assert
        verify(this.taskProcessingQueueHelper).setNodeIdOnUrl(decryptImageQueueId, reservationUrl, nodeId);
    }

    @Test
    public void decryptShouldCallDecryptor() throws Exception {
        File tmpfile = File.createTempFile("unittesting", null);
        when(this.imageCryptographer.decrypt(bucketName, manifestName, this.user)).thenReturn(tmpfile);

        // act
        decryptionHandler.decrypt(image, imageManager);
        Thread.sleep(100);

        // assert
        assertTrue(new File(String.format("%s/%s", this.imagesPath, this.imageId)).exists());
    }

    @Test
    public void decryptShouldMarkImageAsAvailable() throws Exception {
        File tmpfile = File.createTempFile("unittesting", null);
        when(this.imageCryptographer.decrypt(bucketName, manifestName, this.user)).thenReturn(tmpfile);

        // act
        decryptionHandler.decrypt(image, imageManager);
        Thread.sleep(100);

        // assert
        verify(imageHelper).updateImageState(eq(imageId), eq(ImageState.AVAILABLE));
    }

    @Test
    public void decryptShouldNotCallDecryptorIfImageAlreadyInImagesDir() throws Exception {
        // setup
        FileUtils.touch(new File(this.imagesPath + "/" + this.imageId));

        // act
        decryptionHandler.decrypt(image, imageManager);
        Thread.sleep(100);

        // assert
        verify(this.imageCryptographer, never()).decrypt(bucketName, manifestName, this.user);
    }

    @Test
    public void decryptShouldRemoveReservationFromTaskProcessingQueue() throws Exception {
        // setup
        FileUtils.touch(new File(this.imagesPath + "/" + this.imageId));

        // act
        decryptionHandler.decrypt(image, imageManager);
        Thread.sleep(100);

        // assert
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(decryptImageQueueId, reservationUrl);
    }
}
