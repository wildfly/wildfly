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

package org.jboss.as.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.ConnectException;
import java.net.URI;

import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 12100-12199.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 *
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
     * Creates an exception indicating a connection is already connected.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12140, value = "Already connected")
    IllegalStateException alreadyConnected();

    /**
     * Creates an exception indicating the channel and receiver have already been started.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12141, value = "Channel and receiver already started")
    IllegalStateException alreadyStarted();

    /**
     * Creates an exception indicating that both connect timeout and the connect timeout property cannot be used
     * together.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12142, value = "Can't use both a connect timeout and a connect timeout property")
    IllegalArgumentException cannotSpecifyMultipleTimeouts();

    /**
     * Creates an exception indicating the uriScheme cannot be set with the endpoint.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12143, value = "Can't set uriScheme with specified endpoint")
    IllegalArgumentException cannotSetUriScheme();

    /**
     * Creates an exception indicating a connection could not be made.
     *
     * @param uri             the URI attempted to connect.
     * @param e               the exception during the connect
     *
     * @return a {@link ConnectException} for the error.
     */
    @Message(id = 12144, value = "Could not connect to %s. Make sure the server is running.")
    ConnectException couldNotConnect(URI uri, @Cause Exception e);

    @Message(id = 12145, value = "Connection was cancelled")
    ConnectException connectWasCancelled();

    /**
     * Creates an exception indicating a failure to create the server thread.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12146, value = "Failed to create server thread")
    IOException failedToCreateServerThread();

    /**
     * Creates an exception indicating a failure to read the object.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12147, value = "Failed to read object")
    IOException failedToReadObject(@Cause Throwable cause);

    /**
     * Creates an exception indicating a failure to write management response headers.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12148, value = "Failed to write management response headers")
    IOException failedToWriteManagementResponseHeaders(@Cause Throwable cause);

    /**
     * Creates an exception indicating an invalid byte.
     *
     * @return an {@link UTFDataFormatException} for the error.
     */
    @Message(id = 12149, value = "Invalid byte")
    UTFDataFormatException invalidByte();

    /**
     * Creates an exception indicating an invalid byte.
     *
     * @param c the character.
     * @param i the raw integer.
     *
     * @return an {@link UTFDataFormatException} for the error.
     */
    @Message(id = 12150, value = "Invalid byte:%s(%d)")
    UTFDataFormatException invalidByte(char c, int i);

    /**
     * Creates an exception indicating an invalid byte token was found.
     *
     * @param expected the expected value.
     * @param actual   the actual value.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12151, value = "Invalid byte token.  Expecting '%d' received '%d'")
    IOException invalidByteToken(int expected, byte actual);

    /**
     * Creates an exception indicating the an invalid command byte was read.
     *
     * @param commandByte the command byte read.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12152, value = "Invalid command byte read: %s")
    IOException invalidCommandByte(int commandByte);

    /**
     * Creates an exception indicating the signature is invalid.
     *
     * @param signature the invalid signature.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12153, value = "Invalid signature [%s]")
    IOException invalidSignature(String signature);

    /**
     * Creates an exception indicating an invalid start chunk was found.
     *
     * @param chunk the start chunk.
     *
     * @return an {@code IOException} for the error.
     */
    @Message(id = 12154, value = "Invalid start chunk start [%s]")
    IOException invalidStartChunk(int chunk);

    /**
     * Creates an exception indicating the type is invalid.
     *
     * @param type the invalid type.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12155, value = "Invalid type: %s")
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
    @Message(id = 12156, value = "Type is neither %s or %s: %s")
    IllegalArgumentException invalidType(String validType1, String validType2, byte providedType);

    /**
     * Creates an exception indicating only the {@code url} is a valid URL.
     *
     * @param url the valid url.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12157, value = "Only '%s' is a valid url")
    IllegalArgumentException invalidUrl(String url);

    /**
     * Creates an exception indicating there is no operation handler set.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12158, value = "No operation handler set")
    IOException operationHandlerNotSet();

    /**
     * Creates an exception indicating there is no connection.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12159, value = "Not connected")
    IllegalStateException notConnected();

    /**
     * Creates an exception indicating the {@code varName} is {@code null}.
     *
     * @param varName the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12160, value = "%s is null")
    IllegalArgumentException nullVar(String varName);

    /**
     * Creates an exception indicating the {@code parameterName1} and {@code parameterName2} are {@code null}.
     *
     * @param parameterName1 the parameter name.
     * @param parameterName2 the parameter name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12161, value = "%s and %s are null")
    IllegalArgumentException nullParameters(String parameterName1, String parameterName2);

    /**
     * Creates an exception indicating the number of bytes read.
     *
     * @param bytesRead the number of bytes read.
     *
     * @return an {@link EOFException} for the error.
     */
    @Message(id = 12162, value = "Read %d bytes.")
    EOFException readBytes(int bytesRead);

    /**
     * Creates an exception indicating there was no request handler found with the id in the operation handler.
     *
     * @param id               the id of the request handler.
     * @param operationHandler the operation handler the id was not found in.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12163, value = "No request handler found with id %s in operation handler %s")
    IOException requestHandlerIdNotFound(byte id, AbstractMessageHandler operationHandler);

    /**
     * Creates an exception indicating the response handler has already been registered for the request.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12164, value = "Response handler already registered for request")
    IOException responseHandlerAlreadyRegistered();

    /**
     * Creates an exception indicating the response handler id was not found for the request.
     *
     * @param id the id.
     *
     * @return an {@link IOException} for the error.
     */
    @Message("No response handler for request %s")
    IOException responseHandlerNotFound(int id);

    /**
     * Creates an exception indicating a problem happened executing on the server.
     *
     * @param errorMessage the error message.
     *
     * @return an {@link IOException} for the error
     */
    @Message(id = 12165, value = "A problem happened executing on the server: %s")
    IOException serverError(String errorMessage);

    /**
     * Creates an exception indicating the stream is closed.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12166, value = "Stream closed")
    IOException streamClosed();

    /**
     * Creates an exception indicating the thread creation was refused.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12167, value = "Thread creation was refused")
    IllegalStateException threadCreationRefused();

    /**
     * Creates an exception indicating the an unexpected end of stream was detected.
     *
     * @return an {@link EOFException} for the error.
     */
    @Message(id = 12168, value = "Unexpected end of stream")
    EOFException unexpectedEndOfStream();

    /**
     * Creates an exception indicating the scheme does not match the URI.
     *
     * @param scheme the scheme.
     * @param uri    the uri.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12169, value = "Scheme %s does not match uri %s")
    IllegalArgumentException unmatchedScheme(String scheme, URI uri);

    /**
     * Creates an exception indicating the write channel is closed.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12170, value = "Write channel closed")
    IOException writeChannelClosed();

    /**
     * Creates an exception indicating the writes have already been shutdown.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 12171, value = "Writes are already shut down")
    IOException writesAlreadyShutdown();

    /**
     * Creates an exception indicating that no active operation with the given
     * id is registered.
     *
     * @param operationId the operation id
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12172, value = "No active operation with id %d registered")
    IllegalStateException noActiveOperation(int operationId);

    @Message(id = 12173, value = "Null executor")
    IllegalArgumentException nullExecutor();

}
