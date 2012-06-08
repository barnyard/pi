/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.nia.koala.robustness.commands;


public class ImageDetails {
	private String imageId;
	private String imageBucket;
	private String imageFileName;
	private String imageDirectory;
	private String bundledDirectory;
	private ImageType imageType;

	public static enum ImageType {
		KERNEL, RAMDISK, MACHINE
	}

	public ImageDetails() {
	}

	public ImageDetails(String imageBucket, String imageFileName, String imageDirectory, ImageType imageType) {
		this.imageBucket = imageBucket;
		this.imageFileName = imageFileName;
		this.imageDirectory = imageDirectory;
		this.imageType = imageType;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageBucket(String imageBucket) {
		this.imageBucket = imageBucket;
	}

	public String getImageBucket() {
		return imageBucket;
	}

	public void setImageFileName(String imageFileName) {
		this.imageFileName = imageFileName;
	}

	public String getImageFileName() {
		return imageFileName;
	}

	public void setImageDirectory(String imageDirectory) {
		this.imageDirectory = imageDirectory;
	}

	public String getImageDirectory() {
		return imageDirectory;
	}

	public String getManifestLocationOnServer() {
		return getImageBucket() + "/" + getImageFileName() + ".manifest.xml";
	}

	public String getLocalImagePath() {
		return getImageDirectory() + getImageFileName();
	}

	public void setBundledDirectory(String bundledDirectory) {
		this.bundledDirectory = (bundledDirectory.endsWith("/") ? bundledDirectory.substring(0, bundledDirectory.length() - 2) : bundledDirectory);
	}

	public String getBundledDirectory() {
		return bundledDirectory;
	}

	public String getLocalManifestPath() {
		return getBundledDirectory() + "/" + getImageFileName() + ".manifest.xml";
	}

	public void setImageType(ImageType imageType) {
		this.imageType = imageType;
	}

	public ImageType getImageType() {
		return imageType;
	}
}
