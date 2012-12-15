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

import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CommonAttributes.ENABLED;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FILTER_SPEC;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLER_NAME;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.MODULE;
import static org.jboss.as.logging.CommonAttributes.PROPERTIES;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;
import static org.jboss.as.logging.LoggerOperations.ADD_HANDLER;
import static org.jboss.as.logging.Logging.createOperationFailure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.logging.LoggingOperations.LoggingAddOperationStepHandler;
import org.jboss.as.logging.LoggingOperations.LoggingRemoveOperationStepHandler;
import org.jboss.as.logging.LoggingOperations.LoggingUpdateOperationStepHandler;
import org.jboss.as.logging.LoggingOperations.LoggingWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.Logger.AttachmentKey;
import org.jboss.logmanager.config.FormatterConfiguration;
import org.jboss.logmanager.config.HandlerConfiguration;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.jboss.logmanager.formatters.PatternFormatter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class HandlerOperations {

    private static final AttachmentKey<Map<String, String>> DISABLED_HANDLERS_KEY = new AttachmentKey<Map<String, String>>();
    private static final Object HANDLER_LOCK = new Object();


    /**
     * A step handler for updating logging handler properties.
     */
    static class HandlerUpdateOperationStepHandler extends LoggingUpdateOperationStepHandler {
        private final AttributeDefinition[] attributes;

        protected HandlerUpdateOperationStepHandler() {
            this.attributes = null;
        }

        protected HandlerUpdateOperationStepHandler(final AttributeDefinition... attributes) {
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
        public final void performRuntime(final OperationContext context, final ModelNode operation, final LogContextConfiguration logContextConfiguration, final String name, final ModelNode model) throws OperationFailedException {
            final HandlerConfiguration configuration = logContextConfiguration.getHandlerConfiguration(name);
            if (configuration == null) {
                throw createOperationFailure(LoggingMessages.MESSAGES.handlerConfigurationNotFound(name));
            }
            if (attributes != null) {
                boolean restartRequired = false;
                boolean reloadRequired = false;
                for (AttributeDefinition attribute : attributes) {
                    handleProperty(attribute, context, model, logContextConfiguration, configuration);
                    restartRequired = restartRequired || Logging.requiresRestart(attribute.getFlags());
                    reloadRequired = reloadRequired || Logging.requiresReload(attribute.getFlags());
                }
                if (restartRequired) {
                    context.restartRequired();
                } else if (reloadRequired) {
                    context.reloadRequired();
                }
            }
            performRuntime(context, configuration, name, model);
        }

        public void performRuntime(final OperationContext context, final HandlerConfiguration configuration, final String name, final ModelNode model) throws OperationFailedException {
            // No-op by default
        }
    }

    /**
     * A step handler for add operations of logging handlers. Adds default properties to the handler configuration.
     */
    static class HandlerAddOperationStepHandler extends LoggingAddOperationStepHandler {
        private final String[] constructionProperties;
        private final AttributeDefinition[] attributes;
        private final Class<? extends Handler> type;

        protected HandlerAddOperationStepHandler(final Class<? extends Handler> type, final AttributeDefinition[] attributes) {
            this.type = type;
            this.constructionProperties = null;
            this.attributes = attributes;
        }

        protected HandlerAddOperationStepHandler(final Class<? extends Handler> type, final AttributeDefinition[] attributes, final ConfigurationProperty<?>... constructionProperties) {
            this.type = type;
            this.attributes = attributes;
            final List<String> names = new ArrayList<String>();
            for (ConfigurationProperty<?> prop : constructionProperties) {
                names.add(prop.getPropertyName());
            }
            this.constructionProperties = names.toArray(new String[names.size()]);
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
            HandlerConfiguration configuration = logContextConfiguration.getHandlerConfiguration(name);
            final boolean exists = configuration != null;
            if (!exists) {
                LoggingLogger.ROOT_LOGGER.tracef("Adding handler '%s' at '%s'", name, LoggingOperations.getAddress(operation));
                configuration = createHandlerConfiguration(context, model, name, logContextConfiguration);
            }

            for (AttributeDefinition attribute : attributes) {
                // CLASS and MODULE should be ignored
                final boolean skip;
                if ((attribute.equals(CLASS) || attribute.equals(MODULE)) || attribute.equals(FILTER)) {
                    skip = true;
                } else {
                    // No need to change values that are equal
                    skip = (exists && equalValue(attribute, context, model, logContextConfiguration, configuration));
                }

                if (!skip)
                    handleProperty(attribute, context, model, logContextConfiguration, configuration);
            }
        }

        protected HandlerConfiguration createHandlerConfiguration(final OperationContext context,
                                                                  final ModelNode model, final String name,
                                                                  final LogContextConfiguration logContextConfiguration) throws OperationFailedException {
            final String className;
            final String moduleName;

            // Assume if the type is null we are using the MODULE and CLASS attributes
            if (type == null) {
                className = CLASS.resolveModelAttribute(context, model).asString();
                moduleName = MODULE.resolveModelAttribute(context, model).asString();
            } else {
                className = type.getName();
                moduleName = null;
            }
            final HandlerConfiguration configuration;
            // Check for construction parameters
            if (constructionProperties == null) {
                configuration = logContextConfiguration.addHandlerConfiguration(moduleName, className, name);
            } else {
                configuration = logContextConfiguration.addHandlerConfiguration(moduleName, className, name, constructionProperties);
            }
            return configuration;
        }
    }

    /**
     * A default log handler write attribute step handler.
     */
    public static class LogHandlerWriteAttributeHandler extends LoggingWriteAttributeHandler {

        protected LogHandlerWriteAttributeHandler(final AttributeDefinition... attributes) {
            super(attributes);
        }

        @Override
        protected boolean applyUpdate(final OperationContext context, final String attributeName, final String addressName, final ModelNode value, final LogContextConfiguration logContextConfiguration) throws OperationFailedException {
            boolean restartRequired = false;
            if (logContextConfiguration.getHandlerNames().contains(addressName)) {
                final HandlerConfiguration configuration = logContextConfiguration.getHandlerConfiguration(addressName);
                if (LEVEL.getName().equals(attributeName)) {
                    handleProperty(LEVEL, context, value, logContextConfiguration, configuration, false);
                } else if (FILTER.getName().equals(attributeName)) {
                    // Filter should be replaced by the filter-spec in the super class
                    handleProperty(FILTER_SPEC, context, value, logContextConfiguration, configuration, false);
                } else if (FILTER_SPEC.getName().equals(attributeName)) {
                    handleProperty(FILTER_SPEC, context, value, logContextConfiguration, configuration, false);
                } else if (FORMATTER.getName().equals(attributeName)) {
                    handleProperty(FORMATTER, context, value, logContextConfiguration, configuration, false);
                } else if (ENCODING.getName().equals(attributeName)) {
                    handleProperty(ENCODING, context, value, logContextConfiguration, configuration, false);
                } else if (SUBHANDLERS.getName().equals(attributeName)) {
                    handleProperty(SUBHANDLERS, context, value, logContextConfiguration, configuration, false);
                } else if (PROPERTIES.getName().equals(attributeName)) {
                    for (Property property : value.asPropertyList()) {
                        configuration.setPropertyValueString(property.getName(), property.getValue().asString());
                    }
                } else {
                    for (AttributeDefinition attribute : getAttributes()) {
                        if (attribute.getName().equals(attributeName)) {
                            handleProperty(attribute, context, value, logContextConfiguration, configuration, false);
                            restartRequired = Logging.requiresRestart(attribute.getFlags());
                            break;
                        }
                    }
                }
            }
            return restartRequired;
        }
    }

    /**
     * A step handler to remove a handler
     */
    public static HandlerUpdateOperationStepHandler CHANGE_LEVEL = new HandlerUpdateOperationStepHandler(LEVEL);

    /**
     * A step handler to remove a handler
     */
    public static LoggingRemoveOperationStepHandler REMOVE_HANDLER = new LoggingRemoveOperationStepHandler() {

        @Override
        public void performRemove(final OperationContext context, final ModelNode operation, final LogContextConfiguration logContextConfiguration, final String name, final ModelNode model) throws OperationFailedException {
            context.removeResource(PathAddress.EMPTY_ADDRESS);
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LogContextConfiguration logContextConfiguration, final String name, final ModelNode model) throws OperationFailedException {
            // Validating wouldn't work until the LogContextConfiguration.commit() happens as handlers could still be
            // named as attached removed the check for this reason.

            // Remove the handler
            logContextConfiguration.removeHandlerConfiguration(name);
            // Remove the formatter if there is one
            if (logContextConfiguration.getFormatterNames().contains(name)) {
                logContextConfiguration.removeFormatterConfiguration(name);
            }
        }
    };

    /**
     * The handler for adding a subhandler to an {@link org.jboss.logmanager.handlers.AsyncHandler}.
     */
    public static final HandlerUpdateOperationStepHandler ADD_SUBHANDLER = new HandlerUpdateOperationStepHandler() {
        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            HANDLER_NAME.validateAndSet(operation, model);
            model.get(SUBHANDLERS.getName()).add(operation.get(HANDLER_NAME.getName()));
        }

        @Override
        public void performRuntime(final OperationContext context, final HandlerConfiguration configuration, final String name, final ModelNode model) throws OperationFailedException {
            // Get the handler name
            final String handlerName = HANDLER_NAME.resolveModelAttribute(context, model).asString();
            if (name.equals(handlerName)) {
                throw createOperationFailure(LoggingMessages.MESSAGES.cannotAddHandlerToSelf(configuration.getName()));
            }
            if (configuration.getHandlerNames().contains(handlerName)) {
                throw createOperationFailure(LoggingMessages.MESSAGES.handlerAlreadyDefined(handlerName));
            }
            configuration.addHandlerName(handlerName);
        }
    };

    /**
     * The handler for removing a subhandler to an {@link org.jboss.logmanager.handlers.AsyncHandler}.
     */
    public static final HandlerUpdateOperationStepHandler REMOVE_SUBHANDLER = new HandlerUpdateOperationStepHandler() {
        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            HANDLER_NAME.validateAndSet(operation, model);
            final String handlerName = model.get(HANDLER_NAME.getName()).asString();
            // Create a new handler list for the model
            boolean found = false;
            final List<ModelNode> handlers = model.get(SUBHANDLERS.getName()).asList();
            final List<ModelNode> newHandlers = new ArrayList<ModelNode>(handlers.size());
            for (ModelNode handler : handlers) {
                if (handlerName.equals(handler.asString())) {
                    found = true;
                } else {
                    newHandlers.add(handler);
                }
            }
            if (found) {
                model.get(SUBHANDLERS.getName()).set(newHandlers);
            }
        }

        @Override
        public void performRuntime(final OperationContext context, final HandlerConfiguration configuration, final String name, final ModelNode model) throws OperationFailedException {
            configuration.removeHandlerName(HANDLER_NAME.resolveModelAttribute(context, model).asString());
        }
    };

    /**
     * Changes the file for a file handler.
     */
    public static HandlerUpdateOperationStepHandler CHANGE_FILE = new HandlerUpdateOperationStepHandler(FILE);

    public static LoggingUpdateOperationStepHandler ENABLE_HANDLER = new LoggingUpdateOperationStepHandler() {
        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            // Nothing to do
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LogContextConfiguration configuration, final String name, final ModelNode model) throws OperationFailedException {
            enableHandler(configuration, name);
        }
    };

    public static LoggingUpdateOperationStepHandler DISABLE_HANDLER = new LoggingUpdateOperationStepHandler() {
        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            // Nothing to do
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LogContextConfiguration configuration, final String name, final ModelNode model) throws OperationFailedException {
            disableHandler(configuration, name);
        }
    };

    /**
     * Handle updating the configuration.
     *
     * @param attribute               the attribute definition
     * @param context                 the context of the operation
     * @param model                   the model to update
     * @param logContextConfiguration the log context configuration
     * @param configuration           the handler configuration
     *
     * @throws OperationFailedException if an error occurs
     */
    private static void handleProperty(final AttributeDefinition attribute, final OperationContext context, final ModelNode model,
                                       final LogContextConfiguration logContextConfiguration, final HandlerConfiguration configuration)
            throws OperationFailedException {
        handleProperty(attribute, context, model, logContextConfiguration, configuration, true);
    }

    /**
     * Handle updating the configuration.
     *
     * @param attribute               the attribute definition
     * @param context                 the context of the operation
     * @param model                   the model to update
     * @param logContextConfiguration the log context configuration
     * @param configuration           the handler configuration
     * @param resolveValue            {@code true} if the value should be resolved via the attribute, otherwise {@code
     *                                false} if the value is already resolved.
     *
     * @throws OperationFailedException if an error occurs
     */
    private static void handleProperty(final AttributeDefinition attribute, final OperationContext context, final ModelNode model,
                                       final LogContextConfiguration logContextConfiguration, final HandlerConfiguration configuration, final boolean resolveValue)
            throws OperationFailedException {
        if (attribute.getName().equals(ENABLED.getName())) {
            final boolean value = ((resolveValue ? ENABLED.resolveModelAttribute(context, model).asBoolean() : model.asBoolean()));
            if (value) {
                enableHandler(logContextConfiguration, configuration.getName());
            } else {
                disableHandler(logContextConfiguration, configuration.getName());
            }
        } else if (attribute.getName().equals(ENCODING.getName())) {
            final String resolvedValue = (resolveValue ? ENCODING.resolvePropertyValue(context, model) : model.asString());
            configuration.setEncoding(resolvedValue);
        } else if (attribute.getName().equals(FORMATTER.getName())) {
            final String formatterName = configuration.getName();
            final FormatterConfiguration fmtConfig;
            if (logContextConfiguration.getFormatterNames().contains(formatterName)) {
                fmtConfig = logContextConfiguration.getFormatterConfiguration(formatterName);
            } else {
                fmtConfig = logContextConfiguration.addFormatterConfiguration(null, PatternFormatter.class.getName(), formatterName, "pattern");
            }
            final String resolvedValue = (resolveValue ? FORMATTER.resolvePropertyValue(context, model) : model.asString());
            fmtConfig.setPropertyValueString("pattern", resolvedValue);
            configuration.setFormatterName(formatterName);
        } else if (attribute.getName().equals(FILTER_SPEC.getName())) {
            final ModelNode valueNode = (resolveValue ? FILTER_SPEC.resolveModelAttribute(context, model) : model);
            final String resolvedValue = (valueNode.isDefined() ? valueNode.asString() : null);
            configuration.setFilter(resolvedValue);
        } else if (attribute.getName().equals(LEVEL.getName())) {
            final String resolvedValue = (resolveValue ? LEVEL.resolvePropertyValue(context, model) : LEVEL.resolver().resolveValue(context, model));
            configuration.setLevel(resolvedValue);
        } else if (attribute.getName().equals(SUBHANDLERS.getName())) {
            final Collection<String> resolvedValue = (resolveValue ? SUBHANDLERS.resolvePropertyValue(context, model) : SUBHANDLERS.resolver().resolveValue(context, model));
            if (resolvedValue.contains(configuration.getName())) {
                throw createOperationFailure(LoggingMessages.MESSAGES.cannotAddHandlerToSelf(configuration.getName()));
            }
            configuration.setHandlerNames(resolvedValue);
        } else if (attribute.getName().equals(HANDLER_NAME.getName())) {
            // no-op just ignore the name attribute
        } else if (attribute.getName().equals(PROPERTIES.getName())) {
            if (model.hasDefined(PROPERTIES.getName())) {
                for (Property property : PROPERTIES.resolveModelAttribute(context, model).asPropertyList()) {
                    configuration.setPropertyValueString(property.getName(), property.getValue().asString());
                }
            }
        } else {
            if (attribute instanceof ConfigurationProperty<?>) {
                ((ConfigurationProperty<?>) attribute).setPropertyValue(context, model, configuration);
            } else {
                LoggingLogger.ROOT_LOGGER.invalidPropertyAttribute(attribute.getName());
            }
        }
    }

    /**
     * Compare the model value with the current value. If the model value equals the currently configured value {@code
     * true} is returned, otherwise {@code false}.
     *
     * @param attribute               the attribute definition
     * @param context                 the context of the operation
     * @param model                   the model to update
     * @param logContextConfiguration the log context configuration
     * @param configuration           the handler configuration
     *
     * @return {@code true} if the model value equals the current configured value, otherwise {@code false}
     *
     * @throws OperationFailedException if an error occurs
     */
    private static boolean equalValue(final AttributeDefinition attribute, final OperationContext context, final ModelNode model,
                                      final LogContextConfiguration logContextConfiguration, final HandlerConfiguration configuration)
            throws OperationFailedException {
        final boolean result;
        if (attribute.getName().equals(ENABLED.getName())) {
            final boolean resolvedValue = ENABLED.resolveModelAttribute(context, model).asBoolean();
            final boolean currentValue;
            if (configuration.hasProperty(ENABLED.getPropertyName())) {
                currentValue = Boolean.parseBoolean(configuration.getPropertyValueString(ENABLED.getPropertyName()));
            } else {
                currentValue = isDisabledHandler(logContextConfiguration.getLogContext(), configuration.getName());
            }
            result = resolvedValue == currentValue;
        } else if (attribute.getName().equals(ENCODING.getName())) {
            final String resolvedValue = ENCODING.resolvePropertyValue(context, model);
            final String currentValue = configuration.getEncoding();
            result = (resolvedValue == null ? currentValue == null : resolvedValue.equals(currentValue));
        } else if (attribute.getName().equals(FORMATTER.getName())) {
            final String formatterName = configuration.getName();
            final FormatterConfiguration fmtConfig;
            if (logContextConfiguration.getFormatterNames().contains(formatterName)) {
                fmtConfig = logContextConfiguration.getFormatterConfiguration(formatterName);
                final String resolvedValue = FORMATTER.resolvePropertyValue(context, model);
                final String currentValue = fmtConfig.getPropertyValueString("pattern");
                result = (resolvedValue == null ? currentValue == null : resolvedValue.equals(currentValue));
            } else {
                result = false;
            }
        } else if (attribute.getName().equals(FILTER_SPEC.getName())) {
            final ModelNode valueNode = FILTER_SPEC.resolveModelAttribute(context, model);
            final String resolvedValue = (valueNode.isDefined() ? valueNode.asString() : null);
            final String currentValue = configuration.getFilter();
            result = (resolvedValue == null ? currentValue == null : resolvedValue.equals(currentValue));
        } else if (attribute.getName().equals(LEVEL.getName())) {
            final String resolvedValue = LEVEL.resolvePropertyValue(context, model);
            final String currentValue = configuration.getLevel();
            result = (resolvedValue == null ? currentValue == null : resolvedValue.equals(configuration.getLevel()));
        } else if (attribute.getName().equals(SUBHANDLERS.getName())) {
            final Collection<String> resolvedValue = SUBHANDLERS.resolvePropertyValue(context, model);
            final Collection<String> currentValue = configuration.getHandlerNames();
            result = (resolvedValue == null ? currentValue == null : resolvedValue.containsAll(currentValue));
        } else if (attribute.getName().equals(PROPERTIES.getName())) {
            result = true;
            if (model.hasDefined(PROPERTIES.getName())) {
                for (Property property : PROPERTIES.resolveModelAttribute(context, model).asPropertyList()) {
                    final String resolvedValue = property.getValue().asString();
                    final String currentValue = configuration.getPropertyValueString(property.getName());
                    if (!(resolvedValue == null ? currentValue == null : resolvedValue.equals(currentValue))) {
                        return false;
                    }
                }
            }
        } else {
            if (attribute instanceof PropertyAttributeDefinition) {
                final PropertyAttributeDefinition propAttribute = ((PropertyAttributeDefinition) attribute);
                final String resolvedValue = propAttribute.resolvePropertyValue(context, model);
                final String currentValue = configuration.getPropertyValueString(propAttribute.getPropertyName());
                result = (resolvedValue == null ? currentValue == null : resolvedValue.equals(currentValue));
            } else {
                result = false;
            }
        }
        return result;
    }


    /**
     * Checks to see if a handler is disabled
     *
     * @param handlerName   the name of the handler to enable.
     */
    static boolean isDisabledHandler(final LogContext logContext, final String handlerName) {
        final Map<String, String> disableHandlers = logContext.getAttachment(CommonAttributes.ROOT_LOGGER_NAME, DISABLED_HANDLERS_KEY);
        return disableHandlers != null && disableHandlers.containsKey(handlerName);
    }


    /**
     * Enables the handler if it was previously disabled.
     * <p/>
     * If it was not previously disable, nothing happens.
     *
     * @param configuration the log context configuration.
     * @param handlerName   the name of the handler to enable.
     */
    static void enableHandler(final LogContextConfiguration configuration, final String handlerName) {
        final HandlerConfiguration handlerConfiguration = configuration.getHandlerConfiguration(handlerName);
        try {
            handlerConfiguration.setPropertyValueString("enabled", "true");
            return;
        } catch (IllegalArgumentException e) {
            // do nothing
        }
        final Map<String, String> disableHandlers = configuration.getLogContext().getAttachment(CommonAttributes.ROOT_LOGGER_NAME, DISABLED_HANDLERS_KEY);
        if (disableHandlers != null && disableHandlers.containsKey(handlerName)) {
            synchronized (HANDLER_LOCK) {
                final String filter = disableHandlers.get(handlerName);
                handlerConfiguration.setFilter(filter);
                disableHandlers.remove(handlerName);
            }
        }
    }

    /**
     * Disables the handler if the handler exists and is not already disabled.
     * <p/>
     * If the handler does not exist or is already disabled nothing happens.
     *
     * @param configuration the log context configuration.
     * @param handlerName   the handler name to disable.
     */
    static void disableHandler(final LogContextConfiguration configuration, final String handlerName) {
        final HandlerConfiguration handlerConfiguration = configuration.getHandlerConfiguration(handlerName);
        try {
            handlerConfiguration.setPropertyValueString("enabled", "false");
            return;
        } catch (IllegalArgumentException e) {
            // do nothing
        }
        final Logger root = configuration.getLogContext().getLogger(CommonAttributes.ROOT_LOGGER_NAME);
        Map<String, String> disableHandlers = root.getAttachment(DISABLED_HANDLERS_KEY);
        synchronized (HANDLER_LOCK) {
            if (disableHandlers == null) {
                disableHandlers = new HashMap<String, String>();
                final Map<String, String> current = root.attachIfAbsent(DISABLED_HANDLERS_KEY, disableHandlers);
                if (current != null) {
                    disableHandlers = current;
                }
            }
            if (!disableHandlers.containsKey(handlerName)) {
                disableHandlers.put(handlerName, handlerConfiguration.getFilter());
                handlerConfiguration.setFilter(CommonAttributes.DENY.getName());
            }
        }
    }

}
