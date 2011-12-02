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

package org.jboss.as.process;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import java.net.InetAddress;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * This module is using message IDs in the range 12000-12099.
 * This file is using the subset 12000-12039 for logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 * Date: 29.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
interface ProcessLogger extends BasicLogger {
    /**
     * The root logger with a category of the package.
     */
    ProcessLogger ROOT_LOGGER = Logger.getMessageLogger(ProcessLogger.class, ProcessLogger.class.getPackage().getName());

    /**
     * A logger with the category {@code org.jboss.as.process-controller.client}.
     */
    ProcessLogger CLIENT_LOGGER = Logger.getMessageLogger(ProcessLogger.class, "org.jboss.as.process-controller.client");

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
    @Message(id = 12000, value = "Attempted to reconnect non-existent process '%s'")
    void attemptToReconnectNonExistentProcess(String processName);

    /**
     * Logs a warning message indicating an attempt to remove a non-existent process.
     *
     * @param processName the name of the process.
     */
    @LogMessage(level = WARN)
    @Message(id = 12001, value = "Attempted to remove non-existent process '%s'")
    void attemptToRemoveNonExistentProcess(String processName);

    /**
     * Logs a warning message indicating an attempt to start a non-existent process.
     *
     * @param processName the name of the process.
     */
    @LogMessage(level = WARN)
    @Message(id = 12002, value = "Attempted to start non-existent process '%s'")
    void attemptToStartNonExistentProcess(String processName);

    /**
     * Logs a warning message indicating an attempt to stop a non-existent process.
     *
     * @param processName the name of the process.
     */
    @LogMessage(level = WARN)
    @Message(id = 12003, value = "Attempted to stop non-existent process '%s'")
    void attemptToStopNonExistentProcess(String processName);

    /**
     * Logs a warning message indicating an attempt to register a duplicate named process.
     *
     * @param processName the duplicate name.
     */
    @LogMessage(level = WARN)
    @Message(id = 12004, value = "Attempted to register duplicate named process '%s'")
    void duplicateProcessName(String processName);

    /**
     * Logs a warning message indicating the authentication key failed to send to the process.
     *
     * @param processName the process name.
     * @param error       th error.
     */
    @LogMessage(level = WARN)
    @Message(id = 12005, value = "Failed to send authentication key to process '%s': %s")
    void failedToSendAuthKey(String processName, Throwable error);

    /**
     * Logs an error message indicating the data bytes failed to send to the process input stream.
     *
     * @param cause       the cause of the error.
     * @param processName the process name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12006, value = "Failed to send data bytes to process '%s' input stream")
    void failedToSendDataBytes(@Cause Throwable cause, String processName);

    /**
     * Logs an error message indicating the reconnect message failed to send to the process input stream.
     *
     * @param cause       the cause of the error.
     * @param processName the process name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12007, value = "Failed to send reconnect message to process '%s' input stream")
    void failedToSendReconnect(@Cause Throwable cause, String processName);

    /**
     * Logs an error message indicating the process failed to start.
     *
     * @param processName the process name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12008, value = "Failed to start process '%s'")
    void failedToStartProcess(String processName);

    /**
     * Logs an error message indicating a failure to write a message to the connection.
     *
     * @param messageType the type of the message that failed to write.
     * @param t           the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12009, value = "Failed to write %s message to connection: %s")
    void failedToWriteMessage(String messageType, Throwable t);

    /**
     * Logs an informational message indicating the process has finished with the exit status code.
     *
     * @param processName the process name.
     * @param exitCode    the exit code.
     */
    @LogMessage(level = INFO)
    @Message(id = 12010, value = "Process '%s' finished with an exit status of %d")
    void processFinished(String processName, int exitCode);

    /**
     * Logs a warning message indicating a connection with an invalid version from the address, represented by the
     * {@code address} parameter, was received.
     *
     * @param address the address.
     */
    @LogMessage(level = WARN)
    @Message(id = 12011, value = "Received connection with invalid version from %s")
    void receivedInvalidVersion(InetAddress address);

    /**
     * Logs a warning message indicating an unknown greeting code, represented by the {@code code} parameter,
     * was received by the address, represented by the {@code address} parameter.
     *
     * @param code    the unknown code.
     * @param address the address
     */
    @LogMessage(level = WARN)
    @Message(id = 12012, value = "Received unrecognized greeting code 0x%02x from %s")
    void receivedUnknownGreetingCode(int code, InetAddress address);

    /**
     * Logs a warning message indicating unknown credentials were received by the address, represented by the
     * {@code address} parameter.
     *
     * @param address the address
     */
    @LogMessage(level = WARN)
    @Message(id = 12013, value = "Received connection with unknown credentials from %s")
    void receivedUnknownCredentials(InetAddress address);

    /**
     * Logs a warning message indicating an unknown message with the code, represented by the {@code code} parameter,
     * was received.
     *
     * @param code the unknown code.
     */
    @LogMessage(level = WARN)
    @Message(id = 120014, value = "Received unknown message with code 0x%02x")
    void receivedUnknownMessageCode(int code);

    /**
     * Logs an informational message indicating the process controller shutdown is complete.
     */
    @LogMessage(level = INFO)
    @Message(id = 12015, value = "All processes finished; exiting")
    void shutdownComplete();

    /**
     * Logs an informational message indicating the process controller is shutting down.
     */
    @LogMessage(level = INFO)
    @Message(id = 12016, value = "Shutting down process controller")
    void shuttingDown();

    /**
     * Logs an informational message indicating the process is starting.
     *
     * @param processName the process name.
     */
    @LogMessage(level = INFO)
    @Message(id = 12017, value = "Starting process '%s'")
    void startingProcess(String processName);

    /**
     * Logs an informational message indicating the process is stopping.
     *
     * @param processName the process name.
     */
    @LogMessage(level = INFO)
    @Message(id = 12018, value = "Stopping process '%s'")
    void stoppingProcess(String processName);

    /**
     * Logs an error message indicating the stream processing failed for the process.
     *
     * @param processName the process name.
     * @param error       the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12019, value = "Stream processing failed for process '%s': %s")
    void streamProcessingFailed(String processName, Throwable error);

    /**
     * Logs an informational message that the respawn is waiting until another attempt
     * is made to restart the process.
     *
     * @param seconds the seconds
     * @param processName the process name
     */
    @LogMessage(level = INFO)
    @Message(id = 12020, value = "Waiting %d seconds until trying to restart process %s.")
    void waitingToRestart(int seconds, String processName);

}
