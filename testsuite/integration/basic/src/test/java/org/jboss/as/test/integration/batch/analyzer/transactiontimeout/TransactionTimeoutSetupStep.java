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

package org.jboss.as.test.integration.batch.analyzer.transactiontimeout;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

class TransactionTimeoutSetupStep implements ServerSetupTask {
    private int originalTimeout = -1;
    @Override
    public void setup(ManagementClient managementClient, String serverId) throws Exception {
        originalTimeout = readTimeout(managementClient);
        setTimeout(managementClient, 5);

        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    @Override
    public void tearDown(ManagementClient managementClient, String serverId) throws Exception {
        final int timeout = originalTimeout == -1 ? 300 : originalTimeout;
        setTimeout(managementClient, timeout);

        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    private int readTimeout(ManagementClient managementClient) throws IOException, MgmtOperationException {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(ADDRESS).add(SUBSYSTEM);
        op.get(ADDRESS).add("transactions");
        op.get(NAME).set("default-timeout");

        ModelNode res = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        return res.asInt();
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