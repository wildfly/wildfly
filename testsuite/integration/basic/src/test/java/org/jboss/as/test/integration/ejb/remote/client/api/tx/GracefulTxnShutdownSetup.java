/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * Sets up the server with graceful txn shutdown enabled in the EJB3 subsystem.
 *
 * @author Flavia Rainone
 */
public class GracefulTxnShutdownSetup implements ServerSetupTask {

    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        final ModelNode operation = Util
                .createOperation(WRITE_ATTRIBUTE_OPERATION, PathAddress.pathAddress(SUBSYSTEM, "ejb3"));
        operation.get(NAME).set(EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN.getName());
        operation.get(VALUE).set(true);
        managementClient.getControllerClient().execute(operation);
    }

    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        final ModelNode operation = Util
                .createOperation(UNDEFINE_ATTRIBUTE_OPERATION, PathAddress.pathAddress(SUBSYSTEM, "ejb3"));
        operation.get(NAME).set(EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN.getName());
        managementClient.getControllerClient().execute(operation);
    }
}