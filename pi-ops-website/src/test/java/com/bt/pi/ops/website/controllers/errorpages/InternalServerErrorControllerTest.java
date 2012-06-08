package com.bt.pi.ops.website.controllers.errorpages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.ops.website.entities.SimpleErrorMessageEntity;

public class InternalServerErrorControllerTest {
	private InternalServerErrorController internalServerErrorController;

	@Before
	public void before() {
		internalServerErrorController = new InternalServerErrorController();
	}

	@Test
	public void shouldReturnCorrectView() {
		assertEquals("500", internalServerErrorController.get500AsHtml().getTemplateName());
	}

	@Test
	public void shouldReturnCorrectResponseForJson() {
		// act
		Response res = internalServerErrorController.get500AsEntity();

		// assert
		assertEquals(500, res.getStatus());
		assertTrue(res.getEntity() instanceof SimpleErrorMessageEntity);
	}
}
