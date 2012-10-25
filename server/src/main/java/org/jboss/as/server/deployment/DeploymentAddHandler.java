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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_ALL;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_ARCHIVE;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_HASH;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_PATH;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_RELATIVE_TO;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.ENABLED;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.PERSISTENT;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.POLICY;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.RUNTIME_NAME;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.SERVER_ADD_ATTRIBUTES;
import static org.jboss.as.server.deployment.DeploymentHandlerUtils.asString;
import static org.jboss.as.server.deployment.DeploymentHandlerUtils.createFailureException;
import static org.jboss.as.server.deployment.DeploymentHandlerUtils.getInputStream;
import static org.jboss.as.server.deployment.DeploymentHandlerUtils.hasValidContentAdditionParameterDefined;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.dmr.ModelNode;

/**
 * Handles addition of a deployment to the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentAddHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = ADD;

    protected final ContentRepository contentRepository;

    private final AbstractVaultReader vaultReader;

    protected DeploymentAddHandler(final ContentRepository contentRepository, final AbstractVaultReader vaultReader) {
        assert contentRepository != null : "Null contentRepository";
        this.contentRepository = contentRepository;
        this.vaultReader = vaultReader;
    }

    public static DeploymentAddHandler create(final ContentRepository contentRepository, final AbstractVaultReader vaultReader) {
        return new DeploymentAddHandler(contentRepository, vaultReader);
    }

    /**
     * {@inheritDoc}
     */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        ModelNode newModel = resource.getModel();

        if (!operation.hasDefined(PERSISTENT.getName())) {
            operation.get(PERSISTENT.getName()).set(true);
        }

        //Persistent is hidden from CLI users so let's set this to true here if it is not defined
        PERSISTENT.validateAndSet(operation, newModel);

        for (AttributeDefinition def : SERVER_ADD_ATTRIBUTES) {
            if (!def.getName().equals(CONTENT_ALL.getName())) {
                def.validateAndSet(operation, newModel);
            } else {
                //Handle content a bit differently to avoid two copies of the deployment content if bytes was used
                def.validateOperation(operation);
            }
        }

        // TODO: JBAS-9020: for the moment overlays are not supported, so there is a single content item
        ModelNode content = operation.require(CONTENT_ALL.getName());
        ModelNode contentItemNode = content.require(0);

        final ModelNode opAddr = operation.get(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();
        final String runtimeName = operation.hasDefined(RUNTIME_NAME.getName()) ? operation.get(RUNTIME_NAME.getName()).asString() : name;
        newModel.get(RUNTIME_NAME.getName()).set(runtimeName);

        final DeploymentHandlerUtil.ContentItem contentItem;
        if (contentItemNode.hasDefined(CONTENT_HASH.getName())) {
            byte[] hash = contentItemNode.require(CONTENT_HASH.getName()).asBytes();
            contentItem = addFromHash(hash, name, context);
        } else if (hasValidContentAdditionParameterDefined(contentItemNode)) {
            contentItem = addFromContentAdditionParameter(context, contentItemNode);
            contentItemNode = new ModelNode();
            contentItemNode.get(CONTENT_HASH.getName()).set(contentItem.getHash());
            content = new ModelNode();
            content.add(contentItemNode);
        } else {
            contentItem = addUnmanaged(contentItemNode);
        }

        newModel.get(CONTENT_ALL.getName()).set(content);

        if (ENABLED.resolveModelAttribute(context, newModel).asBoolean() && context.isNormalServer()) {
            String policy = POLICY.resolveModelAttribute(context, newModel).asString();
            DeploymentHandlerUtil.deploy(context, runtimeName, name, policy, vaultReader, contentItem);
        }

        context.stepCompleted();
    }

    DeploymentHandlerUtil.ContentItem addFromHash(byte[] hash, String deploymentName, OperationContext context) throws OperationFailedException {
        if (!contentRepository.syncContent(hash)) {
            if (context.isBooting()) {
                if (context.getRunningMode() == RunningMode.ADMIN_ONLY) {
                    // The deployment content is missing, which would be a fatal boot error if we were going to actually
                    // install services. In ADMIN-ONLY mode we allow it to give the admin a chance to correct the problem
                    ServerLogger.DEPLOYMENT_LOGGER.reportAdminOnlyMissingDeploymentContent(HashUtil.bytesToHexString(hash), deploymentName);

                } else {
                    throw ServerMessages.MESSAGES.noSuchDeploymentContentAtBoot(HashUtil.bytesToHexString(hash), deploymentName);
                }
            } else {
                throw ServerMessages.MESSAGES.noSuchDeploymentContent(HashUtil.bytesToHexString(hash));
            }
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
        return new DeploymentHandlerUtil.ContentItem(hash);
    }

    DeploymentHandlerUtil.ContentItem addUnmanaged(ModelNode contentItemNode) throws OperationFailedException {
        final String path = contentItemNode.require(CONTENT_PATH.getName()).asString();
        final String relativeTo = asString(contentItemNode, CONTENT_RELATIVE_TO.getName());
        final boolean archive = contentItemNode.require(CONTENT_ARCHIVE.getName()).asBoolean();
        return new DeploymentHandlerUtil.ContentItem(path, relativeTo, archive);
    }

}
