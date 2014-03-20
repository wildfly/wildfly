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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FULL_REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;
import static org.jboss.as.domain.controller.operations.deployment.AbstractDeploymentHandler.createFailureException;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_HASH;

import java.io.IOException;
import java.util.Arrays;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.as.server.deployment.DeploymentHandlerUtils;
import org.jboss.dmr.ModelNode;

/**
 * Handles replacement in the runtime of one deployment by another.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentFullReplaceHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = FULL_REPLACE_DEPLOYMENT;

    private final ContentRepository contentRepository;
    private final HostFileRepository fileRepository;

    /**
     * Constructor for a master Host Controller
     */
    public DeploymentFullReplaceHandler(final ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
        this.fileRepository = null;
    }

    /**
     * Constructor for a slave Host Controller
     */
    public DeploymentFullReplaceHandler(final HostFileRepository fileRepository) {
        this.contentRepository = null;
        this.fileRepository = fileRepository;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Validate op
        for (AttributeDefinition def : DeploymentAttributes.FULL_REPLACE_DEPLOYMENT_ATTRIBUTES.values()) {
            def.validateOperation(operation);
        }

        // Pull data from the op
        final String name = DeploymentAttributes.NAME.resolveModelAttribute(context, operation).asString();
        final PathElement deploymentPath = PathElement.pathElement(DEPLOYMENT, name);
        String runtimeName = operation.hasDefined(RUNTIME_NAME)
                ? DeploymentAttributes.RUNTIME_NAME.resolveModelAttribute(context, operation).asString() : name;
        // clone the content param, so we can modify it to our own content
        ModelNode content = operation.require(CONTENT).clone();

        // Throw a specific exception if the replaced deployment doesn't already exist
        // BES 2013/10/30 -- this is pointless; the readResourceForUpdate call will throw
        // an exception with an equally informative message if the deployment doesn't exist
//        final Resource root = context.readResource(PathAddress.EMPTY_ADDRESS);
//        boolean exists = root.hasChild(deploymentPath);
//        if (!exists) {
//            throw createFailureException(MESSAGES.noDeploymentContentWithName(name));
//        }

        final ModelNode deploymentModel = context.readResourceForUpdate(PathAddress.pathAddress(deploymentPath)).getModel();

        // Keep track of hash we are replacing so we can drop it from the content repo if all is well
        ModelNode replacedContent = deploymentModel.get(CONTENT).get(0);
        final byte[] replacedHash = replacedContent.hasDefined(CONTENT_HASH.getName())
                ? CONTENT_HASH.resolveModelAttribute(context, replacedContent).asBytes() : null;

        // Set up the new content attribute
        final byte[] newHash;
        ModelNode contentItemNode = content.require(0);
        if (contentItemNode.hasDefined(HASH)) {
            newHash = contentItemNode.require(HASH).asBytes();
            if (contentRepository != null) {
                // We are the master DC. Validate that we actually have this content.
                if (!contentRepository.hasContent(newHash)) {
                    throw createFailureException(MESSAGES.noDeploymentContentWithHash(HashUtil.bytesToHexString(newHash)));
                }
            }
        } else if (DeploymentHandlerUtils.hasValidContentAdditionParameterDefined(contentItemNode)) {
            if (contentRepository == null) {
                // This is a slave DC. We can't handle this operation; it should have been fixed up on the master DC
                throw createFailureException(MESSAGES.slaveCannotAcceptUploads());
            }
            try {
                // Store and transform operation
                newHash = DeploymentUploadUtil.storeContentAndTransformOperation(context, operation, contentRepository);
            } catch (IOException e) {
                throw createFailureException(e.toString());
            }

            // Replace the op-provided content node with one that has a hash
            contentItemNode = new ModelNode();
            contentItemNode.get(HASH).set(newHash);
            content = new ModelNode();
            content.add(contentItemNode);

        } else {
            // Unmanaged content, the user is responsible for replication
            newHash = null;
        }

        // Store state to the model
        //deploymentModel.get(NAME).set(name);  Already there
        deploymentModel.get(RUNTIME_NAME).set(runtimeName);
        deploymentModel.get(CONTENT).set(content);

        // Update server groups
        final Resource root = context.readResource(PathAddress.EMPTY_ADDRESS);
        if (root.hasChild(PathElement.pathElement(SERVER_GROUP))) {
            for (final Resource.ResourceEntry serverGroupResource : root.getChildren(SERVER_GROUP)) {
                Resource deploymentResource = serverGroupResource.getChild(deploymentPath);
                if (deploymentResource != null) {
                    deploymentResource.getModel().get(RUNTIME_NAME).set(runtimeName);
                }
            }
        }

        context.completeStep(new OperationContext.ResultHandler() {
            @Override
            public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
                if (resultAction == ResultAction.KEEP) {
                    if (replacedHash != null  && (newHash == null || !Arrays.equals(replacedHash, newHash))) {
                        // The old content is no longer used; clean from repos
                        if(contentRepository != null) {
                            contentRepository.removeContent(replacedHash, name);
                        } else {
                            fileRepository.deleteDeployment(replacedHash);
                        }
                    }
                    if (newHash != null && contentRepository != null) {
                        contentRepository.addContentReference(newHash, name);
                    }
                } else if (newHash != null && (replacedHash == null || !Arrays.equals(replacedHash, newHash))) {
                    // Due to rollback, the new content isn't used; clean from repos
                    if (contentRepository != null ) {
                        contentRepository.removeContent(newHash, name);
                    } else {
                        fileRepository.deleteDeployment(newHash);
                    }
                }
            }
        });
    }
}
