package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import rice.p2p.commonapi.Id;

import com.bt.pi.api.service.CertificateGenerationException;
import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.api.service.UserNotFoundException;
import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.watchers.UsersInstanceValidationWatcher;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.ops.website.entities.ReadOnlyUser;
import com.bt.pi.ops.website.exception.CannotCreateException;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UsersControllerTest {
	private static final String TRUE = "checkbox";
	private static final String EXTERNAL_REF = "externalRef";
	private final static String USERNAME = "username";
	private final static String REALNAME = "realname";
	private final static String EMAIL = "email";
	private final static String ACCESS_KEY = "accessKey";
	private static final int MAX_INSTANCES = 21;
	private static final int MAX_CORES = 32;
	private static final String INSTANCE_ID = "i-123";
	private final byte[] certificate = { 0, 1, 2, 3, 4, 5 };
	private String userName1 = "adrian";
	private String userName2 = "dan";
	private String url1 = ResourceSchemes.USER + userName1;
	private String url2 = ResourceSchemes.USER + userName2;
	private ReadOnlyUser readOnlyUser;
	@Mock
	private User user;
	@Mock
	private Id pastryId1;
	@Mock
	private Id pastryId2;
	@Mock
	private User user1;
	@Mock
	private User user2;
	@Mock
	private Set<String> setOfStrings;
	@Mock
	private KoalaIdFactory koalaIdFactory;
	@Mock
	private DhtClientFactory dhtClientFactory;
	@Mock
	private DhtReader dhtReader;
	@Mock
	private ThreadPoolTaskExecutor taskExecutor;
	@Mock
	private UserManagementService userManagementService;
	private String[] arrayOfStrings = new String[] { INSTANCE_ID };
	@InjectMocks
	private UsersController usersController = new UsersController();
	@Mock
	private PiIdBuilder piIdBuilder;
	@Mock
	private UsersInstanceValidationWatcher usersInstanceValidationWatcher;

	@Before
	public void doBefore() throws Exception {
		readOnlyUser = new ReadOnlyUser(user);

		when(userManagementService.addUser(any(User.class))).thenReturn(true);
		when(userManagementService.getUser(USERNAME)).thenReturn(user);
		when(userManagementService.getUserByApiAccessKey(ACCESS_KEY)).thenReturn(user);

		when(user.getImageIds()).thenReturn(setOfStrings);
		when(user.getInstanceIds()).thenReturn(arrayOfStrings);
		when(user.getSecurityGroupIds()).thenReturn(setOfStrings);
		when(user.getVolumeIds()).thenReturn(setOfStrings);
		when(user.getCertificate()).thenReturn(certificate);
		when(user.isEnabled()).thenReturn(true);
		when(user.getMaxInstances()).thenReturn(MAX_INSTANCES);
		when(user.getMaxCores()).thenReturn(MAX_CORES);

		when(koalaIdFactory.buildId(url1)).thenReturn(pastryId1);
		when(koalaIdFactory.buildId(url2)).thenReturn(pastryId2);
		when(dhtClientFactory.createReader()).thenReturn(dhtReader);
		when(user1.getUsername()).thenReturn(userName1);
		when(user1.getMaxInstances()).thenReturn(MAX_INSTANCES);
		when(user2.getUsername()).thenReturn(userName2);
		when(user2.getMaxInstances()).thenReturn(MAX_INSTANCES);

		EhCacheFactoryBean ehCacheFactoryBean = new EhCacheFactoryBean();
		ehCacheFactoryBean.setCacheName("unittest");
		ehCacheFactoryBean.afterPropertiesSet();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Runnable r = (Runnable) invocation.getArguments()[0];
				return new Thread(r);
			}
		}).when(taskExecutor).createThread(isA(Runnable.class));

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				when(user.isEnabled()).thenReturn((Boolean) invocation.getArguments()[1]);
				return null;
			}

		}).when(userManagementService).setUserEnabledFlag(eq(USERNAME), isA(Boolean.class));
	}

	// tests for POST on

	@Test
	public void postUserShouldCreateAUser() {
		// act
		Viewable viewable = (Viewable) usersController.postUser(USERNAME, REALNAME, EMAIL, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES)).getEntity();

		// assert
		assertNotNull(viewable);
		assertNotNull(viewable.getModel());
		Map model = (Map<String, Object>) viewable.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		assertEquals(INSTANCE_ID, ((Set<String>) model.get("instanceIds")).iterator().next());
		assertEquals(setOfStrings, model.get("imageIds"));
		assertEquals(setOfStrings, model.get("securityGroupIds"));
		assertEquals(setOfStrings, model.get("volumeIds"));
		assertEquals(MAX_INSTANCES, model.get("maxInstances"));
		assertEquals(MAX_CORES, model.get("maxCores"));
		assertEquals("single_user", viewable.getTemplateName());
	}

	@Test
	public void postUserShouldNotSetUnchangedMaxCores() {
		// act
		usersController.postUser(USERNAME, REALNAME, EMAIL, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), "UNCHANGED").getEntity();

		// assert
		verify(this.userManagementService).addUser(argThat(new ArgumentMatcher<User>() {
			@Override
			public boolean matches(Object argument) {
				if (!(argument instanceof User))
					return false;
				User user = (User) argument;
				return user.getMaxCores() == 8;
			}
		}));
	}

	@Test
	public void postUserShouldResetBlankMaxCores() {
		// act
		usersController.postUser(USERNAME, REALNAME, EMAIL, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), "").getEntity();

		// assert
		verify(this.userManagementService).addUser(argThat(new ArgumentMatcher<User>() {
			@Override
			public boolean matches(Object argument) {
				if (!(argument instanceof User))
					return false;
				User user = (User) argument;
				return user.getMaxCores() == 8;
			}
		}));
	}

	@Test(expected = IllegalArgumentException.class)
	public void postUserShouldFailForBadEnabledValue() {
		// act
		usersController.postUser(USERNAME, REALNAME, EMAIL, "bad", EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES));
	}

	@Test(expected = IllegalArgumentException.class)
	public void postUserShouldFailForBadMaxInstances() {
		// act
		try {
			usersController.postUser(USERNAME, REALNAME, EMAIL, "true", EXTERNAL_REF, "blogs", "1");
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			assertEquals("maxInstances must be a valid number greater than -1", e.getMessage());
			throw e;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void postUserShouldFailForBadMaxCores() {
		// act
		try {
			usersController.postUser(USERNAME, REALNAME, EMAIL, "true", EXTERNAL_REF, "2", "bloggs");
		} catch (IllegalArgumentException e) {
			assertEquals("maxCores must be a valid number greater than -1", e.getMessage());
			throw e;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void postUserShouldFailForBadMaxInstancesNegative() {
		// act
		try {
			usersController.postUser(USERNAME, REALNAME, EMAIL, "true", EXTERNAL_REF, "-24", "32");
		} catch (IllegalArgumentException e) {
			assertEquals("maxInstances must be a valid number greater than -1", e.getMessage());
			throw e;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void postUserShouldFailForBadMaxCoresNegative() {
		// act
		try {
			usersController.postUser(USERNAME, REALNAME, EMAIL, "true", EXTERNAL_REF, "24", "-32");
		} catch (IllegalArgumentException e) {
			assertEquals("maxCores must be a valid number greater than -1", e.getMessage());
			throw e;
		}
	}

	@Test
	public void postUserShouldCreateADisabledUser() {
		// act
		Viewable viewable = (Viewable) usersController.postUser(USERNAME, REALNAME, EMAIL, "", EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES)).getEntity();

		// assert
		assertNotNull(viewable);
		assertNotNull(viewable.getModel());
		Map model = (Map<String, Object>) viewable.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		assertEquals(INSTANCE_ID, ((Set<String>) model.get("instanceIds")).iterator().next());
		assertEquals(setOfStrings, model.get("imageIds"));
		assertEquals(setOfStrings, model.get("securityGroupIds"));
		assertEquals(setOfStrings, model.get("volumeIds"));
		assertEquals(MAX_INSTANCES, model.get("maxInstances"));
		assertEquals("single_user", viewable.getTemplateName());
	}

	@Test(expected = CannotCreateException.class)
	public void postUserShould500IfUnableToCreateUser() {
		// setup
		when(userManagementService.addUser(any(User.class))).thenReturn(false);

		// act
		usersController.postUser(USERNAME, REALNAME, EMAIL, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES));
	}

	@Test(expected = IllegalArgumentException.class)
	public void postUserShould400IfUsernameMissing() {
		// act
		usersController.postUser(null, REALNAME, EMAIL, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES));
	}

	@Test(expected = IllegalArgumentException.class)
	public void postUserShould400IfRealnameMissing() {
		// act
		usersController.postUser(USERNAME, null, EMAIL, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES));
	}

	@Test(expected = IllegalArgumentException.class)
	public void postUserShould400IfEmailMissing() {
		// act
		usersController.postUser(USERNAME, REALNAME, null, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES));
	}

	@Test
	public void postUserShouldDefaultUserDisablesWhenNoEnabledFlagSet() {
		// act
		Viewable viewable = (Viewable) usersController.postUser(USERNAME, REALNAME, EMAIL, null, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES)).getEntity();

		// assert
		assertNotNull(viewable);
		assertNotNull(viewable.getModel());
		Map model = (Map<String, Object>) viewable.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		assertEquals(INSTANCE_ID, ((Set<String>) model.get("instanceIds")).iterator().next());
		assertEquals(setOfStrings, model.get("imageIds"));
		assertEquals(setOfStrings, model.get("securityGroupIds"));
		assertEquals(setOfStrings, model.get("volumeIds"));
		assertEquals(MAX_INSTANCES, model.get("maxInstances"));
		assertEquals("single_user", viewable.getTemplateName());
	}

	@Test
	public void postUserShouldStillWorkWhenNoExternalRefProvided() {
		// act
		Viewable viewable = (Viewable) usersController.postUser(USERNAME, REALNAME, EMAIL, TRUE, null, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES)).getEntity();

		// assert
		assertNotNull(viewable);
		assertNotNull(viewable.getModel());
		Map model = (Map<String, Object>) viewable.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		assertEquals(INSTANCE_ID, ((Set<String>) model.get("instanceIds")).iterator().next());
		assertEquals(setOfStrings, model.get("imageIds"));
		assertEquals(setOfStrings, model.get("securityGroupIds"));
		assertEquals(setOfStrings, model.get("volumeIds"));
		assertEquals(MAX_INSTANCES, model.get("maxInstances"));
		assertEquals("single_user", viewable.getTemplateName());
	}

	// Tests for GET specific users

	@Test
	public void gettingUserShouldReturnTheUser() {
		// act and assert
		assertEquals(readOnlyUser, usersController.getUser(USERNAME));
	}

	@Test(expected = IllegalArgumentException.class)
	public void gettingUserWithNoUsernameShouldNotWork() {
		// act and assert
		usersController.getUser(null);
	}

	@Test(expected = NotFoundException.class)
	public void gettingUserThatDoesntExistShould404() {
		// setup
		when(userManagementService.getUser(any(String.class))).thenThrow(new UserNotFoundException(""));

		// act
		usersController.getUser(USERNAME);
	}

	@Test(expected = NotFoundException.class)
	public void gettingUserThatIsDeletedShould404() {
		// setup
		when(user.isDeleted()).thenReturn(true);

		// act
		usersController.getUser(USERNAME);
	}

	@Test
	public void gettingUserAsHtmlShouldReturnAViewableWithTheRightTemplateAndModelPopulated() {
		// act
		Viewable viewable = usersController.getUserHtml(USERNAME);

		// assert
		assertNotNull(viewable);
		assertNotNull(viewable.getModel());
		Map model = (Map<String, Object>) viewable.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		assertEquals(INSTANCE_ID, ((Set<String>) model.get("instanceIds")).iterator().next());
		assertEquals(setOfStrings, model.get("imageIds"));
		assertEquals(setOfStrings, model.get("securityGroupIds"));
		assertEquals(setOfStrings, model.get("volumeIds"));
		assertEquals(MAX_INSTANCES, model.get("maxInstances"));
		assertEquals("single_user", viewable.getTemplateName());
	}

	// Tests for Delete

	@Test
	public void deletingAUserShouldDeleteTheUser() {
		// act
		usersController.deleteUser(USERNAME);

		//
		verify(userManagementService).deleteUser(eq(USERNAME));
	}

	@Test
	public void deletingAUserShouldReturnSuccessNoContent() {
		// act and assert
		assertEquals(HttpStatus.NO_CONTENT.value(), usersController.deleteUser(USERNAME).getStatus());
	}

	@Test(expected = IllegalArgumentException.class)
	public void deletingAUserWithoutTheUsernameShouldFail() {
		// act
		usersController.deleteUser(null);
	}

	// Tests for GET of user's certificate

	@Test
	public void gettingUsersCertificateShouldReturnThePostForm() {
		// act
		Viewable viewable = usersController.getUserCertificate(USERNAME);

		// assert
		assertEquals(USERNAME, ((Map) viewable.getModel()).get("username"));
		assertEquals("post_user_cert", viewable.getTemplateName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void gettingUsersCertificateWithoutAUserShouldFail() {
		// act
		usersController.getUserCertificate(null);
	}

	// Tests for POST to user's certificate

	@Test
	public void postingUsersCertificateShouldCreateAndReturnANewCertificate() {
		// setup
		when(userManagementService.updateUserCertificate(eq(USERNAME))).thenReturn(certificate);

		// act
		Response response = usersController.postUserCertificate(USERNAME);

		// assert
		verify(userManagementService).updateUserCertificate(eq(USERNAME));
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertEquals(certificate, response.getEntity());
	}

	@Test(expected = CannotCreateException.class)
	public void postingUsersCertificateWithAUserThatDoesntExistShouldFail() {
		// setup
		when(userManagementService.updateUserCertificate(eq(USERNAME))).thenThrow(new CertificateGenerationException("", new RuntimeException()));

		// act
		usersController.postUserCertificate(USERNAME);
	}

	// Tests for PUT to individual user

	@Test
	public void puttingAUserShouldAlterTheUser() {
		// act
		Viewable viewable = usersController.putUser(USERNAME, REALNAME, EMAIL, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES));

		// assert
		verify(userManagementService).updateUser(USERNAME, REALNAME, EMAIL, true, EXTERNAL_REF, MAX_INSTANCES, MAX_CORES);
		assertNotNull(viewable);
		assertNotNull(viewable.getModel());
		Map model = (Map<String, Object>) viewable.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		assertEquals(INSTANCE_ID, ((Set<String>) model.get("instanceIds")).iterator().next());
		assertEquals(setOfStrings, model.get("imageIds"));
		assertEquals(setOfStrings, model.get("securityGroupIds"));
		assertEquals(setOfStrings, model.get("volumeIds"));
		assertEquals(MAX_INSTANCES, model.get("maxInstances"));
		assertEquals("single_user", viewable.getTemplateName());
	}

	@Test
	public void testPutUserWithUnchangedMaxCoresShouldLeaveMaxCoresUnchanged() {
		// act
		usersController.putUser(USERNAME, REALNAME, EMAIL, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), "UNCHANGED");

		// assert
		verify(userManagementService).updateUser(eq(USERNAME), eq(REALNAME), eq(EMAIL), eq(true), eq(EXTERNAL_REF), eq(MAX_INSTANCES), eq(-1));
	}

	@Test
	public void testPutUserWithBlankMaxCoresShouldSetMaxCoresToNull() {
		// act
		usersController.putUser(USERNAME, REALNAME, EMAIL, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), "");

		// assert
		verify(userManagementService).updateUser(eq(USERNAME), eq(REALNAME), eq(EMAIL), eq(true), eq(EXTERNAL_REF), eq(MAX_INSTANCES), (Integer) isNull());
	}

	@Test
	public void testPutUserWithUnchangedMaxInstancesShouldLeaveMaxInstancesUnchanged() {
		// act
		usersController.putUser(USERNAME, REALNAME, EMAIL, TRUE, EXTERNAL_REF, "UNCHANGED", Integer.toString(MAX_CORES));

		// assert
		verify(userManagementService).updateUser(eq(USERNAME), eq(REALNAME), eq(EMAIL), eq(true), eq(EXTERNAL_REF), eq(-1), eq(MAX_CORES));
	}

	@Test
	public void testPutUserWithBlankMaxInstanceShouldSetMaxInstancesToNull() {
		// act
		usersController.putUser(USERNAME, REALNAME, EMAIL, TRUE, EXTERNAL_REF, "", Integer.toString(MAX_CORES));

		// assert
		verify(userManagementService).updateUser(eq(USERNAME), eq(REALNAME), eq(EMAIL), eq(true), eq(EXTERNAL_REF), (Integer) isNull(), eq(MAX_CORES));
	}

	@Test(expected = IllegalArgumentException.class)
	public void puttingAUserShouldFailForBadEnabledValue() {
		// act
		usersController.putUser(USERNAME, REALNAME, EMAIL, "bad", EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES));
	}

	@Test(expected = IllegalArgumentException.class)
	public void puttingAUserShouldFailForBadMaxInstances() {
		// act
		try {
			usersController.putUser(USERNAME, REALNAME, EMAIL, "true", EXTERNAL_REF, "fred", "2");
		} catch (IllegalArgumentException e) {
			assertEquals("maxInstances must be a valid number greater than -1", e.getMessage());
			throw e;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void puttingAUserShouldFailForBadMaxCores() {
		// act
		try {
			usersController.putUser(USERNAME, REALNAME, EMAIL, "true", EXTERNAL_REF, "2", "fred");
		} catch (IllegalArgumentException e) {
			assertEquals("maxCores must be a valid number greater than -1", e.getMessage());
			throw e;
		}
	}

	@Test
	public void changingMaxValuesShouldntDisableAUser() {
		// act
		usersController.putUser(USERNAME, null, null, null, null, "50", "50");

		// assert
		verify(userManagementService).updateUser(USERNAME, null, null, null, null, 50, 50);
	}

	@Test(expected = NotFoundException.class)
	public void puttingAUserThatDoesntExistShouldFail() {
		// setup
		Mockito.doThrow(new UserNotFoundException("")).when(userManagementService)
				.updateUser(any(String.class), any(String.class), any(String.class), anyBoolean(), any(String.class), anyInt(), anyInt());

		usersController.putUser(USERNAME, REALNAME, EMAIL, TRUE, EXTERNAL_REF, Integer.toString(MAX_INSTANCES), Integer.toString(MAX_CORES));
	}

	@Test
	public void gettingUserByAccessKeyShouldReturnTheUser() {
		// act and assert
		assertEquals(readOnlyUser, usersController.getUserByApiAccessKey(ACCESS_KEY));
	}

	@Test(expected = IllegalArgumentException.class)
	public void gettingUserByAccessKeyWithNoAccessKeyShouldNotWork() {
		// act and assert
		usersController.getUserByApiAccessKey(null);
	}

	@Test(expected = NotFoundException.class)
	public void gettingUserThatDoesntExistByAccessKeyShould404() {
		// setup
		when(userManagementService.getUserByApiAccessKey(any(String.class))).thenThrow(new UserNotFoundException(""));

		// act
		usersController.getUserByApiAccessKey(ACCESS_KEY);
	}

	@Test(expected = NotFoundException.class)
	public void gettingUserThatIsDeletedByAccessKeyShould404() {
		// setup
		when(user.isDeleted()).thenReturn(true);

		// act
		usersController.getUserByApiAccessKey(ACCESS_KEY);
	}

	@Test
	public void gettingUserByAccessKeyAsHtmlShouldReturnAViewableWithTheRightTemplateAndModelPopulated() {
		// act
		Viewable viewable = usersController.getUserByApiAccessKeyHtml(ACCESS_KEY);

		// assert
		assertNotNull(viewable);
		assertNotNull(viewable.getModel());
		Map model = (Map<String, Object>) viewable.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		assertEquals(INSTANCE_ID, ((Set<String>) model.get("instanceIds")).iterator().next());
		assertEquals(setOfStrings, model.get("imageIds"));
		assertEquals(setOfStrings, model.get("securityGroupIds"));
		assertEquals(setOfStrings, model.get("volumeIds"));
		assertEquals(MAX_INSTANCES, model.get("maxInstances"));
		assertEquals("single_user", viewable.getTemplateName());
	}

	@Test
	public void setUserEnabledFlagToFalseShouldReturnViewableWithUserDisabled() {
		// act
		Viewable viewable = usersController.disableUser(USERNAME);

		// assert
		verify(userManagementService).setUserEnabledFlag(USERNAME, false);
		assertNotNull(viewable);
		assertNotNull(viewable.getModel());
		Map model = (Map<String, Object>) viewable.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		assertEquals(false, model.get("enabled"));
	}

	@Test
	public void setUserEnabledFlagToTrueShouldReturnViewableWithUserEnabled() {
		// act
		Viewable viewable = usersController.enableUser(USERNAME);

		// assert
		verify(userManagementService).setUserEnabledFlag(USERNAME, true);
		assertNotNull(viewable);
		assertNotNull(viewable.getModel());
		Map<String, Object> model = (Map<String, Object>) viewable.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		assertEquals(true, model.get("enabled"));
	}

	@Test
	public void shouldReturnInstanceValidationAddresss() {
		// setup
		PId pid = mock(PId.class);
		when(piIdBuilder.getPId("user:" + USERNAME)).thenReturn(pid);
		String pidString = "123456";
		when(pid.toStringFull()).thenReturn(pidString);
		String dnsName = "ops.com";
		usersController.setOpsWebsiteDnsName(dnsName);

		// act
		Viewable result = usersController.getInstanceValidationAddress(USERNAME);

		// assert
		assertNotNull(result);
		assertNotNull(result.getModel());
		Map model = (Map<String, Object>) result.getModel();
		assertEquals(USERNAME, model.get("username"));
		assertEquals(pidString, model.get("pid"));
		assertEquals(dnsName, model.get("ops_website_dns_name"));
	}

	@Test
	public void shouldSendInstanceValidationEmail() {
		// act
		Response result = usersController.sendInstanceValidationEmail(USERNAME);

		// assert
		assertEquals(200, result.getStatus());
		verify(usersInstanceValidationWatcher).sendEmail(user);
	}

	@Test(expected = NotFoundException.class)
	public void shouldReturnNotFoundForDeletedUser() {
		// setup
		when(user.isDeleted()).thenReturn(true);

		// act
		usersController.sendInstanceValidationEmail(USERNAME);
	}

	@Test(expected = NotFoundException.class)
	public void shouldReturnNotFoundForNonExistent() {
		// setup
		when(userManagementService.getUser(USERNAME)).thenThrow(new UserNotFoundException(""));

		// act
		usersController.sendInstanceValidationEmail(USERNAME);
	}
}
