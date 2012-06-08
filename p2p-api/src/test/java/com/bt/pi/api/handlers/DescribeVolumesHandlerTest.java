package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DescribeVolumesDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeVolumesResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeVolumesSetItemType;
import com.amazonaws.ec2.doc.x20081201.DescribeVolumesSetType;
import com.amazonaws.ec2.doc.x20081201.DescribeVolumesType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.api.utils.ConversionUtils;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;

public class DescribeVolumesHandlerTest extends AbstractHandlerTest {

    private DescribeVolumesHandler describeVolumesHandler;
    private DescribeVolumesDocument requestDocument;
    private DescribeVolumesType addNewDescribeVolumes;
    private String volumeId;
    private ElasticBlockStorageService elasticBlockStorageService;
    private List<Volume> volumes;

    @Before
    public void setUp() throws Exception {
        super.before();
        this.describeVolumesHandler = new DescribeVolumesHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        requestDocument = DescribeVolumesDocument.Factory.newInstance();
        addNewDescribeVolumes = requestDocument.addNewDescribeVolumes();

        DescribeVolumesSetType addNewVolumeSet = addNewDescribeVolumes.addNewVolumeSet();
        DescribeVolumesSetItemType addNewItem = addNewVolumeSet.addNewItem();
        volumeId = "v-123";
        addNewItem.setVolumeId(volumeId);
        elasticBlockStorageService = mock(ElasticBlockStorageService.class);
        List<String> volumeIds = new ArrayList<String>();
        volumeIds.add("v-123");
        volumes = new ArrayList<Volume>();
        Volume volume = new Volume();
        volume.setVolumeId("v-123");
        volume.setAvailabilityZone("IceCube");
        volume.setStatus(VolumeState.IN_USE);
        volumes.add(volume);
        when(elasticBlockStorageService.describeVolumes("userid", volumeIds)).thenReturn(volumes);
        describeVolumesHandler.setElasticBlockStorageService(elasticBlockStorageService);
        describeVolumesHandler.setConversionUtils(new ConversionUtils());
    }

    @Test
    public void testDescribeVolumes() {
        // setup

        // act
        DescribeVolumesResponseDocument result = this.describeVolumesHandler.describeVolumes(requestDocument);

        // assert
        assertEquals("v-123", result.getDescribeVolumesResponse().getVolumeSet().getItemArray(0).getVolumeId());
        assertEquals("IceCube", result.getDescribeVolumesResponse().getVolumeSet().getItemArray(0).getAvailabilityZone());
        assertEquals("in-use", result.getDescribeVolumesResponse().getVolumeSet().getItemArray(0).getStatus());
    }

    @Test
    public void testDescribeVolumesForceDetaching() {
        // setup
        Volume volume2 = new Volume();
        volume2.setVolumeId("v-force");
        volume2.setStatus(VolumeState.FORCE_DETACHING);
        volumes.add(volume2);

        // act
        DescribeVolumesResponseDocument result = this.describeVolumesHandler.describeVolumes(requestDocument);

        // assert
        assertEquals(2, result.getDescribeVolumesResponse().getVolumeSet().sizeOfItemArray());
        assertEquals("v-123", result.getDescribeVolumesResponse().getVolumeSet().getItemArray(0).getVolumeId());
        assertEquals("IceCube", result.getDescribeVolumesResponse().getVolumeSet().getItemArray(0).getAvailabilityZone());
        assertEquals("in-use", result.getDescribeVolumesResponse().getVolumeSet().getItemArray(0).getStatus());
        assertEquals("detaching", result.getDescribeVolumesResponse().getVolumeSet().getItemArray(1).getStatus());
    }
}
