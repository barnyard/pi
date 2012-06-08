package com.bt.pi.ops.website;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.p2p.commonapi.Id;

import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.common.net.iptables.ManagedAddressingApplicationIpTablesManager;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.RegionScopedSharedRecordConditionalApplicationActivator;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.scribe.KoalaScribeImpl;

@RunWith(MockitoJUnitRunner.class)
public class OpsWebsiteApplicationManagerTest {
	private OpsWebsiteApplicationManager opsWebsiteApplicationManager;
	@Mock
	private KoalaIdFactory koalaIdFactory;
	@Mock
	private KoalaScribeImpl aScribe;
	@Mock
	private RegionScopedSharedRecordConditionalApplicationActivator applicationActivator;
	@Mock
	private ApplicationRegistry applicationRegistry;
	@Mock
	private ApplicationRecord applicationRecord;
	@Mock
	private Id nodeId;
	private String publicIpAddress = "1.2.3.4";
	@Mock
	private NetworkCommandRunner networkCommandRunner;
	@Mock
	private ManagedAddressingApplicationIpTablesManager ipTablesManager;
	@Mock
	private ResourceHandler resourceHandler;

	@Before
	public void setup() {
		opsWebsiteApplicationManager = spy(new OpsWebsiteApplicationManager());
		opsWebsiteApplicationManager.setScribe(aScribe);
		opsWebsiteApplicationManager.setNetworkCommandRunner(networkCommandRunner);
		opsWebsiteApplicationManager.setManagedAddressingApplicationIpTablesManager(ipTablesManager);
		opsWebsiteApplicationManager.setKoalaIdFactory(koalaIdFactory);
		opsWebsiteApplicationManager.setStaticResourceHandler(resourceHandler);

		when(opsWebsiteApplicationManager.getActivatorFromApplication()).thenReturn(applicationActivator);
		when(applicationActivator.getApplicationRegistry()).thenReturn(applicationRegistry);
		when(applicationRegistry.getCachedApplicationRecord(OpsWebsiteApplicationManager.APPLICATION_NAME)).thenReturn(applicationRecord);
		when(applicationRecord.getAssociatedResource(nodeId)).thenReturn(publicIpAddress);
	}

	@Test
	public void shouldGetAndSetAppPort() {
		// act
		opsWebsiteApplicationManager.setWebsitePort(123);

		// assert
		assertEquals(123, opsWebsiteApplicationManager.getPort());
	}

	@Test
	public void getApplicationName() throws Exception {
		// act
		String applicationName = opsWebsiteApplicationManager.getApplicationName();

		// assert
		assertThat(applicationName, equalTo("pi-ops-website"));
	}

	@Test
	public void getStartTimeout() throws Exception {
		// setup
		opsWebsiteApplicationManager.setStartTimeoutMillis(123);

		// act
		long result = opsWebsiteApplicationManager.getStartTimeout();

		// assert
		assertThat(result, equalTo(123L));
	}

	@Test
	public void getActivationCheckPeriod() throws Exception {
		// setup
		opsWebsiteApplicationManager.setActivationCheckPeriodSecs(123);

		// act
		int result = opsWebsiteApplicationManager.getActivationCheckPeriodSecs();

		// assert
		assertThat(result, equalTo(123));
	}

	@Test
	public void shouldRemoveNodeFromApplicationRecordAndForceCheck() {
		// setup
		String anotherNodeId = "anotherNode";
		doNothing().when(opsWebsiteApplicationManager).removeNodeIdFromApplicationRecord(anotherNodeId);
		doNothing().when(opsWebsiteApplicationManager).forceActivationCheck();

		// act
		opsWebsiteApplicationManager.handleNodeDeparture(anotherNodeId);

		// assert
		verify(opsWebsiteApplicationManager).removeNodeIdFromApplicationRecord(anotherNodeId);
		verify(opsWebsiteApplicationManager).forceActivationCheck();
	}

	@Test
	public void shouldPrintStartingMessage() {
		// act
		opsWebsiteApplicationManager.onApplicationStarting();

		// assert
		verify(resourceHandler).getResourceBase();
	}
}
