package com.bt.pi.app.imagemanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.id.PId;

@Component
public class DecryptImageTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(DecryptImageTaskProcessingQueueContinuation.class);
    @Resource
    private DecryptionHandler decryptionHandler;
    @Resource
    private ImageManagerApplication imageManagerApplication;
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private PiIdBuilder piIdBuilder;

    public DecryptImageTaskProcessingQueueContinuation() {
        this.decryptionHandler = null;
        this.imageManagerApplication = null;
        this.piIdBuilder = null;
        this.dhtClientFactory = null;
    }

    @Override
    public void receiveResult(final String url, final String nodeId) {
        LOG.debug(String.format("receiveResult(%s, %s)", url, nodeId));
        PId id = piIdBuilder.getPId(url);
        DhtReader reader = dhtClientFactory.createReader();
        reader.getAsync(id, new PiContinuation<Image>() {
            @Override
            public void handleResult(Image result) {
                decryptionHandler.decrypt(result, imageManagerApplication);
            }
        });
    }
}
