package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.IpPermissionSetType;
import com.amazonaws.ec2.doc.x20081201.IpPermissionType;
import com.amazonaws.ec2.doc.x20081201.IpRangeItemType;
import com.amazonaws.ec2.doc.x20081201.IpRangeSetType;
import com.amazonaws.ec2.doc.x20081201.RevokeSecurityGroupIngressDocument;
import com.amazonaws.ec2.doc.x20081201.RevokeSecurityGroupIngressResponseDocument;
import com.amazonaws.ec2.doc.x20081201.RevokeSecurityGroupIngressType;
import com.bt.pi.api.handlers.RevokeSecurityGroupIngressHandler;
import com.bt.pi.api.service.SecurityGroupService;
import com.bt.pi.app.common.entities.NetworkProtocol;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.NetworkRuleType;
public class RevokeSecurityGroupIngressHandlerTest extends AbstractHandlerTest {

	private RevokeSecurityGroupIngressHandler handler;
	private SecurityGroupService securityGroupService;
	
	@Before 
	public void before(){
		super.before();
		handler = new RevokeSecurityGroupIngressHandler(){
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		List<NetworkRule> networkRules = new ArrayList<NetworkRule>();
		NetworkRule networkRule = new NetworkRule();
		networkRule.setDestinationSecurityGroupName("default");
		networkRule.setNetworkProtocol(NetworkProtocol.TCP);
		networkRule.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
		networkRule.setPortRangeMax(80);
		networkRule.setPortRangeMin(80);
		networkRule.setSourceNetworks(new String[]{"0.0.0.0/0"});
		networkRules.add(networkRule);
		securityGroupService = mock(SecurityGroupService.class);
		when(securityGroupService.revokeIngress("userid", "default", networkRules)).thenReturn(true);
		handler.setSecurityGroupService(securityGroupService);
	}
	
	@Test
	public void testRevokeIngressGood(){
		// setup
		RevokeSecurityGroupIngressDocument requestDocument = RevokeSecurityGroupIngressDocument.Factory.newInstance();
		RevokeSecurityGroupIngressType revokeSecurityGroupIngressType = requestDocument.addNewRevokeSecurityGroupIngress();
		IpPermissionSetType ipPermissions = IpPermissionSetType.Factory.newInstance();
		IpPermissionType ipPermissionType = ipPermissions.addNewItem();
		ipPermissionType.setIpProtocol("tcp");
		ipPermissionType.setToPort(80);
		ipPermissionType.setFromPort(80);
		IpRangeSetType ipRange = IpRangeSetType.Factory.newInstance();
		IpRangeItemType ipRangeItemType = ipRange.addNewItem();
		ipRangeItemType.setCidrIp("0.0.0.0/0");
		ipPermissionType.setIpRanges(ipRange);
		revokeSecurityGroupIngressType.setIpPermissions(ipPermissions);
		revokeSecurityGroupIngressType.setGroupName("default");
		revokeSecurityGroupIngressType.setUserId("userid");
		// act
		RevokeSecurityGroupIngressResponseDocument responseDocument = handler.revokeSecurityGroupIngress(requestDocument);
		//assert
		assertEquals(true, responseDocument.getRevokeSecurityGroupIngressResponse().getReturn());
	}
	
	@Test
	public void testRevokeIngressBad(){
		// setup
		RevokeSecurityGroupIngressDocument requestDocument = RevokeSecurityGroupIngressDocument.Factory.newInstance();
		RevokeSecurityGroupIngressType revokeSecurityGroupIngressType = requestDocument.addNewRevokeSecurityGroupIngress();
		IpPermissionSetType ipPermissions = IpPermissionSetType.Factory.newInstance();
		IpPermissionType ipPermissionType = ipPermissions.addNewItem();
		ipPermissionType.setIpProtocol("tcp");
		ipPermissionType.setToPort(80);
		ipPermissionType.setFromPort(8080);
		IpRangeSetType ipRange = IpRangeSetType.Factory.newInstance();
		IpRangeItemType ipRangeItemType = ipRange.addNewItem();
		ipRangeItemType.setCidrIp("0.0.0.0/0");
		ipPermissionType.setIpRanges(ipRange);
		revokeSecurityGroupIngressType.setIpPermissions(ipPermissions);
		revokeSecurityGroupIngressType.setGroupName("default");
		revokeSecurityGroupIngressType.setUserId("userid");
		// act
		RevokeSecurityGroupIngressResponseDocument responseDocument = handler.revokeSecurityGroupIngress(requestDocument);
		//assert
		assertEquals(false, responseDocument.getRevokeSecurityGroupIngressResponse().getReturn());
	}
	
}
