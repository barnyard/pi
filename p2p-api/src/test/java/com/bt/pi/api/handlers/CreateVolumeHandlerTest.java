package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.CreateVolumeDocument;
import com.amazonaws.ec2.doc.x20081201.CreateVolumeResponseDocument;
import com.amazonaws.ec2.doc.x20081201.CreateVolumeType;
import com.bt.pi.api.ApiException;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.api.utils.ConversionUtils;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;

public class CreateVolumeHandlerTest extends AbstractHandlerTest {

    private CreateVolumeHandler createVolumeHandler;
    private String snapshotId = "snap-123";
    private String zone = "zone";
    private String size = "24";
    private CreateVolumeDocument requestDocument;
    private CreateVolumeType addNewCreateVolume;
    private ElasticBlockStorageService elasticBlockStorageService;

    @Before
    public void setUp() throws Exception {
        super.before();
        this.createVolumeHandler = new CreateVolumeHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        requestDocument = CreateVolumeDocument.Factory.newInstance();
        addNewCreateVolume = requestDocument.addNewCreateVolume();
        addNewCreateVolume.setAvailabilityZone(zone);
        addNewCreateVolume.setSize(size);
        elasticBlockStorageService = mock(ElasticBlockStorageService.class);
        Volume volume = new Volume();
        volume.setAvailabilityZone(zone);
        volume.setVolumeId("v-123");
        volume.setSizeInGigaBytes(Integer.parseInt(size));
        volume.setStatus(VolumeState.CREATING);
        volume.setCreateTime(System.currentTimeMillis());
        Volume volume2 = new Volume();
        volume2.setAvailabilityZone(zone);
        volume2.setVolumeId("v-123");
        volume2.setSizeInGigaBytes(44);
        volume2.setStatus(VolumeState.DELETED);
        volume2.setCreateTime(System.currentTimeMillis());
        when(elasticBlockStorageService.createVolume("userid", Integer.valueOf(size), zone, null)).thenReturn(volume);
        when(elasticBlockStorageService.createVolume("userid", Integer.valueOf(44), zone, null)).thenReturn(volume2);
        when(elasticBlockStorageService.createVolume("userid", 0, zone, snapshotId)).thenReturn(volume);
        createVolumeHandler.setElasticBlockStorageService(elasticBlockStorageService);
        createVolumeHandler.setConversionUtils(new ConversionUtils());
    }

    @Test
    public void testCreateVolume() {
        // setup

        // act
        CreateVolumeResponseDocument result = this.createVolumeHandler.createVolume(requestDocument);

        // assert
        assertEquals("v-123", result.getCreateVolumeResponse().getVolumeId());
        assertEquals(zone, result.getCreateVolumeResponse().getAvailabilityZone());
        assertEquals(size, result.getCreateVolumeResponse().getSize());
        assertEquals(VolumeState.CREATING.toString().toLowerCase(), result.getCreateVolumeResponse().getStatus());
        assertDateEquals(result.getCreateVolumeResponse().getCreateTime());
    }

    @Test
    public void testCreateVolumeFromSnapshot() {
        // setup
        requestDocument.getCreateVolume().setSnapshotId(snapshotId);
        requestDocument.getCreateVolume().setSize("");

        // act
        CreateVolumeResponseDocument result = this.createVolumeHandler.createVolume(requestDocument);

        // assert
        assertEquals("v-123", result.getCreateVolumeResponse().getVolumeId());
        assertEquals(zone, result.getCreateVolumeResponse().getAvailabilityZone());
        // TODO: do we need this if creating from a snapshot?
        // assertEquals(size, result.getCreateVolumeResponse().getSize());
        assertEquals(VolumeState.CREATING.toString().toLowerCase(), result.getCreateVolumeResponse().getStatus());
        assertDateEquals(result.getCreateVolumeResponse().getCreateTime());
    }

    @Test
    public void testCreateVolumeFail() {
        // setup
        addNewCreateVolume.setSize("44");

        // act
        CreateVolumeResponseDocument result = this.createVolumeHandler.createVolume(requestDocument);

        // assert
        assertEquals("v-123", result.getCreateVolumeResponse().getVolumeId());
        assertEquals(zone, result.getCreateVolumeResponse().getAvailabilityZone());
        assertEquals("44", result.getCreateVolumeResponse().getSize());
        assertEquals(VolumeState.DELETED.toString().toLowerCase(), result.getCreateVolumeResponse().getStatus());
        assertDateEquals(result.getCreateVolumeResponse().getCreateTime());
    }

    @Test(expected = ApiException.class)
    public void testCreateVolumeZeroSize() {
        // setup
        addNewCreateVolume.setSize("0");

        // act
        try {
            this.createVolumeHandler.createVolume(requestDocument);
        } catch (ApiException e) {
            assertEquals("size must be greater than zero", e.getMessage());
            throw e;
        }
    }

    @Test(expected = ApiException.class)
    public void testCreateVolumeNotANumber() {
        // setup
        addNewCreateVolume.setSize("abc");

        // act
        try {
            this.createVolumeHandler.createVolume(requestDocument);
        } catch (ApiException e) {
            assertEquals("size not a number", e.getMessage());
            throw e;
        }
    }

    @Test(expected = ApiException.class)
    public void testCreateVolumeGreaterThanDefaultMax() {
        // setup
        addNewCreateVolume.setSize("101");

        // act
        try {
            this.createVolumeHandler.createVolume(requestDocument);
        } catch (ApiException e) {
            assertEquals("size must be less than " + CreateVolumeHandler.DEFAULT_MAX_VOLUME_SIZE_IN_GIGABYTES, e.getMessage());
            throw e;
        }
    }

    @Test(expected = ApiException.class)
    public void testCreateVolumeGreaterThanOverriddenMax() {
        // setup
        int newMax = 50;
        this.createVolumeHandler.setMaxVolumeSizeInGigaBytes(newMax);
        addNewCreateVolume.setSize("51");

        // act
        try {
            this.createVolumeHandler.createVolume(requestDocument);
        } catch (ApiException e) {
            assertEquals("size must be less than " + newMax, e.getMessage());
            throw e;
        }
    }
}
