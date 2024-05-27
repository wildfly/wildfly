/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;

public class MicrometerSetupTask implements ServerSetupTask {
    static OpenTelemetryCollectorContainer otelCollector;

    private final ModelNode micrometerExtension = Operations.createAddress("extension", "org.wildfly.extension.micrometer");
    private final ModelNode micrometerSubsystem = Operations.createAddress("subsystem", "micrometer");
    private boolean extensionAdded = false;
    private boolean subsystemAdded = false;
    private boolean containerStarted = false;

    @Override
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
        try {
            startOpenTelemetryCollector();
        } catch (Exception e) {
            System.err.println("OpenTelemetry Container failed to start.");
        }

        executeOp(managementClient, enableStatistics(true));

        ModelNode outcome = executeRead(managementClient, micrometerExtension);
        if (!Operations.isSuccessfulOutcome(outcome)) {
            executeOp(managementClient, Operations.createAddOperation(micrometerExtension));
            extensionAdded = true;
        }

        outcome = executeRead(managementClient, micrometerSubsystem);
        if (!Operations.isSuccessfulOutcome(outcome)) {
            ModelNode addOp = Operations.createAddOperation(micrometerSubsystem);
            addOp.get("endpoint").set("http://localhost:4318/v1/metrics"); // Default endpoint
            executeOp(managementClient, addOp);
            subsystemAdded = true;
        }

        if (containerStarted) {
            executeOp(managementClient, writeAttribute("micrometer", "endpoint", otelCollector.getOtlpEndpoint()));
        }
        executeOp(managementClient, writeAttribute("micrometer", "step", "1"));

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
        if (containerStarted) {
            otelCollector.stop();
            otelCollector = null;
            containerStarted = false;
        }

        executeOp(managementClient, enableStatistics(false));
        if (subsystemAdded) {
            executeOp(managementClient, Operations.createRemoveOperation(micrometerSubsystem));
        }
        if (extensionAdded) {
            executeOp(managementClient, Operations.createRemoveOperation(micrometerExtension));
        }

        ServerReload.reloadIfRequired(managementClient);
    }

    private void startOpenTelemetryCollector() {
        if ( AssumeTestGroupUtil.isDockerAvailable()) {
            String otelCollectorConfigFile = getClass().getPackage().getName().replaceAll("\\.", "/") +
                    "/otel-collector-config.yaml";
            otelCollector = new OpenTelemetryCollectorContainer()
                    .withCopyFileToContainer(MountableFile.forClasspathResource(otelCollectorConfigFile),
                            OpenTelemetryCollectorContainer.OTEL_COLLECTOR_CONFIG_YAML)
                    .withCommand("--config " + OpenTelemetryCollectorContainer.OTEL_COLLECTOR_CONFIG_YAML)
            ;

            otelCollector.start();
            containerStarted = true;
        }
    }

    private ModelNode enableStatistics(boolean enabled) {
        return writeAttribute("undertow", STATISTICS_ENABLED, String.valueOf(enabled));
    }

    private ModelNode writeAttribute(String subsystem, String name, String value) {
        return Operations.createWriteAttributeOperation(Operations.createAddress(SUBSYSTEM, subsystem), name, value);
    }

    private void executeOp(final ManagementClient client, final ModelNode op) throws IOException {
        executeOp(client.getControllerClient(), Operation.Factory.create(op));
    }

    private void executeOp(final ModelControllerClient client, final Operation op) throws IOException {
        final ModelNode result = client.execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to execute operation: " + Operations.getFailureDescription(result)
                    .asString());
        }
    }

    private ModelNode executeRead(final ManagementClient managementClient, ModelNode address) throws IOException {
        return managementClient.getControllerClient().execute(Operations.createReadResourceOperation(address));
    }
}
