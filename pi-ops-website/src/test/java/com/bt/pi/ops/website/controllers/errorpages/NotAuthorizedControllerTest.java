package com.bt.pi.ops.website.controllers.errorpages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.ops.website.entities.SimpleErrorMessageEntity;

public class NotAuthorizedControllerTest {
	private NotAuthorizedController notAuthorizedController;

	@Before
	public void before() {
		notAuthorizedController = new NotAuthorizedController();
	}

	@Test
	public void shouldReturnCorrectViewForHtml() {
		assertEquals("403", notAuthorizedController.get403AsHtml().getTemplateName());
	}

	@Test
	public void shouldReturnCorrectResponseForJson() {
		// act
		Response res = notAuthorizedController.get403AsEntity();

		// assert
		assertEquals(403, res.getStatus());
		assertTrue(res.getEntity() instanceof SimpleErrorMessageEntity);
	}
}
