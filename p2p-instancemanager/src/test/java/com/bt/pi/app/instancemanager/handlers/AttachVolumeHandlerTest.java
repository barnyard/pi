package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManager;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.SerialExecutor;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class AttachVolumeHandlerTest {
    @InjectMocks
    private AttachVolumeHandler attachHandler = new AttachVolumeHandler();
    private String remoteVolDir = "./volumes/remote";
    private String localVolDir = "./volumes/local";
    @Mock
    private LibvirtManager libvirtManager;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    private String device;
    @Mock
    private ReceivedMessageContext receivedMessageContext;
    private Volume volume;
    private String volumeId = "vol123";
    private Instance instance;
    private String instanceId = "i123";
    private CountDownLatch latch;
    @Mock
    private CommandRunner commandRunner;
    @Mock
    private PId volumePastryId;
    @Mock
    private PId instancePastryId;
    @Mock
    private SerialExecutor serialExecutor;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                new Thread(r).start();
                return null;
            }
        }).when(serialExecutor).execute(isA(Runnable.class));

        this.attachHandler.setNfsVolumesDirectory(remoteVolDir);
        this.attachHandler.setLocalVolumesDirectory(localVolDir);

        when(this.piIdBuilder.getPId(Volume.getUrl(volumeId))).thenReturn(volumePastryId);
        when(volumePastryId.forGlobalAvailablityZoneCode(anyInt())).thenReturn(volumePastryId);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId))).thenReturn(instancePastryId);

        when(this.dhtClientFactory.createWriter()).thenReturn(this.dhtWriter);

        this.attachHandler.setAttachVolumeXml("<%s %s>");
        volume = new Volume();
        volume.setVolumeId(volumeId);
        volume.setInstanceId(instanceId);
        volume.setDevice(device);
        volume.setStatus(VolumeState.ATTACHING);
        instance = new Instance();
        instance.setInstanceId(instanceId);

        latch = new CountDownLatch(1);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(instance, instance);
                continuation.handleResult(instance);
                latch.countDown();
                return null;
            }
        }).when(this.dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        when(this.libvirtManager.getDomainXml(instanceId)).thenReturn("<xml></xml");
    }

    @Test
    public void testAttachVolume() throws Exception {
        // setup
        device = "sda";
        volume.setDevice(device);

        // act
        this.attachHandler.attachVolume(volume, receivedMessageContext);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));

        verify(this.commandRunner).runNicely(String.format("cp %s/%s %s/%s", this.remoteVolDir, volumeId, localVolDir, volumeId));
        String expectedPath = new File(this.localVolDir).getAbsolutePath();
        String expectedXml = String.format("<%s %s>", String.format("%s/%s", expectedPath, volumeId), device);
        verify(this.libvirtManager).attachVolume(eq(instanceId), eq(expectedXml));
        verify(receivedMessageContext).sendResponse(EntityResponseCode.OK, volume);
        assertEquals(1, instance.getBlockDeviceMappings().size());
        assertEquals(volumeId, instance.getBlockDeviceMappings().get(0).getVolumeId());
    }

    @Test
    public void testAttachVolumeRemovesDevPrefixIfProvided() throws Exception {
        // setup
        device = "/dev/sda";
        volume.setDevice(device);

        // act
        this.attachHandler.attachVolume(volume, receivedMessageContext);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner).runNicely(String.format("cp %s/%s %s/%s", this.remoteVolDir, volumeId, this.localVolDir, volumeId));
        String expectedPath = new File(this.localVolDir).getAbsolutePath();
        String expectedXml = String.format("<%s %s>", String.format("%s/%s", expectedPath, volumeId), "sda");
        verify(this.libvirtManager).attachVolume(eq(instanceId), eq(expectedXml));
        verify(receivedMessageContext).sendResponse(EntityResponseCode.OK, volume);
        assertEquals(1, instance.getBlockDeviceMappings().size());
        assertEquals(volumeId, instance.getBlockDeviceMappings().get(0).getVolumeId());
    }

    @Test
    public void attachVolumeShouldNotAttachIfVolumeAlreadyInDomainXml() throws Exception {
        // setup
        device = "sda";
        volume.setDevice(device);
        when(this.libvirtManager.getDomainXml(instanceId)).thenReturn("<xml><vol>" + volumeId + "</volume></xml");

        // act
        this.attachHandler.attachVolume(volume, receivedMessageContext);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(this.libvirtManager, never()).attachVolume(eq(instanceId), isA(String.class));
    }
}
