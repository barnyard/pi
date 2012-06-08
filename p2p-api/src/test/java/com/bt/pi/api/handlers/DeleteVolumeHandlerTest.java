package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.bt.pi.api.handlers.DeleteVolumeHandler;
import com.bt.pi.api.service.ElasticBlockStorageService;

public class DeleteVolumeHandlerTest extends AbstractHandlerTest {

	private DeleteVolumeHandler deleteVolumeHandler;
	private ElasticBlockStorageService elasticBlockStorageService;
	private String volumeId = "vol-123";
	
	@Before
	public void before(){
		super.before();
		deleteVolumeHandler = new DeleteVolumeHandler(){
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		elasticBlockStorageService = mock(ElasticBlockStorageService.class);
		when(elasticBlockStorageService.deleteVolume("userid", volumeId)).thenReturn(true);
		deleteVolumeHandler.setElasticBlockStorageService(elasticBlockStorageService);
	}
	
	@Test
	public void testDeleteVolume20081201(){
		// setup
		com.amazonaws.ec2.doc.x20081201.DeleteVolumeDocument requestDocument = com.amazonaws.ec2.doc.x20081201.DeleteVolumeDocument.Factory.newInstance();
		requestDocument.addNewDeleteVolume().setVolumeId(volumeId);
		// act
		com.amazonaws.ec2.doc.x20081201.DeleteVolumeResponseDocument responseDocument = deleteVolumeHandler.deleteVolume(requestDocument);
		// assert
		assertEquals(true, responseDocument.getDeleteVolumeResponse().getReturn());
	}
	
	@Test
	public void testDeleteVolume20090404(){
		// setup
		com.amazonaws.ec2.doc.x20090404.DeleteVolumeDocument requestDocument = com.amazonaws.ec2.doc.x20090404.DeleteVolumeDocument.Factory.newInstance();
		requestDocument.addNewDeleteVolume().setVolumeId(volumeId);
		// act
		com.amazonaws.ec2.doc.x20090404.DeleteVolumeResponseDocument responseDocument = deleteVolumeHandler.deleteVolume(requestDocument);
		// assert
		assertEquals(true, responseDocument.getDeleteVolumeResponse().getReturn());
	}
	
}
