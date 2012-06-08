package com.bt.pi.app.volumemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.pi.app.common.entities.BlockDeviceMapping;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.app.instancemanager.testing.StubLibvirtConnection;
import com.bt.pi.app.management.QueueSeeder;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.testing.StubCommandExecutor;

public class VolumeManagerApplicationIntegrationTest {
    private static final Log LOG = LogFactory.getLog(VolumeManagerApplicationIntegrationTest.class);
    private static AbstractApplicationContext context;
    private static PiIdBuilder piIdBuilder;
    private static DhtClientFactory dhtClientFactory;
    private static VolumeManagerApplication volumeManagerApplication;
    private static InstanceManagerApplication instanceManagerApplication;
    private static Properties properties;
    private static AtomicInteger volumeIdIndex;
    private static StubLibvirtConnection stubLibvirtConnection;
    private static String instanceManagerNodeId;
    private static KoalaNode koalaNode;
    private static KoalaIdFactory koalaIdFactory;
    private static StubCommandExecutor stubCommandExecutor;
    private static TaskProcessingQueueHelper taskProcessingQueueHelper;
    private static DeleteVolumeTaskQueueWatcherInitiator deleteVolumeTaskQueueWatcherInitiator;
    private static CreateVolumeTaskQueueWatcherInitiator createVolumeTaskQueueWatcherInitiator;
    private static AttachVolumeTaskQueueWatcherInitiator attachVolumeTaskQueueWatcherInitiator;
    private static DetachVolumeTaskQueueWatcherInitiator detachVolumeTaskQueueWatcherInitiator;
    private static CreateSnapshotTaskQueueWatcherInitiator createSnapshotTaskQueueWatcherInitiator;
    private static DeleteSnapshotTaskQueueWatcherInitiator deleteSnapshotTaskQueueWatcherInitiator;
    private static RemoveSnapshotFromUserTaskQueueWatcherInitiator removeSnapshotFromUserTaskQueueWatcherInitiator;
    private static RemoveVolumeFromUserTaskQueueWatcherInitiator removeVolumeFromUserTaskQueueWatcherInitiator;
    private int size = 2;
    @Rule
    public TestName testName = new TestName();

    private String userId;

    @BeforeClass
    public static void beforeClass() throws Exception {
        FileUtils.deleteQuietly(new File("var"));
        try {
            context = new ClassPathXmlApplicationContext("commonApplicationContext.xml");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        koalaNode = (KoalaNode) context.getBean("koalaNode");
        piIdBuilder = (PiIdBuilder) context.getBean("piIdBuilder");
        dhtClientFactory = (DhtClientFactory) context.getBean("dhtClientFactory");
        volumeManagerApplication = (VolumeManagerApplication) context.getBean("volumeManagerApplication");
        instanceManagerApplication = (InstanceManagerApplication) context.getBean("instanceManagerApplication");
        properties = (Properties) context.getBean("properties");
        stubLibvirtConnection = (StubLibvirtConnection) context.getBean("libvirtConnection");
        stubCommandExecutor = (StubCommandExecutor) context.getBean("stubCommandExecutor");
        koalaIdFactory = (KoalaIdFactory) context.getBean("koalaIdFactory");
        taskProcessingQueueHelper = (TaskProcessingQueueHelper) context.getBean("taskProcessingQueueHelper");
        deleteVolumeTaskQueueWatcherInitiator = (DeleteVolumeTaskQueueWatcherInitiator) context.getBean("deleteVolumeTaskQueueWatcherInitiator");
        createVolumeTaskQueueWatcherInitiator = (CreateVolumeTaskQueueWatcherInitiator) context.getBean("createVolumeTaskQueueWatcherInitiator");
        attachVolumeTaskQueueWatcherInitiator = (AttachVolumeTaskQueueWatcherInitiator) context.getBean("attachVolumeTaskQueueWatcherInitiator");
        detachVolumeTaskQueueWatcherInitiator = (DetachVolumeTaskQueueWatcherInitiator) context.getBean("detachVolumeTaskQueueWatcherInitiator");
        createSnapshotTaskQueueWatcherInitiator = (CreateSnapshotTaskQueueWatcherInitiator) context.getBean("createSnapshotTaskQueueWatcherInitiator");
        deleteSnapshotTaskQueueWatcherInitiator = (DeleteSnapshotTaskQueueWatcherInitiator) context.getBean("deleteSnapshotTaskQueueWatcherInitiator");
        removeSnapshotFromUserTaskQueueWatcherInitiator = context.getBean(RemoveSnapshotFromUserTaskQueueWatcherInitiator.class);
        removeVolumeFromUserTaskQueueWatcherInitiator = context.getBean(RemoveVolumeFromUserTaskQueueWatcherInitiator.class);
        volumeIdIndex = new AtomicInteger(0);

        koalaNode.start();

        putInstanceTypeIntoDht();
        seedQueues();
        Thread.sleep(3000);

        instanceManagerNodeId = instanceManagerApplication.getNodeIdFull();
    }

    private static void seedQueues() {
        QueueSeeder queueSeeder = new QueueSeeder();
        queueSeeder.setDhtClientFactory(dhtClientFactory);
        queueSeeder.setPiIdBuilder(piIdBuilder);
        queueSeeder.addQueue(PiQueue.CREATE_VOLUME);
        queueSeeder.addQueue(PiQueue.DELETE_VOLUME);
        queueSeeder.addQueue(PiQueue.ATTACH_VOLUME);
        queueSeeder.addQueue(PiQueue.DETACH_VOLUME);
        queueSeeder.addQueue(PiQueue.CREATE_SNAPSHOT);
        queueSeeder.addQueue(PiQueue.DELETE_SNAPSHOT);
        queueSeeder.addQueue(PiQueue.REMOVE_SNAPSHOT_FROM_USER);
        queueSeeder.addQueue(PiQueue.REMOVE_VOLUME_FROM_USER);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        FileUtils.deleteQuietly(new File("var"));
        File nodeIdFile = new File(properties.getProperty("nodeIdFile"));
        String nodeId = FileUtils.readFileToString(nodeIdFile);

        File file = new File(String.format("storage%s", nodeId));
        FileUtils.deleteQuietly(file);
        FileUtils.deleteQuietly(nodeIdFile);

        koalaNode.stop();
        context.destroy();
    }

    @Before
    public void before() {
        volumeIdIndex.incrementAndGet();
        stubCommandExecutor.clearCommands();
        userId = "user" + testName;
        LOG.info(String.format("running test %s", testName.getMethodName()));
    }

    @After
    public void after() {
        deleteVolumeTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        deleteVolumeTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        deleteVolumeTaskQueueWatcherInitiator.setStaleQueueItemMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        deleteVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);

        createVolumeTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        createVolumeTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        createVolumeTaskQueueWatcherInitiator.setStaleQueueItemMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        createVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);

        attachVolumeTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        attachVolumeTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        attachVolumeTaskQueueWatcherInitiator.setStaleQueueItemMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        attachVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);

        detachVolumeTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        detachVolumeTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        detachVolumeTaskQueueWatcherInitiator.setStaleQueueItemMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        detachVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);

        createSnapshotTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        createSnapshotTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        createSnapshotTaskQueueWatcherInitiator.setStaleQueueItemMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        createSnapshotTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);

        deleteSnapshotTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        deleteSnapshotTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        deleteSnapshotTaskQueueWatcherInitiator.setStaleQueueItemMillis(DeleteVolumeTaskQueueWatcherInitiator.TWO_HUNDRED_SECS);
        deleteSnapshotTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);

        removeSnapshotFromUserTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(RemoveSnapshotFromUserTaskQueueWatcherInitiator.FIVE_MINUTES);
        removeSnapshotFromUserTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(RemoveSnapshotFromUserTaskQueueWatcherInitiator.FIVE_MINUTES);
        removeSnapshotFromUserTaskQueueWatcherInitiator.setStaleQueueItemMillis(RemoveSnapshotFromUserTaskQueueWatcherInitiator.BURIED_TIME);
        removeSnapshotFromUserTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);

        removeVolumeFromUserTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(RemoveVolumeFromUserTaskQueueWatcherInitiator.FOUR_MINUTES);
        removeVolumeFromUserTaskQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(RemoveVolumeFromUserTaskQueueWatcherInitiator.FOUR_MINUTES);
        removeVolumeFromUserTaskQueueWatcherInitiator.setStaleQueueItemMillis(RemoveVolumeFromUserTaskQueueWatcherInitiator.BURIED_TIME);
        removeVolumeFromUserTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);
    }

    @Test
    public void testCreateVolume() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.CREATING);

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.CREATE_VOLUME.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.CREATE, volume);

        // assert
        waitForVolumeState(volume.getVolumeId(), VolumeState.AVAILABLE);

        String expectedDDcommand = String.format("nice -n +10 ionice -c3 dd if=/dev/zero of=var/volumes/remote/%s count=0 seek=%d bs=1M", volume.getVolumeId(), size * 1024);
        assertCommand(expectedDDcommand);
    }

    @Test
    public void testCreateVolumeFromSnapshot() throws Exception {
        // setup
        Snapshot storedSnapshot = storeSnapshotRecord(SnapshotState.COMPLETE);
        Volume volume = storeVolumeRecord(VolumeState.CREATING, storedSnapshot.getSnapshotId());

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.CREATE_VOLUME.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.CREATE, volume);

        // assert
        waitForVolumeState(volume.getVolumeId(), VolumeState.AVAILABLE);

        String expectedDDcommand = String.format("nice -n +10 ionice -c3 cp var/snapshots/%s var/volumes/remote/%s", storedSnapshot.getSnapshotId(), volume.getVolumeId());
        assertCommand(expectedDDcommand);
    }

    @Test
    public void shouldCreateVolumeByProcessingItemFromQueue() throws Exception {
        // setup
        Volume taskQueuedVolume = storeVolumeRecord(VolumeState.CREATING);
        PId createVolumeQueueId = piIdBuilder.getPiQueuePId(PiQueue.CREATE_VOLUME).forLocalScope(PiQueue.CREATE_VOLUME.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(createVolumeQueueId, taskQueuedVolume.getUrl());
        wakeUpCreateVolumeQueue();

        // assert
        waitForVolumeState(taskQueuedVolume.getVolumeId(), VolumeState.AVAILABLE);
        String expectedDDcommand = String.format("nice -n +10 ionice -c3 dd if=/dev/zero of=var/volumes/remote/%s count=0 seek=%d bs=1M", taskQueuedVolume.getVolumeId(), size * 1024);
        assertCommand(expectedDDcommand);
    }

    @Test
    public void createVolumeShouldSetVolumeToFailedWhenRetriesAreExhausted() throws Exception {
        // setup
        Volume taskQueuedVolume = storeVolumeRecord(VolumeState.CREATING);
        PId createVolumeQueueId = piIdBuilder.getPiQueuePId(PiQueue.CREATE_VOLUME).forLocalScope(PiQueue.CREATE_VOLUME.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(createVolumeQueueId, taskQueuedVolume.getUrl(), 0);
        wakeUpCreateVolumeQueue();

        // assert
        waitForVolumeState(taskQueuedVolume.getVolumeId(), VolumeState.FAILED);
    }

    private void wakeUpCreateVolumeQueue() throws Exception {
        Thread.sleep(500);
        createVolumeTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(0);
        createVolumeTaskQueueWatcherInitiator.setStaleQueueItemMillis(1);
        createVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);
    }

    private void assertCommand(String target) {
        assertTrue(stubCommandExecutor.assertCommand(target.split(" ")));
    }

    private void assertCommandShouldNotRun(String target) {
        assertTrue(stubCommandExecutor.assertCommandMissing(target.split(" ")));
    }

    @Test
    public void testAttachVolume() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE);
        String instanceId = storeInstanceRecord();
        volume.setInstanceId(instanceId);
        volume.setStatus(VolumeState.ATTACHING); // for the anycast
        volume.setDevice("/dev/sdb1"); // for the anycast
        ((StubLibvirtConnection) context.getBean(StubLibvirtConnection.class)).startDomain("<name>" + instanceId + "</name>");

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.ATTACH_VOLUME.getPiLocation()).forLocalScope(PiTopics.ATTACH_VOLUME.getNodeScope()), null);
        pubSubMessageContext.sendAnycast(EntityMethod.UPDATE, volume);

        // assert
        waitForVolumeState(volume.getVolumeId(), VolumeState.IN_USE);
        assertLibvirtCommand("attachDevice(");
        assertVolumeHasInstanceId(volume.getVolumeId(), instanceId);
        assertInstanceHasVolumeId(instanceId, volume.getVolumeId());
        String expectedCopyCommand = String.format("nice -n +10 ionice -c3 cp var/volumes/remote/%s var/volumes/local/%s", volume.getVolumeId(), volume.getVolumeId());
        assertCommand(expectedCopyCommand);
    }

    @Test
    public void shouldAttachVolumeByProcessingQueueItem() throws Exception {
        // setup
        Volume taskQueuedVolume = storeVolumeRecord(VolumeState.ATTACHING);
        String instanceId = storeInstanceRecord();
        addInstanceIdAndDeviceIdToVolumeRecord(taskQueuedVolume.getVolumeId(), instanceId, "/dev/sdb1");
        PId attachVolumeQueueId = piIdBuilder.getPiQueuePId(PiQueue.ATTACH_VOLUME).forLocalScope(PiQueue.ATTACH_VOLUME.getNodeScope());
        ((StubLibvirtConnection) context.getBean(StubLibvirtConnection.class)).startDomain("<name>" + instanceId + "</name>");

        // act
        taskProcessingQueueHelper.addUrlToQueue(attachVolumeQueueId, taskQueuedVolume.getUrl());
        wakeUpAttachQueue();

        // assert
        waitForVolumeState(taskQueuedVolume.getVolumeId(), VolumeState.IN_USE);
        assertLibvirtCommand("attachDevice(");
        assertVolumeHasInstanceId(taskQueuedVolume.getVolumeId(), instanceId);
        assertInstanceHasVolumeId(instanceId, taskQueuedVolume.getVolumeId());
        String expectedCopyCommand = String.format("nice -n +10 ionice -c3 cp var/volumes/remote/%s var/volumes/local/%s", taskQueuedVolume.getVolumeId(), taskQueuedVolume.getVolumeId());
        assertCommand(expectedCopyCommand);
    }

    @Test
    public void attachVolumeShouldSetVolumeToAvailableWhenRetriesAreExhausted() throws Exception {
        // setup
        Volume taskQueuedVolume = storeVolumeRecord(VolumeState.ATTACHING);
        PId attachVolumeQueueId = piIdBuilder.getPiQueuePId(PiQueue.ATTACH_VOLUME).forLocalScope(PiQueue.ATTACH_VOLUME.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(attachVolumeQueueId, taskQueuedVolume.getUrl(), 0);
        wakeUpAttachQueue();

        // assert
        waitForVolumeState(taskQueuedVolume.getVolumeId(), VolumeState.AVAILABLE);
    }

    private void wakeUpAttachQueue() throws Exception {
        Thread.sleep(500);
        attachVolumeTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(0);
        attachVolumeTaskQueueWatcherInitiator.setStaleQueueItemMillis(1);
        attachVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);
    }

    @Test
    public void testDetachVolume() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.IN_USE);
        String instanceId = storeInstanceRecord();
        addVolumeToInstanceRecord(instanceId, volume.getVolumeId());
        addInstanceToVolumeRecord(volume.getVolumeId(), instanceId);
        volume.setInstanceId(instanceId);
        volume.setStatus(VolumeState.DETACHING); // for the anycast

        // start instance
        String path = new File(String.format("var/volumes/local/%s", volume.getVolumeId())).getAbsolutePath();
        stubLibvirtConnection.startDomain(String.format("<name>" + instanceId + "</name><disk>%s</disk>", path));

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.DETACH_VOLUME.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.UPDATE, volume);

        // assert
        waitForVolumeState(volume.getVolumeId(), VolumeState.AVAILABLE);
        assertLibvirtCommand("detachDevice(");
        assertVolumeIdInInstance(instanceId, 0);
        String expectedCopyCommand = String.format("cp var/volumes/local/%s var/volumes/remote/%s", volume.getVolumeId(), volume.getVolumeId());
        assertCommand(expectedCopyCommand);
        String expectedDeleteCommand = String.format("rm var/volumes/local/%s", volume.getVolumeId());
        assertCommand(expectedDeleteCommand);
    }

    @Test
    public void shouldDetachVolumeByProcessingQueueItem() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.DETACHING);
        String instanceId = storeInstanceRecord();
        addVolumeToInstanceRecord(instanceId, volume.getVolumeId());
        addInstanceToVolumeRecord(volume.getVolumeId(), instanceId);
        PId detachVolumeQueueId = piIdBuilder.getPiQueuePId(PiQueue.DETACH_VOLUME).forLocalScope(PiQueue.DETACH_VOLUME.getNodeScope());

        // start instance
        String path = new File(String.format("var/volumes/local/%s", volume.getVolumeId())).getAbsolutePath();
        stubLibvirtConnection.startDomain(String.format("<name>" + instanceId + "</name><disk>%s</disk>", path));

        // act
        taskProcessingQueueHelper.addUrlToQueue(detachVolumeQueueId, volume.getUrl());
        wakeUpDetachQueue();

        // assert
        waitForVolumeState(volume.getVolumeId(), VolumeState.AVAILABLE);
        assertLibvirtCommand("detachDevice(");
        assertVolumeIdInInstance(instanceId, 0);
        String expectedCopyCommand = String.format("cp var/volumes/local/%s var/volumes/remote/%s", volume.getVolumeId(), volume.getVolumeId());
        assertCommand(expectedCopyCommand);
        String expectedDeleteCommand = String.format("rm var/volumes/local/%s", volume.getVolumeId());
        assertCommand(expectedDeleteCommand);
    }

    @Test
    public void detachVolumeShouldSetVolumeToInUseWhenRetriesAreExhausted() throws Exception {
        // setup
        Volume taskQueuedVolume = storeVolumeRecord(VolumeState.DETACHING);
        PId detachVolumeQueueId = piIdBuilder.getPiQueuePId(PiQueue.DETACH_VOLUME).forLocalScope(PiQueue.DETACH_VOLUME.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(detachVolumeQueueId, taskQueuedVolume.getUrl(), 0);
        wakeUpDetachQueue();

        // assert
        waitForVolumeState(taskQueuedVolume.getVolumeId(), VolumeState.IN_USE);
    }

    private void wakeUpDetachQueue() throws Exception {
        Thread.sleep(500);
        detachVolumeTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(0);
        detachVolumeTaskQueueWatcherInitiator.setStaleQueueItemMillis(1);
        detachVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);
    }

    @Test
    public void shouldNotDeleteVolumeIfUnableToDetachDevice() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.IN_USE);
        String instanceId = storeInstanceRecord();
        addVolumeToInstanceRecord(instanceId, volume.getVolumeId());
        addInstanceToVolumeRecord(volume.getVolumeId(), instanceId);
        volume.setInstanceId(instanceId);
        volume.setStatus(VolumeState.DETACHING); // for the anycast

        // start instance
        stubLibvirtConnection.startDomain("<name>" + instanceId + "</name>");

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.DETACH_VOLUME.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.UPDATE, volume);

        // assert
        waitForVolumeState(volume.getVolumeId(), VolumeState.IN_USE);
        assertLibvirtCommand("detachDevice(");
        assertVolumeIdInInstance(instanceId, 1);
        String expectedCopyCommand = String.format("cp -v var/volumes/local/%s var/volumes/remote/%s", volume.getVolumeId(), volume.getVolumeId());
        assertCommandShouldNotRun(expectedCopyCommand);
        String expectedDeleteCommand = String.format("rm var/volumes/local/%s", volume.getVolumeId());
        assertCommandShouldNotRun(expectedDeleteCommand);
    }

    protected String getAbsoluteLocalVolumeFilename(Volume volume) {
        return String.format("%s/%s", new File("var/volumes/local").getAbsolutePath(), volume.getVolumeId());
    }

    @Test
    public void testDeleteVolume() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE);

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.DELETE_VOLUME.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.DELETE, volume);

        // assert
        waitForVolumeState(volume.getVolumeId(), VolumeState.DELETED);
        assertCommand("nice -n +10 ionice -c3 rm var/volumes/remote/" + volume.getVolumeId());
    }

    @Test
    public void shouldDeleteVolumeByProcessingItemFromQueue() throws Exception {
        // setup
        Volume taskQueuedVolume = storeVolumeRecord(VolumeState.AVAILABLE);
        PId deleteVolumeQueueId = piIdBuilder.getPiQueuePId(PiQueue.DELETE_VOLUME).forLocalScope(PiQueue.DELETE_VOLUME.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(deleteVolumeQueueId, taskQueuedVolume.getUrl());
        wakeUpDeleteQueue();

        // assert
        waitForVolumeState(taskQueuedVolume.getVolumeId(), VolumeState.DELETED);
        assertCommand("nice -n +10 ionice -c3 rm var/volumes/remote/" + taskQueuedVolume.getVolumeId());
    }

    @Test
    public void deleteVolumeShouldSetVolumeToAvailableWhenRetriesAreExhausted() throws Exception {
        // setup
        Volume taskQueuedVolume = storeVolumeRecord(VolumeState.DELETING);
        PId deleteVolumeQueueId = piIdBuilder.getPiQueuePId(PiQueue.DELETE_VOLUME).forLocalScope(PiQueue.DELETE_VOLUME.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(deleteVolumeQueueId, taskQueuedVolume.getUrl(), 0);
        wakeUpDeleteQueue();

        // assert
        waitForVolumeState(taskQueuedVolume.getVolumeId(), VolumeState.AVAILABLE);
    }

    private void wakeUpDeleteQueue() throws Exception {
        Thread.sleep(500);
        deleteVolumeTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(0);
        deleteVolumeTaskQueueWatcherInitiator.setStaleQueueItemMillis(1);
        deleteVolumeTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);
    }

    @Test
    public void testCreateSnapshotWhenAvailable() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE);
        Snapshot snapshot = storeSnapshotRecord(SnapshotState.PENDING);
        snapshot.setVolumeId(volume.getVolumeId());

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.CREATE_SNAPSHOT.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.CREATE, snapshot);

        // assert
        waitForSnapshotState(snapshot.getSnapshotId(), SnapshotState.COMPLETE);
        waitForVolumeState(volume.getVolumeId(), VolumeState.AVAILABLE);

        String expectedRsyncCommand = String.format("nice -n +10 ionice -c3 cp var/volumes/remote/%s var/snapshots/%s", volume.getVolumeId(), snapshot.getSnapshotId());
        assertCommand(expectedRsyncCommand);
    }

    @Test
    public void shouldNotCreateSnapshotWhenAnotherIsBeingProcessed() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE);
        Snapshot snapshot = storeSnapshotRecord(SnapshotState.PENDING);
        snapshot.setVolumeId(volume.getVolumeId());

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.CREATE_SNAPSHOT.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.CREATE, snapshot);
        pubSubMessageContext.sendAnycast(EntityMethod.CREATE, snapshot);
        // assert
        waitForSnapshotState(snapshot.getSnapshotId(), SnapshotState.COMPLETE);
        waitForVolumeState(volume.getVolumeId(), VolumeState.AVAILABLE);

        String expectedRsyncCommand = String.format("nice -n +10 ionice -c3 cp var/volumes/remote/%s var/snapshots/%s", volume.getVolumeId(), snapshot.getSnapshotId());
        assertCommand(expectedRsyncCommand);
    }

    @Test
    public void shouldCreateSnapshotWhenAvailableByProcessingItemFromQueue() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE_SNAPSHOTTING);
        Snapshot snapshot = storeSnapshotRecord(SnapshotState.PENDING, volume.getVolumeId());
        PId createSnapshotQueueId = piIdBuilder.getPiQueuePId(PiQueue.CREATE_SNAPSHOT).forLocalScope(PiQueue.CREATE_SNAPSHOT.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(createSnapshotQueueId, snapshot.getUrl());
        wakeUpCreateSnapshotQueue();

        // assert
        waitForSnapshotState(snapshot.getSnapshotId(), SnapshotState.COMPLETE);
        waitForVolumeState(volume.getVolumeId(), VolumeState.AVAILABLE);
        String expectedRsyncCommand = String.format("nice -n +10 ionice -c3 cp var/volumes/remote/%s var/snapshots/%s", volume.getVolumeId(), snapshot.getSnapshotId());
        assertCommand(expectedRsyncCommand);
    }

    @Test
    public void createSnapshotShouldSetSnapshotToErrorAndRevertVolumeWhenRetriesAreExhausted() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE_SNAPSHOTTING);
        Snapshot snapshot = storeSnapshotRecord(SnapshotState.PENDING, volume.getVolumeId());
        PId createSnapshotQueueId = piIdBuilder.getPiQueuePId(PiQueue.CREATE_SNAPSHOT).forLocalScope(PiQueue.CREATE_SNAPSHOT.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(createSnapshotQueueId, snapshot.getUrl(), 0);
        wakeUpCreateSnapshotQueue();

        // assert
        waitForSnapshotState(snapshot.getSnapshotId(), SnapshotState.ERROR);
        waitForVolumeState(volume.getVolumeId(), VolumeState.AVAILABLE);
    }

    @Test
    public void testDeleteSnapshot() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE);
        Snapshot snapshot = storeSnapshotRecord(SnapshotState.COMPLETE);
        Collection<String> snapshotIds = new ArrayList<String>(Arrays.asList(snapshot.getSnapshotId(), "extra"));
        addSnapshotIdsToUser(snapshotIds);
        snapshot.setVolumeId(volume.getVolumeId());

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.DELETE_SNAPSHOT.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.DELETE, snapshot);

        // assert
        waitForSnapshotState(snapshot.getSnapshotId(), SnapshotState.DELETED);

        String expectedRsyncCommand = String.format("nice -n +10 ionice -c3 rm var/snapshots/%s", snapshot.getSnapshotId());
        assertCommand(expectedRsyncCommand);
    }

    @Test
    public void testDeleteSnapshotDoesNotDeleteIfSnapshotNotInCompleteState() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE);
        Snapshot snapshot = storeSnapshotRecord(SnapshotState.DELETING);
        Collection<String> snapshotIds = Arrays.asList(snapshot.getSnapshotId(), "extra");
        addSnapshotIdsToUser(snapshotIds);
        snapshot.setVolumeId(volume.getVolumeId());

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.DELETE_SNAPSHOT.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.DELETE, snapshot);

        // assert
        waitForSnapshotState(snapshot.getSnapshotId(), SnapshotState.DELETING);
        waitForUserRecordWithSnapshotIds(snapshotIds);

        String expectedRsyncCommand = String.format("nice -n +10 ionice -c3 rm var/snapshots/%s", snapshot.getSnapshotId());
        assertCommandShouldNotRun(expectedRsyncCommand);
    }

    @Test
    public void shouldDeleteSnapshotWhenAvailableByProcessingItemFromQueue() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE);
        Snapshot snapshot = storeSnapshotRecord(SnapshotState.COMPLETE, volume.getVolumeId());
        Collection<String> snapshotIds = new ArrayList<String>(Arrays.asList(snapshot.getSnapshotId(), "extra"));
        addSnapshotIdsToUser(snapshotIds);
        snapshot.setVolumeId(volume.getVolumeId());

        PId deleteSnapshotQueueId = piIdBuilder.getPiQueuePId(PiQueue.DELETE_SNAPSHOT).forLocalScope(PiQueue.DELETE_SNAPSHOT.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(deleteSnapshotQueueId, snapshot.getUrl());
        wakeUpDeleteSnapshotQueue();

        // assert
        waitForSnapshotState(snapshot.getSnapshotId(), SnapshotState.DELETED);

        String expectedRsyncCommand = String.format("nice -n +10 ionice -c3 rm var/snapshots/%s", snapshot.getSnapshotId());
        assertCommand(expectedRsyncCommand);
    }

    @Test
    public void deleteSnapshotShouldSetSnapshotToErrorWhenRetriesAreExhausted() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE);
        Snapshot snapshot = storeSnapshotRecord(SnapshotState.COMPLETE);
        Collection<String> snapshotIds = Arrays.asList(snapshot.getSnapshotId(), "extra");
        addSnapshotIdsToUser(snapshotIds);
        snapshot.setVolumeId(volume.getVolumeId());

        PId deleteSnapshotQueueId = piIdBuilder.getPiQueuePId(PiQueue.DELETE_SNAPSHOT).forLocalScope(PiQueue.DELETE_SNAPSHOT.getNodeScope());

        // act
        taskProcessingQueueHelper.addUrlToQueue(deleteSnapshotQueueId, snapshot.getUrl(), 0);
        wakeUpDeleteSnapshotQueue();

        // assert
        waitForSnapshotState(snapshot.getSnapshotId(), SnapshotState.ERROR);
        waitForUserRecordWithSnapshotIds(snapshotIds);

        String expectedRsyncCommand = String.format("nice -n +10 ionice -c3 rm var/snapshots/%s", snapshot.getSnapshotId());
        assertCommandShouldNotRun(expectedRsyncCommand);
    }

    @Test
    public void shouldBurySnapshotAndRemoveFromUserByPickingOffCommandsFromTheQueue() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE);
        Snapshot snapshot = storeSnapshotRecord(SnapshotState.COMPLETE);
        Collection<String> snapshotIds = new ArrayList<String>(Arrays.asList(snapshot.getSnapshotId(), "extra"));
        addSnapshotIdsToUser(snapshotIds);
        snapshot.setVolumeId(volume.getVolumeId());

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.DELETE_SNAPSHOT.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.DELETE, snapshot);

        waitForSnapshotState(snapshot.getSnapshotId(), SnapshotState.DELETED);
        Thread.sleep(1000);
        wakeUpRemoveSnapshotFromUserQueue();

        // assert
        waitForSnapshotState(snapshot.getSnapshotId(), SnapshotState.BURIED);

        snapshotIds.remove(snapshot.getSnapshotId());
        waitForUserRecordWithSnapshotIds(snapshotIds);
    }

    @Test
    public void shouldBuryVolumeAndRemoveFromUserByPickingOffCommandsFromTheQueue() throws Exception {
        // setup
        Volume volume = storeVolumeRecord(VolumeState.AVAILABLE);
        Collection<String> volumeIds = new ArrayList<String>(Arrays.asList(volume.getVolumeId(), "extra"));
        addVolumeIdsToUser(volumeIds);

        // act
        // anycast message to volume manager
        PubSubMessageContext pubSubMessageContext = volumeManagerApplication.newPubSubMessageContext(koalaIdFactory.buildPId(PiTopics.DELETE_VOLUME.getPiLocation()).forLocalAvailabilityZone(), null);
        pubSubMessageContext.sendAnycast(EntityMethod.DELETE, volume);

        // assert
        waitForVolumeState(volume.getVolumeId(), VolumeState.DELETED);
        Thread.sleep(1000);
        wakeUpRemoveVolumeFromUserQueue();

        // assert
        waitForVolumeState(volume.getVolumeId(), VolumeState.BURIED);

        volumeIds.remove(volume.getVolumeId());
        waitForUserRecordWithVolumeIds(volumeIds);
    }

    private void wakeUpCreateSnapshotQueue() throws Exception {
        Thread.sleep(500);
        createSnapshotTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(0);
        createSnapshotTaskQueueWatcherInitiator.setStaleQueueItemMillis(1);
        createSnapshotTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);
    }

    private void wakeUpDeleteSnapshotQueue() throws Exception {
        Thread.sleep(500);
        deleteSnapshotTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(0);
        deleteSnapshotTaskQueueWatcherInitiator.setStaleQueueItemMillis(1);
        deleteSnapshotTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);
    }

    private void wakeUpRemoveSnapshotFromUserQueue() throws Exception {
        Thread.sleep(500);
        removeSnapshotFromUserTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(0);
        removeSnapshotFromUserTaskQueueWatcherInitiator.setStaleQueueItemMillis(1);
        removeSnapshotFromUserTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);
    }

    private void wakeUpRemoveVolumeFromUserQueue() throws Exception {
        Thread.sleep(500);
        removeVolumeFromUserTaskQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(0);
        removeVolumeFromUserTaskQueueWatcherInitiator.setStaleQueueItemMillis(1);
        removeVolumeFromUserTaskQueueWatcherInitiator.createTaskProcessingQueueWatcher(instanceManagerNodeId);
    }

    private void assertVolumeIdInInstance(String instanceId, int volumeIdCount) {
        PId instanceIdId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        BlockingDhtReader blockingDhtReader = dhtClientFactory.createBlockingReader();
        Instance instance = (Instance) blockingDhtReader.get(instanceIdId);
        assertEquals(volumeIdCount, instance.getBlockDeviceMappings().size());
    }

    private void addInstanceToVolumeRecord(String volumeId, final String instanceId) {
        PId volumeIdId = piIdBuilder.getPIdForEc2AvailabilityZone(Volume.getUrl(volumeId));
        BlockingDhtWriter blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(volumeIdId, null, new UpdateResolvingPiContinuation<Volume>() {

            @Override
            public Volume update(Volume existingEntity, Volume requestedEntity) {
                existingEntity.setInstanceId(instanceId);
                return existingEntity;
            }

            @Override
            public void handleResult(Volume result) {
            }
        });
    }

    private void addInstanceIdAndDeviceIdToVolumeRecord(final String volumeId, final String instanceId, final String deviceId) {
        PId volumeIdId = piIdBuilder.getPIdForEc2AvailabilityZone(Volume.getUrl(volumeId));
        BlockingDhtWriter blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(volumeIdId, null, new UpdateResolvingPiContinuation<Volume>() {

            @Override
            public Volume update(Volume existingEntity, Volume requestedEntity) {
                existingEntity.setInstanceId(instanceId);
                existingEntity.setDevice(deviceId);
                return existingEntity;
            }

            @Override
            public void handleResult(Volume result) {
            }
        });
    }

    private void addVolumeToInstanceRecord(final String instanceId, final String volumeId) {
        PId instanceIdId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        BlockingDhtWriter blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(instanceIdId, null, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.getBlockDeviceMappings().add(new BlockDeviceMapping(volumeId));
                return existingEntity;
            }

            @Override
            public void handleResult(Instance result) {
            }
        });
    }

    private void assertInstanceHasVolumeId(String instanceId, String volumeId) {
        BlockingDhtReader blockingReader = dhtClientFactory.createBlockingReader();
        PId id = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        Instance instance = (Instance) blockingReader.get(id);
        for (BlockDeviceMapping blockDeviceMapping : instance.getBlockDeviceMappings()) {
            if (volumeId.equals(blockDeviceMapping.getVolumeId()))
                return;
        }
        fail("volume " + volumeId + " not found in instance " + instanceId);
    }

    private void assertVolumeHasInstanceId(String volumeId, String instanceId) {
        BlockingDhtReader blockingReader = dhtClientFactory.createBlockingReader();
        PId id = piIdBuilder.getPIdForEc2AvailabilityZone(Volume.getUrl(volumeId));
        Volume volume = (Volume) blockingReader.get(id);
        assertEquals(instanceId, volume.getInstanceId());
    }

    private void assertLibvirtCommand(final String target) throws InterruptedException {
        assertTrue(stubLibvirtConnection.assertLibvirtCommand(target));
    }

    private String storeInstanceRecord() {
        String instanceId = String.format("i-01pEVj%02d", volumeIdIndex.get());
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instance.setAvailabilityZoneCode(99);
        instance.setRegionCode(99);
        instance.setNodeId(instanceManagerNodeId);

        PId instanceIdId = piIdBuilder.getPIdForEc2AvailabilityZone(instance);

        putPiEntityIntoDht(instance, instanceIdId);

        return instanceId;
    }

    private Volume storeVolumeRecord(VolumeState initialState) {
        return storeVolumeRecord(initialState, null);
    }

    private Volume storeVolumeRecord(VolumeState initialState, String snapshotId) {
        String volumeId = piIdBuilder.generateBase62Ec2Id("v", koalaIdFactory.getGlobalAvailabilityZoneCode());
        Volume result = new Volume();
        result.setVolumeId(volumeId);
        result.setSizeInGigaBytes(size);
        result.setStatus(initialState);
        result.setAvailabilityZone("UK");
        result.setAvailabilityZoneCode(99);
        result.setRegionCode(99);
        result.setSnapshotId(snapshotId);
        result.setOwnerId(userId);
        LOG.debug("Getting id for volumeId: " + volumeId);
        PId volumeIdId = piIdBuilder.getPIdForEc2AvailabilityZone(result);
        putPiEntityIntoDht(result, volumeIdId);

        return result;
    }

    private Snapshot storeSnapshotRecord(SnapshotState initialState) {
        return storeSnapshotRecord(initialState, null);
    }

    private Snapshot storeSnapshotRecord(SnapshotState initialState, String volumeId) {
        String snapshotId = piIdBuilder.generateBase62Ec2Id("snap", koalaIdFactory.getGlobalAvailabilityZoneCode());
        Snapshot result = new Snapshot();
        result.setSnapshotId(snapshotId);
        result.setStatus(initialState);
        result.setVolumeId(volumeId);
        result.setOwnerId(userId);
        PId piSnapshotId = piIdBuilder.getPIdForEc2AvailabilityZone(result);
        putPiEntityIntoDht(result, piSnapshotId);

        return result;
    }

    private void addSnapshotIdsToUser(Collection<String> snapshotIds) {
        User user = new User(userId, "", "");
        user.getSnapshotIds().addAll(snapshotIds);
        PId userId = piIdBuilder.getPId(user);
        putPiEntityIntoDht(user, userId);
    }

    private void addVolumeIdsToUser(Collection<String> volumeIds) {
        User user = new User(userId, "", "");
        user.getVolumeIds().addAll(volumeIds);
        PId userId = piIdBuilder.getPId(user);
        putPiEntityIntoDht(user, userId);
    }

    private void waitForUserRecordWithSnapshotIds(Collection<String> snapshotIds) throws InterruptedException {
        int count = 0;
        int delay = 500;
        int max = 20;
        User user = null;
        while (count < max) {
            BlockingDhtReader blockingReader = dhtClientFactory.createBlockingReader();
            PId id = piIdBuilder.getPId(User.getUrl(userId));
            user = (User) blockingReader.get(id);
            if (user.getSnapshotIds().size() == snapshotIds.size() && user.getSnapshotIds().containsAll(snapshotIds))
                return;
            count++;
            Thread.sleep(delay);
        }
        fail(String.format("user not in expected state (%s) after %d millis. the real state is %s", ArrayUtils.toString(snapshotIds), delay * max, ArrayUtils.toString(user.getSnapshotIds())));
    }

    private void waitForUserRecordWithVolumeIds(Collection<String> volumeIds) throws InterruptedException {
        int count = 0;
        int delay = 500;
        int max = 20;
        User user = null;
        while (count < max) {
            BlockingDhtReader blockingReader = dhtClientFactory.createBlockingReader();
            PId id = piIdBuilder.getPId(User.getUrl(userId));
            user = (User) blockingReader.get(id);
            if (user.getVolumeIds().size() == volumeIds.size() && user.getVolumeIds().containsAll(volumeIds))
                return;
            count++;
            Thread.sleep(delay);
        }
        fail(String.format("user not in expected state (%s) after %d millis. the real state is %s", ArrayUtils.toString(volumeIds), delay * max, ArrayUtils.toString(user.getSnapshotIds())));
    }

    private void waitForSnapshotState(String snapshotId, SnapshotState expectedState) throws InterruptedException {
        int count = 0;
        int delay = 500;
        int max = 20;
        while (count < max) {
            BlockingDhtReader blockingReader = dhtClientFactory.createBlockingReader();
            PId id = piIdBuilder.getPIdForEc2AvailabilityZone(Snapshot.getUrl(snapshotId));
            Snapshot snapshot = (Snapshot) blockingReader.get(id);
            if (expectedState.equals(snapshot.getStatus()))
                return;
            count++;
            Thread.sleep(delay);
        }
        fail(String.format("snapshot %s not in expected state (%s) after %d millis", snapshotId, expectedState, delay * max));
    }

    private void waitForVolumeState(String volumeId, VolumeState expectedState) throws InterruptedException {
        int count = 0;
        int delay = 500;
        int max = 20;
        while (count < max) {
            BlockingDhtReader blockingReader = dhtClientFactory.createBlockingReader();
            PId id = piIdBuilder.getPIdForEc2AvailabilityZone(Volume.getUrl(volumeId));
            Volume volume = (Volume) blockingReader.get(id);
            if (expectedState.equals(volume.getStatus()))
                return;
            count++;
            Thread.sleep(delay);
        }
        fail(String.format("volume %s not in expected state (%s) after %d millis", volumeId, expectedState, delay * max));
    }

    private static void putInstanceTypeIntoDht() {
        InstanceTypeConfiguration instanceTypeConfiguration = new InstanceTypeConfiguration("m1.small", 1, 256, 1);
        InstanceTypes instanceTypes = new InstanceTypes();
        instanceTypes.addInstanceType(instanceTypeConfiguration);
        PId instanceTypesId = piIdBuilder.getPId(instanceTypes);
        putPiEntityIntoDht(instanceTypes, instanceTypesId);
    }

    private static void putPiEntityIntoDht(PiEntity piEntity, PId id) {
        BlockingDhtWriter dhtWriter = dhtClientFactory.createBlockingWriter();
        dhtWriter.put(id, piEntity);
    }
}
