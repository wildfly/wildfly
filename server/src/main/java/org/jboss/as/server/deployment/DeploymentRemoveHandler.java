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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.getContents;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Handles removal of a deployment from the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentRemoveHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    private final ContentRepository contentRepository;

    public DeploymentRemoveHandler(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        final List<byte[]> removedHashes = DeploymentUtils.getDeploymentHash(resource);

        final Resource deployment = context.removeResource(PathAddress.EMPTY_ADDRESS);
        final ImmutableManagementResourceRegistration registration = context.getResourceRegistration();
        final ManagementResourceRegistration mutableRegistration = context.getResourceRegistrationForUpdate();
        final ModelNode model = deployment.getModel();

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    String deploymentUnitName = null;

                    boolean enabled = !model.hasDefined(ENABLED) || model.get(ENABLED).asBoolean();
                    if (enabled) {
                        final String name = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
                        deploymentUnitName = model.hasDefined(RUNTIME_NAME) ? model.get(RUNTIME_NAME).asString() : name;
                        final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
                        context.removeService(deploymentUnitServiceName);
                        context.removeService(deploymentUnitServiceName.append("contents"));
                    }
                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        if (enabled) {
                            recoverServices(context, model, deployment, registration, mutableRegistration);
                        }

                        if (enabled && context.hasFailureDescription()) {
                            ServerLogger.ROOT_LOGGER.undeploymentRolledBack(deploymentUnitName, context.getFailureDescription().asString());
                        } else if (enabled) {
                            ServerLogger.ROOT_LOGGER.undeploymentRolledBackWithNoMessage(deploymentUnitName);
                        }
                    } else {
                        if (enabled) {
                            ServerLogger.ROOT_LOGGER.deploymentUndeployed(deploymentUnitName);
                        }

                        for (byte[] hash : removedHashes) {
                            try {
                                contentRepository.removeContent(hash);
                            } catch (Exception e) {
                                //TODO
                                ServerLogger.DEPLOYMENT_LOGGER.failedToRemoveDeploymentContent(e, HashUtil.bytesToHexString(hash));
                            }
                        }
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ServerDescriptions.getRemoveDeploymentOperation(locale);
    }

    private void recoverServices(OperationContext context, ModelNode model, Resource deployment,
                                   ImmutableManagementResourceRegistration registration,
                                   ManagementResourceRegistration mutableRegistration) {
        final String name = model.require(NAME).asString();
        final String runtimeName = model.hasDefined(RUNTIME_NAME) ? model.get(RUNTIME_NAME).asString() : name;
        final DeploymentHandlerUtil.ContentItem[] contents = getContents(model.require(CONTENT));
        final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
        DeploymentHandlerUtil.doDeploy(context, runtimeName, name, verificationHandler, deployment, registration, mutableRegistration, contents);
    }
}
