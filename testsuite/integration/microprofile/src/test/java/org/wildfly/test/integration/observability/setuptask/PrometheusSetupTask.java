package org.wildfly.test.integration.observability.setuptask;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.test.integration.observability.setuptask.AbstractSetupTask.executeOp;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

public class PrometheusSetupTask extends StabilityServerSetupSnapshotRestoreTasks.Preview {
    public static final ModelNode PROMETHEUS_REGISTRY_ADDRESS = Operations.createAddress(SUBSYSTEM, "micrometer", "registry", "prometheus");
    public static final String PROMETHEUS_CONTEXT = "/prometheus";

    @Override
    protected void doSetup(ManagementClient managementClient) throws Exception {
        super.doSetup(managementClient);

        System.err.println("***** Adding prometheus support");

        ModelNode addOperation = Operations.createAddOperation(PROMETHEUS_REGISTRY_ADDRESS);
        addOperation.get("context").set(PROMETHEUS_CONTEXT);
        addOperation.get("security-enabled").set("false");
        executeOp(managementClient, addOperation);
        ServerReload.reloadIfRequired(managementClient);
    }
}
