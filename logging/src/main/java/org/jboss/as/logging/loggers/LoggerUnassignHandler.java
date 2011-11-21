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

package org.jboss.as.logging.loggers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.logging.util.LogServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;


/**
 * Operation responsible unassigning a handler from a logger.
 *
 * @author Stan Silvert
 */
public class LoggerUnassignHandler extends AbstractLogHandlerAssignmentHandler {
    public static final String OPERATION_NAME = "unassign-handler";
    public static final LoggerUnassignHandler INSTANCE = new LoggerUnassignHandler();

    @Override
    protected void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        updateHandlersForUnassign(HANDLERS, operation, model);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        removeHandler(context, getLoggerName(operation), getHandlerName(operation));
    }

    @Override
    protected String getHandlerName(ModelNode operation) throws OperationFailedException {
        return NAME.validateOperation(operation).asString();
    }

    protected String getLoggerName(final ModelNode operation) {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        return address.getLastElement().getValue();
    }

    /**
     * Removes the handler, represented by the {@code handlerName} parameter, from the logger.
     *
     * @param context     the context of the operation.
     * @param loggerName  the logger name.
     * @param handlerName the name of the handler to remove.
     *
     * @throws OperationFailedException if an error occurs.
     */
    public static void removeHandler(final OperationContext context, final String loggerName, final String handlerName) throws OperationFailedException {
        // Verify the logger exists
        if (context.getServiceRegistry(false).getService(LogServices.loggerName(loggerName)) == null) {
            throw createFailureMessage(MESSAGES.loggerNotFound(loggerName));
        }
        context.removeService(LogServices.loggerHandlerName(loggerName, handlerName));
    }

    /**
     * Removes the handlers to the logger.
     *
     * @param attribute  the attribute definition.
     * @param node       the model node to extract the handlers from.
     * @param context    the context of the operation.
     * @param loggerName the name of the logger.
     *
     * @throws OperationFailedException if an error occurs.
     */
    public static void removeHandlers(final AttributeDefinition attribute, final ModelNode node, final OperationContext context,
                                      final String loggerName) throws OperationFailedException {
        // Verify the logger exists
        if (context.getServiceRegistry(false).getService(LogServices.loggerName(loggerName)) == null) {
            throw createFailureMessage(MESSAGES.loggerNotFound(loggerName));
        }
        final ModelNode handlers = attribute.resolveModelAttribute(context, node);
        if (handlers.isDefined()) {
            if (handlers.getType() == ModelType.LIST) {
                for (ModelNode handler : handlers.asList()) {
                    removeHandler(context, loggerName, handler.asString());
                }
            }
        }
    }

}
