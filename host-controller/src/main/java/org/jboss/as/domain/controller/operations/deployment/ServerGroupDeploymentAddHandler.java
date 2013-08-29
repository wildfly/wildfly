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

import java.util.List;
import java.util.Set;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_HASH;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.RUNTIME_NAME_NILLABLE;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.SERVER_GROUP_ADD_ATTRIBUTES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainControllerMessages;
import org.jboss.as.repository.HostFileRepository;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.RUNTIME_NAME;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Handles addition of a deployment to a server group.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerGroupDeploymentAddHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = ADD;

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
        String runtimeName = deployment.get(RUNTIME_NAME.getName()).asString();

        String serverGroupName = getServerGroupName(operation);
        isRuntimeNameUniqueForServerGroup(serverGroupName, context, name, runtimeName);

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
        if (!RUNTIME_NAME_NILLABLE.resolveModelAttribute(context, subModel).isDefined()) {
            RUNTIME_NAME_NILLABLE.validateAndSet(deployment, subModel);
        }

        context.stepCompleted();
    }

    private void isRuntimeNameUniqueForServerGroup(String serverGroupName, OperationContext context, String name, String runtimeName) throws OperationFailedException {
        if(serverGroupName != null) {
            PathAddress address = PathAddress.pathAddress(PathAddress.pathAddress(SERVER_GROUP, serverGroupName), PathElement.pathElement(DEPLOYMENT));
            Set<Resource.ResourceEntry> deployments = context.readResourceFromRoot(address).getChildren(DEPLOYMENT);
            for(Resource.ResourceEntry existingDeployment : deployments) {
                ModelNode existingDeploymentModel = existingDeployment.getModel();
                if(existingDeploymentModel.hasDefined(RUNTIME_NAME.getName()) && !name.equals(existingDeployment.getName())) {
                    if(existingDeploymentModel.get(RUNTIME_NAME.getName()).asString().equals(runtimeName)) {
                        throw DomainControllerMessages.MESSAGES.runtimeNameMustBeUnique(existingDeployment.getName(), runtimeName, serverGroupName);
                    }
                }
            }
        }
    }

    private String getServerGroupName(ModelNode operation) {
        if(operation.hasDefined(ADDRESS)) {
            List<Property> adress = operation.get(ADDRESS).asPropertyList();
            for(Property prop : adress) {
                if(SERVER_GROUP.equals(prop.getName())) {
                    return prop.getValue().asString();
                }
            }
        }
        return null;
    }
}
