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

    private final ModelNode subsystemAddress = Operations.createAddress("subsystem", "opentelemetry");

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        execute(managementClient, Operations.createWriteAttributeOperation(subsystemAddress, "batch-delay", "1"));
        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        execute(managementClient, Operations.createWriteAttributeOperation(subsystemAddress, "batch-delay", new ModelNode()));
        ServerReload.reloadIfRequired(managementClient);
    }

    private void execute(final ManagementClient managementClient, final ModelNode op) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        assertEquals(response.toString(), "success", outcome);
    }
}
