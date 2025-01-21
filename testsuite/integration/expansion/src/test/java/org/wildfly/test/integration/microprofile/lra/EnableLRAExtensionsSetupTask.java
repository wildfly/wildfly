/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.dmr.ModelNode;

public class EnableLRAExtensionsSetupTask extends SnapshotServerSetupTask {
    private static final String MODULE_LRA_PARTICIPANT = "org.wildfly.extension.microprofile.lra-participant";
    private static final String MODULE_LRA_COORDINATOR = "org.wildfly.extension.microprofile.lra-coordinator";
    private static final String SUBSYSTEM_LRA_PARTICIPANT = "microprofile-lra-participant";
    private static final String SUBSYSTEM_LRA_COORDINATOR = "microprofile-lra-coordinator";

    @Override
    protected void doSetup(final ManagementClient managementClient, final String containerId) throws Exception {
        final Set<String> extensions = listExtensions(managementClient);
        final Set<String> subsystems = listSubsystems(managementClient);
        final CompositeOperationBuilder builder = CompositeOperationBuilder.create();
        if (!extensions.contains(MODULE_LRA_COORDINATOR)) {
            builder.addStep(Operations.createAddOperation(Operations.createAddress("extension", MODULE_LRA_COORDINATOR)));
        }
        if (!subsystems.contains(SUBSYSTEM_LRA_COORDINATOR)) {
            builder.addStep(Operations.createAddOperation(Operations.createAddress("subsystem", SUBSYSTEM_LRA_COORDINATOR)));
        }
        if (!extensions.contains(MODULE_LRA_PARTICIPANT)) {
            builder.addStep(Operations.createAddOperation(Operations.createAddress("extension", MODULE_LRA_PARTICIPANT)));
        }
        if (!subsystems.contains(SUBSYSTEM_LRA_PARTICIPANT)) {
            builder.addStep(Operations.createAddOperation(Operations.createAddress("subsystem", SUBSYSTEM_LRA_PARTICIPANT)));
        }
        executeOperation(managementClient, builder.build());
    }

    private Set<String> listExtensions(final ManagementClient client) throws IOException {
        final ModelNode op = Operations.createOperation("read-children-names");
        op.get(ClientConstants.CHILD_TYPE).set("extension");
        return executeOperation(client, op).asList()
                .stream()
                .map(ModelNode::asString)
                .collect(Collectors.toSet());
    }

    private Set<String> listSubsystems(final ManagementClient client) throws IOException {
        final ModelNode op = Operations.createOperation("read-children-names");
        op.get(ClientConstants.CHILD_TYPE).set("subsystem");
        return executeOperation(client, op).asList()
                .stream()
                .map(ModelNode::asString)
                .collect(Collectors.toSet());
    }
}