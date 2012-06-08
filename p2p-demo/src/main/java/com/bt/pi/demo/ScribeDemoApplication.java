package com.bt.pi.demo;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.KoalaPastryScribeApplicationBase;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.demo.entity.FileEntity;

/**
 * The ScribeDemoApplication is a simple example of how to use the pub-sub mechanism built into Pi. If you add a file
 * into the "Outbox" of one node, that file will be sent to all other nodes via Scribe and placed in the "Inbox". The
 * file will also be moved to the "Sent" folder for the sender.
 * 
 */
@Component
public class ScribeDemoApplication extends KoalaPastryScribeApplicationBase {
    private static final String APPLICATION_NAME = "pi-demo-app";
    private static final Log LOG = LogFactory.getLog(ScribeDemoApplication.class);
    private static final String OUTBOX = "/files/outbox";
    private static final String INBOX = "/files/inbox";
    private static final String SENT = "/files/sent";
    private ApplicationActivator applicationActivator;
    private PiLocation topic = new PiLocation("topic:test", NodeScope.GLOBAL);
    private boolean active;

    public ScribeDemoApplication() {
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
        createDir(getOutboxPath());
        createDir(getNodeIdFull() + INBOX);
        createDir(getSendPath());
        subscribe(getKoalaIdFactory().buildPId(topic), this);
        this.active = true;
        System.err.println(String.format("Node: %s application %s is active", getNodeIdFull(), APPLICATION_NAME));
        return true;
    }

    @Override
    public void becomePassive() {
        this.active = false;
        unsubscribe(topic, this);
    }

    /*
     * Notifies the application on this node that someone nearby has joined or left
     */

    @Override
    public void handleNodeDeparture(String nodeId) {
        System.out.println("Node:  " + nodeId + " has left. LeafSet: " + getLeafNodeHandles());
    }

    @Override
    public void handleNodeArrival(String nodeId) {
        System.out.println("Node: " + nodeId + " has joined. LeafSet: " + getLeafNodeHandles());
    }

    /*
     * Scribe methods
     */

    @Override
    public boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity piEntity) {
        LOG.debug(String.format("handleAnycast(%s, %s)", pubSubMessageContext, piEntity));
        return false;
    }

    @Override
    public void deliver(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity data) {
        if (EntityMethod.UPDATE.equals(entityMethod) && data instanceof FileEntity) {
            FileEntity fileEntity = (FileEntity) data;
            System.out.println("processing inbox file " + fileEntity.getFilename());
            File f = new File(getNodeIdFull() + INBOX + "/" + fileEntity.getFilename());
            try {
                FileUtils.writeStringToFile(f, fileEntity.getContents());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Application to Application Node messaging.
     */

    @Override
    public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
    }

    /*
     * The demo methods
     */

    private void createDir(String name) {
        File dir = new File(name);
        if (dir.exists()) {
            if (dir.isDirectory())
                return;
            else
                throw new RuntimeException(String.format("%s is not a directory", name));
        }
        try {
            FileUtils.forceMkdir(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 30000)
    private void startScanningThread() {
        if (active) {
            try {
                scanOutbox();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void scanOutbox() throws IOException {
        Collection<File> files = FileUtils.listFiles(new File(getOutboxPath()), null, false);
        for (File f : files) {
            System.out.println("processing outbox file " + f.getName());
            PubSubMessageContext pubSubMessageContext = newPubSubMessageContext(getKoalaIdFactory().buildPId(topic), UUID.randomUUID().toString());
            pubSubMessageContext.publishContent(EntityMethod.UPDATE, new FileEntity(f.getName(), FileUtils.readFileToString(f)));
            if (new File(getSendPath() + "/" + f.getName()).exists()) {
                System.out.println("overwriting " + getSendPath() + "/" + f.getName());
                FileUtils.copyFileToDirectory(f, new File(getSendPath()));
                FileUtils.deleteQuietly(f);
            } else
                FileUtils.moveFileToDirectory(f, new File(getSendPath()), true);

            f = null;
        }
    }

    private String getOutboxPath() {
        return getNodeIdFull() + OUTBOX;
    }

    private String getSendPath() {
        return getNodeIdFull() + SENT;
    }

}