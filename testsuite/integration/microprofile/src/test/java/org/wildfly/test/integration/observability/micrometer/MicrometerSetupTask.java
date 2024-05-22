/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.integration.observability.container.OpenTelemetryCollectorContainer;
import org.wildfly.test.integration.observability.setuptask.AbstractSetupTask;

public class MicrometerSetupTask extends AbstractSetupTask {
    private final ModelNode micrometerExtension = Operations.createAddress("extension", "org.wildfly.extension.micrometer");
    private final ModelNode micrometerSubsystem = Operations.createAddress("subsystem", "micrometer");
    private boolean extensionAdded = false;
    private boolean subsystemAdded = false;
    private OpenTelemetryCollectorContainer otelCollector;

    @Override
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
        AssumeTestGroupUtil.assumeDockerAvailable();

        otelCollector = OpenTelemetryCollectorContainer.getInstance();

        executeOp(managementClient, writeAttribute("undertow", STATISTICS_ENABLED, "true"));

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerExtension))) {
            executeOp(managementClient, Operations.createAddOperation(micrometerExtension));
            extensionAdded = true;
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerSubsystem))) {
            executeOp(managementClient, Operations.createAddOperation(micrometerSubsystem));
            subsystemAdded = true;
        }

        final ModelNode otlpRegistryAddress = Operations.createAddress(SUBSYSTEM, "micrometer", "registry", "otlp");
        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, otlpRegistryAddress))) {
            executeOp(managementClient, Operations.createAddOperation(otlpRegistryAddress));
        }
        executeOp(managementClient, writeAttribute(otlpRegistryAddress,
                "endpoint", otelCollector.getOtlpHttpEndpoint() + "/v1/metrics"));
        executeOp(managementClient, writeAttribute(otlpRegistryAddress, "step", "1"));

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
        otelCollector.stop();

        executeOp(managementClient, clearAttribute("undertow", STATISTICS_ENABLED));
        if (subsystemAdded) {
            executeOp(managementClient, Operations.createRemoveOperation(micrometerSubsystem));
        }
        if (extensionAdded) {
            executeOp(managementClient, Operations.createRemoveOperation(micrometerExtension));
        }

        ServerReload.reloadIfRequired(managementClient);
    }
}
