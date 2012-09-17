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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_HASH;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.ENABLED;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.RUNTIME_NAME;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.SERVER_GROUP_ADD_ATTRIBUTES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.dmr.ModelNode;

/**
 * Handles addition of a deployment to a server group.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerGroupDeploymentAddHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = ADD;

    static final ModelNode getOperation(ModelNode address, ModelNode state) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        op.get(RUNTIME_NAME.getName()).set(state.get(RUNTIME_NAME.getName()));
        op.get(HASH).set(state.get(HASH));
        op.get(ENABLED.getName()).set(state.get(ENABLED.getName()));
        return op;
    }

    private final HostFileRepository fileRepository;

    public ServerGroupDeploymentAddHandler(final HostFileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode opAddr = operation.get(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(opAddr);
        String name = address.getLastElement().getValue();

        final Resource deploymentResource = context.readResourceFromRoot(PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, name)));
        ModelNode deployment = deploymentResource.getModel();

        for (ModelNode content : deployment.require(CONTENT).asList()) {
            if ((content.hasDefined(CONTENT_HASH.getName()))) {
                CONTENT_HASH.validateOperation(content);
                // Ensure the local repo has the files
                fileRepository.getDeploymentFiles(CONTENT_HASH.resolveModelAttribute(context, content).asBytes());
            }
        }

        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode subModel = resource.getModel();

        for (AttributeDefinition def : SERVER_GROUP_ADD_ATTRIBUTES) {
            def.validateAndSet(operation, subModel);
        }
        if (!RUNTIME_NAME.resolveModelAttribute(context, subModel).isDefined()) {
            RUNTIME_NAME.validateAndSet(deployment, subModel);
        }

        context.completeStep();
    }
}
