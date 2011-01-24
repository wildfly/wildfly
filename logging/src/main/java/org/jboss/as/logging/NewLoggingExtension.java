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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class NewLoggingExtension implements NewExtension {

    public static final String SUBSYSTEM_NAME = "logging";
    private static final PathElement loggersPath = PathElement.pathElement(CommonAttributes.LOGGER);
    private static final PathElement handlersPath = PathElement.pathElement(CommonAttributes.HANDLER);

    /** {@inheritDoc} */
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(NewLoggingSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, ADD_INSTANCE, NewLoggingSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler("set-root-logger", NewRootLoggerAdd.INSTANCE, NewLoggingSubsystemProviders.SET_ROOT_LOGGER, false);
        registration.registerOperationHandler("remove-root-logger", NewRootLoggerRemove.INSTANCE, NewLoggingSubsystemProviders.SET_ROOT_LOGGER, false);
        // loggers
        final ModelNodeRegistration loggers = registration.registerSubModel(loggersPath, NewLoggingSubsystemProviders.LOGGER);
        loggers.registerOperationHandler(ADD, NewLoggerAdd.INSTANCE, NewLoggingSubsystemProviders.LOGGER_ADD, false);
        loggers.registerOperationHandler(REMOVE, NewLoggerRemove.INSTANCE, NewLoggingSubsystemProviders.LOGGER_REMOVE, false);
        // handlers
        final ModelNodeRegistration handlers = registration.registerSubModel(handlersPath, NewLoggingSubsystemProviders.HANDLERS);
        handlers.registerOperationHandler(ADD, NewLoggerHandlerAdd.INSTANCE, NewLoggingSubsystemProviders.HANDLER_ADD, false);
        handlers.registerOperationHandler(REMOVE, NewLoggerHandlerRemove.INSTANCE, NewLoggingSubsystemProviders.HANDLER_REMOVE, false);
        handlers.registerOperationHandler("add-async-handler", NewAsyncHandlerAdd.INSTANCE, NewLoggingSubsystemProviders.ASYNC_HANDLER_ADD, false);
        handlers.registerOperationHandler("add-console-handler", NewConsoleHandlerAdd.INSTANCE, NewLoggingSubsystemProviders.CONSOLE_HANDLER_ADD, false);
        handlers.registerOperationHandler("add-file-handler", NewFileHandlerAdd.INSTANCE, NewLoggingSubsystemProviders.FILE_HANDLER_ADD, false);
        handlers.registerOperationHandler("add-periodic-handler", NewPeriodicFileHandlerAdd.INSTANCE, NewLoggingSubsystemProviders.PERIODIC_HANDLER_ADD, false);
        handlers.registerOperationHandler("add-size-periodic-handler", NewSizePeriodicFileHandlerAdd.INSTANCE, NewLoggingSubsystemProviders.SIZE_PERIODIC_HANDLER_ADD, false);
    }

    /** {@inheritDoc} */
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewLoggingSubsystemParser.getInstance(), NewLoggingSubsystemParser.getInstance());
    }

    private static final NewLoggingSubsystemAdd ADD_INSTANCE = new NewLoggingSubsystemAdd();

    static class NewLoggingSubsystemAdd implements ModelAddOperationHandler {

        /** {@inheritDoc} */
        public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

            final ModelNode compensatingOperation = new ModelNode();
            compensatingOperation.get(OP).set(REMOVE);
            compensatingOperation.get(OP_ADDR).set(operation.get(OP_ADDR));

            final ModelNode subModel = context.getSubModel();
            subModel.get(CommonAttributes.LOGGER).setEmptyObject();
            subModel.get(CommonAttributes.HANDLER).setEmptyObject();

            resultHandler.handleResultComplete(compensatingOperation);

            return Cancellable.NULL;
        }
    }
}
