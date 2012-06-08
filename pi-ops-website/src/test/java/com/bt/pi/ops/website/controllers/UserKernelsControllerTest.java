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
public class UserKernelsControllerTest {
	private UserKernelsController userKernelsController;
	@Mock
	private ImageServiceImpl imageService;

	@Before
	public void before() {
		when(imageService.registerImage("user", "location", MachineType.KERNEL)).thenReturn("pki-123");
		when(imageService.deregisterImage("user", "pki-123", MachineType.KERNEL)).thenReturn(true);

		userKernelsController = new UserKernelsController();
		userKernelsController.setImageService(imageService);
	}

	@Test
	public void shouldRegisterKernel() {
		// act
		String res = userKernelsController.registerKernel("user", "location");

		// verify
		assertEquals("{\"kernelId\":\"pki-123\"}", res);
	}

	@Test
	public void shouldDeregisterKernel() {
		// act
		String res = userKernelsController.deregisterKernel("user", "pki-123");

		// verify
		assertEquals("{\"status\":\"ok\"}", res);
	}
}
