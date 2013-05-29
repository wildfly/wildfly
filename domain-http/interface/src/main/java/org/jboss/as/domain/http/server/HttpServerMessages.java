/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.modules.ModuleNotFoundException;


/**
 * Message IDs 15120 to 15199 Reserved for the HTTP management interface, HttpServerLogger also contains messages in this
 * range commencing at 15100-15120.
 *
 * @author kabir
 */
@MessageBundle(projectCode = "JBAS")
public interface HttpServerMessages {

    /**
     * The messages.
     */
    HttpServerMessages MESSAGES = Messages.getBundle(HttpServerMessages.class);

    @Message(id = 15127, value = "Invalid operation '%s'")
    IllegalArgumentException invalidOperation(@Cause Throwable cause, String value);

    /**
     * An error message indicating that the security realm is not ready to process requests and a URL that can be viewed for
     * additional information.
     *
     * @param url - the url clients should visit for further information.
     * @return the error message.
     */
    @Message(id = 15135, value = "The security realm is not ready to process requests, see %s")
    String realmNotReadyMessage(final String url);

    //Messages 15120-15136 were used by the old implementation

    @Message(id = 15136, value = "No console module available with module name %s")
    ModuleNotFoundException consoleModuleNotFound(final String moduleName);

    @Message(id = 15137, value = "Failed to read %s")
    RuntimeException failedReadingResource(@Cause Throwable cause, String resource);

    @Message(id = 15138, value = "Invalid resource")
    String invalidResource();

    @Message(id = 15139, value = "Invalid Credential Type '%s'")
    IllegalArgumentException invalidCredentialType(String value);

    /*
     * Message IDs 15100 to 15199 Reserved for the HTTP management interface, HttpServerLogger also contains messages in this
     * range commencing at 15100.
     */

}
