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
    private final ModelNode address = Operations.createAddress("subsystem", "opentelemetry");

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        execute(managementClient, Operations.createAddOperation(Operations.createAddress("extension",
                WILDFLY_EXTENSION_OPENTELEMETRY)), true);
        execute(managementClient, Operations.createAddOperation(address), true);

        execute(managementClient, Operations.createWriteAttributeOperation(address,
                "span-processor-type", "simple"), true);
        execute(managementClient, Operations.createWriteAttributeOperation(address,
                "batch-delay", "1"), true);
        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        final ModelNode address = Operations.createAddress("subsystem", "opentelemetry");
        execute(managementClient, Operations.createRemoveOperation(address), true);
        execute(managementClient, Operations.createRemoveOperation(Operations.createAddress("extension",
                WILDFLY_EXTENSION_OPENTELEMETRY)), true);
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
