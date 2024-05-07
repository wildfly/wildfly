/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.setuptask;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

public class PrometheusSetupTask extends AbstractSetupTask {
    public static final String CONTEXT_PROMETHEUS = "prometheus";
    public static final ModelNode ADDRESS_PROMETHEUS_REGISTRY = Operations.createAddress("subsystem", "micrometer", "registry", CONTEXT_PROMETHEUS);

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        ModelNode op = Operations.createAddOperation(ADDRESS_PROMETHEUS_REGISTRY);
        op.get("context").set(CONTEXT_PROMETHEUS); // Default endpoint
        op.get("security-enabled").set("false");

        executeOp(managementClient, op);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
    }
}
