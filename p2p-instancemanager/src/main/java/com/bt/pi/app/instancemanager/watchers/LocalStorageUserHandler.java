package com.bt.pi.app.instancemanager.watchers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.p2p.commonapi.Id;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.core.application.storage.LocalStorageScanningHandlerBase;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

@Component
public class LocalStorageUserHandler extends LocalStorageScanningHandlerBase {
    private static final Log LOG = LogFactory.getLog(LocalStorageUserHandler.class);
    private static final String USER_ENTITY_TYPE = new User().getType();
    private Set<PId> userPIds;

    public LocalStorageUserHandler() {
        this.userPIds = Collections.synchronizedSet(new HashSet<PId>());
    }

    public Set<PId> getUserPIds() {
        return userPIds;
    }

    @Override
    protected void doHandle(Id id, KoalaGCPastMetadata metadata) {
        LOG.debug(String.format("doHandle(%s, %s)", id.toStringFull(), metadata));
        final PId userPId = getKoalaIdFactory().convertToPId(id);
        LOG.debug(String.format("pid: %s", userPId));
        // ignore backups TODO: get a method on the PId
        if (userPId.toStringFull().endsWith("1") || userPId.toStringFull().endsWith("3"))
            return;
        this.userPIds.add(userPId);
    }

    @Override
    protected String getEntityType() {
        return USER_ENTITY_TYPE;
    }
}
