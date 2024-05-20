/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.setuptask;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.integration.observability.container.OpenTelemetryCollectorContainer;

public class MicrometerSetupTask extends AbstractSetupTask {
    public static final String SUBSYSTEM_NAME = "micrometer";
    private final ModelNode micrometerExtension = Operations.createAddress("extension", "org.wildfly.extension.micrometer");
    private final ModelNode micrometerSubsystem = Operations.createAddress("subsystem", SUBSYSTEM_NAME);
    private boolean extensionAdded = false;
    private boolean subsystemAdded = false;

    @ArquillianResource
    protected OpenTelemetryCollectorContainer otelCollector;

    @Override
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
        AssumeTestGroupUtil.assumeDockerAvailable();

        executeOp(managementClient, writeAttribute("undertow", STATISTICS_ENABLED, "true"));

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerExtension))) {
            executeOp(managementClient, Operations.createAddOperation(micrometerExtension));
            extensionAdded = true;
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerSubsystem))) {
            ModelNode addOp = Operations.createAddOperation(micrometerSubsystem);
            addOp.get("endpoint").set(otelCollector.getOtlpHttpEndpoint() + "/v1/metrics"); // Default endpoint
            addOp.get("step").set("1"); // Default endpoint
            executeOp(managementClient, addOp);
            subsystemAdded = true;
        } else {
            executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "endpoint", otelCollector.getOtlpHttpEndpoint() + "/v1/metrics"));
            executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "step", "1"));
        }

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
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
