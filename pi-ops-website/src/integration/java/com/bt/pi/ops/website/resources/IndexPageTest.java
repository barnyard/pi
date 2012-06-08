/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.resources;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import com.bt.pi.ops.website.IntegrationTestBase;

public class IndexPageTest extends IntegrationTestBase {
	private static final String RESOURCE_URI = BASE_URI + "index.html";

	@Test
	public void getIndexPageAsOpsUser() throws Exception {
		getIndexPageAsRole(opsUserHttpClient);
	}

	@Test
	public void getIndexPageAsMisUser() throws Exception {
		getIndexPageAsRole(misUserHttpClient);
	}

	@Test
	public void failToGetIndexPageForProvisioningUser() throws Exception {
		GetMethod req = new GetMethod(RESOURCE_URI);
		req.setRequestHeader("accept", MediaType.TEXT_HTML);

		// check the user images can be viewed
		assertEquals(200, provisioningUserHttpClient.executeMethod(req));
		assertContains("Not Authorized", req.getResponseBodyAsString());
	}

	private void getIndexPageAsRole(HttpClient httpClient) throws Exception {
		// setup
		GetMethod req = new GetMethod(RESOURCE_URI);
		req.setRequestHeader("accept", MediaType.TEXT_HTML);

		// check the user images can be viewed
		assertEquals(200, httpClient.executeMethod(req));
		assertContains("Welcome", req.getResponseBodyAsString());
	}
}
