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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.logging.logmanager.WildFlyLogContextSelector;
import org.jboss.as.logging.stdio.LogContextStdioContextSelector;
import org.jboss.logmanager.LogContext;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.stdio.StdioContext;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingExtension implements Extension {

    private static final String RESOURCE_NAME = LoggingExtension.class.getPackage().getName() + ".LocalDescriptions";

    static final String SUBSYSTEM_NAME = "logging";

    static final PathElement LOGGING_PROFILE_PATH = PathElement.pathElement(CommonAttributes.LOGGING_PROFILE);

    static final WildFlyLogContextSelector CONTEXT_SELECTOR = WildFlyLogContextSelector.Factory.create();

    static final GenericSubsystemDescribeHandler DESCRIBE_HANDLER = GenericSubsystemDescribeHandler.create(LoggingChildResourceComparator.INSTANCE);

    private static final int MANAGEMENT_API_MAJOR_VERSION = 2;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    private static final ModuleIdentifier[] LOGGING_API_MODULES = new ModuleIdentifier[] {
            ModuleIdentifier.create("org.apache.commons.logging"),
            ModuleIdentifier.create("org.apache.log4j"),
            ModuleIdentifier.create("org.jboss.logging"),
            ModuleIdentifier.create("org.jboss.logging.jul-to-slf4j-stub"),
            ModuleIdentifier.create("org.jboss.logmanager"),
            ModuleIdentifier.create("org.slf4j"),
            ModuleIdentifier.create("org.slf4j.ext"),
            ModuleIdentifier.create("org.slf4j.impl"),
    };

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new LoggingResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, LoggingExtension.class.getClassLoader());
    }

    @Override
    public void initialize(final ExtensionContext context) {
        // The logging subsystem requires JBoss Log Manager to be used
        // Testing the log manager must use the FQCN as the classes may be loaded via different class loaders
        if (!java.util.logging.LogManager.getLogManager().getClass().getName().equals(org.jboss.logmanager.LogManager.class.getName())) {
            throw LoggingMessages.MESSAGES.extensionNotInitialized();
        }
        LogContext.setLogContextSelector(CONTEXT_SELECTOR);
        // Install STDIO context selector
        StdioContext.setStdioContextSelector(new LogContextStdioContextSelector(StdioContext.getStdioContext()));

        // Load logging API modules
        try {
            final ModuleLoader moduleLoader = Module.forClass(LoggingExtension.class).getModuleLoader();
            for (ModuleIdentifier moduleIdentifier : LOGGING_API_MODULES) {
                try {
                    CONTEXT_SELECTOR.addLogApiClassLoader(moduleLoader.loadModule(moduleIdentifier).getClassLoader());
                } catch (Throwable ignore) {
                    // ignore
                }
            }
        } catch (Exception ignore) {
            // ignore
        }

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);

        final LoggingRootResource rootResource;
        final ResolvePathHandler resolvePathHandler;
        // Register for servers only even if in ADMIN_ONLY mode
        if (context.getProcessType().isServer()) {
            rootResource = new LoggingRootResource(context.getPathManager());
            resolvePathHandler = ResolvePathHandler.Builder.of(context.getPathManager())
                    .setParentAttribute(CommonAttributes.FILE)
                    .build();
        } else {
            rootResource = new LoggingRootResource(null);
            resolvePathHandler = null;
        }
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(rootResource);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, DESCRIBE_HANDLER);
        // Register root sub-models
        registerSubModels(registration, resolvePathHandler, true, subsystem, context.isRegisterTransformers());
        // Register logging profile sub-models
        ApplicationTypeConfig atc = new ApplicationTypeConfig(SUBSYSTEM_NAME, CommonAttributes.LOGGING_PROFILE);
        final List<AccessConstraintDefinition> accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
        ResourceDefinition profile = new SimpleResourceDefinition(LOGGING_PROFILE_PATH,
                getResourceDescriptionResolver(),
                LoggingProfileOperations.ADD_PROFILE,
                LoggingProfileOperations.REMOVE_PROFILE) {

            @Override
            public List<AccessConstraintDefinition> getAccessConstraints() {
                return accessConstraints;
            }
        };

        registerLoggingProfileSubModels(registration.registerSubModel(profile), resolvePathHandler);

        subsystem.registerXMLElementWriter(LoggingSubsystemWriter.INSTANCE);
    }


    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        for (Namespace namespace : Namespace.readable()) {
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUriString(), LoggingSubsystemParser.INSTANCE);
        }
    }

    private void registerLoggingProfileSubModels(final ManagementResourceRegistration registration, final ResolvePathHandler resolvePathHandler) {
        registerSubModels(registration, resolvePathHandler, false, null, false);
    }

    private void registerSubModels(final ManagementResourceRegistration registration, final ResolvePathHandler resolvePathHandler,
                                   final boolean includeLegacyAttributes, final SubsystemRegistration subsystem,
                                   final boolean registerTransformers) {
        final RootLoggerResourceDefinition rootLoggerResourceDefinition = new RootLoggerResourceDefinition(includeLegacyAttributes);
        registration.registerSubModel(rootLoggerResourceDefinition);

        final LoggerResourceDefinition loggerResourceDefinition = new LoggerResourceDefinition(includeLegacyAttributes);
        registration.registerSubModel(loggerResourceDefinition);

        final AsyncHandlerResourceDefinition asyncHandlerResourceDefinition = new AsyncHandlerResourceDefinition(includeLegacyAttributes);
        registration.registerSubModel(asyncHandlerResourceDefinition);

        final ConsoleHandlerResourceDefinition consoleHandlerResourceDefinition = new ConsoleHandlerResourceDefinition(includeLegacyAttributes);
        registration.registerSubModel(consoleHandlerResourceDefinition);

        final FileHandlerResourceDefinition fileHandlerResourceDefinition = new FileHandlerResourceDefinition(resolvePathHandler, includeLegacyAttributes);
        registration.registerSubModel(fileHandlerResourceDefinition);

        final PeriodicHandlerResourceDefinition periodicHandlerResourceDefinition = new PeriodicHandlerResourceDefinition(resolvePathHandler, includeLegacyAttributes);
        registration.registerSubModel(periodicHandlerResourceDefinition);

        final SizeRotatingHandlerResourceDefinition sizeRotatingHandlerResourceDefinition = new SizeRotatingHandlerResourceDefinition(resolvePathHandler, includeLegacyAttributes);
        registration.registerSubModel(sizeRotatingHandlerResourceDefinition);

        final CustomHandlerResourceDefinition customHandlerResourceDefinition = new CustomHandlerResourceDefinition(includeLegacyAttributes);
        registration.registerSubModel(customHandlerResourceDefinition);

        registration.registerSubModel(SyslogHandlerResourceDefinition.INSTANCE);
        registration.registerSubModel(PatternFormatterResourceDefinition.INSTANCE);
        registration.registerSubModel(CustomFormatterResourceDefinition.INSTANCE);

        if (registerTransformers) {
            for (KnownModelVersion modelVersion : KnownModelVersion.values()) {
                if (modelVersion.hasTransformers()) {
                    final ResourceTransformationDescriptionBuilder subsystemBuilder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
                    final ResourceTransformationDescriptionBuilder loggingProfileBuilder;
                    if (modelVersion == KnownModelVersion.VERSION_1_1_0) {
                        // Reject the profile
                        subsystemBuilder.rejectChildResource(LOGGING_PROFILE_PATH);
                        loggingProfileBuilder = null;
                    } else {
                        loggingProfileBuilder = subsystemBuilder.addChildResource(LOGGING_PROFILE_PATH);
                    }

                    // Register the transformers
                    rootLoggerResourceDefinition.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);
                    loggerResourceDefinition.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);
                    asyncHandlerResourceDefinition.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);
                    consoleHandlerResourceDefinition.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);
                    fileHandlerResourceDefinition.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);
                    periodicHandlerResourceDefinition.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);
                    sizeRotatingHandlerResourceDefinition.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);
                    customHandlerResourceDefinition.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);
                    SyslogHandlerResourceDefinition.INSTANCE.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);
                    PatternFormatterResourceDefinition.INSTANCE.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);
                    CustomFormatterResourceDefinition.INSTANCE.registerTransformers(modelVersion, subsystemBuilder, loggingProfileBuilder);

                    // Register the transformer description
                    TransformationDescription.Tools.register(subsystemBuilder.build(), subsystem, modelVersion.getModelVersion());
                }
            }
        }
    }

    private static class LoggingResourceDescriptionResolver extends StandardResourceDescriptionResolver {

        private static final Map<String, String> COMMON_ATTRIBUTE_NAMES = new HashMap<String, String>();

        static {
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.APPEND.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.AUTOFLUSH.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.CLASS.getName(), "logging.custom-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.ENABLED.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.ENCODING.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.FILE.getName(), "logging.handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.FILTER.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.FILTER_SPEC.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(AbstractHandlerDefinition.FORMATTER.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.HANDLERS.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.LEVEL.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(SizeRotatingHandlerResourceDefinition.MAX_BACKUP_INDEX.getName(), "logging.size-rotating-file-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.MODULE.getName(), "logging.custom-handler");
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.NAME.getName(), "logging.handler");
            COMMON_ATTRIBUTE_NAMES.put(AbstractHandlerDefinition.NAMED_FORMATTER.getName(), "logging.common");
            COMMON_ATTRIBUTE_NAMES.put(AsyncHandlerResourceDefinition.OVERFLOW_ACTION.getName(), "logging.async-handler");
            COMMON_ATTRIBUTE_NAMES.put(PathResourceDefinition.PATH.getName(), null);
            COMMON_ATTRIBUTE_NAMES.put(CommonAttributes.PROPERTIES.getName(), "logging.custom-handler");
            COMMON_ATTRIBUTE_NAMES.put(AsyncHandlerResourceDefinition.QUEUE_LENGTH.getName(), "logging.async-handler");
            COMMON_ATTRIBUTE_NAMES.put(PathResourceDefinition.RELATIVE_TO.getName(), null);
            COMMON_ATTRIBUTE_NAMES.put(SizeRotatingHandlerResourceDefinition.ROTATE_SIZE.getName(), "logging.size-rotating-file-handler");
            COMMON_ATTRIBUTE_NAMES.put(SizeRotatingHandlerResourceDefinition.ROTATE_ON_BOOT.getName(), "logging.size-rotating-file-handler");
            COMMON_ATTRIBUTE_NAMES.put(AsyncHandlerResourceDefinition.SUBHANDLERS.getName(), "logging.async-handler");
            COMMON_ATTRIBUTE_NAMES.put(PeriodicHandlerResourceDefinition.SUFFIX.getName(), "logging.periodic-rotating-file-handler");
            COMMON_ATTRIBUTE_NAMES.put(ConsoleHandlerResourceDefinition.TARGET.getName(), "logging.console-handler");
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
                    return bundle.getString(getVariableBundleKey(paramName, ModelDescriptionConstants.DEPRECATED));
                }
                return bundle.getString(getVariableBundleKey(operationName, paramName, ModelDescriptionConstants.DEPRECATED));
            }
            return super.getOperationParameterDeprecatedDescription(operationName, paramName, locale, bundle);
        }

        @Override
        public String getResourceAttributeDeprecatedDescription(final String attributeName, final Locale locale, final ResourceBundle bundle) {
            if (COMMON_ATTRIBUTE_NAMES.containsKey(attributeName)) {
                return bundle.getString(getVariableBundleKey(attributeName, ModelDescriptionConstants.DEPRECATED));
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
                } else if (RootLoggerResourceDefinition.ROOT_LOGGER_PATH_NAME.equals(key1)) {
                    result = GREATER;
                } else if (RootLoggerResourceDefinition.ROOT_LOGGER_PATH_NAME.equals(key2)) {
                    result = LESS;
                } else if (LoggerResourceDefinition.LOGGER.equals(key1)) {
                    result = GREATER;
                } else if (LoggerResourceDefinition.LOGGER.equals(key2)) {
                    result = LESS;
                } else if (AsyncHandlerResourceDefinition.ASYNC_HANDLER.equals(key1)) {
                    result = GREATER;
                } else if (AsyncHandlerResourceDefinition.ASYNC_HANDLER.equals(key2)) {
                    result = LESS;
                }
            }
            return result;
        }
    }

}
