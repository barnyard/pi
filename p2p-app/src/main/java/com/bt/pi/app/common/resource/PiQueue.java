/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.resource;

import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.scope.NodeScope;

public enum PiQueue {
    CREATE_VOLUME("CREATE_VOLUME", NodeScope.AVAILABILITY_ZONE),
    DELETE_VOLUME("DELETE_VOLUME", NodeScope.AVAILABILITY_ZONE),
    ATTACH_VOLUME("ATTACH_VOLUME", NodeScope.AVAILABILITY_ZONE),
    DETACH_VOLUME("DETACH_VOLUME", NodeScope.AVAILABILITY_ZONE),
    CREATE_SNAPSHOT("CREATE_SNAPSHOT", NodeScope.AVAILABILITY_ZONE),
    DELETE_SNAPSHOT("DELETE_SNAPSHOT", NodeScope.AVAILABILITY_ZONE),
    REMOVE_SNAPSHOT_FROM_USER("REMOVE_SNAPSHOT_FROM_USER", NodeScope.AVAILABILITY_ZONE),
    REMOVE_VOLUME_FROM_USER("REMOVE_VOLUME_FROM_USER", NodeScope.AVAILABILITY_ZONE),
    DECRYPT_IMAGE("DECRYPT_IMAGE", NodeScope.AVAILABILITY_ZONE),
    RUN_INSTANCE("RUN_INSTANCE", NodeScope.AVAILABILITY_ZONE),
    PAUSE_INSTANCE("PAUSE_INSTANCE", NodeScope.AVAILABILITY_ZONE),
    TERMINATE_INSTANCE("TERMINATE_INSTANCE", NodeScope.AVAILABILITY_ZONE),
    REMOVE_INSTANCE_FROM_USER("REMOVE_INSTANCE_FROM_USER", NodeScope.AVAILABILITY_ZONE),
    UPDATE_SECURITY_GROUP("UPDATE_SECURITY_GROUP", NodeScope.REGION),
    REMOVE_SECURITY_GROUP("REMOVE_SECURITY_GROUP", NodeScope.REGION),
    ASSOCIATE_ADDRESS("ASSOCIATE_ADDRESS", NodeScope.REGION),
    DISASSOCIATE_ADDRESS("DISASSOCIATE_ADDRESS", NodeScope.REGION),
    INSTANCE_NETWORK_MANAGER_TEARDOWN("INSTANCE_NETWORK_MANAGER_TEARDOWN", NodeScope.AVAILABILITY_ZONE);

    private final String queueName;
    private final NodeScope nodeScope;

    private PiQueue(String aQueueName, NodeScope aNodeScope) {
        this.queueName = aQueueName;
        this.nodeScope = aNodeScope;
    }

    public String getUrl() {
        return String.format("%s:%s", ResourceSchemes.QUEUE, this.queueName);
    }

    public NodeScope getNodeScope() {
        return nodeScope;
    }

    public PiLocation getPiLocation() {
        final String url = getUrl();
        return new PiLocation(url, nodeScope);
    }
}
