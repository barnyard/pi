/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.bt.pi.app.common.entities.ManagementRoles;
import com.bt.pi.app.common.entities.ManagementUser;
import com.bt.pi.app.common.entities.ManagementUsers;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class PiOpsUserDetailsServiceTest {
	private static final String PASSWORD = "password";
	private static final String USERNAME = "username";
	@InjectMocks
	PiOpsUserDetailsService service = new PiOpsUserDetailsService();;
	@Mock
	PiIdBuilder piIdBuilder;
	@Mock
	PId id;
	@Mock
	BlockingDhtCache dhtCache;
	@Mock
	ManagementUsers users;
	@Mock
	Map<String, ManagementUser> usermap;
	@Mock
	ManagementUser user;
	Collection<ManagementRoles> roles;

	@Before
	public void doBefore() {
		roles = new ArrayList<ManagementRoles>();
		roles.add(ManagementRoles.ROLE_MIS);

		when(piIdBuilder.getPId(isA(ManagementUsers.class))).thenReturn(id);
		when(dhtCache.get(id)).thenReturn(users);
		when(users.getUserMap()).thenReturn(usermap);
		when(usermap.containsKey(USERNAME)).thenReturn(true);
		when(usermap.get(USERNAME)).thenReturn(user);

		when(user.getUsername()).thenReturn(USERNAME);
		when(user.getPassword()).thenReturn(PASSWORD);
		when(user.getRoles()).thenReturn(roles);
	}

	@Test
	public void loadByUserNameShouldLoadTheUserAndConvertToUserDetails() {
		// act
		UserDetails details = service.loadUserByUsername(USERNAME);

		// assert
		assertDetailsEqual(user, details);
	}

	@Test(expected = UsernameNotFoundException.class)
	public void loadByUserNameShouldThrowWhenUserDoesntExist() {
		// setup
		when(usermap.containsKey(USERNAME)).thenReturn(false);

		// act
		service.loadUserByUsername(USERNAME);
	}

	@Test(expected = UsernameNotFoundException.class)
	public void loadByUserNameShouldThrowWhenRecordNotInDHT() {
		// setup
		when(dhtCache.get(id)).thenReturn(null);

		// act
		service.loadUserByUsername(USERNAME);
	}

	@Test
	public void settersShouldNotExcept() {
		service.setPiIdBuilder(piIdBuilder);
		service.setUserCache(dhtCache);
	}

	private void assertDetailsEqual(ManagementUser theuser, UserDetails details) {
		assertEquals(theuser.getUsername(), details.getUsername());
		assertEquals(theuser.getPassword(), details.getPassword());
		for (GrantedAuthority authority : details.getAuthorities()) {
			boolean foundRole = false;
			for (ManagementRoles role : theuser.getRoles()) {
				if (authority.getAuthority().equals(role.name()))
					foundRole = true;
			}
			assertTrue(authority.getAuthority() + " was not found", foundRole);
		}
	}
}
