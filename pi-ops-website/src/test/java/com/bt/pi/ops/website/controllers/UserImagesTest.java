/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.api.service.ManagementImageService;
import com.bt.pi.api.service.UserNotFoundException;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.ops.website.entities.ReadOnlyImage;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;

public class UserImagesTest {
	private static final String IMAGE_ID = "imageId";
	private static final String USERNAME = "username";
	private UserImages userImages;
	private ManagementImageService imageService;
	private Set<Image> imageList;
	private Image image;

	@SuppressWarnings("unchecked")
	@Before
	public void doBefore() {
		userImages = new UserImages();

		imageList = new HashSet<Image>();
		imageService = mock(ManagementImageService.class);
		image = mock(Image.class);

		when(imageService.describeImages(any(String.class), (List) isNull())).thenReturn(imageList);
		when(imageService.describeImages(any(String.class), anyListOf(String.class))).thenReturn(imageList);

		imageList.add(image);

		userImages.setImageService(imageService);
	}

	// tests for GET to /users/{username}/images

	@Test
	public void getAllShouldReturnAllImages() {
		// act
		List<ReadOnlyImage> result = userImages.getAllImages(USERNAME);

		// assert
		verify(imageService).describeImages(USERNAME, null);
		assertEquals(1, result.size());
		assertEquals(new ReadOnlyImage(image), result.get(0));
	}

	@SuppressWarnings("unchecked")
	@Test(expected = NotFoundException.class)
	public void getAllShould404IfUserDoesntExist() {
		// setup
		when(imageService.describeImages(any(String.class), (List) isNull())).thenThrow(new UserNotFoundException(""));

		// act
		userImages.getAllImages(USERNAME);
	}

	@Test
	public void gettingAllImagesAsHtmlShouldReturnAViewableWithTheRightTemplateAndModelLoaded() {
		// act
		Viewable viewable = userImages.getAllHtml(USERNAME);

		assertUserImagesViewable(viewable);
	}

	// tests for GET to /users/{username}/images/{imageId}

	@Test
	public void getImageShouldReturnTheImage() {
		// act
		ReadOnlyImage result = userImages.getImage(USERNAME, IMAGE_ID);

		// assert
		assertEquals(new ReadOnlyImage(image), result);
	}

	@Test(expected = NotFoundException.class)
	public void getShould404IfUserDoesntExist() {
		// setup
		when(imageService.describeImages(any(String.class), anyListOf(String.class))).thenThrow(new UserNotFoundException(""));

		// act
		userImages.getImage(USERNAME, IMAGE_ID);
	}

	@Test
	public void getImageAsHtmlShouldReturnAViewableWithTheTemplateAndModel() {
		// act
		Viewable viewable = userImages.getImageHtml(USERNAME, IMAGE_ID);

		// assert
		assertUserImagesViewable(viewable);
	}

	@SuppressWarnings("unchecked")
	private void assertUserImagesViewable(Viewable viewable) {
		assertNotNull(viewable);
		assertNotNull(viewable.getModel());
		final Map<String, Object> model = (Map<String, Object>) viewable.getModel();
		assertNotNull(model.get("images"));
		assertTrue(model.get("images") instanceof List);
		assertEquals(USERNAME, model.get("username"));
		assertEquals("user_images", viewable.getTemplateName());
	}

}
