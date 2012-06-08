package com.bt.pi.ops.website.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class SimpleErrorMessageEntityTest {
	private SimpleErrorMessageEntity errorMessageEntity;

	@Before
	public void before() {
	}

	@Test
	public void testEmptyConstructor() {
		new SimpleErrorMessageEntity();
	}

	@Test
	public void testMessageConstructor() {
		// act
		errorMessageEntity = new SimpleErrorMessageEntity("abc");

		// assert
		assertEquals("abc", errorMessageEntity.getMessage());
	}
}
