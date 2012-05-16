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

package org.jboss.as.process.protocol;

import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.ConnectException;
import java.net.URI;

/**
 * This module is using message IDs in the range 16600-16699.
 * This file is using the subset 16640-16699 for non-logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 * Date: 21.07.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface ProtocolMessages {
    /**
     * The default messages.
     */
    ProtocolMessages MESSAGES = Messages.getBundle(ProtocolMessages.class);

    /**
     * Creates an exception indicating a failure to create the server thread.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 16640, value = "Failed to create server thread")
    IOException failedToCreateServerThread();

    /**
     * Creates an exception indicating a failure to read the object.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 16641, value = "Failed to read object")
    IOException failedToReadObject(@Cause Throwable cause);

    /**
     * Creates an exception indicating an invalid byte.
     *
     * @return an {@link java.io.UTFDataFormatException} for the error.
     */
    @Message(id = 16642, value = "Invalid byte")
    UTFDataFormatException invalidByte();

    /**
     * Creates an exception indicating an invalid byte.
     *
     * @param c the character.
     * @param i the raw integer.
     *
     * @return an {@link java.io.UTFDataFormatException} for the error.
     */
    @Message(id = 16643, value = "Invalid byte:%s(%d)")
    UTFDataFormatException invalidByte(char c, int i);

    /**
     * Creates an exception indicating an invalid byte token was found.
     *
     * @param expected the expected value.
     * @param actual   the actual value.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 16644, value = "Invalid byte token.  Expecting '%s' received '%s'")
    IOException invalidByteToken(int expected, byte actual);

    /**
     * Creates an exception indicating the an invalid command byte was read.
     *
     * @param commandByte the command byte read.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 16645, value = "Invalid command byte read: %s")
    IOException invalidCommandByte(int commandByte);

    /**
     * Creates an exception indicating the signature is invalid.
     *
     * @param signature the invalid signature.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 16646, value = "Invalid signature [%s]")
    IOException invalidSignature(String signature);

    /**
     * Creates an exception indicating an invalid start chunk was found.
     *
     * @param chunk the start chunk.
     *
     * @return an {@code IOException} for the error.
     */
    @Message(id = 16647, value = "Invalid start chunk start [%s]")
    IOException invalidStartChunk(int chunk);

    /**
     * Creates an exception indicating the type is invalid.
     *
     * @param type the invalid type.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 16648, value = "Invalid type: %s")
    IOException invalidType(String type);

    /**
     * Creates an exception indicating the provided type was invalid.
     *
     * @param validType1   the first valid type.
     * @param validType2   the second valid type.
     * @param providedType the type provided.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 16649, value = "Type is neither %s or %s: %s")
    IllegalArgumentException invalidType(String validType1, String validType2, byte providedType);

    /**
     * Creates an exception indicating only the {@code url} is a valid URL.
     *
     * @param url the valid url.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 16650, value = "Only '%s' is a valid url")
    IllegalArgumentException invalidUrl(String url);

    /**
     * Creates an exception indicating the {@code varName} is {@code null}.
     *
     * @param varName the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 16651, value = "%s is null")
    IllegalArgumentException nullVar(String varName);

    /**
     * Creates an exception indicating the number of bytes read.
     *
     * @param bytesRead the number of bytes read.
     *
     * @return an {@link java.io.EOFException} for the error.
     */
    @Message(id = 16652, value = "Read %d bytes.")
    EOFException readBytes(int bytesRead);

//    /**
//     * Creates an exception indicating there was no request handler found with the id in the operation handler.
//     *
//     * @param id               the id of the request handler.
//     * @param operationHandler the operation handler the id was not found in.
//     *
//     * @return an {@link java.io.IOException} for the error.
//     */
//    @Message(id = 16653, value = "No request handler found with id %s in operation handler %s")
//    IOException requestHandlerIdNotFound(byte id, ManagementOperationHandler operationHandler);

    /**
     * Creates an exception indicating the stream is closed.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 16654, value = "Stream closed")
    IOException streamClosed();

    /**
     * Creates an exception indicating the thread creation was refused.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 16655, value = "Thread creation was refused")
    IllegalStateException threadCreationRefused();

    /**
     * Creates an exception indicating the an unexpected end of stream was detected.
     *
     * @return an {@link java.io.EOFException} for the error.
     */
    @Message(id = 16656, value = "Unexpected end of stream")
    EOFException unexpectedEndOfStream();

    /**
     * Creates an exception indicating the write channel is closed.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 16657, value = "Write channel closed")
    IOException writeChannelClosed();

    /**
     * Creates an exception indicating the writes have already been shutdown.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 16658, value = "Writes are already shut down")
    IOException writesAlreadyShutdown();
}
