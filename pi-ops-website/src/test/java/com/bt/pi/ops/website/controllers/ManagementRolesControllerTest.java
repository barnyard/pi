package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.parser.KoalaJsonParser;

@RunWith(MockitoJUnitRunner.class)
public class ManagementRolesControllerTest {

	@InjectMocks
	private ManagementRolesController controller = new ManagementRolesController();
	@Mock
	private KoalaJsonParser parser;

	@Test
	public void shouldReturnManagementRoles() {
		// setup
		String json = "json";
		Mockito.when(parser.getJson(Matchers.any())).thenReturn(json);

		// act
		Response response = controller.readAll();

		// assert
		assertNotNull(response);
		assertEquals(200, response.getStatus());
		assertEquals(json, response.getEntity());
	}
}
