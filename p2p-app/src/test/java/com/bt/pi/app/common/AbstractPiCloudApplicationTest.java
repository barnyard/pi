package com.bt.pi.app.common;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.pastry.NodeHandle;

import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.scope.NodeScope;

@RunWith(MockitoJUnitRunner.class)
public class AbstractPiCloudApplicationTest {
    @Mock
    private KoalaIdFactory koalaIdFactory;

    private AbstractPiCloudApplication application;

    @Before
    public void setup() {
        when(koalaIdFactory.getRegion()).thenReturn(0);
        when(koalaIdFactory.getAvailabilityZoneWithinRegion()).thenReturn(0);

        application = new AbstractPiCloudApplication("test") {
            @Override
            public String getNodeIdFull() {
                return "00003E17BD8C49D745E4297C226F23FF34FB0000";
            }

            @Override
            public Collection<NodeHandle> getLeafNodeHandles() {
                Collection<NodeHandle> leafNodes = new ArrayList<NodeHandle>();
                leafNodes.add(buildNodeHandle("0000ADBF90CD5713F6CFA4BCDFA6219F47140000"));
                leafNodes.add(buildNodeHandle("00004DA4540D63A70741266999E408D1980C0000"));
                leafNodes.add(buildNodeHandle("0000428BAD4B9253817ECCE86211C73F26900000"));
                return leafNodes;
            }

            private NodeHandle buildNodeHandle(String id) {
                NodeHandle nodeHandle = mock(NodeHandle.class);
                when(nodeHandle.getId()).thenReturn(rice.pastry.Id.build(id));
                return nodeHandle;
            }

            @Override
            public void becomePassive() {
            }

            @Override
            public boolean becomeActive() {
                return false;
            }

            @Override
            public void handleNodeDeparture(String nodeId) {
            }

            @Override
            public ApplicationActivator getApplicationActivator() {
                return null;
            }
        };
        application.setKoalaIdFactory(koalaIdFactory);
        application.setKoalaIdUtils(new KoalaIdUtils());
    }

    @Test
    public void testIAmAQueueWatchingApplication() throws Exception {
        boolean result = application.iAmAQueueWatchingApplication(2, 3, NodeScope.AVAILABILITY_ZONE);

        assertTrue(result);
    }
}
