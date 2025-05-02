/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.util.Map;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ExtendedSnapshotServerSetupTask;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractWriteAttributesServerSetupTask extends ExtendedSnapshotServerSetupTask {
    private static final ModelNode ADDRESS = Operations.createAddress("subsystem", "jaxrs");
    private final Map<String, ModelNode> attributes;

    AbstractWriteAttributesServerSetupTask(final Map<String, ModelNode> attributes) {
        this.attributes = Map.copyOf(attributes);
    }

    @Override
    protected void doSetup(final ManagementClient client, final String containerId) throws Exception {
        if (!attributes.isEmpty()) {
            final Operations.CompositeOperationBuilder builder = Operations.CompositeOperationBuilder.create();
            for (Map.Entry<String, ModelNode> entry : attributes.entrySet()) {
                builder.addStep(Operations.createWriteAttributeOperation(ADDRESS, entry.getKey(), entry.getValue()));
            }
            executeOperation(client, builder.build());
        }
    }
}
