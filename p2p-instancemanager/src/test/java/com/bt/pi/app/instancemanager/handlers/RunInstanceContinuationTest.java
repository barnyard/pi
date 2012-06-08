package com.bt.pi.app.instancemanager.handlers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public class RunInstanceContinuationTest {
    private static final String SOURCE_IMAGE_PATH = "sourceImagePath";
    private static final String SOURCE_KERNEL_PATH = "sourceKernelPath";
    private static final String SOURCE_RAMDISK_PATH = "sourceRamdiskPath";
    private static final String KERNEL_ID = "kernelId";
    private static final String RAMDISK = "ramdisk";
    private static final ImagePlatform PLATFORM = ImagePlatform.windows;
    private RunInstanceContinuation instanceManagerContinuation;
    private RunInstanceHandler runInstancesHandler;
    private PiIdBuilder piIdBuilder;
    private Instance instance;
    private DhtClientFactory dhtClientFactory;
    private DhtWriter dhtWriter;
    private String nodeId;
    private ThreadPoolTaskExecutor executor;

    @Before
    public void setUp() throws Exception {
        runInstancesHandler = mock(RunInstanceHandler.class);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Instance receivedInstance = (Instance) invocation.getArguments()[0];
                receivedInstance.setSourceImagePath(SOURCE_IMAGE_PATH);
                receivedInstance.setSourceKernelPath(SOURCE_KERNEL_PATH);
                receivedInstance.setSourceRamdiskPath(SOURCE_RAMDISK_PATH);
                receivedInstance.setKernelId(KERNEL_ID);
                receivedInstance.setRamdiskId(RAMDISK);
                receivedInstance.setPlatform(PLATFORM);
                return null;
            }
        }).when(runInstancesHandler).startInstance(isA(Instance.class));

        piIdBuilder = mock(PiIdBuilder.class);
        dhtClientFactory = mock(DhtClientFactory.class);
        dhtWriter = mock(DhtWriter.class);
        nodeId = "nodeId";

        executor = mock(ThreadPoolTaskExecutor.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // TODO Auto-generated method stub
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(executor).execute(isA(Runnable.class));

        instance = new Instance();

        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        instanceManagerContinuation = new RunInstanceContinuation();
        instanceManagerContinuation.setRunInstanceHandler(runInstancesHandler);
        instanceManagerContinuation.setDhtClientFactory(dhtClientFactory);
        instanceManagerContinuation.setPiIdBuilder(piIdBuilder);
        instanceManagerContinuation.setExecutor(executor);
        instanceManagerContinuation.setNodeId(nodeId);

    }

    @Test
    public void shouldStartInstanceUsingRunInstancesHandler() {
        // act
        instanceManagerContinuation.handleResult(instance);

        // assert
        verify(runInstancesHandler).startInstance(instance);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateExistingInstanceStateWithNodeIdInDHT() {
        // setup
        final PId instanceDhtId = mock(PId.class);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(instance)).thenReturn(instanceDhtId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver updateResolver = (UpdateResolver) invocation.getArguments()[2];
                return updateResolver.update(instance, null);
            }
        }).when(dhtWriter).update(eq(instanceDhtId), isA(PiEntity.class), isA(UpdateResolvingContinuation.class));

        // act
        instanceManagerContinuation.handleResult(instance);

        // assert
        assertThat(instance.getNodeId(), equalTo(nodeId));
        assertEquals(PLATFORM, instance.getPlatform());
        assertEquals(SOURCE_IMAGE_PATH, instance.getSourceImagePath());
        assertEquals(SOURCE_KERNEL_PATH, instance.getSourceKernelPath());
        assertEquals(SOURCE_RAMDISK_PATH, instance.getSourceRamdiskPath());
        assertEquals(KERNEL_ID, instance.getKernelId());
        assertEquals(RAMDISK, instance.getRamdiskId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateExistingInstanceWithHostnameInDHT() throws Exception {
        // setup
        final PId instanceDhtId = mock(PId.class);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(instance)).thenReturn(instanceDhtId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver updateResolver = (UpdateResolver) invocation.getArguments()[2];
                return updateResolver.update(instance, null);
            }
        }).when(dhtWriter).update(eq(instanceDhtId), isA(PiEntity.class), isA(UpdateResolvingContinuation.class));

        // act
        instanceManagerContinuation.handleResult(instance);

        // assert
        String actualHostname = InetAddress.getLocalHost().getHostName();
        assertEquals(actualHostname, instance.getHostname());
    }
}
