/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.domain.controller.operations.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Handles removal of a deployment from the model. This can be used at either the domain deployments level
 * or the server-group deployments level
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentRemoveHandler implements ModelRemoveOperationHandler {

    public static final String OPERATION_NAME = REMOVE;

    public static final DeploymentRemoveHandler INSTANCE = new DeploymentRemoveHandler();

    private DeploymentRemoveHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        final ModelNode model = context.getSubModel();
        final ModelNode compensatingOp = DeploymentAddHandler.getOperation(operation.get(OP_ADDR), model);

        final String deploymentName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();

        final ModelNode root = context.getSubModel(PathAddress.EMPTY_ADDRESS);
        if (root.hasDefined(SERVER_GROUP)) {
            List<String> badGroups = new ArrayList<String>();
            for (Property prop : root.get(SERVER_GROUP).asPropertyList()) {
                ModelNode sg = prop.getValue();
                if (sg.hasDefined(DEPLOYMENT) && sg.get(DEPLOYMENT).has(deploymentName)) {
                    badGroups.add(prop.getName());
                }
            }

            if (badGroups.size() > 0) {
                String msg = String.format("Cannot remove deployment %s from the domain as it is still used by server groups %s", deploymentName, badGroups);
                throw new OperationFailedException(new ModelNode().set(msg));
            }
        }

        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOp);
    }
}
