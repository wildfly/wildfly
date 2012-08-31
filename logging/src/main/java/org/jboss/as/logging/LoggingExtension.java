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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.logging.LoggingOperations.LoggingWriteAttributeHandler;
import org.jboss.logmanager.ContextClassLoaderLogContextSelector;
import org.jboss.logmanager.LogContext;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingExtension implements Extension {

    static final String SUBSYSTEM_NAME = "logging";
    static final PathElement rootLoggerPath = PathElement.pathElement(CommonAttributes.ROOT_LOGGER, CommonAttributes.ROOT_LOGGER_ATTRIBUTE_NAME);
    static final PathElement loggersPath = PathElement.pathElement(CommonAttributes.LOGGER);
    static final PathElement asyncHandlersPath = PathElement.pathElement(CommonAttributes.ASYNC_HANDLER);
    static final PathElement consoleHandlersPath = PathElement.pathElement(CommonAttributes.CONSOLE_HANDLER);
    static final PathElement customHandlerPath = PathElement.pathElement(CommonAttributes.CUSTOM_HANDLER);
    static final PathElement fileHandlersPath = PathElement.pathElement(CommonAttributes.FILE_HANDLER);
    static final PathElement periodicHandlersPath = PathElement.pathElement(CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER);
    static final PathElement sizeRotatingHandlersPath = PathElement.pathElement(CommonAttributes.SIZE_ROTATING_FILE_HANDLER);

    static final ContextClassLoaderLogContextSelector CONTEXT_SELECTOR = new ContextClassLoaderLogContextSelector();

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 1;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;


    @Override
    public void initialize(final ExtensionContext context) {
        LogContext.setLogContextSelector(CONTEXT_SELECTOR);
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(LoggingSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, LoggingSubsystemAdd.ADD_INSTANCE, LoggingSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, LoggingDescribeHandler.INSTANCE, LoggingDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        registration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, LoggingSubsystemProviders.SUBSYSTEM_REMOVE, false);

        // Add the handlers to the base path
        registerDefaultHandlers(registration);

        subsystem.registerXMLElementWriter(LoggingSubsystemParser.INSTANCE);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        for (Namespace namespace : Namespace.readable()) {
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUriString(), LoggingSubsystemParser.INSTANCE);
        }
    }

    private void registerDefaultHandlers(final ManagementResourceRegistration registration) {

        // Root logger
        final ManagementResourceRegistration rootLogger = registration.registerSubModel(rootLoggerPath, LoggingSubsystemProviders.ROOT_LOGGER);
        rootLogger.registerOperationHandler(ADD, LoggerOperations.ADD_ROOT_LOGGER, LoggingSubsystemProviders.ADD_ROOT_LOGGER, false);
        rootLogger.registerOperationHandler(LoggerOperations.ROOT_LOGGER_ADD_OPERATION_NAME, LoggerOperations.ADD_ROOT_LOGGER, LoggingSubsystemProviders.LEGACY_ADD_ROOT_LOGGER, false);
        rootLogger.registerOperationHandler(REMOVE, LoggerOperations.REMOVE_LOGGER, LoggingSubsystemProviders.REMOVE_ROOT_LOGGER, false);
        rootLogger.registerOperationHandler(LoggerOperations.ROOT_LOGGER_REMOVE_OPERATION_NAME, LoggerOperations.REMOVE_LOGGER, LoggingSubsystemProviders.LEGACY_REMOVE_ROOT_LOGGER, false);
        rootLogger.registerOperationHandler(LoggerOperations.ROOT_LOGGER_CHANGE_LEVEL_OPERATION_NAME, LoggerOperations.CHANGE_LEVEL, LoggingSubsystemProviders.ROOT_LOGGER_CHANGE_LEVEL, false);
        rootLogger.registerOperationHandler(LoggerOperations.ROOT_LOGGER_ADD_HANDLER_OPERATION_NAME, LoggerOperations.ADD_HANDLER, LoggingSubsystemProviders.ROOT_LOGGER_ASSIGN_HANDLER, false);
        rootLogger.registerOperationHandler(LoggerOperations.ROOT_LOGGER_REMOVE_HANDLER_OPERATION_NAME, LoggerOperations.REMOVE_HANDLER, LoggingSubsystemProviders.ROOT_LOGGER_UNASSIGN_HANDLER, false);
        addWriteAttributes(rootLogger, LoggerOperations.ROOT_LOGGER_WRITE_HANDLER);

        // loggers
        final ManagementResourceRegistration loggers = registration.registerSubModel(loggersPath, LoggingSubsystemProviders.LOGGER);
        loggers.registerOperationHandler(ADD, LoggerOperations.ADD_LOGGER, LoggingSubsystemProviders.LOGGER_ADD, false);
        loggers.registerOperationHandler(REMOVE, LoggerOperations.REMOVE_LOGGER, LoggingSubsystemProviders.LOGGER_REMOVE, false);
        loggers.registerOperationHandler(LoggerOperations.CHANGE_LEVEL_OPERATION_NAME, LoggerOperations.CHANGE_LEVEL, LoggingSubsystemProviders.LOGGER_CHANGE_LEVEL, false);
        loggers.registerOperationHandler(LoggerOperations.ADD_HANDLER_OPERATION_NAME, LoggerOperations.ADD_HANDLER, LoggingSubsystemProviders.LOGGER_ASSIGN_HANDLER, false);
        loggers.registerOperationHandler(LoggerOperations.REMOVE_HANDLER_OPERATION_NAME, LoggerOperations.REMOVE_HANDLER, LoggingSubsystemProviders.LOGGER_UNASSIGN_HANDLER, false);
        addWriteAttributes(loggers, LoggerOperations.LOGGER_WRITE_HANDLER);

        //  Async handlers
        final ManagementResourceRegistration asyncHandler = registration.registerSubModel(asyncHandlersPath, LoggingSubsystemProviders.ASYNC_HANDLER);
        asyncHandler.registerOperationHandler(ADD, HandlerOperations.ADD_ASYNC_HANDLER, LoggingSubsystemProviders.ASYNC_HANDLER_ADD, false);
        asyncHandler.registerOperationHandler(REMOVE, HandlerOperations.REMOVE_HANDLER, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        asyncHandler.registerOperationHandler(ENABLE, HandlerOperations.ENABLE_HANDLER, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        asyncHandler.registerOperationHandler(DISABLE, HandlerOperations.DISABLE_HANDLER, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        asyncHandler.registerOperationHandler(HandlerOperations.CHANGE_LEVEL_OPERATION_NAME, HandlerOperations.CHANGE_LEVEL, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        asyncHandler.registerOperationHandler(HandlerOperations.UPDATE_OPERATION_NAME, HandlerOperations.UPDATE_ASYNC_PROPERTIES, LoggingSubsystemProviders.ASYNC_HANDLER_UPDATE, false);
        asyncHandler.registerOperationHandler(HandlerOperations.ADD_SUBHANDLER_OPERATION_NAME, HandlerOperations.ADD_SUBHANDLER, LoggingSubsystemProviders.ASYNC_HANDLER_ASSIGN_SUBHANDLER, false);
        asyncHandler.registerOperationHandler(HandlerOperations.REMOVE_SUBHANDLER_OPERATION_NAME, HandlerOperations.REMOVE_SUBHANDLER, LoggingSubsystemProviders.ASYNC_HANDLER_UNASSIGN_SUBHANDLER, false);
        addWriteAttributes(asyncHandler, HandlerOperations.ASYNC_WRITE_HANDLER);

        //  Console handlers
        final ManagementResourceRegistration consoleHandler = registration.registerSubModel(consoleHandlersPath, LoggingSubsystemProviders.CONSOLE_HANDLER);
        consoleHandler.registerOperationHandler(ADD, HandlerOperations.ADD_CONSOLE_HANDLER, LoggingSubsystemProviders.CONSOLE_HANDLER_ADD, false);
        consoleHandler.registerOperationHandler(REMOVE, HandlerOperations.REMOVE_HANDLER, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        consoleHandler.registerOperationHandler(ENABLE, HandlerOperations.ENABLE_HANDLER, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        consoleHandler.registerOperationHandler(DISABLE, HandlerOperations.DISABLE_HANDLER, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        consoleHandler.registerOperationHandler(HandlerOperations.CHANGE_LEVEL_OPERATION_NAME, HandlerOperations.CHANGE_LEVEL, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        consoleHandler.registerOperationHandler(HandlerOperations.UPDATE_OPERATION_NAME, HandlerOperations.UPDATE_CONSOLE_PROPERTIES, LoggingSubsystemProviders.CONSOLE_HANDLER_UPDATE, false);
        addWriteAttributes(consoleHandler, HandlerOperations.CONSOLE_WRITE_ATTRIBUTES);

        final ManagementResourceRegistration fileHandler = registration.registerSubModel(fileHandlersPath, LoggingSubsystemProviders.FILE_HANDLER);
        fileHandler.registerOperationHandler(ADD, HandlerOperations.ADD_FILE_HANDLER, LoggingSubsystemProviders.FILE_HANDLER_ADD, false);
        fileHandler.registerOperationHandler(REMOVE, HandlerOperations.REMOVE_HANDLER, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        fileHandler.registerOperationHandler(ENABLE, HandlerOperations.ENABLE_HANDLER, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        fileHandler.registerOperationHandler(DISABLE, HandlerOperations.DISABLE_HANDLER, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        fileHandler.registerOperationHandler(HandlerOperations.CHANGE_LEVEL_OPERATION_NAME, HandlerOperations.CHANGE_LEVEL, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        fileHandler.registerOperationHandler(HandlerOperations.CHANGE_FILE_OPERATION_NAME, HandlerOperations.CHANGE_FILE, LoggingSubsystemProviders.HANDLER_CHANGE_FILE, false);
        fileHandler.registerOperationHandler(HandlerOperations.UPDATE_OPERATION_NAME, HandlerOperations.UPDATE_FILE_HANDLER_PROPERTIES, LoggingSubsystemProviders.FILE_HANDLER_UPDATE, false);
        addWriteAttributes(fileHandler, HandlerOperations.WRITE_FILE_HANDLER_ATTRIBUTES);

        final ManagementResourceRegistration periodicHandler = registration.registerSubModel(periodicHandlersPath, LoggingSubsystemProviders.PERIODIC_HANDLER);
        periodicHandler.registerOperationHandler(ADD, HandlerOperations.ADD_PERIODIC__ROTATING_FILE_HANDLER, LoggingSubsystemProviders.PERIODIC_HANDLER_ADD, false);
        periodicHandler.registerOperationHandler(REMOVE, HandlerOperations.REMOVE_HANDLER, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        periodicHandler.registerOperationHandler(ENABLE, HandlerOperations.ENABLE_HANDLER, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        periodicHandler.registerOperationHandler(DISABLE, HandlerOperations.DISABLE_HANDLER, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        periodicHandler.registerOperationHandler(HandlerOperations.CHANGE_LEVEL_OPERATION_NAME, HandlerOperations.CHANGE_LEVEL, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        periodicHandler.registerOperationHandler(HandlerOperations.CHANGE_FILE_OPERATION_NAME, HandlerOperations.CHANGE_FILE, LoggingSubsystemProviders.HANDLER_CHANGE_FILE, false);
        periodicHandler.registerOperationHandler(HandlerOperations.UPDATE_OPERATION_NAME, HandlerOperations.UPDATE_PERIODIC_ROTATING_FILE_HANDLER_PROPERTIES, LoggingSubsystemProviders.PERIODIC_HANDLER_UPDATE, false);
        addWriteAttributes(periodicHandler, HandlerOperations.WRITE_PERIODIC__ROTATING_FILE_HANDLER_ATTRIBUTES);

        final ManagementResourceRegistration sizePeriodicHandler = registration.registerSubModel(sizeRotatingHandlersPath, LoggingSubsystemProviders.SIZE_PERIODIC_HANDLER);
        sizePeriodicHandler.registerOperationHandler(ADD, HandlerOperations.ADD_SIZE_ROTATING_FILE_HANDLER, LoggingSubsystemProviders.SIZE_PERIODIC_HANDLER_ADD, false);
        sizePeriodicHandler.registerOperationHandler(REMOVE, HandlerOperations.REMOVE_HANDLER, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        sizePeriodicHandler.registerOperationHandler(ENABLE, HandlerOperations.ENABLE_HANDLER, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        sizePeriodicHandler.registerOperationHandler(DISABLE, HandlerOperations.DISABLE_HANDLER, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        sizePeriodicHandler.registerOperationHandler(HandlerOperations.CHANGE_LEVEL_OPERATION_NAME, HandlerOperations.CHANGE_LEVEL, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        sizePeriodicHandler.registerOperationHandler(HandlerOperations.CHANGE_FILE_OPERATION_NAME, HandlerOperations.CHANGE_FILE, LoggingSubsystemProviders.HANDLER_CHANGE_FILE, false);
        sizePeriodicHandler.registerOperationHandler(HandlerOperations.UPDATE_OPERATION_NAME, HandlerOperations.UPDATE_SIZE_ROTATING_FILE_HANDLER_PROPERTIES, LoggingSubsystemProviders.SIZE_PERIODIC_HANDLER_UPDATE, false);
        addWriteAttributes(sizePeriodicHandler, HandlerOperations.WRITE_SIZE_ROTATING_FILE_HANDLER_ATTRIBUTES);

        // Custom logging handler
        final ManagementResourceRegistration customHandler = registration.registerSubModel(customHandlerPath, LoggingSubsystemProviders.CUSTOM_HANDLER);
        customHandler.registerOperationHandler(ADD, HandlerOperations.ADD_CUSTOM_HANDLER, LoggingSubsystemProviders.CUSTOM_HANDLER_ADD, false);
        customHandler.registerOperationHandler(REMOVE, HandlerOperations.REMOVE_HANDLER, LoggingSubsystemProviders.HANDLER_REMOVE, false);
        customHandler.registerOperationHandler(ENABLE, HandlerOperations.ENABLE_HANDLER, LoggingSubsystemProviders.HANDLER_ENABLE, false);
        customHandler.registerOperationHandler(DISABLE, HandlerOperations.DISABLE_HANDLER, LoggingSubsystemProviders.HANDLER_DISABLE, false);
        customHandler.registerOperationHandler(HandlerOperations.CHANGE_LEVEL_OPERATION_NAME, HandlerOperations.CHANGE_LEVEL, LoggingSubsystemProviders.HANDLER_CHANGE_LEVEL, false);
        customHandler.registerOperationHandler(HandlerOperations.UPDATE_OPERATION_NAME, HandlerOperations.UPDATE_CUSTOM_PROPERTIES, LoggingSubsystemProviders.CUSTOM_HANDLER_UPDATE, false);
        addWriteAttributes(customHandler, HandlerOperations.CUSTOM_HANDLER_WRITE_ATTRIBUTES);
        // The properties attribute must be defined manually as it's not an AttributeDefinition
        customHandler.registerReadWriteAttribute(PROPERTIES, null, HandlerOperations.CUSTOM_HANDLER_WRITE_ATTRIBUTES, EnumSet.of(AttributeAccess.Flag.RESTART_NONE));
        customHandler.registerReadOnlyAttribute(CommonAttributes.CLASS, null);
        customHandler.registerReadOnlyAttribute(CommonAttributes.MODULE, null);
    }

    private void addWriteAttributes(final ManagementResourceRegistration handler, final LoggingWriteAttributeHandler stepHandler) {
        for (AttributeDefinition attr : stepHandler.getAttributes()) {
            handler.registerReadWriteAttribute(attr, null, stepHandler);
        }
    }
}
