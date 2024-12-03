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

@DockerRequired
public class OpenTelemetrySetupTask extends AbstractSetupTask {
    private static final String SUBSYSTEM_NAME = "opentelemetry";
    private static final ModelNode extensionAddress = Operations.createAddress("extension", "org.wildfly.extension.opentelemetry");
    private static final ModelNode subsystemAddress = Operations.createAddress("subsystem", SUBSYSTEM_NAME);

    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollectorContainer;

    private volatile boolean addedExtension;
    private volatile boolean addedSubsystem;

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, extensionAddress))) {
            executeOp(managementClient, Operations.createAddOperation(extensionAddress));
            addedExtension = true;
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, subsystemAddress))) {
            executeOp(managementClient, Operations.createAddOperation(subsystemAddress));
            addedSubsystem = true;
        }

        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "batch-delay", "1"));
        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "sampler-type", "on"));
        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "endpoint", otelCollectorContainer.getOtlpGrpcEndpoint()));

        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        otelCollectorContainer.stop();

        if (addedSubsystem) {
            executeOp(managementClient, Operations.createRemoveOperation(subsystemAddress));
        }
        if (addedExtension) {
            executeOp(managementClient, Operations.createRemoveOperation(extensionAddress));
        }

        ServerReload.reloadIfRequired(managementClient);
    }
}
