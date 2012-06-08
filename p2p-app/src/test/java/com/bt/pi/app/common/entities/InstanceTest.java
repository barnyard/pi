package com.bt.pi.app.common.entities;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.watchers.instance.InstanceWatchingStrategy;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.core.application.resource.watched.WatchedResource;
import com.bt.pi.core.parser.KoalaJsonParser;

public class InstanceTest {

    private Instance instance;

    @Before
    public void before() {
        String instanceId = "i-12345678";
        String userId = "dick";
        String securityGroupName = "default";
        ImagePlatform platform = ImagePlatform.linux;
        instance = new Instance(instanceId, userId, securityGroupName, platform);
    }

    @Test
    public void shouldInitialiseLastHeartbeatTimestamp() {
        assertNotNull(instance.getLastHeartbeatTimestamp());
    }

    @Test
    public void testGetUrl() {
        assertEquals("inst:" + instance.getInstanceId(), Instance.getUrl(instance.getInstanceId()));
        assertEquals(instance.getUrl(), Instance.getUrl(instance.getInstanceId()));
    }

    @Test
    public void testGetType() {
        // act
        String result = instance.getType();

        // assert
        assertThat(result, equalTo("Instance"));
    }

    @Test
    public void testEqualsNotInstance() {
        // act
        boolean result = instance.equals("A String");

        // assert
        assertFalse(result);
    }

    @Test
    public void testEqualsSame() {
        // act
        boolean result = instance.equals(instance);

        // assert
        assertTrue(result);
    }

    @Test
    public void testEqualsNull() {
        // act
        boolean result = instance.equals(null);

        // assert
        assertFalse(result);
    }

    @Test
    public void testSerialization() {
        // setup
        instance.setAdditionalInfo("info");
        instance.setRestartRequested(true);
        instance.setState(InstanceState.RUNNING);
        instance.setInstanceActivityState(InstanceActivityState.RED);

        KoalaJsonParser parser = new KoalaJsonParser();
        String json = parser.getJson(instance);
        System.err.println(json);
        Instance result = (Instance) parser.getObject(json, Instance.class);

        assertEquals(instance, result);
    }

    @Test
    public void testSettersGetters() {
        // setup
        Instance i2 = new Instance();
        i2.setPlatform(ImagePlatform.linux);
        i2.setInstanceId("i-1234555");
        i2.setUserId("d2");
        i2.setSecurityGroupName("default");
        i2.setAvailabilityZoneCode(3);
        i2.setRegionCode(4);

        // assert
        assertEquals(ImagePlatform.linux, i2.getPlatform());
        assertEquals("i-1234555", i2.getInstanceId());
        assertEquals("d2", i2.getUserId());
        assertEquals("default", i2.getSecurityGroupName());
        assertEquals(3, i2.getAvailabilityZoneCode());
        assertEquals(4, i2.getRegionCode());
    }

    @Test
    public void shouldBeEqual() {
        // setup
        Instance instance1 = new Instance();
        Instance instance2 = new Instance();
        Instance instance3 = new Instance();

        instance1.setPlatform(ImagePlatform.linux);
        instance1.setInstanceId("i-1234555");
        instance1.setUserId("d2");
        instance1.setSecurityGroupName("default");
        instance1.setRegionCode(1);
        instance1.setAvailabilityZoneCode(2);

        instance2.setPlatform(ImagePlatform.linux);
        instance2.setInstanceId("i-1234555");
        instance2.setUserId("d2");
        instance2.setSecurityGroupName("default");
        instance2.setRegionCode(1);
        instance2.setAvailabilityZoneCode(2);

        instance3.setPlatform(ImagePlatform.linux);
        instance3.setInstanceId("i-1234555");
        instance3.setUserId("d2");
        instance3.setSecurityGroupName("default");
        instance3.setRegionCode(1);
        instance3.setAvailabilityZoneCode(2);

        // assert
        assertTrue(instance1.equals(instance1));
        assertTrue(instance1.equals(instance2));
        assertTrue(instance1.equals(instance3));
        assertTrue(instance2.equals(instance1));
        assertTrue(instance2.equals(instance2));
        assertTrue(instance2.equals(instance3));
        assertTrue(instance3.equals(instance1));
        assertTrue(instance3.equals(instance2));
        assertTrue(instance3.equals(instance3));
    }

    @Test
    public void shouldBeAbleToOverrideDefaultBuriedInterval() {
        // setup
        System.setProperty("instance.buried.interval.millis", "7");

        // act
        Instance inst = new Instance();

        // assert
        assertEquals(7, inst.getBuriedIntervalMillis());
    }

    @Test
    public void shouldNotMarkNonStaleInstanceAsBuried() throws InterruptedException {
        // act
        Instance inst = new Instance();

        // assert
        assertFalse(inst.isBuried());
    }

    @Test
    public void shouldMarkStaleInstanceAsBuried() throws InterruptedException {
        // setup
        System.setProperty("instance.buried.interval.millis", "1");

        // act
        Instance inst = new Instance();
        Thread.sleep(100);

        // assert
        assertTrue(inst.isBuried());
    }

    @Test
    public void shouldTreatPendingInstanceAsActive() throws InterruptedException {
        instance.setState(InstanceState.PENDING);
        assertFalse(instance.isDead());
    }

    @Test
    public void shouldTreatRunningInstanceAsActive() throws InterruptedException {
        instance.setState(InstanceState.RUNNING);
        assertFalse(instance.isDead());
    }

    @Test
    public void shouldTreatShuttingDownInstanceAsInactive() throws InterruptedException {
        instance.setState(InstanceState.SHUTTING_DOWN);
        assertTrue(instance.isDead());
    }

    @Test
    public void shouldTreatTerminatedInstanceAsInactive() throws InterruptedException {
        instance.setState(InstanceState.TERMINATED);
        assertTrue(instance.isDead());
    }

    @Test
    public void shouldTreatStaleInstanceAsInactive() throws InterruptedException {
        instance.setLastHeartbeatTimestamp(0L);
        assertTrue(instance.isDead());
    }

    @Test
    public void shouldSetRebootRequested() {
        // act
        instance.setRestartRequested(true);

        // assert
        assertTrue(instance.isRestartRequested());
    }

    @Test
    public void shouldBeAnnotatedAsWatchedResourceWithAppropriateStrategyAndIntervalSettings() {
        // act
        WatchedResource res = Instance.class.getAnnotation(WatchedResource.class);

        // assert
        assertEquals(InstanceWatchingStrategy.class, res.watchingStrategy());
        assertEquals(60000, res.defaultInitialResourceRefreshIntervalMillis());
        assertEquals(300000, res.defaultRepeatingResourceRefreshIntervalMillis());
        assertEquals("instance.manager.instance.subscribe.initial.wait.time.millis", res.initialResourceRefreshIntervalMillisProperty());
        assertEquals("instance.manager.instance.subscribe.interval.millis", res.repeatingResourceRefreshIntervalMillisProperty());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowAnExceptionIfInstanceTryingToGoBackToAnEarlierState() {
        // setup
        instance.setState(InstanceState.TERMINATED);

        // act
        try {
            instance.setState(InstanceState.RUNNING);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testIsPendingTrue() {
        // setup
        instance.setState(InstanceState.PENDING);

        // act
        boolean result = instance.isPending();

        // assert
        assertTrue(result);
    }

    @Test
    public void testIsPendingFalse() {
        // setup
        instance.setState(InstanceState.RUNNING);

        // act
        boolean result = instance.isPending();

        // assert
        assertFalse(result);
    }

    @Test
    public void testIsDeletedWhenTerminatedAndBuried() {
        // setup
        instance.setState(InstanceState.TERMINATED);
        instance.setLastHeartbeatTimestamp(System.currentTimeMillis() - (25 * 60 * 60 * 1000));

        // act
        boolean result = instance.isDeleted();

        // assert
        assertTrue(result);
    }

    @Test
    public void testIsDeletedWhenFailedAndBuried() {
        // setup
        instance.setState(InstanceState.FAILED);
        instance.setLastHeartbeatTimestamp(System.currentTimeMillis() - (25 * 60 * 60 * 1000));

        // act
        boolean result = instance.isDeleted();

        // assert
        assertTrue(result);
    }

    @Test
    public void settingStateShouldSetStateChangeTimestamp() {
        // act
        instance.setState(InstanceState.RUNNING);

        // assert
        long timestamp = instance.getStateChangeTimestamp();
        long now = System.currentTimeMillis();
        assertTrue(Math.abs(now - timestamp) < 10);
    }

    @Test
    public void shouldSetRequiredAction() {
        // setup
        instance.setActionRequired(InstanceAction.PAUSE);

        KoalaJsonParser parser = new KoalaJsonParser();
        String json = parser.getJson(instance);
        Instance result = (Instance) parser.getObject(json, Instance.class);

        assertEquals(InstanceAction.PAUSE, result.anyActionRequired());
    }

    @Test
    public void shouldReturnNONEInstanceActionIfItIsNull() {
        // setup
        instance.setActionRequired(null);

        KoalaJsonParser parser = new KoalaJsonParser();
        String json = parser.getJson(instance);
        Instance result = (Instance) parser.getObject(json, Instance.class);

        assertEquals(InstanceAction.NONE, result.anyActionRequired());
    }

    @Test
    public void shouldBeAbleToTransitionFromRunningToCrashed() {
        // setup
        instance.setState(InstanceState.RUNNING);

        // act
        instance.setState(InstanceState.CRASHED);

        // verify
        assertEquals(InstanceState.CRASHED, instance.getState());
    }

    @Test
    public void shouldBeAbleToTransitionFromCrashedToRunning() {
        // setup
        instance.setState(InstanceState.CRASHED);

        // act
        instance.setState(InstanceState.RUNNING);

        // verify
        assertEquals(InstanceState.RUNNING, instance.getState());
    }

    @Test
    public void shouldReturnGreenForDefaultInstanceActivityState() {
        // act
        InstanceActivityState result = instance.getInstanceActivityState();

        // assert
        assertEquals(InstanceActivityState.GREEN, result);
    }

    @Test
    public void shouldSetInstanceActivityStateTimestampWhenSettingState() throws Exception {
        // setup
        long current = instance.getInstanceActivityStateChangeTimestamp();

        // act
        Thread.sleep(100);
        instance.setInstanceActivityState(InstanceActivityState.AMBER);

        // assert
        assertTrue(current < instance.getInstanceActivityStateChangeTimestamp());
    }

    @Test
    public void shouldSetActivityStateTimestampOnCreation() {
        // assert
        assertTrue(instance.getInstanceActivityStateChangeTimestamp() > 0);
        assertTrue(instance.getInstanceActivityStateChangeTimestamp() > System.currentTimeMillis() - 1000);
    }
}