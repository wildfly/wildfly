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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.as.controller.PathAddress;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * This module is using message IDs in the range 11500-11599.
 * <p/>
 * This file is using the subset 11500-11529 for logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved JBAS message id blocks.
 * <p/>
 * Date: 09.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface LoggingLogger extends BasicLogger {

    /**
     * A root logger with the category of the package name.
     */
    LoggingLogger ROOT_LOGGER = Logger.getMessageLogger(LoggingLogger.class, LoggingLogger.class.getPackage().getName());

    /**
     * Logs an error message indicating the class, represented by the {@code className} parameter, caught exception
     * attempting to revert the operation, represented by the {@code op} parameter, at the address, represented by the
     * {@code address} parameter.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class that caught the error.
     * @param op        the operation.
     * @param address   the address.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11500, value = "%s caught exception attempting to revert operation %s at address %s")
    void errorRevertingOperation(@Cause Throwable cause, String className, String op, PathAddress address);

    /**
     * Logs a warning message indicating an error occurred trying to set the property, represented by the
     * {@code propertyName} parameter, on the handler class, represented by the {@code className} parameter.
     *
     * @param cause        the cause of the error.
     * @param propertyName the name of the property.
     * @param className    the name of the class.
     */
    @LogMessage(level = WARN)
    @Message(id = 11501, value = "An error occurred trying to set the property '%s' on handler '%s'.")
    void errorSettingProperty(@Cause Throwable cause, String propertyName, String className);

    /**
     * Logs an informational message indicating the bootstrap log handlers are being removed.
     */
    @LogMessage(level = INFO)
    @Message(id = 11502, value = "Removing bootstrap log handlers")
    void removingBootstrapLogHandlers();

    /**
     * Logs an informational message indicating the bootstrap log handlers have been restored.
     */
    @LogMessage(level = INFO)
    @Message(id = 11503, value = "Restored bootstrap log handlers")
    void restoredBootstrapLogHandlers();

    /**
     * Logs a warning message indicating an unknown property, represented by the {@code propertyName} parameter, for
     * the class represented by the {@code className} parameter.
     *
     * @param propertyName the name of the property.
     * @param className    the name of the class.
     */
    @LogMessage(level = WARN)
    @Message(id = 11504, value = "Unknown property '%s' for '%s'.")
    void unknownProperty(String propertyName, String className);
}
