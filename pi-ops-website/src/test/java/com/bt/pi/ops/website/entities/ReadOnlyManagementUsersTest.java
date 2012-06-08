package com.bt.pi.ops.website.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.app.common.entities.ManagementRoles;
import com.bt.pi.app.common.entities.ManagementUser;
import com.bt.pi.app.common.entities.ManagementUsers;
import com.bt.pi.core.parser.KoalaJsonParser;

public class ReadOnlyManagementUsersTest {

	@Test
	public void shouldEncapsulateManagementUsers() throws Exception {
		// setup
		ManagementUsers managementUsers = new ManagementUsers();
		ManagementUser managementUser = createManagementUser("a");
		managementUsers.getUserMap().put("a", managementUser);

		// act
		ReadOnlyManagementUsers roManagementUsers = new ReadOnlyManagementUsers(managementUsers);

		// assert
		assertEquals(1, roManagementUsers.getManagementUsers().size());
		ReadOnlyManagementUser readOnlyManagementUser = roManagementUsers.getManagementUsers().get(0);
		assertEquals("a", readOnlyManagementUser.getUsername());
		assertEquals("ROLE_MIS;ROLE_OPS", readOnlyManagementUser.getRoles());
	}

	@Test
	public void checkJson() {
		// setup
		ManagementUsers managementUsers = new ManagementUsers();
		ManagementUser managementUser = createManagementUser("a");
		managementUsers.getUserMap().put("a", managementUser);
		ReadOnlyManagementUsers roManagementUsers = new ReadOnlyManagementUsers(managementUsers);

		// act
		KoalaJsonParser parser = new KoalaJsonParser();
		String json = parser.getJson(roManagementUsers);

		ReadOnlyManagementUsers actual = (ReadOnlyManagementUsers) parser.getObject(json, ReadOnlyManagementUsers.class);

		// assert
		assertEquals(roManagementUsers.getManagementUsers().size(), actual.getManagementUsers().size());
		assertEquals("a", actual.getManagementUsers().get(0).getUsername());
		assertEquals("ROLE_MIS;ROLE_OPS", actual.getManagementUsers().get(0).getRoles());
	}

	private ManagementUser createManagementUser(String username) {
		ManagementUser user = new ManagementUser();
		user.setUsername(username);
		user.setPassword("pass");
		user.getRoles().add(ManagementRoles.ROLE_MIS);
		user.getRoles().add(ManagementRoles.ROLE_OPS);
		return user;
	}
}
