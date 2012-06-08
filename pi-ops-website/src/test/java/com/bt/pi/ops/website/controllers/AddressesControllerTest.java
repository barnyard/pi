package com.bt.pi.ops.website.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.SortedMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.api.service.ElasticIpAddressesService;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.core.parser.KoalaJsonParser;

@RunWith(MockitoJUnitRunner.class)
public class AddressesControllerTest {

	@InjectMocks
	private AddressesController addressesController = new AddressesController();

	@Mock
	private ElasticIpAddressesService elasticIpAddressesService;

	@Mock
	private KoalaJsonParser koalaJsonParser;

	@Test
	public void shouldReturnAddresses() {
		SortedMap<String, InstanceRecord> addresses = mock(SortedMap.class);
		when(elasticIpAddressesService.describeAddresses("userId", null)).thenReturn(addresses);
		// act
		addressesController.getUserElasticIpAddresses("userId");
		// assert
		verify(elasticIpAddressesService).describeAddresses("userId", null);
		verify(koalaJsonParser).getJson(addresses);
	}
}
