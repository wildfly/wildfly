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
package org.jboss.as.server.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FULL_REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_ARCHIVE;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_HASH;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_PATH;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_RELATIVE_TO;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.ENABLED;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.RUNTIME_NAME;
import static org.jboss.as.server.deployment.DeploymentHandlerUtils.asString;
import static org.jboss.as.server.deployment.DeploymentHandlerUtils.createFailureException;
import static org.jboss.as.server.deployment.DeploymentHandlerUtils.getInputStream;
import static org.jboss.as.server.deployment.DeploymentHandlerUtils.hasValidContentAdditionParameterDefined;

import java.io.IOException;
import java.io.InputStream;
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
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.dmr.ModelNode;

/**
 * Handles replacement in the runtime of one deployment by another.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentFullReplaceHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = FULL_REPLACE_DEPLOYMENT;

    protected final ContentRepository contentRepository;

    private final AbstractVaultReader vaultReader;

    protected DeploymentFullReplaceHandler(final ContentRepository contentRepository, final AbstractVaultReader vaultReader) {
        assert contentRepository != null : "Null contentRepository";
        this.contentRepository = contentRepository;
        this.vaultReader = vaultReader;
    }

    public static DeploymentFullReplaceHandler create(final ContentRepository contentRepository, final AbstractVaultReader vaultReader) {
        return new DeploymentFullReplaceHandler(contentRepository, vaultReader);
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        for (AttributeDefinition def : DeploymentAttributes.FULL_REPLACE_DEPLOYMENT_ATTRIBUTES.values()) {
            def.validateOperation(operation);
        }

        String name = DeploymentAttributes.FULL_REPLACE_DEPLOYMENT_ATTRIBUTES.get(NAME).resolveModelAttribute(context, operation).asString();
        final PathAddress address = PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(DEPLOYMENT, name));

        final Resource root = context.readResource(PathAddress.EMPTY_ADDRESS);
        boolean exists = root.hasChild(PathElement.pathElement(DEPLOYMENT, name));
        if (! exists) {
            throw ServerMessages.MESSAGES.noSuchDeployment(name);
        }

        final ModelNode replaceNode = context.readResourceForUpdate(address).getModel();
        final String replacedRuntimeName = RUNTIME_NAME.resolveModelAttribute(context, replaceNode).asString();
        final String runtimeName = operation.hasDefined(RUNTIME_NAME.getName()) ? operation.get(RUNTIME_NAME.getName()).asString() : name;

        // clone it, so we can modify it to our own content
        final ModelNode content = operation.require(CONTENT).clone();
        // TODO: JBAS-9020: for the moment overlays are not supported, so there is a single content item
        final DeploymentHandlerUtil.ContentItem contentItem;
        ModelNode contentItemNode = content.require(0);
        final byte[] originalHash = replaceNode.get(CONTENT).get(0).hasDefined(CONTENT_HASH.getName()) ? CONTENT_HASH.resolveModelAttribute(context, replaceNode.get(CONTENT).get(0)).asBytes() : null;
        if (contentItemNode.hasDefined(CONTENT_HASH.getName())) {
            byte[] hash = CONTENT_HASH.resolveModelAttribute(context, contentItemNode).asBytes();

            contentItem = addFromHash(hash);
        } else if (hasValidContentAdditionParameterDefined(contentItemNode)) {
            contentItem = addFromContentAdditionParameter(context, contentItemNode);
            contentItemNode = new ModelNode();
            contentItemNode.get(CONTENT_HASH.getName()).set(contentItem.getHash());
            content.clear();
            content.add(contentItemNode);
        } else {
            contentItem = addUnmanaged(context, contentItemNode);
        }

        final ModelNode deployNode = context.readResourceForUpdate(address).getModel();
        // the content repo will already have these, note that content should not be empty
        removeContentAdditions(deployNode.require(CONTENT));

        deployNode.get(NAME).set(name);
        deployNode.get(RUNTIME_NAME.getName()).set(runtimeName);
        deployNode.get(CONTENT).set(content);
        ENABLED.validateAndSet(deployNode, replaceNode);


        if (ENABLED.resolveModelAttribute(context, replaceNode).asBoolean()) {
            DeploymentHandlerUtil.replace(context, replaceNode, runtimeName, name, replacedRuntimeName, vaultReader, contentItem);
        }

        ModelNode contentNode = replaceNode.get(CONTENT).get(0);
        final byte[] newHash = contentNode.hasDefined(CONTENT_HASH.getName()) ? CONTENT_HASH.resolveModelAttribute(context, contentNode).asBytes() : null;

        context.completeStep(new OperationContext.ResultHandler() {
            @Override
            public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
                if (resultAction == ResultAction.KEEP) {
                    if (originalHash != null  && newHash != null && !Arrays.equals(originalHash, newHash)) {
                        contentRepository.removeContent(originalHash);
                    }
                } else if (newHash != null) {
                    contentRepository.removeContent(newHash);
                }
            }
        });
    }

    private static void removeAttributes(final ModelNode node, final Iterable<String> attributeNames) {
        for (final String attributeName : attributeNames) {
            node.remove(attributeName);
        }
    }

    private static void removeContentAdditions(final ModelNode content) {
        for (final ModelNode contentItem : content.asList()) {
            removeAttributes(contentItem, DeploymentAttributes.ALL_CONTENT_ATTRIBUTES.keySet());
        }
    }

    DeploymentHandlerUtil.ContentItem addFromHash(byte[] hash) throws OperationFailedException {
        if (!contentRepository.syncContent(hash)) {
            throw ServerMessages.MESSAGES.noSuchDeploymentContent(HashUtil.bytesToHexString(hash));
        }
        return new DeploymentHandlerUtil.ContentItem(hash);
    }

    DeploymentHandlerUtil.ContentItem addFromContentAdditionParameter(OperationContext context, ModelNode contentItemNode) throws OperationFailedException {
        byte[] hash;
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
        contentItemNode.get(CONTENT_HASH.getName()).set(hash);
        // TODO: remove the content addition stuff?
        return new DeploymentHandlerUtil.ContentItem(hash);
    }

    DeploymentHandlerUtil.ContentItem addUnmanaged(OperationContext context, ModelNode contentItemNode) throws OperationFailedException {
        final String path = CONTENT_PATH.resolveModelAttribute(context, contentItemNode).asString();
        final String relativeTo = asString(contentItemNode, CONTENT_RELATIVE_TO.getName());
        final boolean archive = CONTENT_ARCHIVE.resolveModelAttribute(context, contentItemNode).asBoolean();
        return new DeploymentHandlerUtil.ContentItem(path, relativeTo, archive);
    }

}
