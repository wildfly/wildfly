/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.setuptask;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.dmr.ModelNode;

public class PrometheusSetupTask extends MicrometerSetupTask {
    public static final ModelNode PROMETHEUS_REGISTRY_ADDRESS = Operations.createAddress(SUBSYSTEM, "micrometer", "registry", "prometheus");
    public static final String PROMETHEUS_CONTEXT = "/prometheus";

    @Override
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
        super.setup(managementClient, containerId);

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, PROMETHEUS_REGISTRY_ADDRESS))) {
            ModelNode addOperation = Operations.createAddOperation(PROMETHEUS_REGISTRY_ADDRESS);
            addOperation.get("context").set(PROMETHEUS_CONTEXT);
            addOperation.get("security-enabled").set("false");
            executeOp(managementClient, addOperation);
        }

        ServerReload.reloadIfRequired(managementClient);
    }
}
