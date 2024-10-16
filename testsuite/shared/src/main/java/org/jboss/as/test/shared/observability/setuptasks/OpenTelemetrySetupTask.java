/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.setuptasks;

import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.arquillian.testcontainers.api.Testcontainer;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.dmr.ModelNode;
import org.junit.AssumptionViolatedException;

@DockerRequired(AssumptionViolatedException.class)
public class OpenTelemetrySetupTask extends AbstractSetupTask {
    private static final String SUBSYSTEM_NAME = "opentelemetry";
    private static final ModelNode extensionAddress = Operations.createAddress("extension", "org.wildfly.extension.opentelemetry");
    private static final ModelNode subsystemAddress = Operations.createAddress("subsystem", SUBSYSTEM_NAME);

    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollectorContainer;

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, extensionAddress))) {
            executeOp(managementClient, Operations.createAddOperation(extensionAddress));
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, subsystemAddress))) {
            executeOp(managementClient, Operations.createAddOperation(subsystemAddress));
        }

        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "batch-delay", "1"));
        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "sampler-type", "on"));
        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "endpoint", otelCollectorContainer.getOtlpGrpcEndpoint()));

        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        otelCollectorContainer.stop();

        executeOp(managementClient, Operations.createRemoveOperation(subsystemAddress));
        executeOp(managementClient, Operations.createRemoveOperation(extensionAddress));

        ServerReload.reloadIfRequired(managementClient);
    }
}
