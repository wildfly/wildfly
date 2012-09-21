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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_ALL;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_HASH;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.DOMAIN_ADD_ATTRIBUTES;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.RUNTIME_NAME;

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
import org.jboss.as.domain.controller.DomainControllerLogger;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.deployment.DeploymentHandlerUtils;
import org.jboss.dmr.ModelNode;

/**
 * Handles addition of a deployment to the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentAddHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = ADD;

    private final ContentRepository contentRepository;

    /** Constructor for a slave Host Controller */
    public DeploymentAddHandler() {
        this(null);
    }

    /**
     * Constructor for a master Host Controller
     *
     * @param contentRepository the master content repository. If {@code null} this handler will function as a slave handler would.
     */
    public DeploymentAddHandler(final ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        ModelNode newModel = resource.getModel();

        for (AttributeDefinition def : DOMAIN_ADD_ATTRIBUTES) {
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


        byte[] hash = null;

        if (contentItemNode.hasDefined(CONTENT_HASH.getName())) {
            hash = contentItemNode.require(CONTENT_HASH.getName()).asBytes();
            // If we are the master, validate that we actually have this content. If we're not the master
            // we do not need the content until it's added to a server group we care about, so we defer
            // pulling it until then
            if (contentRepository != null && !contentRepository.hasContent(hash)) {
                if (context.isBooting()) {
                    if (context.getRunningMode() == RunningMode.ADMIN_ONLY) {
                        // The deployment content is missing, which would be a fatal boot error if we were going to actually
                        // install services. In ADMIN-ONLY mode we allow it to give the admin a chance to correct the problem
                        DomainControllerLogger.HOST_CONTROLLER_LOGGER.reportAdminOnlyMissingDeploymentContent(HashUtil.bytesToHexString(hash), name);
                    } else {
                        throw createFailureException(MESSAGES.noDeploymentContentWithHashAtBoot(HashUtil.bytesToHexString(hash), name));
                    }
                } else {
                    throw createFailureException(MESSAGES.noDeploymentContentWithHash(HashUtil.bytesToHexString(hash)));
                }
            }
        } else if (DeploymentHandlerUtils.hasValidContentAdditionParameterDefined(contentItemNode)) {
            if (contentRepository == null) {
                // This is a slave DC. We can't handle this operation; it should have been fixed up on the master DC
                throw createFailureException(MESSAGES.slaveCannotAcceptUploads());
            }

            InputStream in = DeploymentHandlerUtils.getInputStream(context, contentItemNode);
            try {
                try {
                    hash = contentRepository.addContent(in);
                } catch (IOException e) {
                    throw createFailureException(e.toString());
                }

            } finally {
                StreamUtils.safeClose(in);
            }
            contentItemNode = new ModelNode();
            contentItemNode.get(CONTENT_HASH.getName()).set(hash);
            content = new ModelNode();
            content.add(contentItemNode);
        } else {
        }

        newModel.get(CONTENT_ALL.getName()).set(content);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    private static OperationFailedException createFailureException(String msg) {
        return new OperationFailedException(msg);
    }
}
