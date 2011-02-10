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


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the add-system-property operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SystemPropertyAddHandler implements ModelUpdateOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "add-system-property";

    public static final SystemPropertyAddHandler INSTANCE = new SystemPropertyAddHandler();

    public static ModelNode getOperation(ModelNode address, String name, String value) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        if (value == null) {
            op.get(name).set(new ModelNode());
        }
        else {
            op.get(name).set(value);
        }
        return op;
    }

    private final ParametersValidator validator = new ParametersValidator();
    /**
     * Create the SystemPropertyAddHandler
     */
    protected SystemPropertyAddHandler() {
        validator.registerValidator(NAME, new StringLengthValidator(1));
        validator.registerValidator(VALUE, new StringLengthValidator(0, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
        try {
            String failure = validator.validate(operation);
            if (failure == null) {
                String name = operation.get(NAME).asString();
                String value = operation.get(VALUE).isDefined() ? operation.get(VALUE).asString() : null;
                ModelNode node = context.getSubModel().get(SYSTEM_PROPERTIES, name);
                if (value == null) {
                    node.set(new ModelNode());
                }
                else {
                    node.set(value);
                }
                ModelNode compensating = SystemPropertyRemoveHandler.getOperation(operation.get(OP_ADDR), name);
                updateSystemProperty(name, value, context, resultHandler, compensating);
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

    protected void updateSystemProperty(String name, String value, OperationContext context,
            ResultHandler resultHandler, ModelNode compensating) {
        resultHandler.handleResultComplete(compensating);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getAddSystemPropertyOperation(locale);
    }

}
