/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
