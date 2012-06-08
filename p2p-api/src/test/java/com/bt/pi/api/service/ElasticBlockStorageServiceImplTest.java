package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.api.utils.IdFactory;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZoneNotFoundException;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.continuation.scattergather.PiScatterGatherContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class ElasticBlockStorageServiceImplTest {
    private static final int VOLUME_RETRIES = 5;
    @InjectMocks
    private ElasticBlockStorageServiceImpl elasticBlockStorageIntegrationImpl = new ElasticBlockStorageServiceImpl();
    private String ownerId = "bloggs";
    private String volumeId = "vol-12345678";
    private String instanceId = "i-12344444";
    private String device = "/dev/sda2";
    private int size = 1;
    private String availabilityZone = "USA";
    private int availabilityZoneCode = 123;
    private int regionCode = 234;
    private String snapshotId = "snap-12345678";
    private List<String> snapshotIds;
    private List<String> volumeIds;
    private boolean force;
    @Mock
    private UserService userservice;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private ApiApplicationManager apiApplicationManager;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private BlockingDhtReader blockingDhtReader;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private Volume volume1;
    @Mock
    private Volume volume2;
    private String volumeId1 = "v-1";
    private String volumeId2 = "v-2";
    private String volumeId3 = "v-3";
    @Mock
    private Snapshot snapshot1;
    @Mock
    private Snapshot snapshot2;
    private String snapshotId1 = "s-1";
    private String snapshotId2 = "s-2";
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private BlockingDhtWriter blockingWriter;
    @Mock
    private IdFactory idFactory;
    @Mock
    private Instance instance;
    @Mock
    private Volume volume;
    @Mock
    private Snapshot snapshot;
    @Mock
    private PId instancePastryId;
    @Mock
    private PId volumePastryId;
    @Mock
    private PId snapshotPastryId;
    @Mock
    private User user;
    @Mock
    private PId volumePastryId1;
    @Mock
    private PId volumePastryId2;
    @Mock
    private PId volumePastryId3;
    @Mock
    private PId snapshotPastryId1;
    @Mock
    private PId snapshotPastryId2;
    @Mock
    private DhtReader dhtReader;
    private AvailabilityZone zone;
    @Mock
    private PId createVolumeQueueId;
    @Mock
    private PId deleteVolumeQueueId;
    @Mock
    private PId detachVolumeQueueId;
    @Mock
    private PId attachVolumeQueueId;
    @Mock
    private PId createSnapshotQueueId;
    @Mock
    private PId deleteSnapshotQueueId;
    private int globalAvzCode;

    @Before
    public void before() {
        setupVolume();
        zone = new AvailabilityZone(availabilityZone, availabilityZoneCode, regionCode, "status");
        AvailabilityZones availabilityZones = new AvailabilityZones();
        availabilityZones.addAvailabilityZone(zone);

        when(apiApplicationManager.getAvailabilityZoneByName(availabilityZone)).thenReturn(zone);
        when(apiApplicationManager.getAvailabilityZonesRecord()).thenReturn(availabilityZones);

        when(dhtClientFactory.createBlockingReader()).thenReturn(blockingDhtReader);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(blockingWriter);
        when(idFactory.createNewVolumeId(anyInt())).thenReturn(volumeId);
        when(idFactory.createNewSnapshotId(anyInt())).thenReturn(snapshotId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(eq(Instance.getUrl(instanceId)))).thenReturn(instancePastryId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(eq(Volume.getUrl(volumeId)))).thenReturn(volumePastryId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(volumeId)).thenReturn(zone.getGlobalAvailabilityZoneCode());
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instanceId)).thenReturn(zone.getGlobalAvailabilityZoneCode());
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(snapshotId)).thenReturn(availabilityZoneCode);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(Snapshot.class))).thenReturn(snapshotPastryId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Snapshot.getUrl(snapshotId))).thenReturn(snapshotPastryId);
        when(createVolumeQueueId.forGlobalAvailablityZoneCode(zone.getGlobalAvailabilityZoneCode())).thenReturn(createVolumeQueueId);
        when(attachVolumeQueueId.forGlobalAvailablityZoneCode(zone.getGlobalAvailabilityZoneCode())).thenReturn(attachVolumeQueueId);
        when(detachVolumeQueueId.forGlobalAvailablityZoneCode(zone.getGlobalAvailabilityZoneCode())).thenReturn(detachVolumeQueueId);
        when(deleteVolumeQueueId.forGlobalAvailablityZoneCode(zone.getGlobalAvailabilityZoneCode())).thenReturn(deleteVolumeQueueId);
        when(createSnapshotQueueId.forGlobalAvailablityZoneCode(availabilityZoneCode)).thenReturn(createSnapshotQueueId);
        when(deleteSnapshotQueueId.forGlobalAvailablityZoneCode(availabilityZoneCode)).thenReturn(deleteSnapshotQueueId);
        when(piIdBuilder.getPId(PiQueue.CREATE_VOLUME.getUrl())).thenReturn(createVolumeQueueId);
        when(piIdBuilder.getPId(PiQueue.ATTACH_VOLUME.getUrl())).thenReturn(attachVolumeQueueId);
        when(piIdBuilder.getPId(PiQueue.DETACH_VOLUME.getUrl())).thenReturn(detachVolumeQueueId);
        when(piIdBuilder.getPId(PiQueue.DELETE_VOLUME.getUrl())).thenReturn(deleteVolumeQueueId);
        when(piIdBuilder.getPId(PiQueue.CREATE_SNAPSHOT.getUrl())).thenReturn(createSnapshotQueueId);
        when(piIdBuilder.getPId(PiQueue.DELETE_SNAPSHOT.getUrl())).thenReturn(deleteSnapshotQueueId);
        when(blockingDhtReader.get(volumePastryId)).thenReturn(volume);
        when(blockingDhtReader.get(instancePastryId)).thenReturn(instance);
        when(blockingDhtReader.get(snapshotPastryId)).thenReturn(snapshot);

        when(apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(eq(PiTopics.ATTACH_VOLUME), eq(zone.getGlobalAvailabilityZoneCode()))).thenReturn(pubSubMessageContext);
        when(apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(eq(PiTopics.CREATE_SNAPSHOT), eq(globalAvzCode))).thenReturn(pubSubMessageContext);
        when(apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(eq(PiTopics.DELETE_SNAPSHOT), eq(globalAvzCode))).thenReturn(pubSubMessageContext);
        setupSnapshot();
    }

    private void setupSnapshot() {
        when(snapshot.getOwnerId()).thenReturn(ownerId);
        when(snapshot.getVolumeId()).thenReturn(volumeId);
        when(snapshot.getUrl()).thenReturn(Snapshot.getUrl(snapshotId));
        when(snapshot.getAvailabilityZoneCode()).thenReturn(availabilityZoneCode);
        when(snapshot.getAvailabilityZone()).thenReturn(availabilityZone);
        when(snapshot.getRegionCode()).thenReturn(regionCode);
    }

    private void setupScatterGather() {
        ScatterGatherContinuationRunner scatterGatherContinuationRunner = new ScatterGatherContinuationRunner();
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(5);
        scatterGatherContinuationRunner.setScheduledExecutorService(scheduledExecutorService);
        this.elasticBlockStorageIntegrationImpl.setScatterGatherContinuationRunner(scatterGatherContinuationRunner);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttachVolumeNoIdProvided() {
        // setup

        // act
        elasticBlockStorageIntegrationImpl.attachVolume(ownerId, null, instanceId, device);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttachVolumeNoInstanceIdProvided() {
        // setup

        // act
        elasticBlockStorageIntegrationImpl.attachVolume(ownerId, volumeId, null, device);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttachVolumeNoDeviceProvided() {
        // setup

        // act
        try {
            elasticBlockStorageIntegrationImpl.attachVolume(ownerId, volumeId, instanceId, null);
        } catch (IllegalArgumentException e) {
            assertEquals("device must be provided", e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToAttachVolumeWhenVolumeAndInstanceIdsInDifferentAvzs() {
        // act
        try {
            elasticBlockStorageIntegrationImpl.attachVolume(ownerId, volumeId, "i-1111", device);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not in the same availability zone"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttachVolumeInvalidInstanceId() {
        // setup
        when(blockingDhtReader.get(instancePastryId)).thenReturn(null);
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        // act
        try {
            elasticBlockStorageIntegrationImpl.attachVolume(ownerId, volumeId, instanceId, device);
        } catch (IllegalArgumentException e) {
            assertEquals("instance " + instanceId + " not found", e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttachVolumeNotFound() {
        // setup
        when(blockingDhtReader.get(volumePastryId)).thenReturn(null);

        // act
        elasticBlockStorageIntegrationImpl.attachVolume(ownerId, volumeId, instanceId, device);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttachVolumeNotOwner() {
        // setup
        when(volume.getOwnerId()).thenReturn("fred");
        when(instance.getState()).thenReturn(InstanceState.RUNNING);

        // act
        elasticBlockStorageIntegrationImpl.attachVolume(ownerId, volumeId, instanceId, device);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttachVolumeNotInAvailableStatus() {
        // setup
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.DELETED);
        when(instance.getState()).thenReturn(InstanceState.RUNNING);

        // act
        elasticBlockStorageIntegrationImpl.attachVolume(ownerId, volumeId, instanceId, device);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttachVolumeInstanceNotRunning() {
        // setup
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);
        when(instance.getState()).thenReturn(InstanceState.PENDING);

        // act
        try {
            elasticBlockStorageIntegrationImpl.attachVolume(ownerId, volumeId, instanceId, device);
        } catch (IllegalArgumentException e) {
            // assert
            assertEquals("instance must be running", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testAttachVolume() throws Exception {
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);
        when(instance.getState()).thenReturn(InstanceState.RUNNING);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Volume volume = (Volume) invocation.getArguments()[1];
                assertEquals(VolumeState.ATTACHING, volume.getStatus());
                assertEquals(device, volume.getDevice());
                assertEquals(volumeId, volume.getVolumeId());
                assertEquals(instanceId, volume.getInstanceId());
                return null;
            }
        }).when(pubSubMessageContext).randomAnycast(eq(EntityMethod.UPDATE), isA(Volume.class));

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(Volume.getUrl(volumeId), null);
                latch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(eq(attachVolumeQueueId), eq(Volume.getUrl(volumeId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));

        // act
        Volume result = elasticBlockStorageIntegrationImpl.attachVolume(ownerId, volumeId, instanceId, device);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertEquals(VolumeState.ATTACHING, result.getStatus());
        assertEquals(volumeId, result.getVolumeId());
        assertEquals(instanceId, result.getInstanceId());
        verify(pubSubMessageContext).randomAnycast(eq(EntityMethod.UPDATE), isA(Volume.class));
        verify(this.taskProcessingQueueHelper).addUrlToQueue(eq(attachVolumeQueueId), eq(result.getUrl()), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void testCreateSnapshot() {
        // setup
        setupVolume();
        String description = "description";

        // act
        Snapshot result = elasticBlockStorageIntegrationImpl.createSnapshot(ownerId, volumeId, description);

        // assert
        assertNotNull(result);
        assertEquals(snapshotId, result.getSnapshotId());
        assertEquals(volumeId, result.getVolumeId());
        assertEquals(ownerId, result.getOwnerId());
        assertEquals(description, result.getDescription());
        assertEquals(0.0, result.getProgress(), 0.0);
        assertEquals(SnapshotState.PENDING, result.getStatus());
        assertTimestamp(System.currentTimeMillis(), result.getStatusTimestamp(), 100);
        assertTimestamp(System.currentTimeMillis(), result.getStartTime(), 100);

        assertEquals(availabilityZone, result.getAvailabilityZone());
    }

    private void setupVolume() {
        when(volume.getAvailabilityZoneCode()).thenReturn(availabilityZoneCode);
        when(volume.getAvailabilityZone()).thenReturn(availabilityZone);
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);
        when(volume.getRegionCode()).thenReturn(regionCode);
        globalAvzCode = PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(volume.getRegionCode(), volume.getAvailabilityZoneCode());

    }

    private void assertTimestamp(long expected, long actual, long wobble) {
        assertTrue(Math.abs(actual - expected) <= wobble);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenCreateSnapshotWithNullVolumeId() {
        elasticBlockStorageIntegrationImpl.createSnapshot(ownerId, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowNotAutorizedExceptionIfUserDoesNotOwnVolume() {
        // setup
        when(volume.getOwnerId()).thenReturn("Another User Id");

        // act
        elasticBlockStorageIntegrationImpl.createSnapshot(ownerId, volumeId, null);
    }

    @Test
    public void createSnapshotShouldCreateSnapshotIdBasedOnGlobalAvailabilityZoneCode() {
        // act
        elasticBlockStorageIntegrationImpl.createSnapshot(ownerId, volumeId, null);
        // assert
        verify(idFactory).createNewSnapshotId(globalAvzCode);
    }

    @Test
    public void createSnapshotShouldAddSnapshotIdToUser() {

        // act
        elasticBlockStorageIntegrationImpl.createSnapshot(ownerId, volumeId, null);

        // assert
        verify(userservice).addSnapshotToUser(ownerId, snapshotId);
    }

    @Test
    public void createSnapshotShouldPersistSnapShotEntity() {

        // act
        elasticBlockStorageIntegrationImpl.createSnapshot(ownerId, volumeId, null);

        // assert
        verify(blockingWriter).put(eq(snapshotPastryId), argThat(new ArgumentMatcher<Snapshot>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof Snapshot))
                    return false;
                Snapshot s = (Snapshot) argument;
                if (s.getAvailabilityZoneCode() != availabilityZoneCode)
                    return false;
                if (s.getRegionCode() != regionCode)
                    return false;
                return true;
            }
        }));
    }

    @Test
    public void createSnapshotShouldAddToTaskProcessingQueue() {

        // act
        elasticBlockStorageIntegrationImpl.createSnapshot(ownerId, volumeId, null);

        // assert
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(createSnapshotQueueId), eq(Snapshot.getUrl(snapshotId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void createSnapshotShouldSendAnycast() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(Snapshot.getUrl(snapshotId), null);
                latch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(eq(createSnapshotQueueId), eq(Snapshot.getUrl(snapshotId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));

        // act
        elasticBlockStorageIntegrationImpl.createSnapshot(ownerId, volumeId, null);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        verify(pubSubMessageContext).randomAnycast(eq(EntityMethod.CREATE), isA(Snapshot.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteSnapshotShouldThrowIllegalArgumentExceptionIfSnapshotIdIsNullOrEmpty() {
        elasticBlockStorageIntegrationImpl.deleteSnapshot(ownerId, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteSnapshotShouldThrowIllegalArgumentExceptionIfUserDoesNotOwnSnapshot() {
        // setup

        // act
        elasticBlockStorageIntegrationImpl.deleteSnapshot("ANOTHER OWNER ID", snapshotId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteSnapshotShouldThrowExceptionIfSnapshotNotInCompleteState() {
        // setup
        when(snapshot.getStatus()).thenReturn(SnapshotState.PENDING);
        // act
        elasticBlockStorageIntegrationImpl.deleteSnapshot("ANOTHER OWNER ID", snapshotId);
    }

    @Test
    public void deleteSnapshotShouldAddToTaskProcessingQueue() {
        // setup
        when(snapshot.getStatus()).thenReturn(SnapshotState.COMPLETE);

        // act
        elasticBlockStorageIntegrationImpl.deleteSnapshot(ownerId, snapshotId);

        // assert
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(deleteSnapshotQueueId), eq(Snapshot.getUrl(snapshotId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void deleteSnapshotShouldReturnTrueIfAlreadyDeleted() {
        // setup
        when(snapshot.getStatus()).thenReturn(SnapshotState.DELETED);

        // act
        boolean result = elasticBlockStorageIntegrationImpl.deleteSnapshot(ownerId, snapshotId);

        // assert
        assertTrue(result);
    }

    @Test
    public void deleteSnapshotShouldSendAnycast() throws Exception {
        // setup
        when(snapshot.getStatus()).thenReturn(SnapshotState.COMPLETE);

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(Snapshot.getUrl(snapshotId), null);
                latch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(eq(deleteSnapshotQueueId), eq(Snapshot.getUrl(snapshotId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));

        // act
        elasticBlockStorageIntegrationImpl.deleteSnapshot(ownerId, snapshotId);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        verify(pubSubMessageContext).randomAnycast(eq(EntityMethod.DELETE), isA(Snapshot.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateVolumeZeroSize() {
        // setup
        size = 0;

        // act
        elasticBlockStorageIntegrationImpl.createVolume(ownerId, size, availabilityZone, null);
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testCreateVolume() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Volume vol = (Volume) invocation.getArguments()[1];
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[2];
                continuation.update(null, vol);
                return null;
            }
        }).when(blockingWriter).update(isA(PId.class), isA(Volume.class), isA(UpdateResolvingPiContinuation.class));

        when(this.apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(PiTopics.CREATE_VOLUME, zone.getGlobalAvailabilityZoneCode())).thenReturn(pubSubMessageContext);

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(Volume.getUrl(volumeId), null);
                latch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(eq(createVolumeQueueId), eq(Volume.getUrl(volumeId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));

        // act
        Volume result = elasticBlockStorageIntegrationImpl.createVolume(ownerId, size, availabilityZone, null);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(result.getVolumeId().startsWith("vol-"));
        assertEquals(size, result.getSizeInGigaBytes());
        assertEquals(VolumeState.CREATING, result.getStatus());
        assertEquals(availabilityZone, result.getAvailabilityZone());
        assertNull(result.getSnapshotId());
        assertEquals(ownerId, result.getOwnerId());
        assertCreatedDate(result.getCreateTime());
        verify(this.userservice).addVolumeToUser(ownerId, result.getVolumeId());
        verify(pubSubMessageContext).randomAnycast(eq(EntityMethod.CREATE), eq(result));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateVolumeInvalidAvailabilityZone() throws Exception {
        // setup
        when(apiApplicationManager.getAvailabilityZoneByName("invalid")).thenThrow(new AvailabilityZoneNotFoundException(""));

        // act
        elasticBlockStorageIntegrationImpl.createVolume(ownerId, size, "invalid", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateVolumeSnapshotNotFound() throws Exception {
        // setup
        when(blockingDhtReader.get(snapshotPastryId)).thenReturn(null);

        // act
        elasticBlockStorageIntegrationImpl.createVolume(ownerId, size, availabilityZone, snapshotId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateVolumeSnapshotNotComplete() throws Exception {
        // setup
        when(snapshot.getStatus()).thenReturn(SnapshotState.CREATING);

        // act
        elasticBlockStorageIntegrationImpl.createVolume(ownerId, size, availabilityZone, snapshotId);
    }

    private void assertCreatedDate(long createTime) {
        long now = System.currentTimeMillis();
        assertTrue(Math.abs(createTime - now) < 5000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteVolumeNotFound() {
        // setup
        when(blockingDhtReader.get(isA(PId.class))).thenReturn(null);

        // act
        try {
            elasticBlockStorageIntegrationImpl.deleteVolume(ownerId, volumeId);
        } catch (IllegalArgumentException e) {
            assertEquals("volume " + volumeId + " not found", e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteVolumeNotOwner() {
        // setup
        when(volume.getOwnerId()).thenReturn("fred");

        // act
        try {
            elasticBlockStorageIntegrationImpl.deleteVolume(ownerId, volumeId);
        } catch (IllegalArgumentException e) {
            assertEquals("volume " + volumeId + " is not owned by " + ownerId, e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteVolumeNotInAvailableState() {
        // setup
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);

        // act
        try {
            elasticBlockStorageIntegrationImpl.deleteVolume(ownerId, volumeId);
        } catch (IllegalArgumentException e) {
            assertEquals("volume " + volumeId + " is in-use, should be in [available, deleted] state", e.getMessage());
            throw e;
        }
    }

    @Test
    public void deleteVolumeShouldReturnTrueIfAlreadyDeleted() {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.DELETED);

        // act
        boolean result = elasticBlockStorageIntegrationImpl.deleteVolume(ownerId, volumeId);

        // assert
        assertTrue(result);
    }

    @Test
    public void testDeleteVolume() throws Exception {
        // setup
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        when(this.apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(PiTopics.DELETE_VOLUME, zone.getGlobalAvailabilityZoneCode())).thenReturn(pubSubMessageContext);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Volume volume = (Volume) invocation.getArguments()[0];
                assertEquals(VolumeState.DELETING, volume.getStatus());
                assertEquals(volumeId, volume.getVolumeId());
                return null;
            }
        }).when(this.pubSubMessageContext).sendAnycast(eq(EntityMethod.DELETE), isA(Volume.class));

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(Volume.getUrl(volumeId), null);
                latch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(eq(deleteVolumeQueueId), eq(Volume.getUrl(volumeId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));

        // act
        boolean result = elasticBlockStorageIntegrationImpl.deleteVolume(ownerId, volumeId);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(result);
        verify(this.pubSubMessageContext).randomAnycast(eq(EntityMethod.DELETE), isA(Volume.class));
    }

    @Test
    public void shouldAddVolumeUrlToQueueOnDeleteVolume() throws Exception {
        // setup
        volume = new Volume();
        volume.setVolumeId(volumeId);
        volume.setOwnerId(ownerId);
        volume.setStatus(VolumeState.AVAILABLE);

        when(piIdBuilder.getPIdForEc2AvailabilityZone(Volume.getUrl(volumeId))).thenReturn(volumePastryId);
        when(blockingDhtReader.get(volumePastryId)).thenReturn(volume);
        when(this.apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(eq(PiTopics.DELETE_VOLUME), anyInt())).thenReturn(pubSubMessageContext);

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(Volume.getUrl(volumeId), null);
                latch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(eq(deleteVolumeQueueId), eq(Volume.getUrl(volumeId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));

        // act
        elasticBlockStorageIntegrationImpl.deleteVolume(ownerId, volumeId);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(deleteVolumeQueueId), eq(volume.getUrl()), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));
    }

    @SuppressWarnings("unchecked")
    private void setupDescribeVolumeExpectations() {
        when(piIdBuilder.getPIdForEc2AvailabilityZone(eq(Volume.getUrl(volumeId1)))).thenReturn(volumePastryId1);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(eq(Volume.getUrl(volumeId2)))).thenReturn(volumePastryId2);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(eq(Volume.getUrl(volumeId3)))).thenReturn(volumePastryId3);
        when(this.userManagementService.getUser(ownerId)).thenReturn(user);

        Set<String> userVolumeIds = new HashSet<String>() {
            private static final long serialVersionUID = 1L;
            {
                this.add(volumeId1);
                this.add(volumeId2);
                this.add(volumeId3);
            }
        };

        when(user.getVolumeIds()).thenReturn(userVolumeIds);
        when(this.dhtClientFactory.createReader()).thenReturn(dhtReader);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiScatterGatherContinuation continuation = (PiScatterGatherContinuation) invocation.getArguments()[1];
                PId id = (PId) invocation.getArguments()[0];
                if (id.equals(volumePastryId1))
                    continuation.receiveResult(volume1);
                if (id.equals(volumePastryId2))
                    continuation.receiveResult(volume2);
                if (id.equals(volumePastryId3))
                    continuation.receiveResult(null);
                return null;
            }
        }).when(dhtReader).getAsync(isA(PId.class), isA(PiScatterGatherContinuation.class));
    }

    @SuppressWarnings("unchecked")
    private void setupDescribeSnapshotExpectations() {
        when(piIdBuilder.getPIdForEc2AvailabilityZone(eq(Snapshot.getUrl(snapshotId1)))).thenReturn(snapshotPastryId1);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(eq(Snapshot.getUrl(snapshotId2)))).thenReturn(snapshotPastryId2);
        when(this.userManagementService.getUser(ownerId)).thenReturn(user);

        Set<String> userSnapshotIds = new HashSet<String>() {
            private static final long serialVersionUID = 1L;
            {
                this.add(snapshotId1);
                this.add(snapshotId2);
            }
        };

        when(user.getSnapshotIds()).thenReturn(userSnapshotIds);
        when(this.dhtClientFactory.createReader()).thenReturn(dhtReader);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiScatterGatherContinuation<Snapshot> continuation = (PiScatterGatherContinuation<Snapshot>) invocation.getArguments()[1];
                PId id = (PId) invocation.getArguments()[0];
                if (id.equals(snapshotPastryId1))
                    continuation.receiveResult(snapshot1);
                if (id.equals(snapshotPastryId2))
                    continuation.receiveResult(snapshot2);
                return null;
            }
        }).when(dhtReader).getAsync(isA(PId.class), isA(PiScatterGatherContinuation.class));
    }

    @Test
    public void testDescribeVolumes() {
        // setup
        setupScatterGather();
        setupDescribeVolumeExpectations();

        // act
        List<Volume> result = elasticBlockStorageIntegrationImpl.describeVolumes(ownerId, volumeIds);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(volume1));
        assertTrue(result.contains(volume2));
    }

    @Test
    public void testDescribeVolumesExcludesDeletedVolumesThatAreStale() {
        // setup
        setupScatterGather();
        setupDescribeVolumeExpectations();
        long twohours = 1000 * 60 * 60 * 2;
        elasticBlockStorageIntegrationImpl.setDeletedAndStaleMillis(twohours - 100000);

        when(volume2.isDeleted()).thenReturn(true);
        when(volume2.getStatusTimestamp()).thenReturn(System.currentTimeMillis() - twohours);

        volumeIds = Arrays.asList(volumeId1, volumeId2, volumeId3);

        // act
        List<Volume> result = elasticBlockStorageIntegrationImpl.describeVolumes(ownerId, volumeIds);

        // assert
        assertEquals(1, result.size());
        assertTrue(result.contains(volume1));
    }

    @Test
    public void testDescribeVolumesSelected() {
        // setup
        setupScatterGather();
        setupDescribeVolumeExpectations();

        volumeIds = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;
            {
                add(volumeId2);
            }
        };

        // act
        List<Volume> result = elasticBlockStorageIntegrationImpl.describeVolumes(ownerId, volumeIds);

        // assert
        assertEquals(1, result.size());
        assertTrue(result.contains(volume2));
    }

    @Test
    public void testDescribeVolumesSelectedNoneFound() {
        // setup
        setupScatterGather();
        setupDescribeVolumeExpectations();

        volumeIds = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;
            {
                add("v-3");
            }
        };

        // act
        List<Volume> result = elasticBlockStorageIntegrationImpl.describeVolumes(ownerId, volumeIds);

        // assert
        assertEquals(0, result.size());
    }

    @Test
    public void testDescribeVolumesSelectedSomeNotFound() {
        // setup
        setupScatterGather();
        setupDescribeVolumeExpectations();

        volumeIds = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;
            {
                add(volumeId1);
                add(volumeId2);
                add("v-3");
            }
        };

        // act
        List<Volume> result = elasticBlockStorageIntegrationImpl.describeVolumes(ownerId, volumeIds);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(volume1));
        assertTrue(result.contains(volume2));
    }

    @Test
    public void describeSnapshotsShouldFindSnapshotIdsFromUser() {
        // setup
        setupScatterGather();
        setupDescribeSnapshotExpectations();
        when(snapshot1.getStatus()).thenReturn(SnapshotState.PENDING);
        when(snapshot2.getStatus()).thenReturn(SnapshotState.COMPLETE);

        // act
        List<Snapshot> snapshots = elasticBlockStorageIntegrationImpl.describeSnapshots(ownerId, null);

        // assert
        assertEquals(2, snapshots.size());
        assertTrue(snapshots.contains(snapshot1));
        assertTrue(snapshots.contains(snapshot2));
    }

    @Test
    public void describeSnapshotsExcludesDeletedAndStaleSnapshots() {
        // setup
        setupScatterGather();
        setupDescribeSnapshotExpectations();
        long twohours = 1000 * 60 * 60 * 2;
        elasticBlockStorageIntegrationImpl.setDeletedAndStaleMillis(twohours - 100000);
        when(snapshot1.isDeleted()).thenReturn(false);
        when(snapshot2.isDeleted()).thenReturn(true);
        when(snapshot2.getStatusTimestamp()).thenReturn(System.currentTimeMillis() - twohours);

        // act
        List<Snapshot> result = elasticBlockStorageIntegrationImpl.describeSnapshots(ownerId, null);

        // assert
        assertEquals(1, result.size());
        assertTrue(result.contains(snapshot1));
    }

    @Test
    public void describeSnapshotsIncludesDeletedAndNotStaleSnapshots() {
        // setup
        setupScatterGather();
        setupDescribeSnapshotExpectations();
        long twohours = 1000 * 60 * 60 * 2;
        elasticBlockStorageIntegrationImpl.setDeletedAndStaleMillis(twohours - 100000);
        when(snapshot1.isDeleted()).thenReturn(false);
        when(snapshot2.isDeleted()).thenReturn(true);
        when(snapshot2.getStatusTimestamp()).thenReturn(System.currentTimeMillis());

        // act
        List<Snapshot> result = elasticBlockStorageIntegrationImpl.describeSnapshots(ownerId, null);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(snapshot1));
    }

    @Test
    public void testDescribeSnapshotSelected() {
        // setup
        setupScatterGather();
        setupDescribeSnapshotExpectations();
        when(snapshot2.getStatus()).thenReturn(SnapshotState.COMPLETE);

        snapshotIds = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;
            {
                add(snapshotId2);
            }
        };

        // act
        List<Snapshot> result = elasticBlockStorageIntegrationImpl.describeSnapshots(ownerId, snapshotIds);

        // assert
        assertEquals(1, result.size());
        assertTrue(result.contains(snapshot2));
    }

    @Test
    public void testDescribeSnapshotSelectedNoneFound() {
        // setup
        setupScatterGather();
        setupDescribeSnapshotExpectations();

        snapshotIds = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;
            {
                add("s-3");
            }
        };

        // act
        List<Snapshot> result = elasticBlockStorageIntegrationImpl.describeSnapshots(ownerId, snapshotIds);

        // assert
        assertEquals(0, result.size());
    }

    @Test
    public void testDescribeSnapshotSelectedSomeNotFound() {
        // setup
        setupScatterGather();
        setupDescribeSnapshotExpectations();

        when(snapshot1.getStatus()).thenReturn(SnapshotState.COMPLETE);
        when(snapshot2.getStatus()).thenReturn(SnapshotState.PENDING);

        snapshotIds = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;
            {
                add(snapshotId1);
                add(snapshotId2);
                add("s-3");
            }
        };

        // act
        List<Snapshot> result = elasticBlockStorageIntegrationImpl.describeSnapshots(ownerId, snapshotIds);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(snapshot1));
        assertTrue(result.contains(snapshot2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetachVolumeNotFound() {
        // setup
        when(blockingDhtReader.get(isA(PId.class))).thenReturn(null);

        // act
        elasticBlockStorageIntegrationImpl.detachVolume(ownerId, volumeId, instanceId, device, force);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetachVolumeNotOwner() {
        // setup
        when(volume.getOwnerId()).thenReturn("fred");

        // act
        elasticBlockStorageIntegrationImpl.detachVolume(ownerId, volumeId, instanceId, device, force);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetachVolumeNotAttacted() {
        // setup
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.DELETED);

        // act
        elasticBlockStorageIntegrationImpl.detachVolume(ownerId, volumeId, instanceId, device, force);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetachVolumeNotAttactedToQuotedInstance() {
        // setup
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        when(volume.getInstanceId()).thenReturn("i-11122233");

        // act
        elasticBlockStorageIntegrationImpl.detachVolume(ownerId, volumeId, instanceId, device, force);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetachVolumeNotAttactedToQuotedDevice() {
        // setup
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        when(volume.getInstanceId()).thenReturn(instanceId);
        when(volume.getDevice()).thenReturn("/dev/null");

        // act
        elasticBlockStorageIntegrationImpl.detachVolume(ownerId, volumeId, instanceId, device, force);
    }

    @Test
    public void testDetachVolume() throws Exception {
        // setup
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        when(volume.getInstanceId()).thenReturn(instanceId);
        when(volume.getDevice()).thenReturn(device);

        when(apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(PiTopics.DETACH_VOLUME, zone.getGlobalAvailabilityZoneCode())).thenReturn(pubSubMessageContext);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Volume volume = (Volume) invocation.getArguments()[1];
                assertEquals(VolumeState.DETACHING, volume.getStatus());
                assertEquals(device, volume.getDevice());
                assertEquals(volumeId, volume.getVolumeId());
                assertEquals(instanceId, volume.getInstanceId());
                return null;
            }
        }).when(pubSubMessageContext).randomAnycast(eq(EntityMethod.UPDATE), isA(Volume.class));

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(Volume.getUrl(volumeId), null);
                latch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(eq(detachVolumeQueueId), eq(Volume.getUrl(volumeId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));

        // act
        Volume result = elasticBlockStorageIntegrationImpl.detachVolume(ownerId, volumeId, instanceId, device, force);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertEquals(VolumeState.DETACHING, result.getStatus());
        assertEquals(volumeId, result.getVolumeId());
        assertEquals(instanceId, result.getInstanceId());
        assertTrue(result.getAttachTime() != 0); // should set attach time
        verify(pubSubMessageContext).randomAnycast(eq(EntityMethod.UPDATE), isA(Volume.class));
        verify(this.taskProcessingQueueHelper).addUrlToQueue(eq(detachVolumeQueueId), eq(result.getUrl()), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));
        assertEquals(VolumeState.DETACHING, result.getStatus());
    }

    @Test
    public void testDetachVolumeForce() throws Exception {
        // setup
        force = true;
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        when(volume.getInstanceId()).thenReturn(instanceId);
        when(volume.getDevice()).thenReturn(device);

        when(apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(PiTopics.DETACH_VOLUME, zone.getGlobalAvailabilityZoneCode())).thenReturn(pubSubMessageContext);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Volume volume = (Volume) invocation.getArguments()[1];
                assertEquals(VolumeState.FORCE_DETACHING, volume.getStatus());
                assertEquals(device, volume.getDevice());
                assertEquals(volumeId, volume.getVolumeId());
                assertEquals(instanceId, volume.getInstanceId());
                return null;
            }
        }).when(pubSubMessageContext).randomAnycast(eq(EntityMethod.UPDATE), isA(Volume.class));

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(Volume.getUrl(volumeId), null);
                latch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(eq(detachVolumeQueueId), eq(Volume.getUrl(volumeId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));

        // act
        Volume result = elasticBlockStorageIntegrationImpl.detachVolume(ownerId, volumeId, instanceId, device, force);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertEquals(VolumeState.DETACHING, result.getStatus());
        assertEquals(volumeId, result.getVolumeId());
        assertEquals(instanceId, result.getInstanceId());
        assertTrue(result.getAttachTime() != 0); // should set attach time
        verify(pubSubMessageContext).randomAnycast(eq(EntityMethod.UPDATE), isA(Volume.class));
        verify(this.taskProcessingQueueHelper).addUrlToQueue(eq(detachVolumeQueueId), eq(result.getUrl()), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));
        assertEquals(VolumeState.DETACHING, result.getStatus());
    }

    @Test
    public void testDetachVolumeShouldReturnDeviceInResultEvenIfItIsNotProvidedInTheInput() {
        // setup
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        when(volume.getInstanceId()).thenReturn(instanceId);
        when(volume.getDevice()).thenReturn(device);
        when(apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(PiTopics.DETACH_VOLUME, zone.getGlobalAvailabilityZoneCode())).thenReturn(pubSubMessageContext);

        // act
        Volume result = elasticBlockStorageIntegrationImpl.detachVolume(ownerId, volumeId, instanceId, "", force);

        // assert
        assertEquals(device, result.getDevice());
    }

    @Test
    public void testThatDetachVolumePassedInstanceIdToVolumeManagerEvenIfNotSuppliedByAPI() throws Exception {
        // setup
        when(volume.getOwnerId()).thenReturn(ownerId);
        when(volume.getStatus()).thenReturn(VolumeState.IN_USE);
        when(volume.getInstanceId()).thenReturn(instanceId);
        when(volume.getDevice()).thenReturn(device);
        when(apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(PiTopics.DETACH_VOLUME, zone.getGlobalAvailabilityZoneCode())).thenReturn(pubSubMessageContext);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Volume volume = (Volume) invocation.getArguments()[1];
                assertEquals(VolumeState.DETACHING, volume.getStatus());
                assertEquals(device, volume.getDevice());
                assertEquals(volumeId, volume.getVolumeId());
                assertEquals(instanceId, volume.getInstanceId());
                return null;
            }
        }).when(pubSubMessageContext).randomAnycast(eq(EntityMethod.UPDATE), isA(Volume.class));

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(Volume.getUrl(volumeId), null);
                latch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(eq(detachVolumeQueueId), eq(Volume.getUrl(volumeId)), eq(VOLUME_RETRIES), isA(TaskProcessingQueueContinuation.class));

        // act
        Volume result = elasticBlockStorageIntegrationImpl.detachVolume(ownerId, volumeId, "", device, force);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertEquals(VolumeState.DETACHING, result.getStatus());
        assertEquals(volumeId, result.getVolumeId());
        assertEquals(instanceId, result.getInstanceId());
        verify(pubSubMessageContext).randomAnycast(eq(EntityMethod.UPDATE), isA(Volume.class));
    }
}
