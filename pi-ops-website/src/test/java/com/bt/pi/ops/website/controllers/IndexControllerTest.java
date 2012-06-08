package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.api.view.Viewable;

public class IndexControllerTest {
	private IndexController indexController;

	@Before
	public void before() {
		indexController = new IndexController();
	}

	@Test
	public void shouldReturnEmptyModelAndIndexView() {
		// act
		Viewable res = indexController.getAllHtml();

		// assert
		assertEquals("index", res.getTemplateName());
		assertEquals(0, ((Map<?, ?>) res.getModel()).size());
	}
}
