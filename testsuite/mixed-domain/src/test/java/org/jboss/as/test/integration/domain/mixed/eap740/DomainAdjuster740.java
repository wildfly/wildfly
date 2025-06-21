/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.mixed.DomainAdjuster;
import org.jboss.dmr.ModelNode;

/**
 * Does adjustments to the domain model for 7.4.0 legacy secondary hosts.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainAdjuster740 extends DomainAdjuster {

    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress, boolean withPrimaryServers) {
        final List<ModelNode> ops = new ArrayList<>();

        adjustRemoting(ops, profileAddress.append(SUBSYSTEM, "remoting"));
        adjustEjb3(ops, profileAddress.append(SUBSYSTEM, "ejb3"));
        removeDistributableEjb(ops, profileAddress.append(SUBSYSTEM, "distributable-ejb"));

        if (profileAddress.getElement(0).getValue().equals("full-ha")) {
            adjustJGroups(ops, profileAddress.append(SUBSYSTEM, "jgroups"));
        }

        return ops;
    }

    private static void adjustRemoting(final List<ModelNode> ops, final PathAddress subsystem) {
        // This adjusts the configuration to reflect the configuration that was used in EAP 7.4,
        // this could equally be moved all the way back and only adjusted for EAP 7.0.0 as we remove
        // the Elytron subsystem.
        final PathAddress httpRemotingConnector = subsystem
                .append("http-connector", "http-remoting-connector");
        ops.add(Util.getUndefineAttributeOperation(httpRemotingConnector, "sasl-authentication-factory"));
    }

    private static void adjustEjb3(List<ModelNode> operations, PathAddress subsystemAddress) {
        PathAddress timerServiceAddress = subsystemAddress.append("service", "timer-service");
        operations.add(Util.createCompositeOperation(Arrays.asList(Util.getUndefineAttributeOperation(timerServiceAddress, "default-transient-timer-management"), Util.getWriteAttributeOperation(timerServiceAddress, "thread-pool-name", "default"))));
        operations.add(Util.createCompositeOperation(Arrays.asList(Util.getUndefineAttributeOperation(timerServiceAddress, "default-persistent-timer-management"), Util.getWriteAttributeOperation(timerServiceAddress, "default-data-store", "default-file-store"))));
    }

    private static void adjustJGroups(List<ModelNode> operations, PathAddress subsystemAddress) {
        // Remove protocols that do not exist in EAP 7.4, but don't bother replacing
        for (String protocol : Arrays.asList("RED", "FD_ALL3", "FRAG4", "VERIFY_SUSPECT2")) {
            operations.add(Util.createRemoveOperation(subsystemAddress.append("stack", "tcp").append("protocol", protocol)));
        }
        // Remove protocols that do not exist in EAP 7.4, but don't bother replacing
        for (String protocol : Arrays.asList("RED", "FD_SOCK2", "FD_ALL3", "FRAG4", "VERIFY_SUSPECT2")) {
            operations.add(Util.createRemoveOperation(subsystemAddress.append("stack", "udp").append("protocol", protocol)));
        }
    }

    /**
     * Remove the distributable-ejb subsystem from the domain model for EAP 7.4.0 secondary hosts as they
     * do not support this subsystem or its associated module org.wildfly.clustering.ejb.
     *
     * @param ops   list of operations used to adjust the domain model for EAP 7.4.0
     * @param subsystem the root of the subsystem to be adjusted/removed
     */
    private static void removeDistributableEjb(final List<ModelNode> ops, final PathAddress subsystem) {
        // remove the distributable-ejb subsystem in its entirety from the domain model
        ops.add(Util.createRemoveOperation(subsystem));
        // remove its extension from the list of extensions
        ops.add(Util.createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.clustering.ejb")));
    }

    @Override
    protected void adjustExpansionExtensions(DomainClient client, PathAddress profileAddress) throws Exception {
        removeSubsystemExtensionIfExist(client, profileAddress.append(SUBSYSTEM, "microprofile-jwt-smallrye"), PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.microprofile.jwt-smallrye"));
        removeSubsystemExtensionIfExist(client, profileAddress.append(SUBSYSTEM, "microprofile-config-smallrye"), PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.microprofile.config-smallrye"));
    }
}
