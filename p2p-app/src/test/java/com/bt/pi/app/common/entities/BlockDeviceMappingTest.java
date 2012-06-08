package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.app.common.entities.BlockDeviceMapping;

public class BlockDeviceMappingTest {

	@Test
	public void shouldConstructBlockDeviceMapping(){
		// setup
		// act
		BlockDeviceMapping blockDeviceMapping = new BlockDeviceMapping();
		blockDeviceMapping.setDeviceName("devicename");
		blockDeviceMapping.setVirtualName("virtualname");
		// assert
		assertEquals("devicename", blockDeviceMapping.getDeviceName());
		assertEquals("virtualname", blockDeviceMapping.getVirtualName());
	}
	
	@Test
	public void shouldConstructUsingFieldsBlockDeviceMapping(){
		// setup
		// act
		BlockDeviceMapping blockDeviceMapping = new BlockDeviceMapping("virtualname", "devicename");
		// assert
		assertEquals("devicename", blockDeviceMapping.getDeviceName());
		assertEquals("virtualname", blockDeviceMapping.getVirtualName());
	}
	
	@Test
	public void shouldBeEqual(){
		// setup
		BlockDeviceMapping blockDeviceMapping1 = new BlockDeviceMapping("virtualname", "devicename");
		BlockDeviceMapping blockDeviceMapping2 = new BlockDeviceMapping("virtualname", "devicename");
		BlockDeviceMapping blockDeviceMapping3 = new BlockDeviceMapping("virtualname", "devicename");
		// assert
		blockDeviceMapping1.equals(blockDeviceMapping2);
		blockDeviceMapping2.equals(blockDeviceMapping3);
		blockDeviceMapping1.equals(blockDeviceMapping3);
	}
	
	@Test
	public void shouldHaveSameHashCode(){
		// setup
		BlockDeviceMapping blockDeviceMapping1 = new BlockDeviceMapping("virtualname", "devicename");
		BlockDeviceMapping blockDeviceMapping2 = new BlockDeviceMapping("virtualname", "devicename");
		BlockDeviceMapping blockDeviceMapping3 = new BlockDeviceMapping("virtualname", "devicename");
		// assert
		assertEquals(blockDeviceMapping1.hashCode(), blockDeviceMapping2.hashCode());
		assertEquals(blockDeviceMapping1.hashCode(), blockDeviceMapping3.hashCode());
		assertEquals(blockDeviceMapping2.hashCode(), blockDeviceMapping3.hashCode());
	}
	
}
