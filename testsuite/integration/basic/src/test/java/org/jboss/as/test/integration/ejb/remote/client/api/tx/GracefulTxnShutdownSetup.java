/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.client.api.tx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * Sets up the server with graceful txn shutdown enabled in the EJB3 subsystem.
 *
 * @author Flavia Rainone
 */
public class GracefulTxnShutdownSetup implements ServerSetupTask {
    private static final String ENABLE_GRACEFUL_TXN_SHUTDOWN = "enable-graceful-txn-shutdown";

    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        final ModelNode operation = Util
                .createOperation(WRITE_ATTRIBUTE_OPERATION, PathAddress.pathAddress(SUBSYSTEM, "ejb3"));
        operation.get(NAME).set(ENABLE_GRACEFUL_TXN_SHUTDOWN);
        operation.get(VALUE).set(true);
        managementClient.getControllerClient().execute(operation);
    }

    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        final ModelNode operation = Util
                .createOperation(UNDEFINE_ATTRIBUTE_OPERATION, PathAddress.pathAddress(SUBSYSTEM, "ejb3"));
        operation.get(NAME).set(ENABLE_GRACEFUL_TXN_SHUTDOWN);
        managementClient.getControllerClient().execute(operation);
    }
}