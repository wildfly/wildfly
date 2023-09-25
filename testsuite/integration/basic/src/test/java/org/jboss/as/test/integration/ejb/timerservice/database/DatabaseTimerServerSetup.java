/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.timerservice.database;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

/**
 * @author Stuart Douglas
 */
class DatabaseTimerServerSetup implements ServerSetupTask {

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

        ServerReload.BeforeSetupTask.INSTANCE.setup(managementClient, containerId);

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "ejb3");
        op.get(OP_ADDR).add("service", "timer-service");
        op.get(OP_ADDR).add("database-data-store", "dbstore");
        op.get("datasource-jndi-name").set("java:jboss/datasources/ExampleDS");
        op.get("database").set("hsql");
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "ejb3");
        op.get(OP_ADDR).add("service", "timer-service");
        op.get(OP_ADDR).add("database-data-store", "dbstore");
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }
}
