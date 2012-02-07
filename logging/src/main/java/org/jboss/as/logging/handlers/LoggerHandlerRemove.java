/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging.handlers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.Tools;
import org.jboss.as.logging.CommonAttributes;
import org.jboss.as.logging.LoggingExtension;
import org.jboss.as.logging.LoggingMessages;
import org.jboss.as.logging.handlers.async.AsyncHandlerAdd;
import org.jboss.as.logging.handlers.console.ConsoleHandlerAdd;
import org.jboss.as.logging.handlers.custom.CustomHandlerAdd;
import org.jboss.as.logging.handlers.file.FileHandlerAdd;
import org.jboss.as.logging.handlers.file.PeriodicRotatingFileHandlerAdd;
import org.jboss.as.logging.handlers.file.SizeRotatingFileHandlerAdd;
import org.jboss.as.logging.util.LogServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public abstract class LoggerHandlerRemove extends AbstractRemoveStepHandler {

    public static final LoggerHandlerRemove ASYNC = new LoggerHandlerRemove() {
        @Override
        protected void recoverService(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers) throws OperationFailedException {
            AsyncHandlerAdd.INSTANCE.performRuntime(context, operation, model, verificationHandler, controllers);
        }
    };

    public static final LoggerHandlerRemove CONSOLE = new LoggerHandlerRemove() {
        @Override
        protected void recoverService(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers) throws OperationFailedException {
            ConsoleHandlerAdd.INSTANCE.performRuntime(context, operation, model, verificationHandler, controllers);
        }
    };

    public static final LoggerHandlerRemove CUSTOM = new LoggerHandlerRemove() {
        @Override
        protected void recoverService(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers) throws OperationFailedException {
            CustomHandlerAdd.INSTANCE.performRuntime(context, operation, model, verificationHandler, controllers);
        }
    };

    public static final LoggerHandlerRemove FILE = new LoggerFileHandlerRemove() {
        @Override
        protected void recoverService(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers) throws OperationFailedException {
            FileHandlerAdd.INSTANCE.performRuntime(context, operation, model, verificationHandler, controllers);
        }
    };

    public static final LoggerHandlerRemove PERIODIC_ROTATING_FILE = new LoggerFileHandlerRemove() {
        @Override
        protected void recoverService(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers) throws OperationFailedException {
            PeriodicRotatingFileHandlerAdd.INSTANCE.performRuntime(context, operation, model, verificationHandler, controllers);
        }
    };

    public static final LoggerHandlerRemove SIZE_ROTATING_FILE = new LoggerFileHandlerRemove() {
        @Override
        protected void recoverService(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers) throws OperationFailedException {
            SizeRotatingFileHandlerAdd.INSTANCE.performRuntime(context, operation, model, verificationHandler, controllers);
        }
    };

    @Override
    protected final void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        // Make sure the handler isn't attached
        checkHandler(context, name);
        final ServiceName serviceName = LogServices.handlerName(name);
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
        @SuppressWarnings("unchecked")
        final ServiceController<Handler> controller = (ServiceController<Handler>) serviceRegistry.getService(serviceName);
        controller.getValue().close();
        context.removeService(serviceName);
        removeAdditionalServices(context, name);
    }

    @Override
    protected final void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
        final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
        recoverService(context, operation, model, verificationHandler, controllers);
    }

    protected abstract void recoverService(final OperationContext context, final ModelNode operation, final ModelNode model,
                                           final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers)
            throws OperationFailedException;

    /**
     * Removes any additional services.
     *
     * @param context the operation context.
     * @param name    the handler name
     */
    protected void removeAdditionalServices(final OperationContext context, final String name) {

    }

    /**
     * Checks that the handler is not attached to any loggers or {@link org.jboss.logmanager.handlers.AsyncHandler
     * AsynHandler's}.
     *
     * @param context     the context used to find the attached handlers.
     * @param handlerName the name of the handler.
     *
     * @throws OperationFailedException if the handler is attached to a logger or{@link org.jboss.logmanager.handlers.AsyncHandler}.
     */
    static void checkHandler(final OperationContext context, final String handlerName) throws OperationFailedException {
        final Resource root = context.getRootResource();
        final ModelNode rootNode = Tools.readModel(root);
        final ModelNode subsystem = rootNode.get(SUBSYSTEM, LoggingExtension.SUBSYSTEM_NAME);

        final List<String> attached = new ArrayList<String>();

        // Check the root logger
        final ModelNode rootLogger = subsystem.get(CommonAttributes.ROOT_LOGGER, CommonAttributes.ROOT_LOGGER_NAME);
        if (rootLogger.isDefined() && rootLogger.hasDefined(CommonAttributes.HANDLERS.getName())) {
            final ModelNode handlers = rootLogger.get(CommonAttributes.HANDLERS.getName());
            for (ModelNode handler : handlers.asList()) {
                if (handlerName.equals(handler.asString())) {
                    attached.add(CommonAttributes.ROOT_LOGGER_NAME);
                }
            }
        }

        // Check the loggers
        final ModelNode loggers = subsystem.get(CommonAttributes.LOGGER);
        for (Property logger : loggers.asPropertyList()) {
            if (logger.getValue().hasDefined(CommonAttributes.HANDLERS.getName())) {
                final ModelNode handlers = logger.getValue().get(CommonAttributes.HANDLERS.getName());
                for (ModelNode handler : handlers.asList()) {
                    if (handlerName.equals(handler.asString())) {
                        attached.add(logger.getName());
                    }
                }
            }
        }

        if (!attached.isEmpty()) {
            throw new OperationFailedException(LoggingMessages.MESSAGES.handlerAttachedToLoggers(handlerName, attached));
        }

        // Check Async handlers
        final ModelNode asyncHandlers = subsystem.get(CommonAttributes.ASYNC_HANDLER);
        for (Property asyncHandler : asyncHandlers.asPropertyList()) {
            if (asyncHandler.getValue().hasDefined(CommonAttributes.SUBHANDLERS.getName())) {
                final ModelNode subhandlers = asyncHandler.getValue().get(CommonAttributes.SUBHANDLERS.getName());
                for (ModelNode handler : subhandlers.asList()) {
                    if (handlerName.equals(handler.asString())) {
                        attached.add(asyncHandler.getName());
                    }
                }
            }
        }

        if (!attached.isEmpty()) {
            throw new OperationFailedException(LoggingMessages.MESSAGES.handlerAttachedToHandlers(handlerName, attached));
        }
    }



    private abstract static class LoggerFileHandlerRemove extends LoggerHandlerRemove {


        @Override
        protected void removeAdditionalServices(final OperationContext context, final String name) {
            context.removeService(LogServices.handlerFileName(name));
        }
    }

}
