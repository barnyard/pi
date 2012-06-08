/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DetachVolumeDocument;
import com.amazonaws.ec2.doc.x20081201.DetachVolumeResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DetachVolumeType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;

public class DetachVolumeHandlerTest extends AbstractHandlerTest {
    private DetachVolumeHandler detachVolumeHandler;
    private DetachVolumeDocument requestDocument;
    private DetachVolumeType addNewDetachVolume;
    private String volumeId;
    private String device;
    private String instanceId;
    private ElasticBlockStorageService elasticBlockStorageService;
    private Volume volume;

    @Before
    public void setUp() throws Exception {
        super.before();
        this.detachVolumeHandler = new DetachVolumeHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        requestDocument = DetachVolumeDocument.Factory.newInstance();
        addNewDetachVolume = requestDocument.addNewDetachVolume();
        instanceId = "i-123";
        addNewDetachVolume.setInstanceId(instanceId);
        device = "device";
        addNewDetachVolume.setDevice(device);
        volumeId = "volume";
        addNewDetachVolume.setVolumeId(volumeId);
        elasticBlockStorageService = mock(ElasticBlockStorageService.class);
        volume = new Volume();
        volume.setAttachTime(1864445563437L);
        volume.setStatus(VolumeState.DETACHING);
        volume.setInstanceId("i-123");
        volume.setDevice("device");
        volume.setVolumeId("volume");
        when(elasticBlockStorageService.detachVolume("userid", volumeId, instanceId, device, false)).thenReturn(volume);
        detachVolumeHandler.setElasticBlockStorageService(elasticBlockStorageService);
    }

    @Test
    public void testDetachVolume() {
        // setup

        // act
        DetachVolumeResponseDocument result = this.detachVolumeHandler.detachVolume(requestDocument);

        // assert
        assertEquals(VolumeState.DETACHING.toString().toLowerCase(), result.getDetachVolumeResponse().getStatus());
        assertEquals(device, result.getDetachVolumeResponse().getDevice());
        assertEquals(volumeId, result.getDetachVolumeResponse().getVolumeId());
        assertEquals(instanceId, result.getDetachVolumeResponse().getInstanceId());
        assertEquals("2029-01-30T05:32:43.437Z", result.getDetachVolumeResponse().getAttachTime().toString());
    }

    @Test
    public void testDetachVolumeForce() {
        // setup
        addNewDetachVolume.setForce(true);
        when(elasticBlockStorageService.detachVolume("userid", volumeId, instanceId, device, true)).thenReturn(volume);

        // act
        DetachVolumeResponseDocument result = this.detachVolumeHandler.detachVolume(requestDocument);

        // assert
        assertEquals(VolumeState.DETACHING.toString().toLowerCase(), result.getDetachVolumeResponse().getStatus());
        assertEquals(device, result.getDetachVolumeResponse().getDevice());
        assertEquals(volumeId, result.getDetachVolumeResponse().getVolumeId());
        assertEquals(instanceId, result.getDetachVolumeResponse().getInstanceId());
        assertEquals("2029-01-30T05:32:43.437Z", result.getDetachVolumeResponse().getAttachTime().toString());
    }
}
