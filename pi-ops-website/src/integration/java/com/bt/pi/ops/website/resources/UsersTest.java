package com.bt.pi.ops.website.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.UserServiceHelper;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.util.DigestUtils;
import com.bt.pi.ops.website.IntegrationTestBase;

public class UsersTest extends IntegrationTestBase {
	private static final String RESOURCE_URI = BASE_URI + "users";
	private static final String USER_URI = RESOURCE_URI + "/mayordiamondjoequimby";
	private static final String PUT_POST_CONTENT = "username=mayordiamondjoequimby&realname=Mayor%20Diamond%20Joe%20Quimby&email=mayor.diamond.joe.quimby@somewhere.com&enabled=checkbox";
	private static final String PUT_BAD_POST_CONTENT = "username=mayordiamondjoequimby";

	private UserServiceHelper userServiceHelper;
	private DigestUtils digestUtils;

	@Before
	public void setup() {
		digestUtils = new DigestUtils();
		userServiceHelper = new UserServiceHelper();
		userServiceHelper.setDigestUtils(digestUtils);
		stubMailSender.reset();
	}

	@Test
	public void shouldCrudUserAsOpsUser() throws Exception {
		happyPathPostGetPutGetByAccessKeyDeleteUserAsRole(opsUserHttpClient);
	}

	@Test
	public void shouldCrudUserAsProvisioningUser() throws Exception {
		happyPathPostGetPutGetByAccessKeyDeleteUserAsRole(provisioningUserHttpClient);
	}

	@Test
	public void failToAccessUserAsMisUser() throws Exception {
		GetMethod req = new GetMethod(USER_URI);
		req.setRequestHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
		assertEquals(403, misUserHttpClient.executeMethod(req));
	}

	private void happyPathPostGetPutGetByAccessKeyDeleteUserAsRole(HttpClient httpClient) throws HttpException, IOException {
		// setup
		String accessKey = userServiceHelper.generateAccessKey("mayordiamondjoequimby");
		String accessKeyUri = RESOURCE_URI + "/accesskeys/" + accessKey;

		GetMethod getUser = new GetMethod(RESOURCE_URI + "/view" + "/mayordiamondjoequimby");
		getUser.setRequestHeader("accept", MediaType.TEXT_HTML);
		GetMethod getUserByAccessKey = new GetMethod(accessKeyUri);
		getUserByAccessKey.setRequestHeader("accept", MediaType.TEXT_HTML);
		PostMethod postUser = new PostMethod(RESOURCE_URI);
		postUser.setRequestEntity(new StringRequestEntity(PUT_POST_CONTENT, MediaType.APPLICATION_FORM_URLENCODED, "UTF-8"));
		PostMethod postUserCert = new PostMethod(USER_URI + "/certificate");
		PostMethod postUserCertWithExtension = new PostMethod(USER_URI + "/pi-certs.zip");
		DeleteMethod deleteUser = new DeleteMethod(USER_URI);
		PutMethod putUser = new PutMethod(USER_URI);
		putUser.setRequestEntity(new StringRequestEntity(PUT_POST_CONTENT.replace("@somewhere.com", "@springfield.com"), MediaType.APPLICATION_FORM_URLENCODED, "UTF-8"));

		// make sure the user doesn't exist
		assertEquals(404, httpClient.executeMethod(getUser));

		// create the user
		assertEquals(201, httpClient.executeMethod(postUser));
		assertContains("users/mayordiamondjoequimby", postUser.getResponseHeaders("Location")[0].getValue());

		// check the user was created and can be viewed
		assertEquals(200, httpClient.executeMethod(getUser));
		assertContains("Real name:</td><td>Mayor Diamond Joe Quimby", getUser.getResponseBodyAsString());
		assertContains("Username:</td><td>mayordiamondjoequimby", getUser.getResponseBodyAsString());
		assertContains("Email address:</td><td>mayor.diamond.joe.quimby@somewhere.com", getUser.getResponseBodyAsString());

		// check the user can be accessed using json
		getUser = new GetMethod(USER_URI);
		getUser.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		assertEquals(200, httpClient.executeMethod(getUser));
		assertContains("\"realName\":\"Mayor Diamond Joe Quimby\"", getUser.getResponseBodyAsString());
		assertContains("\"username\":\"mayordiamondjoequimby\"", getUser.getResponseBodyAsString());

		// check the user can be accessed using xml
		getUser.setRequestHeader("accept", MediaType.APPLICATION_XML);
		assertEquals(200, httpClient.executeMethod(getUser));
		assertContains("<realName>Mayor Diamond Joe Quimby</realName>", getUser.getResponseBodyAsString());
		assertContains("<username>mayordiamondjoequimby</username", getUser.getResponseBodyAsString());

		// check the user's certificate can be downloaded
		assertEquals(200, httpClient.executeMethod(postUserCert));
		assertEquals("application/zip", postUserCert.getResponseHeaders("Content-Type")[0].getValue());

		// check the user's certificate can be downloaded from file-like uri
		assertEquals(200, httpClient.executeMethod(postUserCertWithExtension));
		assertEquals("application/zip", postUserCertWithExtension.getResponseHeaders("Content-Type")[0].getValue());

		// make a change to the user
		assertEquals(200, httpClient.executeMethod(putUser));

		// check that the change worked
		getUserByAccessKey.setRequestHeader("accept", MediaType.TEXT_HTML);
		assertEquals(200, httpClient.executeMethod(getUserByAccessKey));
		assertContains("Real name:</td><td>Mayor Diamond Joe Quimby", getUserByAccessKey.getResponseBodyAsString());
		assertContains("Username:</td><td>mayordiamondjoequimby", getUserByAccessKey.getResponseBodyAsString());
		assertContains("Email address:</td><td>mayor.diamond.joe.quimby@springfield.com", getUserByAccessKey.getResponseBodyAsString());

		// delete the user
		assertEquals(204, httpClient.executeMethod(deleteUser));

		// check the user was deleted
		assertEquals(404, httpClient.executeMethod(getUser));
	}

	@Test
	public void postWithMissingDetailsShouldReturn400() throws HttpException, IOException {
		// setup
		PostMethod postUser = new PostMethod(RESOURCE_URI);
		postUser.setRequestEntity(new StringRequestEntity(PUT_BAD_POST_CONTENT, MediaType.APPLICATION_FORM_URLENCODED, "UTF-8"));

		// act and assert
		assertEquals(400, opsUserHttpClient.executeMethod(postUser));
	}

	@Test
	public void shouldGetUserValidationPageAddress() throws Exception {
		// setup
		String target = USER_URI + "/instancevalidation";
		HttpMethod getMethod = new GetMethod(target);
		String expectedResult = String.format(
				"<a href=\"https://localhost:8443/users/mayordiamondjoequimby/instancevalidation/%1$s\">https://localhost:8443/users/mayordiamondjoequimby/instancevalidation/%1$s</a>\n", piIdBuilder
						.getPId(User.getUrl("mayordiamondjoequimby")).toStringFull());

		// act
		opsUserHttpClient.executeMethod(getMethod);
		String actualResult = getMethod.getResponseBodyAsString();

		// assert
		assertEquals(expectedResult, actualResult);
	}

	@Test
	public void shouldNotGiveAnonymousAccessToInstanceValidationPageAddress() throws Exception {
		// setup
		String username = USERNAME.replace(" ", "").toLowerCase();
		String target = String.format("%s/%s/instancevalidation", RESOURCE_URI, username);
		HttpMethod getMethod = new GetMethod(target);

		// act
		int result = anonymousHttpClient.executeMethod(getMethod);

		// assert
		assertEquals(200, result);
		// expect to get re-directed to login page
		assertTrue(getMethod.getResponseBodyAsString().contains("The Pi Management Super Cool Tool - Log In"));
	}

	@Test
	public void shouldNotGiveAnonymousAccessToInstanceValidationEmail() throws Exception {
		// setup
		String username = USERNAME.replace(" ", "").toLowerCase();
		String target = String.format("%s/%s/instancevalidation", RESOURCE_URI, username);
		HttpMethod postMethod = new PostMethod(target);

		// act
		int result = anonymousHttpClient.executeMethod(postMethod);

		// assert
		assertEquals(302, result);
		// expect to get re-directed to login page
		assertTrue(hasLocation(postMethod, BASE_URI + "login.html"));
		Thread.sleep(500);
		assertNull(stubMailSender.getLastMessage());
	}

	private boolean hasLocation(HttpMethod method, String target) {
		for (Header header : method.getResponseHeaders())
			if ("Location".endsWith(header.getName()))
				if (header.getValue().startsWith(target))
					return true;
		return false;
	}

	@Test
	public void shouldSendInstanceValidationEmail() throws Exception {
		// setup
		String username = USERNAME.replace(" ", "").toLowerCase();
		String target = String.format("%s/%s/instancevalidation", RESOURCE_URI, username);
		HttpMethod postMethod = new PostMethod(target);

		// act
		int result = opsUserHttpClient.executeMethod(postMethod);

		// assert
		assertEquals(200, result);
		Thread.sleep(500);
		String mailMessage = stubMailSender.getLastMessage();
		assertNotNull(mailMessage);
		System.err.println(mailMessage);
		assertTrue(mailMessage.contains("<h1>PI Instance validation</h1>"));
		assertTrue(mailMessage.contains("click</a> to validate your PI instances"));
	}

	@Test
	public void shouldGiveAnonymousAccessToInstanceValidationPage() throws Exception {
		// setup
		String username = USERNAME.replace(" ", "").toLowerCase();
		String target = String.format("%s/%s/instancevalidation/%s", RESOURCE_URI, username, piIdBuilder.getPId(User.getUrl(username)).toStringFull());
		HttpMethod getMethod = new GetMethod(target);

		// act
		int result = anonymousHttpClient.executeMethod(getMethod);

		// assert
		assertEquals(200, result);
		assertTrue(getMethod.getResponseBodyAsString().contains("<head><title>User Instance Validation</title></head>"));
	}
}
