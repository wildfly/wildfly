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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_REPLACE;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.controller.descriptions.DeploymentDescription;
import org.jboss.dmr.ModelNode;

/**
 * Handles replacement in the runtime of one deployment by another.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentReplaceHandler implements ModelUpdateOperationHandler, RuntimeOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "replace-deployment";

    static final ModelNode getOperation(ModelNode address) {
        return Util.getEmptyOperation(OPERATION_NAME, address);
    }

    public static final DeploymentReplaceHandler INSTANCE = new DeploymentReplaceHandler();

    private final ParametersValidator validator = new ParametersValidator();

    private DeploymentReplaceHandler() {
        this.validator.registerValidator(NAME, new StringLengthValidator(1));
        this.validator.registerValidator(TO_REPLACE, new StringLengthValidator(1));
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getDeployDeploymentOperation(locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
        try {
            String failure = validator.validate(operation);
            if (failure == null) {
                ModelNode deployments = context.getSubModel().get(DEPLOYMENT);
                String name = operation.require(NAME).asString();
                String toReplace = operation.require(TO_REPLACE).asString();

                ModelNode deployNode = has(deployments, name) ? deployments.get(name) : null;
                ModelNode replaceNode = has(deployments, toReplace) ? deployments.get(toReplace) : null;
                if (deployNode == null) {
                    failure = String.format("No deployment with name %s found", name);
                }
                else if (deployNode.get(START).asBoolean()) {
                    failure = String.format("Deployment %s is already started", toReplace);
                }
                else if (replaceNode == null) {
                    failure = String.format("No deployment with name %s found", toReplace);
                }
                if (failure == null) {
                    // Update model
                    deployNode.get(START).set(true);
                    replaceNode.get(START).set(false);

                    ModelNode compensatingOp = operation.clone();
                    compensatingOp.get(NAME).set(toReplace);
                    compensatingOp.get(TO_REPLACE).set(name);

                    DeploymentHandlerUtil.replace(deployNode, toReplace, context, resultHandler, compensatingOp);
                }
            }

            if (failure != null) {
                resultHandler.handleFailed(new ModelNode().set(failure));
            }
        }
        catch (Exception e) {
            resultHandler.handleFailed(new ModelNode().set(e.getLocalizedMessage()));
        }
        return Cancellable.NULL;
    }

    private static boolean has(ModelNode node, String child) {
        return node.has(child) && node.get(child).isDefined();
    }
}
