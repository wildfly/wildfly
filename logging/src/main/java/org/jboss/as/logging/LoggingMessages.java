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

import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.StartException;

import java.util.EnumSet;

/**
 * Date: 09.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
interface LoggingMessages {
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
    @Message(id = 11520, value = "Could not access %s.")
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
    @Message(id = 11521, value = "Could not instantiate %s.")
    StartException cannotInstantiateClass(@Cause Throwable cause, String className);

    /**
     * Creates an exception indicating the module could not be loaded.
     *
     * @param cause      the cause of the error.
     * @param moduleName the name of the module that could not be loaded.
     *
     * @return a {@link StartException} for the error
     */
    @Message(id = 11522, value = "Could not load module %s.")
    StartException cannotLoadModule(@Cause Throwable cause, String moduleName);

    /**
     * A message indicating the handler, represented by the {@code handlerName} parameter could not be unassigned as it
     * was not assigned.
     *
     * @param handlerName the handler name.
     *
     * @return the message.
     */
    @Message(id = 11523, value = "Can not unassign handler.  Handler %s is not assigned.")
    String cannotUnassignHandler(String handlerName);

    /**
     * Creates an exception indicating the class, represented by the {@code className} parameter, could not be found.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class that could not be found.
     *
     * @return an {@link StartException} for the error.
     */
    @Message(id = 11524, value = "Class '%s' could not be found.")
    StartException classNotFound(@Cause Throwable cause, String className);

    /**
     * A message indicating the handler encoding failed to set.
     *
     * @return the message.
     */
    @Message(id = 11525, value = "Failed to set handler encoding.")
    String failedToSetHandlerEncoding();

    /**
     * A message indicating the handler, represented by the {@code name} parameter, is alredy assigned.
     *
     * @param name the handler name.
     *
     * @return the message.
     */
    @Message(id = 11526, value = "Handler %s is already assigned.")
    String handlerAlreadyDefined(String name);

    /**
     * A message indicating the handler, represented by the {@code name} parameter, was not found.
     *
     * @param name the handler name.
     *
     * @return the message.
     */
    @Message(id = 11527, value = "Handler %s not found.")
    String handlerNotFound(String name);

    /**
     * A message indicating the log level, represented by the {@code level} parameter, is invalid.
     *
     * @param level the invalid level.
     *
     * @return the message.
     */
    @Message(id = 11528, value = "Log level %s is invalid.")
    String invalidLogLevel(String level);

    /**
     * A message indicating the overflow action, represented by the {@code overflowAction} parameter, is invalid.
     *
     * @param overflowAction the invalid overflow action.
     *
     * @return the message.
     */
    @Message(id = 11529, value = "Overflow action %s is invalid.")
    String invalidOverflowAction(String overflowAction);

    /**
     * A message indicating the size is invalid.
     *
     * @param size the size.
     *
     * @return the message.
     */
    @Message(id = 11530, value = "Invalid size %s")
    String invalidSize(String size);

    /**
     * A message indicating the target name value is invalid.
     *
     * @param targets a collection of valid target names.
     *
     * @return the message.
     */
    @Message(id = 11531, value = "Invalid value for target name. Valid names include: %s")
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
    @Message(id = 11532, value = "'%s' is not a valid %s.")
    StartException invalidType(String className, Class<?> type);

    /**
     * A message indicating the required nested filter element is missing.
     *
     * @return the message.
     */
    @Message(id = 11533, value = "Missing required nested filter element")
    String missingRequiredNestedFilterElement();

    /**
     * Creates an exception indicating the service has not yet been started.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11534, value = "Service not started")
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
    @Message(id = 11535, value = "Unknown parameter type (%s) for property '%s' on '%s'")
    IllegalArgumentException unknownParameterType(Class<?> type, String propertyName, Class<?> clazz);
}
