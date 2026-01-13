/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.setuptasks;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

//@TestcontainersRequired
public class OpenTelemetrySetupTask extends InMemoryCollectorSetupTask {
    protected static final String SUBSYSTEM_NAME = "opentelemetry";
    public static final ModelNode OPENTELEMETRY_EXTENSION = Operations.createAddress("extension", "org.wildfly.extension.opentelemetry");
    public static final ModelNode OPENTELEMETRY_ADDRESS = Operations.createAddress("subsystem", SUBSYSTEM_NAME);

    private volatile boolean addedExtension;
    private volatile boolean addedSubsystem;

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        super.setup(managementClient, containerId);

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, OPENTELEMETRY_EXTENSION))) {
            executeOp(managementClient, Operations.createAddOperation(OPENTELEMETRY_EXTENSION));
            addedExtension = true;
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, OPENTELEMETRY_ADDRESS))) {
            executeOp(managementClient, Operations.createAddOperation(OPENTELEMETRY_ADDRESS));
            addedSubsystem = true;
        }

        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "batch-delay", "1"));
        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "sampler-type", "on"));

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        if (addedSubsystem) {
            executeOp(managementClient, Operations.createRemoveOperation(OPENTELEMETRY_ADDRESS));
        }
        if (addedExtension) {
            executeOp(managementClient, Operations.createRemoveOperation(OPENTELEMETRY_EXTENSION));
        }

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        super.tearDown(managementClient, containerId);
    }
}
