/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.analyzer.transactiontimeout;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;

class TransactionTimeoutSetupStep extends SnapshotRestoreSetupTask {
    @Override
    public void doSetup(ManagementClient managementClient, String serverId) throws Exception {
        setTimeout(managementClient, 5);

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    private void setTimeout(ManagementClient managementClient, int timeout) throws IOException, MgmtOperationException {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(ADDRESS).add(SUBSYSTEM);
        op.get(ADDRESS).add("transactions");
        op.get(NAME).set("default-timeout");
        op.get(VALUE).set(timeout);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }
}