package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.AttachVolumeDocument;
import com.amazonaws.ec2.doc.x20081201.AttachVolumeResponseDocument;
import com.amazonaws.ec2.doc.x20081201.AttachVolumeType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;

public class AttachVolumeHandlerTest extends AbstractHandlerTest {

    private AttachVolumeHandler attachVolumeHandler;
    private AttachVolumeDocument requestDocument;
    private AttachVolumeType addNewAttachVolume;
    private String volume;
    private String device;
    private ElasticBlockStorageService elasticBlockStorageService;
    private long now = System.currentTimeMillis();

    @Before
    public void setUp() throws Exception {
        super.before();
        this.attachVolumeHandler = new AttachVolumeHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        requestDocument = AttachVolumeDocument.Factory.newInstance();
        addNewAttachVolume = requestDocument.addNewAttachVolume();
        addNewAttachVolume.setInstanceId("i-123");
        device = "device";
        addNewAttachVolume.setDevice(device);
        volume = "volume";
        addNewAttachVolume.setVolumeId(volume);
        elasticBlockStorageService = mock(ElasticBlockStorageService.class);
        Volume value = new Volume("userid", "volume", "i-123", "device", VolumeState.CREATING, now);
        when(elasticBlockStorageService.attachVolume("userid", "volume", "i-123", "device")).thenReturn(value);
        attachVolumeHandler.setElasticBlockStorageService(elasticBlockStorageService);
    }

    @Test
    public void testAttachVolumeGood() {
        // setup

        // act
        AttachVolumeResponseDocument result = this.attachVolumeHandler.attachVolume(requestDocument);

        // assert
        assertEquals(VolumeState.CREATING.toString().toLowerCase(), result.getAttachVolumeResponse().getStatus());
        assertEquals(device, result.getAttachVolumeResponse().getDevice());
        assertEquals(volume, result.getAttachVolumeResponse().getVolumeId());
        assertEquals(now, result.getAttachVolumeResponse().getAttachTime().getTimeInMillis());
    }

    @Test
    public void testAttachVolumeBad() {
        // setup
        addNewAttachVolume.setInstanceId("999");
        Volume value = new Volume("userid", "volume", "999", "device", VolumeState.DELETED, 0l);
        when(elasticBlockStorageService.attachVolume("userid", "volume", "999", "device")).thenReturn(value);
        attachVolumeHandler.setElasticBlockStorageService(elasticBlockStorageService);
        // act
        AttachVolumeResponseDocument result = this.attachVolumeHandler.attachVolume(requestDocument);

        // assert
        assertEquals(VolumeState.DELETED.toString().toLowerCase(), result.getAttachVolumeResponse().getStatus());
        assertEquals(device, result.getAttachVolumeResponse().getDevice());
        assertEquals(volume, result.getAttachVolumeResponse().getVolumeId());
    }
}
