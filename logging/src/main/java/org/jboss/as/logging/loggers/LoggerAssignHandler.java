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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.logging.util.LogServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;


/**
 * Operation responsible assigning a handler to a logger.
 *
 * @author Stan Silvert
 */
public class LoggerAssignHandler extends AbstractLogHandlerAssignmentHandler {
    public static final String OPERATION_NAME = "assign-handler";
    public static final LoggerAssignHandler INSTANCE = new LoggerAssignHandler();

    @Override
    protected void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        updateHandlersForAssign(HANDLERS, operation, model);
    }

    @Override
    protected String getHandlerName(ModelNode operation) throws OperationFailedException {
        return NAME.validateOperation(operation).asString();
    }

    protected String getLoggerName(ModelNode operation) {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        return address.getLastElement().getValue();
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        String loggerName = getLoggerName(operation);
        String handlerName = getHandlerName(operation);
        newControllers.add(addHandler(context, loggerName, handlerName, verificationHandler));
    }

    /**
     * Adds the handler, represented by the {@code handlerNameToAdd} parameter, to the logger.
     *
     * @param context             the context of the operation.
     * @param loggerName          the logger name.
     * @param handlerName         the name of the handler to add.
     * @param verificationHandler a verification handler for the builder, or {@code null} if no verification handler
     *                            is needed.
     *
     * @return the service that was installed.
     *
     * @throws OperationFailedException if an error occurs.
     */
    public static ServiceController<Logger> addHandler(final OperationContext context, final String loggerName, final String handlerName,
                                                       final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
        // Verify the logger exists
        if (serviceRegistry.getService(LogServices.loggerName(loggerName)) == null) {
            throw createFailureMessage(MESSAGES.loggerNotFound(loggerName));
        }
        final ServiceController<?> loggerHandlerController = serviceRegistry.getService(LogServices.loggerHandlerName(loggerName, handlerName));
        @SuppressWarnings("unchecked")
        final ServiceController<Handler> handlerController = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(handlerName));

        if (loggerHandlerController != null) {
            throw createFailureMessage(MESSAGES.handlerAlreadyDefined(handlerName));
        }

        if (handlerController == null) {
            throw createFailureMessage(MESSAGES.handlerNotFound(handlerName));
        }

        final ServiceTarget target = context.getServiceTarget();
        final LoggerHandlerService service = new LoggerHandlerService(loggerName);
        final ServiceBuilder<Logger> builder = target.addService(LogServices.loggerHandlerName(loggerName, handlerName), service);
        builder.addDependency(LogServices.loggerName(loggerName));
        builder.addDependency(LogServices.handlerName(handlerName), Handler.class, service.getHandlerInjector());
        if (verificationHandler != null)
            builder.addListener(verificationHandler);
        return builder.install();
    }

    /**
     * Adds the handlers to the logger.
     *
     * @param attribute           the attribute definition.
     * @param node                the model node to extract the handlers from.
     * @param context             the context of the operation.
     * @param loggerName          the name of the logger.
     * @param verificationHandler a verification handler for the builder, or {@code null} if no verification handler
     *                            is needed.
     *
     * @return a collection of the installed controllers.
     *
     * @throws OperationFailedException if an error occurs.
     */
    public static List<ServiceController<?>> addHandlers(final AttributeDefinition attribute, final ModelNode node, final OperationContext context,
                                                         final String loggerName, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        // Verify the logger exists
        if (context.getServiceRegistry(false).getService(LogServices.loggerName(loggerName)) == null) {
            throw createFailureMessage(MESSAGES.loggerNotFound(loggerName));
        }
        final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
        final ModelNode handlers = attribute.resolveModelAttribute(context, node);
        if (handlers.isDefined()) {
            if (handlers.getType() == ModelType.LIST) {
                for (ModelNode handler : handlers.asList()) {
                    controllers.add(addHandler(context, loggerName, handler.asString(), verificationHandler));
                }
            }
        }
        return controllers;
    }
}
