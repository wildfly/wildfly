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
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.dmr.ModelNode;

/**
 * Handles changes to the value of a system property.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SystemPropertyValueWriteAttributeHandler extends WriteAttributeHandlers.WriteAttributeOperationHandler {

    public static final SystemPropertyValueWriteAttributeHandler INSTANCE = new SystemPropertyValueWriteAttributeHandler();

    private SystemPropertyValueWriteAttributeHandler() {
    }

    protected void modelChanged(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler,
                final String attributeName, final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {

        RuntimeOperationContext runtimeContext = context.getRuntimeContext();
        if (runtimeContext != null) {
            final String propertyName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
            final String propertyValue = newValue.isDefined() ? newValue.asString() : null;
            runtimeContext.setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    SecurityActions.setSystemProperty(propertyName, propertyValue);
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
    }
}
