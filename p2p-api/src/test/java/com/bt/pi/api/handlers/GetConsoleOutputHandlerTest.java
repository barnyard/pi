package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.GetConsoleOutputDocument;
import com.amazonaws.ec2.doc.x20081201.GetConsoleOutputResponseDocument;
import com.amazonaws.ec2.doc.x20081201.GetConsoleOutputType;
import com.bt.pi.api.service.InstancesService;
import com.bt.pi.app.common.entities.ConsoleOutput;
import com.bt.pi.app.common.images.platform.ImagePlatform;

public class GetConsoleOutputHandlerTest extends AbstractHandlerTest {

    private GetConsoleOutputHandler getConsoleOutputHandler;
    private GetConsoleOutputDocument requestDocument;
    private GetConsoleOutputType addNewGetConsoleOutput;
    private String instanceId;
    private InstancesService instancesService;

    @Before
    public void setUp() throws Exception {
        super.before();
        this.getConsoleOutputHandler = new GetConsoleOutputHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        requestDocument = GetConsoleOutputDocument.Factory.newInstance();
        addNewGetConsoleOutput = requestDocument.addNewGetConsoleOutput();
        instanceId = "i-123";
        addNewGetConsoleOutput.setInstanceId(instanceId);
        instancesService = mock(InstancesService.class);
        ConsoleOutput value = new ConsoleOutput("Did you really mean to do rm -rf?", 1864445563437L, "bob", ImagePlatform.windows);
        when(instancesService.getConsoleOutput("userid", "i-123")).thenReturn(value);
        getConsoleOutputHandler.setInstancesService(instancesService);
    }

    @Test
    public void testGetConsoleOutputGood() {
        // setup

        // act
        GetConsoleOutputResponseDocument result = this.getConsoleOutputHandler.getConsoleOutput(requestDocument);

        // assert
        assertEquals(instanceId, result.getGetConsoleOutputResponse().getInstanceId());
        assertEquals(new String(Base64.encode("Did you really mean to do rm -rf?".getBytes())), result.getGetConsoleOutputResponse().getOutput());
    }
}
