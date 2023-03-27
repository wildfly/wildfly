/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.observability.opentelemetry;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

public class OpenTelemetrySetupTask implements ServerSetupTask {
    private final String WILDFLY_EXTENSION_OPENTELEMETRY = "org.wildfly.extension.opentelemetry";
    private final ModelNode subsystemAddress = Operations.createAddress("subsystem", "opentelemetry");
    private final ModelNode extensionAddress = Operations.createAddress("extension", WILDFLY_EXTENSION_OPENTELEMETRY);

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        execute(managementClient, Operations.createAddOperation(extensionAddress), true);
        execute(managementClient, Operations.createAddOperation(subsystemAddress), true);

        execute(managementClient, Operations.createWriteAttributeOperation(subsystemAddress, "batch-delay", "1"), true);
        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        execute(managementClient, Operations.createRemoveOperation(subsystemAddress), true);
        execute(managementClient, Operations.createRemoveOperation(extensionAddress), true);

        ServerReload.reloadIfRequired(managementClient);
    }

    private ModelNode execute(final ManagementClient managementClient, final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            assertEquals(response.toString(), "success", outcome);
            return response.get("result");
        } else {
            assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }
}
