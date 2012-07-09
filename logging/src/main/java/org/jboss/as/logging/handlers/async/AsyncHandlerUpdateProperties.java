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
import org.jboss.as.logging.handlers.HandlerUpdateProperties;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.AsyncHandler;

import java.util.Locale;

/**
 * Operation responsible for updating the properties of an async logging handler.
 *
 * @author John Bailey
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class AsyncHandlerUpdateProperties extends HandlerUpdateProperties<AsyncHandlerService> {
    public static final AsyncHandlerUpdateProperties INSTANCE = new AsyncHandlerUpdateProperties();

    public static final String OPERATION_NAME = HandlerUpdateProperties.OPERATION_NAME;

    private AsyncHandlerUpdateProperties() {
        super(OVERFLOW_ACTION, SUBHANDLERS, QUEUE_LENGTH);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, final String handlerName, final ModelNode model, final ModelNode originalModel, final AsyncHandlerService handlerService) throws OperationFailedException {
        boolean requireRestart = false;
        final ModelNode overflowAction = OVERFLOW_ACTION.resolveModelAttribute(context, model);
        if (overflowAction.isDefined()) {
            handlerService.setOverflowAction(ModelParser.parseOverflowAction(overflowAction));
        }

        final ModelNode queueLength = QUEUE_LENGTH.resolveModelAttribute(context, model);
        if (queueLength.isDefined()) {
            requireRestart = true;
        }

        // Only if not restart required
        final ModelNode subhandlers = SUBHANDLERS.resolveModelAttribute(context, model);
        if (subhandlers.isDefined()) {
            // Remove old handlers
            AsyncHandlerUnassignSubhandler.removeHandlers(SUBHANDLERS, originalModel, context, handlerName);
            // Add the new handlers
            AsyncHandlerAssignSubhandler.addHandlers(SUBHANDLERS, model, context, handlerName);
        }
        return requireRestart;
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final String handlerName, final ModelNode model, final ModelNode originalModel, final AsyncHandlerService handlerService) throws OperationFailedException {
        final ModelNode overflowAction = OVERFLOW_ACTION.resolveModelAttribute(context, originalModel);
        if (overflowAction.isDefined()) {
            handlerService.setOverflowAction(AsyncHandler.OverflowAction.valueOf(overflowAction.asString().toUpperCase(Locale.US)));
        }
    }
}
