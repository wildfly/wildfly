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
package org.jboss.as.controller.operations.common;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.SocketBindingGroupDescription;
import org.jboss.as.controller.operations.validation.InetAddressValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler for the socket-binding resource's add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SocketBindingAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getOperation(ModelNode address, ModelNode socketBinding) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        if (socketBinding.get(INTERFACE).isDefined()) {
            op.get(INTERFACE).set(socketBinding.get(INTERFACE));
        }
        op.get(PORT).set(socketBinding.get(PORT));
        if (socketBinding.get(FIXED_PORT).isDefined()) {
            op.get(FIXED_PORT).set(socketBinding.get(FIXED_PORT));
        }
        if (socketBinding.get(MULTICAST_ADDRESS).isDefined()) {
            op.get(MULTICAST_ADDRESS).set(socketBinding.get(MULTICAST_ADDRESS));
        }
        if (socketBinding.get(MULTICAST_PORT).isDefined()) {
            op.get(MULTICAST_PORT).set(socketBinding.get(MULTICAST_PORT));
        }
        return op;
    }

    public static final SocketBindingAddHandler INSTANCE = new SocketBindingAddHandler();

    private final ParametersValidator validator = new ParametersValidator();

    /**
     * Create the SocketBindingAddHandler
     */
    protected SocketBindingAddHandler() {
        validator.registerValidator(INTERFACE, new StringLengthValidator(1, Integer.MAX_VALUE, true, true));
        validator.registerValidator(PORT, new IntRangeValidator(0, 65535, false, true));
        validator.registerValidator(FIXED_PORT, new ModelTypeValidator(ModelType.BOOLEAN, true, true));
        validator.registerValidator(MULTICAST_ADDRESS, new InetAddressValidator(true, true));
        validator.registerValidator(MULTICAST_PORT, new IntRangeValidator(0, 65535, true, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {
        validator.validate(operation);

        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String name = address.getLastElement().getValue();
        ModelNode model = context.getSubModel();
        model.get(NAME).set(name);
        model.get(INTERFACE).set(operation.get(INTERFACE));
        model.get(PORT).set(operation.get(PORT));
        model.get(FIXED_PORT).set(operation.get(FIXED_PORT));
        model.get(MULTICAST_ADDRESS).set(operation.get(MULTICAST_ADDRESS));
        model.get(MULTICAST_PORT).set(operation.get(MULTICAST_PORT));

        ModelNode compensating = Util.getResourceRemoveOperation(operation.get(OP_ADDR));
        return installSocketBinding(name, operation, context, resultHandler, compensating);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return SocketBindingGroupDescription.getSocketBindingAddOperation(locale);
    }

    protected OperationResult installSocketBinding(String name, ModelNode operation, OperationContext context, ResultHandler resultHandler, ModelNode compensatingOp) throws OperationFailedException {
        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOp);
    }

}
