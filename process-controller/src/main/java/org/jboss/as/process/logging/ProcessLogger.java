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

package org.jboss.as.process.logging;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;

import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 29.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYPC", length = 4)
public interface ProcessLogger extends BasicLogger {
    /**
     * The root logger with a category of the package.
     */
    ProcessLogger ROOT_LOGGER = Logger.getMessageLogger(ProcessLogger.class, "org.jboss.as.process");

    /**
     * A logger with the category {@code org.jboss.as.process-controller.client}.
     */
    ProcessLogger CLIENT_LOGGER = Logger.getMessageLogger(ProcessLogger.class, "org.jboss.as.process-controller.client");

    /**
     * A logger with the category {@code org.jboss.as.process.protocol}
     */
    ProcessLogger PROTOCOL_LOGGER = Logger.getMessageLogger(ProcessLogger.class, "org.jboss.as.process.protocol");

    /**
     * A logger with the category {@code org.jboss.as.process.protocol.client}
     */
    ProcessLogger PROTOCOL_CLIENT_LOGGER = Logger.getMessageLogger(ProcessLogger.class, "org.jboss.as.protocol.client");

    /**
     * A logger with the category {@code org.jboss.as.process.protocol.client}
     */
    ProcessLogger PROTOCOL_CONNECTION_LOGGER = Logger.getMessageLogger(ProcessLogger.class, "org.jboss.as.protocol.connection");

    /**
     * A logger with the category {@code org.jboss.as.process-controller.server}.
     */
    ProcessLogger SERVER_LOGGER = Logger.getMessageLogger(ProcessLogger.class, "org.jboss.as.process-controller.server");

    /**
     * Logs a warning message indicating an attempt to reconnect a non-existent process.
     *
     * @param processName the name of the process.
     */
    @LogMessage(level = WARN)
    @Message(id = 1, value = "Attempted to reconnect non-existent process '%s'")
    void attemptToReconnectNonExistentProcess(String processName);

    /**
     * Logs a warning message indicating an attempt to remove a non-existent process.
     *
     * @param processName the name of the process.
     */
    @LogMessage(level = WARN)
    @Message(id = 2, value = "Attempted to remove non-existent process '%s'")
    void attemptToRemoveNonExistentProcess(String processName);

    /**
     * Logs a warning message indicating an attempt to start a non-existent process.
     *
     * @param processName the name of the process.
     */
    @LogMessage(level = WARN)
    @Message(id = 3, value = "Attempted to start non-existent process '%s'")
    void attemptToStartNonExistentProcess(String processName);

    /**
     * Logs a warning message indicating an attempt to stop a non-existent process.
     *
     * @param processName the name of the process.
     */
    @LogMessage(level = WARN)
    @Message(id = 4, value = "Attempted to stop non-existent process '%s'")
    void attemptToStopNonExistentProcess(String processName);

    /**
     * Logs a warning message indicating an attempt to register a duplicate named process.
     *
     * @param processName the duplicate name.
     */
    @LogMessage(level = WARN)
    @Message(id = 5, value = "Attempted to register duplicate named process '%s'")
    void duplicateProcessName(String processName);

    /**
     * Logs a warning message indicating the authentication key failed to send to the process.
     *
     * @param processName the process name.
     * @param error       th error.
     */
    @LogMessage(level = WARN)
    @Message(id = 6, value = "Failed to send authentication key to process '%s': %s")
    void failedToSendAuthKey(String processName, Throwable error);

    /**
     * Logs an error message indicating the data bytes failed to send to the process input stream.
     *
     * @param cause       the cause of the error.
     * @param processName the process name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 7, value = "Failed to send data bytes to process '%s' input stream")
    void failedToSendDataBytes(@Cause Throwable cause, String processName);

    /**
     * Logs an error message indicating the reconnect message failed to send to the process input stream.
     *
     * @param cause       the cause of the error.
     * @param processName the process name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 8, value = "Failed to send reconnect message to process '%s' input stream")
    void failedToSendReconnect(@Cause Throwable cause, String processName);

    /**
     * Logs an error message indicating the process failed to start.
     *
     * @param processName the process name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 9, value = "Failed to start process '%s'")
    void failedToStartProcess(String processName);

    /**
     * Logs an error message indicating a failure to write a message to the connection.
     *
     * @param messageType the type of the message that failed to write.
     * @param t           the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10, value = "Failed to write %s message to connection: %s")
    void failedToWriteMessage(String messageType, Throwable t);

    /**
     * Logs an informational message indicating the process has finished with the exit status code.
     *
     * @param processName the process name.
     * @param exitCode    the exit code.
     */
    @LogMessage(level = INFO)
    @Message(id = 11, value = "Process '%s' finished with an exit status of %d")
    void processFinished(String processName, int exitCode);

    /**
     * Logs a warning message indicating a connection with an invalid version from the address, represented by the
     * {@code address} parameter, was received.
     *
     * @param address the address.
     */
    @LogMessage(level = WARN)
    @Message(id = 12, value = "Received connection with invalid version from %s")
    void receivedInvalidVersion(InetAddress address);

    /**
     * Logs a warning message indicating an unknown greeting code, represented by the {@code code} parameter,
     * was received by the address, represented by the {@code address} parameter.
     *
     * @param code    the unknown code.
     * @param address the address
     */
    @LogMessage(level = WARN)
    @Message(id = 13, value = "Received unrecognized greeting code 0x%02x from %s")
    void receivedUnknownGreetingCode(int code, InetAddress address);

    /**
     * Logs a warning message indicating unknown credentials were received by the address, represented by the
     * {@code address} parameter.
     *
     * @param address the address
     */
    @LogMessage(level = WARN)
    @Message(id = 14, value = "Received connection with unknown credentials from %s")
    void receivedUnknownCredentials(InetAddress address);

    /**
     * Logs a warning message indicating an unknown message with the code, represented by the {@code code} parameter,
     * was received.
     *
     * @param code the unknown code.
     */
    @LogMessage(level = WARN)
    @Message(id = 15, value = "Received unknown message with code 0x%02x")
    void receivedUnknownMessageCode(int code);

    /**
     * Logs an informational message indicating the process controller shutdown is complete.
     */
    @LogMessage(level = INFO)
    @Message(id = 16, value = "All processes finished; exiting")
    void shutdownComplete();

    /**
     * Logs an informational message indicating the process controller is shutting down.
     */
    @LogMessage(level = INFO)
    @Message(id = 17, value = "Shutting down process controller")
    void shuttingDown();

    /**
     * Logs an informational message indicating the process is starting.
     *
     * @param processName the process name.
     */
    @LogMessage(level = INFO)
    @Message(id = 18, value = "Starting process '%s'")
    void startingProcess(String processName);

    /**
     * Logs an informational message indicating the process is stopping.
     *
     * @param processName the process name.
     */
    @LogMessage(level = INFO)
    @Message(id = 19, value = "Stopping process '%s'")
    void stoppingProcess(String processName);

    /**
     * Logs an error message indicating the stream processing failed for the process.
     *
     * @param processName the process name.
     * @param error       the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 20, value = "Stream processing failed for process '%s': %s")
    void streamProcessingFailed(String processName, Throwable error);

    /**
     * Logs an informational message that the respawn is waiting until another attempt
     * is made to restart the process.
     *
     * @param seconds the seconds
     * @param processName the process name
     */
    @LogMessage(level = INFO)
    @Message(id = 21, value = "Waiting %d seconds until trying to restart process %s.")
    void waitingToRestart(int seconds, String processName);

    @LogMessage(level = WARN)
    @Message(id = 22, value = "Failed to kill process '%s', trying to destroy the process instead.")
    void failedToKillProcess(String process);

    @Message(id = Message.NONE, value = "Usage: %s [args...]%nwhere args include:")
    String argUsage(String executableName);

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#BACKUP_DC} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Keep a copy of the persistent domain configuration even if this host is not the Domain Controller")
    String argBackup();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#CACHED_DC} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "If this host is not the Domain Controller and cannot contact the Domain Controller at boot, boot using a locally cached copy of the domain configuration (see --backup)")
    String argCachedDc();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#DOMAIN_CONFIG} command line arguments.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the domain configuration file to use (default is \"domain.xml\") (Same as -c)")
    String argDomainConfig();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#SHORT_DOMAIN_CONFIG} command line arguments.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the domain configuration file to use (default is \"domain.xml\") (Same as --domain-config)")
    String argShortDomainConfig();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#READ_ONLY_DOMAIN_CONFIG} command line arguments.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the domain configuration file to use. This differs from '" + CommandLineConstants.DOMAIN_CONFIG + "', '" +
            CommandLineConstants.SHORT_DOMAIN_CONFIG + "' and '" + CommandLineConstants.OLD_DOMAIN_CONFIG + "' in that the initial file is never overwritten.")
    String argReadOnlyDomainConfig();

    /**
     * Instructions for the {@link CommandLineConstants#SHORT_HELP} or {@link CommandLineConstants#HELP} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Display this message and exit")
    String argHelp();

    /**
     * Instructions for the {@link CommandLineConstants#INTERPROCESS_HC_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Address on which the host controller should listen for communication from the process controller")
    String argInterProcessHcAddress();

    /**
     * Instructions for the {@link CommandLineConstants#INTERPROCESS_HC_PORT} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Port on which the host controller should listen for communication from the process controller")
    String argInterProcessHcPort();

    /**
     * Instructions for the {@link CommandLineConstants#HOST_CONFIG} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the host configuration file to use (default is \"host.xml\")")
    String argHostConfig();

    /**
     * Instructions for the {@link CommandLineConstants#READ_ONLY_HOST_CONFIG} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the host configuration file to use. This differs from '" + CommandLineConstants.HOST_CONFIG + "' in that the initial file is never overwritten.")
    String argReadOnlyHostConfig();

    /**
     * Instructions for the {@link CommandLineConstants#PROCESS_CONTROLLER_BIND_ADDR} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Address on which the process controller listens for communication from processes it controls")
    String argPcAddress();

    /**
     * Instructions for the {@link CommandLineConstants#PROCESS_CONTROLLER_BIND_PORT} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Port on which the process controller listens for communication from processes it controls")
    String argPcPort();

    /**
     * Instructions for the {@link CommandLineConstants#SHORT_PROPERTIES} or {@link CommandLineConstants#PROPERTIES} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Load system properties from the given url")
    String argProperties();

    /**
     * Instructions for the {@link CommandLineConstants#SYS_PROP} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set a system property")
    String argSystem();

    /**
     * Instructions for the {@link CommandLineConstants#SHORT_VERSION}, {@link CommandLineConstants#OLD_SHORT_VERSION} or {@link CommandLineConstants#VERSION} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Print version and exit")
    String argVersion();

    /**
     * Instructions for the {@link CommandLineConstants#PUBLIC_BIND_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.bind.address to the given value")
    String argPublicBindAddress();

    /**
     * Instructions for the {@code -b<interface></interface>} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.bind.address.<interface> to the given value")
    String argInterfaceBindAddress();

    /**
     * Instructions for the {@link CommandLineConstants#DEFAULT_MULTICAST_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.default.multicast.address to the given value")
    String argDefaultMulticastAddress();

    /**
     * Instructions for the {@link CommandLineConstants#ADMIN_ONLY} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set the host controller's running type to ADMIN_ONLY causing it to open administrative interfaces and accept management requests but not start servers or, if this host controller is the master for the domain, accept incoming connections from slave host controllers.")
    String argAdminOnly();

    /**
     * Instructions for the {@link CommandLineConstants#MASTER_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.domain.master.address to the given value. In a default slave Host Controller config, this is used to configure the address of the master Host Controller.")
    String argMasterAddress();

    /**
     * Instructions for the {@link CommandLineConstants#MASTER_PORT} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.domain.master.port to the given value. In a default slave Host Controller config, this is used to configure the port used for native management communication by the master Host Controller.")
    String argMasterPort();

    /**
     * Error message indicating no value was provided for a command line argument.
     *
     * @param argument the name of the argument
     *
     * @return the message.
     */
    @Message(id = 23, value = "No value was provided for argument %s")
    String noArgValue(String argument);

    /**
     * Creates an exception indicating the Java executable could not be found.
     *
     * @param binDir the directory the executable file should be located.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 24, value = "Could not find java executable under %s.")
    IllegalStateException cannotFindJavaExe(String binDir);

    /**
     * Creates an exception indicating the authentication key must be 16 bytes long.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 25, value = "Authentication key must be 16 bytes long")
    IllegalArgumentException invalidAuthKeyLen();

    /**
     * Creates an exception indicating the command must have at least one entry.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 26, value = "cmd must have at least one entry")
    IllegalArgumentException invalidCommandLen();

    /**
     * Creates an exception indicating the Java home directory does not exist.
     *
     * @param dir the directory to Java home.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 27, value = "Java home '%s' does not exist.")
    IllegalStateException invalidJavaHome(String dir);

    /**
     * Creates an exception indicating the Java home bin directory does not exist.
     *
     * @param binDir      the bin directory.
     * @param javaHomeDir the Java home directory.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 28, value = "Java home's bin '%s' does not exist. The home directory was determined to be %s.")
    IllegalStateException invalidJavaHomeBin(String binDir, String javaHomeDir);

    /**
     * Creates an exception indicating the parameter has an invalid length.
     *
     * @param parameterName the parameter name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 29, value = "%s length is invalid")
    IllegalArgumentException invalidLength(String parameterName);

    /**
     * Creates an exception indicating the option, represented by the {@code option} parameter, is invalid.
     *
     * @param option the invalid option.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 30, value = "Invalid option: %s")
    IllegalArgumentException invalidOption(String option);

    /**
     * Creates an exception indicating a command contains a {@code null} component.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 31, value = "Command contains a null component")
    IllegalArgumentException nullCommandComponent();

    /**
     * Creates an exception indicating the variable is {@code null}.
     *
     * @param varName the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 32, value = "%s is null")
    IllegalArgumentException nullVar(String varName);

    /**
     * Logs an error message indicating a failure to accept the connection.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 33, value = "Failed to accept a connection")
    void failedToAcceptConnection(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to close the resource.
     *
     * @param cause    the cause of the error.
     * @param resource the resource.
     */
    @LogMessage(level = ERROR)
    @Message(id = 34, value = "Failed to close resource %s")
    void failedToCloseResource(@Cause Throwable cause, Object resource);

    /**
     * Logs an error message indicating a failure to close the server socket.
     *
     * @param cause  the cause of the error.
     * @param socket the server socket.
     */
    @LogMessage(level = ERROR)
    @Message(id = 35, value = "Failed to close the server socket %s")
    void failedToCloseServerSocket(@Cause Throwable cause, ServerSocket socket);

    /**
     * Logs an error message indicating a failure to close the socket.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 36, value = "Failed to close a socket")
    void failedToCloseSocket(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to finish the marshaller.
     *
     * @param cause      the cause of the error.
     * @param marshaller the marshaller in error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 37, value = "Failed to finish the marshaller %s")
    void failedToFinishMarshaller(@Cause Throwable cause, Marshaller marshaller);

    /**
     * Logs an error message indicating a failure to finish the unmarshaller.
     *
     * @param cause        the cause of the error.
     * @param unmarshaller the marshaller in error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 38, value = "Failed to finish the unmarshaller %s")
    void failedToFinishUnmarshaller(@Cause Throwable cause, Unmarshaller unmarshaller);

    /**
     * Logs an error message indicating a failure to handle the incoming connection.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 39, value = "Failed to handle incoming connection")
    void failedToHandleIncomingConnection(@Cause Throwable cause);

    /**
     * Logs an error messaged indicating a failure to handle the socket failure condition.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 40, value = "Failed to handle socket failure condition")
    void failedToHandleSocketFailure(@Cause Throwable cause);

    /**
     * Logs an error messaged indicating a failure to handle the socket finished condition.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 41, value = "Failed to handle socket finished condition")
    void failedToHandleSocketFinished(@Cause Throwable cause);

    /**
     * Logs an error messaged indicating a failure to handle the socket shut down condition.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 42, value = "Failed to handle socket shut down condition")
    void failedToHandleSocketShutdown(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to read a message.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 43, value = "Failed to read a message")
    void failedToReadMessage(@Cause Throwable cause);

    /**
     * Logs a warning message indicating the leakage of the message outout stream.
     */
    @LogMessage(level = WARN)
    @Message(id = 44, value = "Leaked a message output stream; cleaning")
    void leakedMessageOutputStream();

    /**
     * Creates an exception indicating a failure to create the server thread.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 45, value = "Failed to create server thread")
    IOException failedToCreateServerThread();

    /**
     * Creates an exception indicating a failure to read the object.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 46, value = "Failed to read object")
    IOException failedToReadObject(@Cause Throwable cause);

    /**
     * Creates an exception indicating an invalid byte.
     *
     * @return an {@link java.io.UTFDataFormatException} for the error.
     */
    @Message(id = 47, value = "Invalid byte")
    UTFDataFormatException invalidByte();

    /**
     * Creates an exception indicating an invalid byte.
     *
     * @param c the character.
     * @param i the raw integer.
     *
     * @return an {@link java.io.UTFDataFormatException} for the error.
     */
    @Message(id = 48, value = "Invalid byte:%s(%d)")
    UTFDataFormatException invalidByte(char c, int i);

    /**
     * Creates an exception indicating an invalid byte token was found.
     *
     * @param expected the expected value.
     * @param actual   the actual value.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 49, value = "Invalid byte token.  Expecting '%s' received '%s'")
    IOException invalidByteToken(int expected, byte actual);

    /**
     * Creates an exception indicating an invalid command byte was read.
     *
     * @param commandByte the command byte read.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 50, value = "Invalid command byte read: %s")
    IOException invalidCommandByte(int commandByte);

    // @Message(id = 61, value = "Invalid signature [%s]")
    // IOException invalidSignature(String signature);

    /**
     * Creates an exception indicating an invalid start chunk was found.
     *
     * @param chunk the start chunk.
     *
     * @return an {@code IOException} for the error.
     */
    @Message(id = 51, value = "Invalid start chunk start [%s]")
    IOException invalidStartChunk(int chunk);

    // @Message(id = 53, value = "Invalid type: %s")
    // IOException invalidType(String type);

    // @Message(id = 54, value = "Type is neither %s or %s: %s")
    // IllegalArgumentException invalidType(String validType1, String validType2, byte providedType);

    // @Message(id = 55, value = "Only '%s' is a valid url")
    // IllegalArgumentException invalidUrl(String url);

    /**
     * Creates an exception indicating the number of bytes read.
     *
     * @param bytesRead the number of bytes read.
     *
     * @return an {@link java.io.EOFException} for the error.
     */
    @Message(id = 56, value = "Read %d bytes.")
    EOFException readBytes(int bytesRead);

    //    /**
    //     * Creates an exception indicating there was no request handler found with the id in the operation handler.
    //     *
    //     * @param id               the id of the request handler.
    //     * @param operationHandler the operation handler the id was not found in.
    //     *
    //     * @return an {@link java.io.IOException} for the error.
    //     */
    //    @Message(id = 57, value = "No request handler found with id %s in operation handler %s")
    //    IOException requestHandlerIdNotFound(byte id, ManagementOperationHandler operationHandler);

    /**
     * Creates an exception indicating the stream is closed.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 58, value = "Stream closed")
    IOException streamClosed();

    /**
     * Creates an exception indicating the thread creation was refused.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 59, value = "Thread creation was refused")
    IllegalStateException threadCreationRefused();

    /**
     * Creates an exception indicating an unexpected end of stream was detected.
     *
     * @return an {@link java.io.EOFException} for the error.
     */
    @Message(id = 60, value = "Unexpected end of stream")
    EOFException unexpectedEndOfStream();

    /**
     * Creates an exception indicating the write channel is closed.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 61, value = "Write channel closed")
    IOException writeChannelClosed();

    /**
     * Creates an exception indicating the writes have already been shutdown.
     *
     * @return an {@link java.io.IOException} for the error.
     */
    @Message(id = 62, value = "Writes are already shut down")
    IOException writesAlreadyShutdown();

}
