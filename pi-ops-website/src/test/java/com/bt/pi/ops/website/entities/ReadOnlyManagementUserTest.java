package com.bt.pi.ops.website.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.app.common.entities.ManagementRoles;
import com.bt.pi.app.common.entities.ManagementUser;
import com.bt.pi.core.parser.KoalaJsonParser;

public class ReadOnlyManagementUserTest {

	@Test
	public void shouldEncapsulateManagementUser() throws Exception {
		// setup
		ManagementUser managementUser = new ManagementUser();
		managementUser.setUsername("a");
		managementUser.setPassword("b");
		managementUser.getRoles().add(ManagementRoles.ROLE_MIS);
		managementUser.getRoles().add(ManagementRoles.ROLE_OPS);

		// act
		ReadOnlyManagementUser roManagementUser = new ReadOnlyManagementUser(managementUser);

		// assert
		assertEquals("a", roManagementUser.getUsername());
		assertEquals("ROLE_MIS;ROLE_OPS", roManagementUser.getRoles());
	}

	@Test
	public void checkJson() {
		// setup
		ManagementUser managementUser = new ManagementUser();
		managementUser.setUsername("a");
		managementUser.setPassword("b");
		managementUser.getRoles().add(ManagementRoles.ROLE_MIS);
		managementUser.getRoles().add(ManagementRoles.ROLE_OPS);
		ReadOnlyManagementUser roManagementUser = new ReadOnlyManagementUser(managementUser);

		// act
		KoalaJsonParser koalaJsonParser = new KoalaJsonParser();
		String json = koalaJsonParser.getJson(roManagementUser);

		ReadOnlyManagementUser actual = (ReadOnlyManagementUser) koalaJsonParser.getObject(json, ReadOnlyManagementUser.class);
		assertEquals(roManagementUser.getUsername(), actual.getUsername());
		assertEquals(roManagementUser.getRoles(), actual.getRoles());
	}
}
