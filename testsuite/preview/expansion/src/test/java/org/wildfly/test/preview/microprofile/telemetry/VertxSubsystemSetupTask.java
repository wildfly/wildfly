/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.microprofile.telemetry;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * A ServerSetupTask to add vertx extension and subsystem, add a Vertx instance with default options.
 * It will clean up the configuration on tearDown.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxSubsystemSetupTask implements ServerSetupTask {
    private static final String VERTX_EXTENSION = "org.wildfly.extension.vertx";
    private static final String VERTX_SUBSYSTEM = "vertx";
    private static final ModelNode extensionAddress = Operations.createAddress("extension", VERTX_EXTENSION);
    private static final ModelNode subsystemAddress = Operations.createAddress("subsystem", VERTX_SUBSYSTEM);
    private static final ModelNode vertxResourceAddress = Operations.createAddress("subsystem", VERTX_SUBSYSTEM, "vertx", "vertx");

    private volatile boolean cleanExtension;

    @Override
    public void setup(ManagementClient managementClient, String s) {
        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, extensionAddress))) {
            ModelNode op = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
            ModelNode steps = op.get(STEPS);
            steps.add(Operations.createAddOperation(extensionAddress));
            steps.add(Operations.createAddOperation(subsystemAddress));
            executeManagementOperation(managementClient, op);
            addVertxInstance(managementClient);
            cleanExtension = true;
        }
    }

    private void addVertxInstance(ManagementClient managementClient) {
        executeManagementOperation(managementClient, Operations.createAddOperation(vertxResourceAddress));
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        if (cleanExtension) {
            ModelNode op = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
            ModelNode steps = op.get(STEPS);
            steps.add(Operations.createRemoveOperation(vertxResourceAddress));
            steps.add(Operations.createRemoveOperation(subsystemAddress));
            steps.add(Operations.createRemoveOperation(extensionAddress));
            executeManagementOperation(managementClient, op);
        }
    }

    private ModelNode executeRead(final ManagementClient managementClient, ModelNode address) {
        try {
            return managementClient.getControllerClient().execute(Operations.createReadResourceOperation(address));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeManagementOperation(ManagementClient managementClient, ModelNode operation) {
        try {
            ModelNode response = managementClient.getControllerClient().execute(operation);
            if (!SUCCESS.equals(response.get(OUTCOME).asString())) {
                throw new RuntimeException(String.format("%s response -- %s", operation, response));
            }
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
