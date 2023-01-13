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
        execute(managementClient, Operations.createRemoveOperation(metricsSubsystem), true);
        execute(managementClient, Operations.createRemoveOperation(metricsExtension), true);

        execute(managementClient, Operations.createAddOperation(micrometerExtension), true);
        execute(managementClient, Operations.createAddOperation(micrometerSubsystem), true);

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        execute(managementClient, Operations.createRemoveOperation(micrometerSubsystem), true);
        execute(managementClient, Operations.createRemoveOperation(micrometerExtension), true);

        execute(managementClient, Operations.createAddOperation(metricsExtension), true);
        execute(managementClient, Operations.createAddOperation(metricsSubsystem), true);

        ServerReload.reloadIfRequired(managementClient);
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
