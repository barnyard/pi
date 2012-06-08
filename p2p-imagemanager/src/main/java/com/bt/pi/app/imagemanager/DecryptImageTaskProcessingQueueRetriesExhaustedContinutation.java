package com.bt.pi.app.imagemanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueRetriesExhaustedContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;

@Component
public class DecryptImageTaskProcessingQueueRetriesExhaustedContinutation implements TaskProcessingQueueRetriesExhaustedContinuation {
    private static final Log LOG = LogFactory.getLog(DecryptImageTaskProcessingQueueRetriesExhaustedContinutation.class);
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private ImageHelper imageHelper;

    public DecryptImageTaskProcessingQueueRetriesExhaustedContinutation() {
        this.piIdBuilder = null;
        this.dhtClientFactory = null;
        this.imageHelper = null;
    }

    @Override
    public void receiveResult(String uri, String nodeId) {
        LOG.debug(String.format("receiveResult(%s, %s)", uri, nodeId));
        DhtReader dhtReader = dhtClientFactory.createReader();
        dhtReader.getAsync(piIdBuilder.getPId(uri), new PiContinuation<Image>() {
            @Override
            public void handleResult(Image result) {
                final String imageId = result.getImageId();
                imageHelper.updateImageState(imageId, ImageState.FAILED);
            }
        });
    }
}
