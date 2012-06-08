package com.bt.pi.app.imagemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

import rice.p2p.scribe.ScribeMultiClient;
import rice.p2p.scribe.Topic;
import rice.pastry.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.util.QueueOwnerRemovalContinuation;
import com.bt.pi.app.common.entities.util.QueueOwnerRemovalHelper;
import com.bt.pi.app.common.os.FileManager;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.scribe.KoalaScribeImpl;

@RunWith(MockitoJUnitRunner.class)
public class ImageManagerApplicationTest {
    @Mock
    private NodeHandle leafNodeHandle1;
    @Mock
    private NodeHandle leafNodeHandle2;
    protected Collection<NodeHandle> leafNodeHandles = Arrays.asList(new NodeHandle[] { leafNodeHandle1, leafNodeHandle2 });
    @InjectMocks
    private ImageManagerApplication imageManager = new ImageManagerApplication() {
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
    private Image image;
    @Mock
    private DecryptionHandler decryptionHandler;
    @Mock
    private KoalaScribeImpl koalaScribeImpl;
    @Mock
    private Topic topic;
    @Mock
    private AlwaysOnApplicationActivator applicationActivator;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private ImageManagerApplicationWatcherManager imageManagerApplicationWatcherManager;
    private String nodeIdFull = "aaaaaaaaaaassssssss";
    @Mock
    private QueueOwnerRemovalHelper queueOwnerRemovalHelper;
    @Mock
    private KoalaIdUtils koalaIdUtils;
    @Mock
    private FileManager fileManager;
    private String imagesProcessingPath = "var/images_processing";

    @Before
    public void before() {
        KoalaIdFactory koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        imageManager.setKoalaIdFactory(koalaIdFactory);
        imageManager.setScribe(koalaScribeImpl);

        when(koalaScribeImpl.subscribeToTopic(anyString(), any(ScribeMultiClient.class))).thenReturn(topic);

        imageManager.setActivationCheckPeriodSecs(123);
        imageManager.setStartTimeout(456);

        when(koalaIdUtils.isIdClosestToMe(eq(nodeIdFull), eq(leafNodeHandles), isA(Id.class), eq(NodeScope.AVAILABILITY_ZONE))).thenReturn(false).thenReturn(true);
    }

    @Test
    public void startShouldAlwaysReturnTrue() {
        assertTrue("start returned false", imageManager.becomeActive());
    }

    @Test
    public void startShouldAlsoStartWatcherManager() {
        // act
        imageManager.onApplicationStarting();

        // assert
        verify(this.imageManagerApplicationWatcherManager).createTaskProcessingQueueWatcher(nodeIdFull);
    }

    @Test
    public void startShouldSubscribeToRegisterImageTopic() {
        KoalaIdFactory koalaIdFactory = imageManager.getKoalaIdFactory();

        // act
        imageManager.onApplicationStarting();

        // assert
        assertEquals("Did not register for Register Image topic", 1, imageManager.getSubscribedTopics().size());
        assertTrue(imageManager.getSubscribedTopics().contains(new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.DECRYPT_IMAGE.getPiLocation()).forLocalScope(PiTopics.DECRYPT_IMAGE.getNodeScope())))));
    }

    @Test
    public void anycastShouldNotPropogateMessage() {
        assertTrue(imageManager.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, image));
    }

    @Test
    public void anycastShouldCallUponTheDecryptionHandlerToRegisterTheImage() {
        imageManager.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, image);
        verify(decryptionHandler).decrypt(same(image), same(imageManager));
    }

    @Test
    public void anycastShouldNotAllowEntitiesThatArentImages() {
        PiEntity piEntity = mock(PiEntity.class);
        assertFalse(imageManager.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, piEntity));
    }

    @Test
    public void shouldGetActivationTimeFromConfigIfDefined() {
        assertEquals(123, imageManager.getActivationCheckPeriodSecs());
    }

    @Test
    public void shouldGetStartTimeFromConfigIfDefined() {
        assertEquals(456, imageManager.getStartTimeout());
    }

    @Test
    public void shouldGetStartTimeUnitAsMillis() {
        assertEquals(TimeUnit.MILLISECONDS, imageManager.getStartTimeoutUnit());
    }

    @Test
    public void getApplicationActivatorShouldReturnSameInstanceAsSet() {
        assertTrue("applicationActivator was not same instance", imageManager.getApplicationActivator() == applicationActivator);
    }

    @Test
    public void getMutuallyExclusiveApplicationsShouldReturnEmptyList() {
        assertEquals(Collections.emptyList(), imageManager.getPreferablyExcludedApplications());
    }

    @Test
    public void getApplicationNameShouldReturnCorrectName() {
        assertEquals("pi-image-manager", imageManager.getApplicationName());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleNodeDeparture() throws Exception {
        // setup
        final TaskProcessingQueue taskProcessingQueue = mock(TaskProcessingQueue.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                QueueOwnerRemovalContinuation continuation = (QueueOwnerRemovalContinuation) invocation.getArguments()[1];
                continuation.handleResult(taskProcessingQueue);
                return null;
            }
        }).when(this.queueOwnerRemovalHelper).removeNodeIdFromAllQueues(argThat(new ArgumentMatcher<List>() {
            @Override
            public boolean matches(Object argument) {
                List<PiLocation> list = (List<PiLocation>) argument;
                if (list.size() != 1)
                    return false;
                return (list.contains(PiQueue.DECRYPT_IMAGE.getPiLocation()));
            }
        }), isA(QueueOwnerRemovalContinuation.class));

        // act
        imageManager.handleNodeDeparture(nodeIdFull);

        // assert
        verify(this.imageManagerApplicationWatcherManager).createTaskProcessingQueueWatcher(nodeIdFull);
    }

    @Test
    public void handleNodeArrivalShouldManageQueueWatchers() {

        // act
        imageManager.handleNodeArrival(nodeIdFull);

        // assert
        verify(this.imageManagerApplicationWatcherManager).createTaskProcessingQueueWatcher(nodeIdFull);
    }

    @Test
    public void shouldDeleteAllFilesInImageProcessingDirectory() throws GeneralSecurityException, IOException {
        // setup

        imageManager.setImageProcessingPath(imagesProcessingPath);

        // act
        imageManager.onApplicationStarting();

        // assert
        verify(fileManager).deleteAllFilesInDirectory(imagesProcessingPath);
    }
}
