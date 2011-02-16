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


import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;

/**
 * Abstract superclass of handlers for the socket-binding-group resource's add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractSocketBindingGroupAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    private final ParametersValidator validator;

    /**
     * Create the AbstractSocketBindingGroupAddHandler
     */
    protected AbstractSocketBindingGroupAddHandler(final ParametersValidator validator) {
        this.validator = new ParametersValidator(validator);
        this.validator.registerValidator(DEFAULT_INTERFACE, new StringLengthValidator(1, Integer.MAX_VALUE, true, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {
        try {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            String name = address.getLastElement().getValue();
            ModelNode model = context.getSubModel();
            model.get(NAME).set(name);
            ModelNode compensating = Util.getResourceRemoveOperation(operation.get(OP_ADDR));
            String failure = validator.validate(operation);
            if (failure != null) {
                throw new OperationFailedException(new ModelNode().set(failure));
            }
            model.get(DEFAULT_INTERFACE).set(operation.get(DEFAULT_INTERFACE));
            populateModel(model, operation);
            model.get(SOCKET_BINDING);
            return installSocketBindingGroup(name, operation, context, resultHandler, compensating);
        }
        catch (Exception e) {
            throw new OperationFailedException(new ModelNode().set(e.getLocalizedMessage()));
        }
    }

    protected abstract void populateModel(ModelNode model, ModelNode operation);

    protected OperationResult installSocketBindingGroup(String name, ModelNode operation, OperationContext context, ResultHandler resultHandler, ModelNode compensatingOp) throws OperationFailedException {
        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOp);
    }

}
