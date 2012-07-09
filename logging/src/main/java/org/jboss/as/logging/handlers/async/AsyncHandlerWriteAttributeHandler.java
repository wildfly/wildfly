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

package org.jboss.as.logging.handlers.async;

import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.handlers.AbstractLogHandlerWriteAttributeHandler;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;

/**
 * Date: 12.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class AsyncHandlerWriteAttributeHandler extends AbstractLogHandlerWriteAttributeHandler<AsyncHandlerService> {

    public static final AsyncHandlerWriteAttributeHandler INSTANCE = new AsyncHandlerWriteAttributeHandler();

    private AsyncHandlerWriteAttributeHandler() {
        super(OVERFLOW_ACTION, SUBHANDLERS, QUEUE_LENGTH);
    }

    @Override
    protected boolean doApplyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final String handlerName, final AsyncHandlerService handlerService) throws OperationFailedException {
        if (OVERFLOW_ACTION.getName().equals(attributeName)) {
            handlerService.setOverflowAction(ModelParser.parseOverflowAction(resolvedValue));
        } else if (SUBHANDLERS.getName().equals(attributeName)) {
            // Remove the subhandlers
            AsyncHandlerUnassignSubhandler.removeHandlers(SUBHANDLERS, currentValue, context, handlerName);
            // Add the new handlers
            AsyncHandlerAssignSubhandler.addHandlers(SUBHANDLERS, resolvedValue, context, handlerName);
        } else if (QUEUE_LENGTH.getName().equals(attributeName)) {
            return true;
        }
        return false;
    }

    @Override
    protected void doRevertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final String handlerName, final AsyncHandlerService handlerService) throws OperationFailedException {
        if (OVERFLOW_ACTION.getName().equals(attributeName)) {
            handlerService.setOverflowAction(ModelParser.parseOverflowAction(valueToRestore));
        } else if (SUBHANDLERS.getName().equals(attributeName)) {
            // Remove the subhandlers
            AsyncHandlerUnassignSubhandler.removeHandlers(SUBHANDLERS, valueToRevert, context, handlerName);
            // Add the new handlers
            AsyncHandlerAssignSubhandler.addHandlers(SUBHANDLERS, valueToRestore, context, handlerName);
        }
    }
}
