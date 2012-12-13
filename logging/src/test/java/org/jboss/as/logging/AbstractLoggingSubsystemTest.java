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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.as.logging.resolvers.SizeResolver;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.config.FormatterConfiguration;
import org.jboss.logmanager.config.HandlerConfiguration;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.jboss.logmanager.config.LoggerConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractLoggingSubsystemTest extends AbstractSubsystemBaseTest {

    static final String[] HANDLER_RESOURCE_KEYS = {
            CommonAttributes.ASYNC_HANDLER,
            CommonAttributes.CONSOLE_HANDLER,
            CommonAttributes.CUSTOM_HANDLER,
            CommonAttributes.FILE_HANDLER,
            CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER,
            CommonAttributes.SIZE_ROTATING_FILE_HANDLER
    };

    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, LoggingExtension.SUBSYSTEM_NAME);

    static PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(SUBSYSTEM_PATH);

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    public AbstractLoggingSubsystemTest() {
        super(LoggingExtension.SUBSYSTEM_NAME, new LoggingExtension(), RemoveOperationComparator.INSTANCE);
    }

    @BeforeClass
    public static void setUp() {
        // Just need to set-up the test environment
        LoggingTestEnvironment.get();
    }

    @After
    public void clearLogContext() {
        clearLogContext(LogContext.getLogContext());
    }

    protected void clearLogContext(final LogContext logContext) {
        final ConfigurationPersistence configuration = ConfigurationPersistence.getConfigurationPersistence(logContext);
        if (configuration != null) {
            final LogContextConfiguration logContextConfiguration = configuration.getLogContextConfiguration();
            // Remove all loggers
            for (String loggerName : logContextConfiguration.getLoggerNames()) {
                logContextConfiguration.removeLoggerConfiguration(loggerName);
            }
            // Remove all the handlers
            for (String handlerName : logContextConfiguration.getHandlerNames()) {
                logContextConfiguration.removeHandlerConfiguration(handlerName);
            }
            // Remove all the filters
            for (String filterName : logContextConfiguration.getFilterNames()) {
                logContextConfiguration.removeFilterConfiguration(filterName);
            }
            // Remove all the formatters
            for (String formatterName : logContextConfiguration.getFormatterNames()) {
                logContextConfiguration.removeFormatterConfiguration(formatterName);
            }
            // Remove all the error managers
            for (String errorManager : logContextConfiguration.getErrorManagerNames()) {
                logContextConfiguration.removeErrorManagerConfiguration(errorManager);
            }
            configuration.commit();
        }
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return LoggingTestEnvironment.get();
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }

    static PathAddress createAddress(final String resourceKey, final String resourceName) {
        return PathAddress.pathAddress(
                SUBSYSTEM_PATH,
                PathElement.pathElement(resourceKey, resourceName)
        );
    }

    static PathAddress createAddress(final String profileName, final String resourceKey, final String resourceName) {
        if (profileName == null) {
            return createAddress(resourceKey, resourceName);
        }
        return PathAddress.pathAddress(
                SUBSYSTEM_PATH,
                PathElement.pathElement(CommonAttributes.LOGGING_PROFILE, profileName),
                PathElement.pathElement(resourceKey, resourceName)
        );
    }

    static PathAddress createRootLoggerAddress() {
        return createAddress(CommonAttributes.ROOT_LOGGER, CommonAttributes.ROOT_LOGGER_ATTRIBUTE_NAME);
    }

    static PathAddress createRootLoggerAddress(final String profileName) {
        return createAddress(profileName, CommonAttributes.ROOT_LOGGER, CommonAttributes.ROOT_LOGGER_ATTRIBUTE_NAME);
    }

    static PathAddress createLoggerAddress(final String name) {
        return createAddress(CommonAttributes.LOGGER, name);
    }

    static PathAddress createLoggerAddress(final String profileName, final String name) {
        return createAddress(profileName, CommonAttributes.LOGGER, name);
    }

    static PathAddress createConsoleHandlerAddress(final String name) {
        return createAddress(CommonAttributes.CONSOLE_HANDLER, name);
    }

    static PathAddress createConsoleHandlerAddress(final String profileName, final String name) {
        return createAddress(profileName, CommonAttributes.CONSOLE_HANDLER, name);
    }

    static PathAddress createFileHandlerAddress(final String name) {
        return createAddress(CommonAttributes.FILE_HANDLER, name);
    }

    static PathAddress createFileHandlerAddress(final String profileName, final String name) {
        return createAddress(profileName, CommonAttributes.FILE_HANDLER, name);
    }

    protected KernelServices boot() throws Exception {
        final KernelServices kernelServices = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml()).build();
        final Throwable bootError = kernelServices.getBootError();
        Assert.assertTrue("Failed to boot: " + String.valueOf(bootError), kernelServices.isSuccessfulBoot());
        return kernelServices;
    }

    protected void compare(final String profileName, final ModelNode node1, final ModelNode node2) {
        if (profileName == null) {
            compare(node1, node2);
        } else {
            Assert.assertTrue("Logging profile not found: " + profileName, node1.hasDefined(CommonAttributes.LOGGING_PROFILE));
            Assert.assertTrue("Logging profile not found: " + profileName, node1.get(CommonAttributes.LOGGING_PROFILE).hasDefined(profileName));
            Assert.assertTrue("Logging profile not found: " + profileName, node2.hasDefined(CommonAttributes.LOGGING_PROFILE));
            Assert.assertTrue("Logging profile not found: " + profileName, node2.get(CommonAttributes.LOGGING_PROFILE).hasDefined(profileName));
            final ModelNode node1Profile = node1.get(CommonAttributes.LOGGING_PROFILE, profileName);
            final ModelNode node2Profile = node2.get(CommonAttributes.LOGGING_PROFILE, profileName);
            compare(node1Profile, node2Profile);
        }
    }

    protected void compare(final ModelNode currentModel, final ConfigurationPersistence config) throws OperationFailedException {
        final LogContextConfiguration logContextConfig = config.getLogContextConfiguration();
        final List<String> handlerNames = logContextConfig.getHandlerNames();
        final List<String> modelHandlerNames = getHandlerNames(currentModel);
        final List<String> missingConfigHandlers = new ArrayList<String>(handlerNames);
        missingConfigHandlers.removeAll(modelHandlerNames);
        final List<String> missingModelHandlers = new ArrayList<String>(modelHandlerNames);
        missingModelHandlers.removeAll(handlerNames);

        Assert.assertTrue("Configuration contains handlers not in the model: " + missingConfigHandlers, missingConfigHandlers.isEmpty());
        Assert.assertTrue("Model contains handlers not in the configuration: " + missingModelHandlers, missingModelHandlers.isEmpty());

        // Compare property values for the handlers
        compareHandlers(logContextConfig, handlerNames, currentModel);

        // Compare logger values
        compareLoggers(logContextConfig, currentModel);

    }

    protected void compare(final String profileName, final ModelNode currentModel, final ConfigurationPersistence config) throws OperationFailedException {
        if (profileName == null) {
            compare(currentModel, config);
        } else {
            Assert.assertTrue("Logging profile not found: " + profileName, currentModel.hasDefined(CommonAttributes.LOGGING_PROFILE));
            Assert.assertTrue("Logging profile not found: " + profileName, currentModel.get(CommonAttributes.LOGGING_PROFILE).hasDefined(profileName));
            final ModelNode profileModel = currentModel.get(CommonAttributes.LOGGING_PROFILE, profileName);
            compare(profileModel, config);
        }
    }

    protected void compareLoggers(final LogContextConfiguration logContextConfiguration, final ModelNode model) {
        final List<String> loggerNames = logContextConfiguration.getLoggerNames();
        for (String name : loggerNames) {
            final LoggerConfiguration loggerConfig = logContextConfiguration.getLoggerConfiguration(name);
            final ModelNode loggerModel = (name.isEmpty() ? model.get(CommonAttributes.ROOT_LOGGER, CommonAttributes.ROOT_LOGGER_ATTRIBUTE_NAME) :
                    model.get(CommonAttributes.LOGGER, name));
            // Logger could be empty
            if (loggerModel.isDefined()) {
                final Set<String> attributes = loggerModel.keys();
                attributes.remove(CommonAttributes.CATEGORY.getName());
                attributes.remove(CommonAttributes.FILTER.getName());
                attributes.remove(CommonAttributes.NAME.getName());
                for (String attribute : attributes) {
                    if (attribute.equals(CommonAttributes.LEVEL.getName())) {
                        final String configValue = loggerConfig.getLevel();
                        final String modelValue = loggerModel.get(attribute).asString();
                        Assert.assertEquals(String.format("Levels do not match. Config Value: %s  Model Value: %s", configValue, modelValue), configValue, modelValue);
                    } else if (attribute.equals(CommonAttributes.FILTER_SPEC.getName())) {
                        final String configValue = loggerConfig.getFilter();
                        final String modelValue = loggerModel.hasDefined(attribute) ? loggerModel.get(attribute).asString() : null;
                        Assert.assertEquals(String.format("Filter expressions do not match. Config Value: %s  Model Value: %s", configValue, modelValue), configValue, modelValue);
                    } else if (attribute.equals(CommonAttributes.HANDLERS.getName())) {
                        final List<String> handlerNames = loggerConfig.getHandlerNames();
                        final ModelNode handlers = loggerModel.get(attribute);
                        if (handlers.isDefined()) {
                            final List<String> modelHandlerNames = new ArrayList<String>();
                            for (ModelNode handler : handlers.asList()) {
                                modelHandlerNames.add(handler.asString());
                            }
                            final List<String> missingConfigHandlers = new ArrayList<String>(handlerNames);
                            missingConfigHandlers.removeAll(modelHandlerNames);
                            final List<String> missingModelHandlers = new ArrayList<String>(modelHandlerNames);
                            missingModelHandlers.removeAll(handlerNames);
                            Assert.assertTrue("Logger in model contains handlers not in the configuration: " + missingConfigHandlers, missingConfigHandlers.isEmpty());
                            Assert.assertTrue("Logger in configuration contains handlers not in the model: " + missingModelHandlers, missingModelHandlers.isEmpty());
                        } else {
                            Assert.assertTrue("Handlers attached to loggers in the configuration that are not attached to loggers in the model. Logger: " + name, handlerNames.isEmpty());
                        }
                    } else if (attribute.equals(CommonAttributes.USE_PARENT_HANDLERS.getName())) {
                        final Boolean configValue = loggerConfig.getUseParentHandlers();
                        final Boolean modelValue = loggerModel.get(attribute).asBoolean();
                        Assert.assertEquals(String.format("Use parent handler attributes do not match. Config Value: %s  Model Value: %s", configValue, modelValue), configValue, modelValue);
                    } else {
                        // Invalid
                        Assert.assertTrue("Invalid attribute: " + attribute, false);
                    }
                }
            }
        }
    }

    protected void compareHandlers(final LogContextConfiguration logContextConfig, final Collection<String> handlerNames, final ModelNode model) throws OperationFailedException {
        // Compare property values for the handlers
        for (String name : handlerNames) {
            final HandlerConfiguration handlerConfig = logContextConfig.getHandlerConfiguration(name);
            final ModelNode handlerModel = findHandlerModel(model, name);
            final Set<String> modelPropertyNames = handlerModel.keys();
            final List<String> configPropertyNames = handlerConfig.getPropertyNames();

            // Remove unneeded properties
            modelPropertyNames.remove(CommonAttributes.FILTER.getName());
            modelPropertyNames.remove(CommonAttributes.NAME.getName());

            // Process the properties
            for (String modelPropertyName : modelPropertyNames) {
                final ModelNode modelValue = handlerModel.get(modelPropertyName);
                String modelStringValue = modelValue.asString();
                final String configValue;
                // Special properties
                if (modelPropertyName.equals(CommonAttributes.ENCODING.getName())) {
                    configValue = handlerConfig.getEncoding();
                } else if (modelPropertyName.equals(CommonAttributes.FORMATTER.getName())) {
                    final String formatterName = handlerConfig.getFormatterName();
                    if (formatterName == null) {
                        configValue = null;
                    } else {
                        final FormatterConfiguration formatterConfig = logContextConfig.getFormatterConfiguration(formatterName);
                        configValue = formatterConfig.getPropertyValueString(CommonAttributes.PATTERN.getName());
                    }
                } else if (modelPropertyName.equals(CommonAttributes.FILTER_SPEC.getName())) {
                    configValue = handlerConfig.getFilter();
                } else if (modelPropertyName.equals(CommonAttributes.LEVEL.getName())) {
                    configValue = handlerConfig.getLevel();
                } else {
                    // Process custom properties
                    final String configPropertyName;
                    if (modelPropertyName.equals(CommonAttributes.AUTOFLUSH.getName())) {
                        configPropertyName = CommonAttributes.AUTOFLUSH.getPropertyName();
                    } else if (modelPropertyName.equals(CommonAttributes.MAX_BACKUP_INDEX.getName())) {
                        configPropertyName = CommonAttributes.MAX_BACKUP_INDEX.getPropertyName();
                    } else if (modelPropertyName.equals(CommonAttributes.OVERFLOW_ACTION.getName())) {
                        configPropertyName = CommonAttributes.OVERFLOW_ACTION.getPropertyName();
                    } else if (modelPropertyName.equals(CommonAttributes.QUEUE_LENGTH.getName())) {
                        configPropertyName = CommonAttributes.QUEUE_LENGTH.getPropertyName();
                    } else if (modelPropertyName.equals(CommonAttributes.ROTATE_SIZE.getName())) {
                        configPropertyName = CommonAttributes.ROTATE_SIZE.getPropertyName();
                        modelStringValue = String.valueOf(SizeResolver.INSTANCE.parseSize(modelValue));
                    } else if (modelPropertyName.equals(CommonAttributes.USE_PARENT_HANDLERS.getName())) {
                        configPropertyName = CommonAttributes.USE_PARENT_HANDLERS.getPropertyName();
                    } else if (modelPropertyName.equals(CommonAttributes.FILE.getName())) {
                        configPropertyName = CommonAttributes.FILE.getPropertyName();
                        // Resolve the file
                        modelStringValue = modelValue.get(PathResourceDefinition.PATH.getName()).asString();
                        if (modelValue.hasDefined(PathResourceDefinition.RELATIVE_TO.getName())) {
                            final String relativeTo = System.getProperty(modelValue.get(PathResourceDefinition.RELATIVE_TO.getName()).asString());
                            modelStringValue = relativeTo + File.separator + modelStringValue;
                        }
                    } else if (modelPropertyName.equals(CommonAttributes.TARGET.getName())) {
                        configPropertyName = CommonAttributes.TARGET.getPropertyName();
                        modelStringValue = Target.fromString(modelValue.asString()).name();
                    } else if (modelPropertyName.equals(CommonAttributes.SUBHANDLERS.getName())) {
                        final List<String> handlerHandlerNames = handlerConfig.getHandlerNames();
                        final ModelNode handlers = handlerModel.get(modelPropertyName);
                        if (handlers.isDefined()) {
                            final List<String> modelHandlerNames = new ArrayList<String>();
                            for (ModelNode handler : handlers.asList()) {
                                modelHandlerNames.add(handler.asString());
                            }
                            final List<String> missingConfigHandlers = new ArrayList<String>(handlerHandlerNames);
                            missingConfigHandlers.removeAll(modelHandlerNames);
                            final List<String> missingModelHandlers = new ArrayList<String>(modelHandlerNames);
                            missingModelHandlers.removeAll(handlerHandlerNames);
                            Assert.assertTrue("Logger in model contains handlers not in the configuration: " + missingConfigHandlers, missingConfigHandlers.isEmpty());
                            Assert.assertTrue("Logger in configuration contains handlers not in the model: " + missingModelHandlers, missingModelHandlers.isEmpty());
                        } else {
                            Assert.assertTrue("Handlers attached to loggers in the configuration that are not attached to loggers in the model. Logger: " + name, handlerHandlerNames.isEmpty());
                        }
                        continue;
                    } else {
                        configPropertyName = modelPropertyName;
                    }

                    Assert.assertTrue("Configuration is missing property name: " + modelPropertyName, configPropertyNames.contains(configPropertyName));
                    configValue = handlerConfig.getPropertyValueString(configPropertyName);
                }
                if (configValue == null) {
                    Assert.assertFalse(String.format("Handler property values do not match.%nConfig Value: %s%nModel Value:  %s", configValue, modelValue), modelValue.isDefined());
                } else {
                    Assert.assertEquals(String.format("Handler property values do not match.%nConfig Value: %s%nModel Value:  %s", configValue, modelStringValue), configValue, modelStringValue);
                }
            }
        }
    }

    protected ModelNode findHandlerModel(final ModelNode model, final String name) {
        for (String handler : HANDLER_RESOURCE_KEYS) {
            if (model.hasDefined(handler)) {
                final ModelNode handlerModel = model.get(handler);
                if (handlerModel.hasDefined(name)) {
                    return handlerModel.get(name).clone();
                }
            }
        }
        return Operations.UNDEFINED;
    }

    protected List<String> getHandlerNames(final ModelNode currentModel) {
        final List<String> result = new ArrayList<String>();
        for (String handler : HANDLER_RESOURCE_KEYS) {
            if (currentModel.hasDefined(handler)) {
                result.addAll(currentModel.get(handler).keys());
            }
        }
        return result;
    }

    static List<String> modelNodeAsStringList(final ModelNode node) {
        if (node.getType() == ModelType.LIST) {
            final List<String> result = new ArrayList<String>();
            for (ModelNode n : node.asList()) result.add(n.asString());
            return result;
        }
        return Collections.emptyList();
    }

    static ModelNode getSubsystemModel(final KernelServices kernelServices) throws OperationFailedException {
        final ModelNode op = Operations.createReadResourceOperation(SUBSYSTEM_ADDRESS.toModelNode(), true);
        final ModelNode result = kernelServices.executeOperation(op);
        Assert.assertTrue(Operations.getFailureDescription(result), Operations.successful(result));
        return Operations.readResult(result);
    }

    static String resolveRelativePath(final KernelServices kernelServices, final String relativeTo) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(Operations.PATH, relativeTo));
        final ModelNode op = Operations.createReadAttributeOperation(address.toModelNode(), Operations.PATH);
        final ModelNode result = kernelServices.executeOperation(op);
        if (Operations.successful(result)) {
            return Operations.readResultAsString(result);
        }
        return null;
    }


    static class RemoveOperationComparator implements Comparator<PathAddress> {
        static final RemoveOperationComparator INSTANCE = new RemoveOperationComparator();
        static final int GREATER = 1;
        static final int EQUAL = 0;
        static final int LESS = -1;

        @Override
        public int compare(final PathAddress o1, final PathAddress o2) {
            final String key1 = o1.getLastElement().getKey();
            final String key2 = o2.getLastElement().getKey();
            int result = key1.compareTo(key2);
            if (result != EQUAL) {
                if (LoggingProfileOperations.isLoggingProfileAddress(o1) && !LoggingProfileOperations.isLoggingProfileAddress(o2)) {
                    result = GREATER;
                } else if (!LoggingProfileOperations.isLoggingProfileAddress(o1) && LoggingProfileOperations.isLoggingProfileAddress(o2)) {
                    result = LESS;
                } else if (LoggingProfileOperations.isLoggingProfileAddress(o1) && LoggingProfileOperations.isLoggingProfileAddress(o2)) {
                    if (CommonAttributes.LOGGING_PROFILE.equals(key1) && !CommonAttributes.LOGGING_PROFILE.equals(key2)) {
                        result = LESS;
                    } else if (!CommonAttributes.LOGGING_PROFILE.equals(key1) && CommonAttributes.LOGGING_PROFILE.equals(key2)) {
                        result = GREATER;
                    } else {
                        result = compare(key1, key2);
                    }
                } else {
                    result = compare(key1, key2);
                }
            }
            return result;
        }

        private int compare(final String key1, final String key2) {
            int result = EQUAL;
            if (ModelDescriptionConstants.SUBSYSTEM.equals(key1)) {
                result = LESS;
            } else if (ModelDescriptionConstants.SUBSYSTEM.equals(key2)) {
                result = GREATER;
            } else if (CommonAttributes.LOGGING_PROFILE.equals(key1)) {
                result = LESS;
            } else if (CommonAttributes.LOGGING_PROFILE.equals(key2)) {
                result = GREATER;
            } else if (CommonAttributes.ROOT_LOGGER.equals(key1)) {
                result = LESS;
            } else if (CommonAttributes.ROOT_LOGGER.equals(key2)) {
                result = GREATER;
            } else if (CommonAttributes.LOGGER.equals(key1)) {
                result = LESS;
            } else if (CommonAttributes.LOGGER.equals(key2)) {
                result = GREATER;
            } else if (CommonAttributes.ASYNC_HANDLER.equals(key1) && !(CommonAttributes.LOGGER.equals(key2) || CommonAttributes.ROOT_LOGGER.equals(key2))) {
                result = LESS;
            } else if (CommonAttributes.ASYNC_HANDLER.equals(key2) && !(CommonAttributes.LOGGER.equals(key1) || CommonAttributes.ROOT_LOGGER.equals(key1))) {
                result = GREATER;
            }
            return result;
        }
    }
}
