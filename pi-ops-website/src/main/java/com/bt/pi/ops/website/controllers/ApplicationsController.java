package com.bt.pi.ops.website.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.AbstractManagedAddressingPiApplication;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.core.application.activation.ActivationAwareApplication;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;
import com.bt.pi.ops.website.entities.ApplicationRecordInfo;

@Component
@Path("/applications")
public class ApplicationsController extends ControllerBase {
	private static final String ERROR = "ERROR";
	private static final String DECIMAL = "%d";
	private static final String NAME = "name";
	private static final String SCOPE = "scope";
	private static final String VALUE = "value";
	private static final String NODE_ID = "nodeId";

	@Resource
	private OpsWebsiteApplicationManager opsWebsiteApplicationManager;
	private ConcurrentHashMap<String, NodeScope> applicationScopeMap;

	public ApplicationsController() {
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/record/{name}/{scope}/{value}")
	public String getApplicationRecord(@PathParam(NAME) String name, @PathParam(SCOPE) NodeScope scope, @PathParam(VALUE) String value) {
		ApplicationRecordInfo applicationRecord = getApplicationRecordInfoFromDht(name, scope, value);
		return getKoalaJsonParser().getJson(applicationRecord);
	}

	private ApplicationRecordInfo getApplicationRecordInfoFromDht(String applicationName, NodeScope nodeScope, String value) {
		PId appPid = getApplicationId(applicationName, nodeScope, value);
		ApplicationRecord applicationRecord = (ApplicationRecord) getDhtClientFactory().createBlockingReader().get(appPid);

		return new ApplicationRecordInfo(nodeScope, value, applicationRecord);
	}

	private PId getApplicationId(String applicationName, NodeScope nodeScope, String value) {
		PId appPid = null;
		if (nodeScope.equals(NodeScope.REGION))
			appPid = getKoalaIdFactory().buildPId(RegionScopedApplicationRecord.getUrl(applicationName)).forRegion(Integer.parseInt(value));
		else if (nodeScope.equals(NodeScope.AVAILABILITY_ZONE))
			appPid = getKoalaIdFactory().buildPId(AvailabilityZoneScopedApplicationRecord.getUrl(applicationName)).forGlobalAvailablityZoneCode(Integer.parseInt(value));
		return appPid;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/list")
	public String getApplicationRecordList() {
		Collection<AbstractManagedAddressingPiApplication> applications = opsWebsiteApplicationManager.getManageableSharedResources();
		List<ApplicationRecordInfo> applicationRecords = new ArrayList<ApplicationRecordInfo>();
		applicationScopeMap = new ConcurrentHashMap<String, NodeScope>();
		for (ActivationAwareApplication application : applications) {
			ApplicationActivator applicationActivator = application.getApplicationActivator();
			if (applicationActivator instanceof SharedRecordConditionalApplicationActivator) {
				SharedRecordConditionalApplicationActivator sharedRecordConditionalApplicationActivator = (SharedRecordConditionalApplicationActivator) applicationActivator;
				NodeScope applicationScope = sharedRecordConditionalApplicationActivator.getActivationScope();
				applicationScopeMap.putIfAbsent(application.getApplicationName(), applicationScope);
				List<String> scopeValues = getAllValuesForScope(applicationScope);
				for (String scopeValue : scopeValues) {
					applicationRecords.add(getApplicationRecordInfoFromDht(application.getApplicationName(), sharedRecordConditionalApplicationActivator.getActivationScope(), scopeValue));
				}
			}
		}
		return getKoalaJsonParser().getJson(applicationRecords);
	}

	private List<String> getAllValuesForScope(NodeScope applicationScope) {
		List<String> scopeValues = new ArrayList<String>();
		if (applicationScope == NodeScope.REGION) {
			Regions regions = getRegionsFromDht();
			Collection<Region> regionList = regions.getRegions().values();
			for (Region region : regionList) {
				scopeValues.add(String.format(DECIMAL, region.getRegionCode()));
			}
		} else if (applicationScope == NodeScope.AVAILABILITY_ZONE) {
			AvailabilityZones availabilityZones = getAvailabilityZonesFromDht();
			for (AvailabilityZone availavilibyZone : availabilityZones.getAvailabilityZones().values()) {
				scopeValues.add(String.format(DECIMAL, availavilibyZone.getGlobalAvailabilityZoneCode()));
			}
		}
		return scopeValues;
	}

	@GET
	@Path("/deactivate/{name}/{nodeId}")
	public String deActivateApplication(@PathParam(NAME) String applicationName, @PathParam(NODE_ID) String nodeId) {
		if (applicationScopeMap == null)
			getApplicationRecordList();
		NodeScope nodeScope = applicationScopeMap.get(applicationName);
		if (nodeScope == null)
			return ERROR;
		String value = getScopeValueFromNodeId(nodeId, nodeScope);
		ApplicationRecordInfo applicationRecordInfo = getApplicationRecordInfoFromDht(applicationName, nodeScope, value);
		applicationRecordInfo.removeActiveNode(nodeId);
		PId applicationId = getApplicationId(applicationName, nodeScope, value);
		if (applicationId == null)
			return ERROR;
		applicationRecordInfo.updateApplicationRecord(getDhtClientFactory().createBlockingWriter(), applicationId);
		return "OK";
	}

	private String getScopeValueFromNodeId(String nodeId, NodeScope nodeScope) {
		PId nodePId = getPiIdBuilder().getNodeIdFromNodeId(nodeId);
		if (nodeScope == NodeScope.REGION) {
			return String.format(DECIMAL, nodePId.getRegion());
		} else if (nodeScope == NodeScope.AVAILABILITY_ZONE) {
			return String.format(DECIMAL, nodePId.getAvailabilityZone());
		}
		return null;
	}

	private Regions getRegionsFromDht() {
		PId regionsPId = getPiIdBuilder().getRegionsId();
		return getBlockingDhtCache().get(regionsPId);
	}

	private AvailabilityZones getAvailabilityZonesFromDht() {
		PId availabilityZonesPId = getPiIdBuilder().getAvailabilityZonesId();
		return getBlockingDhtCache().get(availabilityZonesPId);
	}
}
