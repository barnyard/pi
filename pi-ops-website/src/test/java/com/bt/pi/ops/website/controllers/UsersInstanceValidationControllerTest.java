package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.task.TaskExecutor;

import com.bt.pi.api.service.InstancesServiceImpl;
import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceActivityState;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.handlers.PauseInstanceServiceHelper;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.continuation.scattergather.UpdateResolvingPiScatterGatherContinuation;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;
import com.bt.pi.ops.website.entities.ReadOnlyInstance;
import com.bt.pi.ops.website.entities.ReadOnlyUser;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@RunWith(MockitoJUnitRunner.class)
public class UsersInstanceValidationControllerTest {
	private final static String USERNAME = "username";
	private static final String PID = "ABCD00112233445566778899";
	private ReadOnlyUser readOnlyUser;
	@Mock
	private User user;
	@Mock
	private DhtClientFactory dhtClientFactory;
	@Mock
	private UserManagementService userManagementService;
	private String[] arrayOfStrings = new String[] {};
	@InjectMocks
	private UsersInstanceValidationController usersController = new UsersInstanceValidationController();
	@Mock
	private InstancesServiceImpl instancesServiceImpl;
	@Mock
	private PiIdBuilder piIdBuilder;
	@Mock
	private ScatterGatherContinuationRunner scatterGatherContinuationRunner;
	@Mock
	private DhtWriter dhtWriter;
	@Mock
	private OpsWebsiteApplicationManager opsWebsiteApplicationManager;
	@Mock
	private MessageContext messageContext;
	@Mock
	private BlockingDhtReader blockingReader;
	private String nodeId = "1213232142341241235315125125";
	@Mock
	private PId nodePid;
	@Mock
	private TaskExecutor taskExecutor;
	@Mock
	private PauseInstanceServiceHelper pauseInstanceServiceHelper;

	@SuppressWarnings("unchecked")
	@Before
	public void doBefore() throws Exception {
		readOnlyUser = new ReadOnlyUser(user);

		when(userManagementService.getUser(USERNAME)).thenReturn(user);
		when(user.getInstanceIds()).thenReturn(arrayOfStrings);
		when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
		when(dhtClientFactory.createBlockingReader()).thenReturn(blockingReader);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Collection<ScatterGatherContinuationRunnable> runnables = (Collection<ScatterGatherContinuationRunnable>) invocation.getArguments()[0];
				CountDownLatch latch = new CountDownLatch(runnables.size());
				for (ScatterGatherContinuationRunnable runnable : runnables) {
					runnable.setLatch(latch);
					runnable.run();
				}
				return null;
			}
		}).when(scatterGatherContinuationRunner).execute(anyCollection(), eq(2L), eq(TimeUnit.MINUTES));

		when(opsWebsiteApplicationManager.newMessageContext()).thenReturn(messageContext);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Runnable runnable = (Runnable) invocation.getArguments()[0];
				runnable.run();
				// new Thread(runnable).start();
				return null;
			}
		}).when(taskExecutor).execute(isA(Runnable.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testGetUserWithInstancesForValidation() {
		// setup
		PId userPid = mock(PId.class);
		when(userPid.toStringFull()).thenReturn(PID);
		when(piIdBuilder.getPId(User.getUrl(USERNAME))).thenReturn(userPid);
		Map<String, Set<Instance>> reservationMap = new HashMap<String, Set<Instance>>();

		Set<Instance> set1 = new HashSet<Instance>();
		Instance instance1 = mock(Instance.class);
		when(instance1.getState()).thenReturn(InstanceState.RUNNING);
		set1.add(instance1);
		Instance instance2 = mock(Instance.class);
		when(instance2.getState()).thenReturn(InstanceState.RUNNING);
		set1.add(instance2);

		Set<Instance> set2 = new HashSet<Instance>();
		Instance instance3 = mock(Instance.class);
		when(instance3.getState()).thenReturn(InstanceState.TERMINATED);
		set2.add(instance3);
		Instance instance4 = mock(Instance.class);
		when(instance4.getState()).thenReturn(InstanceState.RUNNING);
		set2.add(instance4);

		reservationMap.put("r1", set1);
		reservationMap.put("r2", set2);
		when(instancesServiceImpl.describeInstances(eq(USERNAME), anyCollection())).thenReturn(reservationMap);

		// act
		Viewable result = usersController.getUserWithInstancesForValidation(USERNAME, PID);

		// assert
		assertNotNull(result);
		assertEquals("user_instances_validation", result.getTemplateName());
		assertNotNull(result.getModel());
		Map<String, Object> model = (Map<String, Object>) result.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		Collection instances = (Collection) model.get("instances");
		assertNotNull(instances);
		assertEquals(3, instances.size());

		assertTrue(instances.contains(new ReadOnlyInstance(instance1)));
		assertTrue(instances.contains(new ReadOnlyInstance(instance2)));
		assertTrue(instances.contains(new ReadOnlyInstance(instance4)));

		assertEquals(PID, model.get("pid"));
	}

	@Test(expected = NotFoundException.class)
	public void shouldCheckThatPidMatchedUsernameonGet() {
		// setup
		PId bogusPid = mock(PId.class);
		when(bogusPid.toStringFull()).thenReturn("ABCD");
		when(piIdBuilder.getPId(User.getUrl(USERNAME))).thenReturn(bogusPid);

		// act
		usersController.getUserWithInstancesForValidation(USERNAME, PID);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void shouldReValidateUserInstances() throws Exception {
		// setup
		PId userPid = mock(PId.class);
		when(userPid.toStringFull()).thenReturn(PID);
		when(piIdBuilder.getPId(User.getUrl(USERNAME))).thenReturn(userPid);
		Map<String, Set<Instance>> reservationMap = new HashMap<String, Set<Instance>>();

		Set<Instance> set1 = new HashSet<Instance>();
		Integer globalAvzCode = 345;
		when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(isA(String.class))).thenReturn(globalAvzCode);

		String instanceId1 = "i-1111";
		final Instance instance1 = mock(Instance.class);
		when(instance1.getState()).thenReturn(InstanceState.RUNNING);
		when(instance1.getInstanceId()).thenReturn(instanceId1);
		when(instance1.getNodeId()).thenReturn(nodeId);
		when(instance1.getInstanceActivityState()).thenReturn(InstanceActivityState.RED);
		set1.add(instance1);
		final PId instancePid1 = mock(PId.class);
		when(piIdBuilder.getPId(Instance.getUrl(instanceId1))).thenReturn(instancePid1);

		when(instancePid1.forGlobalAvailablityZoneCode(globalAvzCode)).thenReturn(instancePid1);
		when(instancePid1.toStringFull()).thenReturn("1111111111111111111111111111111111111111111111");

		String instanceId2 = "i-2222";
		final Instance instance2 = mock(Instance.class);
		when(instance2.getState()).thenReturn(InstanceState.RUNNING);
		when(instance2.getInstanceId()).thenReturn(instanceId2);
		when(instance2.getNodeId()).thenReturn(nodeId);
		set1.add(instance2);
		final PId instancePid2 = mock(PId.class);
		when(piIdBuilder.getPId(Instance.getUrl(instanceId2))).thenReturn(instancePid2);
		when(instancePid2.forGlobalAvailablityZoneCode(globalAvzCode)).thenReturn(instancePid2);
		when(instancePid2.toStringFull()).thenReturn("2222222222222222222222222222222222222222222222");

		reservationMap.put("r1", set1);

		Set<Instance> set2 = new HashSet<Instance>();

		String instanceId3 = "i-3333";
		final Instance instance3 = mock(Instance.class);
		when(instance3.getState()).thenReturn(InstanceState.RUNNING);
		when(instance3.getInstanceId()).thenReturn(instanceId3);
		when(instance3.getNodeId()).thenReturn(nodeId);
		when(instance3.getInstanceActivityState()).thenReturn(InstanceActivityState.AMBER);
		set2.add(instance3);
		final PId instancePid3 = mock(PId.class);
		when(piIdBuilder.getPId(Instance.getUrl(instanceId3))).thenReturn(instancePid3);
		when(instancePid3.forGlobalAvailablityZoneCode(globalAvzCode)).thenReturn(instancePid3);
		when(instancePid3.toStringFull()).thenReturn("333333333333333333333333333333333333333333333333");

		reservationMap.put("r2", set2);

		when(instancesServiceImpl.describeInstances(eq(USERNAME), anyCollection())).thenReturn(reservationMap);

		MultivaluedMap<String, String> formParams = new MultivaluedMapImpl();
		formParams.putSingle("validate_" + instanceId1, "true");
		formParams.putSingle("validate_" + instanceId2, "false");
		formParams.putSingle("validate_" + instanceId3, "true");

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				PId pid = (PId) invocation.getArguments()[0];
				UpdateResolvingPiScatterGatherContinuation continuation = (UpdateResolvingPiScatterGatherContinuation) invocation.getArguments()[1];
				if (instancePid1.equals(pid)) {
					Object updated = continuation.update(instance1, null);
					continuation.handleResult(updated);
				}
				if (instancePid3.equals(pid)) {
					Object updated = continuation.update(instance3, null);
					continuation.handleResult(updated);
				}
				return null;
			}
		}).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingContinuation.class));

		when(piIdBuilder.getPId(instance1)).thenReturn(instancePid1);
		when(piIdBuilder.getPId(instance3)).thenReturn(instancePid3);
		when(blockingReader.get(instancePid1)).thenReturn(instance1);
		when(blockingReader.get(instancePid3)).thenReturn(instance3);

		when(piIdBuilder.getNodeIdFromNodeId(nodeId)).thenReturn(nodePid);

		// act
		Viewable result = usersController.validateUserInstances(formParams, USERNAME, PID);
		Thread.sleep(1000);

		// assert
		verify(instance1).setInstanceActivityState(InstanceActivityState.GREEN);
		verify(instance2, never()).setInstanceActivityState(isA(InstanceActivityState.class));
		verify(instance3).setInstanceActivityState(InstanceActivityState.GREEN);

		assertNotNull(result);
		assertEquals("user_instances_validation", result.getTemplateName());
		assertNotNull(result.getModel());
		Map<String, Object> model = (Map<String, Object>) result.getModel();
		assertEquals(readOnlyUser, model.get("user"));
		Collection instances = (Collection) model.get("instances");
		assertNotNull(instances);
		assertEquals(3, instances.size());

		assertTrue(instances.contains(new ReadOnlyInstance(instance1)));
		assertTrue(instances.contains(new ReadOnlyInstance(instance2)));
		assertTrue(instances.contains(new ReadOnlyInstance(instance3)));

		assertEquals(PID, model.get("pid"));

		verify(pauseInstanceServiceHelper).unPauseInstance(instance1);
		verify(pauseInstanceServiceHelper, never()).unPauseInstance(instance3); // because old state was AMBER
	}

	@Test(expected = NotFoundException.class)
	public void shouldCheckThatPidMatchedUsernameOnPost() {
		// setup
		PId bogusPid = mock(PId.class);
		when(bogusPid.toStringFull()).thenReturn("ABCD");
		when(piIdBuilder.getPId(User.getUrl(USERNAME))).thenReturn(bogusPid);
		MultivaluedMap<String, String> formParams = new MultivaluedMapImpl();

		// act
		usersController.validateUserInstances(formParams, USERNAME, PID);
	}
}
