package org.wildfly.test.integration.metrics;

import java.io.IOException;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

public class MetricsSubsystemSetupTask implements ServerSetupTask {
    private static final String METRICS_EXTENSION = "org.wildfly.extension.metrics";
    private static final ModelNode EXTENSION_ADDRESS = Operations.createAddress(
            "extension", METRICS_EXTENSION);
    private static final ModelNode SUBSYSTEM_ADDRESS =
            Operations.createAddress("subsystem", "metrics");

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        if (!resourceExists(managementClient, EXTENSION_ADDRESS)) {
            executeOperation(managementClient,
                             Operations.createAddOperation(EXTENSION_ADDRESS));
        }
        if (!resourceExists(managementClient, SUBSYSTEM_ADDRESS)) {
            ModelNode addOperation = Operations.createAddOperation(SUBSYSTEM_ADDRESS);
            addOperation.get("security-enabled").set(false);
            addOperation.get("exposed-subsystems").add("*");
            addOperation.get("prefix").set("wildfly");
            executeOperation(managementClient, addOperation);
        }
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) {
    }

    private static boolean resourceExists(ManagementClient managementClient,
                                          ModelNode address) throws Exception {
        return Operations.isSuccessfulOutcome(managementClient.getControllerClient()
                                                              .execute(
                                                                      Operations.createReadResourceOperation(
                                                                              address)));
    }

    public ModelNode executeOperation(ManagementClient managementClient,
                                      ModelNode operation) throws IOException {
        ModelNode result = managementClient.getControllerClient().execute(operation);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new IllegalStateException(
                    Operations.getFailureDescription(result).asString());
        }
        return result;
    }
}
