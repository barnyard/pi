package com.bt.pi.ops.website.controllers.errorpages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.ops.website.entities.SimpleErrorMessageEntity;

public class NotFoundControllerTest {
	private NotFoundController notFoundController;

	@Before
	public void before() {
		notFoundController = new NotFoundController();
	}

	@Test
	public void shouldReturnCorrectView() {
		assertEquals("404", notFoundController.get404AsHtml().getTemplateName());
	}

	@Test
	public void shouldReturnCorrectResponseForJson() {
		// act
		Response res = notFoundController.get404AsEntity();

		// assert
		assertEquals(404, res.getStatus());
		assertTrue(res.getEntity() instanceof SimpleErrorMessageEntity);
	}
}
