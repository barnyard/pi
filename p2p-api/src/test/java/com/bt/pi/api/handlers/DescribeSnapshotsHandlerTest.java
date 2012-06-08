/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.http.HttpExchangeConnection;

import com.amazonaws.ec2.doc.x20081201.DescribeSnapshotsDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeSnapshotsResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeSnapshotsSetResponseType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.api.utils.ConversionUtils;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.sun.net.httpserver.HttpExchange;

public class DescribeSnapshotsHandlerTest extends AbstractHandlerTest {

    private DescribeSnapshotsHandler handler;
    private TransportContext transportContext;
    private HttpExchangeConnection connection;
    private HttpExchange httpExchange;
    private Object userid = "userid";
    private ElasticBlockStorageService elasticBlockStorage;
    private String snapshotId = "snapshotId";
    private String volumeId = "volumeId";
    private SnapshotState status = SnapshotState.PENDING;
    private long startTime = System.currentTimeMillis();
    private double progress = 67.4;
    private String description = "description";
    private String ownerId = "ownerId";

    @Before
    public void before() {
        elasticBlockStorage = mock(ElasticBlockStorageService.class);
        List<Snapshot> listOfSnapshots = new ArrayList<Snapshot>();
        listOfSnapshots.add(new Snapshot(snapshotId, volumeId, status, startTime, progress, description, ownerId));
        when(elasticBlockStorage.describeSnapshots("userid", new ArrayList<String>())).thenReturn(listOfSnapshots);
        this.connection = mock(HttpExchangeConnection.class);
        this.transportContext = mock(TransportContext.class);
        this.httpExchange = mock(HttpExchange.class);
        when(this.transportContext.getConnection()).thenReturn(connection);
        when(this.connection.getHttpExchange()).thenReturn(httpExchange);
        when(this.httpExchange.getAttribute("koala.api.userid")).thenReturn(userid);
        handler = new DescribeSnapshotsHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        handler.setElasticBlockStorage(elasticBlockStorage);
        ReflectionTestUtils.setField(handler, "conversionUtils", new ConversionUtils());
    }

    @Test
    public void testDescribeSnapshotsHandler() {
        // setup
        DescribeSnapshotsDocument requestDocument = DescribeSnapshotsDocument.Factory.newInstance();
        requestDocument.addNewDescribeSnapshots().addNewSnapshotSet();
        // act
        DescribeSnapshotsResponseDocument response = handler.describeSnapshots(requestDocument);
        // assert
        DescribeSnapshotsSetResponseType describeSnapshotsSetResponseType = response.getDescribeSnapshotsResponse().getSnapshotSet();
        assertEquals(1, describeSnapshotsSetResponseType.getItemArray().length);
        assertEquals(String.valueOf(progress), describeSnapshotsSetResponseType.getItemArray(0).getProgress());
        assertEquals(snapshotId, describeSnapshotsSetResponseType.getItemArray(0).getSnapshotId());
        assertEquals(volumeId, describeSnapshotsSetResponseType.getItemArray(0).getVolumeId());
        assertEquals(status.toString(), describeSnapshotsSetResponseType.getItemArray(0).getStatus());
        assertEquals(startTime, describeSnapshotsSetResponseType.getItemArray(0).getStartTime().getTimeInMillis());
    }

}
