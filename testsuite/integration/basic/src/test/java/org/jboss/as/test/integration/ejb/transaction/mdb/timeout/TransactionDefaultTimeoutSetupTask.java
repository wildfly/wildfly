/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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