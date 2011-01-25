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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_REPLACE;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.controller.descriptions.DeploymentDescription;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handles replacement in the runtime of one deployment by another.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentFullReplaceHandler implements ModelUpdateOperationHandler, RuntimeOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "full-replace-deployment";

    static final ModelNode getOperation(ModelNode address) {
        return Util.getEmptyOperation(OPERATION_NAME, address);
    }

    private final DeploymentRepository deploymentRepository;

    private final ParametersValidator validator = new ParametersValidator();

    public DeploymentFullReplaceHandler(final DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
        this.validator.registerValidator(NAME, new StringLengthValidator(1, Integer.MAX_VALUE, false, false));
        this.validator.registerValidator(RUNTIME_NAME, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        this.validator.registerValidator(HASH, new ModelTypeValidator(ModelType.BYTES));
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
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
        try {
            byte[] hash = null;
            String failure = validator.validate(operation);
            if (failure == null) {
                hash = operation.get(HASH).asBytes();
                if (!deploymentRepository.hasDeploymentContent(hash)) {
                    failure = String.format("No deployment content with hash %s is available in the deployment content repository.", HashUtil.bytesToHexString(hash));
                }
            }
            if (failure == null) {
                ModelNode rootModel = context.getSubModel();
                ModelNode deployments = rootModel.get(DEPLOYMENT);
                String name = operation.require(NAME).asString();
                String toReplace = operation.require(TO_REPLACE).asString();

                ModelNode replaceNode = has(deployments, toReplace) ? deployments.get(toReplace) : null;
                if (has(deployments, name)) {
                    failure = String.format("Deployment with name %s already exists", name);
                }
                else if (replaceNode == null) {
                    failure = String.format("No deployment with name %s found", toReplace);
                }
                if (failure == null) {
                    boolean start = replaceNode.get(START).asBoolean();
                    String runtimeName = has(operation, RUNTIME_NAME) ? operation.get(RUNTIME_NAME).asString() : name;
                    ModelNode deployNode = new ModelNode();
                    deployNode.get(NAME).set(name);
                    deployNode.get(RUNTIME_NAME).set(runtimeName);
                    deployNode.get(HASH).set(hash);
                    deployNode.get(START).set(start);

                    deployments.get(name).set(deployNode);
                    deployments.remove(toReplace);

                    ModelNode compensatingOp = operation.clone();
                    compensatingOp.get(NAME).set(toReplace);
                    compensatingOp.get(RUNTIME_NAME).set(replaceNode.get(RUNTIME_NAME));
                    compensatingOp.get(HASH).set(replaceNode.get(HASH));
                    compensatingOp.get(START).set(start);
                    compensatingOp.get(TO_REPLACE).set(name);

                    if (start) {
                        DeploymentHandlerUtil.replace(deployNode, toReplace, context, resultHandler, compensatingOp);
                    }
                    else {
                        resultHandler.handleResultComplete(compensatingOp);
                    }
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
