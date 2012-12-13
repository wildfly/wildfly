/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FILTER_SPEC;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.HANDLER_NAME;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER_ATTRIBUTE_NAME;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER_NAME;
import static org.jboss.as.logging.CommonAttributes.USE_PARENT_HANDLERS;
import static org.jboss.as.logging.Logging.createOperationFailure;
import static org.jboss.as.logging.LoggingOperations.LoggingAddOperationStepHandler;
import static org.jboss.as.logging.LoggingOperations.LoggingRemoveOperationStepHandler;
import static org.jboss.as.logging.LoggingOperations.LoggingUpdateOperationStepHandler;
import static org.jboss.as.logging.LoggingOperations.LoggingWriteAttributeHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.jboss.logmanager.config.LoggerConfiguration;

/**
 * Date: 14.12.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class LoggerOperations {

    abstract static class LoggerUpdateOperationStepHandler extends LoggingUpdateOperationStepHandler {

        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {

        }

        @Override
        public final void performRuntime(final OperationContext context, final ModelNode operation, final LogContextConfiguration logContextConfiguration, final String name, final ModelNode model) throws OperationFailedException {
            final String loggerName = getLogManagerLoggerName(name);
            LoggerConfiguration configuration = logContextConfiguration.getLoggerConfiguration(loggerName);
            if (configuration == null) {
                throw createOperationFailure(LoggingMessages.MESSAGES.loggerConfigurationNotFound(loggerName));
            }
            performRuntime(context, operation, configuration, loggerName, model);
        }

        /**
         * Executes additional processing for this step.
         *
         * @param context       the operation context
         * @param operation     the operation being executed
         * @param configuration the logging configuration
         * @param name          the name of the logger
         * @param model         the model to update
         *
         * @throws OperationFailedException if a processing error occurs
         */
        public abstract void performRuntime(OperationContext context, ModelNode operation, LoggerConfiguration configuration, String name, ModelNode model) throws OperationFailedException;
    }


    /**
     * A step handler for add operations of logging handlers. Adds default properties to the handler configuration.
     */
    static final class LoggerAddOperationStepHandler extends LoggingAddOperationStepHandler {
        private final AttributeDefinition[] attributes;

        LoggerAddOperationStepHandler(final AttributeDefinition[] attributes) {
            this.attributes = attributes;
        }

        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            for (AttributeDefinition attribute : attributes) {
                // Filter attribute needs to be converted to filter spec
                if (CommonAttributes.FILTER.equals(attribute)) {
                    final ModelNode filter = CommonAttributes.FILTER.validateOperation(operation);
                    if (filter.isDefined()) {
                        final String value = Filters.filterToFilterSpec(filter);
                        model.get(CommonAttributes.FILTER_SPEC.getName()).set(value);
                    }
                } else {
                    attribute.validateAndSet(operation, model);
                }
            }
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LogContextConfiguration logContextConfiguration, final String name, final ModelNode model) throws OperationFailedException {
            final String loggerName = getLogManagerLoggerName(name);
            LoggerConfiguration configuration = logContextConfiguration.getLoggerConfiguration(loggerName);
            if (configuration == null) {
                LoggingLogger.ROOT_LOGGER.tracef("Adding logger '%s' at '%s'", name, LoggingOperations.getAddress(operation));
                configuration = logContextConfiguration.addLoggerConfiguration(loggerName);
            }

            for (AttributeDefinition attribute : attributes) {
                handleProperty(attribute, context, model, configuration);
            }
        }
    }


    /**
     * A default log handler write attribute step handler.
     */
    public static class LoggerWriteAttributeHandler extends LoggingWriteAttributeHandler {

        protected LoggerWriteAttributeHandler(final AttributeDefinition[] attributes) {
            super(attributes);
        }

        @Override
        protected boolean applyUpdate(final OperationContext context, final String attributeName, final String addressName, final ModelNode value, final LogContextConfiguration logContextConfiguration) throws OperationFailedException {
            final String loggerName = getLogManagerLoggerName(addressName);
            if (logContextConfiguration.getLoggerNames().contains(loggerName)) {
                final LoggerConfiguration configuration = logContextConfiguration.getLoggerConfiguration(loggerName);
                if (LEVEL.getName().equals(attributeName)) {
                    handleProperty(LEVEL, context, value, configuration, false);
                } else if (FILTER.getName().equals(attributeName)) {
                    // Filter should be replaced by the filter-spec in the super class
                    handleProperty(FILTER_SPEC, context, value, configuration, false);
                } else if (FILTER_SPEC.getName().equals(attributeName)) {
                    handleProperty(FILTER_SPEC, context, value, configuration, false);
                } else if (HANDLERS.getName().equals(attributeName)) {
                    handleProperty(HANDLERS, context, value, configuration, false);
                } else if (USE_PARENT_HANDLERS.getName().equals(attributeName)) {
                    handleProperty(USE_PARENT_HANDLERS, context, value, configuration, false);
                }
            }
            return false;
        }
    }

    /**
     * A step handler to remove a logger
     */
    static LoggingRemoveOperationStepHandler REMOVE_LOGGER = new LoggingRemoveOperationStepHandler() {

        @Override
        public void performRemove(final OperationContext context, final ModelNode operation, final LogContextConfiguration logContextConfiguration, final String name, final ModelNode model) throws OperationFailedException {
            context.removeResource(PathAddress.EMPTY_ADDRESS);
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LogContextConfiguration logContextConfiguration, final String name, final ModelNode model) throws OperationFailedException {
            // Disable the logger before removing it
            final String loggerName = getLogManagerLoggerName(name);
            final LoggerConfiguration configuration = logContextConfiguration.getLoggerConfiguration(loggerName);
            if (configuration == null) {
                throw createOperationFailure(LoggingMessages.MESSAGES.loggerNotFound(loggerName));
            }
            logContextConfiguration.removeLoggerConfiguration(loggerName);
        }
    };

    /**
     * A step handler to add a handler.
     */
    public static LoggerUpdateOperationStepHandler ADD_HANDLER = new LoggerUpdateOperationStepHandler() {

        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            HANDLER_NAME.validateAndSet(operation, model);
            model.get(HANDLERS.getName()).add(operation.get(HANDLER_NAME.getName()));
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LoggerConfiguration configuration, final String name, final ModelNode model) throws OperationFailedException {
            // Get the handler name
            final String handlerName = HANDLER_NAME.resolveModelAttribute(context, model).asString();
            final String loggerName = getLogManagerLoggerName(name);
            if (configuration.getHandlerNames().contains(handlerName)) {
                throw createOperationFailure(LoggingMessages.MESSAGES.handlerAlreadyDefined(handlerName));
            }
            LoggingLogger.ROOT_LOGGER.tracef("Adding handler '%s' to logger '%s' at '%s'", handlerName, getLogManagerLoggerName(loggerName), LoggingOperations.getAddress(operation));
            configuration.addHandlerName(handlerName);
        }
    };

    /**
     * A step handler to remove a handler.
     */
    public static LoggerUpdateOperationStepHandler REMOVE_HANDLER = new LoggerUpdateOperationStepHandler() {

        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            HANDLER_NAME.validateAndSet(operation, model);
            final String handlerName = model.get(HANDLER_NAME.getName()).asString();
            // Create a new handler list for the model
            boolean found = false;
            final List<ModelNode> handlers = model.get(HANDLERS.getName()).asList();
            final List<ModelNode> newHandlers = new ArrayList<ModelNode>(handlers.size());
            for (ModelNode handler : handlers) {
                if (handlerName.equals(handler.asString())) {
                    found = true;
                } else {
                    newHandlers.add(handler);
                }
            }
            if (found) {
                model.get(HANDLERS.getName()).set(newHandlers);
            }
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LoggerConfiguration configuration, final String name, final ModelNode model) throws OperationFailedException {
            configuration.removeHandlerName(HANDLER_NAME.resolveModelAttribute(context, model).asString());
        }
    };

    /**
     * A step handler to remove a handler.
     */
    public static LoggerUpdateOperationStepHandler CHANGE_LEVEL = new LoggerUpdateOperationStepHandler() {

        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            LEVEL.validateAndSet(operation, model);
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LoggerConfiguration configuration, final String name, final ModelNode model) throws OperationFailedException {
            handleProperty(LEVEL, context, model, configuration);
        }
    };

    private static void handleProperty(final AttributeDefinition attribute, final OperationContext context, final ModelNode model,
                                       final LoggerConfiguration configuration) throws OperationFailedException {
        handleProperty(attribute, context, model, configuration, true);
    }

    private static void handleProperty(final AttributeDefinition attribute, final OperationContext context, final ModelNode model,
                                       final LoggerConfiguration configuration, final boolean resolveValue) throws OperationFailedException {
        if (FILTER_SPEC.equals(attribute)) {
            final ModelNode valueNode = (resolveValue ? model.get(FILTER_SPEC.getName()) : model);
            final String resolvedValue = (valueNode.isDefined() ? valueNode.asString() : null);
            configuration.setFilter(resolvedValue);
        } else if (LEVEL.equals(attribute)) {
            final String resolvedValue = (resolveValue ? LEVEL.resolvePropertyValue(context, model) : LEVEL.resolver().resolveValue(context, model));
            configuration.setLevel(resolvedValue);
        } else if (HANDLERS.equals(attribute)) {
            final Collection<String> resolvedValue = (resolveValue ? HANDLERS.resolvePropertyValue(context, model) : HANDLERS.resolver().resolveValue(context, model));
            configuration.setHandlerNames(resolvedValue);
        } else if (USE_PARENT_HANDLERS.equals(attribute)) {
            final ModelNode useParentHandlers = (resolveValue ? USE_PARENT_HANDLERS.resolveModelAttribute(context, model) : model);
            final Boolean resolvedValue = (useParentHandlers.isDefined() ? useParentHandlers.asBoolean() : null);
            configuration.setUseParentHandlers(resolvedValue);
        }
    }

    /**
     * Returns the logger name that should be used in the log manager.
     *
     * @param name the name of the logger from the resource
     *
     * @return the name of the logger
     */
    private static String getLogManagerLoggerName(final String name) {
        return (name.equals(ROOT_LOGGER_ATTRIBUTE_NAME) ? CommonAttributes.ROOT_LOGGER_NAME : name);
    }
}
