/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.images.platform.ImagePlatform;

public class ReadOnlyImageTest {
	private static final String RAMDISK_ID = "ramdiskId";
	private static final String OWNER_ID = "ownerId";
	private static final String MANIFEST = "manifest";
	private static final String KERNEL_ID = "kernelId";
	private static final String ARCH = "arch";
	private static final String IMAGE_ID = "imageId";
	private final Image image = mock(Image.class);
	private final ReadOnlyImage roImage = new ReadOnlyImage(image);

	@Test
	public void equalsShouldBeTrueWhenIsSameObject() {
		assertTrue(roImage.equals(roImage));
	}

	@Test
	public void equalsShouldBeTrueWhenContainsSameObject() {
		assertTrue(roImage.equals(image));
	}

	@Test
	public void equalsShouldBeTrueWhenSetWithSameObject() {
		// setup
		roImage.setimage(image);

		// assert
		assertTrue(roImage.equals(image));
	}

	@Test
	public void gettersShouldDelegate() {
		// setup
		when(image.getImageId()).thenReturn(IMAGE_ID);
		when(image.getArchitecture()).thenReturn(ARCH);
		when(image.getKernelId()).thenReturn(KERNEL_ID);
		when(image.getMachineType()).thenReturn(MachineType.MACHINE);
		when(image.getManifestLocation()).thenReturn(MANIFEST);
		when(image.getOwnerId()).thenReturn(OWNER_ID);
		when(image.getPlatform()).thenReturn(ImagePlatform.linux);
		when(image.getRamdiskId()).thenReturn(RAMDISK_ID);
		when(image.getState()).thenReturn(ImageState.AVAILABLE);
		when(image.isPublic()).thenReturn(true);

		// act and assert
		assertEquals(image, roImage.getImage());
		assertEquals(IMAGE_ID, roImage.getImageId());
		assertEquals(ARCH, roImage.getArchitecture());
		assertEquals(KERNEL_ID, roImage.getKernelId());
		assertEquals(MachineType.MACHINE, roImage.getMachineType());
		assertEquals(MANIFEST, roImage.getManifestLocation());
		assertEquals(OWNER_ID, roImage.getOwnerId());
		assertEquals(ImagePlatform.linux, roImage.getPlatform());
		assertEquals(RAMDISK_ID, roImage.getRamdiskId());
		assertEquals(ImageState.AVAILABLE, roImage.getState());
		assertTrue(roImage.isPublic());

		assertNotNull(roImage.toString());
		assertNotNull(roImage.hashCode());
	}
}
