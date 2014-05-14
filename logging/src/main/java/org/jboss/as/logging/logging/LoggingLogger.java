/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.Closeable;
import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.logging.Handler;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.Target;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Transform;
import org.jboss.logging.annotations.Transform.TransformType;
import org.jboss.logmanager.Configurator;
import org.jboss.logmanager.LogContext;

/**
 * Date: 09.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYLOG", length = 4)
public interface LoggingLogger extends BasicLogger {

    /**
     * A root logger with the category of the package name.
     */
    LoggingLogger ROOT_LOGGER = Logger.getMessageLogger(LoggingLogger.class, "org.jboss.as.logging");

    // id = 1, value = "%s caught exception attempting to revert operation %s at address %s" -- now unused

    /**
     * Logs a warning message indicating an error occurred trying to set the property, represented by the
     * {@code propertyName} parameter, on the handler class, represented by the {@code className} parameter.
     *
     * @param cause        the cause of the error.
     * @param propertyName the name of the property.
     * @param className    the name of the class.
     */
    @LogMessage(level = WARN)
    @Message(id = 2, value = "An error occurred trying to set the property '%s' on handler '%s'.")
    void errorSettingProperty(@Cause Throwable cause, String propertyName, String className);

    // id = 3, value = "Removing bootstrap log handlers" -- now unused
    // id = 4, value = "Restored bootstrap log handlers" -- now unused

    /**
     * Logs a warning message indicating an unknown property, represented by the {@code propertyName} parameter, for
     * the class represented by the {@code className} parameter.
     *
     * @param propertyName the name of the property.
     * @param className    the name of the class.
     */
    @LogMessage(level = WARN)
    @Message(id = 5, value = "Unknown property '%s' for '%s'.")
    void unknownProperty(String propertyName, String className);

    /**
     * Logs an error message indicating to failure to close the resource represented by the {@code closeable}
     * parameter.
     *
     * @param cause     the cause of the error
     * @param closeable the resource
     */
    @LogMessage(level = ERROR)
    @Message(id = 6, value = "Failed to close resource %s")
    void failedToCloseResource(@Cause Throwable cause, Closeable closeable);

    /**
     * Logs a warning message indicating the attribute, represented by the {@code name} parameter, is not a
     * configurable  property value.
     *
     * @param name the name of the attribute
     */
    @LogMessage(level = WARN)
    @Message(id = 7, value = "The attribute %s could not be set as it is not a configurable property value.")
    void invalidPropertyAttribute(String name);

    @Message(id = 8, value = "The path manager service does not appear to be started. Any changes may be lost as a result of this.")
    String pathManagerServiceNotStarted();

    /**
     * Logs a warning message indicating filters are not currently supported for log4j appenders.
     */
    @LogMessage(level = WARN)
    @Message(id = 9, value = "Filters are not currently supported for log4j appenders.")
    void filterNotSupported();

    /**
     * Logs a warning message indicating the deployment specified a logging profile, but the logging profile was not
     * found.
     *
     * @param loggingProfile the logging profile that was not found
     * @param deployment     the deployment that specified the logging profile
     */
    @LogMessage(level = WARN)
    @Message(id = 10, value = "Logging profile '%s' was specified for deployment '%s' but was not found. Using system logging configuration.")
    void loggingProfileNotFound(String loggingProfile, ResourceRoot deployment);

    /**
     * Logs a warning message indicating the configuration file found appears to be a {@link
     * java.util.logging.LogManager J.U.L.} configuration file and the log manager does not allow this configuration.
     *
     * @param fileName the configuration file name
     */
    @LogMessage(level = WARN)
    @Message(id = 11, value = "The configuration file in '%s' appears to be a J.U.L. configuration file. The log manager does not allow this type of configuration file.")
    void julConfigurationFileFound(String fileName);

    /**
     * Logs a warning message indicating the handler is being replaced due to a different type or module name.
     *
     * @param name the name of the handler
     */
    @LogMessage(level = WARN)
    @Message(id = 12, value = "Replacing handler '%s' during add operation. Either the handler type or the module name differs from the initial configuration.")
    void replacingNamedHandler(String name);

    /**
     * Logs a warning message indicating the configurator class is an unknown type and will be replaced.
     *
     * @param c the class that is being replaced
     */
    @LogMessage(level = WARN)
    @Message(id = 13, value = "A configurator class, '%s', is not a known configurator and will be replaced.")
    void replacingConfigurator(@Transform(TransformType.GET_CLASS) Configurator c);

    /**
     * Logs an error message indicating the {@link org.jboss.logmanager.LogContext log context} associated with the
     * deployment could not be removed from the {@link org.jboss.logmanager.LogContextSelector log context selector}.
     *
     * @param logContext     the log context that could not be removed
     * @param deploymentName the name of the deployment
     */
    @LogMessage(level = ERROR)
    @Message(id = 14, value = "The log context (%s) could not be removed for deployment %s")
    void logContextNotRemoved(LogContext logContext, String deploymentName);

    /**
     * Logs a warning message indicating the per-logging deployment property has been deprecated and should not be
     * used. Instead the attribute on the root subsystem should be used.
     *
     * @param propertyName  the per-deployment property name
     * @param attributeName the name of the new attribute
     *
     * @deprecated will be removed when the {@link org.jboss.as.logging.deployments.LoggingConfigDeploymentProcessor#PER_DEPLOYMENT_LOGGING}
     * is removed
     */
    @Deprecated
    @LogMessage(level = WARN)
    @Message(id = 15, value = "The per-logging deployment property (%s) has been deprecated. Please use the %s attribute to enable/disable per-deployment logging.")
    void perDeploymentPropertyDeprecated(String propertyName, String attributeName);

    /**
     * Logs a warning message indicating the per-logging deployment property is being ignored because the attribute has
     * been set to ignore the deployments logging configuration.
     *
     * @param propertyName   the per-deployment property name
     * @param attributeName  the name of the new attribute
     * @param deploymentName the name of the deployment
     *
     * @deprecated will be removed when the {@link org.jboss.as.logging.deployments.LoggingConfigDeploymentProcessor#PER_DEPLOYMENT_LOGGING}
     * is removed
     */
    @Deprecated
    @LogMessage(level = WARN)
    @Message(id = 16, value = "The per-logging deployment property (%s) is being ignored because the attribute %s has been set to ignore configuration files in the deployment %s.")
    void perLoggingDeploymentIgnored(String propertyName, String attributeName, String deploymentName);


    /**
     * Creates an exception indicating the class, represented by the {@code className} parameter, cannot be accessed.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class.
     *
     * @return the message.
     */
    @Message(id = 17, value = "Could not access %s.")
    String cannotAccessClass(@Cause Throwable cause, String className);

    /**
     * Creates an exception indicating the class, represented by the {@code className} parameter, could not be
     * instantiated.
     *
     * @param cause       the cause of the error
     * @param className   the name of the class
     * @param description a description
     * @param name        the name of the configuration
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 18, value = "Failed to instantiate class '%s' for %s '%s'")
    IllegalArgumentException cannotInstantiateClass(@Cause Throwable cause, String className, String description, String name);

    /**
     * Creates an exception indicating a failure to load the module.
     *
     * @param cause       the cause of the error
     * @param moduleName  the name of the module that could not be loaded
     * @param description a description
     * @param name        the name of the configuration
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 19, value = "Failed to load module '%s' for %s '%s'")
    IllegalArgumentException cannotLoadModule(@Cause Throwable cause, String moduleName, String description, String name);

    /**
     * A message indicating the handler, represented by the {@code handlerName} parameter could not be unassigned as it
     * was not assigned.
     *
     * @param handlerName the handler name.
     *
     * @return the message.
     */
    @Message(id = 20, value = "Can not unassign handler. Handler %s is not assigned.")
    String cannotUnassignHandler(String handlerName);

    /**
     * Creates an exception indicating the class, represented by the {@code className} parameter, could not be found.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class that could not be found.
     *
     * @return the message.
     */
    @Message(id = 21, value = "Class '%s' could not be found.")
    String classNotFound(@Cause Throwable cause, String className);

    /**
     * A message indicating the handler encoding failed to set.
     *
     * @param cause    the cause of the error
     * @param encoding the encoding
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 22, value = "The encoding value '%s' is invalid.")
    IllegalArgumentException failedToSetHandlerEncoding(@Cause Throwable cause, String encoding);

    /**
     * A message indicating the handler, represented by the {@code name} parameter, is already assigned.
     *
     * @param name the handler name.
     *
     * @return the message.
     */
    @Message(id = 23, value = "Handler %s is already assigned.")
    String handlerAlreadyDefined(String name);

    /**
     * A message indicating the handler, represented by the {@code name} parameter, was not found.
     *
     * @param name the handler name
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 24, value = "Handler %s not found.")
    IllegalArgumentException handlerNotFound(String name);

    /**
     * A message indicating the filter is invalid.
     *
     * @param name the name of the filter.
     *
     * @return the message.
     */
    @Message(id = 25, value = "Filter %s is invalid")
    String invalidFilter(String name);

    /**
     * A message indicating the log level, represented by the {@code level} parameter, is invalid.
     *
     * @param level the invalid level.
     *
     * @return the message.
     */
    @Message(id = 26, value = "Log level %s is invalid.")
    String invalidLogLevel(String level);

    /**
     * A message indicating the overflow action, represented by the {@code overflowAction} parameter, is invalid.
     *
     * @param overflowAction the invalid overflow action.
     *
     * @return the message.
     */
    @Message(id = 27, value = "Overflow action %s is invalid.")
    String invalidOverflowAction(String overflowAction);

    /**
     * A message indicating the size is invalid.
     *
     * @param size the size.
     *
     * @return the message.
     */
    @Message(id = 28, value = "Invalid size %s")
    String invalidSize(String size);

    /**
     * A message indicating the target name value is invalid.
     *
     * @param targets a collection of valid target names.
     *
     * @return the message.
     */
    @Message(id = 29, value = "Invalid value for target name. Valid names include: %s")
    String invalidTargetName(EnumSet<Target> targets);

    // id = 30, value = "'%s' is not a valid %s." -- now unused

    /**
     * A message indicating the value type key, represented by the {@code kry} parameter, is invalid.
     *
     * @param key           the key.
     * @param allowedValues a collection of allowed values.
     *
     * @return the message.
     */
    @Message(id = 31, value = "Value type key '%s' is invalid. Valid value type keys are; %s")
    String invalidValueTypeKey(String key, Collection<String> allowedValues);

    /**
     * A message indicating the required nested filter element is missing.
     *
     * @return the message.
     */
    @Message(id = 32, value = "Missing required nested filter element")
    String missingRequiredNestedFilterElement();

    // id = 33, value = "Service not started" -- now unused

    /**
     * Creates an exception indicating an unknown parameter type, represented by the {@code type} parameter, for the
     * property represented by the {@code propertyName} parameter on the class.
     *
     * @param type         the parameter type.
     * @param propertyName the name of the property.
     * @param clazz        the class the property with the type could not found on.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 34, value = "Unknown parameter type (%s) for property '%s' on '%s'")
    IllegalArgumentException unknownParameterType(Class<?> type, String propertyName, Class<?> clazz);

    /**
     * A message indicating the logger, represented by the {@code name} parameter was not found.
     *
     * @param name the name of the missing logger.
     *
     * @return the message.
     */
    @Message(id = 35, value = "Logger '%s' was not found.")
    String loggerNotFound(String name);

    // id = 36, value = "'%s' is not a valid %s, found type %s." -- now unused

    // id = 37, value = "File '%s' was not found." -- now unused

    // id = 38, value = "Service '%s' was not found." -- now unused

    /**
     * A message indicating an absolute path cannot be specified for relative-to.
     *
     * @param relativeTo the invalid absolute path.
     *
     * @return the message.
     */
    @Message(id = 39, value = "An absolute path (%s) cannot be specified for relative-to.")
    String invalidRelativeTo(String relativeTo);

    /**
     * A message indicating an absolute path cannot be used when a relative-to path is being used.
     *
     * @param relativeTo the relative path.
     * @param path       absolute path.
     *
     * @return the message.
     */
    @Message(id = 40, value = "An absolute path (%2$s) cannot be used when a relative-to path (%1$s) is being used.")
    String invalidPath(String relativeTo, String path);

    /**
     * A message indicating a suffix is invalid.
     *
     * @param suffix the suffix.
     *
     * @return the message.
     */
    @Message(id = 41, value = "The suffix (%s) is invalid. A suffix must be a valid date format and not contain seconds or milliseconds.")
    String invalidSuffix(String suffix);

    /**
     * Creates an exception indicating a failure to configure logging with the configuration file represented by the
     * {@code fileName} parameter.
     *
     * @param cause    the cause of the error.
     * @param fileName the configuration file name.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 42, value = "Failed to configure logging using '%s' configuration file.")
    DeploymentUnitProcessingException failedToConfigureLogging(@Cause Throwable cause, String fileName);

    /**
     * Creates an exception indicating an error occurred while searching for logging configuration files.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 43, value = "Error occurred while searching for logging configuration files.")
    DeploymentUnitProcessingException errorProcessingLoggingConfiguration(@Cause Throwable cause);

    /**
     * A message indicating the handler is attached to the handlers.
     *
     * @param handlerName the name of tha attached handler.
     * @param handlers    a collection of the handler names.
     *
     * @return the message.
     */
    @Message(id = 44, value = "Handler %s is attached to the following handlers and cannot be removed; %s")
    String handlerAttachedToHandlers(String handlerName, Collection<String> handlers);

    /**
     * A message indicating the handler is attached to the loggers.
     *
     * @param handlerName the name of tha attached handler.
     * @param loggers     a collection of the logger names.
     *
     * @return the message.
     */
    @Message(id = 45, value = "Handler %s is attached to the following loggers and cannot be removed; %s")
    String handlerAttachedToLoggers(String handlerName, Collection<String> loggers);

    /**
     * A message indicating the handler cannot be attached to itself.
     *
     * @param handlerName the name of the handler
     *
     * @return the message
     */
    @Message(id = 46, value = "Cannot add handler (%s) to itself")
    String cannotAddHandlerToSelf(String handlerName);

    /**
     * An exception indicating a handler has been closed.
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 47, value = "The handler is closed, cannot publish to a closed handler")
    IllegalStateException handlerClosed();

    /**
     * An exception indicating a property cannot be set on a closed handler.
     *
     * @param name  the name of the property
     * @param value the value of the property
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = Message.INHERIT, value = "Cannot set property '%s' on a closed handler with value '%s'.")
    IllegalStateException handlerClosed(String name, String value);

    /**
     * A message indicating the handler configuration could not be found.
     *
     * @param name the name of the handler
     *
     * @return the message
     */
    @Message(id = 48, value = "Configuration for handler '%s' could not be found.")
    String handlerConfigurationNotFound(String name);


    /**
     * A message indicating the logger configuration could not be found.
     *
     * @param name the name of the logger
     *
     * @return the message
     */
    @Message(id = 49, value = "Configuration for logger '%s' could not be found.")
    String loggerConfigurationNotFound(String name);

    /**
     * Creates an exception indicating the method on the class is not supported.
     *
     * @param methodName the name of the method
     * @param className  the name of the class
     *
     * @return an {@link UnsupportedOperationException} for the error
     */
    @Message(id = 50, value = "Method %s on class %s is not supported")
    UnsupportedOperationException unsupportedMethod(String methodName, String className);

    /**
     * A message indicating a failure to write the configuration file.
     *
     * @param fileName the name of the file
     *
     * @return the message
     */
    @Message(id = 51, value = "Failed to write configuration file %s")
    RuntimeException failedToWriteConfigurationFile(@Cause Throwable e, File fileName);

    /**
     * Creates an exception indicating a failure was detected while performing a rollback.
     *
     * @param cause the cause of the failure
     *
     * @return an {@link RuntimeException} for the error
     */
    @Message(id = 52, value = "A failure was detecting while performing a rollback.")
    RuntimeException rollbackFailure(@Cause Throwable cause);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the variable
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 53, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating a failure to load the class.
     *
     * @param cause       the cause of the error
     * @param className   the name of the class that could not be loaded
     * @param description a description
     * @param name        the name of the configuration
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 54, value = "Failed to load class '%s' for %s '%s'")
    IllegalArgumentException failedToLoadClass(@Cause Throwable cause, String className, String description, String name);

    /**
     * Creates an exception indicating no property with the name, represented by the {@code propertyName} parameter,
     * was found on the type.
     *
     * @param propertyName the name of the property
     * @param description  a description
     * @param name         the name of the configuration
     * @param type         the type the property does not exist on
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 55, value = "No property named '%s' for %s '%s\' of type '%s'")
    IllegalArgumentException invalidProperty(String propertyName, String description, String name, Class<?> type);

    /**
     * Creates an exception indicating a failure to locate the constructor.
     *
     * @param cause       the cause of the error
     * @param className   the name of the class that could not be loaded
     * @param description a description
     * @param name        the name of the configuration
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 56, value = "Failed to locate constructor in class \"%s\" for %s \"%s\"")
    IllegalArgumentException failedToLocateConstructor(@Cause Throwable cause, String className, String description, String name);

    /**
     * Creates an exception indicating the property cannot be set as it's been removed.
     *
     * @param propertyName the name of the property that cannot be set
     * @param description  a description
     * @param name         the name of the configuration
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 57, value = "Cannot set property '%s' on %s '%s' (removed)")
    IllegalArgumentException cannotSetRemovedProperty(String propertyName, String description, String name);

    /**
     * Creates an exception indicating the property has no setter method.
     *
     * @param propertyName the name of the property that cannot be set
     * @param description  a description
     * @param name         the name of the configuration
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 58, value = "No property '%s' setter found for %s '%s'")
    IllegalArgumentException propertySetterNotFound(String propertyName, String description, String name);

    /**
     * Creates an exception indicating the property type could not be determined.
     *
     * @param propertyName the name of the property that cannot be set
     * @param description  a description
     * @param name         the name of the configuration
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 59, value = "No property '%s' type could be determined for %s '%s'")
    IllegalArgumentException propertyTypeNotFound(String propertyName, String description, String name);

    /**
     * Creates an exception indicating the property could not be removed as it's already been removed
     *
     * @param propertyName the name of the property that cannot be set
     * @param description  a description
     * @param name         the name of the configuration
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 60, value = "Cannot remove property '%s' on %s '%s' (removed)")
    IllegalArgumentException propertyAlreadyRemoved(String propertyName, String description, String name);

    /**
     * Creates an exception indicating the formatter could not be found.
     *
     * @param name the name of the formatter
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 61, value = "Formatter '%s' is not found")
    String formatterNotFound(String name);

    /**
     * Creates an exception indicating the character set is not supported.
     *
     * @param encoding the character set
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 62, value = "Unsupported character set '%s'")
    IllegalArgumentException unsupportedCharSet(String encoding);

    /**
     * Creates an exception indicating the error manager could not be found.
     *
     * @param name the name of the error manager
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 63, value = "Error manager '%s' is not found")
    IllegalArgumentException errorManagerNotFound(String name);

    /**
     * Creates an exception indicating the handler does not support nested handlers.
     *
     * @param handlerClass the handler class
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 64, value = "Nested handlers not supported for handler %s")
    IllegalArgumentException nestedHandlersNotSupported(Class<? extends Handler> handlerClass);

    /**
     * Creates an exception indicating the logger already exists.
     *
     * @param name the name of the logger
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 65, value = "Logger '%s' already exists")
    IllegalArgumentException loggerAlreadyExists(String name);

    /**
     * Creates an exception indicating the formatter already exists.
     *
     * @param name the name of the formatter
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 66, value = "Formatter '%s' already exists")
    IllegalArgumentException formatterAlreadyExists(String name);

    /**
     * Creates an exception indicating the filter already exists.
     *
     * @param name the name of the filter
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 67, value = "Filter '%s' already exists")
    IllegalArgumentException filterAlreadyExists(String name);

    /**
     * Creates an exception indicating the error manager already exists.
     *
     * @param name the name of the error manager
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 68, value = "ErrorManager '%s' already exists")
    IllegalArgumentException errorManagerAlreadyExists(String name);

    /**
     * Creates an exception indicating {@code null} cannot be assigned to a primitive property.
     *
     * @param name the name of the property
     * @param type the type
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 69, value = "Cannot assign null value to primitive property '%s' of %s")
    IllegalArgumentException cannotAssignNullToPrimitive(String name, Class<?> type);

    /**
     * Creates an exception indicating the filter expression string is truncated.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 70, value = "Truncated filter expression string")
    IllegalArgumentException truncatedFilterExpression();

    /**
     * Creates an exception indicating an invalid escape was found in the filter expression string.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 71, value = "Invalid escape found in filter expression string")
    IllegalArgumentException invalidEscapeFoundInFilterExpression();

    /**
     * Creates an exception indicating the filter could not be found.
     *
     * @param name the name of the filter
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 72, value = "Filter '%s' is not found")
    IllegalArgumentException filterNotFound(String name);

    /**
     * Creates an exception indicating an identifier was expected next in the filter expression.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 73, value = "Expected identifier next in filter expression")
    IllegalArgumentException expectedIdentifier();

    /**
     * Creates an exception indicating a string was expected next in the filter expression.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 74, value = "Expected string next in filter expression")
    IllegalArgumentException expectedString();

    /**
     * Creates an exception indicating the token was expected next in the filter expression.
     *
     * @param token the token expected
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 75, value = "Expected '%s' next in filter expression")
    IllegalArgumentException expected(String token);

    /**
     * Creates an exception indicating the one or the other tokens was expected next in the filter expression.
     *
     * @param trueToken  the true token
     * @param falseToken the false token
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = Message.INHERIT, value = "Expected '%s' or '%s' next in filter expression")
    IllegalArgumentException expected(String trueToken, String falseToken);

    /**
     * Creates an exception indicating an unexpected end of the filter expression.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 76, value = "Unexpected end of filter expression")
    IllegalArgumentException unexpectedEnd();

    /**
     * Creates an exception indicating extra data was found in the filter expression.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 77, value = "Extra data after filter expression")
    IllegalArgumentException extraData();

    /**
     * Logs a warning message indicating the {@link org.jboss.logmanager.LogManager} is required and the logging
     * subsystem was not initialized.
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 78, value = "The logging subsystem requires the log manager to be org.jboss.logmanager.LogManager. " +
            "The subsystem has not be initialized and cannot be used. To use JBoss Log Manager you must add the system " +
            "property \"java.util.logging.manager\" and set it to \"org.jboss.logmanager.LogManager\"")
    IllegalStateException extensionNotInitialized();

    /**
     * Creates an exception indicating a failure to read the log file
     *
     * @param cause the cause of the error
     * @param name  the name of the file that was not found
     *
     * @return an {@link OperationFailedException} for the error
     */
    @Message(id = 79, value = "Failed to read the log file '%s'")
    OperationFailedException failedToReadLogFile(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating the file was found in the log directory.
     *
     * @param name              the name of the file that was not found
     * @param directoryProperty the name of the property used to resolved the log directory
     *
     * @return an {@link OperationFailedException} for the error
     */
    @Message(id = 80, value = "File '%s' was not found and cannot be found in the %s directory property.")
    OperationFailedException logFileNotFound(String name, String directoryProperty);

    /**
     * Creates an exception indicating the user cannot read the file.
     *
     * @param name the name of the file that was not allowed to be read
     *
     * @return an {@link OperationFailedException} for the error
     */
    @Message(id = 81, value = "File '%s' is not allowed to be read.")
    OperationFailedException readNotAllowed(String name);
}
