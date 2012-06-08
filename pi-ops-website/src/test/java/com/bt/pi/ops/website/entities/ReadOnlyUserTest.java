/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.JAXB;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.app.common.entities.User;

@SuppressWarnings("unchecked")
public class ReadOnlyUserTest {
	private static final String EXTERNAL_REF = "external ref";
	private static final byte[] CERT = new byte[3];
	private static final String USERNAME = "username";
	private static final String REAL_NAME = "real name";
	private static final String EMAIL = "email";
	private static final String SECRET_KEY = "secret key";
	private static final String ACCESS_KEY = "access key";
	private static final Set<String> BUCKETS = mock(Set.class);
	private static final Set<KeyPair> KEYPAIRS = mock(Set.class);
	private static final int MAX_INSTANCES = 26;
	private static final int MAX_CORES = 6;
	private final User user = mock(User.class);
	private final ReadOnlyUser roUser = new ReadOnlyUser(user);
	private Iterator iterator = mock(Iterator.class);

	@Before
	public void before() {
		when(user.getApiAccessKey()).thenReturn(ACCESS_KEY);
		when(user.getApiSecretKey()).thenReturn(SECRET_KEY);
		when(user.getEmailAddress()).thenReturn(EMAIL);
		when(user.getRealName()).thenReturn(REAL_NAME);
		when(user.getUsername()).thenReturn(USERNAME);
		when(user.isEnabled()).thenReturn(true);
		when(user.getCertificate()).thenReturn(CERT);
		when(user.getBucketNames()).thenReturn(BUCKETS);
		when(user.getKeyPairs()).thenReturn(KEYPAIRS);
		when(user.getExternalRefId()).thenReturn(EXTERNAL_REF);
		when(user.getMaxInstances()).thenReturn(MAX_INSTANCES);
		when(user.getMaxCores()).thenReturn(MAX_CORES);
		when(user.getInstanceIds()).thenReturn(new String[] {});
		when(BUCKETS.iterator()).thenReturn(iterator);
		when(KEYPAIRS.iterator()).thenReturn(iterator);
	}

	@Test
	public void equalsShouldBeTrueWhenIsSameObject() {
		assertTrue(roUser.equals(roUser));
	}

	@Test
	public void equalsShouldBeTrueWhenContainsSameObject() {
		assertTrue(roUser.equals(user));
	}

	@Test
	public void gettersShouldDelegate() {
		// setup

		// act and assert
		assertEquals(user, roUser.getUser());
		assertEquals(USERNAME, roUser.getUsername());
		assertEquals(ACCESS_KEY, roUser.getApiAccessKey());
		assertEquals(SECRET_KEY, roUser.getApiSecretKey());
		assertEquals(EMAIL, roUser.getEmailAddress());
		assertEquals(REAL_NAME, roUser.getRealName());
		assertEquals(true, roUser.isEnabled());
		assertEquals(CERT, roUser.getCertificate());
		assertEquals(BUCKETS, roUser.getBucketNames());
		assertEquals(KEYPAIRS, roUser.getKeyPairs());
		assertEquals(EXTERNAL_REF, roUser.getExternalRefId());
		assertEquals(MAX_INSTANCES, roUser.getMaxInstances());
		assertEquals(MAX_CORES, roUser.getMaxCores());

		assertNotNull(roUser.toString());
		assertNotNull(roUser.hashCode());
	}

	@Test
	public void testJaxb() throws Exception {
		// setup
		StringWriter sw = new StringWriter();

		// act
		JAXB.marshal(roUser, sw);
		String result = sw.toString();

		// assert
		assertTrue(result.contains("<maxInstances>26</maxInstances>"));
		assertTrue(result.contains("<apiAccessKey>access key</apiAccessKey>"));
		assertTrue(result.contains("<apiSecretKey>secret key</apiSecretKey>"));
	}
}
