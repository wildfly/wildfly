/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.setuptasks;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.arquillian.testcontainers.api.Testcontainer;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.dmr.ModelNode;

/**
 * Sets up a functioning Micrometer subsystem configuration. Requires functioning Docker environment! Tests using this
 * are expected to call AssumeTestGroupUtil.assumeDockerAvailable(); in a @BeforeClass.
 */
@TestcontainersRequired
public class MicrometerSetupTask extends AbstractSetupTask {
    public static final ModelNode MICROMETER_EXTENSION = Operations.createAddress("extension", "org.wildfly.extension.micrometer");
    public static final ModelNode MICROMETER_SUBSYSTEM = Operations.createAddress("subsystem", "micrometer");
    private static final ModelNode OTLP_REGISTRY = Operations.createAddress(SUBSYSTEM, "micrometer", "registry", "otlp");

    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;

    @Override
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
        executeOp(managementClient, writeAttribute("undertow", STATISTICS_ENABLED, "true"));

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, MICROMETER_EXTENSION))) {
            executeOp(managementClient, Operations.createAddOperation(MICROMETER_EXTENSION));
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, MICROMETER_SUBSYSTEM))) {
            executeOp(managementClient, Operations.createAddOperation(MICROMETER_SUBSYSTEM));
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, OTLP_REGISTRY))) {
            ModelNode addOtlpOp = Operations.createAddOperation(OTLP_REGISTRY);
            addOtlpOp.get("endpoint").set(otelCollector.getOtlpHttpEndpoint() + "/v1/metrics");
            addOtlpOp.get("step").set("1");
            executeOp(managementClient, addOtlpOp);
        } else {
            executeOp(managementClient, writeAttribute(OTLP_REGISTRY, "endpoint",
                otelCollector.getOtlpHttpEndpoint() + "/v1/metrics"));
        }

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
        otelCollector.stop();

        executeOp(managementClient, clearAttribute("undertow", STATISTICS_ENABLED));
        executeOp(managementClient, Operations.createRemoveOperation(MICROMETER_SUBSYSTEM));
        executeOp(managementClient, Operations.createRemoveOperation(MICROMETER_EXTENSION));

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }
}
