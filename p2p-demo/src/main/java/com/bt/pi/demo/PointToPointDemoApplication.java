package com.bt.pi.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import rice.pastry.NodeHandle;

import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.continuation.LoggingContinuation;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.demo.entity.WordEntity;

@Component
public class PointToPointDemoApplication extends KoalaPastryApplicationBase {
    private static final Log LOG = LogFactory.getLog(PointToPointDemoApplication.class);

    private static final String APPLICATION_NAME = "point-to-point-demo-app";

    @Resource
    private KoalaIdFactory koalaIdFactory;

    private Random random;

    @Resource(type = AlwaysOnApplicationActivator.class)
    private ApplicationActivator applicationActivator;

    public PointToPointDemoApplication() {
        random = new Random();
    }

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    @Override
    public int getActivationCheckPeriodSecs() {
        return 10;
    }

    @Override
    public long getStartTimeout() {
        return 5;
    }

    @Override
    public TimeUnit getStartTimeoutUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public boolean becomeActive() {
        System.err.println(String.format("Node: %s application %s is active", getNodeIdFull(), APPLICATION_NAME));
        return true;
    }

    @Scheduled(fixedRate = 5000)
    public void sendWordToFirstNodeInLeafset() {
        try {
            MessageContext messageContext = new MessageContext(this);
            LOG.debug("sendWordToFirstNodeInLeafset()");

            String destinationFull = findMyDestination();
            if (destinationFull != null) {
                final PId destinationPId = koalaIdFactory.buildPIdFromHexString(destinationFull);
                final String wordToSend = getWordToSend();
                System.err.println(String.format("[%s] Sending word %s to destination node %s", getNodeIdFull(), wordToSend, destinationPId.toStringFull()));
                messageContext.routePiMessage(destinationPId, EntityMethod.GET, new WordEntity(wordToSend), new LoggingContinuation<WordEntity>());
            }

        } catch (Throwable e) {
            LOG.error("Exception obtaining word to send", e);
        }
    }

    /*
     * This method is used to pair nodes so that it is easier to see what is send and what is received. 
     * first communicates with second, third with fourth node, and so on.
     */
    private String findMyDestination() {
        TreeSet<String> sortedNodeIds = new TreeSet<String>(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                BigInteger i1 = new BigInteger(o1, 16);
                BigInteger i2 = new BigInteger(o2, 16);
                return i1.compareTo(i2);
            }

        });
        Collection<NodeHandle> leafNodeHandles = getLeafNodeHandles();
        LOG.debug(leafNodeHandles);
        String nodeIdFull = getNodeIdFull();
        if (leafNodeHandles != null && !leafNodeHandles.isEmpty()) {
            if (leafNodeHandles.size() == 1)
                return leafNodeHandles.iterator().next().getNodeId().toStringFull();
            for (NodeHandle nodeHandle : leafNodeHandles) {
                sortedNodeIds.add(nodeHandle.getNodeId().toStringFull());
            }
            sortedNodeIds.add(nodeIdFull);

        }
        int index = 0;
        for (String nodeId : sortedNodeIds.descendingSet()) {
            if (nodeIdFull.equals(nodeId))
                break;
            index++;
        }
        String result = null;
        if (index % 2 == 0) {
            result = sortedNodeIds.lower(nodeIdFull);
        } else {
            result = sortedNodeIds.higher(nodeIdFull);
        }
        return result;

    }

    @Override
    public void becomePassive() {

    }

    @Override
    public void handleNodeDeparture(String nodeId) {

    }

    @Override
    public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
        PiEntity entity = receivedMessageContext.getReceivedEntity();
        if (entity instanceof WordEntity) {
            String wordReceived = ((WordEntity) entity).getWord();
            try {
                String wordToSend = getWordToSend();
                System.err.println(String.format("[%s] Received word %s from node %s and replying word %s", getNodeIdFull(), wordReceived, receivedMessageContext.getApplicationMessage().getSourceId(), wordToSend));

                receivedMessageContext.sendResponse(EntityResponseCode.OK, new WordEntity(wordToSend));
            } catch (Exception e) {
                LOG.error("Exception obtaining word to send", e);
            }
        }

    }

    private String getWordToSend() throws Exception {
        LOG.debug("getWordToSend()");
        int randomLine = random.nextInt(1000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/dictionary.txt")));
        for (int i = 1; i < randomLine; i++) {
            reader.readLine();
        }
        String word = reader.readLine();
        LOG.info("Returning word to send: " + word);
        return word;
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
    }

}
