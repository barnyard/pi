package com.bt.pi.app.networkmanager.handlers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.networkmanager.addressing.PublicIpAddressManager;
import com.bt.pi.app.networkmanager.net.NetworkManager;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@SuppressWarnings("unchecked")
public class InstanceNetworkSetupHandlerTest {
    private static final String PUBLIC_IP_ADDRESS = "1.2.3.4";
    private static final String INSTANCE_ID = "i-12345678";
    private static final String DEFAULT = "default";
    private static final String USER_ID = "userid";
    private InstanceNetworkSetupHandler instanceNetworkSetupHandler;
    private Instance instance;
    private NetworkManager networkManager;
    private PublicIpAddressManager addressManager;
    private DhtClientFactory dhtFactory;
    private GenericContinuationAnswer<Instance> networkManagerAnswer;
    private GenericContinuationAnswer<String> addressManagerAnswer;
    private DhtWriter dhtWriter;
    private UpdateResolvingContinuationAnswer dhtWriterAnswer;
    private PId instanceDhtId;
    private PiIdBuilder piIdBuilder;
    private ReceivedMessageContext messageContext;

    @Before
    public void before() {
        instance = new Instance();
        instance.setUserId(USER_ID);
        instance.setInstanceId(INSTANCE_ID);
        instance.setSecurityGroupName(DEFAULT);
        instance.setPublicIpAddress("109.144.11.1");
        instance.setPrivateIpAddress("10.1.1.1");
        instance.setPrivateMacAddress("00:d0:0d");
        instance.setVlanId(100);

        instanceDhtId = mock(PId.class);
        messageContext = mock(ReceivedMessageContext.class);

        networkManagerAnswer = new GenericContinuationAnswer<Instance>(instance);
        addressManagerAnswer = new GenericContinuationAnswer<String>(PUBLIC_IP_ADDRESS);
        dhtWriterAnswer = new UpdateResolvingContinuationAnswer(instance);

        networkManager = mock(NetworkManager.class);
        addressManager = mock(PublicIpAddressManager.class);

        piIdBuilder = mock(PiIdBuilder.class);
        dhtFactory = mock(DhtClientFactory.class);
        dhtWriter = mock(DhtWriter.class);

        doAnswer(networkManagerAnswer).when(networkManager).setupNetworkForInstance(eq(instance), isA(Continuation.class));
        doAnswer(addressManagerAnswer).when(addressManager).allocatePublicIpAddressForInstance(eq(INSTANCE_ID), isA(Continuation.class));
        doAnswer(dhtWriterAnswer).when(dhtWriter).update(eq(instanceDhtId), isA(UpdateResolvingContinuation.class));
        when(dhtFactory.createWriter()).thenReturn(dhtWriter);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instance.getInstanceId()))).thenReturn(instanceDhtId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(instance)).thenReturn(instanceDhtId);
        instanceNetworkSetupHandler = new InstanceNetworkSetupHandler();
        instanceNetworkSetupHandler.setNetworkManager(networkManager);
        instanceNetworkSetupHandler.setPublicIpAddressManager(addressManager);
        instanceNetworkSetupHandler.setDhtFactory(dhtFactory);
        instanceNetworkSetupHandler.setPiIdBuilder(piIdBuilder);
    }

    @Test
    public void shouldPersistInstanceAndSendResponse() {
        // act
        instanceNetworkSetupHandler.handle(instance, messageContext);

        // assert
        Instance persistedInstance = (Instance) dhtWriterAnswer.getResult();
        assertNotNull(persistedInstance);
        verify(networkManager).setupNetworkForInstance(eq(instance), isA(Continuation.class));
        verify(addressManager).allocatePublicIpAddressForInstance(eq(INSTANCE_ID), isA(Continuation.class));
        verify(dhtWriter).update(eq(instanceDhtId), isA(UpdateResolvingContinuation.class));
        verify(messageContext).sendResponse(EntityResponseCode.OK, instance);
    }

    @Test
    public void throwExceptionWhenNoInstanceReturned() {
        // setup
        networkManagerAnswer = new GenericContinuationAnswer<Instance>(null);
        doAnswer(networkManagerAnswer).when(networkManager).setupNetworkForInstance(eq(instance), isA(Continuation.class));

        // act
        instanceNetworkSetupHandler.handle(instance, messageContext);

        //
        verifyNoMoreInteractions(addressManager);
    }

    @Test
    public void shouldPersistInstanceAndNotSendMessageWhenNotInitialInstanceCreation() {
        // act
        instanceNetworkSetupHandler.handle(instance, false, messageContext);

        // assert
        verify(networkManager).setupNetworkForInstance(eq(instance), isA(Continuation.class));
        verify(addressManager).allocatePublicIpAddressForInstance(eq(INSTANCE_ID), isA(Continuation.class));
        verify(dhtWriter).update(isA(PId.class), isA(UpdateResolvingContinuation.class));
        verify(messageContext, never()).sendResponse(isA(EntityResponseCode.class), isA(Instance.class));
    }

    @Test
    public void shouldNotPersistInstanceButStillSendResponseIfNothingChangedAndInitialInstanceCreation() {
        // setup
        instance.setPublicIpAddress(PUBLIC_IP_ADDRESS);

        // act
        instanceNetworkSetupHandler.handle(instance, true, messageContext);

        // assert
        Instance persistedInstance = (Instance) dhtWriterAnswer.getResult();
        assertNull(persistedInstance);
        verify(messageContext).sendResponse(isA(EntityResponseCode.class), isA(Instance.class));
    }

    @Test
    public void shouldPersistInstanceIfPublicAddressIsNull() {
        // setup
        instance.setPublicIpAddress(null);

        // act
        instanceNetworkSetupHandler.handle(instance, true, messageContext);

        // assert
        Instance persistedInstance = (Instance) dhtWriterAnswer.getResult();
        assertNotNull(persistedInstance);
        verify(messageContext).sendResponse(isA(EntityResponseCode.class), isA(Instance.class));
    }
}
