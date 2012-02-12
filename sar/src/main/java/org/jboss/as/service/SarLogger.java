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

package org.jboss.as.service;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * This module is using message IDs in the range 17200-17299. This file is using the subset 17200-17219 for
 * logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface SarLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    SarLogger ROOT_LOGGER = Logger.getMessageLogger(SarLogger.class, SarLogger.class.getPackage().getName());

    /**
     * A logger with the category {@code org.jboss.as.deployment.service}.
     */
    SarLogger DEPLOYMENT_SERVICE_LOGGER = Logger.getMessageLogger(SarLogger.class, "org.jboss.as.deployment.service");

    /**
     * Logs an error message indicating a failure to execute a legacy service method, represented by the {@code
     * methodName} parameter.
     *
     * @param cause      the cause of the error.
     * @param methodName the name of the method that failed to execute.
     */
    @LogMessage(level = ERROR)
    @Message(id = 17200, value = "Failed to execute legacy service %s method")
    void failedExecutingLegacyMethod(@Cause Throwable cause, String methodName);

    /**
     * Logs a warning message indicating the inability to find a {@link java.beans.PropertyEditor} for the type.
     *
     * @param type the type.
     */
    @LogMessage(level = WARN)
    @Message(id = 17201, value = "Unable to find PropertyEditor for type %s")
    void propertyNotFound(Class<?> type);
}
