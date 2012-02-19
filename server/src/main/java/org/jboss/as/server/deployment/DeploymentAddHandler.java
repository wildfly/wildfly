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

import static org.jboss.as.server.ServerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import static org.jboss.as.controller.operations.validation.ChainedParameterValidator.chain;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.asString;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.createFailureException;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.getInputStream;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.hasValidContentAdditionParameterDefined;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.validateOnePieceOfContent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.DeploymentDescription;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.AbstractParameterValidator;
import org.jboss.as.controller.operations.validation.ListValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersOfValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.DeploymentFileRepository;
import org.jboss.as.server.ServerMessages;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handles addition of a deployment to the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentAddHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    static ModelNode getOperation(ModelNode address, ModelNode state) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        op.get(RUNTIME_NAME).set(state.get(RUNTIME_NAME));
        op.get(CONTENT).set(state.get(CONTENT));
        if (state.hasDefined(ENABLED)) {
            op.get(ENABLED).set(state.get(ENABLED));
        }
        return op;
    }

    protected final ContentRepository contentRepository;

    protected final ParametersValidator validator = new ParametersValidator();
    protected final ParametersValidator unmanagedContentValidator = new ParametersValidator();
    protected final ParametersValidator managedContentValidator = new ParametersValidator();

    protected DeploymentAddHandler(final ContentRepository contentRepository) {
        assert contentRepository != null : "Null contentRepository";
        this.contentRepository = contentRepository;
        this.validator.registerValidator(RUNTIME_NAME, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        this.validator.registerValidator(ENABLED, new ModelTypeValidator(ModelType.BOOLEAN, true));
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

    public static DeploymentAddHandler createForStandalone(final ContentRepository contentRepository) {
        return new DeploymentAddHandler(contentRepository);
    }

    public static DeploymentAddHandler createForDomainServer(final ContentRepository contentRepository, final DeploymentFileRepository remoteFileRepository) {
        return new DomainServerDeploymentAddHandler(contentRepository, remoteFileRepository);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getAddDeploymentOperation(locale, true);
    }

    /**
     * {@inheritDoc}
     */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validator.validate(operation);

        final ModelNode opAddr = operation.get(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();
        final String runtimeName = operation.hasDefined(RUNTIME_NAME) ? operation.get(RUNTIME_NAME).asString() : name;

        // clone it, so we can modify it to our own content
        final ModelNode content = operation.require(CONTENT).clone();
        // TODO: JBAS-9020: for the moment overlays are not supported, so there is a single content item
        final DeploymentHandlerUtil.ContentItem contentItem;
        final ModelNode contentItemNode = content.require(0);
        if (contentItemNode.hasDefined(HASH)) {
            managedContentValidator.validate(contentItemNode);
            byte[] hash = contentItemNode.require(HASH).asBytes();
            contentItem = addFromHash(hash, contentItemNode);
        } else if (hasValidContentAdditionParameterDefined(contentItemNode)) {
            contentItem = addFromContentAdditionParameter(context, contentItemNode);
        } else {
            contentItem = addUnmanaged(contentItemNode);
        }

        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        ModelNode subModel = resource.getModel();
        subModel.get(NAME).set(name);
        subModel.get(RUNTIME_NAME).set(runtimeName);
        // content is a clone
        subModel.get(CONTENT).set(content);
        subModel.get(ENABLED).set(operation.has(ENABLED) && operation.get(ENABLED).asBoolean()); // TODO consider starting
        subModel.get(PERSISTENT).set(!operation.hasDefined(PERSISTENT) || operation.get(PERSISTENT).asBoolean());

        if (subModel.get(ENABLED).asBoolean() && context.isNormalServer()) {
            DeploymentHandlerUtil.deploy(context, runtimeName, name, contentItem);
        }

        context.completeStep();
    }

    DeploymentHandlerUtil.ContentItem addFromHash(byte[] hash, ModelNode contentItemNode) throws OperationFailedException {
        if (!contentRepository.hasContent(hash)) {
            throw ServerMessages.MESSAGES.noSuchDeploymentContent(HashUtil.bytesToHexString(hash));
        }
        return new DeploymentHandlerUtil.ContentItem(hash);
    }

    DeploymentHandlerUtil.ContentItem addFromContentAdditionParameter(OperationContext context, ModelNode contentItemNode) throws OperationFailedException {
        byte[] hash;
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
        contentItemNode.clear(); // AS7-1029
        contentItemNode.get(HASH).set(hash);
        // TODO: remove the content addition stuff?
        return new DeploymentHandlerUtil.ContentItem(hash);
    }

    DeploymentHandlerUtil.ContentItem addUnmanaged(ModelNode contentItemNode) throws OperationFailedException {
        unmanagedContentValidator.validate(contentItemNode);
        final String path = contentItemNode.require(PATH).asString();
        final String relativeTo = asString(contentItemNode, RELATIVE_TO);
        final boolean archive = contentItemNode.require(ARCHIVE).asBoolean();
        return new DeploymentHandlerUtil.ContentItem(path, relativeTo, archive);
    }

    private static class DomainServerDeploymentAddHandler extends DeploymentAddHandler {
        final DeploymentFileRepository remoteFileRepository;

        DomainServerDeploymentAddHandler(ContentRepository contentRepository, DeploymentFileRepository remoteFileRepository) {
            super(contentRepository);
            assert remoteFileRepository != null : "Null remoteFileRepository";
            this.remoteFileRepository = remoteFileRepository;
        }

        @Override
        DeploymentHandlerUtil.ContentItem addFromHash(byte[] hash, ModelNode contentItemNode) throws OperationFailedException {
            remoteFileRepository.getDeploymentFiles(hash);
            return super.addFromHash(hash, contentItemNode);
        }

        @Override
        DeploymentHandlerUtil.ContentItem addFromContentAdditionParameter(OperationContext context, ModelNode contentItemNode) throws OperationFailedException {
            throw MESSAGES.onlyHashAllowedForDeploymentFullReplaceInDomainServer(contentItemNode);
        }
    }
}
