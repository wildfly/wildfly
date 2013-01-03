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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_REPLACE;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.NoSuchElementException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.dmr.ModelNode;

/**
 * Handles replacement in the runtime of one deployment by another.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerGroupDeploymentReplaceHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = REPLACE_DEPLOYMENT;

    private final HostFileRepository fileRepository;

    public ServerGroupDeploymentReplaceHandler(final HostFileRepository fileRepository) {
        if (fileRepository == null) {
            throw MESSAGES.nullVar("fileRepository");
        }
        this.fileRepository = fileRepository;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        for (AttributeDefinition def : DeploymentAttributes.SERVER_GROUP_REPLACE_DEPLOYMENT_ATTRIBUTES.values()) {
            def.validateOperation(operation);
        }
        String name = DeploymentAttributes.SERVER_GROUP_REPLACE_DEPLOYMENT_ATTRIBUTES.get(NAME).resolveModelAttribute(context, operation).asString();
        String toReplace = DeploymentAttributes.SERVER_GROUP_REPLACE_DEPLOYMENT_ATTRIBUTES.get(TO_REPLACE).resolveModelAttribute(context, operation).asString();

        if (name.equals(toReplace)) {
            throw operationFailed(MESSAGES.cannotUseSameValueForParameters(OPERATION_NAME, NAME, TO_REPLACE,
                    ServerGroupDeploymentRedeployHandler.OPERATION_NAME, DeploymentFullReplaceHandler.OPERATION_NAME));
        }

        final PathElement deploymentPath = PathElement.pathElement(DEPLOYMENT, name);
        final PathElement replacePath = PathElement.pathElement(DEPLOYMENT, toReplace);

        Resource domainDeployment;
        try {
            // check if the domain deployment exists
            domainDeployment = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS.append(deploymentPath));
        } catch (NoSuchElementException e) {
            throw operationFailed(MESSAGES.noDeploymentContentWithName(name));
        }

        final ModelNode deployment = domainDeployment.getModel();
        for (ModelNode content : deployment.require(CONTENT).asList()) {
            if ((content.hasDefined(HASH))) {
                byte[] hash = content.require(HASH).asBytes();
                // Ensure the local repo has the files
                fileRepository.getDeploymentFiles(hash);
            }
        }

        final Resource serverGroup = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        if (! serverGroup.hasChild(replacePath)) {
            throw operationFailed(MESSAGES.noDeploymentContentWithName(toReplace));
        }
        final Resource replaceResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS.append(replacePath));
        //
        final Resource deploymentResource;
        if(! serverGroup.hasChild(deploymentPath)) {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS.append(deploymentPath));
            final ModelNode deployNode = resource.getModel();
            deployNode.set(deployment); // Get the information from the domain deployment
            deployNode.remove("content"); // Prune the content information
            deployNode.get(ENABLED).set(true); // Enable
        } else {
            deploymentResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS.append(deploymentPath));
            if(deploymentResource.getModel().get(ENABLED).asBoolean()) {
                throw operationFailed(MESSAGES.deploymentAlreadyStarted(toReplace));
            }
            deploymentResource.getModel().get(ENABLED).set(true);
        }
        //
        replaceResource.getModel().get(ENABLED).set(false);
        context.stepCompleted();
    }

    private static OperationFailedException operationFailed(String msg) {
        return new OperationFailedException(new ModelNode().set(msg));
    }
}
