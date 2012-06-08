package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DescribeImagesDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeImagesInfoType;
import com.amazonaws.ec2.doc.x20081201.DescribeImagesResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeImagesType;
import com.bt.pi.api.service.ManagementImageService;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.images.platform.ImagePlatform;

public class DescribeImagesHandlerTest extends AbstractHandlerTest {

    private DescribeImagesHandler describeImagesHandler;
    private ManagementImageService imageService;
    private Set<Image> images;

    @Before
    public void setUp() throws Exception {
        super.before();
        this.describeImagesHandler = new DescribeImagesHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        imageService = mock(ManagementImageService.class);
        List<String> imageIds = new ArrayList<String>();
        imageIds.add("kmi-111");
        imageIds.add("kmi-222");
        images = new HashSet<Image>();
        images.add(new Image("kmi-111", "k-111", "r-111", "manifest", "userid", "architecture", ImagePlatform.linux, true, MachineType.KERNEL));
        images.add(new Image("kmi-111", "k-222", "r-111", "manifest", "userid", "architecture", ImagePlatform.linux, true, MachineType.RAMDISK));
        when(imageService.describeImages("userid", imageIds)).thenReturn(images);
        describeImagesHandler.setImageService(imageService);
    }

    @Test
    public void testDescribeImages() {
        // setup
        DescribeImagesDocument requestDocument = DescribeImagesDocument.Factory.newInstance();
        DescribeImagesType addNewDescribeImages = requestDocument.addNewDescribeImages();
        DescribeImagesInfoType addNewImagesSet = addNewDescribeImages.addNewImagesSet();
        addNewImagesSet.addNewItem().setImageId("kmi-111");
        addNewImagesSet.addNewItem().setImageId("kmi-222");

        // act
        DescribeImagesResponseDocument result = this.describeImagesHandler.describeImages(requestDocument);

        // assert
        assertEquals(2, result.getDescribeImagesResponse().getImagesSet().getItemArray().length);
        assertKernelIds(result);

    }

    private void assertKernelIds(DescribeImagesResponseDocument result) {
        List<String> kernelIds = new ArrayList<String>();
        for (com.amazonaws.ec2.doc.x20081201.DescribeImagesResponseItemType item : result.getDescribeImagesResponse().getImagesSet().getItemArray()) {
            kernelIds.add(item.getKernelId());
        }
        assertTrue(kernelIds.contains("k-111"));
        assertTrue(kernelIds.contains("k-222"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDescribeImagesPassesNullListIfInputListIsEmpty() {
        // setup
        DescribeImagesDocument requestDocument = DescribeImagesDocument.Factory.newInstance();
        requestDocument.addNewDescribeImages();
        when(imageService.describeImages(Matchers.eq("userid"), (List<String>) Matchers.isNull())).thenReturn(images);

        // act
        DescribeImagesResponseDocument result = this.describeImagesHandler.describeImages(requestDocument);

        // assert
        assertEquals(2, result.getDescribeImagesResponse().getImagesSet().getItemArray().length);
        assertKernelIds(result);
    }
}
