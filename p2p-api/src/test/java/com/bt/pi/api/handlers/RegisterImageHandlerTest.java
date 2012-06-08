package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.RegisterImageDocument;
import com.amazonaws.ec2.doc.x20081201.RegisterImageResponseDocument;
import com.amazonaws.ec2.doc.x20081201.RegisterImageType;
import com.bt.pi.api.handlers.RegisterImageHandler;
import com.bt.pi.api.service.ManagementImageService;

public class RegisterImageHandlerTest extends AbstractHandlerTest {

	private RegisterImageHandler registerImageHandler;
	private RegisterImageDocument requestDocument;
	private RegisterImageType addNewRegisterImage;
	private ManagementImageService imageService;

	@Before
	public void setUp() throws Exception {
		super.before();
		this.registerImageHandler = new RegisterImageHandler() {
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		requestDocument = RegisterImageDocument.Factory.newInstance();
		addNewRegisterImage = requestDocument.addNewRegisterImage();
		addNewRegisterImage.setImageLocation("/tmp/imagefile");
		imageService = mock(ManagementImageService.class);
		when(imageService.registerImage("userid", "/tmp/imagefile")).thenReturn("kmi-123");
		registerImageHandler.setImageService(imageService);
	}

	@Test
	public void testRegisterImageGood() {
		// setup
		
		// act
		RegisterImageResponseDocument result = this.registerImageHandler.registerImage(requestDocument);
		
		// assert
		assertEquals("kmi-123", result.getRegisterImageResponse().getImageId());
	}
}
