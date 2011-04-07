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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler for the server add-system-property operation
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class SystemPropertyAddHandler extends org.jboss.as.controller.operations.common.SystemPropertyAddHandler {

    public static final SystemPropertyAddHandler INSTANCE = new SystemPropertyAddHandler();

    public static ModelNode getOperation(ModelNode address, String name, String value, boolean boottime) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        op.get(NAME).set(name);
        if (value == null) {
            op.get(VALUE).set(new ModelNode());
        }
        else {
            op.get(VALUE).set(value);
        }
        op.get(BOOT_TIME).set(boottime);
        return op;
    }

    private final ParametersValidator validator = new ParametersValidator();
    /**
     * Create the SystemPropertyAddHandler
     */
    protected SystemPropertyAddHandler() {
        validator.registerValidator(NAME, new StringLengthValidator(1));
        validator.registerValidator(VALUE, new StringLengthValidator(0, true));
        validator.registerValidator(BOOT_TIME, new ModelTypeValidator(ModelType.BOOLEAN, true));
    }

    /**
     * {@inheritDoc}
     */
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {
        validator.validate(operation);

        String name = operation.get(NAME).asString();
        String value = operation.get(VALUE).isDefined() ? operation.get(VALUE).asString() : null;
        boolean boottime = operation.get(BOOT_TIME).isDefined() ? operation.get(BOOT_TIME).asBoolean() : true;
        ModelNode node = context.getSubModel().get(SYSTEM_PROPERTIES, name);
        if (value == null) {
            node.get(VALUE).set(new ModelNode());
        }
        else {
            node.get(VALUE).set(value);
        }
        node.get(BOOT_TIME).set(boottime);
        ModelNode compensating = SystemPropertyRemoveHandler.getOperation(operation.get(OP_ADDR), name);
        return updateSystemProperty(name, value, context, resultHandler, compensating);
    }

    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getAddSystemPropertyOperation(locale, false);
    }

    protected OperationResult updateSystemProperty(final String name, final String value, final OperationContext context, final ResultHandler resultHandler, final ModelNode compensating) {
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    System.setProperty(name, value);
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensating);
    }
}
