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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_ALL;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.ENABLED;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.RUNTIME_NAME;
import static org.jboss.as.server.deployment.DeploymentHandlerUtils.getContents;

import java.util.List;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Handles removal of a deployment from the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentRemoveHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = REMOVE;

    private final ContentRepository contentRepository;
    private final AbstractVaultReader vaultReader;

    public DeploymentRemoveHandler(ContentRepository contentRepository, final AbstractVaultReader vaultReader) {
        this.contentRepository = contentRepository;
        this.vaultReader = vaultReader;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        final List<byte[]> removedHashes = DeploymentUtils.getDeploymentHash(resource);

        final Resource deployment = context.removeResource(PathAddress.EMPTY_ADDRESS);
        final ImmutableManagementResourceRegistration registration = context.getResourceRegistration();
        final ManagementResourceRegistration mutableRegistration = context.getResourceRegistrationForUpdate();
        final ModelNode model = deployment.getModel();

        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    String deploymentUnitName = null;

                    boolean enabled = ENABLED.resolveModelAttribute(context, model).asBoolean();
                    if (enabled) {
                        deploymentUnitName = RUNTIME_NAME.resolveModelAttribute(context, model).asString();
                        final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
                        context.removeService(deploymentUnitServiceName);
                        context.removeService(deploymentUnitServiceName.append("contents"));
                    }
                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        if (enabled) {
                            recoverServices(context, model, deployment, registration, mutableRegistration, vaultReader);
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

        context.stepCompleted();
    }

    private void recoverServices(OperationContext context, ModelNode model, Resource deployment,
                                   ImmutableManagementResourceRegistration registration,
                                   ManagementResourceRegistration mutableRegistration, final AbstractVaultReader vaultReader) throws OperationFailedException {
        final String name = model.require(NAME).asString();
        final String runtimeName = RUNTIME_NAME.resolveModelAttribute(context, model).asString();
        final DeploymentHandlerUtil.ContentItem[] contents = getContents(CONTENT_ALL.resolveModelAttribute(context, model));
        final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
        DeploymentHandlerUtil.doDeploy(context, runtimeName, name, verificationHandler, deployment, registration, mutableRegistration, vaultReader, contents);
    }
}
