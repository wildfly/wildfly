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
package org.jboss.as.domain.http.server.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

import java.net.InetAddress;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.modules.ModuleNotFoundException;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYDMHTTP", length = 4)

public interface HttpServerLogger extends BasicLogger {
    HttpServerLogger ROOT_LOGGER = Logger.getMessageLogger(HttpServerLogger.class, "org.jboss.as.domain.http.api.undertow");

    @LogMessage(level = ERROR)
    @Message(id = 1, value = "Unexpected error executing model request")
    void modelRequestError(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Unexpected error executing deployment upload request")
    void uploadError(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 3, value = "Unable to load console module for slot %s, disabling console")
    void consoleModuleNotFound(String slot);

    @LogMessage(level = ERROR)
    @Message(id = 4, value = "Unable to load error contest for slot %s, disabling error context.")
    void errorContextModuleNotFound(String slot);

    @LogMessage(level = INFO)
    @Message(id = 11, value = "Management interface is using different addresses for HTTP (%s) and HTTPS (%s). Redirection of HTTPS requests from HTTP socket to HTTPS socket will not be supported.")
    void httpsRedirectNotSupported(InetAddress bindAddress, InetAddress secureBindAddress);

    @Message(id = 5, value = "Invalid operation '%s'")
    IllegalArgumentException invalidOperation(@Cause Throwable cause, String value);

    /**
     * An error message indicating that the security realm is not ready to process requests and a URL that can be viewed for
     * additional information.
     *
     * @param url - the url clients should visit for further information.
     * @return the error message.
     */
    @Message(id = 6, value = "The security realm is not ready to process requests, see %s")
    String realmNotReadyMessage(final String url);

    @Message(id = 7, value = "No console module available with module name %s")
    ModuleNotFoundException consoleModuleNotFoundMsg(final String moduleName);

    @Message(id = 8, value = "Failed to read %s")
    RuntimeException failedReadingResource(@Cause Throwable cause, String resource);

    @Message(id = 9, value = "Invalid resource")
    String invalidResource();

    @Message(id = 10, value = "Invalid Credential Type '%s'")
    IllegalArgumentException invalidCredentialType(String value);
}
