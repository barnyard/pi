package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import com.bt.pi.app.common.entities.ManagementUser;
import com.bt.pi.app.common.entities.ManagementUsers;
import com.bt.pi.app.management.ManagementUsersSeeder;
import com.bt.pi.core.parser.KoalaJsonParser;

@RunWith(MockitoJUnitRunner.class)
public class ManagementUsersControllerTest {

	private static final String ROLE_MIS = null;

	@Mock
	private KoalaJsonParser koalaJsonParser;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ManagementUsersSeeder managementUsersSeeder;

	@InjectMocks
	ManagementUsersController controller = new ManagementUsersController();

	@Mock
	private ManagementUser mockedUser;

	public ManagementUsersControllerTest() {

	}

	@Test
	public void shouldCreateManagementUser() throws Exception {
		// setup

		// act
		Response response = controller.create("user", "pass", "ROLE_MIS");

		// assert
		assertEquals(201, response.getStatus());
	}

	@Test
	public void shouldReturnConflictedStatusIfUserAlreadyExistsOnCreatingUser() {
		// setup
		when(managementUsersSeeder.getUser("user")).thenReturn(mockedUser);

		// act
		Response response = controller.create("user", "passwo", "ROLE_OPS");

		// assert
		assertEquals(Status.CONFLICT, Status.fromStatusCode(response.getStatus()));
	}

	@Test
	public void shouldUpdateManagementUserIfItexists() throws Exception {
		// setup
		when(managementUsersSeeder.getUser("user")).thenReturn(mockedUser);

		// act
		Response response = controller.update("user", "pass", "ROLE_MIS");

		// assert
		assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
	}

	@Test
	public void shouldReturn404IfUserNotFoundOnUpdatingUser() {
		// setup
		when(managementUsersSeeder.getUser("user")).thenReturn(null);

		// act
		Response response = controller.update("user", "pass", "ROLE_MIS");

		// assert
		assertEquals(Status.NOT_FOUND, Status.fromStatusCode(response.getStatus()));
	}

	@Test
	public void shouldDeleteManagementUser() throws Exception {
		// setup
		when(managementUsersSeeder.getUser("user")).thenReturn(mockedUser);

		// act
		Response response = controller.delete("user");

		// assert
		assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
	}

	@Test
	public void shouldReturn404IfUserNotFoundOnDeletingUser() {
		// setup

		// act
		Response response = controller.delete("user");

		// assert
		assertEquals(Status.NOT_FOUND, Status.fromStatusCode(response.getStatus()));
	}

	@Test
	public void shouldGetAllManagementUsers() throws Exception {
		// setup
		ManagementUsers users = new ManagementUsers();
		when(managementUsersSeeder.getAllUsers()).thenReturn(users);

		// act
		Response response = controller.readAll();

		// assert
		assertEquals(200, response.getStatus());
	}

	@Test
	public void shouldReturn404IfUnableToGetManagementUsers() throws Exception {
		// setup
		when(managementUsersSeeder.getAllUsers()).thenReturn(null);

		// act
		Response response = controller.readAll();

		// assert
		assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
	}

	@Test
	public void shouldGetManagementUser() throws Exception {
		// setup
		ManagementUser user = new ManagementUser();
		when(managementUsersSeeder.getUser("user")).thenReturn(user);

		// act
		Response response = controller.read("user");

		// assert
		assertEquals(200, response.getStatus());
	}

	@Test
	public void shouldReturn404IfUnableToGetManagementUser() throws Exception {
		// setup
		when(managementUsersSeeder.getUser("user")).thenReturn(null);

		// act
		Response response = controller.read("user");

		// assert
		assertEquals(404, response.getStatus());
	}
}
