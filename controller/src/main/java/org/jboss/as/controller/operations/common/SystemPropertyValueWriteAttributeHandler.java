/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.operations.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.dmr.ModelNode;

/**
 * Handles changes to the value of a system property.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SystemPropertyValueWriteAttributeHandler extends WriteAttributeHandlers.WriteAttributeOperationHandler {

    public static final SystemPropertyValueWriteAttributeHandler INSTANCE = new SystemPropertyValueWriteAttributeHandler(null);

    private final ProcessEnvironment processEnvironment;

    public SystemPropertyValueWriteAttributeHandler(ProcessEnvironment processEnvironment) {
        this.processEnvironment = processEnvironment;
    }

    protected void modelChanged(final OperationContext context, final ModelNode operation, final String attributeName,
                                final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {


        final String name = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
        final String value = newValue.isDefined() ? newValue.asString() : null;
        final boolean applyToRuntime = processEnvironment != null && processEnvironment.isRuntimeSystemPropertyUpdateAllowed(name, value, context.isBooting());
        final boolean reload = !applyToRuntime && context.getProcessType().isServer();

        if (applyToRuntime) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) {
                    SecurityActions.setSystemProperty(name, value);
                    if (processEnvironment != null) {
                        processEnvironment.systemPropertyUpdated(name, value);
                    }
                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        final String oldValue = currentValue.isDefined() ? currentValue.asString() : null;
                        if (oldValue != null) {
                            SecurityActions.setSystemProperty(name, oldValue);
                        } else {
                            SecurityActions.clearSystemProperty(name);
                        }
                        if (processEnvironment != null) {
                            processEnvironment.systemPropertyUpdated(name, oldValue);
                        }
                    }
                }
            }, OperationContext.Stage.RUNTIME);

        } else if (reload) {
            context.reloadRequired();
        }

        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                if (reload) {
                    context.revertReloadRequired();
                }
            }
        });
    }
}
