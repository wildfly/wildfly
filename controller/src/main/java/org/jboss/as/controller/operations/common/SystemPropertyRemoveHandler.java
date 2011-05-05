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
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Handler for system property remove operations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SystemPropertyRemoveHandler implements ModelRemoveOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    public static final SystemPropertyRemoveHandler INSTANCE = new SystemPropertyRemoveHandler();

    public static ModelNode getOperation(ModelNode address, String name) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        op.get(NAME).set(name);
        return op;
    }

    /**
     * Create the SystemPropertyRemoveHandler
     */
    private SystemPropertyRemoveHandler() {
    }

    /**
     * {@inheritDoc}
     */
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {

        ModelNode opAddr = operation.require(OP_ADDR);
        String name = PathAddress.pathAddress(opAddr).getLastElement().getValue();

        ModelNode toRemove = context.getSubModel();

        String value = toRemove.hasDefined(VALUE) ? toRemove.get(VALUE).asString() : null;
        Boolean boottime = toRemove.has(BOOT_TIME) ? toRemove.get(BOOT_TIME).asBoolean() : null;
        ModelNode compensating = SystemPropertyAddHandler.getOperation(operation.get(OP_ADDR), value, boottime);
        return removeSystemProperty(name, context, resultHandler, compensating);
    }

    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getRemoveSystemPropertyOperation(locale);
    }

    protected OperationResult removeSystemProperty(final String name, OperationContext context, final ResultHandler resultHandler, final ModelNode compensating) {
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    SecurityActions.clearSystemProperty(name);
                    resultHandler.handleResultComplete();
                }
            });

        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensating);
    }

}
