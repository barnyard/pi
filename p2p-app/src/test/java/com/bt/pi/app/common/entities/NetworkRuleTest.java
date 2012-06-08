package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.bt.pi.app.common.entities.NetworkProtocol;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.NetworkRuleType;
import com.bt.pi.app.common.entities.OwnerIdGroupNamePair;


public class NetworkRuleTest {
	private static final String PI_DEFAULT = "pi-default";
	private static final String SOURCE_NETWORK = "0.0.0.0/0";
	private NetworkRule networkRule;
	private NetworkRule otherNetworkRule;
	private OwnerIdGroupNamePair ownerIdGroupNamePair;
	
	@Before
	public void before() {
		ownerIdGroupNamePair = new OwnerIdGroupNamePair("koala", "default");
		
		networkRule = new NetworkRule();
		networkRule.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
		networkRule.setOwnerIdGroupNamePair(new OwnerIdGroupNamePair[] {ownerIdGroupNamePair});
		networkRule.setSourceNetworks(new String[] {SOURCE_NETWORK});
		networkRule.setDestinationSecurityGroupName(PI_DEFAULT);
		networkRule.setNetworkProtocol(NetworkProtocol.TCP);
		networkRule.setPortRangeMin(0);
		networkRule.setPortRangeMax(0);
		
		otherNetworkRule = new NetworkRule(networkRule);
	}
	
	@Test
	public void shouldBeAbleToMatchEquals() {
		// act
		boolean res = networkRule.equals(otherNetworkRule);
		
		// assert
		assertTrue(res);
		assertTrue(networkRule.hashCode() == otherNetworkRule.hashCode());
	}
	
	@Test
	public void shouldNotMatchWhenSourceChainNameDifferent() {
		// setup
		otherNetworkRule.setOwnerIdGroupNamePair(new OwnerIdGroupNamePair[]{new OwnerIdGroupNamePair("main", "source-chain")});
		
		// act
		boolean res = networkRule.equals(otherNetworkRule);
		
		// assert
		assertFalse(res);
		assertFalse(networkRule.hashCode() == otherNetworkRule.hashCode());
	}
	
	@Test
	public void shouldNotMatchForAdditionalSourceChainNames() {
		// setup
		otherNetworkRule.setOwnerIdGroupNamePair(new OwnerIdGroupNamePair[]{new OwnerIdGroupNamePair("main", "source chain")});
		
		// act
		boolean res = networkRule.equals(otherNetworkRule);
		
		// assert
		assertFalse(res);
		assertFalse(networkRule.hashCode() == otherNetworkRule.hashCode());
	}
	
	@Test
	public void shouldNotMatchWhenSourceNetsDifferent() {
		// setup
		otherNetworkRule.setSourceNetworks(new String[] {"1.2.2.2/23"});
		
		// act
		boolean res = networkRule.equals(otherNetworkRule);
		
		// assert
		assertFalse(res);
		assertFalse(networkRule.hashCode() == otherNetworkRule.hashCode());
	}
	
	@Test
	public void shouldNotMatchForAdditionalSourceNets() {
		// setup
		otherNetworkRule.setSourceNetworks(new String[] {SOURCE_NETWORK, "main source chain"});
		
		// act
		boolean res = networkRule.equals(otherNetworkRule);
		
		// assert
		assertFalse(res);
		assertFalse(networkRule.hashCode() == otherNetworkRule.hashCode());
	}
	
	// restore this test if we ever add more network rule types
	@Test
	@Ignore
	public void shouldNotMatchWhenRuleTypeNotEqual() {
		// setup
		otherNetworkRule.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
		
		// act
		boolean res = networkRule.equals(otherNetworkRule);
		
		// assert
		assertFalse(res);
		assertFalse(networkRule.hashCode() == otherNetworkRule.hashCode());
	}
	
	@Test
	public void shouldNotMatchWhenDestinationGroupNotEqual() {
		// setup
		otherNetworkRule.setDestinationSecurityGroupName("something else");
		
		// act
		boolean res = networkRule.equals(otherNetworkRule);
		
		// assert
		assertFalse(res);
		assertFalse(networkRule.hashCode() == otherNetworkRule.hashCode());
	}
	
	@Test
	public void shouldNotMatchWhenProtocolNotEqual() {
		// setup
		otherNetworkRule.setNetworkProtocol(NetworkProtocol.UDP);
		
		// act
		boolean res = networkRule.equals(otherNetworkRule);
		
		// assert
		assertFalse(res);
		assertFalse(networkRule.hashCode() == otherNetworkRule.hashCode());
	}
	
	@Test
	public void shouldNotMatchWhenMinPortNotEqual() {
		// setup
		otherNetworkRule.setPortRangeMin(1);
		
		// act
		boolean res = networkRule.equals(otherNetworkRule);
		
		// assert
		assertFalse(res);
		assertFalse(networkRule.hashCode() == otherNetworkRule.hashCode());
	}
	
	@Test
	public void shouldNotMatchWhenMaxPortNotEqual() {
		// setup
		otherNetworkRule.setPortRangeMax(1);
		
		// act
		boolean res = networkRule.equals(otherNetworkRule);
		
		// assert
		assertFalse(res);
		assertFalse(networkRule.hashCode() == otherNetworkRule.hashCode());
	}
}
