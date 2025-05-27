/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME;

import java.io.IOException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

public class OpenTelemetryManagementUtils {
    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, SUBSYSTEM_NAME);

    static void setSignalEnabledStatus(ModelControllerClient client, String signal, boolean enabled)  {
        ModelNode op = Operations.createWriteAttributeOperation(SUBSYSTEM_ADDRESS.toModelNode(), signal + "-enabled",
            Boolean.valueOf(enabled).toString());
        executeOperation(client, op);
    }

    private static void executeOperation(ModelControllerClient client, ModelNode op) {
        try {
            Assert.assertTrue(Operations.isSuccessfulOutcome(client.execute(op)));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    static void setOtlpEndpoint(ModelControllerClient client, String endpoint) {
        ModelNode op = Operations.createWriteAttributeOperation(SUBSYSTEM_ADDRESS.toModelNode(), "endpoint", endpoint);
        executeOperation(client, op);

    }

    static void reload(ModelControllerClient client) {
        ServerReload.executeReloadAndWaitForCompletion(client);
    }
}
