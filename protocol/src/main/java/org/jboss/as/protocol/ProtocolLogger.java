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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import java.net.ServerSocket;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.remoting3.Channel;

/**
 * Date: 21.07.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface ProtocolLogger extends BasicLogger {

    /**
     * The root logger with a category of the package.
     */
    ProtocolLogger ROOT_LOGGER = Logger.getMessageLogger(ProtocolLogger.class, ProtocolLogger.class.getPackage().getName());

    /**
     * A logger with the category {@code org.jboss.as.protocol.client}.
     */
    ProtocolLogger CLIENT_LOGGER = Logger.getMessageLogger(ProtocolLogger.class, "org.jboss.as.protocol.client");

    /**
     * A logger with the category {@code org.jboss.as.protocol.connection}.
     */
    ProtocolLogger CONNECTION_LOGGER = Logger.getMessageLogger(ProtocolLogger.class, "org.jboss.as.protocol.connection");

    /**
     * A logger with the category {@code org.jboss.as.protocol.server}.
     */
    ProtocolLogger SERVER_LOGGER = Logger.getMessageLogger(ProtocolLogger.class, "org.jboss.as.protocol.server");

    /**
     * Logs a warning message indicating an error occurred when closing the channel.
     *
     * @param message the error message.
     */
    @LogMessage(level = WARN)
    @Message(id = 12100, value = "Got error closing channel %s")
    void errorClosingChannel(String message);

    /**
     * Logs an error message indicating a failure to accept the connection.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12101, value = "Failed to accept a connection")
    void failedToAcceptConnection(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to close the resource.
     *
     * @param cause    the cause of the error.
     * @param resource the resource.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12102, value = "Failed to close resource %s")
    void failedToCloseResource(@Cause Throwable cause, Object resource);

    /**
     * Logs an error message indicating a failure to close the server socket.
     *
     * @param cause  the cause of the error.
     * @param socket the server socket.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12103, value = "Failed to close the server socket %s")
    void failedToCloseServerSocket(@Cause Throwable cause, ServerSocket socket);

    /**
     * Logs an error message indicating a failure to close the socket.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12104, value = "Failed to close a socket")
    void failedToCloseSocket(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to finish the marshaller.
     *
     * @param cause      the cause of the error.
     * @param marshaller the marshaller in error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12105, value = "Failed to finish the marshaller %s")
    void failedToFinishMarshaller(@Cause Throwable cause, Marshaller marshaller);

    /**
     * Logs an error message indicating a failure to finish the unmarshaller.
     *
     * @param cause        the cause of the error.
     * @param unmarshaller the marshaller in error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12106, value = "Failed to finish the unmarshaller %s")
    void failedToFinishUnmarshaller(@Cause Throwable cause, Unmarshaller unmarshaller);

    /**
     * Logs an error message indicating a failure to handle the incoming connection.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12107, value = "Failed to handle incoming connection")
    void failedToHandleIncomingConnection(@Cause Throwable cause);

    /**
     * Logs an error messaged indicating a failure to handle the socket failure condition.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12108, value = "Failed to handle socket failure condition")
    void failedToHandleSocketFailure(@Cause Throwable cause);

    /**
     * Logs an error messaged indicating a failure to handle the socket finished condition.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12109, value = "Failed to handle socket finished condition")
    void failedToHandleSocketFinished(@Cause Throwable cause);

    /**
     * Logs an error messaged indicating a failure to handle the socket shut down condition.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12110, value = "Failed to handle socket shut down condition")
    void failedToHandleSocketShutdown(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to read a message.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12111, value = "Failed to read a message")
    void failedToReadMessage(@Cause Throwable cause);

    /**
     * Logs a warning message indicating the leakage of the message outout stream.
     */
    @LogMessage(level = WARN)
    @Message(id = 12112, value = "Leaked a message output stream; cleaning")
    void leakedMessageOutputStream();

    /**
     * Logs a warning message indicating the wrong channel was received.
     */
    @LogMessage(level = WARN)
    @Message(id = 12113, value = "Received end for wrong channel!")
    void receivedWrongChannel();

    @LogMessage(level = WARN)
    @Message(id = 12114, value = "Executor is not needed for client")
    void executorNotNeeded();

    @LogMessage(level = WARN)
    @Message(id = 12115, value = "Connection timeout is no longer needed for client")
    void connectTimeoutNotNeeded();

    @LogMessage(level = WARN)
    @Message(id = 12117, value = "Connection timeout property is no longer needed for client")
    void connectTimeoutPropertyNotNeeded();

    @LogMessage(level = WARN)
    @Message(id = 12118, value = "No such request (%d) associated with channel %s")
    void noSuchRequest(int requestId, Channel channel);

}
