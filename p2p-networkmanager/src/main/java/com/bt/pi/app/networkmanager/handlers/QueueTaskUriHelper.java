package com.bt.pi.app.networkmanager.handlers;

import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.app.common.entities.SecurityGroup;

public final class QueueTaskUriHelper {
    private static final String S_S_S_S_S_S = "%s:%s;%s=%s;%s=%s";

    private QueueTaskUriHelper() {
    }

    public static String getUriForAssociateAddress(PublicIpAddress addr) {
        return String.format(S_S_S_S_S_S, ResourceSchemes.ADDRESS.toString(), addr.getIpAddress(), ResourceSchemes.SECURITY_GROUP.toString(), String.format(SecurityGroup.SEC_GROUP_ID_FORMAT_STRING, addr.getOwnerId(), addr
                .getSecurityGroupName()), ResourceSchemes.INSTANCE.toString(), addr.getInstanceId());
    }

    public static String getUriForDisassociateAddress(PublicIpAddress addr) {
        return String.format(S_S_S_S_S_S, ResourceSchemes.ADDRESS.toString(), addr.getIpAddress(), ResourceSchemes.SECURITY_GROUP.toString(), String.format(SecurityGroup.SEC_GROUP_ID_FORMAT_STRING, addr.getOwnerId(), addr
                .getSecurityGroupName()), ResourceSchemes.INSTANCE.toString(), addr.getInstanceId());
    }
}
