/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.entities;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.app.common.entities.User;

@XmlRootElement(name = "user")
public class ReadOnlyUser {
	private User user;

	public ReadOnlyUser() {
	}

	public ReadOnlyUser(User theUser) {
		user = theUser;
	}

	public void setUser(User theUser) {
		user = theUser;
	}

	@XmlElement
	public byte[] getCertificate() {
		return user.getCertificate();
	}

	@XmlElement
	public String getUsername() {
		return user.getUsername();
	}

	@XmlElement
	public String getApiAccessKey() {
		return user.getApiAccessKey();
	}

	@XmlElement
	public String getApiSecretKey() {
		return user.getApiSecretKey();
	}

	@XmlElementWrapper
	public Set<String> getInstanceIds() {
		Set<String> result = new HashSet<String>();
		result.addAll(Arrays.asList(user.getInstanceIds()));
		return result;
	}

	@XmlElementWrapper
	public Set<String> getVolumeIds() {
		return user.getVolumeIds();
	}

	@XmlElementWrapper
	public Set<String> getImageIds() {
		return user.getImageIds();
	}

	@XmlElementWrapper
	public Set<String> getBucketNames() {
		return user.getBucketNames();
	}

	@XmlElementWrapper
	public Set<String> getSecurityGroupIds() {
		return user.getSecurityGroupIds();
	}

	@XmlElementWrapper
	public Set<KeyPair> getKeyPairs() {
		return user.getKeyPairs();
	}

	@XmlElement
	public String getRealName() {
		return user.getRealName();
	}

	@XmlElement
	public String getEmailAddress() {
		return user.getEmailAddress();
	}

	@XmlElement
	public boolean isEnabled() {
		return user.isEnabled();
	}

	@XmlElement
	public String getExternalRefId() {
		return user.getExternalRefId();
	}

	User getUser() {
		return user;
	}

	@Override
	public String toString() {
		return user.toString();
	}

	@Override
	public int hashCode() {
		return user.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		Object o;
		if (obj instanceof ReadOnlyUser) {
			o = ((ReadOnlyUser) obj).getUser();
		} else {
			o = obj;
		}
		return user.equals(o);
	}

	@XmlElement
	public int getMaxInstances() {
		return user.getMaxInstances();
	}

	@XmlElement
	public int getMaxCores() {
		return user.getMaxCores();
	}
}
