/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.api.service.ImageServiceImpl;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.ops.website.IntegrationTestBase;
import com.bt.pi.ops.website.controllers.UserKernelsController;
import com.bt.pi.ops.website.controllers.UserRamdisksController;

public class UserKernelsAndRamdisksIntegrationTest extends IntegrationTestBase {
	private static final String DUMMY_KERNEL_ID = "pki-abc";
	private static final String DUMMY_RAMDISK_ID = "pri-def";
	private static final String DUMMY_MANIFEST_LOCATION = "mybucket/manifest.xml";
	private static final String KERNELS_URI = BASE_URI + "users/mrbarneygumble/kernels";
	private static final String RAMDISKS_URI = BASE_URI + "users/mrbarneygumble/ramdisks";
	private ImageServiceImpl originalImageService = null;

	@Before
	public void before() {
		ImageServiceImpl imageService = mock(ImageServiceImpl.class);
		when(imageService.registerImage("mrbarneygumble", DUMMY_MANIFEST_LOCATION, MachineType.KERNEL)).thenReturn(DUMMY_KERNEL_ID);
		when(imageService.registerImage("mrbarneygumble", DUMMY_MANIFEST_LOCATION, MachineType.RAMDISK)).thenReturn(DUMMY_RAMDISK_ID);
		when(imageService.deregisterImage("mrbarneygumble", DUMMY_KERNEL_ID, MachineType.KERNEL)).thenReturn(true);
		when(imageService.deregisterImage("mrbarneygumble", DUMMY_RAMDISK_ID, MachineType.RAMDISK)).thenReturn(true);

		originalImageService = classPathXmlApplicationContext.getBean(ImageServiceImpl.class);

		setImageService(imageService);
	}

	@After
	public void after() {
		setImageService(originalImageService);
	}

	private void setImageService(ImageServiceImpl imageService) {
		UserKernelsController userKernelsController = (UserKernelsController) classPathXmlApplicationContext.getBean("userKernelsController");
		userKernelsController.setImageService(imageService);

		UserRamdisksController userRamdisksController = (UserRamdisksController) classPathXmlApplicationContext.getBean("userRamdisksController");
		userRamdisksController.setImageService(imageService);
	}

	@Test
	public void postUserKernelAsOpsUser() throws HttpException, IOException {
		// setup
		PostMethod req = new PostMethod(KERNELS_URI);
		req.setRequestEntity(new StringRequestEntity("image_manifest_location=" + DUMMY_MANIFEST_LOCATION, MediaType.APPLICATION_FORM_URLENCODED, "UTF-8"));
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(200, opsUserHttpClient.executeMethod(req));
		assertContains(DUMMY_KERNEL_ID, req.getResponseBodyAsString());
	}

	@Test
	public void failToPostUserKernelAsMisUser() throws HttpException, IOException {
		// setup
		PostMethod req = new PostMethod(KERNELS_URI);
		req.setRequestEntity(new StringRequestEntity("image_manifest_location=" + DUMMY_MANIFEST_LOCATION, MediaType.APPLICATION_FORM_URLENCODED, "UTF-8"));
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(405, misUserHttpClient.executeMethod(req));
	}

	@Test
	public void failToPostUserKernelAsProvisioningUser() throws HttpException, IOException {
		// setup
		PostMethod req = new PostMethod(KERNELS_URI);
		req.setRequestEntity(new StringRequestEntity("image_manifest_location=" + DUMMY_MANIFEST_LOCATION, MediaType.APPLICATION_FORM_URLENCODED, "UTF-8"));
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(405, provisioningUserHttpClient.executeMethod(req));
	}

	@Test
	public void deleteUserKernelAsOpsUser() throws HttpException, IOException {
		// setup
		DeleteMethod req = new DeleteMethod(KERNELS_URI + "/" + DUMMY_KERNEL_ID);
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(200, opsUserHttpClient.executeMethod(req));
		assertContains("ok", req.getResponseBodyAsString());
	}

	@Test
	public void failToDeleteUserKernelAsMisUser() throws HttpException, IOException {
		// setup
		DeleteMethod req = new DeleteMethod(KERNELS_URI + "/kmi-1234");
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(403, misUserHttpClient.executeMethod(req));
	}

	@Test
	public void failToDeleteUserKernelAsProvisioningUser() throws HttpException, IOException {
		// setup
		DeleteMethod req = new DeleteMethod(KERNELS_URI + "/kmi-1234");
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(403, provisioningUserHttpClient.executeMethod(req));
	}

	@Test
	public void postUserRamdiskAsOpsUser() throws HttpException, IOException {
		// setup
		PostMethod req = new PostMethod(RAMDISKS_URI);
		req.setRequestEntity(new StringRequestEntity("image_manifest_location=" + DUMMY_MANIFEST_LOCATION, MediaType.APPLICATION_FORM_URLENCODED, "UTF-8"));
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(200, opsUserHttpClient.executeMethod(req));
		assertContains(DUMMY_RAMDISK_ID, req.getResponseBodyAsString());
	}

	@Test
	public void failToPostUserRamdiskAsMisUser() throws HttpException, IOException {
		// setup
		PostMethod req = new PostMethod(RAMDISKS_URI);
		req.setRequestEntity(new StringRequestEntity("image_manifest_location=" + DUMMY_MANIFEST_LOCATION, MediaType.APPLICATION_FORM_URLENCODED, "UTF-8"));
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(405, misUserHttpClient.executeMethod(req));
	}

	@Test
	public void failToPostUserRamdiskAsProvisioningUser() throws HttpException, IOException {
		// setup
		PostMethod req = new PostMethod(RAMDISKS_URI);
		req.setRequestEntity(new StringRequestEntity("image_manifest_location=" + DUMMY_MANIFEST_LOCATION, MediaType.APPLICATION_FORM_URLENCODED, "UTF-8"));
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(405, provisioningUserHttpClient.executeMethod(req));
	}

	@Test
	public void deleteUserRamdiskAsOpsUser() throws HttpException, IOException {
		// setup
		DeleteMethod req = new DeleteMethod(RAMDISKS_URI + "/" + DUMMY_RAMDISK_ID);
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(200, opsUserHttpClient.executeMethod(req));
		assertContains("ok", req.getResponseBodyAsString());
	}

	@Test
	public void failToDeleteUserRamdiskAsMisUser() throws HttpException, IOException {
		// setup
		DeleteMethod req = new DeleteMethod(RAMDISKS_URI + "/rmi-1234");
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(403, misUserHttpClient.executeMethod(req));
	}

	@Test
	public void failToDeleteUserRamdiskAsProvisioningUser() throws HttpException, IOException {
		// setup
		DeleteMethod req = new DeleteMethod(RAMDISKS_URI + "/rmi-1234");
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// assert
		assertEquals(403, provisioningUserHttpClient.executeMethod(req));
	}
}
