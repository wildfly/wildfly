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

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FULL_REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import org.jboss.as.controller.descriptions.common.DeploymentDescription;
import org.jboss.as.controller.operations.validation.AbstractParameterValidator;
import static org.jboss.as.controller.operations.validation.ChainedParameterValidator.chain;
import org.jboss.as.controller.operations.validation.ListValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersOfValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.protocol.StreamUtils;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.CONTENT_ADDITION_PARAMETERS;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.asString;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.createFailureException;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.getInputStream;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.hasValidContentAdditionParameterDefined;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.validateOnePieceOfContent;
import org.jboss.as.server.deployment.api.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handles replacement in the runtime of one deployment by another.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentFullReplaceHandler implements NewStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = FULL_REPLACE_DEPLOYMENT;

    private final ContentRepository contentRepository;

    private final ParametersValidator validator = new ParametersValidator();
    private final ParametersValidator unmanagedContentValidator = new ParametersValidator();
    private final ParametersValidator managedContentValidator = new ParametersValidator();

    public DeploymentFullReplaceHandler(final ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
        this.validator.registerValidator(NAME, new StringLengthValidator(1, Integer.MAX_VALUE, false, false));
        this.validator.registerValidator(RUNTIME_NAME, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        // TODO: can we force enablement on replace?
        //this.validator.registerValidator(ENABLED, new ModelTypeValidator(ModelType.BOOLEAN, true));
        final ParametersValidator contentValidator = new ParametersValidator();
        // existing managed content
        contentValidator.registerValidator(HASH, new ModelTypeValidator(ModelType.BYTES, true));
        // existing unmanaged content
        contentValidator.registerValidator(ARCHIVE, new ModelTypeValidator(ModelType.BOOLEAN, true));
        contentValidator.registerValidator(PATH, new StringLengthValidator(1, true));
        contentValidator.registerValidator(RELATIVE_TO, new ModelTypeValidator(ModelType.STRING, true));
        // content additions
        contentValidator.registerValidator(INPUT_STREAM_INDEX, new ModelTypeValidator(ModelType.INT, true));
        contentValidator.registerValidator(BYTES, new ModelTypeValidator(ModelType.BYTES, true));
        contentValidator.registerValidator(URL, new StringLengthValidator(1, true));
        this.validator.registerValidator(CONTENT, chain(new ListValidator(new ParametersOfValidator(contentValidator)),
                new AbstractParameterValidator() {
                    @Override
                    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                        validateOnePieceOfContent(value);
                    }
                }));
        this.managedContentValidator.registerValidator(HASH, new ModelTypeValidator(ModelType.BYTES));
        this.unmanagedContentValidator.registerValidator(ARCHIVE, new ModelTypeValidator(ModelType.BOOLEAN));
        this.unmanagedContentValidator.registerValidator(PATH, new StringLengthValidator(1));

    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getFullReplaceDeploymentOperation(locale);
    }

    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

        validator.validate(operation);

        String name = operation.require(NAME).asString();

        ModelNode rootModel = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
        ModelNode deployments = rootModel.get(DEPLOYMENT);
        ModelNode replaceNode = deployments.hasDefined(name) ? deployments.get(name) : null;
        if (replaceNode == null) {
            throw createFailureException("No deployment with name %s found", name);
        }

        String replacedRuntimeName = replaceNode.require(RUNTIME_NAME).asString();
        String runtimeName = operation.hasDefined(RUNTIME_NAME) ? operation.get(RUNTIME_NAME).asString() : replaceNode.require(RUNTIME_NAME).asString();

        final byte[] hash;
        // clone it, so we can modify it to our own content
        final ModelNode content = operation.require(CONTENT).clone();
        // TODO: JBAS-9020: for the moment overlays are not supported, so there is a single content item
        final DeploymentHandlerUtil.ContentItem contentItem;
        final ModelNode contentItemNode = content.require(0);
        if (contentItemNode.hasDefined(HASH)) {
            managedContentValidator.validate(contentItemNode);
            hash = contentItemNode.require(HASH).asBytes();
            if (!contentRepository.hasContent(hash))
                throw createFailureException("No deployment content with hash %s is available in the deployment content repository.", HashUtil.bytesToHexString(hash));
            contentItem = new DeploymentHandlerUtil.ContentItem(hash);
        } else if (hasValidContentAdditionParameterDefined(contentItemNode)) {
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
            contentItemNode.get(HASH).set(hash);
            // TODO: remove the content addition stuff?
            contentItem = new DeploymentHandlerUtil.ContentItem(hash);
        } else {
            unmanagedContentValidator.validate(contentItemNode);
            final String path = contentItemNode.require(PATH).asString();
            final String relativeTo = asString(contentItemNode, RELATIVE_TO);
            final boolean archive = contentItemNode.require(ARCHIVE).asBoolean();
            contentItem = new DeploymentHandlerUtil.ContentItem(path, relativeTo, archive);
        }

        boolean start = replaceNode.get(ENABLED).asBoolean();

        ModelNode deployNode = new ModelNode();
        deployNode.get(NAME).set(name);
        deployNode.get(RUNTIME_NAME).set(runtimeName);
        deployNode.get(CONTENT).set(content);
        deployNode.get(ENABLED).set(start);

        deployments.get(name).set(deployNode);

        // the content repo will already have these, note that content should not be empty
        removeContentAdditions(replaceNode.require(CONTENT));

        if (start) {
            DeploymentHandlerUtil.replace(context, runtimeName, name, replacedRuntimeName, contentItem);
        }
        context.completeStep();
    }

    private static void removeAttributes(final ModelNode node, final Iterable<String> attributeNames) {
        for (final String attributeName : attributeNames) {
            node.remove(attributeName);
        }
    }

    private static void removeContentAdditions(final ModelNode content) {
        for (final ModelNode contentItem : content.asList()) {
            removeAttributes(contentItem, CONTENT_ADDITION_PARAMETERS);
        }
    }
}
