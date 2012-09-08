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

package org.jboss.as.logging;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.logging.Handler;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 11500-11599.
 * <p/>
 * This file is using the subset 11530-11599 for non-logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved JBAS message id blocks.
 * <p/>
 * Date: 09.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface LoggingMessages {

    /**
     * The default messages.
     */
    LoggingMessages MESSAGES = Messages.getBundle(LoggingMessages.class);

    /**
     * Creates an exception indicating the class, represented by the {@code className} parameter, cannot be accessed.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class.
     *
     * @return the message.
     */
    @Message(id = 11530, value = "Could not access %s.")
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
    @Message(id = 11531, value = "Failed to instantiate class '%s' for %s '%s'")
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
    @Message(id = 11532, value = "Failed to load module '%s' for %s '%s'")
    IllegalArgumentException cannotLoadModule(@Cause Throwable cause, String moduleName, String description, String name);

    /**
     * A message indicating the handler, represented by the {@code handlerName} parameter could not be unassigned as it
     * was not assigned.
     *
     * @param handlerName the handler name.
     *
     * @return the message.
     */
    @Message(id = 11533, value = "Can not unassign handler. Handler %s is not assigned.")
    String cannotUnassignHandler(String handlerName);

    /**
     * Creates an exception indicating the class, represented by the {@code className} parameter, could not be found.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class that could not be found.
     *
     * @return the message.
     */
    @Message(id = 11534, value = "Class '%s' could not be found.")
    String classNotFound(@Cause Throwable cause, String className);

    /**
     * A message indicating the handler encoding failed to set.
     *
     * @param cause    the cause of the error
     * @param encoding the encoding
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11535, value = "The encoding value '%s' is invalid.")
    IllegalArgumentException failedToSetHandlerEncoding(@Cause Throwable cause, String encoding);

    /**
     * A message indicating the handler, represented by the {@code name} parameter, is already assigned.
     *
     * @param name the handler name.
     *
     * @return the message.
     */
    @Message(id = 11536, value = "Handler %s is already assigned.")
    String handlerAlreadyDefined(String name);

    /**
     * A message indicating the handler, represented by the {@code name} parameter, was not found.
     *
     * @param name the handler name
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11537, value = "Handler %s not found.")
    IllegalArgumentException handlerNotFound(String name);

    /**
     * A message indicating the filter is invalid.
     *
     * @param name the name of the filter.
     *
     * @return the message.
     */
    @Message(id = 11538, value = "Filter %s is invalid")
    String invalidFilter(String name);

    /**
     * A message indicating the log level, represented by the {@code level} parameter, is invalid.
     *
     * @param level the invalid level.
     *
     * @return the message.
     */
    @Message(id = 11539, value = "Log level %s is invalid.")
    String invalidLogLevel(String level);

    /**
     * A message indicating the overflow action, represented by the {@code overflowAction} parameter, is invalid.
     *
     * @param overflowAction the invalid overflow action.
     *
     * @return the message.
     */
    @Message(id = 11540, value = "Overflow action %s is invalid.")
    String invalidOverflowAction(String overflowAction);

    /**
     * A message indicating the size is invalid.
     *
     * @param size the size.
     *
     * @return the message.
     */
    @Message(id = 11541, value = "Invalid size %s")
    String invalidSize(String size);

    /**
     * A message indicating the target name value is invalid.
     *
     * @param targets a collection of valid target names.
     *
     * @return the message.
     */
    @Message(id = 11542, value = "Invalid value for target name. Valid names include: %s")
    String invalidTargetName(EnumSet<Target> targets);

    // id = 11543, value = "'%s' is not a valid %s." -- now unused
    // id = 11549, value = "'%s' is not a valid %s, found type %s." -- now unused

    /**
     * A message indicating the value type key, represented by the {@code kry} parameter, is invalid.
     *
     * @param key           the key.
     * @param allowedValues a collection of allowed values.
     *
     * @return the message.
     */
    @Message(id = 11544, value = "Value type key '%s' is invalid. Valid value type keys are; %s")
    String invalidValueTypeKey(String key, Collection<String> allowedValues);

    /**
     * A message indicating the logger, represented by the {@code name} parameter was not found.
     *
     * @param name the name of the missing logger.
     *
     * @return the message.
     */
    @Message(id = 11548, value = "Logger '%s' was not found.")
    String loggerNotFound(String name);

    /**
     * A message indicating the required nested filter element is missing.
     *
     * @return the message.
     */
    @Message(id = 11545, value = "Missing required nested filter element")
    String missingRequiredNestedFilterElement();

    // id = 11546, value = "Service not started" -- now unused

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
    @Message(id = 11547, value = "Unknown parameter type (%s) for property '%s' on '%s'")
    IllegalArgumentException unknownParameterType(Class<?> type, String propertyName, Class<?> clazz);

    // id = 11550, value = "File '%s' was not found." -- now unused
    // id = 11551, value = "Service '%s' was not found." -- now unused

    /**
     * A message indicating an absolute path cannot be specified for relative-to.
     *
     * @param relativeTo the invalid absolute path.
     *
     * @return the message.
     */
    @Message(id = 11552, value = "An absolute path (%s) cannot be specified for relative-to.")
    String invalidRelativeTo(String relativeTo);

    /**
     * A message indicating an absolute path cannot be used when a relative-to path is being used.
     *
     * @param relativeTo the relative path.
     * @param path       absolute path.
     *
     * @return the message.
     */
    @Message(id = 11553, value = "An absolute path (%2$s) cannot be used when a relative-to path (%1$s) is being used.")
    String invalidPath(String relativeTo, String path);

    /**
     * A message indicating an suffix is invalid.
     *
     * @param suffix the suffix.
     *
     * @return the message.
     */
    @Message(id = 11554, value = "The suffix (%s) is invalid. A suffix must be a valid date format and not contain seconds or milliseconds.")
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
    @Message(id = 11555, value = "Failed to configure logging using '%s' configuration file.")
    DeploymentUnitProcessingException failedToConfigureLogging(@Cause Throwable cause, String fileName);

    /**
     * Creates an exception indicating an error occurred while searching for logging configuration files.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11556, value = "Error occurred while searching for logging configuration files.")
    DeploymentUnitProcessingException errorProcessingLoggingConfiguration(@Cause Throwable cause);

    /**
     * A message indicating the handler is attached to the handlers.
     *
     * @param handlerName the name of tha attached handler.
     * @param handlers    a collection of the handler names.
     *
     * @return the message.
     */
    @Message(id = 11557, value = "Handler %s is attached to the following handlers and cannot be removed; %s")
    String handlerAttachedToHandlers(String handlerName, Collection<String> handlers);

    /**
     * A message indicating the handler is attached to the loggers.
     *
     * @param handlerName the name of tha attached handler.
     * @param loggers     a collection of the logger names.
     *
     * @return the message.
     */
    @Message(id = 11558, value = "Handler %s is attached to the following loggers and cannot be removed; %s")
    String handlerAttachedToLoggers(String handlerName, Collection<String> loggers);

    /**
     * A message indicating the handler cannot be attached to itself.
     *
     * @param handlerName the name of the handler
     *
     * @return the message
     */
    @Message(id = 11559, value = "Cannot add handler (%s) to itself")
    String cannotAddHandlerToSelf(String handlerName);

    /**
     * An exception indicating a handler has been closed.
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 11560, value = "The handler is closed, cannot publish to a closed handler")
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
    @Message(id = 11561, value = "Configuration for handler '%s' could not be found.")
    String handlerConfigurationNotFound(String name);


    /**
     * A message indicating the logger configuration could not be found.
     *
     * @param name the name of the logger
     *
     * @return the message
     */
    @Message(id = 11562, value = "Configuration for logger '%s' could not be found.")
    String loggerConfigurationNotFound(String name);

    /**
     * Creates an exception indicating the path manager service has not been started and any changes may be lost as a
     * result of this.
     * <p/>
     * Essentially this means the {@code logging.properties} could not be written out and until the changes may have
     * been lost unless they were written to the logging subsystem in the XML configuration.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11563, value = "The path manager service does not appear to be started.")
    IllegalStateException pathManagerServiceNotStarted();

    /**
     * Creates an exception indicating the method on the class is not supported.
     *
     * @param methodName the name of the method
     * @param className  the name of the class
     *
     * @return an {@link UnsupportedOperationException} for the error
     */
    @Message(id = 11564, value = "Method %s on class %s is not supported")
    UnsupportedOperationException unsupportedMethod(String methodName, String className);

    /**
     * A message indicating a failure to write the configuration file.
     *
     * @param fileName the name of the file
     *
     * @return the message
     */
    @Message(id = 11565, value = "Failed to write configuration file %s")
    String failedToWriteConfigurationFile(File fileName);

    /**
     * Creates an exception indicating a failure was detected while performing a rollback.
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 11566, value = "A failure was detecting while performing a rollback.")
    String rollbackFailure();

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the variable
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11567, value = "%s is null")
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
    @Message(id = 11568, value = "Failed to load class '%s' for %s '%s'")
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
    @Message(id = 11569, value = "No property named '%s' for %s '%s\' of type '%s'")
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
    @Message(id = 11570, value = "Failed to locate constructor in class \"%s\" for %s \"%s\"")
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
    @Message(id = 11571, value = "Cannot set property '%s' on %s '%s' (removed)")
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
    @Message(id = 11572, value = "No property '%s' setter found for %s '%s'")
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
    @Message(id = 11573, value = "No property '%s' type could be determined for %s '%s'")
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
    @Message(id = 11574, value = "Cannot remove property '%s' on %s '%s' (removed)")
    IllegalArgumentException propertyAlreadyRemoved(String propertyName, String description, String name);

    /**
     * Creates an exception indicating the formatter could not be found.
     *
     * @param name the name of the formatter
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11575, value = "Formatter '%s' is not found")
    IllegalArgumentException formatterNotFound(String name);

    /**
     * Creates an exception indicating the character set is not supported.
     *
     * @param encoding the character set
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11576, value = "Unsupported character set '%s'")
    IllegalArgumentException unsupportedCharSet(String encoding);

    /**
     * Creates an exception indicating the error manager could not be found.
     *
     * @param name the name of the error manager
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11577, value = "Error manager '%s' is not found")
    IllegalArgumentException errorManagerNotFound(String name);

    /**
     * Creates an exception indicating the handler does not support nested handlers.
     *
     * @param handlerClass the handler class
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11578, value = "Nested handlers not supported for handler %s")
    IllegalArgumentException nestedHandlersNotSupported(Class<? extends Handler> handlerClass);

    /**
     * Creates an exception indicating the logger already exists.
     *
     * @param name the name of the logger
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11579, value = "Logger '%s' already exists")
    IllegalArgumentException loggerAlreadyExists(String name);

    /**
     * Creates an exception indicating the formatter already exists.
     *
     * @param name the name of the formatter
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11580, value = "Formatter '%s' already exists")
    IllegalArgumentException formatterAlreadyExists(String name);

    /**
     * Creates an exception indicating the filter already exists.
     *
     * @param name the name of the filter
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11581, value = "Filter '%s' already exists")
    IllegalArgumentException filterAlreadyExists(String name);

    /**
     * Creates an exception indicating the error manager already exists.
     *
     * @param name the name of the error manager
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11582, value = "ErrorManager '%s' already exists")
    IllegalArgumentException errorManagerAlreadyExists(String name);

    /**
     * Creates an exception indicating {@code null} cannot be assigned to a primitive property.
     *
     * @param name the name of the property
     * @param type the type
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11583, value = "Cannot assign null value to primitive property '%s' of %s")
    IllegalArgumentException cannotAssignNullToPrimitive(String name, Class<?> type);

    /**
     * Creates an exception indicating the filter expression string is truncated.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11584, value = "Truncated filter expression string")
    IllegalArgumentException truncatedFilterExpression();

    /**
     * Creates an exception indicating the an invalid escape was found in the filter expression string.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11585, value = "Invalid escape found in filter expression string")
    IllegalArgumentException invalidEscapeFoundInFilterExpression();

    /**
     * Creates an exception indicating the filter could not be found.
     *
     * @param name the name of the filter
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11586, value = "Filter '%s' is not found")
    IllegalArgumentException filterNotFound(String name);

    /**
     * Creates an exception indicating an identifier was expected next in the filter expression.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11587, value = "Expected identifier next in filter expression")
    IllegalArgumentException expectedIdentifier();

    /**
     * Creates an exception indicating a string was expected next in the filter expression.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11588, value = "Expected string next in filter expression")
    IllegalArgumentException expectedString();

    /**
     * Creates an exception indicating the token was expected next in the filter expression.
     *
     * @param token the token expected
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11589, value = "Expected '%s' next in filter expression")
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
    @Message(id = 11590, value = "Unexpected end of filter expression")
    IllegalArgumentException unexpectedEnd();

    /**
     * Creates an exception indicating extra data was found in the filter expression.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 11591, value = "Extra data after filter expression")
    IllegalArgumentException extraData();
}
