package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.api.service.ImageServiceImpl;
import com.bt.pi.app.common.entities.MachineType;

@RunWith(MockitoJUnitRunner.class)
public class UserRamdisksControllerTest {
	private UserRamdisksController userRamdisksController;
	@Mock
	private ImageServiceImpl imageService;

	@Before
	public void before() {
		when(imageService.registerImage("user", "location", MachineType.RAMDISK)).thenReturn("pri-123");
		when(imageService.deregisterImage("user", "pri-123", MachineType.RAMDISK)).thenReturn(true);

		userRamdisksController = new UserRamdisksController();
		userRamdisksController.setImageService(imageService);
	}

	@Test
	public void shouldRegisterRamdisk() {
		// act
		String res = userRamdisksController.registerRamdisk("user", "location");

		// verify
		assertEquals("{\"ramdiskId\":\"pri-123\"}", res);
	}

	@Test
	public void shouldDeregisterKernel() {
		// act
		String res = userRamdisksController.deregisterRamdisk("user", "pri-123");

		// verify
		assertEquals("{\"status\":\"ok\"}", res);
	}
}
