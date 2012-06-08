package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DescribeAddressesDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeAddressesInfoType;
import com.amazonaws.ec2.doc.x20081201.DescribeAddressesResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeAddressesType;
import com.bt.pi.api.service.ElasticIpAddressesService;
import com.bt.pi.app.common.entities.InstanceRecord;

public class DescribeAddressesHandlerTest extends AbstractHandlerTest {

    private DescribeAddressesHandler describeAddressesHandler;
    private ElasticIpAddressesService elasticIpAddressesService;

    @Before
    public void setUp() throws Exception {
        super.before();
        this.describeAddressesHandler = new DescribeAddressesHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        elasticIpAddressesService = mock(ElasticIpAddressesService.class);
        List<String> addresses = new ArrayList<String>();
        List<String> doubleUpAddresses = new ArrayList<String>();
        doubleUpAddresses.add("1.1.1.1");
        doubleUpAddresses.add("2.2.2.2");
        SortedMap<String, InstanceRecord> addressesRecords = new TreeMap<String, InstanceRecord>();
        addressesRecords.put("1.1.1.1", new InstanceRecord("i-001", "userid"));
        addressesRecords.put("2.2.2.2", new InstanceRecord("i-002", "userid"));
        when(elasticIpAddressesService.describeAddresses("userid", addresses)).thenReturn(addressesRecords);
        when(elasticIpAddressesService.describeAddresses("userid", doubleUpAddresses)).thenReturn(addressesRecords);
        describeAddressesHandler.setElasticIpAddressesService(elasticIpAddressesService);
    }

    @Test
    public void testDescribeAddresses() {
        // setup
        DescribeAddressesDocument requestDocument = DescribeAddressesDocument.Factory.newInstance();
        DescribeAddressesType addNewDescribeAddresses = requestDocument.addNewDescribeAddresses();
        DescribeAddressesInfoType addNewAddressesSet = addNewDescribeAddresses.addNewPublicIpsSet();
        addNewAddressesSet.addNewItem().setPublicIp("1.1.1.1");
        addNewAddressesSet.addNewItem().setPublicIp("2.2.2.2");

        // act
        DescribeAddressesResponseDocument result = this.describeAddressesHandler.describeAddresses(requestDocument);

        // assert
        assertEquals(2, result.getDescribeAddressesResponse().getAddressesSet().getItemArray().length);
        assertEquals("i-001", result.getDescribeAddressesResponse().getAddressesSet().getItemArray(0).getInstanceId());
        assertEquals("1.1.1.1", result.getDescribeAddressesResponse().getAddressesSet().getItemArray(0).getPublicIp());
        assertEquals("i-002", result.getDescribeAddressesResponse().getAddressesSet().getItemArray(1).getInstanceId());
        assertEquals("2.2.2.2", result.getDescribeAddressesResponse().getAddressesSet().getItemArray(1).getPublicIp());
    }

    @Test
    public void testDescribeAddressesSingles() {
        // setup
        DescribeAddressesDocument requestDocument = DescribeAddressesDocument.Factory.newInstance();
        DescribeAddressesType addNewDescribeAddresses = requestDocument.addNewDescribeAddresses();
        addNewDescribeAddresses.addNewPublicIpsSet();

        // act
        DescribeAddressesResponseDocument result = this.describeAddressesHandler.describeAddresses(requestDocument);

        // assert
        assertEquals(2, result.getDescribeAddressesResponse().getAddressesSet().getItemArray().length);
        assertEquals("i-001", result.getDescribeAddressesResponse().getAddressesSet().getItemArray(0).getInstanceId());
        assertEquals("1.1.1.1", result.getDescribeAddressesResponse().getAddressesSet().getItemArray(0).getPublicIp());
    }
}
