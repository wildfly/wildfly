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

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.EnumSet;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.logging.handlers.AbstractLogHandlerWriteAttributeHandler;
import org.jboss.as.logging.handlers.HandlerDisable;
import org.jboss.as.logging.handlers.HandlerEnable;
import org.jboss.as.logging.handlers.HandlerLevelChange;
import org.jboss.as.logging.handlers.LoggerHandlerRemove;
import org.jboss.as.logging.handlers.async.AsyncHandlerAdd;
import org.jboss.as.logging.handlers.async.AsyncHandlerAssignSubhandler;
import org.jboss.as.logging.handlers.async.AsyncHandlerUnassignSubhandler;
import org.jboss.as.logging.handlers.async.AsyncHandlerUpdateProperties;
import org.jboss.as.logging.handlers.async.AsyncHandlerWriteAttributeHandler;
import org.jboss.as.logging.handlers.console.ConsoleHandlerAdd;
import org.jboss.as.logging.handlers.console.ConsoleHandlerUpdateProperties;
import org.jboss.as.logging.handlers.console.ConsoleHandlerWriteAttributeHandler;
import org.jboss.as.logging.handlers.custom.CustomHandlerAdd;
import org.jboss.as.logging.handlers.custom.CustomHandlerUpdateProperties;
import org.jboss.as.logging.handlers.custom.CustomHandlerWriteAttributeHandler;
import org.jboss.as.logging.handlers.file.FileHandlerAdd;
import org.jboss.as.logging.handlers.file.FileHandlerUpdateProperties;
import org.jboss.as.logging.handlers.file.FileHandlerWriteAttributeHandler;
import org.jboss.as.logging.handlers.file.HandlerFileChange;
import org.jboss.as.logging.handlers.file.PeriodicHandlerUpdateProperties;
import org.jboss.as.logging.handlers.file.PeriodicHandlerWriteAttributeHandler;
import org.jboss.as.logging.handlers.file.PeriodicRotatingFileHandlerAdd;
import org.jboss.as.logging.handlers.file.SizeRotatingFileHandlerAdd;
import org.jboss.as.logging.handlers.file.SizeRotatingHandlerUpdateProperties;
import org.jboss.as.logging.handlers.file.SizeRotatingHandlerWriteAttributeHandler;
import org.jboss.as.logging.loggers.AbstractLoggerWriteAttributeHandler;
import org.jboss.as.logging.loggers.LoggerAdd;
import org.jboss.as.logging.loggers.LoggerAssignHandler;
import org.jboss.as.logging.loggers.LoggerFileHandlerRemove;
import org.jboss.as.logging.loggers.LoggerLevelChange;
import org.jboss.as.logging.loggers.LoggerRemove;
import org.jboss.as.logging.loggers.LoggerUnassignHandler;
import org.jboss.as.logging.loggers.LoggerWriteAttributeHandler;
import org.jboss.as.logging.loggers.RootLoggerAdd;
import org.jboss.as.logging.loggers.RootLoggerAssignHandler;
import org.jboss.as.logging.loggers.RootLoggerLevelChange;
import org.jboss.as.logging.loggers.RootLoggerRemove;
import org.jboss.as.logging.loggers.RootLoggerUnassignHandler;
import org.jboss.as.logging.loggers.RootLoggerWriteAttributeHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class LoggingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "logging";
    public static final PathElement rootLoggerPath = PathElement.pathElement(CommonAttributes.ROOT_LOGGER, CommonAttributes.ROOT_LOGGER_NAME);
    private static final PathElement loggersPath = PathElement.pathElement(CommonAttributes.LOGGER);
    private static final PathElement asyncHandlersPath = PathElement.pathElement(CommonAttributes.ASYNC_HANDLER);
    private static final PathElement consoleHandlersPath = PathElement.pathElement(CommonAttributes.CONSOLE_HANDLER);
    private static final PathElement customHandlerPath = PathElement.pathElement(CommonAttributes.CUSTOM_HANDLER);
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
        registration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, LoggingSubsystemProviders.SUBSYSTEM_REMOVE, false);

        subsystem.registerXMLElementWriter(LoggingSubsystemParser.INSTANCE);

        // Root logger
        final ManagementResourceRegistration rootLogger = registration.registerSubModel(rootLoggerPath, LoggingSubsystemProviders.ROOT_LOGGER);
        rootLogger.registerOperationHandler(RootLoggerAdd.OPERATION_NAME, RootLoggerAdd.INSTANCE, LoggingSubsystemProviders.ADD_ROOT_LOGGER, false);
        rootLogger.registerOperationHandler(RootLoggerRemove.OPERATION_NAME, RootLoggerRemove.INSTANCE, LoggingSubsystemProviders.REMOVE_ROOT_LOGGER, false);
        rootLogger.registerOperationHandler(RootLoggerLevelChange.OPERATION_NAME, RootLoggerLevelChange.INSTANCE, LoggingSubsystemProviders.ROOT_LOGGER_CHANGE_LEVEL, false);
        rootLogger.registerOperationHandler(RootLoggerAssignHandler.OPERATION_NAME, RootLoggerAssignHandler.INSTANCE, LoggingSubsystemProviders.ROOT_LOGGER_ASSIGN_HANDLER, false);
        rootLogger.registerOperationHandler(RootLoggerUnassignHandler.OPERATION_NAME, RootLoggerUnassignHandler.INSTANCE, LoggingSubsystemProviders.ROOT_LOGGER_UNASSIGN_HANDLER, false);
        addWriteAttributes(rootLogger,  RootLoggerWriteAttributeHandler.INSTANCE);

        // loggers
        final ManagementResourceRegistration loggers = registration.registerSubModel(loggersPath, LoggingSubsystemProviders.LOGGER);
        loggers.registerOperationHandler(ADD, LoggerAdd.INSTANCE, LoggingSubsystemProviders.LOGGER_ADD, false);
        loggers.registerOperationHandler(REMOVE, LoggerRemove.INSTANCE, LoggingSubsystemProviders.LOGGER_REMOVE, false);
        loggers.registerOperationHandler(LoggerLevelChange.OPERATION_NAME, LoggerLevelChange.INSTANCE, LoggingSubsystemProviders.LOGGER_CHANGE_LEVEL, false);
        loggers.registerOperationHandler(LoggerAssignHandler.OPERATION_NAME, LoggerAssignHandler.INSTANCE, LoggingSubsystemProviders.LOGGER_ASSIGN_HANDLER, false);
        loggers.registerOperationHandler(LoggerUnassignHandler.OPERATION_NAME, LoggerUnassignHandler.INSTANCE, LoggingSubsystemProviders.LOGGER_UNASSIGN_HANDLER, false);
        addWriteAttributes(loggers, LoggerWriteAttributeHandler.INSTANCE);

        //  Async handlers
        final ManagementResourceRegistration asyncHandler = registration.registerSubModel(asyncHandlersPath, LoggingSubsystemProviders.ASYNC_HANDLER);
        asyncHandler.registerOperationHandler(ADD, AsyncHandlerAdd.INSTANCE, LoggingSubsystemProviders.ASYNC_HANDLER_ADD, false);
        asyncHandler.registerOperationHandler(REMOVE, LoggerHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        asyncHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        asyncHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        asyncHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        asyncHandler.registerOperationHandler(AsyncHandlerUpdateProperties.OPERATION_NAME, AsyncHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.ASYNC_HANDLER_UPDATE, false);
        asyncHandler.registerOperationHandler(AsyncHandlerAssignSubhandler.OPERATION_NAME, AsyncHandlerAssignSubhandler.INSTANCE, LoggingSubsystemProviders.ASYNC_HANDLER_ASSIGN_SUBHANDLER, false);
        asyncHandler.registerOperationHandler(AsyncHandlerUnassignSubhandler.OPERATION_NAME, AsyncHandlerUnassignSubhandler.INSTANCE, LoggingSubsystemProviders.ASYNC_HANDLER_UNASSIGN_SUBHANDLER, false);
        addWriteAttributes(asyncHandler, AsyncHandlerWriteAttributeHandler.INSTANCE);

        //  Console handlers
        final ManagementResourceRegistration consoleHandler = registration.registerSubModel(consoleHandlersPath, LoggingSubsystemProviders.CONSOLE_HANDLER);
        consoleHandler.registerOperationHandler(ADD, ConsoleHandlerAdd.INSTANCE, LoggingSubsystemProviders.CONSOLE_HANDLER_ADD, false);
        consoleHandler.registerOperationHandler(REMOVE, LoggerHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        consoleHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        consoleHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        consoleHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        consoleHandler.registerOperationHandler(ConsoleHandlerUpdateProperties.OPERATION_NAME, ConsoleHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.CONSOLE_HANDLER_UPDATE, false);
        addWriteAttributes(consoleHandler, ConsoleHandlerWriteAttributeHandler.INSTANCE);

        final ManagementResourceRegistration fileHandler = registration.registerSubModel(fileHandlersPath, LoggingSubsystemProviders.FILE_HANDLER);
        fileHandler.registerOperationHandler(ADD, FileHandlerAdd.INSTANCE, LoggingSubsystemProviders.FILE_HANDLER_ADD, false);
        fileHandler.registerOperationHandler(REMOVE, LoggerFileHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        fileHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        fileHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        fileHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        fileHandler.registerOperationHandler(HandlerFileChange.OPERATION_NAME, HandlerFileChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_FILE, false);
        fileHandler.registerOperationHandler(FileHandlerUpdateProperties.OPERATION_NAME, FileHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.FILE_HANDLER_UPDATE, false);
        addWriteAttributes(fileHandler, FileHandlerWriteAttributeHandler.INSTANCE);

        final ManagementResourceRegistration periodicHandler = registration.registerSubModel(periodicHandlersPath, LoggingSubsystemProviders.PERIODIC_HANDLER);
        periodicHandler.registerOperationHandler(ADD, PeriodicRotatingFileHandlerAdd.INSTANCE, LoggingSubsystemProviders.PERIODIC_HANDLER_ADD, false);
        periodicHandler.registerOperationHandler(REMOVE, LoggerFileHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        periodicHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        periodicHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        periodicHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        periodicHandler.registerOperationHandler(HandlerFileChange.OPERATION_NAME, HandlerFileChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_FILE, false);
        periodicHandler.registerOperationHandler(PeriodicHandlerUpdateProperties.OPERATION_NAME, PeriodicHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.PERIODIC_HANDLER_UPDATE, false);
        addWriteAttributes(periodicHandler, PeriodicHandlerWriteAttributeHandler.INSTANCE);

        final ManagementResourceRegistration sizePeriodicHandler = registration.registerSubModel(sizePeriodicHandlersPath, LoggingSubsystemProviders.SIZE_PERIODIC_HANDLER);
        sizePeriodicHandler.registerOperationHandler(ADD, SizeRotatingFileHandlerAdd.INSTANCE, LoggingSubsystemProviders.SIZE_PERIODIC_HANDLER_ADD, false);
        sizePeriodicHandler.registerOperationHandler(REMOVE, LoggerFileHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        sizePeriodicHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        sizePeriodicHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        sizePeriodicHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        sizePeriodicHandler.registerOperationHandler(HandlerFileChange.OPERATION_NAME, HandlerFileChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_FILE, false);
        sizePeriodicHandler.registerOperationHandler(SizeRotatingHandlerUpdateProperties.OPERATION_NAME, SizeRotatingHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.SIZE_PERIODIC_HANDLER_UPDATE, false);
        addWriteAttributes(sizePeriodicHandler, SizeRotatingHandlerWriteAttributeHandler.INSTANCE);

        // Custom logging handler
        final ManagementResourceRegistration customHandler = registration.registerSubModel(customHandlerPath, LoggingSubsystemProviders.CUSTOM_HANDLER);
        customHandler.registerOperationHandler(ADD, CustomHandlerAdd.INSTANCE, LoggingSubsystemProviders.CUSTOM_HANDLER_ADD, false);
        customHandler.registerOperationHandler(REMOVE, LoggerHandlerRemove.INSTANCE, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        customHandler.registerOperationHandler(ENABLE, HandlerEnable.INSTANCE, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        customHandler.registerOperationHandler(DISABLE, HandlerDisable.INSTANCE, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        customHandler.registerOperationHandler(HandlerLevelChange.OPERATION_NAME, HandlerLevelChange.INSTANCE, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        customHandler.registerOperationHandler(CustomHandlerUpdateProperties.OPERATION_NAME, CustomHandlerUpdateProperties.INSTANCE, LoggingSubsystemProviders.CUSTOM_HANDLER_UPDATE, false);
        addWriteAttributes(customHandler, CustomHandlerWriteAttributeHandler.INSTANCE);
        // The properties attribute must be defined manually as it's not an AttributeDefinition
        customHandler.registerReadWriteAttribute(PROPERTIES, null, CustomHandlerWriteAttributeHandler.INSTANCE, EnumSet.of(AttributeAccess.Flag.RESTART_NONE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.LOGGING_1_0.getUriString(), LoggingSubsystemParser.INSTANCE);
        context.setSubsystemXmlMapping(Namespace.LOGGING_1_1.getUriString(), LoggingSubsystemParser.INSTANCE);
    }

    private void addWriteAttributes(final ManagementResourceRegistration handler, final AbstractLogHandlerWriteAttributeHandler<?> stepHandler) {
        for (AttributeDefinition attr : stepHandler.getAttributes()) {
            handler.registerReadWriteAttribute(attr, null, stepHandler);
        }
    }

    private void addWriteAttributes(final ManagementResourceRegistration handler, final AbstractLoggerWriteAttributeHandler stepHandler) {
        for (AttributeDefinition attr : stepHandler.getAttributes()) {
            handler.registerReadWriteAttribute(attr, null, stepHandler);
        }
    }


    static class NewLoggingSubsystemAdd extends AbstractAddStepHandler {

        static final NewLoggingSubsystemAdd ADD_INSTANCE = new NewLoggingSubsystemAdd();

        protected void populateModel(ModelNode operation, ModelNode model) {
            model.get(CommonAttributes.LOGGER).setEmptyObject();
            model.get(CommonAttributes.ASYNC_HANDLER).setEmptyObject();
            model.get(CommonAttributes.CONSOLE_HANDLER).setEmptyObject();
            model.get(CommonAttributes.CUSTOM_HANDLER).setEmptyObject();
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
