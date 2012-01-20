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

import java.util.Collection;
import java.util.EnumSet;

import org.jboss.as.logging.handlers.console.Target;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

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
     * @return a {@link StartException} for the error.
     */
    @Message(id = 11530, value = "Could not access %s.")
    StartException cannotAccessClass(@Cause Throwable cause, String className);

    /**
     * Creates an exception indicating the class, represented by the {@code className} parameter, could not be
     * instantiated.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 11531, value = "Could not instantiate %s.")
    StartException cannotInstantiateClass(@Cause Throwable cause, String className);

    /**
     * Creates an exception indicating the module could not be loaded.
     *
     * @param cause      the cause of the error.
     * @param moduleName the name of the module that could not be loaded.
     *
     * @return a {@link StartException} for the error
     */
    @Message(id = 11532, value = "Could not load module %s.")
    StartException cannotLoadModule(@Cause Throwable cause, String moduleName);

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
     * @return an {@link StartException} for the error.
     */
    @Message(id = 11534, value = "Class '%s' could not be found.")
    StartException classNotFound(@Cause Throwable cause, String className);

    /**
     * A message indicating the handler encoding failed to set.
     *
     * @return the message.
     */
    @Message(id = 11535, value = "Failed to set handler encoding.")
    String failedToSetHandlerEncoding();

    /**
     * A message indicating the handler, represented by the {@code name} parameter, is alredy assigned.
     *
     * @param name the handler name.
     *
     * @return the message.
     */
    @Message(id = 11536, value = "Handler %s is already assigned.")
    String handlerAlreadyDefined(String name);

    /**
     * A message indicating the handler is attached to the handlers.
     *
     * @param handlerName the name of tha attached handler.
     * @param handlers    a collection of the handler names.
     *
     * @return the message.
     */
    @Message(value = "Handler %s is attached to the following handlers and cannot be removed; %s")
    String handlerAttachedToHandlers(String handlerName, Collection<String> handlers);

    /**
     * A message indicating the handler is attached to the loggers.
     *
     * @param handlerName the name of tha attached handler.
     * @param loggers     a collection of the logger names.
     *
     * @return the message.
     */
    @Message(value = "Handler %s is attached to the following loggers and cannot be removed; %s")
    String handlerAttachedToLoggers(String handlerName, Collection<String> loggers);

    /**
     * A message indicating the handler, represented by the {@code name} parameter, was not found.
     *
     * @param name the handler name.
     *
     * @return the message.
     */
    @Message(id = 11537, value = "Handler %s not found.")
    String handlerNotFound(String name);

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

    /**
     * Creates an exception indicating the class, represented by the {@code className} parameter, is not subclass of
     * the type.
     *
     * @param className the class name.
     * @param type      the type the class name should be a subtype of.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 11543, value = "'%s' is not a valid %s.")
    StartException invalidType(String className, Class<?> type);

    /**
     * A message indicating the attribute, represented by the {@code name} parameter, is not a valid type.
     *
     * @param name      the name of the attribute
     * @param validType the valid model type.
     * @param foundType the type found.
     *
     * @return the message.
     */
    @Message(id = 11549, value = "'%s' is not a valid %s, found type %s.")
    String invalidType(String name, ModelType validType, ModelType foundType);

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

    /**
     * Creates an exception indicating the service has not yet been started.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11546, value = "Service not started")
    IllegalStateException serviceNotStarted();

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

    /**
     * A message indicating the file was not found.
     *
     * @param fileName the file name that was not found.
     *
     * @return the message.
     */
    @Message(id = 11550, value = "File '%s' was not found.")
    String fileNotFound(String fileName);

    /**
     * A message indicating the service was not found.
     *
     * @param name the name of the service.
     *
     * @return the message.
     */
    @Message(id = 11551, value = "Service '%s' was not found.")
    String serviceNotFound(ServiceName name);

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
}
