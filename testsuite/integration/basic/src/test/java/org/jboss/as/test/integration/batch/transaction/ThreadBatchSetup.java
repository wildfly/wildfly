/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.transaction;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
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

class ThreadBatchSetup extends SnapshotRestoreSetupTask {

    @Override
    public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
        setThreadSize(managementClient, 3);
    }

    private void setThreadSize(ManagementClient managementClient, int threadCount) throws IOException, MgmtOperationException {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(SUBSYSTEM, "batch-jberet");
        op.get(OP_ADDR).add("thread-pool", "batch");
        op.get(NAME).set("max-threads");
        op.get(VALUE).set(threadCount);

        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }
}
