/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

public class OpenTelemetrySetupTask implements ServerSetupTask {

    private final ModelNode subsystemAddress = Operations.createAddress("subsystem", "opentelemetry");

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        execute(managementClient, Operations.createWriteAttributeOperation(subsystemAddress, "batch-delay", "1"));
        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        execute(managementClient, Operations.createWriteAttributeOperation(subsystemAddress, "batch-delay", new ModelNode()));
        ServerReload.reloadIfRequired(managementClient);
    }

    private void execute(final ManagementClient managementClient, final ModelNode op) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        assertEquals(response.toString(), "success", outcome);
    }
}
