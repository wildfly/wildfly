/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.setuptasks;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.arquillian.testcontainers.api.Testcontainer;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.dmr.ModelNode;

/**
 * Sets up a functioning Micrometer subsystem configuration. Requires functioning Docker environment! Tests using this
 * are expected to call AssumeTestGroupUtil.assumeDockerAvailable(); in a @BeforeClass.
 */
@DockerRequired
public class MicrometerSetupTask extends AbstractSetupTask {
    private static final ModelNode micrometerExtension = Operations.createAddress("extension", "org.wildfly.extension.micrometer");
    private static final ModelNode micrometerSubsystem = Operations.createAddress("subsystem", "micrometer");
    private static final ModelNode otlpRegistry = Operations.createAddress(SUBSYSTEM, "micrometer", "registry", "otlp");

    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;

    @Override
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
        executeOp(managementClient, writeAttribute("undertow", STATISTICS_ENABLED, "true"));

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerExtension))) {
            executeOp(managementClient, Operations.createAddOperation(micrometerExtension));
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerSubsystem))) {
            executeOp(managementClient, Operations.createAddOperation(micrometerSubsystem));
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, otlpRegistry))) {
            ModelNode addOtlpOp = Operations.createAddOperation(otlpRegistry);
            addOtlpOp.get("endpoint").set(otelCollector.getOtlpHttpEndpoint() + "/v1/metrics");
            addOtlpOp.get("step").set("1");
            executeOp(managementClient, addOtlpOp);
        } else {
            executeOp(managementClient, writeAttribute(otlpRegistry, "endpoint",
                otelCollector.getOtlpHttpEndpoint() + "/v1/metrics"));
        }

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
        otelCollector.stop();

        executeOp(managementClient, clearAttribute("undertow", STATISTICS_ENABLED));
        executeOp(managementClient, Operations.createRemoveOperation(micrometerSubsystem));
        executeOp(managementClient, Operations.createRemoveOperation(micrometerExtension));

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }
}
