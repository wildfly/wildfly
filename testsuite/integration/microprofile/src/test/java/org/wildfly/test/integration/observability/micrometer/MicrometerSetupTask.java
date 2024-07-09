/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.integration.observability.container.OpenTelemetryCollectorContainer;
import org.wildfly.test.integration.observability.setuptask.AbstractSetupTask;

/**
 * Sets up a functioning Micrometer subsystem configuration. Requires functioning Docker environment! Tests using this
 * are expected to call AssumeTestGroupUtil.assumeDockerAvailable(); in a @BeforeClass.
 */
public class MicrometerSetupTask extends AbstractSetupTask {
    protected boolean dockerAvailable = AssumeTestGroupUtil.isDockerAvailable();

    private final ModelNode micrometerExtension = Operations.createAddress("extension", "org.wildfly.extension.micrometer");
    private final ModelNode micrometerSubsystem = Operations.createAddress("subsystem", "micrometer");
    private boolean extensionAdded = false;
    private boolean subsystemAdded = false;
    private OpenTelemetryCollectorContainer otelCollector;

    @Override
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
        executeOp(managementClient, writeAttribute("undertow", STATISTICS_ENABLED, "true"));

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerExtension))) {
            executeOp(managementClient, Operations.createAddOperation(micrometerExtension));
            extensionAdded = true;
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerSubsystem))) {
            ModelNode addOp = Operations.createAddOperation(micrometerSubsystem);
            addOp.get("endpoint").set("http://localhost:4318/v1/metrics"); // Default endpoint
            executeOp(managementClient, addOp);
            subsystemAdded = true;
        }

        if (dockerAvailable) {
            otelCollector = OpenTelemetryCollectorContainer.getInstance();
            executeOp(managementClient, writeAttribute("micrometer", "endpoint",
                    otelCollector.getOtlpHttpEndpoint() + "/v1/metrics"));
            executeOp(managementClient, writeAttribute("micrometer", "step", "1"));
        }

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
        if (dockerAvailable) {
            otelCollector.stop();
        }

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
