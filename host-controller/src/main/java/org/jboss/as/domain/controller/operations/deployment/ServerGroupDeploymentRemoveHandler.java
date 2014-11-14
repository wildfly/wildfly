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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.List;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.deployment.ModelContentReference;
import org.jboss.dmr.ModelNode;

/**
 * Handles removal of a deployment from the model. This can be used at either the domain deployments level or the
 * server-group deployments level
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerGroupDeploymentRemoveHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = REMOVE;

    private final ContentRepository contentRepository;

    public ServerGroupDeploymentRemoveHandler(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final PathAddress operationAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
        PathAddress deploymentAddress = PathAddress.pathAddress(operationAddress.getLastElement());
        ModelNode deployment = context.readResourceFromRoot(deploymentAddress, false).getModel();
        final byte[] hash;
        if (deployment.has(CONTENT)) {
            byte[] currentHash = null;
            if (deployment.get(CONTENT).has(HASH)) {
                currentHash = deployment.get(CONTENT).get(HASH).asBytes();
            } else {
                List<ModelNode> nodes = deployment.get(CONTENT).asList();
                for (ModelNode node : nodes) {
                    if (node.has(HASH)) {
                        currentHash = node.get(HASH).asBytes();
                    }
                }
            }
            hash = currentHash;
        } else {
            hash = null;
        }
        context.removeResource(PathAddress.EMPTY_ADDRESS);
        context.completeStep(new OperationContext.ResultHandler() {
            @Override
            public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                if (resultAction != OperationContext.ResultAction.ROLLBACK) {
                    if (contentRepository != null && hash != null) {
                        PathAddress operationAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
                        contentRepository.removeContent(ModelContentReference.fromModelAddress(operationAddress, hash));
                    }
                }
            }
        });
    }
}
