package com.bt.pi.app.common.entities;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.InstanceAddress;


public class InstanceAddressTest {
	private InstanceAddress instanceAddress;
	
	@Before
	public void before() {
		instanceAddress = new InstanceAddress("172.0.0.2","1.2.3.4", "d0:0d:d0:0d:d0:0d");
	}
	
	@Test
	public void shouldCreateViaCopyConstructor() throws Exception {
		// act
		InstanceAddress cloned = new InstanceAddress(instanceAddress);
		
		// assert
		assertEquals(cloned.getPrivateIpAddress(), instanceAddress.getPrivateIpAddress());
		assertEquals(cloned.getPublicIpAddress(), instanceAddress.getPublicIpAddress());
		assertEquals(cloned.getMacAddress(), instanceAddress.getMacAddress());
	}
	
	@Test
	public void shouldNotBeEqualToDifferentType() {
		// act
		boolean res = instanceAddress.equals(new Object());
		
		// assert
		assertFalse(res);
	}
	
	@Test
	public void shouldHaveSameHashcodeIfEqual() {
		// act
		int res = new InstanceAddress("172.0.0.2", "1.2.3.4", "d0:0d:d0:0d:d0:0d").hashCode();
		
		// assert
		assertEquals(res, instanceAddress.hashCode());
	}
	
	@Test
	public void shouldBeEqualWithNonNullFields() {
		// act
		boolean res = instanceAddress.equals(new InstanceAddress("172.0.0.2", "1.2.3.4", "d0:0d:d0:0d:d0:0d"));
		
		// assert
		assertTrue(res);
	}
	
	@Test
	public void shouldBeEqualWithNullPublicIp() {
		// setup
		instanceAddress = new InstanceAddress("172.0.0.2", null, "d0:0d:d0:0d:d0:0d");
		
		// act
		boolean res = instanceAddress.equals(new InstanceAddress("172.0.0.2", null, "d0:0d:d0:0d:d0:0d"));
		
		// assert
		assertTrue(res);
	}
	
	@Test
	public void shouldBeEqualWithNullPrivateIp() {
		// setup
		instanceAddress = new InstanceAddress(null, "1.2.3.4", "d0:0d:d0:0d:d0:0d");
		
		// act
		boolean res = instanceAddress.equals(new InstanceAddress(null, "1.2.3.4", "d0:0d:d0:0d:d0:0d"));
		
		// assert
		assertTrue(res);
	}
	
	@Test
	public void shouldBeEqualWithNullMac() {
		// setup
		instanceAddress = new InstanceAddress("172.0.0.2", "1.2.3.4", null);
		
		// act
		boolean res = instanceAddress.equals(new InstanceAddress("172.0.0.2", "1.2.3.4", null));
		
		// assert
		assertTrue(res);
	}
	
	@Test
	public void shouldNotBeEqualWithNonMatchingMac() {
		// act
		boolean res = instanceAddress.equals(new InstanceAddress("172.0.0.2", "1.2.3.4", "eh"));
		
		// assert
		assertFalse(res);
	}
	
	@Test
	public void shouldNotBeEqualWithNonMatchingPublic() {
		// act
		boolean res = instanceAddress.equals(new InstanceAddress("172.0.0.2", null, "d0:0d:d0:0d:d0:0d"));
		
		// assert
		assertFalse(res);
	}
	
	@Test
	public void shouldNotBeEqualWithNonMatchingPrivate() {
		// act
		boolean res = instanceAddress.equals(new InstanceAddress("172.0.0.3", "1.2.3.4", "d0:0d:d0:0d:d0:0d"));
		
		// assert
		assertFalse(res);
	}

	@Test
	public void testEqualsSame() {
		// act
		boolean result = instanceAddress.equals(instanceAddress);

		// assert
		assertTrue(result);
	}

	@Test
	public void testEqualsNull() {
		// act
		boolean result = instanceAddress.equals(null);

		// assert
		assertFalse(result);
	}
}
