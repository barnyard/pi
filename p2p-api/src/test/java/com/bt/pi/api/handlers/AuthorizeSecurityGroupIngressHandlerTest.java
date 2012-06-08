package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.AuthorizeSecurityGroupIngressDocument;
import com.amazonaws.ec2.doc.x20081201.AuthorizeSecurityGroupIngressResponseDocument;
import com.amazonaws.ec2.doc.x20081201.AuthorizeSecurityGroupIngressType;
import com.amazonaws.ec2.doc.x20081201.IpPermissionSetType;
import com.amazonaws.ec2.doc.x20081201.IpPermissionType;
import com.amazonaws.ec2.doc.x20081201.IpRangeItemType;
import com.amazonaws.ec2.doc.x20081201.IpRangeSetType;
import com.bt.pi.api.handlers.AuthorizeSecurityGroupIngressHandler;
import com.bt.pi.api.service.SecurityGroupService;
import com.bt.pi.app.common.entities.NetworkProtocol;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.NetworkRuleType;

public class AuthorizeSecurityGroupIngressHandlerTest extends AbstractHandlerTest {

	private AuthorizeSecurityGroupIngressHandler authorizeSecurityGroupIngressHandler;
	private AuthorizeSecurityGroupIngressDocument requestDocument;
	private AuthorizeSecurityGroupIngressType addNewAuthorizeSecurityGroupIngress;
	private String groupName;
	private SecurityGroupService securityGroupService;

	@Before
	public void setUp() throws Exception {
		super.before();
		this.authorizeSecurityGroupIngressHandler = new AuthorizeSecurityGroupIngressHandler(){
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		requestDocument = AuthorizeSecurityGroupIngressDocument.Factory.newInstance();
		addNewAuthorizeSecurityGroupIngress = requestDocument.addNewAuthorizeSecurityGroupIngress();
		groupName = "default";
		addNewAuthorizeSecurityGroupIngress.setGroupName(groupName);
		IpPermissionSetType ipPermissions = IpPermissionSetType.Factory.newInstance();
		IpPermissionType ipPermissionType = ipPermissions.addNewItem();
		ipPermissionType.setIpProtocol("tcp");
		ipPermissionType.setToPort(80);
		ipPermissionType.setFromPort(80);
		IpRangeSetType ipRange = IpRangeSetType.Factory.newInstance();
		IpRangeItemType ipRangeItemType = ipRange.addNewItem();
		ipRangeItemType.setCidrIp("0.0.0.0/0");
		ipPermissionType.setIpRanges(ipRange);
		addNewAuthorizeSecurityGroupIngress.setIpPermissions(ipPermissions);
		securityGroupService = mock(SecurityGroupService.class);
		List<NetworkRule> networkRules = new ArrayList<NetworkRule>();
		NetworkRule networkRule = new NetworkRule();
		networkRule.setDestinationSecurityGroupName("default");
		networkRule.setNetworkProtocol(NetworkProtocol.TCP);
		networkRule.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
		networkRule.setPortRangeMax(80);
		networkRule.setPortRangeMin(80);
		networkRule.setSourceNetworks(new String[]{"0.0.0.0/0"});
		networkRules.add(networkRule);
		when(securityGroupService.authoriseIngress("userid", "default", networkRules)).thenReturn(true);
		authorizeSecurityGroupIngressHandler.setSecurityGroupService(securityGroupService);
	}

	@Test
	public void testAllocateAddressGood() {
		// setup
		
		// act
		AuthorizeSecurityGroupIngressResponseDocument result = this.authorizeSecurityGroupIngressHandler.authorizeSecurityGroupIngress(requestDocument);
		
		// assert
		assertEquals(true, result.getAuthorizeSecurityGroupIngressResponse().getReturn());
	}

	@Test
	public void testAllocateAddressBad() {
		// setup
		addNewAuthorizeSecurityGroupIngress.setGroupName("test");
		
		// act
		AuthorizeSecurityGroupIngressResponseDocument result = this.authorizeSecurityGroupIngressHandler.authorizeSecurityGroupIngress(requestDocument);
		
		// assert
		assertEquals(false, result.getAuthorizeSecurityGroupIngressResponse().getReturn());
	}
}
