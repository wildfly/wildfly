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


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.SocketBindingGroupDescription;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the domain socket-binding-group resource's add-include operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SocketBindingGroupIncludeAddHandler implements ModelUpdateOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "add-include";

    public static final SocketBindingGroupIncludeAddHandler INSTANCE = new SocketBindingGroupIncludeAddHandler();

    public static ModelNode getOperation(ModelNode address, String include) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        op.get(INCLUDE).set(include);
        return op;
    }

    private final ParameterValidator typeValidator = new StringLengthValidator(1);

    /**
     * Create the SocketBindingGroupIncludeAddHandler
     */
    private SocketBindingGroupIncludeAddHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
        try {
            ModelNode param = operation.get(INCLUDE);
            ModelNode includes = context.getSubModel().get(INCLUDE);
            String failure = typeValidator.validateParameter(INCLUDE, param);
            if (failure == null) {
                includes.add(param);
                ModelNode compensating = SocketBindingGroupIncludeRemoveHandler.getOperation(operation.get(OP_ADDR), param.asString());
                resultHandler.handleResultComplete(compensating);
            }
            else {
                resultHandler.handleFailed(new ModelNode().set(failure));
            }
        }
        catch (Exception e) {
            resultHandler.handleFailed(new ModelNode().set(e.getLocalizedMessage()));
        }
        return Cancellable.NULL;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return SocketBindingGroupDescription.getAddSocketBindingGroupIncludeOperation(locale);
    }

}
