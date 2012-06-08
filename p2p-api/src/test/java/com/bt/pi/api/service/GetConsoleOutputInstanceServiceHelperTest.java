package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.ConsoleOutput;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class GetConsoleOutputInstanceServiceHelperTest {
    private static final String AVAILABILITY_ZONE_1_NAME = "availabilityZone1";
    private static final int AVAILABILITY_ZONE_1_CODE = 10;
    private static final String REGION_ZONE_1_NAME = "Region1";
    private static final int REGION_ZONE_1_CODE = 1;
    @InjectMocks
    private GetConsoleOutputInstanceServiceHelper consoleOutputHelper = new GetConsoleOutputInstanceServiceHelper();
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private PiIdBuilder piIdBuilder;
    private PId instanceId;
    private String instanceIdStr = "i-DEAFDEED";
    @Mock
    private MessageContext messageContext;
    private String ownerId = "ownerId";
    private String nodeIdStr = "nodeIdStr";
    private Instance instance;
    private PId nodeId;
    private ConsoleOutput consoleOutput;
    @Mock
    private BlockingDhtReader dhtReader;
    @Mock
    private ApiApplicationManager apiApplicationManager;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    private boolean triggerException;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        instanceId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceIdStr));
        nodeId = piIdBuilder.getNodeIdFromNodeId(nodeIdStr);

        instance = new Instance();
        instance.setUserId(ownerId);
        instance.setNodeId(nodeIdStr);
        instance.setPlatform(ImagePlatform.windows);

        consoleOutput = new ConsoleOutput("I pity the fool.", 101L, instanceIdStr, ImagePlatform.windows);

        when(dhtReader.get(instanceId)).thenReturn(instance);

        when(dhtClientFactory.createBlockingReader()).thenReturn(dhtReader);

        when(apiApplicationManager.getAvailabilityZoneByName(AVAILABILITY_ZONE_1_NAME)).thenReturn(new AvailabilityZone(AVAILABILITY_ZONE_1_NAME, AVAILABILITY_ZONE_1_CODE, REGION_ZONE_1_CODE, "rockin"));
        when(apiApplicationManager.getRegion(REGION_ZONE_1_NAME)).thenReturn(new Region(REGION_ZONE_1_NAME, REGION_ZONE_1_CODE, "bob", ""));
        AvailabilityZones zones = new AvailabilityZones();
        zones.addAvailabilityZone(new AvailabilityZone(AVAILABILITY_ZONE_1_NAME, AVAILABILITY_ZONE_1_CODE, REGION_ZONE_1_CODE, AVAILABILITY_ZONE_1_NAME));
        when(apiApplicationManager.getAvailabilityZonesRecord()).thenReturn(zones);

        when(koalaIdFactory.getRegion()).thenReturn(REGION_ZONE_1_CODE);

        triggerException = false;
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation<ConsoleOutput> continuation = (PiContinuation<ConsoleOutput>) invocation.getArguments()[4];
                if (triggerException) {
                    continuation.receiveException(new RuntimeException());
                } else
                    continuation.receiveResult(consoleOutput);
                return null;
            }
        }).when(messageContext).routePiMessageToApplication(eq(nodeId), eq(EntityMethod.GET), isA(ConsoleOutput.class), eq(InstanceManagerApplication.APPLICATION_NAME), isA(PiContinuation.class));
    }

    @Test
    public void shouldReturnConsoleOutput() {
        // act
        ConsoleOutput result = consoleOutputHelper.getConsoleOutput(ownerId, instanceIdStr, messageContext);

        // verify
        assertEquals(consoleOutput, result);
    }

    @Test
    public void shouldReturnBackErrorConsoleOutputOnException() {
        // setup
        triggerException = true;

        // act
        ConsoleOutput result = consoleOutputHelper.getConsoleOutput(ownerId, instanceIdStr, messageContext);

        // verify
        assertEquals(instanceIdStr, result.getInstanceId());
        assertTrue(result.getOutput().contains("currently not available"));
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowExceptingIfNoInstanceForID() {
        // setup
        when(dhtReader.get(instanceId)).thenReturn(null);

        // act
        consoleOutputHelper.getConsoleOutput(ownerId, instanceIdStr, messageContext);
    }

    @Test(expected = NotAuthorizedException.class)
    public void shouldThrowExceptionIfInstanceIsNotOwnedByUser() {
        // setup
        instance.setUserId("someOtherDude");

        // act
        consoleOutputHelper.getConsoleOutput(ownerId, instanceIdStr, messageContext);
    }
}
