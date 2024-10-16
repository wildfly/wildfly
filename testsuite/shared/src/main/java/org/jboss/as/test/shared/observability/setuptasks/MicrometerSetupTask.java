/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.setuptasks;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;

import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.arquillian.testcontainers.api.Testcontainer;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.dmr.ModelNode;
import org.junit.AssumptionViolatedException;

/**
 * Sets up a functioning Micrometer subsystem configuration. Requires functioning Docker environment! Tests using this
 * are expected to call AssumeTestGroupUtil.assumeDockerAvailable(); in a @BeforeClass.
 */
@DockerRequired(AssumptionViolatedException.class)
public class MicrometerSetupTask extends AbstractSetupTask {
    private static final ModelNode micrometerExtension = Operations.createAddress("extension", "org.wildfly.extension.micrometer");
    private static final ModelNode micrometerSubsystem = Operations.createAddress("subsystem", "micrometer");

    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;

    @Override
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
        executeOp(managementClient, writeAttribute("undertow", STATISTICS_ENABLED, "true"));

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerExtension))) {
            executeOp(managementClient, Operations.createAddOperation(micrometerExtension));
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerSubsystem))) {
            ModelNode addOp = Operations.createAddOperation(micrometerSubsystem);
            addOp.get("endpoint").set(otelCollector.getOtlpHttpEndpoint() + "/v1/metrics");
            executeOp(managementClient, addOp);
        }

        executeOp(managementClient, writeAttribute("micrometer", "endpoint",
                otelCollector.getOtlpHttpEndpoint() + "/v1/metrics"));
        executeOp(managementClient, writeAttribute("micrometer", "step", "1"));

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
