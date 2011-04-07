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
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Handler for the server remove-system-property operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SystemPropertyRemoveHandler extends org.jboss.as.controller.operations.common.SystemPropertyRemoveHandler {


    public static final SystemPropertyRemoveHandler INSTANCE = new SystemPropertyRemoveHandler();

    public static ModelNode getOperation(ModelNode address, String name) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        op.get(NAME).set(name);
        return op;
    }

    private final ParameterValidator typeValidator = new StringLengthValidator(1);

    /**
     * Create the SystemPropertyRemoveHandler
     */
    protected SystemPropertyRemoveHandler() {
    }

    /**
     * {@inheritDoc}
     */
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {

        ModelNode param = operation.get(NAME);
        typeValidator.validateParameter(NAME, param);

        ModelNode properties = context.getSubModel().get(SYSTEM_PROPERTIES);
        ModelNode toRemove = null;
        ModelNode newMap = new ModelNode().setEmptyObject();
        String name = param.asString();
        if (properties.isDefined()) {
            for (Property property : properties.asPropertyList()) {
                if (!name.equals(property.getName())) {
                    toRemove = newMap.get(property.getName()).set(property.getValue());
                }
                else {
                    toRemove = property.getValue();
                }
            }
        }

        if (toRemove != null) {
            properties.set(newMap);
            String value = toRemove.get(VALUE).isDefined() ? toRemove.asString() : null;
            boolean boottime = toRemove.get(BOOT_TIME).asBoolean();
            ModelNode compensating = SystemPropertyAddHandler.getOperation(operation.get(OP_ADDR), name, value, boottime);
            return removeSystemProperty(name, context, resultHandler, compensating);
        }
        else {
            throw new OperationFailedException(new ModelNode().set("No property with " + name + "found"));
        }
    }

    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getRemoveSystemPropertyOperation(locale);
    }

    protected OperationResult removeSystemProperty(final String name, OperationContext context, final ResultHandler resultHandler, final ModelNode compensating) {
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    System.clearProperty(name);
                    resultHandler.handleResultComplete();
                }
            });

        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensating);
    }
}
