/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap800;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
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
 * Does adjustments to the domain model for 8.0.0 legacy secondary hosts.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainAdjuster800 extends DomainAdjuster {

    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress, boolean withPrimaryServers) {
        List<ModelNode> operations = new LinkedList<>();

        if (profileAddress.getElement(0).getValue().equals("full-ha")) {
            adjustJGroups(operations, profileAddress.append(SUBSYSTEM, "jgroups"));
        }

        return operations;
    }

    @Override
    protected void adjustExpansionExtensions(DomainClient client, PathAddress profileAddress) throws Exception {
        removeSubsystemExtensionIfExist(client, profileAddress.append(SUBSYSTEM, "microprofile-jwt-smallrye"), PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.microprofile.jwt-smallrye"));
        removeSubsystemExtensionIfExist(client, profileAddress.append(SUBSYSTEM, "microprofile-config-smallrye"), PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.microprofile.config-smallrye"));
    }

    private static void adjustJGroups(List<ModelNode> operations, PathAddress subsystemAddress) {
        // Remove protocols that do not exist in EAP 8.0, but don't bother replacing
        for (String stack : Arrays.asList("tcp", "udp")) {
            for (String protocol : Arrays.asList("NAKACK4", "UNICAST4")) {
                operations.add(Util.createRemoveOperation(subsystemAddress.append("stack", stack).append("protocol", protocol)));
            }
            // Remote GMS too - this requires reliable unicast/multicast
            operations.add(Util.createRemoveOperation(subsystemAddress.append("stack", stack).append("protocol", "pbcast.GMS")));
        }
    }
}
