package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;

import com.bt.pi.app.common.entities.ManagementRoles;

@RunWith(MockitoJUnitRunner.class)
public class CurrentUserControllerTest {
	private CurrentUserController currentUserController;
	@Mock
	private Authentication authentication;

	@Before
	public void before() {
		currentUserController = new CurrentUserController() {
			@Override
			protected Authentication getAuthentication() {
				return authentication;
			}
		};
	}

	@Test
	public void shouldReturnUserDetailsForUserWithOneRole() {
		// / setup
		when(authentication.getName()).thenReturn("dick");
		when(authentication.getAuthorities()).thenReturn(Arrays.asList(new GrantedAuthority[] { new GrantedAuthorityImpl(ManagementRoles.ROLE_OPS.name()) }));

		// act
		String res = currentUserController.getUserInfo();

		// assert
		assertEquals("{\"username\":\"dick\", \"roles\":\"ROLE_OPS\"}", res);
	}

	@Test
	public void shouldReturnUserDetailsForUserWithNoRoles() {
		// / setup
		when(authentication.getName()).thenReturn("dick");
		when(authentication.getAuthorities()).thenReturn(Arrays.asList(new GrantedAuthority[0]));

		// act
		String res = currentUserController.getUserInfo();

		// assert
		assertEquals("{\"username\":\"dick\", \"roles\":\"\"}", res);
	}

	@Test
	public void shouldReturnUserDetailsForUserWithTwoRoles() {
		// / setup
		when(authentication.getName()).thenReturn("dick");
		when(authentication.getAuthorities()).thenReturn(
				Arrays.asList(new GrantedAuthority[] { new GrantedAuthorityImpl(ManagementRoles.ROLE_OPS.name()), new GrantedAuthorityImpl(ManagementRoles.ROLE_MIS.name()) }));

		// act
		String res = currentUserController.getUserInfo();

		// assert
		assertEquals("{\"username\":\"dick\", \"roles\":\"ROLE_OPS,ROLE_MIS\"}", res);
	}
}
