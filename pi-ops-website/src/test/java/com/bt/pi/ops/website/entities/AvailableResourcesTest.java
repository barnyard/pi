package com.bt.pi.ops.website.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class AvailableResourcesTest {

	@Test
	public void shouldStoreAvailableValues() {
		// setup
		AvailableResources availableResources = new AvailableResources();
		// act

		availableResources.setFreeCores(1);
		availableResources.setFreeDiskInMB((long) 10);
		availableResources.setFreeMemoryInMB((long) 20);
		// assert
		assertEquals(new Integer(1), availableResources.getFreeCores());
		assertEquals(new Long(10), availableResources.getFreeDiskInMB());
		assertEquals(new Long(20), availableResources.getFreeMemoryInMB());
	}

	@Test
	public void shouldInitializeAvailableInstancesMap() {
		// setup
		AvailableResources availableResources = new AvailableResources();
		// assert
		assertNotNull(availableResources.getAvailableInstancesByType());
	}
}
