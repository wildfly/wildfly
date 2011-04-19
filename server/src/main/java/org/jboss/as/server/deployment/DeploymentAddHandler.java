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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.DeploymentDescription;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

/**
 * Handles addition of a deployment to the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    static ModelNode getOperation(ModelNode address, ModelNode state) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        op.get(RUNTIME_NAME).set(state.get(RUNTIME_NAME));
        op.get(HASH).set(state.get(HASH));
        op.get(ENABLED).set(state.get(ENABLED));
        return op;
    }

    private static final List<String> VALID_DEPLOYMENT_PARAMETERS = Arrays.asList(INPUT_STREAM_INDEX, BYTES, HASH, URL);

    private final DeploymentRepository deploymentRepository;

    private final ParametersValidator validator = new ParametersValidator();

    public DeploymentAddHandler(final DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
        this.validator.registerValidator(RUNTIME_NAME, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        this.validator.registerValidator(HASH, new ModelTypeValidator(ModelType.BYTES, true));
        this.validator.registerValidator(INPUT_STREAM_INDEX, new ModelTypeValidator(ModelType.INT, true));
        this.validator.registerValidator(BYTES, new ModelTypeValidator(ModelType.BYTES, true));
        this.validator.registerValidator(URL, new StringLengthValidator(1, true));
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getAddDeploymentOperation(locale, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {

        validator.validate(operation);

        ModelNode opAddr = operation.get(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(opAddr);
        String name = address.getLastElement().getValue();
        String runtimeName = operation.hasDefined(RUNTIME_NAME) ? operation.get(RUNTIME_NAME).asString() : name;

        byte[] hash;
        if (tooManyDeploymentParametersDefined(operation)) {
            throw createFailureException("Only allowed one of the following parameters is allowed %s.", VALID_DEPLOYMENT_PARAMETERS);
        } else if (operation.hasDefined(HASH)) {
            hash = operation.get(HASH).asBytes();
        } else if (hasValidDeploymentParameterDefined(operation)) {
            InputStream in = getContents(context, operation);
            try {
                try {
                    hash = deploymentRepository.addDeploymentContent(in);
                } catch (IOException e) {
                    throw createFailureException(e.toString());
                }

            } finally {
                StreamUtils.safeClose(in);
            }
        } else {
            throw createFailureException("None of the following parameters were defined %s.", VALID_DEPLOYMENT_PARAMETERS);
        }

        boolean isResultComplete = false;
        if (deploymentRepository.hasDeploymentContent(hash)) {
            ModelNode subModel = context.getSubModel();
            subModel.get(NAME).set(name);
            subModel.get(RUNTIME_NAME).set(runtimeName);
            subModel.get(HASH).set(hash);
            subModel.get(ENABLED).set(operation.has(ENABLED) && operation.get(ENABLED).asBoolean()); // TODO consider starting
            if (context.getRuntimeContext() != null && subModel.get(ENABLED).asBoolean()) {
                DeploymentHandlerUtil.deploy(subModel, context, resultHandler);
                isResultComplete = true;
            }
        } else {
            throw createFailureException("No deployment content with hash %s is available in the deployment content repository.", HashUtil.bytesToHexString(hash));
        }
        if (!isResultComplete) {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(Util.getResourceRemoveOperation(operation.get(OP_ADDR)));
    }

    private InputStream getContents(OperationContext context, ModelNode operation) throws OperationFailedException {
        InputStream in = null;
        String message = "";
        if (operation.hasDefined(INPUT_STREAM_INDEX)) {
            int streamIndex = operation.get(INPUT_STREAM_INDEX).asInt();
            if (streamIndex > context.getInputStreams().size() - 1) {
                IllegalArgumentException e = new IllegalArgumentException("Invalid " + INPUT_STREAM_INDEX + "=" + streamIndex + ", the maximum index is " + (context.getInputStreams().size() - 1));
                throw createFailureException(e, e.getMessage());
            }
            message = "Null stream at index " + streamIndex;
            in = context.getInputStreams().get(streamIndex);
        } else if (operation.hasDefined(BYTES)) {
            message = "Invalid byte stream.";
            in = new ByteArrayInputStream(operation.get(BYTES).asBytes());
        } else if (operation.hasDefined(URL)) {
            final String urlSpec = operation.get(URL).asString();
            try {
                message = "Invalid url stream.";
                in = new URL(urlSpec).openStream();
            } catch (MalformedURLException e) {
                throw createFailureException(message);
            } catch (IOException e) {
                throw createFailureException(message);
            }
        }
        if (in == null) {
            throw createFailureException(message);
        }
        return in;
    }

    /**
     * Checks to see if a valid deployment parameter has been defined.
     *
     * @param operation the operation to check.
     *
     * @return {@code true} of the parameter is valid, otherwise {@code false}.
     */
    private boolean hasValidDeploymentParameterDefined(ModelNode operation) {
        for (String s : VALID_DEPLOYMENT_PARAMETERS) {
            if (operation.hasDefined(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if too many deployment parameters have been defined.
     *
     * @param operation the operation.
     *
     * @return {@code true} if there are too many deployment parameters, otherwise {@code false}.
     */
    private boolean tooManyDeploymentParametersDefined(ModelNode operation) {
        int count = 0;
        for (String s : VALID_DEPLOYMENT_PARAMETERS) {
            if (operation.hasDefined(s)) {
                count++;
            }
        }
        return (count > 1);
    }

    private OperationFailedException createFailureException(String format, Object... params) {
        return createFailureException(String.format(format, params));
    }

    private OperationFailedException createFailureException(Throwable cause, String format, Object... params) {
        return createFailureException(cause, String.format(format, params));
    }

    private OperationFailedException createFailureException(String msg) {
        return new OperationFailedException(new ModelNode().set(msg));
    }

    private OperationFailedException createFailureException(Throwable cause, String msg) {
        return new OperationFailedException(cause, new ModelNode().set(msg));
    }
}
