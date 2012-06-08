/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import com.bt.pi.ops.website.IntegrationTestBase;

public class DhtRecordAuthorizationIntegrationTest extends IntegrationTestBase {
	private static final String RESOURCE_URI = BASE_URI + "dhtrecords";

	@Test
	public void getDhtRecordAsOpsUser() throws Exception {
		assertDhtRecordAsRole(opsUserHttpClient, "global/user/mrbarneygumble", "mrbarneygumble");
	}

	@Test
	public void failToGetDhtRecordAsMisUser() throws Exception {
		failToGetDhtRecordAsRole(misUserHttpClient, "global/user/mrbarneygumble");
	}

	@Test
	public void failToGetDhtRecordAsProvisioningUser() throws Exception {
		failToGetDhtRecordAsRole(provisioningUserHttpClient, "global/user/mrbarneygumble");
	}

	@Test
	public void getRegionsRecordAsOpsUser() throws Exception {
		assertDhtRecordAsRole(opsUserHttpClient, "regions", "legion region");
	}

	@Test
	public void getRegionsRecordAsMisUser() throws Exception {
		assertDhtRecordAsRole(misUserHttpClient, "regions", "legion region");
	}

	@Test
	public void failToGetRegionsRecordAsProvisioning() throws Exception {
		failToGetDhtRecordAsRole(provisioningUserHttpClient, "regions");
	}

	@Test
	public void getAvailabilityZonesRecordAsOpsUser() throws Exception {
		assertDhtRecordAsRole(opsUserHttpClient, "availabilityzones", "availabilityZones");
	}

	@Test
	public void getAvailabilityZonesRecordAsMisUser() throws Exception {
		assertDhtRecordAsRole(misUserHttpClient, "availabilityzones", "availabilityZones");
	}

	@Test
	public void failToGetAvailabilityZonesRecordAsProvisioning() throws Exception {
		failToGetDhtRecordAsRole(provisioningUserHttpClient, "availabilityzones");
	}

	@Test
	public void getInstanceTypesRecordAsOpsUser() throws Exception {
		assertDhtRecordAsRole(opsUserHttpClient, "instancetypes", "small");
	}

	@Test
	public void getInstanceTypesRecordAsMisUser() throws Exception {
		assertDhtRecordAsRole(misUserHttpClient, "instancetypes", "small");
	}

	@Test
	public void getInstanceTypesRecordAsProvisioning() throws Exception {
		assertDhtRecordAsRole(provisioningUserHttpClient, "instancetypes", "small");
	}

	@Test
	public void testImageInDifferentWays() throws Exception {
		String result1 = getDhtRecordAsRole(opsUserHttpClient, "global/img/" + KERNEL_ID);
		System.err.println(result1);

		String result2 = getDhtRecordAsRole(opsUserHttpClient, "scopes/global/img/" + KERNEL_ID);
		System.err.println(result2);

		assertEqualsAndNotNull(result1, result2);
	}

	private void assertEqualsAndNotNull(String result1, String result2) {
		assertNotNull(result1);
		assertNotNull(result2);
		assertFalse("null".equals(result1));
		assertFalse("null".equals(result2));
		assertEquals(result1, result2);
	}

	@Test
	public void testInstanceInDifferentWays() throws Exception {
		String result1 = getDhtRecordAsRole(opsUserHttpClient, "availabilityzones/" + AVAILABILITY_ZONE.replaceAll(" ", "%20") + "/inst/" + instanceId);
		System.err.println(result1);

		String result2 = getDhtRecordAsRole(opsUserHttpClient, "scopes/availability_zone/inst/" + instanceId);
		System.err.println(result2);

		assertEqualsAndNotNull(result1, result2);
	}

	@Test
	public void testSecurityGroupInDifferentWays() throws Exception {
		String id = USERNAME.replaceAll(" ", "").toLowerCase() + ":" + SECURITY_GROUP_NAME;

		String result1 = getDhtRecordAsRole(opsUserHttpClient, "regions/" + REGION.replaceAll(" ", "%20") + "/sg/" + id);
		System.err.println(result1);

		String result2 = getDhtRecordAsRole(opsUserHttpClient, "scopes/region/sg/" + id);
		System.err.println(result2);

		assertEqualsAndNotNull(result1, result2);
	}

	private void assertDhtRecordAsRole(HttpClient httpClient, String resource, String stringToMatch) throws Exception {
		assertContains(stringToMatch, getDhtRecordAsRole(httpClient, resource));
	}

	private String getDhtRecordAsRole(HttpClient httpClient, String resource) throws Exception {
		System.err.println(resource);
		GetMethod req = new GetMethod(RESOURCE_URI + "/" + resource + ".json");
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);
		assertEquals(200, httpClient.executeMethod(req));
		return req.getResponseBodyAsString();
	}

	private void failToGetDhtRecordAsRole(HttpClient httpClient, String resource) throws Exception {
		// setup
		GetMethod req = new GetMethod(RESOURCE_URI + "/" + resource + ".json");
		req.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		// check the user images can be viewed
		assertEquals(403, httpClient.executeMethod(req));
	}
}
