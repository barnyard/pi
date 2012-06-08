/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DescribeImagesInfoType;
import com.amazonaws.ec2.doc.x20090404.DescribeImagesItemType;
import com.amazonaws.ec2.doc.x20090404.DescribeImagesResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeImagesResponseInfoType;
import com.amazonaws.ec2.doc.x20090404.DescribeImagesResponseItemType;
import com.amazonaws.ec2.doc.x20090404.DescribeImagesResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeImagesType;
import com.bt.pi.api.service.ManagementImageService;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DescribeImages
 */
@Endpoint
public class DescribeImagesHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(DescribeImagesHandler.class);
    private static final String OPERATION = "DescribeImages";
    private ManagementImageService imageService;

    public DescribeImagesHandler() {
        imageService = null;
    }

    @Resource
    public void setImageService(ManagementImageService anImageService) {
        imageService = anImageService;
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DescribeImagesResponseDocument describeImages(com.amazonaws.ec2.doc.x20081201.DescribeImagesDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DescribeImagesResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20090404)
    public com.amazonaws.ec2.doc.x20090404.DescribeImagesResponseDocument describeImages(com.amazonaws.ec2.doc.x20090404.DescribeImagesDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            DescribeImagesType describeImages = requestDocument.getDescribeImages();

            DescribeImagesResponseDocument resultDocument = DescribeImagesResponseDocument.Factory.newInstance();
            DescribeImagesResponseType addNewDescribeImagesResponse = resultDocument.addNewDescribeImagesResponse();
            DescribeImagesResponseInfoType addNewImagesSet = addNewDescribeImagesResponse.addNewImagesSet();

            DescribeImagesInfoType imagesSet = describeImages.getImagesSet();
            // TODO: process these?
            // DescribeImagesExecutableBySetType executableBySet = describeImages.getExecutableBySet();
            // DescribeImagesOwnersType ownersSet = describeImages.getOwnersSet();

            List<String> imageIds = null;
            if (null != imagesSet && null != imagesSet.getItemArray()) {
                imageIds = new ArrayList<String>();
                for (DescribeImagesItemType describeImagesItemType : imagesSet.getItemArray())
                    imageIds.add(describeImagesItemType.getImageId());
            }

            Set<Image> images = imageService.describeImages(getUserId(), imageIds);

            for (Image image : images) {
                DescribeImagesResponseItemType addNewItem = addNewImagesSet.addNewItem();
                addNewItem.setImageId(image.getImageId());
                addNewItem.setImageLocation(image.getManifestLocation());
                addNewItem.setImageOwnerId(image.getOwnerId());
                addNewItem.setKernelId(image.getKernelId());
                addNewItem.setArchitecture(image.getArchitecture());
                addNewItem.setImageState(image.getState().toString());
                addNewItem.setImageType(image.getMachineType().toString());
                addNewItem.setIsPublic(image.isPublic());
                addNewItem.setPlatform(image.getPlatform().toString());
                addNewItem.setRamdiskId(image.getRamdiskId());
            }
            addNewDescribeImagesResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (DescribeImagesResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
