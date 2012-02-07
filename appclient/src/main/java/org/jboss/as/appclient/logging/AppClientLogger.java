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

package org.jboss.as.appclient.logging;

import static org.jboss.logging.Logger.Level.ERROR;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * This module is using message IDs in the range 13200-13299. This file is using the subset 13200-13249 for
 * logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface AppClientLogger extends BasicLogger {

    /**
     * The root logger.
     */
    AppClientLogger ROOT_LOGGER = Logger.getMessageLogger(AppClientLogger.class, AppClientLogger.class.getPackage().getName());

    /**
     * Logs a generic error message using the {@link Throwable#toString() t.toString()} for the error message.
     *
     * @param cause the cause of the error.
     * @param t     the cause to use for the log message.
     */
    @LogMessage(level = ERROR)
    @Message(id = 13200, value = "%s")
    void caughtException(@Cause Throwable cause, Throwable t);

    /**
     * Logs an error message indicating there was an error running the app client.
     *
     * @param cause         the cause of the error.
     * @param exceptionName the exception name thrown.
     */
    @LogMessage(level = ERROR)
    @Message(id = 13201, value = "%s running app client main")
    void exceptionRunningAppClient(@Cause Throwable cause, String exceptionName);


    /**
     *
     * @param cause         the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 13202, value = "Error closing connection")
    void exceptionClosingConnection(@Cause Throwable cause);
}
