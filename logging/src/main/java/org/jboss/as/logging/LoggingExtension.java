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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class LoggingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "logging";
    private static final PathElement loggersPath = PathElement.pathElement(CommonAttributes.LOGGER);
    private static final PathElement handlersPath = PathElement.pathElement(CommonAttributes.HANDLER);

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(LoggingSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, NewLoggingSubsystemAdd.ADD_INSTANCE, LoggingSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, LoggingDescribeHandler.INSTANCE, LoggingDescribeHandler.INSTANCE, false);
        registration.registerOperationHandler(RootLoggerAdd.OPERATION_NAME, RootLoggerAdd.INSTANCE, LoggingSubsystemProviders.SET_ROOT_LOGGER, false);
        registration.registerOperationHandler(RootLoggerRemove.OPERATION_NAME, RootLoggerRemove.INSTANCE, LoggingSubsystemProviders.REMOVE_ROOT_LOGGER, false);
        subsystem.registerXMLElementWriter(LoggingSubsystemParser.getInstance());
        // loggers
        final ModelNodeRegistration loggers = registration.registerSubModel(loggersPath, LoggingSubsystemProviders.LOGGER);
        loggers.registerOperationHandler(ADD, LoggerAdd.INSTANCE, LoggingSubsystemProviders.LOGGER_ADD, false);
        loggers.registerOperationHandler(REMOVE, LoggerRemove.INSTANCE, LoggingSubsystemProviders.LOGGER_REMOVE, false);
        // handlers
        final ModelNodeRegistration handlers = registration.registerSubModel(handlersPath, LoggingSubsystemProviders.HANDLERS);
        handlers.registerOperationHandler(ADD, LoggerHandlerAdd.INSTANCE, LoggingSubsystemProviders.HANDLER_ADD, false);
        handlers.registerOperationHandler(REMOVE, LoggerHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        handlers.registerOperationHandler(AsyncHandlerAdd.OPERATION_NAME, AsyncHandlerAdd.INSTANCE, AsyncHandlerAdd.INSTANCE, false);
        handlers.registerOperationHandler(ConsoleHandlerAdd.OPERATION_NAME, ConsoleHandlerAdd.INSTANCE, LoggingSubsystemProviders.CONSOLE_HANDLER_ADD, false);
        handlers.registerOperationHandler(FileHandlerAdd.OPERATION_NAME, FileHandlerAdd.INSTANCE, LoggingSubsystemProviders.FILE_HANDLER_ADD, false);
        handlers.registerOperationHandler(PeriodicRotatingFileHandlerAdd.OPERATION_NAME, PeriodicRotatingFileHandlerAdd.INSTANCE, LoggingSubsystemProviders.PERIODIC_HANDLER_ADD, false);
        handlers.registerOperationHandler(SizeRotatingFileHandlerAdd.OPERATION_NAME, SizeRotatingFileHandlerAdd.INSTANCE, LoggingSubsystemProviders.SIZE_PERIODIC_HANDLER_ADD, false);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), LoggingSubsystemParser.getInstance());
    }


    static class NewLoggingSubsystemAdd implements ModelAddOperationHandler {

        static final NewLoggingSubsystemAdd ADD_INSTANCE = new NewLoggingSubsystemAdd();

        /** {@inheritDoc} */
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

            final ModelNode compensatingOperation = new ModelNode();
            compensatingOperation.get(OP).set(REMOVE);
            compensatingOperation.get(OP_ADDR).set(operation.get(OP_ADDR));

            final ModelNode subModel = context.getSubModel();
            subModel.get(CommonAttributes.LOGGER).setEmptyObject();
            subModel.get(CommonAttributes.HANDLER).setEmptyObject();

            resultHandler.handleResultComplete();
            return new BasicOperationResult(compensatingOperation);
        }

        static ModelNode createOperation(ModelNode address) {
            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);
            return subsystem;
        }
    }
}
