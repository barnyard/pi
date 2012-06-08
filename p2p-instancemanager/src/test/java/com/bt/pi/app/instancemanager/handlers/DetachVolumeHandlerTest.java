package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.concurrent.CountDownLatch;
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

import com.bt.pi.app.common.entities.BlockDeviceMapping;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManager;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManagerException;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.util.SerialExecutor;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class DetachVolumeHandlerTest {
    private boolean fileExists = true;
    @InjectMocks
    private DetachVolumeHandler detachVolHandler = new DetachVolumeHandler() {
        protected boolean fileExists(String path) {
            return fileExists;
        };
    };
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
    @Mock
    private DhtReader dhtReader;
    @Mock
    private ReceivedMessageContext receivedMessageContext;
    private Volume volume;
    private Volume volumeWithTerminatedInstance;
    private String volumeId = "vol123";
    private String volumeWithTerminatedInstanceId = "vol456";
    private String instanceId = "i123";
    private String terminatedInstanceId = "i-780abc";
    private String device = "/dev/sda";
    private Instance instance;
    private CountDownLatch latch;
    @Mock
    private CommandRunner commandRunner;
    @Mock
    private PId volumePastryId;
    @Mock
    private PId volumeWithTerminatedInstancePId;
    @Mock
    private PId instancePastryId;
    @Mock
    private PId terminatedInstancePId;
    private Instance terminatedInstance;
    @Mock
    private SerialExecutor serialExecutor;
    private String expectedPath = new File(this.localVolDir).getAbsolutePath();
    private String expectedXml = String.format("<%s %s>", String.format("%s/%s", expectedPath, volumeId), "sda");

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

        volume = new Volume();
        volume.setVolumeId(volumeId);
        volume.setInstanceId(instanceId);
        volume.setStatus(VolumeState.DETACHING);
        volume.setDevice(device);

        volumeWithTerminatedInstance = new Volume();
        volumeWithTerminatedInstance.setVolumeId(volumeWithTerminatedInstanceId);
        volumeWithTerminatedInstance.setInstanceId(terminatedInstanceId);
        volumeWithTerminatedInstance.setStatus(VolumeState.DETACHING);
        volumeWithTerminatedInstance.setDevice(device);

        this.detachVolHandler.setNfsVolumesDirectory(remoteVolDir);
        this.detachVolHandler.setLocalVolumesDirectory(localVolDir);

        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(volume)).thenReturn(volumePastryId);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(volumeWithTerminatedInstance)).thenReturn(volumeWithTerminatedInstancePId);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId))).thenReturn(instancePastryId);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(terminatedInstanceId))).thenReturn(terminatedInstancePId);

        when(this.dhtClientFactory.createWriter()).thenReturn(this.dhtWriter);
        when(this.dhtClientFactory.createReader()).thenReturn(this.dhtReader);

        this.detachVolHandler.setDetachVolumeXml("<%s %s>");

        instance = new Instance();
        instance.setInstanceId(instanceId);
        instance.setState(InstanceState.RUNNING);
        instance.getBlockDeviceMappings().add(new BlockDeviceMapping(volumeId));

        terminatedInstance = new Instance();
        terminatedInstance.setInstanceId(terminatedInstanceId);
        terminatedInstance.setState(InstanceState.TERMINATED);
        terminatedInstance.getBlockDeviceMappings().add(new BlockDeviceMapping(volumeWithTerminatedInstanceId));
        latch = new CountDownLatch(1);

        GenericContinuationAnswer<Volume> gcV = new GenericContinuationAnswer<Volume>(volume);
        GenericContinuationAnswer<Volume> gcVWTI = new GenericContinuationAnswer<Volume>(volumeWithTerminatedInstance);
        doAnswer(gcV).when(this.dhtReader).getAsync(eq(volumePastryId), isA(PiContinuation.class));
        doAnswer(gcVWTI).when(this.dhtReader).getAsync(eq(volumeWithTerminatedInstancePId), isA(PiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(instance, instance);
                continuation.handleResult(instance);
                latch.countDown();
                return null;
            }
        }).when(this.dhtWriter).update(eq(instancePastryId), isA(UpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(terminatedInstance, terminatedInstance);
                continuation.handleResult(terminatedInstance);
                latch.countDown();
                return null;
            }
        }).when(this.dhtWriter).update(eq(terminatedInstancePId), isA(UpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation<Instance> piContinuation = (PiContinuation<Instance>) invocation.getArguments()[1];
                piContinuation.handleResult(terminatedInstance);
                return null;
            }
        }).when(dhtReader).getAsync(eq(terminatedInstancePId), isA(PiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation<Instance> piContinuation = (PiContinuation<Instance>) invocation.getArguments()[1];

                piContinuation.handleResult(instance);
                return null;
            }
        }).when(dhtReader).getAsync(eq(instancePastryId), isA(PiContinuation.class));
    }

    @Test
    public void testDetachVolume() throws Exception {
        // setup
        when(this.libvirtManager.volumeExists(eq(instanceId), anyString())).thenReturn(true).thenReturn(false);

        // act
        this.detachVolHandler.detachVolume(volume, receivedMessageContext);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.commandRunner).run(String.format("rm %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.libvirtManager).detachVolume(eq(instanceId), eq(expectedXml));
        verify(receivedMessageContext).sendResponse(EntityResponseCode.OK, volume);
        assertEquals(0, instance.getBlockDeviceMappings().size());
        assertTrue("Lock should have been released", detachVolHandler.acquireLock(volumeId, 1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDetachVolumeShouldStillWorkIfVolumeFileNotFoundOnLocalDrive() throws Exception {
        // setup
        fileExists = false;
        when(this.libvirtManager.volumeExists(eq(instanceId), anyString())).thenReturn(true).thenReturn(false);
        when(this.commandRunner.run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId))).thenThrow(new CommandExecutionException("file not found"));

        // act
        this.detachVolHandler.detachVolume(volume, receivedMessageContext);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner, never()).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.commandRunner, never()).run(String.format("rm %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.libvirtManager).detachVolume(eq(instanceId), eq(expectedXml));
        verify(receivedMessageContext).sendResponse(EntityResponseCode.OK, volume);
        assertEquals(0, instance.getBlockDeviceMappings().size());
        assertTrue("Lock should exist", detachVolHandler.acquireLock(volumeId, 1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldNotSendResponseIfReceivedMessageContextIsNull() throws InterruptedException {
        // setup
        when(this.libvirtManager.volumeExists(eq(instanceId), anyString())).thenReturn(true).thenReturn(false);

        // act
        this.detachVolHandler.detachVolume(volume, null);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.libvirtManager).detachVolume(eq(instanceId), eq(expectedXml));
        verify(receivedMessageContext, never()).sendResponse(eq(EntityResponseCode.OK), isA(PiEntity.class));
        assertEquals(0, instance.getBlockDeviceMappings().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDetachVolumeGetInstanceAndDeviceFromDHTIfNotSupplied() throws Exception {
        // setup
        when(this.libvirtManager.volumeExists(eq(instanceId), anyString())).thenReturn(true).thenReturn(false);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[1];
                Volume v = new Volume();
                v.setVolumeId(volumeId);
                v.setInstanceId(instanceId);
                v.setDevice(device);
                continuation.handleResult(v);
                return null;
            }
        }).when(this.dhtReader).getAsync(eq(volumePastryId), isA(PiContinuation.class));

        // act
        this.detachVolHandler.detachVolume(volume, receivedMessageContext);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.commandRunner).run(String.format("rm %s/%s", this.localVolDir, volumeId));
        verify(this.libvirtManager).detachVolume(eq(instanceId), eq(expectedXml));
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), isA(Volume.class));
        assertEquals(0, instance.getBlockDeviceMappings().size());
    }

    @Test
    public void shouldNotCopyAndDeleteAndSetVolumeStatusToInUseIfUnableToDetachDevice() throws InterruptedException {
        // setup
        this.detachVolHandler.setDetachVolumeRetries(1);
        when(libvirtManager.volumeExists(anyString(), anyString())).thenReturn(true);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Volume vol = (Volume) invocation.getArguments()[1];
                assertEquals(VolumeState.IN_USE, vol.getStatus());
                return null;
            }
        }).when(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), isA(Volume.class));

        // act
        this.detachVolHandler.detachVolume(volume, receivedMessageContext);

        // assert
        assertFalse(latch.await(3000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner, never()).run(String.format("cp -v %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.commandRunner, never()).run(String.format("rm %s/%s", this.localVolDir, volumeId));
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), isA(Volume.class));
        assertTrue(instance.getBlockDeviceMappings().size() > 0);
    }

    @Test
    public void shouldCopyAndDeleteVolumeAndSetVolumeStatusToAvailableIfDetachIsSuccessful() throws InterruptedException {
        // setup

        // act
        this.detachVolHandler.detachVolume(volume, receivedMessageContext);

        // assert
        latch.await(1000, TimeUnit.MILLISECONDS);
        verify(this.commandRunner).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.commandRunner).run(String.format("rm %s/%s", this.localVolDir, volumeId));
        verify(receivedMessageContext).sendResponse(EntityResponseCode.OK, volume);
        assertEquals(0, instance.getBlockDeviceMappings().size());
    }

    @Test
    public void shouldCopyAndDeleteVolumeIfDetachVolumeWorkedWithinRetryCount() throws InterruptedException {
        // setup
        when(libvirtManager.volumeExists(anyString(), anyString())).thenReturn(true).thenReturn(true).thenReturn(false);

        // act
        this.detachVolHandler.detachVolume(volume, receivedMessageContext);

        // assert
        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.commandRunner).run(String.format("rm %s/%s", this.localVolDir, volumeId));
        verify(receivedMessageContext).sendResponse(EntityResponseCode.OK, volume);
        verify(libvirtManager, atLeast(2)).detachVolume(anyString(), anyString());
        assertEquals(0, instance.getBlockDeviceMappings().size());
    }

    @Test
    public void shouldCopyAndDeleteVolumeIfDetachVolumeThrowsAnExceptionButEventuallyDetaches() throws InterruptedException {
        // setup
        when(libvirtManager.volumeExists(anyString(), anyString())).thenReturn(true).thenReturn(true).thenReturn(false);
        doNothing().doThrow(new LibvirtManagerException("FAIL")).when(libvirtManager).detachVolume(anyString(), anyString());

        // act
        this.detachVolHandler.detachVolume(volume, receivedMessageContext);

        // assert
        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.commandRunner).run(String.format("rm %s/%s", this.localVolDir, volumeId));
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), argThat(new ArgumentMatcher<Volume>() {
            @Override
            public boolean matches(Object argument) {
                return VolumeState.AVAILABLE.equals(volume.getStatus());
            }
        }));
        verify(libvirtManager, atLeast(2)).detachVolume(anyString(), anyString());
        assertEquals(0, instance.getBlockDeviceMappings().size());
    }

    @Test
    public void shouldNotCopyAndDeleteVolumeIfDetachVolumeThrowsAnException() throws InterruptedException {
        // setup
        this.detachVolHandler.setDetachVolumeRetries(3);
        when(libvirtManager.volumeExists(anyString(), anyString())).thenReturn(true);
        doNothing().doThrow(new LibvirtManagerException("FAIL")).when(libvirtManager).detachVolume(anyString(), anyString());

        // act
        this.detachVolHandler.detachVolume(volume, receivedMessageContext);

        // assert
        assertFalse(latch.await(4000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner, never()).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.commandRunner, never()).run(String.format("rm %s/%s", this.localVolDir, volumeId));
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), argThat(new ArgumentMatcher<Volume>() {
            @Override
            public boolean matches(Object argument) {
                return VolumeState.IN_USE.equals(volume.getStatus());
            }
        }));
        verify(libvirtManager, atLeast(2)).detachVolume(anyString(), anyString());
        assertEquals(1, instance.getBlockDeviceMappings().size());
        assertTrue("Lock should have been released", detachVolHandler.acquireLock(volumeId, 1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldCopyAndDeleteVolumeIfDetachVolumeThrowsAnExceptionButForceIsTrue() throws InterruptedException {
        // setup
        this.detachVolHandler.setDetachVolumeRetries(2);
        when(libvirtManager.volumeExists(anyString(), anyString())).thenReturn(true);
        doNothing().doThrow(new LibvirtManagerException("FAIL")).when(libvirtManager).detachVolume(anyString(), anyString());
        volume.setStatus(VolumeState.FORCE_DETACHING);

        // act
        this.detachVolHandler.detachVolume(volume, receivedMessageContext);

        // assert
        assertTrue(latch.await(4000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.commandRunner).run(String.format("rm %s/%s", this.localVolDir, volumeId));
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), argThat(new ArgumentMatcher<Volume>() {
            @Override
            public boolean matches(Object argument) {
                return VolumeState.AVAILABLE.equals(volume.getStatus());
            }
        }));
        verify(libvirtManager, atLeast(2)).detachVolume(anyString(), anyString());
        assertEquals(0, instance.getBlockDeviceMappings().size());
    }

    @Test
    public void shouldCopyAndDeleteVolumeIfVolumeExistsThrowsAnExceptionButForceIsTrue() throws InterruptedException {
        // setup
        this.detachVolHandler.setDetachVolumeRetries(2);
        when(libvirtManager.volumeExists(anyString(), anyString())).thenThrow(new LibvirtManagerException("FAILED"));
        volume.setStatus(VolumeState.FORCE_DETACHING);

        // act
        this.detachVolHandler.detachVolume(volume, receivedMessageContext);

        // assert
        assertTrue(latch.await(4000, TimeUnit.MILLISECONDS));
        verify(this.commandRunner).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeId, this.remoteVolDir, volumeId));
        verify(this.commandRunner).run(String.format("rm %s/%s", this.localVolDir, volumeId));
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), argThat(new ArgumentMatcher<Volume>() {
            @Override
            public boolean matches(Object argument) {
                return VolumeState.AVAILABLE.equals(volume.getStatus());
            }
        }));
        verify(libvirtManager, never()).detachVolume(anyString(), anyString());
        assertEquals(0, instance.getBlockDeviceMappings().size());
    }

    @Test
    public void shouldDetachVolumeIfInstanceIsTerminated() throws Exception {
        // act
        detachVolHandler.detachVolume(volumeWithTerminatedInstance, receivedMessageContext);

        // assert
        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
        verify(libvirtManager, never()).detachVolume(anyString(), anyString());
        verify(this.commandRunner).run(String.format("cp %s/%s %s/%s", this.localVolDir, volumeWithTerminatedInstanceId, this.remoteVolDir, volumeWithTerminatedInstanceId));
        verify(this.commandRunner).run(String.format("rm %s/%s", this.localVolDir, volumeWithTerminatedInstanceId));
        verify(receivedMessageContext).sendResponse(EntityResponseCode.OK, volumeWithTerminatedInstance);
        assertEquals(0, terminatedInstance.getBlockDeviceMappings().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfReleasingUnacquiredLock() throws Exception {
        // act
        detachVolHandler.releaseLock(volumeId);
    }

    @Test
    public void shouldAcquireLockIfFirstTime() throws Exception {
        // act
        boolean result = detachVolHandler.acquireLock(volumeId, 1, TimeUnit.MILLISECONDS);

        // assert
        assertTrue(result);
    }

    @Test
    public void shouldNotAcquireLockIfAlreadyAcquired() throws Exception {
        // setup
        detachVolHandler.acquireLock(volumeId, 1, TimeUnit.MILLISECONDS);

        // act
        boolean result = detachVolHandler.acquireLock(volumeId, 1, TimeUnit.MILLISECONDS);

        // assert
        assertFalse(result);
    }

    @Test
    public void shouldBeAbleToAcquireReleasedLock() throws Exception {
        // setup
        detachVolHandler.acquireLock(volumeId, 1, TimeUnit.MILLISECONDS);
        detachVolHandler.releaseLock(volumeId);

        // act
        boolean result = detachVolHandler.acquireLock(volumeId, 1, TimeUnit.MILLISECONDS);

        // assert
        assertTrue(result);
    }
}
