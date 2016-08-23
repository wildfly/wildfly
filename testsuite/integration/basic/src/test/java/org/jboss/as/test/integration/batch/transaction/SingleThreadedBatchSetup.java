/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.batch.transaction;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

class SingleThreadedBatchSetup implements ServerSetupTask {

    private int originalThreadCount = -1;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        this.originalThreadCount = readBatchThreadSize(managementClient);

        setThreadSize(managementClient, 1);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        final int threadCount = originalThreadCount == -1 ? 10 : originalThreadCount;

        setThreadSize(managementClient, threadCount);
    }

    private int readBatchThreadSize(ManagementClient managementClient) throws IOException, MgmtOperationException {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(SUBSYSTEM, "batch-jberet");
        op.get(OP_ADDR).add("thread-pool", "batch");
        op.get(NAME).set("max-threads");

        final ModelNode res = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        return res.asInt();
    }

    private void setThreadSize(ManagementClient managementClient, int threadCount) throws IOException, MgmtOperationException {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(SUBSYSTEM, "batch-jberet");
        op.get(OP_ADDR).add("thread-pool", "batch");
        op.get(NAME).set("max-threads");
        op.get(VALUE).set(threadCount);

        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }
}
