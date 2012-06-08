package com.bt.pi.ops.website.controllers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.management.SuperNodeSeeder;

@RunWith(MockitoJUnitRunner.class)
public class SupernodeControllerTest {

	private static final String SUPERNODE_APP = "some-app";

	@Mock
	SuperNodeSeeder superNodeSeeder;

	@InjectMocks
	SupernodeController controller = new SupernodeController();

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentExceptionIfApplicationNameIsNotProvided() {
		// act
		controller.configureSuperNodes("", 4, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentExceptionIfOffsetIsNegative() {
		// act
		controller.configureSuperNodes(SUPERNODE_APP, 2, -1);
	}

	@Test
	public void shouldReturnCorrectMessageIfSuperNodesConfiguredSuccessfully() {
		// act
		String result = controller.configureSuperNodes(SUPERNODE_APP, 4, 0);

		// assert
		verify(superNodeSeeder).configureNumberOfSuperNodes(SUPERNODE_APP, 4, 0);
		assertThat(result, is(String.format("Application (%s) configured", SUPERNODE_APP)));
	}
}
