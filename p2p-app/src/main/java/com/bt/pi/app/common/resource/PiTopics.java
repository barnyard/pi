/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.resource;

import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.scope.NodeScope;

public enum PiTopics {
    RUN_INSTANCE("RUN_INSTANCE", NodeScope.AVAILABILITY_ZONE),
    DECRYPT_IMAGE("DECRYPT_IMAGE", NodeScope.AVAILABILITY_ZONE),
    CREATE_VOLUME("CREATE_VOLUME", NodeScope.AVAILABILITY_ZONE),
    DELETE_VOLUME("DELETE_VOLUME", NodeScope.AVAILABILITY_ZONE),
    ATTACH_VOLUME("ATTACH_VOLUME", NodeScope.AVAILABILITY_ZONE),
    DETACH_VOLUME("DETACH_VOLUME", NodeScope.AVAILABILITY_ZONE),
    CREATE_SNAPSHOT("CREATE_SNAPSHOT", NodeScope.AVAILABILITY_ZONE),
    DELETE_SNAPSHOT("DELETE_SNAPSHOT", NodeScope.AVAILABILITY_ZONE),
    NETWORK_MANAGERS_IN_REGION("NETWORK_MANAGERS_IN_REGION", NodeScope.REGION);

    public static final String TYPE = "piTopic";
    private final String topicName;
    private final NodeScope nodeScope;

    private PiTopics(String aTopicName, NodeScope aScope) {
        this.topicName = aTopicName;
        this.nodeScope = aScope;
    }

    public NodeScope getNodeScope() {
        return nodeScope;
    }

    public String getUrl() {
        return String.format("%s:%s", ResourceSchemes.TOPIC, this.topicName);
    }

    public PiLocation getPiLocation() {
        return new PiLocation(getUrl(), getNodeScope());
    }
}
