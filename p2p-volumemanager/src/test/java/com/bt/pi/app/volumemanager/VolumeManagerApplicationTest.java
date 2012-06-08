package com.bt.pi.app.volumemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.p2p.scribe.ScribeMultiClient;
import rice.p2p.scribe.Topic;
import rice.pastry.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.entities.util.QueueOwnerRemovalContinuation;
import com.bt.pi.app.common.entities.util.QueueOwnerRemovalHelper;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.volumemanager.handlers.SnapshotHandler;
import com.bt.pi.app.volumemanager.handlers.VolumeHandler;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.scribe.KoalaScribeImpl;

@RunWith(MockitoJUnitRunner.class)
public class VolumeManagerApplicationTest {
    @Mock
    private NodeHandle leafNodeHandle1;
    @Mock
    private NodeHandle leafNodeHandle2;
    private Collection<NodeHandle> leafNodeHandles = Arrays.asList(new NodeHandle[] { leafNodeHandle1, leafNodeHandle2 });
    @InjectMocks
    private VolumeManagerApplication volumeManagerApplication = new VolumeManagerApplication() {
        @Override
        public String getNodeIdFull() {
            return nodeIdFull;
        }

        @Override
        public java.util.Collection<rice.pastry.NodeHandle> getLeafNodeHandles() {
            return leafNodeHandles;
        }
    };
    @Mock
    private VolumeHandler volumeHandler;
    @Mock
    private SnapshotHandler snapshotHandler;
    @Mock
    private KoalaScribeImpl aScribe;
    private Topic createVolumeTopic;
    private Topic deleteVolumeTopic;
    private Topic attachVolumeTopic;
    private Topic detachVolumeTopic;
    private Topic createSnapshotTopic;
    private Topic deleteSnapshotTopic;
    private KoalaIdFactory koalaIdFactory;
    private String nodeIdFull = "99998877";
    @Mock
    private VolumeManagerQueueManager volumeManagerQueueManager;
    @Mock
    private QueueOwnerRemovalHelper queueOwnerRemovalHelper;
    @Mock
    private KoalaIdUtils koalaIdUtils;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private Volume volume;
    @Mock
    private Snapshot snapshot;

    @Before
    public void before() {
        this.koalaIdFactory = new KoalaIdFactory(255, 255);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        this.volumeManagerApplication.setKoalaIdFactory(koalaIdFactory);
    }

    @Test
    public void testConstructor() {
        // assert
        assertEquals(Integer.parseInt(VolumeManagerApplication.DEFAULT_ACTIVATION_CHECK_PERIOD_SECS), volumeManagerApplication.getActivationCheckPeriodSecs());
        assertEquals(Long.parseLong(VolumeManagerApplication.DEFAULT_START_TIMEOUT_MILLIS), volumeManagerApplication.getStartTimeout());
        assertEquals(TimeUnit.MILLISECONDS, volumeManagerApplication.getStartTimeoutUnit());
    }

    @Test
    public void testOnApplicationStarting() {
        // setup
        setupTopics();
        Topic topic = mock(Topic.class);
        when(aScribe.subscribeToTopic(isA(String.class), isA(ScribeMultiClient.class))).thenReturn(topic);

        when(koalaIdUtils.isIdClosestToMe(eq(nodeIdFull), eq(leafNodeHandles), isA(Id.class), eq(NodeScope.AVAILABILITY_ZONE))).thenReturn(false).thenReturn(true);

        // act
        volumeManagerApplication.onApplicationStarting();

        // assert
        assertTrue(this.volumeManagerApplication.getSubscribedTopics().contains(createVolumeTopic));
        assertTrue(this.volumeManagerApplication.getSubscribedTopics().contains(deleteVolumeTopic));
        assertTrue(this.volumeManagerApplication.getSubscribedTopics().contains(detachVolumeTopic));
        assertTrue(this.volumeManagerApplication.getSubscribedTopics().contains(attachVolumeTopic));
        assertTrue(this.volumeManagerApplication.getSubscribedTopics().contains(createSnapshotTopic));
        assertTrue(this.volumeManagerApplication.getSubscribedTopics().contains(deleteSnapshotTopic));

        verify(volumeManagerQueueManager).createVolumeApplicationWatchers(nodeIdFull);
    }

    @Test
    public void testGetApplicationName() {
        // act
        String result = volumeManagerApplication.getApplicationName();

        // assert
        assertEquals("pi-volume-manager", result);
    }

    @Test
    public void testOverrideDefaultActivationPeriod() {
        // setup
        int period = 3333;
        volumeManagerApplication.setActivationCheckPeriodSecs(period);

        // act
        int result = volumeManagerApplication.getActivationCheckPeriodSecs();

        // assert
        assertEquals(period, result);
    }

    @Test
    public void testOverrideDefaultStartTimeout() {
        // setup
        long timeout = 4444;
        volumeManagerApplication.setStartTimeout(timeout);

        // act
        long result = volumeManagerApplication.getStartTimeout();

        // assert
        assertEquals(timeout, result);
    }

    private void setupTopics() {
        createVolumeTopic = new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.CREATE_VOLUME.getPiLocation()).forLocalScope(PiTopics.CREATE_VOLUME.getNodeScope())));
        deleteVolumeTopic = new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.DELETE_VOLUME.getPiLocation()).forLocalScope(PiTopics.DELETE_VOLUME.getNodeScope())));
        attachVolumeTopic = new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.ATTACH_VOLUME.getPiLocation()).forLocalScope(PiTopics.ATTACH_VOLUME.getNodeScope())));
        detachVolumeTopic = new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.DETACH_VOLUME.getPiLocation()).forLocalScope(PiTopics.DETACH_VOLUME.getNodeScope())));
        createSnapshotTopic = new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.CREATE_SNAPSHOT.getPiLocation()).forLocalScope(PiTopics.CREATE_SNAPSHOT.getNodeScope())));
        deleteSnapshotTopic = new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.DELETE_SNAPSHOT.getPiLocation()).forLocalScope(PiTopics.CREATE_SNAPSHOT.getNodeScope())));

        when(aScribe.subscribeToTopic(eq(PiTopics.CREATE_VOLUME.getUrl()), isA(ScribeMultiClient.class))).thenReturn(createVolumeTopic);
        when(aScribe.subscribeToTopic(eq(PiTopics.DELETE_VOLUME.getUrl()), isA(ScribeMultiClient.class))).thenReturn(deleteVolumeTopic);
        when(aScribe.subscribeToTopic(eq(PiTopics.ATTACH_VOLUME.getUrl()), isA(ScribeMultiClient.class))).thenReturn(attachVolumeTopic);
        when(aScribe.subscribeToTopic(eq(PiTopics.DETACH_VOLUME.getUrl()), isA(ScribeMultiClient.class))).thenReturn(detachVolumeTopic);
        when(aScribe.subscribeToTopic(eq(PiTopics.CREATE_SNAPSHOT.getUrl()), isA(ScribeMultiClient.class))).thenReturn(createSnapshotTopic);
        when(aScribe.subscribeToTopic(eq(PiTopics.DELETE_SNAPSHOT.getUrl()), isA(ScribeMultiClient.class))).thenReturn(deleteSnapshotTopic);
    }

    @Test
    public void testHandleAnycastCreateVolume() {
        // setup
        setupTopics();
        this.volumeManagerApplication.onApplicationStarting();
        when(pubSubMessageContext.getTopicPId()).thenReturn(koalaIdFactory.convertToPId(createVolumeTopic.getId()));
        when(volume.getStatus()).thenReturn(VolumeState.CREATING);
        when(this.volumeHandler.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, volume, nodeIdFull)).thenReturn(true);

        // act
        boolean result = volumeManagerApplication.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, volume);

        // assert
        assertTrue(result);
    }

    @Test
    public void testHandleAnycastCreateSnapshot() {
        // setup
        setupTopics();
        this.volumeManagerApplication.onApplicationStarting();
        when(pubSubMessageContext.getTopicPId()).thenReturn(koalaIdFactory.convertToPId(createSnapshotTopic.getId()));
        when(this.snapshotHandler.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, snapshot, nodeIdFull)).thenReturn(true);

        // act
        boolean result = volumeManagerApplication.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, snapshot);

        // assert
        assertTrue(result);
    }

    @Test
    public void testHandleAnycastNotVolume() {
        // setup
        Instance instance = mock(Instance.class);

        // act
        boolean result = volumeManagerApplication.handleAnycast(pubSubMessageContext, EntityMethod.UPDATE, instance);

        // assert
        assertFalse(result);
    }

    @Test
    public void testHandleAnycastWrongTopic() {
        // setup
        setupTopics();
        PId anotherTopicPId = mock(PId.class);
        this.volumeManagerApplication.onApplicationStarting();
        when(pubSubMessageContext.getTopicPId()).thenReturn(anotherTopicPId);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        // act
        boolean result = volumeManagerApplication.handleAnycast(pubSubMessageContext, EntityMethod.UPDATE, volume);

        // assert
        assertFalse(result);
    }

    @Test
    public void testHandleNodeDeparture() {
        // setup
        String nodeId = "nodeId";
        ArrayList<PiLocation> locations = new ArrayList<PiLocation>();
        locations.add(PiQueue.ATTACH_VOLUME.getPiLocation());
        locations.add(PiQueue.CREATE_VOLUME.getPiLocation());
        locations.add(PiQueue.DETACH_VOLUME.getPiLocation());
        locations.add(PiQueue.DELETE_VOLUME.getPiLocation());
        locations.add(PiQueue.CREATE_SNAPSHOT.getPiLocation());
        locations.add(PiQueue.DELETE_SNAPSHOT.getPiLocation());
        locations.add(PiQueue.REMOVE_SNAPSHOT_FROM_USER.getPiLocation());
        locations.add(PiQueue.REMOVE_VOLUME_FROM_USER.getPiLocation());
        final TaskProcessingQueue queue = mock(TaskProcessingQueue.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                QueueOwnerRemovalContinuation cont = ((QueueOwnerRemovalContinuation) invocation.getArguments()[1]);
                cont.update(queue, null);
                cont.handleResult(queue);
                return null;
            }
        }).when(queueOwnerRemovalHelper).removeNodeIdFromAllQueues(eq(locations), isA(QueueOwnerRemovalContinuation.class));

        // act
        volumeManagerApplication.handleNodeDeparture(nodeId);

        // assert
        verify(queue).removeOwnerFromAllTasks(eq(nodeId));
    }

    @Test
    public void handleNodeDepartureShouldCheckQueueWatchingStatus() {
        // setup
        String nodeId = "nodeId";
        when(koalaIdUtils.isIdClosestToMe(eq(nodeIdFull), eq(leafNodeHandles), isA(Id.class))).thenReturn(false);

        ArrayList<PiLocation> locations = new ArrayList<PiLocation>();
        locations.add(PiQueue.ATTACH_VOLUME.getPiLocation());
        locations.add(PiQueue.CREATE_VOLUME.getPiLocation());
        locations.add(PiQueue.DETACH_VOLUME.getPiLocation());
        locations.add(PiQueue.DELETE_VOLUME.getPiLocation());
        locations.add(PiQueue.CREATE_SNAPSHOT.getPiLocation());
        locations.add(PiQueue.DELETE_SNAPSHOT.getPiLocation());
        locations.add(PiQueue.REMOVE_SNAPSHOT_FROM_USER.getPiLocation());
        locations.add(PiQueue.REMOVE_VOLUME_FROM_USER.getPiLocation());
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                QueueOwnerRemovalContinuation cont = ((QueueOwnerRemovalContinuation) invocation.getArguments()[1]);
                cont.handleResult(null);
                return null;
            }
        }).when(queueOwnerRemovalHelper).removeNodeIdFromAllQueues(eq(locations), isA(QueueOwnerRemovalContinuation.class));

        // act
        volumeManagerApplication.handleNodeDeparture(nodeId);

        // assert
        verify(volumeManagerQueueManager).removeVolumeApplicationWatchers();
    }

    @Test
    public void handleNodeArrivalShouldCheckQueueWatchingStatus() {
        // setup
        String nodeId = "nodeId";
        when(koalaIdUtils.isIdClosestToMe(eq(nodeIdFull), eq(leafNodeHandles), isA(Id.class))).thenReturn(false);

        // act
        volumeManagerApplication.handleNodeArrival(nodeId);

        // assert
        verify(volumeManagerQueueManager).removeVolumeApplicationWatchers();
    }
}
