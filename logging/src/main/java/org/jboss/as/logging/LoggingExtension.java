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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class LoggingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "logging";
    private static final PathElement loggersPath = PathElement.pathElement(CommonAttributes.LOGGER);
    private static final PathElement asyncHandlersPath = PathElement.pathElement(CommonAttributes.ASYNC_HANDLER);
    private static final PathElement consoleHandlersPath = PathElement.pathElement(CommonAttributes.CONSOLE_HANDLER);
    private static final PathElement fileHandlersPath = PathElement.pathElement(CommonAttributes.FILE_HANDLER);
    private static final PathElement periodicHandlersPath = PathElement.pathElement(CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER);
    private static final PathElement sizePeriodicHandlersPath = PathElement.pathElement(CommonAttributes.SIZE_ROTATING_FILE_HANDLER);


    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(LoggingSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, NewLoggingSubsystemAdd.ADD_INSTANCE, LoggingSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, LoggingDescribeHandler.INSTANCE, LoggingDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        registration.registerOperationHandler(RootLoggerAdd.OPERATION_NAME, RootLoggerAdd.INSTANCE, LoggingSubsystemProviders.SET_ROOT_LOGGER, false);
        registration.registerOperationHandler(RootLoggerRemove.OPERATION_NAME, RootLoggerRemove.INSTANCE, LoggingSubsystemProviders.REMOVE_ROOT_LOGGER, false);
        registration.registerOperationHandler(RootLoggerLevelChange.OPERATION_NAME, RootLoggerLevelChange.INSTANCE, LoggingSubsystemProviders.ROOT_LOGGER_CHANGE_LEVEL, false);

        subsystem.registerXMLElementWriter(LoggingSubsystemParser.getInstance());
        // loggers
        final ManagementResourceRegistration loggers = registration.registerSubModel(loggersPath, LoggingSubsystemProviders.LOGGER);
        loggers.registerOperationHandler(ADD, LoggerAdd.INSTANCE, LoggingSubsystemProviders.LOGGER_ADD, false);
        loggers.registerOperationHandler(REMOVE, LoggerRemove.INSTANCE, LoggingSubsystemProviders.LOGGER_REMOVE, false);
        loggers.registerOperationHandler(LoggerLevelChange.OPERATION_NAME, LoggerLevelChange.INSTANCE, LoggingSubsystemProviders.LOGGER_CHANGE_LEVEL, false);

        //  Async handlers
        final ManagementResourceRegistration asyncHandler = registration.registerSubModel(asyncHandlersPath, LoggingSubsystemProviders.ASYNC_HANDLER);
        asyncHandler.registerOperationHandler(ADD, AsyncHandlerAdd.INSTANCE, LoggingSubsystemProviders.ASYNC_HANDLER_ADD, false);
        asyncHandler.registerOperationHandler(REMOVE, LoggerHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        asyncHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        asyncHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        asyncHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        asyncHandler.registerOperationHandler(AsyncHandlerUpdateProperties.OPERATION_NAME, AsyncHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.ASYNC_HANDLER_UPDATE, false);

        //  Console handlers
        final ManagementResourceRegistration consoleHandler = registration.registerSubModel(consoleHandlersPath, LoggingSubsystemProviders.CONSOLE_HANDLER);
        consoleHandler.registerOperationHandler(ADD, ConsoleHandlerAdd.INSTANCE, LoggingSubsystemProviders.CONSOLE_HANDLER_ADD, false);
        consoleHandler.registerOperationHandler(REMOVE, LoggerHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        consoleHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        consoleHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        consoleHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        consoleHandler.registerOperationHandler(ConsoleHandlerUpdateProperties.OPERATION_NAME, ConsoleHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.CONSOLE_HANDLER_UPDATE, false);

        final ManagementResourceRegistration fileHandler = registration.registerSubModel(fileHandlersPath, LoggingSubsystemProviders.FILE_HANDLER);
        fileHandler.registerOperationHandler(ADD, FileHandlerAdd.INSTANCE, LoggingSubsystemProviders.FILE_HANDLER_ADD, false);
        fileHandler.registerOperationHandler(REMOVE, LoggerHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        fileHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        fileHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        fileHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        fileHandler.registerOperationHandler(HandlerFileChange.OPERATION_NAME, HandlerFileChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_FILE, false);
        fileHandler.registerOperationHandler(FileHandlerUpdateProperties.OPERATION_NAME, FileHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.FILE_HANDLER_UPDATE, false);

        final ManagementResourceRegistration periodicHandler = registration.registerSubModel(periodicHandlersPath, LoggingSubsystemProviders.PERIODIC_HANDLER);
        periodicHandler.registerOperationHandler(ADD, PeriodicRotatingFileHandlerAdd.INSTANCE, LoggingSubsystemProviders.PERIODIC_HANDLER_ADD, false);
        periodicHandler.registerOperationHandler(REMOVE, LoggerHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        periodicHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        periodicHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        periodicHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        periodicHandler.registerOperationHandler(HandlerFileChange.OPERATION_NAME, HandlerFileChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_FILE, false);
        periodicHandler.registerOperationHandler(PeriodicHandlerUpdateProperties.OPERATION_NAME, PeriodicHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.PERIODIC_HANDLER_UPDATE, false);

        final ManagementResourceRegistration sizePeriodicHandler = registration.registerSubModel(sizePeriodicHandlersPath, LoggingSubsystemProviders.SIZE_PERIODIC_HANDLER);
        sizePeriodicHandler.registerOperationHandler(ADD, SizeRotatingFileHandlerAdd.INSTANCE, LoggingSubsystemProviders.SIZE_PERIODIC_HANDLER_ADD, false);
        sizePeriodicHandler.registerOperationHandler(REMOVE, LoggerHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        sizePeriodicHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        sizePeriodicHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        sizePeriodicHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        sizePeriodicHandler.registerOperationHandler(HandlerFileChange.OPERATION_NAME, HandlerFileChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_FILE, false);
        sizePeriodicHandler.registerOperationHandler(SizeRotatingHandlerUpdateProperties.OPERATION_NAME, SizeRotatingHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.SIZE_PERIODIC_HANDLER_UPDATE, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), LoggingSubsystemParser.getInstance());
    }


    static class NewLoggingSubsystemAdd extends AbstractAddStepHandler {

        static final NewLoggingSubsystemAdd ADD_INSTANCE = new NewLoggingSubsystemAdd();

        protected void populateModel(ModelNode operation, ModelNode model) {
            model.get(CommonAttributes.LOGGER).setEmptyObject();
            model.get(CommonAttributes.ASYNC_HANDLER).setEmptyObject();
            model.get(CommonAttributes.CONSOLE_HANDLER).setEmptyObject();
            model.get(CommonAttributes.FILE_HANDLER).setEmptyObject();
            model.get(CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER).setEmptyObject();
            model.get(CommonAttributes.SIZE_ROTATING_FILE_HANDLER).setEmptyObject();
        }

        protected boolean requiresRuntime(OperationContext context) {
            return false;
        }

        static ModelNode createOperation(ModelNode address) {
            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);
            return subsystem;
        }
    }
}
