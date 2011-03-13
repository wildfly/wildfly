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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.server.controller.descriptions.DeploymentDescription;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handles addition of a deployment to the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    static final ModelNode getOperation(ModelNode address, ModelNode state) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        op.get(RUNTIME_NAME).set(state.get(RUNTIME_NAME));
        op.get(HASH).set(state.get(HASH));
        op.get(START).set(state.get(START));
        return op;
    }

    private final DeploymentRepository deploymentRepository;

    private final ParametersValidator validator = new ParametersValidator();

    public DeploymentAddHandler(final DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
        this.validator.registerValidator(RUNTIME_NAME, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        this.validator.registerValidator(HASH, new ModelTypeValidator(ModelType.BYTES, true));
        this.validator.registerValidator(INPUT_STREAM_INDEX, new ModelTypeValidator(ModelType.INT, true));
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getAddDeploymentOperation(locale);
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
        if (operation.hasDefined(INPUT_STREAM_INDEX) && operation.hasDefined(HASH)) {
            throw new OperationFailedException(new ModelNode().set("Can't pass in both an input-stream-index and a hash"));
        } else if (operation.hasDefined(INPUT_STREAM_INDEX)) {
            InputStream in = getContents(context, operation);
            try {
                try {
                    hash = deploymentRepository.addDeploymentContent(in);
                } catch (IOException e) {
                    throw new OperationFailedException(new ModelNode().set(e.toString()));
                }

            } finally {
                StreamUtils.safeClose(in);
            }
        } else if (operation.hasDefined(HASH)){
            hash = operation.get(HASH).asBytes();
        } else {
            throw new OperationFailedException(new ModelNode().set("Neither an attachment nor a hash were passed in"));
        }

        if (deploymentRepository.hasDeploymentContent(hash)) {
            ModelNode subModel = context.getSubModel();
            subModel.get(NAME).set(name);
            subModel.get(RUNTIME_NAME).set(runtimeName);
            subModel.get(HASH).set(hash);
            subModel.get(START).set(operation.has(START) && operation.get(START).asBoolean()); // TODO consider starting
        }
        else {
            throw new OperationFailedException(new ModelNode().set(String.format("No deployment content with hash %s is available in the deployment content repository.", HashUtil.bytesToHexString(hash))));
        }

        resultHandler.handleResultComplete();
        return new BasicOperationResult(Util.getResourceRemoveOperation(operation.get(OP_ADDR)));
    }

    private InputStream getContents(OperationContext context, ModelNode operation) {
        int streamIndex = operation.get(INPUT_STREAM_INDEX).asInt();
        if (streamIndex > context.getInputStreams().size() - 1) {
            throw new IllegalArgumentException("Invalid " + INPUT_STREAM_INDEX + "=" + streamIndex + ", the maximum index is " + (context.getInputStreams().size() - 1));
        }

        InputStream in = context.getInputStreams().get(streamIndex);
        if (in == null) {
            throw new IllegalStateException("Null stream at index " + streamIndex);
        }
        return in;
    }
}
