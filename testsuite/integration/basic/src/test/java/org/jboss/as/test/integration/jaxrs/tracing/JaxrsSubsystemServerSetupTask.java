/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.tracing;

import java.util.Map;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class JaxrsSubsystemServerSetupTask extends SnapshotRestoreSetupTask {

    @Override
    protected void doSetup(final ManagementClient client, final String containerId) throws Exception {
        final ModelNode address = Operations.createAddress("subsystem", "jaxrs");
        // Write each attribute in a composite operation
        final CompositeOperationBuilder builder = CompositeOperationBuilder.create();
        subsystemAttributes().forEach((name, value) -> {
            if (value instanceof String) {
                builder.addStep(Operations.createWriteAttributeOperation(address, name, (String) value));
            } else if (value instanceof Boolean) {
                builder.addStep(Operations.createWriteAttributeOperation(address, name, (Boolean) value));
            } else if (value instanceof Integer) {
                builder.addStep(Operations.createWriteAttributeOperation(address, name, (Integer) value));
            } else if (value instanceof Long) {
                builder.addStep(Operations.createWriteAttributeOperation(address, name, (Long) value));
            } else if (value instanceof ModelNode) {
                builder.addStep(Operations.createWriteAttributeOperation(address, name, (ModelNode) value));
            } else {
                builder.addStep(Operations.createWriteAttributeOperation(address, name, String.valueOf(value)));
            }
        });

        final ModelNode result = client.getControllerClient().execute(builder.build());
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to write attributes: " + Operations.getFailureDescription(result).asString());
        }
        // Reload if required
        ServerReload.reloadIfRequired(client);
    }

    protected abstract Map<String, Object> subsystemAttributes();
}
