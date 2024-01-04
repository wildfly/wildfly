/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.mdb.timeout;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;

/**
 * Setup task to set default transaction timeout for 1 second.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
public class TransactionDefaultTimeoutSetupTask implements ServerSetupTask {

    private static final String DEFAULT_TIMEOUT_PARAM_NAME = "default-timeout";
    private static ModelNode address = new ModelNode().add(ClientConstants.SUBSYSTEM, "transactions");
    private static ModelNode operation = new ModelNode();

    static {
        operation.get(ClientConstants.OP_ADDR).set(address);
    }

    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        operation.get(ClientConstants.OP).set(ClientConstants.WRITE_ATTRIBUTE_OPERATION);
        operation.get(ClientConstants.NAME).set(DEFAULT_TIMEOUT_PARAM_NAME);
        operation.get(ClientConstants.VALUE).set(1); // 1 second

        managementClient.getControllerClient().execute(operation);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        operation.get(ClientConstants.OP).set(ClientConstants.UNDEFINE_ATTRIBUTE_OPERATION);
        operation.get(ClientConstants.NAME).set(DEFAULT_TIMEOUT_PARAM_NAME);

        managementClient.getControllerClient().execute(operation);
    }

}