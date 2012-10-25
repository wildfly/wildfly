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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPRECATED;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.AbstractOperationTransformer;
import org.jboss.as.controller.transform.AbstractSubsystemTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.ContextClassLoaderLogContextSelector;
import org.jboss.logmanager.LogContext;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingExtension implements Extension {

    private static final String RESOURCE_NAME = LoggingExtension.class.getPackage().getName() + ".LocalDescriptions";

    static final String SUBSYSTEM_NAME = "logging";

    static final PathElement LOGGING_PROFILE_PATH = PathElement.pathElement(CommonAttributes.LOGGING_PROFILE);


    static final ResourceDescriptionResolver HANDLER_ATTRIBUTE_RESOLVER = getResourceDescriptionResolver(CommonAttributes.HANDLER.getName());

    static final ContextClassLoaderLogContextSelector CONTEXT_SELECTOR = new ContextClassLoaderLogContextSelector();

    static final GenericSubsystemDescribeHandler DESCRIBE_HANDLER = GenericSubsystemDescribeHandler.create(LoggingChildResourceComparator.INSTANCE);

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;


    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new LoggingResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, LoggingExtension.class.getClassLoader());
    }

    @Override
    public void initialize(final ExtensionContext context) {
        LogContext.setLogContextSelector(CONTEXT_SELECTOR);
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(LoggingRootResource.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, DESCRIBE_HANDLER);
        registerSubModels(registration, true);
        registerSubModels(registration.registerSubModel(new SimpleResourceDefinition(LOGGING_PROFILE_PATH,
                getResourceDescriptionResolver(),
                LoggingProfileOperations.ADD_PROFILE,
                LoggingProfileOperations.REMOVE_PROFILE)), false);

        final TransformersSubRegistration reg = subsystem.registerModelTransformers(ModelVersion.create(1, 1, 0), new AbstractSubsystemTransformer(SUBSYSTEM_NAME) {
            @Override
            protected ModelNode transformModel(final TransformationContext context, final ModelNode model) {
                // Transform the filter expression string
                if (model.hasDefined(CommonAttributes.FILTER_SPEC.getName())) {
                    final String filterExpression = model.get(CommonAttributes.FILTER.getName()).asString();
                    model.get(CommonAttributes.FILTER.getName()).set(Filters.filterSpecToFilter(filterExpression));
                }
                return model;
            }
        });
        reg.registerSubResource(LOGGING_PROFILE_PATH, true);
        registerTransformersSubModels(reg);

        subsystem.registerXMLElementWriter(LoggingSubsystemParser.INSTANCE);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        for (Namespace namespace : Namespace.readable()) {
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUriString(), LoggingSubsystemParser.INSTANCE);
        }
    }

    private void registerSubModels(final ManagementResourceRegistration registration, final boolean includeLegacyAttributes) {
        registration.registerSubModel(new RootLoggerResourceDefinition(includeLegacyAttributes));
        registration.registerSubModel(new LoggerResourceDefinition(includeLegacyAttributes));
        registration.registerSubModel(new AsyncHandlerResourceDefinition(includeLegacyAttributes));
        registration.registerSubModel(new ConsoleHandlerResourceDefinition(includeLegacyAttributes));
        registration.registerSubModel(new FileHandlerResourceDefinition(includeLegacyAttributes));
        registration.registerSubModel(new PeriodicHandlerResourceDefinition(includeLegacyAttributes));
        registration.registerSubModel(new SizeRotatingHandlerResourceDefinition(includeLegacyAttributes));
        registration.registerSubModel(new CustomHandlerResourceDefinition(includeLegacyAttributes));
    }

    private void registerTransformersSubModels(final TransformersSubRegistration registration) {
        registerTransformersSubModels(registration, RootLoggerResourceDefinition.ROOT_LOGGER_PATH);
        registerTransformersSubModels(registration, LoggerResourceDefinition.LOGGER_PATH);
        registerTransformersSubModels(registration, AsyncHandlerResourceDefinition.ASYNC_HANDLER_PATH);
        registerTransformersSubModels(registration, ConsoleHandlerResourceDefinition.CONSOLE_HANDLER_PATH);
        registerTransformersSubModels(registration, FileHandlerResourceDefinition.FILE_HANDLER_PATH);
        registerTransformersSubModels(registration, PeriodicHandlerResourceDefinition.PERIODIC_HANDLER_PATH);
        registerTransformersSubModels(registration, SizeRotatingHandlerResourceDefinition.SIZE_ROTATING_HANDLER_PATH);
        registerTransformersSubModels(registration, CustomHandlerResourceDefinition.CUSTOM_HANDLE_PATH);
    }

    private void registerTransformersSubModels(final TransformersSubRegistration registration, final PathElement pathElement) {
        final TransformersSubRegistration reg = registration.registerSubResource(pathElement);
        reg.registerOperationTransformer(ADD, LoggingOperationTransformer.INSTANCE);
    }

    private static class LoggingOperationTransformer extends AbstractOperationTransformer {
        static final LoggingOperationTransformer INSTANCE = new LoggingOperationTransformer();

        @Override
        protected ModelNode transform(final TransformationContext context, final PathAddress address, final ModelNode operation) {
            final String name = address.getLastElement().getValue();
            if (CommonAttributes.LOGGER.equals(address.getLastElement().getKey())) {
                operation.get(CommonAttributes.CATEGORY.getName()).set(name);
            } else {
                operation.get(CommonAttributes.NAME.getName()).set(name);
            }
            if (operation.hasDefined(CommonAttributes.FILTER.getName())) {
                final String filterExpression = operation.get(CommonAttributes.FILTER.getName()).asString();
                operation.get(CommonAttributes.FILTER.getName()).set(Filters.filterSpecToFilter(filterExpression));
            }
            return operation;
        }
    }

    private static class LoggingResourceDescriptionResolver extends StandardResourceDescriptionResolver {

        private static final Map<String, String> COMMON_ATTRIBUTE_NAMES = new HashMap<String, String>();

        static {
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.APPEND.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.AUTOFLUSH.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.CLASS.getName(), "logging.custom-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.ENCODING.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.FILTER.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.FILTER_SPEC.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.FORMATTER.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.HANDLERS.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.LEVEL.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.MAX_BACKUP_INDEX.getName(), "logging.size-rotating-file-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.MODULE.getName(), "logging.custom-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.OVERFLOW_ACTION.getName(), "logging.async-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.PATH.getName(), null);
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.PROPERTIES.getName(), "logging.custom-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.QUEUE_LENGTH.getName(), "logging.async-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.RELATIVE_TO.getName(), null);
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.ROTATE_SIZE.getName(), "logging.size-rotating-file-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.SUBHANDLERS.getName(), "logging.async-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.SUFFIX.getName(), "logging.periodic-rotating-file-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.TARGET.getName(), "logging.console-handler");
        }

        public LoggingResourceDescriptionResolver(final String keyPrefix, final String bundleBaseName, final ClassLoader bundleLoader) {
            super(keyPrefix, bundleBaseName, bundleLoader, true, false);
        }

        @Override
        public String getResourceAttributeDescription(final String attributeName, final Locale locale, final ResourceBundle bundle) {
            if (COMMON_ATTRIBUTE_NAMES.containsKey(attributeName.split("\\.")[0])) {
                return bundle.getString(getBundleKey(attributeName));
            }
            return super.getResourceAttributeDescription(attributeName, locale, bundle);
        }

        @Override
        public String getResourceAttributeValueTypeDescription(final String attributeName, final Locale locale, final ResourceBundle bundle, final String... suffixes) {
            if (COMMON_ATTRIBUTE_NAMES.containsKey(attributeName)) {
                return bundle.getString(getVariableBundleKey(attributeName, suffixes));
            }
            return super.getResourceAttributeValueTypeDescription(attributeName, locale, bundle, suffixes);
        }

        @Override
        public String getOperationParameterDescription(final String operationName, final String paramName, final Locale locale, final ResourceBundle bundle) {
            if (COMMON_ATTRIBUTE_NAMES.containsKey(paramName)) {
                return bundle.getString(getBundleKey(paramName));
            }
            return super.getOperationParameterDescription(operationName, paramName, locale, bundle);
        }

        @Override
        public String getOperationParameterValueTypeDescription(final String operationName, final String paramName, final Locale locale, final ResourceBundle bundle, final String... suffixes) {
            if (COMMON_ATTRIBUTE_NAMES.containsKey(paramName)) {
                return bundle.getString(getVariableBundleKey(paramName, suffixes));
            }
            return super.getOperationParameterValueTypeDescription(operationName, paramName, locale, bundle, suffixes);
        }

        @Override
        public String getOperationParameterDeprecatedDescription(final String operationName, final String paramName, final Locale locale, final ResourceBundle bundle) {
            if (COMMON_ATTRIBUTE_NAMES.containsKey(paramName)) {
                if (isReuseAttributesForAdd()) {
                    return bundle.getString(getVariableBundleKey(paramName, DEPRECATED));
                }
                return bundle.getString(getVariableBundleKey(operationName, paramName, DEPRECATED));
            }
            return super.getOperationParameterDeprecatedDescription(operationName, paramName, locale, bundle);
        }

        @Override
        public String getResourceAttributeDeprecatedDescription(final String attributeName, final Locale locale, final ResourceBundle bundle) {
            if (COMMON_ATTRIBUTE_NAMES.containsKey(attributeName)) {
                return bundle.getString(getVariableBundleKey(attributeName, DEPRECATED));
            }
            return super.getResourceAttributeDeprecatedDescription(attributeName, locale, bundle);
        }


        private String getBundleKey(final String name) {
            return getVariableBundleKey(name);
        }

        private String getVariableBundleKey(final String name, final String... variable) {
            final String prefix = COMMON_ATTRIBUTE_NAMES.get(name.split("\\.")[0]);
            final StringBuilder sb;
            // Special handling for filter
            if (prefix == null) {
                sb = new StringBuilder(name);
            } else {
                sb = new StringBuilder(prefix).append('.').append(name);
            }
            if (variable != null) {
                for (String arg : variable) {
                    if (sb.length() > 0)
                        sb.append('.');
                    sb.append(arg);
                }
            }
            return sb.toString();
        }
    }

    public static class LoggingChildResourceComparator implements Comparator<PathElement> {
        static final LoggingChildResourceComparator INSTANCE = new LoggingChildResourceComparator();
        static final int GREATER = 1;
        static final int EQUAL = 0;
        static final int LESS = -1;

        @Override
        public int compare(final PathElement o1, final PathElement o2) {
            final String key1 = o1.getKey();
            final String key2 = o2.getKey();
            int result = key1.compareTo(key2);
            if (result != EQUAL) {
                if (ModelDescriptionConstants.SUBSYSTEM.equals(key1)) {
                    result = LESS;
                } else if (ModelDescriptionConstants.SUBSYSTEM.equals(key2)) {
                    result = GREATER;
                } else if (CommonAttributes.LOGGING_PROFILE.equals(key1)) {
                    result = LESS;
                } else if (CommonAttributes.LOGGING_PROFILE.equals(key2)) {
                    result = GREATER;
                } else if (CommonAttributes.ROOT_LOGGER.equals(key1)) {
                    result = GREATER;
                } else if (CommonAttributes.ROOT_LOGGER.equals(key2)) {
                    result = LESS;
                } else if (CommonAttributes.LOGGER.equals(key1)) {
                    result = GREATER;
                } else if (CommonAttributes.LOGGER.equals(key2)) {
                    result = LESS;
                } else if (CommonAttributes.ASYNC_HANDLER.equals(key1)) {
                    result = GREATER;
                } else if (CommonAttributes.ASYNC_HANDLER.equals(key2)) {
                    result = LESS;
                }
            }
            return result;
        }
    }
}
