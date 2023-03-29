/*
 * Copyright 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.observability.micrometer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

public class MicrometerSetupTask implements ServerSetupTask {
    private final ModelNode metricsExtension = Operations.createAddress("extension", "org.wildfly.extension.metrics");
    private final ModelNode micrometerExtension = Operations.createAddress("extension", "org.wildfly.extension.micrometer");
    private final ModelNode metricsSubsystem = Operations.createAddress("subsystem", "metrics");
    private final ModelNode micrometerSubsystem = Operations.createAddress("subsystem", "micrometer");

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        if (modificationsNeeded()) {
            execute(managementClient, Operations.createAddOperation(micrometerExtension), true);

            ModelNode addOp = Operations.createAddOperation(micrometerSubsystem);
            addOp.get("endpoint").set("http://localhost:4318/v1/metrics");
            execute(managementClient, addOp, true);

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        if (modificationsNeeded()) {
            execute(managementClient, Operations.createRemoveOperation(micrometerSubsystem), true);
            execute(managementClient, Operations.createRemoveOperation(micrometerExtension), true);

            ServerReload.reloadIfRequired(managementClient);
        }
    }

    private boolean modificationsNeeded() {
        return (System.getProperty("ts.layers") == null) &&
                (System.getProperty("ts.bootable") == null) &&
                (System.getProperty("ts.bootable.ee9") == null);
    }

    private ModelNode execute(final ManagementClient managementClient,
                              final ModelNode op,
                              final boolean expectSuccess) throws IOException {
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
