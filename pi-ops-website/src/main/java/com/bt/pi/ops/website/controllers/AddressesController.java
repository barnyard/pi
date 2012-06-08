package com.bt.pi.ops.website.controllers;

import java.util.SortedMap;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.bt.pi.api.service.ElasticIpAddressesService;
import com.bt.pi.app.common.entities.InstanceRecord;

@Component
@Path("/addresses/{userid}")
public class AddressesController extends ControllerBase {

	@Resource
	private ElasticIpAddressesService elasticIpAddressesService;

	public AddressesController() {
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getUserElasticIpAddresses(@PathParam("userid") String userId) {
		SortedMap<String, InstanceRecord> addresses = elasticIpAddressesService.describeAddresses(userId, null);
		return getKoalaJsonParser().getJson(addresses);
	}
}
