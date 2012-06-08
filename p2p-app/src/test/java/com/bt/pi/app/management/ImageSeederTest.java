package com.bt.pi.app.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageIndex;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

public class ImageSeederTest {
    private ImageSeeder imageSeeder;
    private BlockingDhtWriter dhtWriterImage;
    private BlockingDhtReader dhtReaderImage;
    private String createdImageId;
    private AtomicBoolean writeSucceeded;
    private Answer<Boolean> entityWrittenAnswer;
    private KoalaIdFactory koalaIdFactory;
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;

    @Before
    public void before() throws Exception {
        ArrayList<PiEntity> entities = new ArrayList<PiEntity>();
        entities.add(new Image());

        KoalaPiEntityFactory koalaPiEntityFactory = new KoalaPiEntityFactory();
        koalaPiEntityFactory.setKoalaJsonParser(new KoalaJsonParser());
        koalaPiEntityFactory.setPiEntityTypes(entities);
        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        writeSucceeded = new AtomicBoolean(true);

        entityWrittenAnswer = new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return writeSucceeded.get();
            }
        };

        dhtReaderImage = mock(BlockingDhtReader.class);
        dhtWriterImage = mock(BlockingDhtWriter.class);

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterImage);
        when(dhtClientFactory.createBlockingReader()).thenReturn(dhtReaderImage);

        imageSeeder = new ImageSeeder();
        imageSeeder.setDhtClientFactory(dhtClientFactory);
        imageSeeder.setPiIdBuilder(piIdBuilder);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldHandleFailureWhenImageFailsToWrite() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterImage);
        doAnswer(entityWrittenAnswer).when(dhtWriterImage).update(isA(PId.class), isA(Image.class), isA(UpdateResolver.class));
        when(dhtWriterImage.getValueWritten()).thenReturn(null);

        // act
        String res = imageSeeder.createImage("imageId", 123, "kernelId", "ramDiskId", "manifestLocation", "ownerId", "architecture", "linux", true, "KERNEL");

        // assert
        assertNull(res);
    }

    @Test
    public void shouldCreateImageUsingTheImageNamePassedIn() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterImage);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                createdImageId = ((Image) invocation.getArguments()[1]).getImageId();
                return null;
            }
        }).when(dhtWriterImage).writeIfAbsent(isA(PId.class), isA(Image.class));

        // act
        imageSeeder.createImage("imageId", 123, "kernelId", "ramDiskId", "manifestLocation", "ownerId", "architecture", "linux", true, MachineType.KERNEL.toString());

        // assert
        assertEquals("imageId", createdImageId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatePublicImageCreatesNewImageIndexAndAddsImage() {
        // setup
        BlockingDhtWriter dhtWriterImageIndex = mock(BlockingDhtWriter.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterImageIndex).thenReturn(dhtWriterImage);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                createdImageId = ((Image) invocation.getArguments()[1]).getImageId();
                return null;
            }
        }).when(dhtWriterImage).writeIfAbsent(isA(PId.class), isA(Image.class));

        PiEntity piEntity = mock(PiEntity.class);
        when(dhtWriterImage.getValueWritten()).thenReturn(piEntity);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver updateResolver = (UpdateResolver) invocation.getArguments()[2];
                Object requestedEntity = invocation.getArguments()[1];
                Object result = updateResolver.update(null, requestedEntity);
                assertTrue(result instanceof ImageIndex);
                ImageIndex imageIndex = (ImageIndex) result;
                assertEquals(1, imageIndex.getImages().size());
                assertTrue(imageIndex.getImages().contains("imageId"));
                return null;
            }
        }).when(dhtWriterImageIndex).update(isA(PId.class), isA(ImageIndex.class), isA(UpdateResolver.class));

        // act
        imageSeeder.createImage("imageId", 123, "kernelId", "ramDiskId", "manifestLocation", "ownerId", "architecture", "linux", true, MachineType.KERNEL.toString());

        // assert
        Mockito.verify(dhtWriterImageIndex).update(isA(PId.class), isA(ImageIndex.class), isA(UpdateResolver.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatePublicImageAddsImageToExistingImageIndex() {
        // setup
        BlockingDhtWriter dhtWriterImageIndex = mock(BlockingDhtWriter.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterImageIndex).thenReturn(dhtWriterImage);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                createdImageId = ((Image) invocation.getArguments()[1]).getImageId();
                return null;
            }
        }).when(dhtWriterImage).update(isA(PId.class), isA(Image.class), isA(UpdateResolver.class));

        PiEntity piEntity = mock(PiEntity.class);
        when(dhtWriterImage.getValueWritten()).thenReturn(piEntity);

        final ImageIndex existingImageIndex = new ImageIndex();
        existingImageIndex.getImages().add("existingImageId");

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver updateResolver = (UpdateResolver) invocation.getArguments()[2];
                Object requestedEntity = invocation.getArguments()[1];
                Object result = updateResolver.update(existingImageIndex, requestedEntity);
                assertTrue(result instanceof ImageIndex);
                ImageIndex imageIndex = (ImageIndex) result;
                assertEquals(2, imageIndex.getImages().size());
                assertTrue(imageIndex.getImages().contains("existingImageId"));
                assertTrue(imageIndex.getImages().contains("imageId"));
                return null;
            }
        }).when(dhtWriterImageIndex).update(isA(PId.class), isA(ImageIndex.class), isA(UpdateResolver.class));

        // act
        imageSeeder.createImage("imageId", 123, "kernelId", "ramDiskId", "manifestLocation", "ownerId", "architecture", "linux", true, MachineType.KERNEL.toString());

        // assert
        Mockito.verify(dhtWriterImageIndex).update(isA(PId.class), isA(ImageIndex.class), isA(UpdateResolver.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatePrivateImageAddsImageToUser() {
        // setup
        BlockingDhtWriter dhtWriterUser = mock(BlockingDhtWriter.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterUser).thenReturn(dhtWriterImage);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                createdImageId = ((Image) invocation.getArguments()[1]).getImageId();
                return null;
            }
        }).when(dhtWriterImage).update(isA(PId.class), isA(Image.class), isA(UpdateResolver.class));

        PiEntity piEntity = mock(PiEntity.class);
        when(dhtWriterImage.getValueWritten()).thenReturn(piEntity);

        final String ownerId = "ownerId";
        final User user = new User(ownerId, null, null);
        user.getImageIds().add("existingImageId");

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver updateResolver = (UpdateResolver) invocation.getArguments()[2];
                updateResolver.update(user, user);
                return null;
            }
        }).when(dhtWriterUser).update(isA(PId.class), (PiEntity) isNull(), isA(UpdateResolver.class));

        // act
        imageSeeder.createImage("imageId", 123, "kernelId", "ramDiskId", "manifestLocation", ownerId, "architecture", "linux", false, MachineType.KERNEL.toString());

        // assert
        assertTrue(user.getImageIds().contains("existingImageId"));
        assertTrue(user.getImageIds().contains("imageId"));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testCreatePrivateImageThrowsExceptionIfUserDoesNotExist() {
        // setup
        BlockingDhtWriter dhtWriterUser = mock(BlockingDhtWriter.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterUser).thenReturn(dhtWriterImage);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                createdImageId = ((Image) invocation.getArguments()[1]).getImageId();
                return null;
            }
        }).when(dhtWriterImage).update(isA(PId.class), isA(Image.class), isA(UpdateResolver.class));

        PiEntity piEntity = mock(PiEntity.class);
        when(dhtWriterImage.getValueWritten()).thenReturn(piEntity);

        final String ownerId = "ownerId";
        final User user = new User(ownerId, null, null);
        user.getImageIds().add("existingImageId");

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver updateResolver = (UpdateResolver) invocation.getArguments()[2];
                updateResolver.update(null, null);
                return null;
            }
        }).when(dhtWriterUser).update(isA(PId.class), (PiEntity) isNull(), isA(UpdateResolver.class));

        // act
        imageSeeder.createImage("imageId", 123, "kernelId", "ramDiskId", "manifestLocation", ownerId, "architecture", "linux", false, MachineType.KERNEL.toString());
    }

    @Test
    public void shouldCreateRandomImageIdForNullWithTheCorrectMACHINEPrefixes() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterImage);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                createdImageId = ((Image) invocation.getArguments()[1]).getImageId();
                return null;
            }
        }).when(dhtWriterImage).writeIfAbsent(isA(PId.class), isA(Image.class));

        // act
        imageSeeder.createImage(null, 123, "kernelId", "ramDiskId", "manifestLocation", "ownerId", "architecture", "linux", true, MachineType.MACHINE.toString());

        // assert
        assertTrue("Image did not have corrext prefix.", createdImageId.startsWith(MachineType.MACHINE.getImagePrefix()));
        assertNotSame(createdImageId, "pmi-");
        assertEquals(12, createdImageId.length());
    }

    @Test
    public void shouldCreateRandomImageIdForEmptyStringWithTheCorrectMACHINEPrefixes() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterImage);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                createdImageId = ((Image) invocation.getArguments()[1]).getImageId();
                return null;
            }
        }).when(dhtWriterImage).writeIfAbsent(isA(PId.class), isA(Image.class));

        // act
        imageSeeder.createImage("", 123, "kernelId", "ramDiskId", "manifestLocation", "ownerId", "architecture", "linux", true, MachineType.MACHINE.toString());

        // assert
        assertTrue("Image did not have corrext prefix.", createdImageId.startsWith(MachineType.MACHINE.getImagePrefix()));
        assertEquals(12, createdImageId.length());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateImagePlatformToTheProvidedPlatform() {
        // setup
        final String testImageId = "pmi-AAA";
        final String windowsPlatform = "windows";

        Image testImage = new Image();
        testImage.setImageId(testImageId);
        testImage.setPlatform(ImagePlatform.linux);

        PId testPiImageId = piIdBuilder.getPId(testImage);

        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterImage);

        UpdateResolvingContinuationAnswer c = new UpdateResolvingContinuationAnswer(testImage);
        doAnswer(c).when(dhtWriterImage).update(eq(testPiImageId), (PiEntity) isNull(), isA(UpdateResolvingPiContinuation.class));

        // act
        imageSeeder.updateImagePlatform("pmi-AAA", windowsPlatform);

        Image res = (Image) c.getResult();

        // assert
        assertEquals(windowsPlatform, res.getPlatform().toString());
        verify(dhtWriterImage).update(eq(testPiImageId), (PiEntity) isNull(), isA(UpdateResolvingPiContinuation.class));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnableToFindImageWithTheImageId() {
        // setup
        final String testImageId = "pmi-BBB";
        final String platform = "windows";

        PId testPiImageId = piIdBuilder.getPId(Image.getUrl(testImageId));

        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterImage);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation<Image> c = (UpdateResolvingPiContinuation<Image>) invocation.getArguments()[2];
                Image updatedImage = c.update(null, null);
                c.handleResult(updatedImage);
                return null;
            }
        }).when(dhtWriterImage).update(eq(testPiImageId), (PiEntity) isNull(), isA(UpdateResolvingPiContinuation.class));

        // act
        imageSeeder.updateImagePlatform(testImageId, platform);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfPlatformValueIsIncorrect() {
        // setup
        final String testImageId = "pmi-BBB";
        final String platform = "windoz";

        // act
        imageSeeder.updateImagePlatform(testImageId, platform);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfPlatformIsNotProvided() {
        // setup
        String testImageId = "pmi-CCC";

        // act
        imageSeeder.updateImagePlatform(testImageId, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfImageIdIsNotProvided() {
        // setup
        String platform = "windows";

        // act
        imageSeeder.updateImagePlatform(null, platform);
    }
}
