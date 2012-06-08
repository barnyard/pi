package com.bt.pi.app.networkmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NetworkManagerIntegrationStopNodeTest extends IntegrationTestBase {
    private static final Log LOG = LogFactory.getLog(NetworkManagerIntegrationStopNodeTest.class);

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestBase.beforeClassBase(4);
    }

    @Before
    public void before() throws InterruptedException {
        LOG.debug("#### running " + testName.getMethodName());
        seedPublicIpAddresses();
        seedVlans();
        seedSubnets();
        waitForLiveNetworkManager();
        changeContextToNode(getLiveNetworkAppNodePort());
    }

    @Test
    public void inactiveNodeShouldActivateAndCleanupAfterDepartingNode() throws Exception {
        // setup
        String originalLiveNetworkAppPort = getLiveNetworkAppNodePort();

        // act
        stopNode(originalLiveNetworkAppPort);
        System.err.println("stop called");

        // we wait for the nodes to discover that a node is gone and finish the clean up.
        Thread.sleep(90 * 1000);

        // assert
        waitForLiveNetworkManager();
        String newLiveNetworkAppPort = getLiveNetworkAppNodePort();

        System.err.println("old live: " + originalLiveNetworkAppPort);
        System.err.println("new live: " + newLiveNetworkAppPort);
        assertNotNull(newLiveNetworkAppPort);
        assertFalse(originalLiveNetworkAppPort.equals(newLiveNetworkAppPort));
    }
}
