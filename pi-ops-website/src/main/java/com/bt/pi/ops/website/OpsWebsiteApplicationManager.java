/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.ops.website;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.bt.pi.api.service.ApiApplicationManager;
import com.bt.pi.app.common.AbstractManagedAddressingPiApplication;
import com.bt.pi.app.common.AbstractPublicManagedAddressingPiApplication;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.activation.RegionScopedSharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.conf.Property;
import com.bt.pi.sss.PisssApplicationManager;

@Component
public class OpsWebsiteApplicationManager extends AbstractPublicManagedAddressingPiApplication implements ApplicationContextAware {

	public static final String APPLICATION_NAME = "pi-ops-website";
	private static final Log LOG = LogFactory.getLog(OpsWebsiteApplicationManager.class);
	private int appPort;
	private ResourceHandler staticResourceHandler;
	private RegionScopedSharedRecordConditionalApplicationActivator applicationActivator;
	private ApplicationContext applicationContext;

	public OpsWebsiteApplicationManager() {
		super(APPLICATION_NAME);
		appPort = -1;
		staticResourceHandler = null;
		applicationContext = null;

	}

	@Resource
	public void setStaticResourceHandler(ResourceHandler resourceHandler) {
		this.staticResourceHandler = resourceHandler;
	}

	@Override
	protected int getPort() {
		return appPort;
	}

	@Property(key = "pi-ops-website.port", defaultValue = "8443")
	public void setWebsitePort(int value) {
		appPort = value;
	}

	@Override
	@Property(key = "pi-ops-website.app.activation.check.period.secs", defaultValue = DEFAULT_ACTIVATION_CHECK_PERIOD_SECS)
	public void setActivationCheckPeriodSecs(int value) {
		super.setActivationCheckPeriodSecs(value);
	}

	@Override
	@Property(key = "pi-ops-website.app.start.timeout.millis", defaultValue = DEFAULT_START_TIMEOUT_MILLIS)
	public void setStartTimeoutMillis(long value) {
		super.setStartTimeoutMillis(value);
	}

	@Override
	public void handleNodeDeparture(String nodeId) {
		LOG.warn(String.format("Application: %s Node: %s has left the ring!", APPLICATION_NAME, nodeId));
		removeNodeIdFromApplicationRecord(nodeId);
		forceActivationCheck();
	}

	@Resource(type = RegionScopedSharedRecordConditionalApplicationActivator.class)
	public void setSharedRecordConditionalApplicationActivator(RegionScopedSharedRecordConditionalApplicationActivator aSharedRecordConditionalApplicationActivator) {
		applicationActivator = aSharedRecordConditionalApplicationActivator;
	}

	@Override
	public SharedRecordConditionalApplicationActivator getActivatorFromApplication() {
		return applicationActivator;
	}

	@Override
	protected void onApplicationStarting() {
		super.onApplicationStarting();
		String staticResourcePath = staticResourceHandler.getResourceBase();
		LOG.info(String.format("Starting with static resource path of %s", staticResourcePath));
	}

	@Override
	public void setApplicationContext(ApplicationContext anApplicationContext) {
		this.applicationContext = anApplicationContext;
	}

	public Collection<AbstractManagedAddressingPiApplication> getManageableSharedResources() {
		Collection<AbstractManagedAddressingPiApplication> applications = applicationContext.getBeansOfType(AbstractManagedAddressingPiApplication.class).values();

		return applications;
	}

	@Override
	public List<String> getPreferablyExcludedApplications() {
		return Arrays.asList(PisssApplicationManager.APPLICATION_NAME, ApiApplicationManager.APPLICATION_NAME, NetworkManagerApplication.APPLICATION_NAME);
	}

}
