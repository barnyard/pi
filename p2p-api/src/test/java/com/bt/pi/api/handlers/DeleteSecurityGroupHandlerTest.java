package com.bt.pi.api.handlers;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DeleteSecurityGroupDocument;
import com.amazonaws.ec2.doc.x20081201.DeleteSecurityGroupResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DeleteSecurityGroupType;
import com.bt.pi.api.handlers.DeleteSecurityGroupHandler;
import com.bt.pi.api.service.SecurityGroupService;

public class DeleteSecurityGroupHandlerTest extends AbstractHandlerTest {

	private DeleteSecurityGroupHandler deleteSecurityGroupHandler;
	private SecurityGroupService securityGroupService;
	
	@Before
	public void before(){
		super.before();
		deleteSecurityGroupHandler = new DeleteSecurityGroupHandler(){
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		securityGroupService = mock(SecurityGroupService.class);
		when(securityGroupService.deleteSecurityGroup("userid", "default")).thenReturn(true);
		deleteSecurityGroupHandler.setSecurityGroupService(securityGroupService);
	}
	
	@Test
	public void testDeleteSecurityGroup(){
		// setup
		DeleteSecurityGroupDocument requestDocument = DeleteSecurityGroupDocument.Factory.newInstance();
		DeleteSecurityGroupType deleteSecurityGroupType = requestDocument.addNewDeleteSecurityGroup();
		deleteSecurityGroupType.setGroupName("default");
		// act
		DeleteSecurityGroupResponseDocument deleteSecurityGroupResponseDocument = deleteSecurityGroupHandler.deleteSecurityGroup(requestDocument);
		// assert
		assertEquals(true, deleteSecurityGroupResponseDocument.getDeleteSecurityGroupResponse().getReturn());
	}
	
}
