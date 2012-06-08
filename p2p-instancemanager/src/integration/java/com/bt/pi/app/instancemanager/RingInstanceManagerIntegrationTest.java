package com.bt.pi.app.instancemanager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.libvirt.Domain;
import org.springframework.util.ReflectionUtils;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceActivityState;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.common.testing.StubDeviceUtils;
import com.bt.pi.app.instancemanager.handlers.PauseInstanceTaskQueueWatcherInitiator;
import com.bt.pi.app.instancemanager.handlers.TerminateInstanceTaskQueueWatcherInitiator;
import com.bt.pi.app.instancemanager.testing.StubMailSender;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.resource.ConsumedUriResourceRegistry;
import com.bt.pi.core.continuation.LoggingContinuation;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

public class RingInstanceManagerIntegrationTest extends InstanceManagerApplicationIntegrationTestBase {
    @Rule
    public TestName testName = new TestName();
    private static final Log LOG = LogFactory.getLog(RingInstanceManagerIntegrationTest.class);
    private static final String STUB_DEVICE_UTILS = "stubDeviceUtils";
    private static final String CONSUMED_DHT_RESOURCE_REGISTRY = "consumedDhtResourceRegistry";
    private static final String CONSUMED_URI_RESOURCE_REGISTRY = "consumedUriResourceRegistry";

    private String initialResourceRefreshIntervalMillisFromProperties;
    private String repeatingResourceRefreshIntervalMillisFromProperties;

    @Before
    public void storeRefreshIntervals() throws Exception {
        LOG.info(String.format("************* running test %s", testName.getMethodName()));
        initialResourceRefreshIntervalMillisFromProperties = properties.getProperty(Instance.INITIAL_INTERVAL_MILLIS_PROPERTY);
        repeatingResourceRefreshIntervalMillisFromProperties = properties.getProperty(Instance.REPEATING_INTERVAL_MILLIS_PROPERTY);
    }

    @After
    public void resetRefreshIntervals() throws Exception {
        if (initialResourceRefreshIntervalMillisFromProperties != null)
            properties.setProperty(Instance.INITIAL_INTERVAL_MILLIS_PROPERTY, initialResourceRefreshIntervalMillisFromProperties);
        else
            properties.remove(Instance.INITIAL_INTERVAL_MILLIS_PROPERTY);

        if (repeatingResourceRefreshIntervalMillisFromProperties != null)
            properties.setProperty(Instance.REPEATING_INTERVAL_MILLIS_PROPERTY, repeatingResourceRefreshIntervalMillisFromProperties);
        else
            properties.remove(Instance.REPEATING_INTERVAL_MILLIS_PROPERTY);

        LOG.info(String.format("************** stopping test %s", testName.getMethodName()));
    }

    @After
    public void resetSharedResourceManager() throws Exception {
        ConsumedDhtResourceRegistry consumedDhtResourceRegistry = (ConsumedDhtResourceRegistry) context.getBean(CONSUMED_DHT_RESOURCE_REGISTRY);
        consumedDhtResourceRegistry.deregisterConsumer(instancePastryId, instance.getUserId());
    }

    @After
    public void resetImmutableResourceManager() throws Exception {
        ConsumedUriResourceRegistry consumedUriResourceRegistry = (ConsumedUriResourceRegistry) context.getBean(CONSUMED_URI_RESOURCE_REGISTRY);
        consumedUriResourceRegistry.clearResource(URI.create(String.format("%s:%d", ResourceSchemes.VIRTUAL_NETWORK, instance.getVlanId())));
    }

    @After
    public void resetDeviceUtils() throws Exception {
        StubDeviceUtils stubDeviceUtils = (StubDeviceUtils) context.getBean(STUB_DEVICE_UTILS);
        stubDeviceUtils.setDeviceAlwaysExists(false);
    }

    @After
    public void resetQueueWatcher() {
        terminateInstanceTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(TerminateInstanceTaskQueueWatcherInitiator.THIRTY_MINUTE);
        terminateInstanceTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(TerminateInstanceTaskQueueWatcherInitiator.THIRTY_MINUTE);
        terminateInstanceTaskQueueWatcherInitiator.setStaleQueueItemMillis(TerminateInstanceTaskQueueWatcherInitiator.THIRTY_MINUTE);
        terminateInstanceTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher("nodeId1234");

        runInstanceWatcherManager.setInitialQueueWatcherIntervalMillis(TerminateInstanceTaskQueueWatcherInitiator.THIRTY_MINUTE);
        runInstanceWatcherManager.setRepeatingQueueWatcherIntervalMillis(TerminateInstanceTaskQueueWatcherInitiator.THIRTY_MINUTE);
        runInstanceWatcherManager.setStaleQueueItemMillis(TerminateInstanceTaskQueueWatcherInitiator.THIRTY_MINUTE);
        runInstanceWatcherManager.createTaskProcessingQueueWatcher(instanceManagerApplication.getNodeIdFull());

        removeInstanceFromUserTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(TerminateInstanceTaskQueueWatcherInitiator.THIRTY_MINUTE);
        removeInstanceFromUserTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(TerminateInstanceTaskQueueWatcherInitiator.THIRTY_MINUTE);
        removeInstanceFromUserTaskQueueWatcherInitiator.setStaleQueueItemMillis(TerminateInstanceTaskQueueWatcherInitiator.THIRTY_MINUTE);
        removeInstanceFromUserTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerApplication.getNodeIdFull());

        pauseInstanceTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(PauseInstanceTaskQueueWatcherInitiator.FOUR_AND_HALF_MINS);
        pauseInstanceTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(PauseInstanceTaskQueueWatcherInitiator.FOUR_AND_HALF_MINS);
        pauseInstanceTaskQueueWatcherInitiator.setStaleQueueItemMillis(PauseInstanceTaskQueueWatcherInitiator.FIVE_MINUTE);
        pauseInstanceTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerApplication.getNodeIdFull());
    }

    @Test
    public void testRunInstance() throws Exception {
        // setup
        SystemState startSystemState = new SystemState(systemResourceState.getFreeCores(), systemResourceState.getFreeMemoryInMB());
        PubSubMessageContext pubSubMessageContext = setupMessageContextForRunInstance();
        Reservation reservation = setupReservationForRunInstance();

        // act
        instanceManagerApplication.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, reservation);

        waitForInstanceStateChange(InstanceState.RUNNING);

        // assert
        assertThatTheRightCommandsAreExecutedForRunInstance();
        assertThatTheRightLibvirtApiIsInvokedForRunInstance();
        assertThatInstanceStateInDhtIs(instancePastryId, InstanceState.RUNNING);
        assertThatSystemResourcesHaveBeenConsumed(startSystemState);
    }

    @Test
    public void shouldRunInstanceByProcessingQueueItem() throws Exception {
        // setup
        SystemState startSystemState = new SystemState(systemResourceState.getFreeCores(), systemResourceState.getFreeMemoryInMB());
        PId runInstanceQueueId = piIdBuilder.getPiQueuePId(PiQueue.RUN_INSTANCE).forLocalScope(PiQueue.RUN_INSTANCE.getNodeScope());
        updateInstanceState(InstanceState.PENDING, "random");

        // act
        taskProcessingQueueHelper.addUrlToQueue(runInstanceQueueId, instance.getUrl());
        wakeUpRunInstanceQueue();

        waitForInstanceStateChange(InstanceState.RUNNING);

        // assert
        assertThatTheRightCommandsAreExecutedForRunInstance();
        assertThatTheRightLibvirtApiIsInvokedForRunInstance();
        assertThatInstanceStateInDhtIs(instancePastryId, InstanceState.RUNNING);
        assertThatSystemResourcesHaveBeenConsumed(startSystemState);
    }

    @Test
    public void shouldSetInstanceStateToFailedWhenQueueRetriesAreExhuasted() throws Exception {
        // setup
        PId runInstanceQueueId = piIdBuilder.getPiQueuePId(PiQueue.RUN_INSTANCE).forLocalScope(PiQueue.RUN_INSTANCE.getNodeScope());
        updateInstanceState(InstanceState.PENDING, null);

        // act
        taskProcessingQueueHelper.addUrlToQueue(runInstanceQueueId, instance.getUrl(), 0);
        wakeUpRunInstanceQueue();

        // assert
        waitForInstanceStateChange(InstanceState.FAILED);
    }

    @Test
    public void shouldSetUserInstanceToBeTerminatedWhenQueueRetriesAreExchausted() throws Exception {
        // setup
        PId runInstanceQueueId = piIdBuilder.getPiQueuePId(PiQueue.RUN_INSTANCE).forLocalScope(PiQueue.RUN_INSTANCE.getNodeScope());
        updateInstanceState(InstanceState.PENDING, null);
        clearQueue(PiQueue.REMOVE_INSTANCE_FROM_USER);

        // act
        taskProcessingQueueHelper.addUrlToQueue(runInstanceQueueId, instance.getUrl(), 0);
        wakeUpRunInstanceQueue();

        // assert
        waitForInstanceStateChange(InstanceState.FAILED);
        Thread.sleep(2000);
        assertUserHasTerminatedInstance();
        assertQueueSize(PiQueue.REMOVE_INSTANCE_FROM_USER, 1);
    }

    private void assertUserHasTerminatedInstance() {
        User user = (User) dhtClientFactory.createBlockingReader().get(piIdBuilder.getPId(User.getUrl(USER_NAME)));
        assertTrue(user.getTerminatedInstanceIds().contains(instanceId));
        assertFalse(Arrays.asList(user.getInstanceIds()).contains(instanceId));
    }

    private void updateInstanceState(final InstanceState newState, final String nodeId) {
        dhtClientFactory.createBlockingWriter().update(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId)), null, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                if (null == existingEntity)
                    return null;
                existingEntity.setState(newState);
                existingEntity.setNodeId(nodeId);
                return existingEntity;
            }

            @Override
            public void handleResult(Instance result) {
            }
        });
    }

    private void wakeUpRunInstanceQueue() throws Exception {
        Thread.sleep(500);
        runInstanceWatcherManager.setInitialQueueWatcherIntervalMillis(100);
        runInstanceWatcherManager.setStaleQueueItemMillis(1000 * 30);
        runInstanceWatcherManager.setRepeatingQueueWatcherIntervalMillis(100);
        runInstanceWatcherManager.createTaskProcessingQueueWatcher(instanceManagerApplication.getNodeIdFull());
    }

    private void wakeUpPauseInstanceQueue() throws Exception {
        Thread.sleep(500);
        pauseInstanceTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(100);
        pauseInstanceTaskQueueWatcherInitiator.setStaleQueueItemMillis(1000 * 30);
        pauseInstanceTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(100);
        pauseInstanceTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerApplication.getNodeIdFull());
    }

    @Test
    public void testTerminateInstance() throws Exception {
        setupImmutableResourceManagerToMimicExistingNetworkForVlanId(instanceId, instance);
        setupSharedResourceManagerToMimicRegisteredInstance(instancePastryId, instance.getInstanceId());
        setupDeviceUtilsToMimicNetworkForRunningInstance();
        stubLibvirtConnection.startDomain("<name>" + instanceId + "</name>");

        SystemState startSystemState = new SystemState(systemResourceState.getFreeCores(), systemResourceState.getFreeMemoryInMB());
        ReceivedMessageContext deleteMessageContext = setupMessageContextForTerminateInstance();

        // act
        instanceManagerApplication.deliver(instancePastryId, deleteMessageContext);

        waitForInstanceStateChange(InstanceState.TERMINATED);

        // assert

        assertThatInstanceStateInDhtIs(instancePastryId, InstanceState.TERMINATED);
        assertThatSystemResourcesHaveBeenReleased(startSystemState);
        assertThatTheRightCommandsAreExecutedForTerminateInstance(instanceId);
        assertThatInstanceIsRemovedFromSharedResourceManager(instance);
    }

    @Test
    public void shouldTerminateInstanceByProcessingTerminateInstanceTaskFromQueue() throws Exception {
        // setup
        instanceManagerApplication.setRetainFaileInstanceArtifacts(false);
        stubLibvirtConnection.startDomain("<name>" + instanceId + "</name>");
        PId terminateInstanceQueueId = piIdBuilder.getPiQueuePId(PiQueue.TERMINATE_INSTANCE).forLocalScope(PiQueue.TERMINATE_INSTANCE.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(terminateInstanceQueueId, instance.getUrl());
        Thread.sleep(500);
        terminateInstanceTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(0);
        terminateInstanceTaskQueueWatcherInitiator.setStaleQueueItemMillis(1);
        terminateInstanceTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher("nodeId1234");

        // assert
        waitForInstanceStateChange(InstanceState.TERMINATED);
        assertQueueSize(PiQueue.TERMINATE_INSTANCE, 0);
    }

    @Test
    public void shouldPauseInstanceByProcessingQueueItem() throws Exception {
        // setup
        instanceManagerApplication.setRetainFaileInstanceArtifacts(false);
        stubLibvirtConnection.startDomain("<name>" + instanceId + "</name>");
        PId pauseInstanceQueueId = piIdBuilder.getPiQueuePId(PiQueue.PAUSE_INSTANCE).forLocalScope(PiQueue.PAUSE_INSTANCE.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(pauseInstanceQueueId, instance.getUrl());
        wakeUpPauseInstanceQueue();

        // assert
        assertTrue(stubLibvirtConnection.waitForLibvirtCommand(String.format("pauseInstance(%s)", instanceId), 61 * 10));
        assertQueueSize(PiQueue.PAUSE_INSTANCE, 0);
    }

    private void assertQueueSize(PiQueue piQueue, int size) {
        LOG.debug(String.format("assertQueueSize(%s, %d)", piQueue, size));
        TaskProcessingQueue taskProcessingQueue = (TaskProcessingQueue) getPiEntityFromDht(piIdBuilder.getPiQueuePId(piQueue).forLocalScope(piQueue.getNodeScope()));
        assertEquals(size, taskProcessingQueue.size());
    }

    @Test
    public void testRebootInstance() throws Exception {
        setupImmutableResourceManagerToMimicExistingNetworkForVlanId(instanceId, instance);
        setupDeviceUtilsToMimicNetworkForRunningInstance();
        Domain mockDomain = stubLibvirtConnection.startDomain("<name>" + instanceId + "</name>");

        ReceivedMessageContext rebootMessageContext = setupMessageContextForInstanceReboot();

        // act
        instanceManagerApplication.deliver(instancePastryId, rebootMessageContext);

        Thread.sleep(3000);

        // assert
        verify(mockDomain).reboot(eq(0));
    }

    @Test
    public void testThatRunningInstanceIsTerminatedByWatcherIfStateInDhtHasBeenUpdated() throws Exception {
        // setup
        String newInstanceId = "i-111";
        Instance newInstance = putInstanceIntoDht(newInstanceId);
        PId newInstancePiKey = piIdBuilder.getPIdForEc2AvailabilityZone(newInstance);
        stubLibvirtConnection.startDomain("<name>" + newInstanceId + "</name>");

        setupSharedResourceManagerToMimicRegisteredInstance(newInstancePiKey, newInstance.getUserId());
        setupImmutableResourceManagerToMimicExistingNetworkForVlanId(newInstanceId, newInstance);
        setupDeviceUtilsToMimicNetworkForRunningInstance();

        SystemState startSystemState = new SystemState(systemResourceState.getFreeCores(), systemResourceState.getFreeMemoryInMB());

        // act
        dhtClientFactory.createBlockingWriter().update(newInstancePiKey, null, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public void handleResult(Instance result) {
            }

            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.setState(InstanceState.TERMINATED);
                return existingEntity;
            }
        });

        Thread.sleep(7000);

        // assert
        assertThatInstanceStateInDhtIs(newInstancePiKey, InstanceState.TERMINATED);
        assertThatSystemResourcesHaveBeenReleased(startSystemState);
        assertThatTheRightCommandsAreExecutedForTerminateInstance(newInstanceId);
    }

    @Test
    public void testThatInstanceManagerRegistersRunningInstancesOnStartup() throws Exception {
        // setup
        instanceManagerApplication.becomePassive();
        Thread.sleep(2000);
        Instance newInstance = putInstanceIntoDht(instanceId);
        PId newInstancePiId = piIdBuilder.getPIdForEc2AvailabilityZone(newInstance);

        stubLibvirtConnection.startDomain("<name>" + instanceId + "</name>");

        // act
        instanceManagerApplication.becomeActive();
        Thread.sleep(5000);

        // assert
        assertThatInstanceIsRegisteredWithSharedResourceManager(newInstancePiId, newInstance);
    }

    @Test
    public void testThatXenRefreshHandlerPicksUpAnInstanceIfNotRegisteredOnBecomeActive() throws Exception {
        // setup
        instanceManagerApplication.becomePassive();
        Thread.sleep(2000);
        stubLibvirtConnection.startDomain("<name>" + instanceId + "</name>");

        // put the new instance in DHT but don't register it with shared resource manager
        String newInstanceId = "i-999";
        Instance newInstance = putInstanceIntoDht(newInstanceId);
        PId newInstancePiKey = piIdBuilder.getPIdForEc2AvailabilityZone(newInstance);

        // act
        instanceManagerApplication.becomeActive();
        Thread.sleep(2 * 1000);

        // start a new domain and wait for the XenRefreshHandler to pick it up
        stubLibvirtConnection.startDomain("<name>" + newInstanceId + "</name>");
        Thread.sleep(10 * 1000);

        // assert
        assertThatInstanceIsRegisteredWithSharedResourceManager(instancePastryId, instance);
        assertThatInstanceIsRegisteredWithSharedResourceManager(newInstancePiKey, newInstance);
    }

    @Test
    public void testThatXenRefreshHandlerUpdatesTheNodeIdAndHostnameIfTheInstanceIsMigrated() throws Exception {
        // setup
        instanceManagerApplication.becomePassive();
        Thread.sleep(2 * 1000);

        String migratedInstanceId = "i-07VO7Z0";
        Instance migratedInstance = putInstanceIntoDht(migratedInstanceId);
        PId migratedInstancePastryId = piIdBuilder.getPIdForEc2AvailabilityZone(migratedInstance);

        stubLibvirtConnection.startDomain("<name>" + migratedInstanceId + "</name>");
        Thread.sleep(2 * 1000);

        // act
        instanceManagerApplication.becomeActive();
        Thread.sleep(2 * 1000);

        // assert
        Instance instanceFromDht = (Instance) getPiEntityFromDht(migratedInstancePastryId);
        assertNotNull(instanceFromDht);
        assertEquals(instanceManagerApplication.getNodeIdFull(), instanceFromDht.getNodeId());
        assertEquals(InetAddress.getLocalHost().getHostName(), instanceFromDht.getHostname());
    }

    @Test
    public void testThatXenRefreshHandlerUpdatesStatusOfCrashedInstance() throws Exception {
        // setup
        instanceManagerApplication.becomePassive();
        Thread.sleep(2 * 1000);

        String crashedInstanceId = "i-07VOCCC";
        Instance crashedInstance = putInstanceIntoDht(crashedInstanceId);
        PId crashedInstancePastryId = piIdBuilder.getPIdForEc2AvailabilityZone(crashedInstance);

        dhtClientFactory.createBlockingWriter().update(crashedInstancePastryId, null, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public void handleResult(Instance result) {
            }

            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.setState(InstanceState.RUNNING);
                return existingEntity;
            }
        });

        stubLibvirtConnection.crashDomain("<name>" + crashedInstanceId + "</name>");
        Thread.sleep(2 * 1000);

        // act
        instanceManagerApplication.becomeActive();
        Thread.sleep(2 * 1000);

        // assert
        Instance instanceFromDht = (Instance) getPiEntityFromDht(crashedInstancePastryId);
        assertNotNull(instanceFromDht);
        assertEquals(InstanceState.CRASHED, instanceFromDht.getState());
    }

    @Test
    public void shouldRemoveInstanceIdFromUserEntityAfterInstanceTermination() throws Exception {
        String newInstanceId = "i-888";

        addInstanceToUser(USER_NAME, newInstanceId);

        Instance newInstance = putInstanceIntoDht(newInstanceId);
        PId newInstancePastryId = piIdBuilder.getPIdForEc2AvailabilityZone(newInstanceId);

        clearQueue(PiQueue.REMOVE_INSTANCE_FROM_USER);

        setupImmutableResourceManagerToMimicExistingNetworkForVlanId(newInstanceId, newInstance);
        setupSharedResourceManagerToMimicRegisteredInstance(newInstancePastryId, newInstance.getInstanceId());
        setupDeviceUtilsToMimicNetworkForRunningInstance();
        stubLibvirtConnection.startDomain("<name>" + newInstanceId + "</name>");

        // check that the user has the instance id
        PId userId = piIdBuilder.getPId(User.getUrl(USER_NAME));
        User user1 = (User) getPiEntityFromDht(userId);
        assertThat(arrayContains(user1.getInstanceIds(), newInstance.getInstanceId()), is(true));

        // act - terminate instance
        ReceivedMessageContext deleteMessageContext = setupMessageContextForTerminateInstance();
        instanceManagerApplication.deliver(newInstancePastryId, deleteMessageContext);

        waitForInstanceStateChange(InstanceState.TERMINATED);

        Thread.sleep(500);
        assertQueueSize(PiQueue.REMOVE_INSTANCE_FROM_USER, 1);
        removeInstanceFromUserTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(100);
        removeInstanceFromUserTaskQueueWatcherInitiator.setStaleQueueItemMillis(1);
        removeInstanceFromUserTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(100);
        removeInstanceFromUserTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerApplication.getNodeIdFull());

        // assert that user entity doesn't have instance Id in it
        waitForUserRecordWithOutInstanceId(userId, instance.getInstanceId());
    }

    private void setField(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }

    @Test
    public void shouldSendEmailToUserWithUnvalidatedInstance() throws Exception {
        // setup
        addInstanceToUser(USER_NAME, instanceId);

        // set last timestamp to eons ago and instance to RUNNING
        dhtClientFactory.createBlockingWriter().update(instancePastryId, null, new UpdateResolver<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                setField(existingEntity, "instanceActivityStateChangeTimestamp", 10000L);
                existingEntity.setState(InstanceState.RUNNING);
                return existingEntity;
            }
        });

        // act
        StubMailSender stubMailSender = context.getBean(StubMailSender.class);
        String message = null;
        int count = 0;
        while (null == (message = stubMailSender.getLastMessage()) && count < 10) {
            count++;
            Thread.sleep(10 * 1000);
        }

        // assert
        assertNotNull(message);
        Instance instance = (Instance) getPiEntityFromDht(instancePastryId);
        assertEquals(InstanceActivityState.AMBER, instance.getInstanceActivityState());
        long instanceActivityStateChangeTimestamp = instance.getInstanceActivityStateChangeTimestamp();
        assertTrue(instanceActivityStateChangeTimestamp > (System.currentTimeMillis() - (3 * 60 * 1000)));
    }

    @Test
    public void shouldPauseUnvalidatedAmberInstance() throws Exception {
        // setup
        addInstanceToUser(USER_NAME, instanceId);
        setupImmutableResourceManagerToMimicExistingNetworkForVlanId(instanceId, instance);
        setupSharedResourceManagerToMimicRegisteredInstance(instancePastryId, instance.getInstanceId());
        setupDeviceUtilsToMimicNetworkForRunningInstance();
        stubLibvirtConnection.startDomain("<name>" + instanceId + "</name>");

        // set last timestamp to eons ago and instance to RUNNING
        dhtClientFactory.createBlockingWriter().update(instancePastryId, null, new UpdateResolver<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.setInstanceActivityState(InstanceActivityState.AMBER);
                setField(existingEntity, "instanceActivityStateChangeTimestamp", 10000L);
                existingEntity.setState(InstanceState.RUNNING);
                existingEntity.setNodeId(instanceManagerApplication.getNodeIdFull());
                return existingEntity;
            }
        });

        // act
        boolean result = stubLibvirtConnection.waitForLibvirtCommand(String.format("pauseInstance(%s)", instanceId), 61 * 10);

        // assert
        assertTrue(result);
        Instance instance = (Instance) getPiEntityFromDht(instancePastryId);
        assertEquals(InstanceActivityState.RED, instance.getInstanceActivityState());
        long instanceActivityStateChangeTimestamp = instance.getInstanceActivityStateChangeTimestamp();
        assertTrue(instanceActivityStateChangeTimestamp > (System.currentTimeMillis() - (3 * 60 * 1000)));
    }

    @Test
    public void shouldTerminateUnvalidatedRedInstance() throws Exception {
        // setup
        addInstanceToUser(USER_NAME, instanceId);
        setupImmutableResourceManagerToMimicExistingNetworkForVlanId(instanceId, instance);
        setupSharedResourceManagerToMimicRegisteredInstance(instancePastryId, instance.getInstanceId());
        setupDeviceUtilsToMimicNetworkForRunningInstance();
        stubLibvirtConnection.startDomain("<name>" + instanceId + "</name>");

        // set last timestamp to eons ago and instance to RUNNING
        dhtClientFactory.createBlockingWriter().update(instancePastryId, null, new UpdateResolver<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.setInstanceActivityState(InstanceActivityState.RED);
                setField(existingEntity, "instanceActivityStateChangeTimestamp", 10000L);
                existingEntity.setState(InstanceState.RUNNING);
                existingEntity.setNodeId(instanceManagerApplication.getNodeIdFull());
                return existingEntity;
            }
        });

        // act
        waitForInstanceStateChange(InstanceState.TERMINATED);

        // assert
        assertThatInstanceStateInDhtIs(instancePastryId, InstanceState.TERMINATED);
        Thread.sleep(5000);
    }

    private boolean arrayContains(String[] array, String target) {
        for (String s : array)
            if (s.equals(target))
                return true;
        return false;
    }

    private void addInstanceToUser(String userName, final String newInstanceId) {
        PId userPastryId = piIdBuilder.getPId(User.getUrl(USER_NAME));

        dhtClientFactory.createBlockingWriter().update(userPastryId, null, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                existingEntity.addInstance(newInstanceId);
                return existingEntity;
            }
        });
    }

    private void clearQueue(PiQueue piQueue) {
        System.err.println(String.format("clearQueue(%s)", piQueue));
        PId queuePastryId = piIdBuilder.getPiQueuePId(piQueue).forLocalScope(piQueue.getNodeScope());

        dhtClientFactory.createBlockingWriter().update(queuePastryId, null, new UpdateResolver<TaskProcessingQueue>() {
            @Override
            public TaskProcessingQueue update(TaskProcessingQueue existingEntity, TaskProcessingQueue requestedEntity) {
                existingEntity.removeAllTasks();
                return existingEntity;
            }
        });
    }

    private void waitForUserRecordWithOutInstanceId(PId userPastryId, String instanceId) throws InterruptedException {
        int count = 0;
        int delay = 500;
        int max = 20;
        User user = null;
        while (count < max) {
            user = (User) getPiEntityFromDht(userPastryId);
            LOG.debug(">>> Checking user:" + user + ", with instances:" + user.getInstanceIds());
            if (arrayContains(user.getInstanceIds(), instanceId)) {
                LOG.debug(">>>> Found:" + instanceId + " for user:" + user + "...Trying again");
                count++;
                Thread.sleep(delay);
            } else {
                LOG.debug(">>>> Can't find instance:" + instanceId + " for user:" + user + "...Returning");
                return;
            }
        }
        fail(String.format("user not in expected state after %d millis. the real state is %s", delay * max, ArrayUtils.toString(user.getInstanceIds())));
    }

    private Reservation setupReservationForRunInstance() {
        Set<String> instanceIds = new TreeSet<String>(Arrays.asList(new String[] { instanceId }));
        Reservation reservation = new Reservation();
        reservation.setInstanceIds(instanceIds);
        reservation.setInstanceType(instance.getInstanceType());
        reservation.setImageId(IMAGE_ID);
        return reservation;
    }

    private PubSubMessageContext setupMessageContextForRunInstance() {
        KoalaIdFactory koalaIdFactory = (KoalaIdFactory) context.getBean("koalaIdFactory");
        PubSubMessageContext pubSubMessageContext = instanceManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.RUN_INSTANCE.getPiLocation()).forLocalAvailabilityZone(), null);
        return pubSubMessageContext;
    }

    private ReceivedMessageContext setupMessageContextForTerminateInstance() {
        ReceivedMessageContext deleteMessageContext = mock(ReceivedMessageContext.class);
        when(deleteMessageContext.getMethod()).thenReturn(EntityMethod.DELETE);
        when(deleteMessageContext.getReceivedEntity()).thenReturn(instance);
        return deleteMessageContext;
    }

    private ReceivedMessageContext setupMessageContextForInstanceReboot() {
        ReceivedMessageContext rebootMessageContext = mock(ReceivedMessageContext.class);
        when(rebootMessageContext.getMethod()).thenReturn(EntityMethod.UPDATE);
        when(rebootMessageContext.getReceivedEntity()).thenReturn(instance);
        instance.setRestartRequested(true);
        return rebootMessageContext;
    }

    private void setupSharedResourceManagerToMimicRegisteredInstance(PId instancePiKey, String consumerId) throws Exception {
        properties.setProperty(Instance.INITIAL_INTERVAL_MILLIS_PROPERTY, "1000");
        properties.setProperty(Instance.REPEATING_INTERVAL_MILLIS_PROPERTY, "1000");

        ConsumedDhtResourceRegistry consumedDhtResourceRegistry = (ConsumedDhtResourceRegistry) context.getBean(CONSUMED_DHT_RESOURCE_REGISTRY);
        consumedDhtResourceRegistry.registerConsumer(instancePiKey, consumerId, Instance.class, new LoggingContinuation<Boolean>());
    }

    private void setupImmutableResourceManagerToMimicExistingNetworkForVlanId(String instanceId, Instance currentInstance) {
        ConsumedUriResourceRegistry consumedUriResourceRegistry = (ConsumedUriResourceRegistry) context.getBean(CONSUMED_URI_RESOURCE_REGISTRY);
        consumedUriResourceRegistry.registerConsumer(URI.create(String.format("%s:%d", ResourceSchemes.VIRTUAL_NETWORK, currentInstance.getVlanId())), instanceId, new LoggingContinuation<Boolean>());
    }

    private void setupDeviceUtilsToMimicNetworkForRunningInstance() {
        StubDeviceUtils stubDeviceUtils = (StubDeviceUtils) context.getBean(STUB_DEVICE_UTILS);
        stubDeviceUtils.setDeviceAlwaysExists(true);
    }

    private void waitForInstanceStateChange(InstanceState requiredState) throws Exception {
        int count = 20;
        long sleep = 2 * 1000;
        Instance instance = null;
        for (int i = 0; i < count; i++) {
            instance = (Instance) getPiEntityFromDht(instancePastryId);
            if (requiredState.equals(instance.getState()))
                return;

            Thread.sleep(sleep);
        }
        org.junit.Assert.fail(String.format("instance state not %s after %d millis. State was: %s", requiredState, count * sleep, instance != null ? instance.getState() : null));
    }

    private void assertThatInstanceStateInDhtIs(PId instancePiKey, InstanceState state) {
        Instance instanceFromDht = (Instance) getPiEntityFromDht(instancePiKey);
        assertNotNull(instanceFromDht);
        assertEquals(state, instanceFromDht.getState());
    }

    private void assertThatInstanceIsRegisteredWithSharedResourceManager(PId instancePastryId, Instance instance) {
        ConsumedDhtResourceRegistry consumedDhtResourceRegistry = (ConsumedDhtResourceRegistry) context.getBean(CONSUMED_DHT_RESOURCE_REGISTRY);
        assertThat((Instance) consumedDhtResourceRegistry.getCachedEntity(instancePastryId), equalTo(instance));
    }

    private void assertThatInstanceIsRemovedFromSharedResourceManager(Instance instance) {
        ConsumedDhtResourceRegistry consumedDhtResourceRegistry = (ConsumedDhtResourceRegistry) context.getBean(CONSUMED_DHT_RESOURCE_REGISTRY);
        Instance instanceFromResourceRegistry = consumedDhtResourceRegistry.getCachedEntity(instancePastryId);
        assertNull(instanceFromResourceRegistry);
    }
}
