/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.List;
import java.util.Set;

import com.bt.pi.app.common.entities.Image;

/**
 * Interface for all image relatied API calls - Deregister Image - Describe Image Attribute - Describe Images - Modify
 * Image Attribute - Register Image - Reset Image Attribute
 * 
 */
public interface ImageService {

    boolean deregisterImage(String ownerId, String imageId);

    String registerImage(String ownerId, String imageManifestLocation);

    Set<Image> describeImages(String ownerId, List<String> imageIds);

}
