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

package org.jboss.as.protocol.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.URI;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.remoting3.Channel;

/**
 * Date: 21.07.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYPRT", length = 4)
public interface ProtocolLogger extends BasicLogger {

    /**
     * The root logger with a category of the package.
     */
    ProtocolLogger ROOT_LOGGER = Logger.getMessageLogger(ProtocolLogger.class, "org.jboss.as.protocol");

    /**
     * A logger with the category {@code org.jboss.as.protocol.client}.
     */
    ProtocolLogger CLIENT_LOGGER = Logger.getMessageLogger(ProtocolLogger.class, "org.jboss.as.protocol.client");

    /**
     * A logger with the category {@code org.jboss.as.protocol.connection}.
     */
    ProtocolLogger CONNECTION_LOGGER = Logger.getMessageLogger(ProtocolLogger.class, "org.jboss.as.protocol.connection");

    /**
     * Logs a warning message indicating an error occurred when closing the channel.
     *
     * @param message the error message.
     */
    @LogMessage(level = WARN)
    @Message(id = 1, value = "Got error closing channel %s")
    void errorClosingChannel(String message);


    //    @LogMessage(level = ERROR)
    //    @Message(id = 2, value = "Failed to accept a connection")
    //    void failedToAcceptConnection(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to close the resource.
     *
     * @param cause    the cause of the error.
     * @param resource the resource.
     */
    @LogMessage(level = ERROR)
    @Message(id = 3, value = "Failed to close resource %s")
    void failedToCloseResource(@Cause Throwable cause, Object resource);

    /**
     * Logs an error message indicating a failure to close the server socket.
     *
     * @param cause  the cause of the error.
     * @param socket the server socket.
     */
    @LogMessage(level = ERROR)
    @Message(id = 4, value = "Failed to close the server socket %s")
    void failedToCloseServerSocket(@Cause Throwable cause, ServerSocket socket);

    //    @LogMessage(level = ERROR)
    //    @Message(id = 5, value = "Failed to close a socket")
    //    void failedToCloseSocket(@Cause Throwable cause);

    //    @LogMessage(level = ERROR)
    //    @Message(id = 6, value = "Failed to finish the marshaller %s")
    //    void failedToFinishMarshaller(@Cause Throwable cause, Marshaller marshaller);

    //    @LogMessage(level = ERROR)
    //    @Message(id = 7, value = "Failed to finish the unmarshaller %s")
    //    void failedToFinishUnmarshaller(@Cause Throwable cause, Unmarshaller unmarshaller);

    //    @LogMessage(level = ERROR)
    //    @Message(id = 8, value = "Failed to handle incoming connection")
    //    void failedToHandleIncomingConnection(@Cause Throwable cause);

    //    @LogMessage(level = ERROR)
    //    @Message(id = 9, value = "Failed to handle socket failure condition")
    //    void failedToHandleSocketFailure(@Cause Throwable cause);

    //    @LogMessage(level = ERROR)
    //    @Message(id = 10, value = "Failed to handle socket finished condition")
    //    void failedToHandleSocketFinished(@Cause Throwable cause);

    //    @LogMessage(level = ERROR)
    //    @Message(id = 11, value = "Failed to handle socket shut down condition")
    //    void failedToHandleSocketShutdown(@Cause Throwable cause);

    //    @LogMessage(level = ERROR)
    //    @Message(id = 12, value = "Failed to read a message")
    //    void failedToReadMessage(@Cause Throwable cause);

    //    @LogMessage(level = WARN)
    //    @Message(id = 13, value = "Leaked a message output stream; cleaning")
    //    void leakedMessageOutputStream();

    //    @LogMessage(level = WARN)
    //    @Message(id = 14, value = "Received end for wrong channel!")
    //    void receivedWrongChannel();

    @LogMessage(level = WARN)
    @Message(id = 15, value = "Executor is not needed for client")
    void executorNotNeeded();

    //    @LogMessage(level = WARN)
    //    @Message(id = 16, value = "Connection timeout is no longer needed for client")
    //    void connectTimeoutNotNeeded();
    //
    //    @LogMessage(level = WARN)
    //    @Message(id = 17, value = "Connection timeout property is no longer needed for client")
    //    void connectTimeoutPropertyNotNeeded();

    @LogMessage(level = WARN)
    @Message(id = 18, value = "No such request (%d) associated with channel %s")
    void noSuchRequest(int requestId, Channel channel);

    //    @Message(id = 19, value = "Already connected")
    //    IllegalStateException alreadyConnected();

    //    @Message(id = 20, value = "Channel and receiver already started")
    //    IllegalStateException alreadyStarted();

    //    @Message(id = 21, value = "Can't use both a connect timeout and a connect timeout property")
    //    IllegalArgumentException cannotSpecifyMultipleTimeouts();

    //    @Message(id = 22, value = "Can't set uriScheme with specified endpoint")
    //    IllegalArgumentException cannotSetUriScheme();

    /**
     * Creates an exception indicating a connection could not be made.
     *
     * @param uri             the URI attempted to connect.
     *
     * @return a {@link java.net.ConnectException} for the error.
     */
    @Message(id = 23, value = "Could not connect to %s. The connection timed out")
    ConnectException couldNotConnect(URI uri);

    //    @Message(id = 24, value = "Connection was cancelled")
    //    ConnectException connectWasCancelled();

    //    @Message(id = 25, value = "Failed to create server thread")
    //    IOException failedToCreateServerThread();

    //    @Message(id = 26, value = "Failed to read object")
    //    IOException failedToReadObject(@Cause Throwable cause);

    //    @Message(id = 27, value = "Failed to write management response headers")
    //    IOException failedToWriteManagementResponseHeaders(@Cause Throwable cause);

    //    @Message(id = 28, value = "Invalid byte")
    //    UTFDataFormatException invalidByte();

    //    @Message(id = 29, value = "Invalid byte:%s(%d)")
    //    UTFDataFormatException invalidByte(char c, int i);

    /**
     * Creates an exception indicating an invalid byte token was found.
     *
     * @param expected the expected value.
     * @param actual   the actual value.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 30, value = "Invalid byte token.  Expecting '%d' received '%d'")
    IOException invalidByteToken(int expected, byte actual);

    //    @Message(id = 31, value = "Invalid command byte read: %s")
    //    IOException invalidCommandByte(int commandByte);

    /**
     * Creates an exception indicating the signature is invalid.
     *
     * @param signature the invalid signature.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 32, value = "Invalid signature [%s]")
    IOException invalidSignature(String signature);

    //    @Message(id = 33, value = "Invalid start chunk start [%s]")
    //    IOException invalidStartChunk(int chunk);

    /**
     * Creates an exception indicating the type is invalid.
     *
     * @param type the invalid type.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 34, value = "Invalid type: %s")
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
    @Message(id = 35, value = "Type is neither %s or %s: %s")
    IllegalArgumentException invalidType(String validType1, String validType2, byte providedType);

    // @Message(id = 36, value = "Only '%s' is a valid url")
    // IllegalArgumentException invalidUrl(String url);

    //    @Message(id = 37, value = "No operation handler set")
    //    IOException operationHandlerNotSet();

    //    @Message(id = 38, value = "Not connected")
    //    IllegalStateException notConnected();

    /**
     * Creates an exception indicating the {@code varName} is {@code null}.
     *
     * @param varName the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 39, value = "%s is null")
    IllegalArgumentException nullVar(String varName);

    //    @Message(id = 40, value = "%s and %s are null")
    //    IllegalArgumentException nullParameters(String parameterName1, String parameterName2);

    //    @Message(id = 41, value = "Read %d bytes.")
    //    EOFException readBytes(int bytesRead);

    //    @Message(id = 42, value = "No request handler found with id %s in operation handler %s")
    //    IOException requestHandlerIdNotFound(byte id, AbstractMessageHandler operationHandler);

    //    @Message(id = 43, value = "Response handler already registered for request")
    //    IOException responseHandlerAlreadyRegistered();

    //    @Message(id = 44, value = "A problem happened executing on the server: %s")
    //    IOException serverError(String errorMessage);

    //    @Message(id = 45, value = "Stream closed")
    //    IOException streamClosed();

    //    @Message(id = 46, value = "Thread creation was refused")
    //    IllegalStateException threadCreationRefused();

    //    @Message(id = 47, value = "Unexpected end of stream")
    //    EOFException unexpectedEndOfStream();

    //    @Message(id = 48, value = "Scheme %s does not match uri %s")
    //    IllegalArgumentException unmatchedScheme(String scheme, URI uri);

    //    @Message(id = 49, value = "Write channel closed")
    //    IOException writeChannelClosed();

    //    @Message(id = 50, value = "Writes are already shut down")
    //    IOException writesAlreadyShutdown();

    /**
     * Creates an exception indicating that the operation id is already taken.
     *
     * @param operationId the operation id
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 51, value = "Operation with id %d already registered")
    IllegalStateException operationIdAlreadyExists(int operationId);

    @Message(id = 52, value = "Null executor")
    IllegalArgumentException nullExecutor();

    /**
     * Creates an exception indicating a connection could not be made.
     *
     * @param uri             the URI attempted to connect.
     * @param cause           the cause of the failure.
     *
     * @return a {@link ConnectException} for the error.
     */
    @Message(id = 53, value = "Could not connect to %s. The connection failed")
    ConnectException failedToConnect(URI uri, @Cause IOException cause);

    /**
     * Creates an exception indicating that the channel is closed.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 54, value = "Channel closed")
    IOException channelClosed();

    @Message(id = 55, value = "no handler registered for request type '%s'.")
    IOException noSuchResponseHandler(String type);

    /**
     * Creates an exception indicating the response handler id was not found for the request.
     *
     * @param id the id.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 56, value = "No response handler for request %s")
    IOException responseHandlerNotFound(int id);

}
