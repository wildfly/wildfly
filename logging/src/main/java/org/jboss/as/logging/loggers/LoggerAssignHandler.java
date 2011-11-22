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
import static org.jboss.as.logging.LoggingLogger.ROOT_LOGGER;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Handler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.logging.util.LogServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
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


    public static Collection<ServiceController<?>> installHandlers(final ServiceTarget serviceTarget, final String loggerName, final ModelNode handlers, final ServiceVerificationHandler verificationHandler) {
        final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
        // Install logger handler services
        for(final ModelNode handler : handlers.asList()) {
            final String handlerName = handler.asString();
            final LoggerHandlerService service = new LoggerHandlerService(loggerName);
            final Injector<Handler> injector = service.getHandlerInjector();
            final ServiceName serviceName = LogServices.loggerHandlerName(loggerName, handlerName);
            final ServiceName loggerDep = LogServices.loggerName(loggerName);
            final ServiceName handlerDep = LogServices.handlerName(handlerName);
            ROOT_LOGGER.tracef("Installing service '%s' for logger '%s' with handler '%s'", serviceName, loggerDep, handlerDep);
            controllers.add(serviceTarget.addService(serviceName, service)
                    .addDependency(loggerDep)
                    .addDependency(handlerDep, Handler.class, injector)
                    .addListener(verificationHandler)
                    .install());
        }
        return controllers;
    }

    /**
     * Adds the handlers to the logger.
     *
     * @param context             the context of the operation.
     * @param loggerName          the name of the logger.
     * @param verificationHandler a verification handler for the builder, or {@code null} if no verification handler
     *                            is needed.
     *
     * @return a collection of the installed controllers.
     *
     * @throws OperationFailedException if an error occurs.
     */
    public static List<ServiceController<?>> addHandlers(final ModelNode handlers, final OperationContext context,
                                                         final String loggerName, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
        if (handlers.isDefined()) {
            // Verify the logger exists
            if (context.getServiceRegistry(false).getService(LogServices.loggerName(loggerName)) == null) {
                throw createFailureMessage(MESSAGES.loggerNotFound(loggerName));
            }
            if (handlers.getType() == ModelType.LIST) {
                for (ModelNode handler : handlers.asList()) {
                    controllers.add(addHandler(context, loggerName, handler.asString(), verificationHandler));
                }
            }
        }
        return controllers;
    }
}
