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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.DeploymentDescription;
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
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

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
import static org.jboss.as.server.ServerMessages.MESSAGES;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.asString;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.createFailureException;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.getInputStream;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.hasValidContentAdditionParameterDefined;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.validateOnePieceOfContent;

/**
 * Handles addition of a deployment to the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentAddHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    protected final ContentRepository contentRepository;

    protected final ParametersValidator validator = new ParametersValidator();
    protected final ParametersValidator unmanagedContentValidator = new ParametersValidator();
    protected final ParametersValidator managedContentValidator = new ParametersValidator();
    private final AbstractVaultReader vaultReader;

    protected DeploymentAddHandler(final ContentRepository contentRepository, final AbstractVaultReader vaultReader) {
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
        this.vaultReader = vaultReader;
    }

    public static DeploymentAddHandler createForStandalone(final ContentRepository contentRepository, final AbstractVaultReader vaultReader) {
        return new DeploymentAddHandler(contentRepository, vaultReader);
    }

    public static DeploymentAddHandler createForDomainServer(final ContentRepository contentRepository, final DeploymentFileRepository remoteFileRepository, final AbstractVaultReader vaultReader) {
        return new DomainServerDeploymentAddHandler(contentRepository, remoteFileRepository, vaultReader);
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
            contentItem = addFromHash(hash, name, context);
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
            DeploymentHandlerUtil.deploy(context, runtimeName, name, vaultReader, contentItem);
        }

        context.completeStep();
    }

    DeploymentHandlerUtil.ContentItem addFromHash(byte[] hash, String deploymentName, OperationContext context) throws OperationFailedException {
        if (!contentRepository.hasContent(hash)) {
            if (context.isBooting()) {
                if (context.getRunningMode() == RunningMode.ADMIN_ONLY) {
                    // The deployment content is missing, which would be a fatal boot error if we were going to actually
                    // install services. In ADMIN-ONLY mode we allow it to give the admin a chance to correct the problem
                    ServerLogger.DEPLOYMENT_LOGGER.reportAdminOnlyMissingDeploymentContent(HashUtil.bytesToHexString(hash), deploymentName);

                } else {
                    throw ServerMessages.MESSAGES.noSuchDeploymentContentAtBoot(HashUtil.bytesToHexString(hash), deploymentName);
                }
            } else {
                throw ServerMessages.MESSAGES.noSuchDeploymentContent(HashUtil.bytesToHexString(hash));
            }
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

        DomainServerDeploymentAddHandler(ContentRepository contentRepository, DeploymentFileRepository remoteFileRepository, final AbstractVaultReader vaultReader) {
            super(contentRepository, vaultReader);
            assert remoteFileRepository != null : "Null remoteFileRepository";
            this.remoteFileRepository = remoteFileRepository;
        }

        @Override
        DeploymentHandlerUtil.ContentItem addFromHash(byte[] hash, String deploymentName, OperationContext context) throws OperationFailedException {
            remoteFileRepository.getDeploymentFiles(hash);
            return super.addFromHash(hash, deploymentName, context);
        }

        @Override
        DeploymentHandlerUtil.ContentItem addFromContentAdditionParameter(OperationContext context, ModelNode contentItemNode) throws OperationFailedException {
            throw MESSAGES.onlyHashAllowedForDeploymentFullReplaceInDomainServer(contentItemNode);
        }
    }
}
