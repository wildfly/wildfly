/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.scanner;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
* Default implementation of {@link DeploymentOperations}.
*
* @author Stuart Douglas
*/
final class DefaultDeploymentOperations implements DeploymentOperations {

    private final ModelControllerClient controllerClient;

    DefaultDeploymentOperations(final ModelControllerClient controllerClient) {
        this.controllerClient = controllerClient;
    }

    @Override
    public Future<ModelNode> deploy(final ModelNode operation, final ScheduledExecutorService scheduledExecutor) {
        return controllerClient.executeAsync(operation, OperationMessageHandler.DISCARD);
    }

    @Override
    public Map<String, Boolean> getDeploymentsStatus() {
        final ModelNode op = Util.getEmptyOperation(READ_CHILDREN_RESOURCES_OPERATION, new ModelNode());
        op.get(CHILD_TYPE).set(DEPLOYMENT);
        ModelNode response;
        try {
            response = controllerClient.execute(op);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final ModelNode result = response.get(RESULT);
        final Map<String, Boolean> deployments = new HashMap<String, Boolean>();
        if (result.isDefined()) {
            for (Property property : result.asPropertyList()) {
                deployments.put(property.getName(), property.getValue().get(ENABLED).asBoolean(false));
            }
        }
        return deployments;
    }

    @Override
    public void close() throws IOException {
        controllerClient.close();
    }
}
