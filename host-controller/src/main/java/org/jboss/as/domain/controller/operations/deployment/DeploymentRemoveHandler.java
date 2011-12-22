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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.domain.controller.DomainControllerLogger.DEPLOYMENT_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.descriptions.DomainRootDescription;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;

/**
 * Handles removal of a deployment from the model. This can be used at either the domain deployments level
 * or the server-group deployments level
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentRemoveHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    private static final String DEPLOYMENT_HASHES = "DEPLOYMENT_HASHES";

    private final ContentRepository contentRepository;

    /** Constructor for a slave Host Controller */
    public DeploymentRemoveHandler() {
        this(null);
    }

    /**
     * Constructor for a master Host Controller
     *
     * @param contentRepository the master content repository. If {@code null} this handler will function as a slave hander would.
     */
    public DeploymentRemoveHandler(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        checkCanRemove(context, operation);
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        List<byte[]> deploymentHashes = DeploymentUtils.getDeploymentHash(resource);

        //HACK since we don't seem to be able to read the original resource containing the removed elements from from the RUNTIME stage
        operation.get(OPERATION_HEADERS, DEPLOYMENT_HASHES).setEmptyList();
        for (byte[] hash : deploymentHashes) {
            operation.get(OPERATION_HEADERS, DEPLOYMENT_HASHES).add(hash);
        }

        context.removeResource(PathAddress.EMPTY_ADDRESS);

        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                if (context.completeStep() != ResultAction.ROLLBACK) {
                    for (ModelNode node : operation.get(OPERATION_HEADERS, DEPLOYMENT_HASHES).asList()) {
                        try {
                            contentRepository.removeContent(node.asBytes());
                        } catch (Exception e) {
                            DEPLOYMENT_LOGGER.debugf(e, "Exception occurred removing %s", node.asBytes());
                        }
                    }
                }
            }
        }, OperationContext.Stage.RUNTIME);
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    protected void checkCanRemove(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String deploymentName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
        final Resource root = context.getRootResource();

        if(root.hasChild(PathElement.pathElement(SERVER_GROUP))) {
            final List<String> badGroups = new ArrayList<String>();
            for(final Resource.ResourceEntry entry : root.getChildren(SERVER_GROUP)) {
                if(entry.hasChild(PathElement.pathElement(DEPLOYMENT, deploymentName))) {
                    badGroups.add(entry.getName());
                }
            }

            if (badGroups.size() > 0) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.cannotRemoveDeploymentInUse(deploymentName, badGroups)));
            }
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DomainRootDescription.getDeploymentRemoveOperation(locale);
    }
}
