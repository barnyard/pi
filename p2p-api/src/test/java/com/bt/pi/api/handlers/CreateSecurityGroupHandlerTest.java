package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.CreateSecurityGroupDocument;
import com.amazonaws.ec2.doc.x20081201.CreateSecurityGroupResponseDocument;
import com.amazonaws.ec2.doc.x20081201.CreateSecurityGroupType;
import com.bt.pi.api.handlers.CreateSecurityGroupHandler;
import com.bt.pi.api.service.SecurityGroupService;

public class CreateSecurityGroupHandlerTest extends AbstractHandlerTest {

	private CreateSecurityGroupHandler createSecurityGroupHandler;
	private SecurityGroupService securityGroupService;

	@Before
	public void setUp() throws Exception {
		super.before();
		this.createSecurityGroupHandler = new CreateSecurityGroupHandler() {
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		securityGroupService = mock(SecurityGroupService.class);
		when(securityGroupService.createSecurityGroup("userid", "default", "groupDescription")).thenReturn(true);
		createSecurityGroupHandler.setSecurityGroupService(securityGroupService);
	}

	@Test
	public void testCreateSecurityGroup() {
		// setup
		CreateSecurityGroupDocument requestDocument = CreateSecurityGroupDocument.Factory.newInstance();
		CreateSecurityGroupType addNewCreateSecurityGroup = requestDocument.addNewCreateSecurityGroup();
		addNewCreateSecurityGroup.setGroupName("default");
		addNewCreateSecurityGroup.setGroupDescription("groupDescription");
		
		// act
		CreateSecurityGroupResponseDocument result = this.createSecurityGroupHandler.createSecurityGroup(requestDocument);
		
		// assert
		assertEquals(true, result.getCreateSecurityGroupResponse().getReturn());
	}

	@Test
	public void testCreateSecurityFail() {
		// setup
		CreateSecurityGroupDocument requestDocument = CreateSecurityGroupDocument.Factory.newInstance();
		CreateSecurityGroupType addNewCreateSecurityGroup = requestDocument.addNewCreateSecurityGroup();
		addNewCreateSecurityGroup.setGroupName("bogus");
		
		// act
		CreateSecurityGroupResponseDocument result = this.createSecurityGroupHandler.createSecurityGroup(requestDocument);
		
		// assert
		assertEquals(false, result.getCreateSecurityGroupResponse().getReturn());
	}
}
