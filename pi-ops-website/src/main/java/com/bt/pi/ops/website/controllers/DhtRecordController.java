package com.bt.pi.ops.website.controllers;

import java.util.Locale;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

@Component
@Path("/dhtrecords")
public class DhtRecordController extends ControllerBase {
	private static final String PID_S = "Pid: %s";
	private static final String S_COLON_S = "%s:%s";
	private static final String NO_URI_ID_IN_REQUEST = "No URI id in request";
	private static final Log LOG = LogFactory.getLog(DhtRecordController.class);
	private static final String ID = "id";
	private static final String SCOPE = "scope";
	private static final String SCHEME = "scheme";

	public DhtRecordController() {
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/scopes/{scope}/{scheme}/{id}")
	public String getLocalDhtRecord(@PathParam(SCOPE) String scope, @PathParam(SCHEME) String scheme, @PathParam(ID) String id) {
		LOG.debug(String.format("getDhtRecord(%s, %s, %s)", scope, scheme, id));

		if (!StringUtils.hasText(scope) || !StringUtils.hasText(scheme) || !StringUtils.hasText(id))
			throw new IllegalArgumentException(String.format("One or more blank fields in request: %s, %s, %s", scope, scheme, id));

		NodeScope nodeScope = NodeScope.valueOf(scope.toUpperCase(Locale.ENGLISH));

		if (NodeScope.GLOBAL.equals(nodeScope)) {
			return getGlobalDhtRecord(scheme, id);
		}

		String uri = String.format(S_COLON_S, getResourceScheme(scheme), id);
		PiLocation piLocation = new PiLocation(uri, nodeScope);
		LOG.debug(String.format("url: %s", piLocation.getUrl()));

		PId dhtId = null;

		if (NodeScope.REGION.equals(nodeScope))
			dhtId = getKoalaIdFactory().buildPId(piLocation.getUrl()).forLocalRegion();
		else
			dhtId = getKoalaIdFactory().buildPId(piLocation.getUrl()).forLocalAvailabilityZone();
		LOG.debug(String.format(PID_S, dhtId.toStringFull()));
		return getDhtRecordAsString(dhtId);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/global/{scheme}/{id}")
	public String getGlobalDhtRecord(@PathParam(SCHEME) String scheme, @PathParam(ID) String id) {
		LOG.debug(String.format("getDhtRecord(%s, %s)", scheme, id));

		if (!StringUtils.hasText(id))
			throw new IllegalArgumentException(String.format(NO_URI_ID_IN_REQUEST));

		String uri = String.format(S_COLON_S, getResourceScheme(scheme), id);
		LOG.debug("uri: " + uri);

		PId dhtId = getKoalaIdFactory().buildPId(uri);
		LOG.debug(String.format(PID_S, dhtId.toStringFull()));
		return getDhtRecordAsString(dhtId);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/regions/{region}/{scheme}/{id}")
	public String getDhtRecordForRegion(@PathParam("region") String regionName, @PathParam(SCHEME) String scheme, @PathParam(ID) String id) {
		LOG.debug(String.format("getDhtRecordForRegion(%s, %s, %s)", regionName, scheme, id));

		if (!StringUtils.hasText(id))
			throw new IllegalArgumentException(String.format(NO_URI_ID_IN_REQUEST));

		int regionCode = getRegionCodeFromRegionName(regionName);

		String uri = String.format(S_COLON_S, getResourceScheme(scheme), id);

		PId dhtId = getKoalaIdFactory().buildPId(uri).forRegion(regionCode);
		return getDhtRecordAsString(dhtId);
	}

	private String getResourceScheme(String scheme) {
		for (ResourceSchemes rs : ResourceSchemes.values()) {
			if (rs.toString().toLowerCase(Locale.ENGLISH).equals(scheme.toLowerCase(Locale.ENGLISH)))
				return rs.toString();
		}
		throw new IllegalArgumentException(String.format("Unknown scheme: %s", scheme));
	}

	private int getRegionCodeFromRegionName(String regionName) {
		PId regionsId = getPiIdBuilder().getRegionsId();
		Regions regions = getBlockingDhtCache().get(regionsId);
		Region region = regions.getRegion(regionName);

		if (region == null)
			throw new IllegalArgumentException(String.format("Region %s not found", regionName));
		int regionCode = region.getRegionCode();
		return regionCode;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/availabilityzones/{availabilityzone}/{scheme}/{id}")
	public String getDhtRecordForAvailabilityZone(@PathParam("availabilityzone") String availabilityZoneName, @PathParam(SCHEME) String scheme, @PathParam(ID) String id) {
		LOG.debug(String.format("getDhtRecordForAvailabilityZone(%s, %s, %s)", availabilityZoneName, scheme, id));

		if (!StringUtils.hasText(id))
			throw new IllegalArgumentException(String.format(NO_URI_ID_IN_REQUEST));

		PId availabilityZonesId = getPiIdBuilder().getAvailabilityZonesId();
		AvailabilityZones availabilityZones = getBlockingDhtCache().get(availabilityZonesId);
		AvailabilityZone availabilityZone = availabilityZones.getAvailabilityZoneByName(availabilityZoneName);
		if (availabilityZone == null)
			throw new IllegalArgumentException(String.format("Availability zone %s not found", availabilityZoneName));
		int globalAvailabilityZoneCode = availabilityZone.getGlobalAvailabilityZoneCode();

		String uri = String.format(S_COLON_S, getResourceScheme(scheme), id);

		PId dhtId = getKoalaIdFactory().buildPId(uri).forGlobalAvailablityZoneCode(globalAvailabilityZoneCode);
		return getDhtRecordAsString(dhtId);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/instancetypes")
	public String getInstanceTypes() {
		LOG.debug(String.format("getInstanceTypes()"));

		PId dhtId = getPiIdBuilder().getPId(InstanceTypes.URL_STRING);
		return getDhtRecordAsString(dhtId);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/regions")
	public String getRegions() {
		LOG.debug(String.format("getRegions()"));

		PId dhtId = getPiIdBuilder().getRegionsId();
		return getDhtRecordAsString(dhtId);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/availabilityzones")
	public String getAvailabilityZones() {
		LOG.debug(String.format("getAvailabilityZones()"));

		PId dhtId = getPiIdBuilder().getAvailabilityZonesId();
		return getDhtRecordAsString(dhtId);
	}

	private String getDhtRecordAsString(PId dhtId) {
		BlockingDhtReader dhtReader = getDhtClientFactory().createBlockingReader();
		PiEntity piEntity = dhtReader.get(dhtId);
		return getKoalaJsonParser().getJson(piEntity);
	}
}
