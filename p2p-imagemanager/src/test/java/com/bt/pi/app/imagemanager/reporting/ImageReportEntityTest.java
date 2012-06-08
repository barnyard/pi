package com.bt.pi.app.imagemanager.reporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.MachineType;

@RunWith(MockitoJUnitRunner.class)
public class ImageReportEntityTest {

    @Test
    public void shouldReturnCreationTime() {
        // setup
        long now = new Date().getTime();
        // act
        ImageReportEntity entity = new ImageReportEntity();
        // assert
        assertTrue("Creation time should be after timestamp", entity.getCreationTime() >= now);

    }

    @Test
    public void shouldBeEqualIfImagesAreEqual() {
        // setup
        Image image1 = new Image();
        image1.setImageId("ID");
        image1.setKernelId("KERNEL_ID");
        image1.setMachineType(MachineType.KERNEL);
        image1.setManifestLocation("MANIFEST_LOCATION");
        Image image2 = new Image();
        image2.setImageId(image1.getImageId());
        image2.setKernelId(image1.getKernelId());
        image2.setMachineType(image1.getMachineType());
        image2.setManifestLocation(image1.getManifestLocation());
        // act
        ImageReportEntity entity1 = new ImageReportEntity(image1);
        ImageReportEntity entity2 = new ImageReportEntity(image2);
        // assert
        assertTrue(entity1.equals(entity2));
    }

    @Test
    public void shouldHaveSimpleNameAsUriScheme() {
        // act
        ImageReportEntity entity = new ImageReportEntity();
        // assert

        assertEquals(ImageReportEntity.class.getSimpleName(), entity.getUriScheme());
    }

    @Test
    public void shouldReturnSimpleClassNameAsType() {
        // act
        ImageReportEntity entity = new ImageReportEntity();
        // assert

        assertEquals(ImageReportEntity.class.getSimpleName(), entity.getType());
    }

    @Test
    public void shouldReturnImageid() {
        // setup
        Image image1 = new Image();
        image1.setImageId("ID");
        // act
        ImageReportEntity entity1 = new ImageReportEntity(image1);
        // assert
        assertEquals(image1.getImageId(), entity1.getId());
    }

    @Test
    public void shouldReturnZeroKeysForMapCount() {
        // act
        ImageReportEntity entity = new ImageReportEntity();
        // assert
        assertEquals(0, entity.getKeysForMapCount());
    }

    @Test
    public void shouldReturnEmptyArrayOfKeysForMap() {
        // act
        ImageReportEntity entity = new ImageReportEntity();
        // assert
        assertEquals(0, entity.getKeysForMap().length);
    }

    @Test
    public void shouldReturnNullUrl() {
        // act
        ImageReportEntity entity = new ImageReportEntity();
        // assert
        assertNull(entity.getUrl());
    }

}
