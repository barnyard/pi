package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DeregisterImageDocument;
import com.amazonaws.ec2.doc.x20081201.DeregisterImageResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DeregisterImageType;
import com.bt.pi.api.handlers.DeregisterImageHandler;
import com.bt.pi.api.service.ManagementImageService;

public class DeregisterImageHandlerTest extends AbstractHandlerTest {

	private DeregisterImageHandler deregisterImageHandler;
	private DeregisterImageDocument requestDocument;
	private DeregisterImageType addNewDederegisterImage;
	private ManagementImageService imageService;
	
	@Before
	public void setUp() throws Exception {
		super.before();
		this.deregisterImageHandler = new DeregisterImageHandler() {
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		requestDocument = DeregisterImageDocument.Factory.newInstance();
		addNewDederegisterImage = requestDocument.addNewDeregisterImage();
		addNewDederegisterImage.setImageId("kmi-123");
		imageService = mock(ManagementImageService.class);
		when(imageService.deregisterImage("userid", "kmi-123")).thenReturn(true);
		deregisterImageHandler.setImageService(imageService);
	}

	@Test
	public void testDeregisterImageGood() {
		// setup
		// act
		DeregisterImageResponseDocument result = this.deregisterImageHandler.deregisterImage(requestDocument);
		
		// assert
		assertEquals(true, result.getDeregisterImageResponse().getReturn());
	}
}
