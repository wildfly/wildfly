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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_REPLACE;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.createFailureException;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.getContents;

import java.util.Locale;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.DeploymentDescription;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handles replacement in the runtime of one deployment by another.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentReplaceHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = REPLACE_DEPLOYMENT;

    private final ContentRepository contentRepository;
    private final ParametersValidator validator = new ParametersValidator();
    private final ParametersValidator unmanagedContentValidator = new ParametersValidator();
    private final ParametersValidator managedContentValidator = new ParametersValidator();

    public DeploymentReplaceHandler(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
        this.validator.registerValidator(NAME, new StringLengthValidator(1));
        this.validator.registerValidator(TO_REPLACE, new StringLengthValidator(1));
        this.managedContentValidator.registerValidator(HASH, new ModelTypeValidator(ModelType.BYTES));
        this.unmanagedContentValidator.registerValidator(ARCHIVE, new ModelTypeValidator(ModelType.BOOLEAN));
        this.unmanagedContentValidator.registerValidator(PATH, new StringLengthValidator(1));
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getReplaceDeploymentOperation(locale);
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validator.validate(operation);

        String name = operation.require(NAME).asString();
        String toReplace = operation.require(TO_REPLACE).asString();

        if (name.equals(toReplace)) {
            throw operationFailed(String.format("Cannot use %s with the same value for parameters %s and %s. " +
                    "Use %s to redeploy the same content or %s to replace content with a new version with the same name.",
                    OPERATION_NAME, NAME, TO_REPLACE, DeploymentRedeployHandler.OPERATION_NAME,
                    DeploymentFullReplaceHandler.OPERATION_NAME));
        }

        final PathElement deployPath = PathElement.pathElement(DEPLOYMENT, name);
        final PathElement replacePath = PathElement.pathElement(DEPLOYMENT, toReplace);

        final Resource root = context.readResource(PathAddress.EMPTY_ADDRESS);
        if (! root.hasChild(replacePath)) {
            throw operationFailed(String.format("No deployment with name %s found", toReplace));
        }

        final ModelNode replaceNode = context.readResourceForUpdate(PathAddress.pathAddress(replacePath)).getModel();
        final String replacedName = replaceNode.require(RUNTIME_NAME).asString();

        ModelNode deployNode;
        String runtimeName;
        if (!root.hasChild(deployPath)) {
            if (!operation.hasDefined(CONTENT)) {
                throw operationFailed(String.format("No deployment with name %s found", name));
            }
            // else -- the HostController handles a server group replace-deployment like an add, so we do too

            // clone it, so we can modify it to our own content
            final ModelNode content = operation.require(CONTENT).clone();
            // TODO: JBAS-9020: for the moment overlays are not supported, so there is a single content item
            final ModelNode contentItemNode = content.require(0);
            if (contentItemNode.hasDefined(HASH)) {
                managedContentValidator.validate(contentItemNode);
                byte[] hash = contentItemNode.require(HASH).asBytes();
                if (!contentRepository.hasContent(hash))
                    throw createFailureException("No deployment content with hash %s is available in the deployment content repository.", HashUtil.bytesToHexString(hash));
            } else {
                unmanagedContentValidator.validate(contentItemNode);
            }
            runtimeName = operation.hasDefined(RUNTIME_NAME) ? operation.get(RUNTIME_NAME).asString() : replacedName;

            // Create the resource
            final Resource deployResource = context.createResource(PathAddress.pathAddress(deployPath));
            deployNode = deployResource.getModel();
            deployNode.get(RUNTIME_NAME).set(runtimeName);
            deployNode.get(CONTENT).set(content);

        } else {
            deployNode = context.readResourceForUpdate(PathAddress.pathAddress(deployPath)).getModel();
            if (deployNode.get(ENABLED).asBoolean()) {
                throw operationFailed(String.format("Deployment %s is already started", toReplace));
            }
            runtimeName = deployNode.require(RUNTIME_NAME).asString();
        }

        deployNode.get(ENABLED).set(true);
        replaceNode.get(ENABLED).set(false);

        final DeploymentHandlerUtil.ContentItem[] contents = getContents(deployNode.require(CONTENT));
        DeploymentHandlerUtil.replace(context, replaceNode, runtimeName, name, replacedName, contents);

        context.completeStep();
    }

    private static OperationFailedException operationFailed(String msg) {
        return new OperationFailedException(new ModelNode().set(msg));
    }
}
