package com.bt.pi.demo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.demo.entity.Bunny;
import com.bt.pi.demo.entity.Hutch;

/**
 * This is a simple demo accessing the DHT. Each DHTDemoApplication creates a bunny adds the bunny to the DHT and then
 * adds the bunny to the Hutch (an index of bunnies). The application then goes on to feed its bunny. If you kill the
 * node/application it will abandon its bunny. This bunny is then adopted by another running DHTDemoApplication in the
 * ring and then fed by that node.
 * 
 */
@Component
public class DHTDemoApplication extends KoalaPastryApplicationBase {
    private static String APPLICATION_NAME = DHTDemoApplication.class.getSimpleName();
    private ApplicationActivator applicationActivator;
    private DhtClientFactory dhtClientFactory;
    private ArrayList<Bunny> myBunnies;

    public DHTDemoApplication() {
        applicationActivator = null;
        dhtClientFactory = null;
        myBunnies = new ArrayList<Bunny>();
    }

    /*
     * General Application info.
     */

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    @Override
    protected void onApplicationStarting() {
    }

    /*
     * Determines when the application is Active.
     */

    @Resource(type = AlwaysOnApplicationActivator.class)
    public void setApplicationActivator(ApplicationActivator anApplicationActivator) {
        this.applicationActivator = anApplicationActivator;
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
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

    /*
     * Tell the application to be active or passive.
     */

    @Override
    public boolean becomeActive() {
        System.err.println(String.format("Node: %s application %s is active", getNodeIdFull(), APPLICATION_NAME));
        addABunnyToTheHutch();
        return true;
    }

    @Override
    public void becomePassive() {
    }

    /*
     * Notifies the application on this node that someone nearby has joined or left
     */

    @Override
    public void handleNodeDeparture(final String nodeId) {
        System.out.println(nodeId + " has left so lets adopt.");
        dhtClientFactory.createWriter().update(getKoalaIdFactory().buildPId(Hutch.URL), new UpdateResolvingContinuation<Hutch, Exception>() {

            @Override
            public Hutch update(Hutch existingEntity, Hutch requestedEntity) {
                myBunnies = existingEntity.adoptABunny(nodeId, getNodeIdFull());
                return existingEntity;
            }

            @Override
            public void receiveException(Exception exception) {
            }

            @Override
            public void receiveResult(Hutch result) {
            }
        });
    }

    @Override
    public void handleNodeArrival(String nodeId) {
        System.out.println("Node: " + nodeId + " has joined. LeafSet: " + getLeafNodeHandles());
    }

    /*
     * Application to Application Node messaging.
     */

    @Override
    public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
    }

    /*
     * Application code.
     */

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        dhtClientFactory = aDhtClientFactory;
    }

    @Scheduled(fixedRate = 20000)
    public void feedYourBunnies() {
        for (Bunny b : myBunnies) {
            dhtClientFactory.createWriter().update(getKoalaIdFactory().buildPId(b), new UpdateResolvingContinuation<Bunny, Exception>() {
                @Override
                public Bunny update(Bunny existingEntity, Bunny requestedEntity) {
                    existingEntity.setLastFed(new Timestamp(System.currentTimeMillis()).toString());
                    return existingEntity;
                }

                @Override
                public void receiveException(Exception exception) {
                }

                public void receiveResult(Bunny result) {
                    System.out.println(result);
                };
            });
        }
    }

    private void addABunnyToTheHutch() {
        Random rand = new Random();
        final Bunny b = new Bunny(Bunny.BUNNY_NAMES[rand.nextInt(10)], rand.nextInt(7));
        dhtClientFactory.createWriter().put(getKoalaIdFactory().buildPId(b), b, new PiContinuation<Bunny>() {
            @Override
            public void handleResult(Bunny result) {
                System.out.println(b + " has just been purchased.");
                addBunnyToHutch(b);
            }

        });
    }

    private void addBunnyToHutch(final Bunny b) {
        dhtClientFactory.createWriter().update(getKoalaIdFactory().buildPId(Hutch.URL), new UpdateResolvingContinuation<Hutch, Exception>() {
            @Override
            public Hutch update(Hutch existingEntity, Hutch requestedEntity) {
                System.err.println("Adding bunny" + b + " to the hutch.");
                Hutch hutchToReturn = existingEntity;
                if (hutchToReturn == null) {
                    hutchToReturn = new Hutch();
                }
                hutchToReturn.addABunny(getNodeIdFull(), b);
                return hutchToReturn;
            }

            @Override
            public void receiveException(Exception exception) {
                System.out.println(b + " died.");
            }

            @Override
            public void receiveResult(Hutch result) {
                System.out.println(b + " has been added to the hutch.");
                System.out.println("There are now " + result.getNumberOfBunnies() + " bunnies in the hutch.");
                myBunnies.add(b);
            }
        });
    }
}
