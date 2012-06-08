package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.RunInstancesDocument;
import com.amazonaws.ec2.doc.x20081201.RunInstancesResponseDocument;
import com.amazonaws.ec2.doc.x20081201.RunInstancesType;
import com.bt.pi.api.entities.ReservationInstances;
import com.bt.pi.api.service.InstancesService;
import com.bt.pi.api.utils.ConversionUtils;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Reservation;

public class RunInstancesHandlerTest extends AbstractHandlerTest {
    private static final String INSTANCE_TYPE = "m1.large";
    private static final String INSTANCE_NAME = "i-123";
    private static final String IMAGE_NAME = "kmi-123";
    private RunInstancesHandler runInstancesHandler;
    private RunInstancesDocument requestDocument;
    private RunInstancesType addNewRunInstances;
    private InstancesService instancesService;
    private ConversionUtils conversionUtils;
    private ReservationInstances reservationInstances;
    private Reservation createdReservation;

    @Before
    public void setUp() throws Exception {
        super.before();
        this.runInstancesHandler = new RunInstancesHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };

        requestDocument = RunInstancesDocument.Factory.newInstance();
        addNewRunInstances = requestDocument.addNewRunInstances();
        addNewRunInstances.setImageId(IMAGE_NAME);
        instancesService = mock(InstancesService.class);
        Reservation reservation = new Reservation();
        reservation.setImageId(IMAGE_NAME);
        reservation.setUserId("userid");
        Set<Instance> setOfInstances = new HashSet<Instance>();
        Instance instance = new Instance(INSTANCE_NAME, "userid", "default");
        instance.setImageId(IMAGE_NAME);
        instance.setInstanceType(INSTANCE_TYPE);
        instance.setState(InstanceState.RUNNING);
        setOfInstances.add(instance);
        reservationInstances = new ReservationInstances(reservation, setOfInstances);
        doAnswer(new Answer<ReservationInstances>() {
            @Override
            public ReservationInstances answer(InvocationOnMock invocation) throws Throwable {
                createdReservation = (Reservation) invocation.getArguments()[0];
                return reservationInstances;
            }
        }).when(instancesService).runInstances(isA(Reservation.class));
        runInstancesHandler.setInstancesService(instancesService);
        conversionUtils = new ConversionUtils();
        runInstancesHandler.setConversionUtils(conversionUtils);
    }

    @Test
    public void testRunInstancesGood() {
        // setup

        // act
        RunInstancesResponseDocument result = this.runInstancesHandler.runInstances(requestDocument);

        // assert
        assertEquals(IMAGE_NAME, result.getRunInstancesResponse().getInstancesSet().getItemArray(0).getImageId());
        assertEquals(userid, result.getRunInstancesResponse().getRequesterId());
        assertEquals(userid, result.getRunInstancesResponse().getOwnerId());
        assertEquals(userid, createdReservation.getUserId());
        assertEquals(IMAGE_NAME, createdReservation.getImageId());
    }

    @Test
    public void testRunInstancesWithType() {
        // setup
        requestDocument.getRunInstances().setInstanceType(INSTANCE_TYPE);

        // act
        RunInstancesResponseDocument result = this.runInstancesHandler.runInstances(requestDocument);

        // assert
        assertEquals(IMAGE_NAME, result.getRunInstancesResponse().getInstancesSet().getItemArray(0).getImageId());
        assertEquals(INSTANCE_TYPE, result.getRunInstancesResponse().getInstancesSet().getItemArray(0).getInstanceType());
        assertEquals(userid, result.getRunInstancesResponse().getRequesterId());
        assertEquals(userid, result.getRunInstancesResponse().getOwnerId());
        assertEquals(userid, createdReservation.getUserId());
        assertEquals(IMAGE_NAME, createdReservation.getImageId());
    }

    @Test
    public void shouldUseDefaultInstanceType() {
        // act
        this.runInstancesHandler.runInstances(requestDocument);

        // assert
        verify(instancesService).runInstances(argThat(new ArgumentMatcher<Reservation>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof Reservation))
                    return false;
                Reservation reservation = (Reservation) argument;
                return RunInstancesHandler.DEFAULT_INSTANCE_TYPE.equals(reservation.getInstanceType());
            }
        }));
    }

    @Test
    public void shouldUsePropertyInstanceType() {
        // setup
        final String property = "super";
        this.runInstancesHandler.setDefaultInstanceType(property);

        // act
        this.runInstancesHandler.runInstances(requestDocument);

        // assert
        verify(instancesService).runInstances(argThat(new ArgumentMatcher<Reservation>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof Reservation))
                    return false;
                Reservation reservation = (Reservation) argument;
                return property.equals(reservation.getInstanceType());
            }
        }));
    }
}
