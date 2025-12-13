/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly31;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.mixed.DomainAdjuster;
import org.jboss.dmr.ModelNode;

/**
 * Does adjustments to the domain model for WF31 legacy secondary hosts.
 */
public class DomainAdjusterWFLY31 extends DomainAdjuster {

    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress, boolean withPrimaryServers) {
        List<ModelNode> operations = new LinkedList<>();

        if (profileAddress.getElement(0).getValue().equals("full-ha")) {
            adjustJGroups(operations, profileAddress.append(SUBSYSTEM, "jgroups"));
        }

        return operations;
    }

    private static void adjustJGroups(List<ModelNode> operations, PathAddress subsystemAddress) {
        // Remove protocols that do not exist in WFLY31, but don't bother replacing
        for (String stack : Arrays.asList("tcp", "udp")) {
            for (String protocol : Arrays.asList("NAKACK4", "UNICAST4")) {
                operations.add(Util.createRemoveOperation(subsystemAddress.append("stack", stack).append("protocol", protocol)));
            }
            // Remote GMS too - this requires reliable unicast/multicast
            operations.add(Util.createRemoveOperation(subsystemAddress.append("stack", stack).append("protocol", "pbcast.GMS")));
        }
    }
}
