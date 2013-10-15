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

import static org.jboss.as.logging.AbstractHandlerDefinition.FORMATTER;
import static org.jboss.as.logging.AbstractHandlerDefinition.NAMED_FORMATTER;
import static org.jboss.as.logging.AsyncHandlerResourceDefinition.QUEUE_LENGTH;
import static org.jboss.as.logging.AsyncHandlerResourceDefinition.SUBHANDLERS;
import static org.jboss.as.logging.CommonAttributes.ENABLED;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FILTER_SPEC;
import static org.jboss.as.logging.CommonAttributes.HANDLER_NAME;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CommonAttributes.MODULE;
import static org.jboss.as.logging.CommonAttributes.PROPERTIES;
import static org.jboss.as.logging.Logging.createOperationFailure;
import static org.jboss.as.logging.PatternFormatterResourceDefinition.PATTERN;
import static org.jboss.as.logging.PatternFormatterResourceDefinition.PATTERN_FORMATTER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;

import org.apache.log4j.Appender;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultHandler;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.logging.logmanager.Log4jAppenderHandler;
import org.jboss.as.logging.logmanager.PropertySorter;
import org.jboss.as.logging.resolvers.ModelNodeResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.Logger.AttachmentKey;
import org.jboss.logmanager.config.FormatterConfiguration;
import org.jboss.logmanager.config.HandlerConfiguration;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.jboss.logmanager.config.PojoConfiguration;
import org.jboss.logmanager.config.PropertyConfigurable;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

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
    static class HandlerUpdateOperationStepHandler extends LoggingOperations.LoggingUpdateOperationStepHandler {
        private final AttributeDefinition[] attributes;
        private final PropertySorter propertySorter;

        protected HandlerUpdateOperationStepHandler() {
            this(PropertySorter.NO_OP);
        }

        protected HandlerUpdateOperationStepHandler(final PropertySorter propertySorter, final AttributeDefinition... attributes) {
            this.attributes = attributes;
            this.propertySorter = propertySorter;
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
                    // Only update the model for attributes that are defined in the operation
                    if (operation.has(attribute.getName())) {
                        attribute.validateAndSet(operation, model);
                    }
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
                    // Only update if the attribute is on the operation
                    if (operation.has(attribute.getName())) {
                        handleProperty(attribute, context, model, logContextConfiguration, configuration);
                        restartRequired = restartRequired || Logging.requiresRestart(attribute.getFlags());
                        reloadRequired = reloadRequired || Logging.requiresReload(attribute.getFlags());
                    }
                }
                if (restartRequired) {
                    context.restartRequired();
                } else if (reloadRequired) {
                    context.reloadRequired();
                }
            }
            performRuntime(context, configuration, name, model);

            // It's important that properties are written in the correct order, reorder the properties if
            // needed before the commit.
            addOrderPropertiesStep(context, propertySorter, configuration);
        }

        public void performRuntime(final OperationContext context, final HandlerConfiguration configuration, final String name, final ModelNode model) throws OperationFailedException {
            // No-op by default
        }
    }

    /**
     * A step handler for add operations of logging handlers. Adds default properties to the handler configuration.
     */
    static class HandlerAddOperationStepHandler extends LoggingOperations.LoggingAddOperationStepHandler {
        private final String[] constructionProperties;
        private final AttributeDefinition[] attributes;
        private final Class<? extends Handler> type;
        private final PropertySorter propertySorter;

        protected HandlerAddOperationStepHandler(final Class<? extends Handler> type, final AttributeDefinition[] attributes) {
            this.type = type;
            this.constructionProperties = null;
            this.attributes = attributes;
            this.propertySorter = PropertySorter.NO_OP;
        }

        protected HandlerAddOperationStepHandler(final PropertySorter propertySorter, final Class<? extends Handler> type, final AttributeDefinition[] attributes, final ConfigurationProperty<?>... constructionProperties) {
            this.type = type;
            this.attributes = attributes;
            final List<String> names = new ArrayList<String>();
            for (ConfigurationProperty<?> prop : constructionProperties) {
                names.add(prop.getPropertyName());
            }
            this.constructionProperties = names.toArray(new String[names.size()]);
            this.propertySorter = propertySorter;
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

            HandlerConfiguration configuration = logContextConfiguration.getHandlerConfiguration(name);
            boolean replaceHandler = false;
            final boolean exists = configuration != null;
            if (!exists) {
                LoggingLogger.ROOT_LOGGER.tracef("Adding handler '%s' at '%s'", name, LoggingOperations.getAddress(operation));
                configuration = createHandlerConfiguration(className, moduleName, name, logContextConfiguration);
            } else if (Log4jAppenderHandler.class.getName().equals(configuration.getClassName())) {
                // Check the POJO names
                final PojoConfiguration log4jPojo = logContextConfiguration.getPojoConfiguration(name);
                replaceHandler = (log4jPojo != null && !className.equals(log4jPojo.getClassName()) || (moduleName == null ? log4jPojo.getModuleName() != null : !moduleName.equals(log4jPojo.getModuleName())));
            } else if (!className.equals(configuration.getClassName()) || (moduleName == null ? configuration.getModuleName() != null : !moduleName.equals(configuration.getModuleName()))) {
                replaceHandler = true;
            }

            if (replaceHandler) {
                LoggingLogger.ROOT_LOGGER.replacingNamedHandler(name);
                LoggingLogger.ROOT_LOGGER.debugf("Removing handler %s of type '%s' in module '%s' and replacing with type '%s' in module '%s'",
                        name, configuration.getClassName(), configuration.getModuleName(), className, moduleName);
                // Remove the original configuration and set-up the new one. This overrides anything in the original
                // configuration, e.g. the logging.properties configuration, with the model configuration.
                logContextConfiguration.removeHandlerConfiguration(name);
                // Remove POJO if it exists
                if (logContextConfiguration.getPojoNames().contains(name)) {
                    logContextConfiguration.removePojoConfiguration(name);
                }
                configuration = createHandlerConfiguration(className, moduleName, name, logContextConfiguration);
            }

            for (AttributeDefinition attribute : attributes) {
                // CLASS and MODULE should be ignored
                final boolean skip;
                if ((attribute.equals(CLASS) || attribute.equals(MODULE)) || attribute.equals(FILTER)) {
                    skip = true;
                } else {
                    // No need to change values that are equal, also values like a file name that are equal could result
                    // already logged data being overwritten
                    skip = (exists && equalValue(attribute, context, model, logContextConfiguration, configuration));
                }

                if (!skip)
                    handleProperty(attribute, context, model, logContextConfiguration, configuration);
            }

            // It's important that properties are written in the correct order, reorder the properties if
            // needed before the commit.
            addOrderPropertiesStep(context, propertySorter, configuration);
        }

        protected HandlerConfiguration createHandlerConfiguration(final String className,
                                                                  final String moduleName, final String name,
                                                                  final LogContextConfiguration logContextConfiguration) throws OperationFailedException {

            final HandlerConfiguration configuration;

            if (moduleName != null) {
                // Check if this is a log4j appender
                final ModuleLoader moduleLoader = ModuleLoader.forClass(HandlerOperations.class);
                final ModuleIdentifier id = ModuleIdentifier.create(moduleName);
                try {
                    final Class<?> actualClass = Class.forName(className, false, moduleLoader.loadModule(id).getClassLoader());
                    if (Appender.class.isAssignableFrom(actualClass)) {
                        // Check for construction parameters
                        if (constructionProperties == null) {
                            logContextConfiguration.addPojoConfiguration(moduleName, className, name);
                        } else {
                            logContextConfiguration.addPojoConfiguration(moduleName, className, name, constructionProperties);
                        }
                        configuration = logContextConfiguration.addHandlerConfiguration("org.jboss.as.logging", Log4jAppenderHandler.class.getName(), name);
                        configuration.addPostConfigurationMethod("activate");
                        configuration.setPropertyValueString("appender", name);
                    } else {
                        // Check for construction parameters
                        if (constructionProperties == null) {
                            configuration = logContextConfiguration.addHandlerConfiguration(moduleName, className, name);
                        } else {
                            configuration = logContextConfiguration.addHandlerConfiguration(moduleName, className, name, constructionProperties);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw createOperationFailure(LoggingMessages.MESSAGES.classNotFound(e, className));
                } catch (ModuleLoadException e) {
                    throw LoggingMessages.MESSAGES.cannotLoadModule(e, moduleName, "handler", name);
                }
            } else {
                // Check for construction parameters
                if (constructionProperties == null) {
                    configuration = logContextConfiguration.addHandlerConfiguration(moduleName, className, name);
                } else {
                    configuration = logContextConfiguration.addHandlerConfiguration(moduleName, className, name, constructionProperties);
                }
            }
            return configuration;
        }
    }

    /**
     * A default log handler write attribute step handler.
     */
    static class LogHandlerWriteAttributeHandler extends LoggingOperations.LoggingWriteAttributeHandler {
        private final PropertySorter propertySorter;

        protected LogHandlerWriteAttributeHandler(final AttributeDefinition... attributes) {
            this(PropertySorter.NO_OP, attributes);
        }

        protected LogHandlerWriteAttributeHandler(final PropertySorter propertySorter, final AttributeDefinition... attributes) {
            super(attributes);
            this.propertySorter = propertySorter;
        }

        @Override
        protected boolean applyUpdate(final OperationContext context, final String attributeName, final String addressName, final ModelNode value, final LogContextConfiguration logContextConfiguration) throws OperationFailedException {
            boolean restartRequired = false;
            if (logContextConfiguration.getHandlerNames().contains(addressName)) {
                final HandlerConfiguration configuration = logContextConfiguration.getHandlerConfiguration(addressName);
                if (LEVEL.getName().equals(attributeName)) {
                    handleProperty(LEVEL, context, value, logContextConfiguration, configuration, false);
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
                    final PropertyConfigurable propertyConfigurable;
                    // A POJO configuration will have the same name as the handler
                    final PojoConfiguration pojoConfiguration = logContextConfiguration.getPojoConfiguration(configuration.getName());
                    if (pojoConfiguration == null) {
                        propertyConfigurable = configuration;
                    } else {
                        propertyConfigurable = pojoConfiguration;
                    }
                    if (value.isDefined()) {
                        for (Property property : value.asPropertyList()) {
                            propertyConfigurable.setPropertyValueString(property.getName(), property.getValue().asString());
                        }
                    } else {
                        final List<String> propertyNames = propertyConfigurable.getPropertyNames();
                        for (String propertyName : propertyNames) {
                            // Ignore the enable attribute if found
                            if (propertyName.equals("enabled")) continue;
                            propertyConfigurable.removeProperty(propertyName);
                            // Set to restart required if undefining properties
                            restartRequired = true;
                        }
                    }
                } else if (QUEUE_LENGTH.getName().equals(attributeName)) {
                    // queue-length is a construction parameter, runtime changes are not allowed
                    restartRequired = true;
                } else {
                    for (AttributeDefinition attribute : getAttributes()) {
                        if (attribute.getName().equals(attributeName)) {
                            handleProperty(attribute, context, value, logContextConfiguration, configuration, false);
                            restartRequired = Logging.requiresRestart(attribute.getFlags());
                            break;
                        }
                    }
                }

                // It's important that properties are written in the correct order, reorder the properties if
                // needed before the commit.
                addOrderPropertiesStep(context, propertySorter, configuration);
            }
            return restartRequired;
        }

        @Override
        protected void finishModelStage(final OperationContext context, final ModelNode operation, final String attributeName,
                                        final ModelNode newValue, final ModelNode oldValue, final Resource model) throws OperationFailedException {
            super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
            // If a filter attribute, update the filter-spec attribute
            if (CommonAttributes.FILTER.getName().equals(attributeName)) {
                final String filterSpec = Filters.filterToFilterSpec(newValue);
                final ModelNode filterSpecValue = (filterSpec == null ? new ModelNode() : new ModelNode(filterSpec));
                // Undefine the filter-spec
                model.getModel().get(CommonAttributes.FILTER_SPEC.getName()).set(filterSpecValue);
            }

            // Undefine formatter attribute if writing a named-formatter
            if (AbstractHandlerDefinition.NAMED_FORMATTER.getName().equals(attributeName)) {
                // If the formatter is defined in the model, remove it
                final ModelNode m = model.getModel();
                if (m.hasDefined(AbstractHandlerDefinition.FORMATTER.getName())) {
                    m.get(AbstractHandlerDefinition.FORMATTER.getName()).clear();
                }
            } else if (AbstractHandlerDefinition.FORMATTER.getName().equals(attributeName)) {
                // If the named-formatter is defined in the model, remove it
                final ModelNode m = model.getModel();
                if (m.hasDefined(AbstractHandlerDefinition.NAMED_FORMATTER.getName())) {
                    m.get(AbstractHandlerDefinition.NAMED_FORMATTER.getName()).clear();
                }
            }
        }
    }

    /**
     * A step handler to remove a handler
     */
    static final OperationStepHandler CHANGE_LEVEL = new HandlerUpdateOperationStepHandler(PropertySorter.NO_OP, LEVEL);

    /**
     * A step handler to remove a handler
     */
    static final OperationStepHandler REMOVE_HANDLER = new LoggingOperations.LoggingRemoveOperationStepHandler() {

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
            // Remove the POJO if it exists
            if (logContextConfiguration.getPojoNames().contains(name)) {
                logContextConfiguration.removePojoConfiguration(name);
            }
        }
    };

    /**
     * The handler for adding a subhandler to an {@link org.jboss.logmanager.handlers.AsyncHandler}.
     */
    static final OperationStepHandler ADD_SUBHANDLER = new HandlerUpdateOperationStepHandler() {
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
    static final OperationStepHandler REMOVE_SUBHANDLER = new HandlerUpdateOperationStepHandler() {
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
    static final OperationStepHandler CHANGE_FILE = new HandlerUpdateOperationStepHandler(PropertySorter.NO_OP, FILE);

    static final LoggingOperations.LoggingUpdateOperationStepHandler ENABLE_HANDLER = new LoggingOperations.LoggingUpdateOperationStepHandler() {
        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            // Set the enable attribute to true
            model.get(CommonAttributes.ENABLED.getName()).set(true);
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LogContextConfiguration configuration, final String name, final ModelNode model) throws OperationFailedException {
            enableHandler(configuration, name);
        }
    };

    static final LoggingOperations.LoggingUpdateOperationStepHandler DISABLE_HANDLER = new LoggingOperations.LoggingUpdateOperationStepHandler() {
        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            // Set the enable attribute to false
            model.get(CommonAttributes.ENABLED.getName()).set(false);
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
            final String resolvedValue = (resolveValue ? ENCODING.resolvePropertyValue(context, model) : model.isDefined() ? model.asString() : null);
            configuration.setEncoding(resolvedValue);
        } else if (attribute.getName().equals(FORMATTER.getName())) {
            final String formatterName = configuration.getName();
            // Use a formatter only if a named-formatter is not defined, note too that if explicitly undefining the named-formatter
            // the formatter pattern will be used
            if (model.hasDefined(NAMED_FORMATTER.getName())) {
                final ModelNode valueNode = (resolveValue ? NAMED_FORMATTER.resolveModelAttribute(context, model) : model);
                final String resolvedValue = (valueNode.isDefined() ? valueNode.asString() : null);
                configuration.setFormatterName(resolvedValue);
                // Check the current formatter name, if it's the same name as the handler, remove the old formatter
                if (!formatterName.equals(resolvedValue) && logContextConfiguration.getFormatterNames().contains(formatterName)) {
                    logContextConfiguration.removeFormatterConfiguration(formatterName);
                }
            } else {
                // Use a formatter only if a named-formatter is not defined or the named-formatter was explicitly undefined
                final FormatterConfiguration fmtConfig;
                if (logContextConfiguration.getFormatterNames().contains(formatterName)) {
                    fmtConfig = logContextConfiguration.getFormatterConfiguration(formatterName);
                } else {
                    fmtConfig = logContextConfiguration.addFormatterConfiguration(null, PatternFormatter.class.getName(), formatterName, PATTERN.getPropertyName());
                }
                final String resolvedValue = (resolveValue ? FORMATTER.resolvePropertyValue(context, model) : model.asString());
                fmtConfig.setPropertyValueString(PATTERN.getPropertyName(), resolvedValue);
                configuration.setFormatterName(formatterName);
            }
        } else if (attribute.getName().equals(NAMED_FORMATTER.getName())) {
            final String formatterName = configuration.getName();
            final ModelNode valueNode = (resolveValue ? NAMED_FORMATTER.resolveModelAttribute(context, model) : model);
            // If the value not is undefined, this may have come from a undefine-attribute operation
            if (valueNode.isDefined()) {
                final String resolvedValue = valueNode.asString();
                configuration.setFormatterName(resolvedValue);
                // Check the current formatter name, if it's the same name as the handler, remove the old formatter
                if (!formatterName.equals(resolvedValue) && logContextConfiguration.getFormatterNames().contains(formatterName)) {
                    logContextConfiguration.removeFormatterConfiguration(formatterName);
                }
            } else {
                // If the current formatter name already equals the name defined in the configuration, there is no need to process
                if (!formatterName.equals(configuration.getFormatterName())) {
                    // Use a formatter only if a named-formatter is not defined or the named-formatter was explicitly undefined
                    final FormatterConfiguration fmtConfig;
                    if (logContextConfiguration.getFormatterNames().contains(formatterName)) {
                        fmtConfig = logContextConfiguration.getFormatterConfiguration(formatterName);
                    } else {
                        fmtConfig = logContextConfiguration.addFormatterConfiguration(null, PatternFormatter.class.getName(), formatterName, PATTERN.getPropertyName());
                    }
                    fmtConfig.setPropertyValueString(PATTERN.getPropertyName(), FORMATTER.resolvePropertyValue(context, model));
                    configuration.setFormatterName(formatterName);
                }
            }
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
            final PropertyConfigurable propertyConfigurable;
            // A POJO configuration will have the same name as the handler
            final PojoConfiguration pojoConfiguration = logContextConfiguration.getPojoConfiguration(configuration.getName());
            if (pojoConfiguration == null) {
                propertyConfigurable = configuration;
            } else {
                propertyConfigurable = pojoConfiguration;
            }
            // Should be safe here to only process defined properties. The write-attribute handler handles removing
            // undefined properties
            if (model.hasDefined(PROPERTIES.getName())) {
                final ModelNode resolvedValue = (resolveValue ? PROPERTIES.resolveModelAttribute(context, model) : model);
                for (Property property : resolvedValue.asPropertyList()) {
                    propertyConfigurable.setPropertyValueString(property.getName(), property.getValue().asString());
                }
            }
        } else {
            if (attribute instanceof ConfigurationProperty) {
                @SuppressWarnings("unchecked")
                final ConfigurationProperty<String> configurationProperty = (ConfigurationProperty<String>) attribute;
                if (resolveValue) {
                    configurationProperty.setPropertyValue(context, model, configuration);
                } else {
                    // Get the resolver
                    final ModelNodeResolver<String> resolver = configurationProperty.resolver();
                    // Resolve the value
                    final String resolvedValue = (resolver == null ? model.asString() : resolver.resolveValue(context, model));
                    // Set the string value
                    configuration.setPropertyValueString(configurationProperty.getPropertyName(),
                            (resolvedValue == null ? null : resolvedValue));
                }
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
            // Only check the pattern if the name matches the currently configured name
            if (formatterName.equals(configuration.getFormatterNameValueExpression().getResolvedValue())) {
                final FormatterConfiguration fmtConfig;
                if (logContextConfiguration.getFormatterNames().contains(formatterName)) {
                    fmtConfig = logContextConfiguration.getFormatterConfiguration(formatterName);
                    final String resolvedValue = FORMATTER.resolvePropertyValue(context, model);
                    final String currentValue = fmtConfig.getPropertyValueString(PATTERN_FORMATTER.getName());
                    result = (resolvedValue == null ? currentValue == null : resolvedValue.equals(currentValue));
                } else {
                    result = false;
                }
            } else {
                result = false;
            }
        } else if (attribute.getName().equals(NAMED_FORMATTER.getName())) {
            final ModelNode valueNode = NAMED_FORMATTER.resolveModelAttribute(context, model);
            final String resolvedValue = (valueNode.isDefined() ? valueNode.asString() : null);
            final String currentValue = configuration.getFormatterName();
            result = (resolvedValue == null ? currentValue == null : resolvedValue.equals(currentValue));
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
            final PropertyConfigurable propertyConfigurable;
            // A POJO configuration will have the same name as the handler
            final PojoConfiguration pojoConfiguration = logContextConfiguration.getPojoConfiguration(configuration.getName());
            if (pojoConfiguration == null) {
                propertyConfigurable = configuration;
            } else {
                propertyConfigurable = pojoConfiguration;
            }
            if (model.hasDefined(PROPERTIES.getName())) {
                for (Property property : PROPERTIES.resolveModelAttribute(context, model).asPropertyList()) {
                    final String resolvedValue = property.getValue().asString();
                    final String currentValue = propertyConfigurable.getPropertyValueString(property.getName());
                    if (!(resolvedValue == null ? currentValue == null : resolvedValue.equals(currentValue))) {
                        return false;
                    }
                }
            } else if (model.has(PROPERTIES.getName())) {
                final List<String> propertyNames = propertyConfigurable.getPropertyNames();
                for (String propertyName : propertyNames) {
                    final String propertyValue = propertyConfigurable.getPropertyValueString(propertyName);
                    if (propertyValue != null) {
                        return false;
                    }
                }
            }
        } else {
            if (attribute instanceof ConfigurationProperty) {
                final ConfigurationProperty<?> propAttribute = ((ConfigurationProperty<?>) attribute);
                final String resolvedValue = String.valueOf(propAttribute.resolvePropertyValue(context, model));
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
     * @param handlerName the name of the handler to enable.
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

    private static void addOrderPropertiesStep(final OperationContext context, final PropertySorter propertySorter, final PropertyConfigurable configuration) {
        if (propertySorter.isReorderRequired(configuration)) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    propertySorter.sort(configuration);
                    // Nothing to really rollback, properties are only reordered
                    context.completeStep(ResultHandler.NOOP_RESULT_HANDLER);
                }
            }, Stage.RUNTIME);
        }
    }
}
