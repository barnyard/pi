package com.bt.pi.ops.website.entities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;

import com.bt.pi.app.common.entities.ManagementRoles;
import com.bt.pi.app.common.entities.ManagementUser;

@XmlRootElement(name = "managementuser")
public class ReadOnlyManagementUser {

	private final ManagementUser managementUser;

	public ReadOnlyManagementUser() {
		managementUser = new ManagementUser();
	}

	public ReadOnlyManagementUser(ManagementUser aManagementUser) {
		this.managementUser = aManagementUser;
	}

	@XmlElement
	public String getUsername() {
		return managementUser.getUsername();
	}

	public void setUsername(String username) {
		managementUser.setUsername(username);
	}

	@XmlElement
	public String getRoles() {
		return StringUtils.join(managementUser.getRoles().toArray(), ';');
	}

	public void setRoles(String roles) {
		for (String role : roles.split(";")) {
			managementUser.getRoles().add(ManagementRoles.valueOf(role));
		}
	}
}