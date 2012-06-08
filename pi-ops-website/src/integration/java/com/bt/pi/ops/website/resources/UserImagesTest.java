/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.resources;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import com.bt.pi.ops.website.IntegrationTestBase;

public class UserImagesTest extends IntegrationTestBase {
	private static final String RESOURCE_URI = BASE_URI + "users/mrbarneygumble/images";

	@Test
	public void getUsersImagesAsOpsUser() throws HttpException, IOException {
		getUsersImagesAsUser(opsUserHttpClient);
	}

	@Test
	public void getUsersImagesAsMisUser() throws HttpException, IOException {
		getUsersImagesAsUser(misUserHttpClient);
	}

	@Test
	public void getUsersImagesAsProvisioningUser() throws HttpException, IOException {
		getUsersImagesAsUser(provisioningUserHttpClient);
	}

	private void getUsersImagesAsUser(HttpClient httpClient) throws HttpException, IOException {
		// setup
		GetMethod getUserImages = new GetMethod(RESOURCE_URI);
		getUserImages.setRequestHeader("accept", MediaType.TEXT_HTML);

		// check the user images can be viewed
		assertEquals(200, httpClient.executeMethod(getUserImages));
		assertContains("Image Id:</td><td>kmi-1234", getUserImages.getResponseBodyAsString());
		assertContains("Image Id:</td><td>rmi-1234", getUserImages.getResponseBodyAsString());

		// check the users images can be accessed using json
		getUserImages.setRequestHeader("accept", MediaType.APPLICATION_JSON);
		assertEquals(200, httpClient.executeMethod(getUserImages));
		assertContains("\"imageId\":\"kmi-1234\"", getUserImages.getResponseBodyAsString());
		assertContains("\"imageId\":\"rmi-1234\"", getUserImages.getResponseBodyAsString());

		// check the users images can be accessed using xml
		getUserImages.setRequestHeader("accept", MediaType.APPLICATION_XML);
		assertEquals(200, httpClient.executeMethod(getUserImages));
		assertContains("<imageId>kmi-1234</imageId>", getUserImages.getResponseBodyAsString());
		assertContains("<imageId>rmi-1234</imageId>", getUserImages.getResponseBodyAsString());
	}

	@Test
	public void getUserImageAsOpsUser() throws Exception {
		getUserImageAsUser(opsUserHttpClient);
	}

	@Test
	public void getUserImageAsMisUser() throws Exception {
		getUserImageAsUser(misUserHttpClient);
	}

	@Test
	public void getUserImageAsProvisioningUser() throws Exception {
		getUserImageAsUser(provisioningUserHttpClient);
	}

	private void getUserImageAsUser(HttpClient httpClient) throws Exception {
		// setup
		GetMethod getUserImages = new GetMethod(RESOURCE_URI + "/kmi-1234");
		getUserImages.setRequestHeader("accept", MediaType.TEXT_HTML);

		// check the user images can be viewed
		assertEquals(200, httpClient.executeMethod(getUserImages));
		final String responseBody = getUserImages.getResponseBodyAsString();
		assertContains("Image Id:</td><td>kmi-1234", responseBody);
		// check there is only one image
		assertEquals(responseBody.indexOf("Image Id:"), responseBody.lastIndexOf("Image Id:"));
	}

}
