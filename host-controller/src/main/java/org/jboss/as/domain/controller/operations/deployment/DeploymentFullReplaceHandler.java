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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;
import static org.jboss.as.domain.controller.operations.deployment.AbstractDeploymentHandler.CONTENT_ADDITION_PARAMETERS;
import static org.jboss.as.domain.controller.operations.deployment.AbstractDeploymentHandler.createFailureException;
import static org.jboss.as.domain.controller.operations.deployment.AbstractDeploymentHandler.getInputStream;
import static org.jboss.as.domain.controller.operations.deployment.AbstractDeploymentHandler.hasValidContentAdditionParameterDefined;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.descriptions.DeploymentDescription;
import org.jboss.dmr.ModelNode;

/**
 * Handles replacement in the runtime of one deployment by another.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentFullReplaceHandler implements OperationStepHandler, DescriptionProvider {

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

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getFullReplaceDeploymentOperation(locale);
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        //TODO readd validation and define attributes

        String name = operation.require(NAME).asString();
        String runtimeName = operation.hasDefined(RUNTIME_NAME) ? operation.get(RUNTIME_NAME).asString() : name;
        byte[] hash;
        // clone it, so we can modify it to our own content
        final ModelNode content = operation.require(CONTENT).clone();
        // TODO: JBAS-9020: for the moment overlays are not supported, so there is a single content item
        final ModelNode contentItemNode = content.require(0);
        if (contentItemNode.hasDefined(HASH)) {
            hash = contentItemNode.require(HASH).asBytes();
            if (contentRepository != null) {
                // We are the master DC. Validate that we actually have this content.
                if (!contentRepository.hasContent(hash)) {
                    throw createFailureException(MESSAGES.noDeploymentContentWithHash(HashUtil.bytesToHexString(hash)));
                }
            } else {
                // We are a slave controller
                // Ensure the local repo has the files
                fileRepository.getDeploymentFiles(hash);
            }
        } else if (hasValidContentAdditionParameterDefined(contentItemNode)) {
            if (contentRepository == null) {
                // This is a slave DC. We can't handle this operation; it should have been fixed up on the master DC
                throw createFailureException(MESSAGES.slaveCannotAcceptUploads());
            }

            InputStream in = getInputStream(context, contentItemNode);
            try {
                try {
                    hash = contentRepository.addContent(in);
                } catch (IOException e) {
                    throw createFailureException(e.toString());
                }

            } finally {
                StreamUtils.safeClose(in);
            }
            contentItemNode.clear(); // AS7-1029
            contentItemNode.get(HASH).set(hash);
        } else {
            // Unmanaged content, the user is responsible for replication
            // Just validate the required attributes are present
        }

        final Resource root = context.readResource(PathAddress.EMPTY_ADDRESS);
        final PathElement deploymentPath = PathElement.pathElement(DEPLOYMENT, name);
        final Resource replaceNode = root.getChild(deploymentPath);
        if (replaceNode == null) {
            throw createFailureException(MESSAGES.noDeploymentContentWithName(name));
        }

        final Resource deployment = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(DEPLOYMENT, name)));
        ModelNode deployNode = deployment.getModel();
        byte[] originalHash = deployNode.get(CONTENT).get(0).hasDefined(HASH) ? deployNode.get(CONTENT).get(0).get(HASH).asBytes() : null;
        deployNode.get(NAME).set(name);
        deployNode.get(RUNTIME_NAME).set(runtimeName);
        deployNode.get(CONTENT).set(content);

        if (root.hasChild(PathElement.pathElement(SERVER_GROUP))) {
            for (final Resource.ResourceEntry serverGroupResource : root.getChildren(SERVER_GROUP)) {
                Resource deploymentResource = serverGroupResource.getChild(deploymentPath);
                if (deploymentResource != null) {
                    deploymentResource.getModel().get(RUNTIME_NAME).set(runtimeName);
                }
            }
        }
        // the content repo will already have these, note that content should not be empty
        removeContentAdditions(replaceNode.getModel().require(CONTENT));
        if (context.completeStep() == ResultAction.KEEP) {
            if (originalHash != null && contentRepository != null) {
                if (deployNode.get(CONTENT).get(0).hasDefined(HASH)) {
                    byte[] newHash = deployNode.get(CONTENT).get(0).get(HASH).asBytes();
                    if (!Arrays.equals(originalHash, newHash)) {
                        contentRepository.removeContent(originalHash);
                    }
                }
            }
        } else {
            if (contentRepository != null && operation.get(CONTENT).get(0).hasDefined(HASH)) {
                byte[] newHash = operation.get(CONTENT).get(0).get(HASH).asBytes();
                contentRepository.removeContent(newHash);
            }
        }

    }

    private static void removeAttributes(final ModelNode node, final Iterable<String> attributeNames) {
        for (final String attributeName : attributeNames) {
            node.remove(attributeName);
        }
    }

    private static void removeContentAdditions(final ModelNode content) {
        for (final ModelNode contentItem : content.asList()) {
            removeAttributes(contentItem, CONTENT_ADDITION_PARAMETERS);
        }
    }
}
