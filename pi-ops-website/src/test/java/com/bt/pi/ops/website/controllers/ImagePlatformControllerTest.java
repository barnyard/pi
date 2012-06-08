package com.bt.pi.ops.website.controllers;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.management.ImageSeeder;

@RunWith(MockitoJUnitRunner.class)
public class ImagePlatformControllerTest {
	@InjectMocks
	ImagePlatformController controller = new ImagePlatformController();

	@Mock
	private ImageSeeder imageSeeder;

	private String imageId;

	private String imagePlatform;

	@Test
	public void shouldUpdateImagePlatformByUsingImageSeeder() {
		// setup
		imageId = "pmi-000";
		imagePlatform = "windows";

		// act
		controller.changeImagePlatform(imageId, imagePlatform);

		// assert
		verify(imageSeeder).updateImagePlatform(imageId, imagePlatform);
	}
}
