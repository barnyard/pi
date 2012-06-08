package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import com.bt.pi.api.utils.IdFactory;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageIndex;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.imagemanager.xml.Manifest;
import com.bt.pi.app.imagemanager.xml.ManifestBuilder;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.scattergather.PiScatterGatherContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.sss.client.PisssClient;
import com.bt.pi.sss.client.PisssClient.BucketAccess;
import com.bt.pi.sss.exception.BucketObjectNotFoundException;
import com.bt.pi.sss.exception.NoSuchBucketException;

@RunWith(MockitoJUnitRunner.class)
public class ImageServiceImplTest {
    private String deletedFileName;

    @InjectMocks
    private ImageServiceImpl imageService = new ImageServiceImpl();

    private String imageId = "pmi-12345678";
    private String ownerId = "admin";
    private List<String> imageIds;
    private String imageManifestLocation;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private PiIdBuilder piIdBuilder;
    private Image image1;
    private Image image2;
    private Image image3;
    private String imageId1 = "i1";
    private String imageId2 = "i2";
    private String imageId3 = "i3";
    @Mock
    private BlockingDhtReader blockingReader;
    @Mock
    private PisssClient pisssClient;
    private String bucketName = "bucket";
    private String manifestFileName = "manifest.xml";
    @Mock
    private BlockingDhtWriter blockingWriter;
    @Mock
    private IdFactory idFactory;
    @Mock
    private UserService userService;
    @Mock
    private UserManagementService userManagementService;
    private User user;
    private ImageIndex imageIndex = new ImageIndex();
    @Mock
    private ManifestBuilder manifestBuilder;
    @Mock
    private Manifest manifest;
    @Mock
    private ApiApplicationManager apiApplicationManager;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    private String imagesPath = "var/images";
    @Mock
    private PId imageIndexPastryId;
    @Mock
    private PId imagePastryId;
    private String kernelId = "pki-DECAFBAD";
    private String ramdiskId = "pri-DECAFBAD";
    @Mock
    private Image image;
    @Mock
    private PId decryptImageQueueId;
    private CountDownLatch continuationLatch;

    @Mock
    private ImageRetriever imageRetriever;
    @InjectMocks
    private ImageServiceHelper imageServiceHelper = new ImageServiceHelper() {
        public void deleteFile(String name) {
            deletedFileName = name;
        }
    };

    @Before
    public void before() {
        imageService.setImageServiceHelper(imageServiceHelper);
        when(apiApplicationManager.newLocalPubSubMessageContext(PiTopics.DECRYPT_IMAGE)).thenReturn(pubSubMessageContext);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(blockingWriter);
        String xml = "<manifest/>";
        this.user = new User(ownerId, null, null);
        when(this.pisssClient.getObjectFromBucket(bucketName, manifestFileName, user)).thenReturn(xml);
        setupScatterGather();
        when(this.userManagementService.getUser(ownerId)).thenReturn(this.user);
        when(this.dhtClientFactory.createBlockingReader()).thenReturn(blockingReader);
        when(this.dhtClientFactory.createBlockingWriter()).thenReturn(blockingWriter);
        when(this.manifestBuilder.build(isA(String.class))).thenReturn(manifest);
        this.imageService.setImagesPath(imagesPath);
        when(piIdBuilder.getPId(ImageIndex.URL)).thenReturn(imageIndexPastryId);
        when(imageIndexPastryId.forLocalRegion()).thenReturn(imageIndexPastryId);
        when(piIdBuilder.getPId(isA(Image.class))).thenReturn(imagePastryId);
        when(piIdBuilder.getPId(Image.getUrl(imageId))).thenReturn(imagePastryId);
        when(piIdBuilder.getPiQueuePId(PiQueue.DECRYPT_IMAGE)).thenReturn(decryptImageQueueId);
        when(decryptImageQueueId.forLocalScope(PiQueue.DECRYPT_IMAGE.getNodeScope())).thenReturn(decryptImageQueueId);
        when(blockingReader.get(eq(imageIndexPastryId))).thenReturn(imageIndex);
        imageManifestLocation = bucketName + "/" + manifestFileName;

        this.continuationLatch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(null, null);
                continuationLatch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(isA(PId.class), isA(String.class), anyInt(), isA(TaskProcessingQueueContinuation.class));
    }

    private void setupScatterGather() {
        ScatterGatherContinuationRunner scatterGatherContinuationRunner = new ScatterGatherContinuationRunner();
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(10);
        scatterGatherContinuationRunner.setScheduledExecutorService(scheduledExecutorService);
        this.imageService.setScatterGatherContinuationRunner(scatterGatherContinuationRunner);
    }

    @SuppressWarnings({ "serial", "unchecked", "rawtypes" })
    private void setupDin() {
        when(this.userManagementService.getUser(ownerId)).thenReturn(this.user);
        final PId imagePastryId1 = mock(PId.class);
        final PId imagePastryId2 = mock(PId.class);
        final PId imagePastryId3 = mock(PId.class);
        image1 = new Image(imageId1, null, null, null, null, null, ImagePlatform.linux, false, MachineType.MACHINE);
        image2 = new Image(imageId2, null, null, null, null, null, ImagePlatform.linux, true, MachineType.MACHINE);
        image3 = new Image(imageId3, null, null, null, "joeBloggs", null, ImagePlatform.linux, false, MachineType.MACHINE);
        DhtReader reader = mock(DhtReader.class);
        when(this.dhtClientFactory.createReader()).thenReturn(reader);
        when(this.piIdBuilder.getPId(eq(Image.getUrl(imageId1)))).thenReturn(imagePastryId1);
        when(this.piIdBuilder.getPId(eq(Image.getUrl(imageId2)))).thenReturn(imagePastryId2);
        when(this.piIdBuilder.getPId(eq(Image.getUrl(imageId3)))).thenReturn(imagePastryId3);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiScatterGatherContinuation continuation = (PiScatterGatherContinuation) invocation.getArguments()[1];
                PId id = (PId) invocation.getArguments()[0];
                if (id.equals(imagePastryId1))
                    continuation.receiveResult(image1);
                if (id.equals(imagePastryId2))
                    continuation.receiveResult(image2);
                if (id.equals(imagePastryId3))
                    continuation.receiveResult(image3);
                return null;
            }
        }).when(reader).getAsync(isA(PId.class), isA(PiScatterGatherContinuation.class));
        Set<String> imageSet = new HashSet<String>() {
            {
                add(imageId1);
                add(imageId2);
                add(imageId3);
            }
        };
        imageIndex.getImages().addAll(imageSet);
    }

    @Test
    public void testDescribeImages() {
        // setup
        setupDin();

        // act
        Set<Image> result = imageService.describeImages(ownerId, imageIds);

        // assert
        assertEquals(3, result.size());
        assertTrue(result.contains(image1));
        assertTrue(result.contains(image2));
        assertTrue(result.contains(image3));
    }

    @Test
    public void testDescribeImagesWithSomeInUserRecord() {
        // setup
        setupDin();
        user.getImageIds().add(imageId1);
        user.getImageIds().add(imageId2);
        imageIndex.getImages().remove(imageId1);
        imageIndex.getImages().remove(imageId2);

        // act
        Set<Image> result = imageService.describeImages(ownerId, imageIds);

        // assert
        assertEquals(3, result.size());
        assertTrue(result.contains(image1));
        assertTrue(result.contains(image2));
        assertTrue(result.contains(image3));
    }

    @Test
    public void testDescribeImagesIndexNotPresent() {
        // setup
        setupDin();
        Mockito.when(blockingReader.get(Matchers.eq(imageIndexPastryId))).thenReturn(null);

        // act
        Set<Image> result = imageService.describeImages(ownerId, imageIds);

        // assert
        assertEquals(0, result.size());
    }

    @Test
    public void testDescribeSelectImagesWithNoImagesIndex() {
        // setup
        setupDin();

        imageIds = new ArrayList<String>();
        imageIds.add(imageId2);

        Mockito.when(blockingReader.get(Matchers.eq(this.piIdBuilder.getPId(ImageIndex.URL).forLocalRegion()))).thenReturn(null);

        // act
        Set<Image> result = imageService.describeImages(ownerId, imageIds);

        // assert
        assertEquals(0, result.size());
    }

    @Test
    public void testDescribeSelectedImages() {
        // setup
        setupDin();

        imageIds = new ArrayList<String>();
        imageIds.add(imageId2);
        imageIds.add(imageId3);

        // act
        Set<Image> result = imageService.describeImages(ownerId, imageIds);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(image2));
        assertTrue(result.contains(image3));
    }

    @Test
    public void testDescribeImagesSelectedWithBogus() throws Exception {
        // setup
        setupDin();

        imageIds = new ArrayList<String>();
        imageIds.add(imageId1);
        imageIds.add(imageId3);
        imageIds.add("bogus");

        // act
        Set<Image> result = imageService.describeImages(ownerId, imageIds);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(image1));
        assertTrue(result.contains(image3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterImageNullImageManifestLocation() {
        // setup
        imageManifestLocation = null;

        // act
        imageService.registerImage(ownerId, imageManifestLocation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterImageImageManifestLocationNoSlash() {
        // setup
        imageManifestLocation = "abc123";

        // act
        imageService.registerImage(ownerId, imageManifestLocation);
    }

    @Test
    public void shouldCallPisssForManifestWhenRegisteringImage() throws Exception {
        // setup

        // act
        imageService.registerImage(ownerId, imageManifestLocation);

        // assert
        verify(this.pisssClient).getObjectFromBucket(bucketName, manifestFileName, user);
    }

    @Test
    public void shouldNotRejectEmptyStringForKernelAndOrRamdiskWhenRegisteringImage() throws Exception {
        // setup
        when(manifest.getKernelId()).thenReturn("");
        when(manifest.getRamdiskId()).thenReturn("");

        // act
        imageService.registerImage(ownerId, imageManifestLocation);

        // assert
        verify(this.pisssClient).getObjectFromBucket(bucketName, manifestFileName, user);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterImageNoAccessToKernel() {
        // setup
        String kernelId = "k-123";
        when(manifest.getKernelId()).thenReturn(kernelId);

        // act
        try {
            imageService.registerImage(ownerId, imageManifestLocation);
        } catch (IllegalArgumentException e) {
            // assert
            assertEquals(String.format("user %s does not have access to kernel %s", ownerId, kernelId), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterImageNoAccessToRamdisk() {
        // setup
        String ramdiskId = "r-123";
        when(manifest.getRamdiskId()).thenReturn(ramdiskId);

        // act
        try {
            imageService.registerImage(ownerId, imageManifestLocation);
        } catch (IllegalArgumentException e) {
            // assert
            assertEquals(String.format("user %s does not have access to ramdisk %s", ownerId, ramdiskId), e.getMessage());
            throw e;
        }
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundExceptionIfManifestNotInPisss() throws Exception {
        // setup
        String message = "bugger";
        doThrow(new NotFoundException(message)).when(this.pisssClient).getObjectFromBucket(bucketName, manifestFileName, user);

        // act
        try {
            imageService.registerImage(ownerId, imageManifestLocation);
        } catch (NotFoundException e) {
            assertEquals(message, e.getMessage());
            throw e;
        }
    }

    @Test
    public void shouldCallPisssForBucketAclWhenRegisteringImage() throws Exception {
        // setup

        // act
        imageService.registerImage(ownerId, imageManifestLocation);

        // assert
        verify(this.pisssClient).getBucketAccess(bucketName, user);
    }

    @Test(expected = NotAuthorizedException.class)
    public void shouldThrowNotAuthorizedIfNoAccessToBucket() throws Exception {
        // setup
        when(this.pisssClient.getBucketAccess(bucketName, user)).thenReturn(BucketAccess.NONE);

        // act
        imageService.registerImage(ownerId, imageManifestLocation);
    }

    @Test
    public void shouldWriteImageToDhtRegisteringImage() throws Exception {
        // setup
        when(this.idFactory.createNewImageId()).thenReturn(this.imageId);

        // act
        imageService.registerImage(ownerId, imageManifestLocation);

        // assert
        verify(this.blockingWriter).put(eq(imagePastryId), Matchers.argThat(new ArgumentMatcher<Image>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof Image))
                    return false;
                Image image = (Image) argument;
                if (!image.getImageId().equals(imageId))
                    return false;
                if (!ImageState.PENDING.equals(image.getState()))
                    return false;
                return true;
            }
        }));
    }

    @Test
    public void shouldAddPrivateImageToUserWhenRegisteringImage() throws Exception {
        // setup
        when(this.pisssClient.getBucketAccess(bucketName, user)).thenReturn(BucketAccess.PRIVATE);
        when(this.idFactory.createNewImageId()).thenReturn(this.imageId);

        // act
        imageService.registerImage(ownerId, imageManifestLocation);

        // assert
        verify(this.userService).addImageToUser(ownerId, imageId);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldAddPublicImageToImageIndexWhenRegisteringImage() throws Exception {
        // setup
        when(this.pisssClient.getBucketAccess(bucketName, user)).thenReturn(BucketAccess.PUBLIC);
        when(this.idFactory.createNewImageId()).thenReturn(this.imageId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[2];
                resolver.update(imageIndex, null);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imageIndexPastryId), (PiEntity) isNull(), isA(UpdateResolver.class));

        // act
        imageService.registerImage(ownerId, imageManifestLocation);

        // assert
        assertTrue(imageIndex.getImages().contains(imageId));
    }

    @Test
    public void shouldReadManifestFileWhenRegisteringImage() throws Exception {
        // setup
        when(this.idFactory.createNewImageId()).thenReturn(this.imageId);
        final String arch = "i386";
        final String kernelId = "kkkk";
        final String ramdiskId = "rrrr";
        imageIndex.getImages().add(kernelId);
        imageIndex.getImages().add(ramdiskId);
        when(manifest.getArch()).thenReturn(arch);
        when(manifest.getRamdiskId()).thenReturn(ramdiskId);
        when(manifest.getKernelId()).thenReturn(kernelId);
        String xml = "<manifest><machine_configuration><ramdisk_id>" + ramdiskId + "</ramdisk_id><kernel_id>" + kernelId + "</kernel_id><architecture>" + arch + "</architecture></machine_configuration></manifest>";
        when(this.pisssClient.getObjectFromBucket(bucketName, manifestFileName, user)).thenReturn(xml);

        // act
        imageService.registerImage(ownerId, imageManifestLocation);

        // assert
        verify(this.blockingWriter).put(eq(imagePastryId), Matchers.argThat(new ArgumentMatcher<Image>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof Image))
                    return false;
                Image image = (Image) argument;
                if (!image.getImageId().equals(imageId))
                    return false;
                if (!image.getArchitecture().equals(arch))
                    return false;
                if (!image.getKernelId().equals(kernelId))
                    return false;
                if (!image.getRamdiskId().equals(ramdiskId))
                    return false;
                return true;
            }
        }));
    }

    @Test
    public void shouldAnycastImageForDecryptionWhenRegisteringImage() throws InterruptedException {
        // setup

        // act
        imageService.registerImage(ownerId, imageManifestLocation);

        // assert
        assertTrue(continuationLatch.await(250, TimeUnit.MILLISECONDS));
        verify(this.pubSubMessageContext).randomAnycast(eq(EntityMethod.CREATE), isA(Image.class));
    }

    @Test
    public void shouldAddImageUrlToQueueWhenRegisteringImage() throws InterruptedException {
        // setup
        when(this.idFactory.createNewImageId()).thenReturn(this.imageId);

        // act
        imageService.registerImage(ownerId, imageManifestLocation);

        // assert
        assertTrue(continuationLatch.await(250, TimeUnit.MILLISECONDS));
        verify(this.taskProcessingQueueHelper).addUrlToQueue(eq(decryptImageQueueId), eq(Image.getUrl(this.imageId)), eq(Integer.parseInt(ImageServiceImpl.DEFAULT_DECRYPTION_RETRIES)), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void shouldRegisterKernelImage() {
        // setup
        when(this.idFactory.createNewKernelId()).thenReturn(this.kernelId);
        when(this.piIdBuilder.getPId(kernelId)).thenReturn(imagePastryId);

        // act
        String result = imageService.registerImage(ownerId, imageManifestLocation, MachineType.KERNEL);

        // assert
        assertEquals(this.kernelId, result);
        verify(this.blockingWriter).put(eq(imagePastryId), argThat(new ArgumentMatcher<Image>() {
            @Override
            public boolean matches(Object argument) {
                Image image = (Image) argument;
                return image.getImageId().equals(kernelId) && image.getMachineType().equals(MachineType.KERNEL) && image.getKernelId() == null && image.getRamdiskId() == null;
            }
        }));
    }

    @Test
    public void shouldRegisterRamdiskImage() {
        // setup
        when(this.idFactory.createNewRamdiskId()).thenReturn(this.ramdiskId);
        when(this.piIdBuilder.getPId(ramdiskId)).thenReturn(imagePastryId);

        // act
        String result = imageService.registerImage(ownerId, imageManifestLocation, MachineType.RAMDISK);

        // assert
        assertEquals(this.ramdiskId, result);
        verify(this.blockingWriter).put(eq(imagePastryId), argThat(new ArgumentMatcher<Image>() {
            @Override
            public boolean matches(Object argument) {
                Image image = (Image) argument;
                return image.getImageId().equals(ramdiskId) && image.getMachineType().equals(MachineType.RAMDISK) && image.getKernelId() == null && image.getRamdiskId() == null;
            }
        }));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldDeleteImageFromImageIndexIfPublicForDeregisterImage() throws Exception {
        // setup
        when(image.isPublic()).thenReturn(true);
        when(image.getOwnerId()).thenReturn(ownerId);
        when(image.getMachineType()).thenReturn(MachineType.MACHINE);

        imageIndex.getImages().add(imageId);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[2];
                continuation.update(imageIndex, imageIndex);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imageIndexPastryId), (ImageIndex) isNull(), isA(UpdateResolver.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[2];
                continuation.update(image, image);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imagePastryId), (Image) isNull(), isA(UpdateResolver.class));

        // act
        boolean result = this.imageService.deregisterImage(ownerId, imageId);

        // assert
        assertTrue(result);
        assertFalse(imageIndex.getImages().contains(imageId));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(expected = NotAuthorizedException.class)
    public void shouldNotDeleteImageFromImageIndexIfPublicButNotOwnerForDeregisterImage() throws Exception {
        // setup
        when(image.isPublic()).thenReturn(true);
        when(image.getOwnerId()).thenReturn("loser");

        imageIndex.getImages().add(imageId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[2];
                continuation.update(image, image);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imagePastryId), (Image) isNull(), isA(UpdateResolver.class));

        // act
        try {
            this.imageService.deregisterImage(ownerId, imageId);
        } catch (NotAuthorizedException e) {
            // assert
            assertTrue(imageIndex.getImages().contains(imageId));
            throw e;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(expected = NotAuthorizedException.class)
    public void shouldNotDeleteImageFromImageIndexIfNotMachine() throws Exception {
        // setup
        when(image.isPublic()).thenReturn(true);
        when(image.getOwnerId()).thenReturn(ownerId);
        when(image.getMachineType()).thenReturn(MachineType.KERNEL);

        imageIndex.getImages().add(imageId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[2];
                continuation.update(image, image);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imagePastryId), (Image) isNull(), isA(UpdateResolver.class));

        // act
        try {
            this.imageService.deregisterImage(ownerId, imageId);
        } catch (NotAuthorizedException e) {
            // assert
            assertTrue(imageIndex.getImages().contains(imageId));
            throw e;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldDeleteImageFromUserIfNotPublicForDeregisterImage() throws Exception {
        // setup
        PId userId = mock(PId.class);
        when(image.isPublic()).thenReturn(false);
        when(image.getOwnerId()).thenReturn(ownerId);
        when(image.getMachineType()).thenReturn(MachineType.MACHINE);
        final User user = new User(ownerId, null, null);
        user.getImageIds().add(imageId);

        when(this.piIdBuilder.getPId(User.getUrl(ownerId))).thenReturn(userId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                user.getImageIds().remove(invocation.getArguments()[1]);
                return null;
            }
        }).when(this.userService).removeImageFromUser(ownerId, imageId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[2];
                continuation.update(image, image);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imagePastryId), (Image) isNull(), isA(UpdateResolver.class));

        // act
        boolean result = this.imageService.deregisterImage(ownerId, imageId);

        // assert
        assertTrue(result);
        assertFalse(user.getImageIds().contains(imageId));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldDeleteImageFileForDeregisterImage() throws Exception {
        // setup
        when(image.isPublic()).thenReturn(true);
        when(image.getOwnerId()).thenReturn(ownerId);
        when(image.getMachineType()).thenReturn(MachineType.MACHINE);

        imageIndex.getImages().add(imageId);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[2];
                continuation.update(imageIndex, imageIndex);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imageIndexPastryId), (ImageIndex) isNull(), isA(UpdateResolver.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[2];
                continuation.update(image, image);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imagePastryId), (Image) isNull(), isA(UpdateResolver.class));

        // act
        this.imageService.deregisterImage(ownerId, imageId);

        // assert
        assertEquals(String.format("%s/%s", imagesPath, imageId), deletedFileName);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldMarkImageAsDeletedForDeregisterImage() throws Exception {
        // setup
        when(image.isPublic()).thenReturn(true);
        when(image.getOwnerId()).thenReturn(ownerId);
        when(image.getMachineType()).thenReturn(MachineType.MACHINE);
        when(this.blockingReader.get(imagePastryId)).thenReturn(image);

        imageIndex.getImages().add(imageId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[2];
                continuation.update(imageIndex, imageIndex);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imageIndexPastryId), (ImageIndex) isNull(), isA(UpdateResolver.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[2];
                continuation.update(image, image);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imagePastryId), (Image) isNull(), isA(UpdateResolver.class));

        PowerMockito.mockStatic(FileUtils.class);

        // act
        this.imageService.deregisterImage(ownerId, imageId);

        // assert
        verify(image).setDeleted(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentWhenBucketNotFound() throws Exception {
        // setup
        when(pisssClient.getObjectFromBucket(bucketName, manifestFileName, user)).thenThrow(new NoSuchBucketException());

        // act
        imageService.registerImage(ownerId, imageManifestLocation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentWhenManifestNotFound() throws Exception {
        // setup
        when(pisssClient.getObjectFromBucket(bucketName, manifestFileName, user)).thenThrow(new BucketObjectNotFoundException());

        // act
        imageService.registerImage(ownerId, imageManifestLocation);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldDeregisterImageWithoutMachineTypeCheck() {
        // setup
        when(image.getMachineType()).thenReturn(MachineType.KERNEL);
        when(image.getOwnerId()).thenReturn(ownerId);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[2];
                continuation.update(image, image);
                return null;
            }
        }).when(this.blockingWriter).update(eq(imagePastryId), (Image) isNull(), isA(UpdateResolver.class));

        // act
        imageService.deregisterImageWithoutMachineTypeCheck(ownerId, imageId);

        // assert
        verify(image).setDeleted(true);
    }
}
