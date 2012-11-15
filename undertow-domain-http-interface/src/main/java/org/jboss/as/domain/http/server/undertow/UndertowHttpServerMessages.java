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

package org.jboss.as.domain.http.server.undertow;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;


/**
 * TODO use proper ids
 *
 * @author kabir
 */
@MessageBundle(projectCode = "JBAS")
public interface UndertowHttpServerMessages {

    /**
     * The messages.
     */
    UndertowHttpServerMessages MESSAGES = Messages.getBundle(UndertowHttpServerMessages.class);

    @Message(id = Message.NONE, value = "Invalid operation '%s'")
    IllegalArgumentException invalidOperation(@Cause Throwable cause, String value);

    @Message(id = Message.NONE, value = "Failed to read %s")
    RuntimeException failedReadingResource(@Cause Throwable cause, String resource);

    @Message(id = Message.NONE, value = "Invalid resource")
    String invalidResource();

}
