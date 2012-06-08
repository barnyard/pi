/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.List;

import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.SecurityGroup;

/**
 * Security Group Service serves all security group related requsts from the API - authorize ingress - create group -
 * delete group - describe group - revoke ingress
 * 
 */
public interface SecurityGroupService {

    boolean authoriseIngress(String ownerId, String groupName, List<NetworkRule> networkRules);

    boolean createSecurityGroup(String ownerId, String groupName, String groupDescription);

    List<SecurityGroup> describeSecurityGroups(String ownerId, List<String> securityGroups);

    boolean revokeIngress(String ownerId, String groupName, List<NetworkRule> networkRules);

    boolean deleteSecurityGroup(String userId, String groupName);
}
