/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.coordinator;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.integration.microprofile.lra.EnableLRAExtensionsSetupTask;

public class EnableLRAOpenAPISetupTask extends EnableLRAExtensionsSetupTask {

    @Override
    protected void doSetup(final ManagementClient managementClient, final String containerId) throws Exception {
        super.doSetup(managementClient, containerId);

        // Ensure server and host attributes are set on the coordinator subsystem.
        // When provisioned via Glow (e.g. bootable jar), the subsystem may already exist
        // without these attributes, and the OpenAPI service is only registered when both are defined.
        ModelNode address = Operations.createAddress("subsystem", "microprofile-lra-coordinator");
        ModelNode result = executeOperation(managementClient, Operations.createReadAttributeOperation(address, "server"));
        if (!result.isDefined()) {
            executeOperation(managementClient, Operations.createWriteAttributeOperation(address, "server", "default-server"));
            executeOperation(managementClient, Operations.createWriteAttributeOperation(address, "host", "default-host"));
        }

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }
}
