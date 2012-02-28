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

package org.jboss.as.domain.http.server;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface HttpServerLogger extends BasicLogger {

    /**
     * A logger with a category of {@code org.jboss.as.domain.http.api}.
     */
    HttpServerLogger ROOT_LOGGER = Logger.getMessageLogger(HttpServerLogger.class, "org.jboss.as.domain.http.api");

    /**
     * Logs an error message indicating an unexpected error was found executing a model request.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 15100, value = "Unexpected error executing model request")
    void modelRequestError(@Cause Throwable cause);

    /**
     * Logs an error message indicating an unexpected error was found executing a deployment upload request.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 15101, value = "Unexpected error executing deployment upload request")
    void uploadError(@Cause Throwable cause);

    /**
    * A message indicating that admin console module could not be loaded
    *
    * @param slot name of the console slot
    */
    @LogMessage(level = WARN)
    @Message(id = 15102, value = "Unable to load console module for slot %s, disabling console")
    void consoleModuleNotFound(String slot);

    /**
     * A message indicating that SSL has been requested but not properly configured.
     */
    @LogMessage(level = ERROR)
    @Message(id = 15103, value = "A secure port has been specified for the HTTP interface but no SSL configuration in the realm.")
    void sslConfigurationNotFound();

    /*
     * Message IDs 15100 to 15199 Reserved for the HTTP management interface, HTTPServerMessages also contains messages in this
     * range commencing at 15120.
     */

}
