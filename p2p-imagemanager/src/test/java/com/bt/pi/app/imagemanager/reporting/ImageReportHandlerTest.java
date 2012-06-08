package com.bt.pi.app.imagemanager.reporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.core.application.reporter.ReportableEntityStore;

@RunWith(MockitoJUnitRunner.class)
public class ImageReportHandlerTest {

    private ImageReportHandler handler = spy(new ImageReportHandler());

    @Mock
    private ReportableEntityStore<ImageReportEntity> reportableEntityStore;

    @Before
    public void before() {
        ReflectionTestUtils.setField(handler, "reportableEntityStore", reportableEntityStore);

    }

    @Test
    public void shouldHandleImageReportEntities() {
        // assert
        assertEquals(Arrays.asList(new String[] { new ImageReportEntityCollection().getType() }), handler.getReportableEntityTypesHandled());
    }

    @Test
    public void shouldReturnAnImageReportEntityCollection() {
        assertEquals(new ImageReportEntityCollection(), handler.getPiEntityCollection());
    }

    @Test
    public void shouldUpdateReportableEntityStoreWhenTimeToLiveIsSet() {
        // act
        handler.setTimeToLive(0);
        // assert
        verify(handler).updateReportableEntityStore();
    }

    @Test
    public void shouldUpdateReportableEntityStoreWhenSizeIsSet() {
        // act
        handler.setSize(0);
        // assert
        verify(handler).updateReportableEntityStore();
    }

    @Test
    public void shouldSetPublishIntervalSeconds() {
        // act
        handler.setPublishIntervalSeconds(0);

    }

    @Test
    public void shouldReturnOnlyTheEntitiesRequested() {
        // setup
        Image image1 = mock(Image.class);
        Image image2 = mock(Image.class);
        Image image3 = mock(Image.class);
        when(image1.getImageId()).thenReturn("IMAGE_1");
        when(image2.getImageId()).thenReturn("IMAGE_2");
        when(image3.getImageId()).thenReturn("IMAGE_3");
        ImageReportEntity entity1 = new ImageReportEntity(image1);

        ImageReportEntity entity2 = new ImageReportEntity(image2);

        ImageReportEntity entity3 = new ImageReportEntity(image3);

        Image receivedImage1 = new Image();
        receivedImage1.setImageId("IMAGE_1");

        Image receivedImage2 = new Image();
        receivedImage2.setImageId("IMAGE_2");
        ImageReportEntityCollection receivedCollection = new ImageReportEntityCollection();
        receivedCollection.setEntities(Arrays.asList(new ImageReportEntity[] { new ImageReportEntity(receivedImage1), new ImageReportEntity(receivedImage2) }));

        List<ImageReportEntity> entityList = new ArrayList<ImageReportEntity>();
        entityList.addAll(Arrays.asList(new ImageReportEntity[] { entity1, entity2, entity3 }));

        when(reportableEntityStore.getAllEntities()).thenReturn(entityList);

        // act
        ImageReportEntityCollection collection = (ImageReportEntityCollection) handler.getEntities(receivedCollection);
        // assert
        assertTrue(collection.getEntities().contains(entity1));
        assertTrue(collection.getEntities().contains(entity2));
        assertFalse(collection.getEntities().contains(entity3));
    }
}
