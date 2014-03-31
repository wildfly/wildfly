/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

import java.net.InetAddress;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Message IDs 15100 to 15199 Reserved for the HTTP management interface, HttpServerMessages also contains messages in this
 * range commencing at 15120.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface HttpServerLogger extends BasicLogger {
    HttpServerLogger ROOT_LOGGER = Logger.getMessageLogger(HttpServerLogger.class, "org.jboss.as.domain.http.api.undertow");

    @LogMessage(level = ERROR)
    @Message(id = 15100, value = "Unexpected error executing model request")
    void modelRequestError(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 15101, value = "Unexpected error executing deployment upload request")
    void uploadError(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 15102, value = "Unable to load console module for slot %s, disabling console")
    void consoleModuleNotFound(String slot);

    //15103 was used before

    @LogMessage(level = ERROR)
    @Message(id = 15104, value = "Unable to load error contest for slot %s, disabling error context.")
    void errorContextModuleNotFound(String slot);

    @LogMessage(level = INFO)
    @Message(id = 15105, value = "Management interface is using different addresses for HTTP (%s) and HTTPS (%s). Redirection of HTTPS requests from HTTP socket to HTTPS socket will not be supported.")
    void httpsRedirectNotSupported(InetAddress bindAddress, InetAddress secureBindAddress);
}
