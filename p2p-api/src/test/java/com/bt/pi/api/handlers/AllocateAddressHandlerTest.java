package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.AllocateAddressDocument;
import com.amazonaws.ec2.doc.x20081201.AllocateAddressResponseDocument;
import com.bt.pi.api.ApiException;
import com.bt.pi.api.ApiServerException;
import com.bt.pi.api.service.ElasticIpAddressesService;
import com.bt.pi.core.util.MDCHelper;

@RunWith(MockitoJUnitRunner.class)
public class AllocateAddressHandlerTest extends AbstractHandlerTest {
    @InjectMocks
    private AllocateAddressHandler allocateAddressHandler = new AllocateAddressHandler() {
        @Override
        protected TransportContext getTransportContext() {
            return transportContext;
        }
    };
    @Mock
    private ElasticIpAddressesService elasticIpAddressesService;
    private AllocateAddressDocument requestDocument;

    @Before
    public void setUp() throws Exception {
        super.before();
        requestDocument = AllocateAddressDocument.Factory.newInstance();
        requestDocument.addNewAllocateAddress();
    }

    @Test
    public void testAllocateAddress() {
        // setup
        when(elasticIpAddressesService.allocateAddress("userid")).thenReturn("1.2.3.4");

        // act
        AllocateAddressResponseDocument result = this.allocateAddressHandler.allocateAddress(requestDocument);

        // assert
        assertEquals("1.2.3.4", result.getAllocateAddressResponse().getPublicIp());
    }

    @SuppressWarnings("serial")
    @Test(expected = ApiException.class)
    public void testThatApiExceptionIsThrownUnchanged() throws Exception {
        // setup
        String message = "oops";
        ApiException apiException = new ApiException(message) {
        };
        when(elasticIpAddressesService.allocateAddress("userid")).thenThrow(apiException);

        // act
        try {
            this.allocateAddressHandler.allocateAddress(requestDocument);
        } catch (Exception e) {
            assertEquals(message, e.getMessage());
            throw e;
        }
    }

    @Test(expected = ApiServerException.class)
    public void testThatNonApiExceptionIsConvertedToApiServerException() throws Exception {
        // setup
        String message = "oops";
        Exception exception = new RuntimeException(message);
        when(elasticIpAddressesService.allocateAddress("userid")).thenThrow(exception);

        // act
        try {
            this.allocateAddressHandler.allocateAddress(requestDocument);
        } catch (Exception e) {
            assertEquals("Internal Server Error", e.getMessage());
            throw e;
        }
    }

    @Test
    public void shouldReturnTransactionIdInResponse() {
        // setup
        when(elasticIpAddressesService.allocateAddress("userid")).thenReturn("1.2.3.4");
        String transactionId = "fred1234";
        MDCHelper.putTransactionUID(transactionId);

        // act
        AllocateAddressResponseDocument result = this.allocateAddressHandler.allocateAddress(requestDocument);

        // assert
        assertEquals(transactionId, result.getAllocateAddressResponse().getRequestId());
    }
}
