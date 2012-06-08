package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DescribeInstancesDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeInstancesInfoType;
import com.amazonaws.ec2.doc.x20081201.DescribeInstancesItemType;
import com.amazonaws.ec2.doc.x20081201.DescribeInstancesResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeInstancesType;
import com.amazonaws.ec2.doc.x20081201.ReservationInfoType;
import com.bt.pi.api.service.InstancesService;
import com.bt.pi.api.utils.ConversionUtils;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Reservation;

public class DescribeInstancesHandlerTest extends AbstractHandlerTest {

    private DescribeInstancesHandler describeInstancesHandler;
    private InstancesService instancesService;
    private Set<Instance> setOfInstances;
    private Instance instance1;

    @Before
    public void setUp() throws Exception {
        super.before();
        this.describeInstancesHandler = new DescribeInstancesHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        instancesService = mock(InstancesService.class);
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add("i-123");
        Reservation reservation = new Reservation();
        reservation.setReservationId("r-123");
        setOfInstances = new HashSet<Instance>();
        Instance instance = new Instance("i-123", "userid", "default");
        instance.setMonitoring(true);
        instance.setState(InstanceState.RUNNING);
        setOfInstances.add(instance);

        instance1 = new Instance("i-456", "userid", "default");
        instance1.setState(InstanceState.CRASHED);
        setOfInstances.add(instance1);

        Map<String, Set<Instance>> setOfReservationInstances = new HashMap<String, Set<Instance>>();
        setOfReservationInstances.put(reservation.getReservationId(), setOfInstances);
        when(instancesService.describeInstances("userid", instanceIds)).thenReturn(setOfReservationInstances);
        describeInstancesHandler.setInstancesService(instancesService);
        describeInstancesHandler.setConversionUtils(new ConversionUtils());
    }

    @Test
    public void testDescribeInstances20081201() {
        // setup
        DescribeInstancesDocument requestDocument = DescribeInstancesDocument.Factory.newInstance();
        DescribeInstancesType addNewDescribeInstances = requestDocument.addNewDescribeInstances();
        DescribeInstancesInfoType addNewInstancesSet = addNewDescribeInstances.addNewInstancesSet();
        DescribeInstancesItemType addNewItem = addNewInstancesSet.addNewItem();
        addNewItem.setInstanceId("i-123");

        // act
        DescribeInstancesResponseDocument result = this.describeInstancesHandler.describeInstances(requestDocument);

        // assert
        assertEquals(2, result.getDescribeInstancesResponse().getReservationSet().sizeOfItemArray());
        int count = 0;
        for (ReservationInfoType itemArray : result.getDescribeInstancesResponse().getReservationSet().getItemArray()) {
            if ("i-123".equals(itemArray.getInstancesSet().getItemArray(0).getInstanceId())) {
                count++;
                assertEquals("running", itemArray.getInstancesSet().getItemArray(0).getInstanceState().getName());
            }
            if ("i-456".equals(itemArray.getInstancesSet().getItemArray(0).getInstanceId())) {
                count++;
                assertEquals("crashed", itemArray.getInstancesSet().getItemArray(0).getInstanceState().getName());
            }
        }
        assertEquals(2, count);
    }

    @Test
    public void testDescribeInstances20081201NoInstancesNoResults() {
        // setup
        DescribeInstancesDocument requestDocument = DescribeInstancesDocument.Factory.newInstance();
        requestDocument.addNewDescribeInstances();
        Map<String, Set<Instance>> emptySetOfReservationInstances = new HashMap<String, Set<Instance>>();
        when(instancesService.describeInstances("userid", new ArrayList<String>())).thenReturn(emptySetOfReservationInstances);

        // act
        DescribeInstancesResponseDocument result = this.describeInstancesHandler.describeInstances(requestDocument);

        // assert
        assertEquals(0, result.getDescribeInstancesResponse().getReservationSet().getItemArray().length);
    }

    @Test
    public void testDescribeInstances20081201EmptyInstancesNoResults() {
        // setup
        DescribeInstancesDocument requestDocument = DescribeInstancesDocument.Factory.newInstance();
        DescribeInstancesType addNewDescribeInstances = requestDocument.addNewDescribeInstances();
        addNewDescribeInstances.addNewInstancesSet();
        Map<String, Set<Instance>> emptySetOfReservationInstances = new HashMap<String, Set<Instance>>();
        when(instancesService.describeInstances("userid", new ArrayList<String>())).thenReturn(emptySetOfReservationInstances);

        // act
        DescribeInstancesResponseDocument result = this.describeInstancesHandler.describeInstances(requestDocument);

        // assert
        assertEquals(0, result.getDescribeInstancesResponse().getReservationSet().getItemArray().length);
    }

    @Test
    public void testDescribeInstances20090404() {
        // setup
        com.amazonaws.ec2.doc.x20090404.DescribeInstancesDocument requestDocument = com.amazonaws.ec2.doc.x20090404.DescribeInstancesDocument.Factory.newInstance();
        com.amazonaws.ec2.doc.x20090404.DescribeInstancesType addNewDescribeInstances = requestDocument.addNewDescribeInstances();
        com.amazonaws.ec2.doc.x20090404.DescribeInstancesInfoType addNewInstancesSet = addNewDescribeInstances.addNewInstancesSet();
        com.amazonaws.ec2.doc.x20090404.DescribeInstancesItemType addNewItem = addNewInstancesSet.addNewItem();
        addNewItem.setInstanceId("i-123");

        setOfInstances.remove(instance1);

        // act
        com.amazonaws.ec2.doc.x20090404.DescribeInstancesResponseDocument result = this.describeInstancesHandler.describeInstances(requestDocument);

        // assert
        assertEquals("i-123", result.getDescribeInstancesResponse().getReservationSet().getItemArray(0).getInstancesSet().getItemArray(0).getInstanceId());
        assertEquals("true", result.getDescribeInstancesResponse().getReservationSet().getItemArray(0).getInstancesSet().getItemArray(0).getMonitoring().getState());
    }
}
