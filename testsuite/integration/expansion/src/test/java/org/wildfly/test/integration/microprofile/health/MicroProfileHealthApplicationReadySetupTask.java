/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

public class MicroProfileHealthApplicationReadySetupTask implements ServerSetupTask {

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        final ModelNode address = Operations.createAddress("subsystem", "microprofile-health-smallrye");
        final ModelNode op = Operations.createWriteAttributeOperation(address, "empty-readiness-checks-status", "DOWN" );
        managementClient.getControllerClient().execute(op);

        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        final ModelNode address = Operations.createAddress("subsystem", "microprofile-health-smallrye");
        final ModelNode op = Operations.createUndefineAttributeOperation(address, "empty-readiness-checks-status");
        managementClient.getControllerClient().execute(op);

        ServerReload.reloadIfRequired(managementClient);
    }
}
