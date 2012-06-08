package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.images.PlatformBuilder;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

public class BuildInstanceRunnerTest {
    private BuildInstanceRunner buildInstanceRunner;
    private PlatformBuilder platformBuilder;
    private Instance instance;
    private SystemResourceState systemResourceState;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    private String instanceId = "i-01234567";
    private PId queueId;
    private PiIdBuilder piIdBuilder;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        instance = new Instance();
        instance.setInstanceId(instanceId);
        instance.setState(InstanceState.PENDING);

        consumedDhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);
        doAnswer(new UpdateResolvingContinuationAnswer(instance)).when(consumedDhtResourceRegistry).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        KoalaPiEntityFactory koalaPiEntityFactory = new KoalaPiEntityFactory();
        koalaPiEntityFactory.setKoalaJsonParser(new KoalaJsonParser());
        koalaPiEntityFactory.setPiEntityTypes(Arrays.asList(new PiEntity[] { new Instance() }));
        KoalaIdFactory koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);

        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);
        queueId = piIdBuilder.getPiQueuePId(PiQueue.RUN_INSTANCE).forLocalScope(PiQueue.RUN_INSTANCE.getNodeScope());

        platformBuilder = mock(PlatformBuilder.class);
        systemResourceState = mock(SystemResourceState.class);
        taskProcessingQueueHelper = mock(TaskProcessingQueueHelper.class);

        buildInstanceRunner = new BuildInstanceRunner(consumedDhtResourceRegistry, instance, null, piIdBuilder, platformBuilder, systemResourceState, taskProcessingQueueHelper);
    }

    @Test
    public void shouldWriteInstanceAsRunning() {
        // act
        buildInstanceRunner.run();

        // assert
        assertEquals(InstanceState.RUNNING, instance.getState());
        assertTrue(instance.getLaunchTime() > (System.currentTimeMillis() - 500));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateInstancePlatformForLinux() {
        // setup
        instance.setPlatform(ImagePlatform.linux);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation<Instance> u = (UpdateResolvingPiContinuation<Instance>) invocation.getArguments()[1];

                Instance existingEntity = new Instance();
                existingEntity.setPlatform(null);
                Instance updatedInstance = (Instance) u.update(existingEntity, null);
                assertEquals(ImagePlatform.linux, updatedInstance.getPlatform());
                return null;
            }
        }).when(consumedDhtResourceRegistry).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        // act
        buildInstanceRunner.run();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateInstancePlatformForWindows() {
        // setup
        instance.setPlatform(ImagePlatform.windows);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation<Instance> u = (UpdateResolvingPiContinuation<Instance>) invocation.getArguments()[1];

                Instance existingEntity = new Instance();
                existingEntity.setPlatform(null);
                Instance updatedInstance = (Instance) u.update(existingEntity, null);
                assertEquals(ImagePlatform.windows, updatedInstance.getPlatform());
                return null;
            }
        }).when(consumedDhtResourceRegistry).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        // act
        buildInstanceRunner.run();
    }

    @Test
    public void shouldLeaveForTaskProcessingQueueWhenFails() {
        // setup
        doThrow(new RuntimeException("blaaaaaaaaah  <- That is the puking sound. :D")).when(platformBuilder).build(isA(Instance.class), (String) isNull());

        // act
        buildInstanceRunner.run();

        // assert
        assertEquals(InstanceState.PENDING, instance.getState());
        verify(taskProcessingQueueHelper, never()).removeUrlFromQueue(queueId, Instance.getUrl(instanceId));
        verify(systemResourceState, never()).unreserveResources(instanceId);
    }

    @Test
    public void shouldDeregisterFromDhtRegistryWhenFails() {
        // setup
        doThrow(new RuntimeException("blaaaaaaaaah  <- That is the puking sound. :D")).when(platformBuilder).build(isA(Instance.class), (String) isNull());
        PId resourceId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));

        // act
        buildInstanceRunner.run();

        // assert
        verify(consumedDhtResourceRegistry).deregisterConsumer(resourceId, instanceId);
    }

    @Test
    public void shouldReleaseSystemStateReservationAfterInstanceIsUp() {
        // setup

        // act
        buildInstanceRunner.run();

        // assert
        verify(systemResourceState).unreserveResources(instanceId);
    }

    @Test
    public void shouldDeleteInstanceUrlFromTaskProcessingQueueAfterInstanceIsUp() {
        // setup

        // act
        buildInstanceRunner.run();

        // assert
        verify(taskProcessingQueueHelper).removeUrlFromQueue(queueId, Instance.getUrl(instanceId));
    }
}
