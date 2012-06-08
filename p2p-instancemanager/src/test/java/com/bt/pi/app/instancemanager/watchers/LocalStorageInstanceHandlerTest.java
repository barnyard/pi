package com.bt.pi.app.instancemanager.watchers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.instancemanager.reporting.InstanceReportEntity;
import com.bt.pi.app.instancemanager.reporting.ZombieInstanceReportEntityCollection;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

@RunWith(MockitoJUnitRunner.class)
public class LocalStorageInstanceHandlerTest {
    @InjectMocks
    private LocalStorageInstanceHandler localStorageInstanceHandler = new LocalStorageInstanceHandler();
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private Id id;
    @Mock
    private KoalaGCPastMetadata metadata;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private PId pid;
    @Mock
    private Instance instance;
    private String instanceId;
    @Mock
    private InstanceReportEntity instanceReportEntity;
    @Mock
    private ReportingApplication reportingApplication;
    @Mock
    private KoalaIdUtils koalaIdUtils;
    private String nodeId = "7777777777777778888888888888";
    private Collection<NodeHandle> leafNodeHandles = new ArrayList<NodeHandle>();
    private Instance updatedInstance;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void before() {
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        when(koalaIdFactory.convertToPId(id)).thenReturn(pid);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                updatedInstance = (Instance) continuation.update(instance, null);
                continuation.handleResult(updatedInstance);
                return null;
            }
        }).when(dhtWriter).update(eq(pid), isA(UpdateResolvingPiContinuation.class));
        when(reportingApplication.getNodeIdFull()).thenReturn(nodeId);
        when(reportingApplication.getLeafNodeHandles()).thenReturn(leafNodeHandles);
        when(koalaIdUtils.isIdClosestToMe(eq(nodeId), eq(leafNodeHandles), eq(id))).thenReturn(true);
        when(metadata.isDeletedAndDeletable()).thenReturn(false);
        when(metadata.getEntityType()).thenReturn(new Instance().getType());
        when(instance.isBuried()).thenReturn(true);
        when(instance.getState()).thenReturn(InstanceState.RUNNING);
        when(instance.getInstanceId()).thenReturn(instanceId);
    }

    @Test
    public void testHandle() {
        // setup
        when(instance.isDeleted()).thenReturn(false);

        // act
        this.localStorageInstanceHandler.handle(id, metadata);

        // assert
        assertEquals(1, this.localStorageInstanceHandler.getZombieInstanceReportEntities().size());
        assertEquals(instanceId, this.localStorageInstanceHandler.getZombieInstanceReportEntities().get(0).getInstanceId());
        assertNull(updatedInstance);
    }

    @Test
    public void shouldUpdateInstanceIfIsDeleted() throws Exception {
        // setup
        when(instance.isDeleted()).thenReturn(true);

        // act
        this.localStorageInstanceHandler.handle(id, metadata);

        // assert
        assertNotNull(updatedInstance);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldHandleNullInstance() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(null, null);
                continuation.handleResult(null);
                return null;
            }
        }).when(dhtWriter).update(eq(pid), isA(UpdateResolvingPiContinuation.class));

        // act
        this.localStorageInstanceHandler.handle(id, metadata);

        // assert
        assertEquals(0, this.localStorageInstanceHandler.getZombieInstanceReportEntities().size());
    }

    @Test
    public void testHandleNullType() {
        // setup
        when(metadata.getEntityType()).thenReturn(null);

        // act
        this.localStorageInstanceHandler.handle(id, metadata);

        // assert
        assertEquals(0, this.localStorageInstanceHandler.getZombieInstanceReportEntities().size());
        assertNull(updatedInstance);
    }

    @Test
    public void testHandleNotDaddyNodeForId() {
        // setup
        when(koalaIdUtils.isIdClosestToMe(eq(nodeId), eq(leafNodeHandles), eq(id))).thenReturn(false);

        // act
        this.localStorageInstanceHandler.handle(id, metadata);

        // assert
        assertEquals(0, this.localStorageInstanceHandler.getZombieInstanceReportEntities().size());
        assertNull(updatedInstance);
    }

    @Test
    public void testCheckAndDispatchToSupernode() {
        // setup
        this.localStorageInstanceHandler.getZombieInstanceReportEntities().add(instanceReportEntity);

        // act
        this.localStorageInstanceHandler.checkAndDispatchToSupernode();

        // assert
        verify(reportingApplication).sendReportingUpdateToASuperNode(argThat(new ArgumentMatcher<PiEntity>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof ZombieInstanceReportEntityCollection))
                    return false;
                ZombieInstanceReportEntityCollection zombieInstanceReportEntityCollection = (ZombieInstanceReportEntityCollection) argument;
                if (!(zombieInstanceReportEntityCollection.getEntities() instanceof List))
                    return false;
                List<InstanceReportEntity> list = (List<InstanceReportEntity>) zombieInstanceReportEntityCollection.getEntities();
                return list.get(0).equals(instanceReportEntity);
            }
        }));
        assertEquals(0, this.localStorageInstanceHandler.getZombieInstanceReportEntities().size());
    }

    @Test
    public void testCheckAndDispatchToSupernodeThrows() {
        // setup
        this.localStorageInstanceHandler.getZombieInstanceReportEntities().add(instanceReportEntity);
        doThrow(new RuntimeException("shit happens")).when(reportingApplication).sendReportingUpdateToASuperNode(isA(PiEntity.class));

        // act
        this.localStorageInstanceHandler.checkAndDispatchToSupernode();

        // assert
        // no exception
    }
}
