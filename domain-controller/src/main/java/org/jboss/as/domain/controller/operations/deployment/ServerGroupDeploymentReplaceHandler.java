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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_REPLACE;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.DeploymentDescription;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.dmr.ModelNode;

/**
 * Handles replacement in the runtime of one deployment by another.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerGroupDeploymentReplaceHandler implements ModelUpdateOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = REPLACE_DEPLOYMENT;

    static final ModelNode getOperation(ModelNode address) {
        return Util.getEmptyOperation(OPERATION_NAME, address);
    }

    private final FileRepository fileRepository;
    private final ParametersValidator validator = new ParametersValidator();

    public ServerGroupDeploymentReplaceHandler(final FileRepository fileRepository) {
        if (fileRepository == null) {
            throw new IllegalArgumentException("fileRepository is null");
        }
        this.fileRepository = fileRepository;
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
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {

        validator.validate(operation);

        String name = operation.require(NAME).asString();
        String toReplace = operation.require(TO_REPLACE).asString();

        if (name.equals(toReplace)) {
            throw operationFailed(String.format("Cannot use %s with the same value for parameters %s and %s. " +
                    "Use %s to redeploy the same content or %s to replace content with a new version with the same name.",
                    OPERATION_NAME, NAME, TO_REPLACE, ServerGroupDeploymentRedeployHandler.OPERATION_NAME,
                    DeploymentFullReplaceHandler.OPERATION_NAME));
        }

        ModelNode deployment;
        try {
            deployment = context.getSubModel(PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, name)));
        }
        catch (IllegalArgumentException iae) {
            throw operationFailed(String.format("No deployment with name %s found", name));
        }


        for (ModelNode content : deployment.require(CONTENT).asList()) {
            if ((content.hasDefined(HASH))) {
                byte[] hash = content.require(HASH).asBytes();
                // Ensure the local repo has the files
                fileRepository.getDeploymentFiles(hash);
            }
        }

        ModelNode deployments = context.getSubModel().get(DEPLOYMENT);
        ModelNode replaceNode = deployments.hasDefined(toReplace) ? deployments.get(toReplace) : null;
        if (replaceNode == null) {
            throw operationFailed(String.format("No deployment with name %s found", toReplace));
        }
        ModelNode deployNode = deployments.hasDefined(name) ? deployments.get(name) : null;
        if (deployNode == null) {
            deployNode = new ModelNode();
            deployNode = deployment.clone();
            deployNode.remove("content");
            deployments.get(name).set(deployNode);
        }
        else if (deployNode.get(ENABLED).asBoolean()) {
            throw operationFailed(String.format("Deployment %s is already started", toReplace));
        }

        // Update model
        deployNode.get(ENABLED).set(true);
        replaceNode.get(ENABLED).set(false);

        ModelNode compensatingOp = operation.clone();
        compensatingOp.get(NAME).set(toReplace);
        compensatingOp.get(TO_REPLACE).set(name);

        resultHandler.handleResultComplete();

        return new BasicOperationResult();
    }

    private static OperationFailedException operationFailed(String msg) {
        return new OperationFailedException(new ModelNode().set(msg));
    }
}
