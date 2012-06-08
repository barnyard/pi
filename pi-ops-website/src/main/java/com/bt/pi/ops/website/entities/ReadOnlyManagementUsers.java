package com.bt.pi.ops.website.entities;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.bt.pi.app.common.entities.ManagementRoles;
import com.bt.pi.app.common.entities.ManagementUser;
import com.bt.pi.app.common.entities.ManagementUsers;

@XmlRootElement(name = "managementusers")
public class ReadOnlyManagementUsers {

	private final ManagementUsers managementUsers;

	public ReadOnlyManagementUsers() {
		managementUsers = new ManagementUsers();
	}

	public ReadOnlyManagementUsers(ManagementUsers aManagementUsers) {
		this.managementUsers = aManagementUsers;
	}

	@XmlElementWrapper
	public List<ReadOnlyManagementUser> getManagementUsers() {
		List<ReadOnlyManagementUser> readOnlyUsers = new ArrayList<ReadOnlyManagementUser>();

		for (ManagementUser mgmtUser : managementUsers.getUserMap().values()) {
			ReadOnlyManagementUser u = new ReadOnlyManagementUser(mgmtUser);
			readOnlyUsers.add(u);
		}

		return readOnlyUsers;
	}

	public void setManagementUsers(List<ReadOnlyManagementUser> users) {
		for (ReadOnlyManagementUser user : users) {
			ManagementUser managementUser = new ManagementUser();
			managementUser.setUsername(user.getUsername());
			for (String role : user.getRoles().split(";")) {
				managementUser.getRoles().add(ManagementRoles.valueOf(role));
			}
			managementUsers.getUserMap().put(user.getUsername(), managementUser);
		}
	}
}
