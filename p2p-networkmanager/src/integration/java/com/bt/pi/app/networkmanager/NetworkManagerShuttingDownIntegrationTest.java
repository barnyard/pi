package com.bt.pi.app.networkmanager;

import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.networkmanager.addressing.resolution.AddressDeleteQueue;
import com.bt.pi.app.networkmanager.addressing.resolution.AddressInconsistencyDeleteItem;
import com.bt.pi.app.networkmanager.addressing.resolution.VlanDeleteItem;
import com.bt.pi.core.testing.StubCommandExecutor;

public class NetworkManagerShuttingDownIntegrationTest extends IntegrationTestBase {
    private static final Log LOG = LogFactory.getLog(NetworkManagerShuttingDownIntegrationTest.class);

    @BeforeClass
    public static void beforeClass() throws Exception {
        beforeClassBase(1);
    }

    @Before
    public void before() throws InterruptedException {
        LOG.debug("#### running " + testName.getMethodName());
        waitForLiveNetworkManager();
        changeContextToNode(getLiveNetworkAppNodePort());
    }

    @Test
    public void shouldCleanupPublicAddressAndAddressesInAddressDeleteQueueOnApplicationShutdown() throws InterruptedException {
        // setup
        NetworkCommandRunner networkCommandRunner = currentApplicationContext.getBean(NetworkCommandRunner.class);
        StubCommandExecutor stubCommandExecutor = currentApplicationContext.getBean(StubCommandExecutor.class);

        AddressDeleteQueue addressDeleteQueue = currentApplicationContext.getBean(AddressDeleteQueue.class);
        addressDeleteQueue.add(new AddressInconsistencyDeleteItem(addressDeleteQueue.getPriorityThreshold(), "10.0.0.1", "eth1", networkCommandRunner));
        addressDeleteQueue.add(new VlanDeleteItem(100, "eth0", networkCommandRunner));

        // act
        stopNode(Integer.toString(CLUSTER_START_PORT));
        Thread.sleep(5000);

        // assert
        assertTrue(stubCommandExecutor.assertCommand("ip addr del 1/32 dev eth1".split(" ")));
        assertTrue(stubCommandExecutor.assertCommand("ip addr del 10.0.0.1/32 dev eth1".split(" ")));
        assertTrue(stubCommandExecutor.assertCommand("vconfig rem eth0.100".split(" ")));
        assertTrue(stubCommandExecutor.assertCommand("brctl delbr pibr100".split(" ")));
    }
}
