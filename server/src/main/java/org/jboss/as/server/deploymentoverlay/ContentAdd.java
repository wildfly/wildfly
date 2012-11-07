/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.server.deploymentoverlay;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.CompositeOperationAwareTransformer;
import org.jboss.as.controller.operations.DomainOperationTransformer;
import org.jboss.as.controller.operations.OperationAttachments;
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
import org.jboss.as.server.deploymentoverlay.service.ContentService;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import static org.jboss.as.controller.operations.validation.ChainedParameterValidator.chain;

/**
 * @author Stuart Douglas
 */
public class ContentAdd extends AbstractAddStepHandler {

    protected final ContentRepository contentRepository;
    private final DeploymentFileRepository remoteRepository;

    protected final ParametersValidator validator = new ParametersValidator();
    protected final ParametersValidator managedContentValidator = new ParametersValidator();

    public ContentAdd(final ContentRepository contentRepository, final DeploymentFileRepository remoteRepository) {
        this.contentRepository = contentRepository;
        this.remoteRepository = remoteRepository;
        this.validator.registerValidator(RUNTIME_NAME, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        this.validator.registerValidator(ENABLED, new ModelTypeValidator(ModelType.BOOLEAN, true));
        final ParametersValidator contentValidator = new ParametersValidator();
        // existing managed content
        contentValidator.registerValidator(HASH, new ModelTypeValidator(ModelType.BYTES, true));
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
    }

    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String path = address.getLastElement().getValue();
        final String name = address.getElement(address.size() - 2).getValue();
        final ModelNode content = operation.get(CONTENT);
        final byte[] hash;
        if (content.hasDefined(HASH)) {
            managedContentValidator.validate(content);
            hash = content.require(HASH).asBytes();
            addFromHash(hash, name, path, context);
        } else {
            hash = addFromContentAdditionParameter(context, content);

            final ModelNode slave = operation.clone();
            slave.get(CONTENT).clear();
            slave.get(CONTENT).get(HASH).set(hash);

            List<DomainOperationTransformer> transformers = context.getAttachment(OperationAttachments.SLAVE_SERVER_OPERATION_TRANSFORMERS);
            if(transformers == null) {
                context.attach(OperationAttachments.SLAVE_SERVER_OPERATION_TRANSFORMERS, transformers = new ArrayList<DomainOperationTransformer>());
            }
            transformers.add(new CompositeOperationAwareTransformer(slave));
        }



        ModelNode modified = operation.clone();
        modified.get(CONTENT).clone();
        modified.get(CONTENT).set(hash);
        for (AttributeDefinition attr : ContentDefinition.attributes()) {
            attr.validateAndSet(modified, resource.getModel());
        }
        if (!contentRepository.syncContent(hash)) {
            throw ServerMessages.MESSAGES.noSuchDeploymentContent(Arrays.toString(hash));
        }
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {

    }


    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String path = address.getLastElement().getValue();
        final String name = address.getElement(address.size() - 2).getValue();
        final byte[] content = model.get(ModelDescriptionConstants.CONTENT).asBytes();

        installServices(context, verificationHandler, newControllers, name, path, content);

    }

    static void installServices(final OperationContext context, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers, final String name, final String path, final byte[] content) {
        final ServiceName serviceName = ContentService.SERVICE_NAME.append(name).append(path);
        final ContentService service = new ContentService(path, content);

        ServiceBuilder<ContentService> builder = context.getServiceTarget().addService(serviceName, service)
                .addDependency(DeploymentOverlayService.SERVICE_NAME.append(name), DeploymentOverlayService.class, service.getDeploymentOverlayServiceInjectedValue())
                .addDependency(ContentRepository.SERVICE_NAME, ContentRepository.class, service.getContentRepositoryInjectedValue());
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        final ServiceController<ContentService> controller = builder.install();
        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    protected static void validateOnePieceOfContent(final ModelNode content) throws OperationFailedException {
        if (content.asList().size() != 1)
            throw ServerMessages.MESSAGES.multipleContentItemsNotSupported();
    }

    byte[] addFromHash(byte[] hash, String deploymentOverlayName, final String contentName, final OperationContext context) throws OperationFailedException {
        if(remoteRepository != null) {
            remoteRepository.getDeploymentFiles(hash);
        }
        if (!contentRepository.syncContent(hash)) {
            if (context.isBooting()) {
                if (context.getRunningMode() == RunningMode.ADMIN_ONLY) {
                    // The deployment content is missing, which would be a fatal boot error if we were going to actually
                    // install services. In ADMIN-ONLY mode we allow it to give the admin a chance to correct the problem
                    ServerLogger.DEPLOYMENT_LOGGER.reportAdminOnlyMissingDeploymentOverlayContent(HashUtil.bytesToHexString(hash), deploymentOverlayName, contentName);

                } else {
                    throw ServerMessages.MESSAGES.noSuchDeploymentOverlayContentAtBoot(HashUtil.bytesToHexString(hash), deploymentOverlayName, contentName);
                }
            } else {
                throw ServerMessages.MESSAGES.noSuchDeploymentOverlayContent(HashUtil.bytesToHexString(hash));
            }
        }
        return hash;
    }

    byte[] addFromContentAdditionParameter(OperationContext context, ModelNode contentItemNode) throws OperationFailedException {
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
        return hash;
    }

    protected static OperationFailedException createFailureException(String msg) {
        return new OperationFailedException(new ModelNode(msg));
    }

    protected static InputStream getInputStream(OperationContext context, ModelNode operation) throws OperationFailedException {
        InputStream in = null;
        if (operation.hasDefined(INPUT_STREAM_INDEX)) {
            int streamIndex = operation.get(INPUT_STREAM_INDEX).asInt();
            int maxIndex = context.getAttachmentStreamCount();
            if (streamIndex > maxIndex) {
                throw ServerMessages.MESSAGES.invalidStreamIndex(INPUT_STREAM_INDEX, streamIndex, maxIndex);
            }
            in = context.getAttachmentStream(streamIndex);
        } else if (operation.hasDefined(BYTES)) {
            try {
                in = new ByteArrayInputStream(operation.get(BYTES).asBytes());
            } catch (IllegalArgumentException iae) {
                throw ServerMessages.MESSAGES.invalidStreamBytes(BYTES);
            }
        } else if (operation.hasDefined(URL)) {
            final String urlSpec = operation.get(URL).asString();
            try {
                in = new java.net.URL(urlSpec).openStream();
            } catch (MalformedURLException e) {
                throw ServerMessages.MESSAGES.invalidStreamURL(e, urlSpec);
            } catch (IOException e) {
                throw ServerMessages.MESSAGES.invalidStreamURL(e, urlSpec);
            }
        }
        if (in == null) {
            // Won't happen, as we call hasValidContentAdditionParameterDefined first
            throw new IllegalStateException();
        }
        return in;
    }
}
