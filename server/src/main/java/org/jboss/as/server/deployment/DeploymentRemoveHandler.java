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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

/**
 * Handles removal of a deployment from the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentRemoveHandler implements OperationStepHandler, DescriptionProvider {

    private static final Logger log = Logger.getLogger("org.jboss.as.server.controller");

    public static final String OPERATION_NAME = REMOVE;

    private final ContentRepository contentRepository;
    private final boolean standalone;

    public DeploymentRemoveHandler(ContentRepository contentRepository, boolean standalone) {
        this.contentRepository = contentRepository;
        this.standalone = standalone;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        final List<byte[]> removedHashes = DeploymentUtils.getDeploymentHash(resource);

        final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

        context.removeResource(PathAddress.EMPTY_ADDRESS);

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    String deploymentUnitName = null;

                    boolean enabled = model.hasDefined(ENABLED) ? model.get(ENABLED).asBoolean() : true;
                    if (enabled) {
                        final ModelNode opAddr = operation.get(OP_ADDR);
                        final PathAddress address = PathAddress.pathAddress(opAddr);
                        final String name = address.getLastElement().getValue();
                        deploymentUnitName = model.hasDefined(RUNTIME_NAME) ? model.get(RUNTIME_NAME).asString() : name;
                        final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
                        context.removeService(deploymentUnitServiceName);
                        context.removeService(deploymentUnitServiceName.append("contents"));
                    }
                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        recoverServices(context, operation, model);

                        if (enabled && context.hasFailureDescription()) {
                            log.infof("Undeploy of deployment \"%s\" was rolled back with failure message %s",
                                    deploymentUnitName, context.getFailureDescription().asString());
                        } else if (enabled) {
                            log.infof("Undeploy of deployment \"%s\" was rolled back with no failure message",
                                    deploymentUnitName);
                        }
                    } else {
                        if (enabled) {
                            log.infof("Undeployed \"%s\"", deploymentUnitName);
                        }
                        if (standalone) {
                            for (byte[] hash : removedHashes) {
                                try {
                                    contentRepository.removeContent(hash);
                                } catch (Exception e) {
                                    //TODO
                                    log.infof(e, "Exception occurred removing %s", hash);
                                }
                            }
                        }
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    protected boolean requiresRuntime(OperationContext context) {
        return context.getType() == OperationContext.Type.SERVER;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ServerDescriptions.getRemoveDeploymentOperation(locale);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        boolean enabled = model.hasDefined(ENABLED) ? model.get(ENABLED).asBoolean() : true;
        if (!enabled) return;
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }
}
