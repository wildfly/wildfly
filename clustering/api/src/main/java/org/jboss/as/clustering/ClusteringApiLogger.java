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

package org.jboss.as.clustering;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import java.io.Serializable;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 26.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface ClusteringApiLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = ClusteringApiLogger.class.getPackage().getName();

    /**
     * A logger with the category of the default clustering package.
     */
    ClusteringApiLogger ROOT_LOGGER = Logger.getMessageLogger(ClusteringApiLogger.class, ROOT_LOGGER_CATEGORY);

    /**
     * Logs a warning message indicating an exception was thrown invoking a method, represented by the
     * {@code methodName} parameter, on the service, represented by the {@code serviceName} parameter, asynchronously.
     *
     * @param cause       the cause of the error.
     * @param methodName  the method name.
     * @param serviceName the service name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10200, value = "Caught exception asynchronously invoking method %s on service %s")
    void caughtErrorInvokingAsyncMethod(@Cause Throwable cause, String methodName, String serviceName);

    /**
     * Logs an error message indicating a failure to disconnect the channel.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10201, value = "Channel disconnection failed")
    void channelDisconnectError(@Cause Throwable cause);

    /**
     * Logs a warning message indicating the attribute on the element is deprecated.
     *
     * @param attributeName the attribute name.
     * @param elementName   the element name.
     * @param reference     a reference description.
     */
    @LogMessage(level = WARN)
    @Message(id = 10202, value = "The %s attribute of the %s element is deprecated.  See %s")
    void deprecatedAttribute(String attributeName, String elementName, String reference);

    /**
     * Logs an error message indicating a throwable was caught during an asynchronous event.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10203, value = "Caught Throwable handling asynch events")
    void errorHandlingAsyncEvent(@Cause Throwable cause);

    /**
     * Logs an error message indicating the property represented by the {@code propertyName} parameter failed to be set
     * from the service represented by the {@code serviceName} parameter.
     *
     * @param cause        the cause of the error.
     * @param propertyName the property name.
     * @param serviceName  the service name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10204, value = "failed setting %s for service %s")
    void failedSettingServiceProperty(@Cause Throwable cause, String propertyName, String serviceName);

    /**
     * Logs a warning message indicating the service represented by the {@code serviceName} parameter failed to stop.
     *
     * @param cause       the cause of the error.
     * @param serviceName the service name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10205, value = "Failed to stop %s")
    void failedToStop(@Cause Throwable cause, String serviceName);

    /**
     * Logs an informational message indicating the number of cluster members.
     *
     * @param size the number of cluster members.
     */
    @LogMessage(level = INFO)
    @Message(id = 10206, value = "Number of cluster members: %d")
    void numberOfClusterMembers(int size);

    /**
     * Logs an error message indicating the thread was interrupted.
     *
     * @param cause the cause of the error.
     * @param name  the name of the thread.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10207, value = "%s Thread interrupted")
    void threadInterrupted(@Cause Throwable cause, String name);

    /**
     * Logs an error message indicating an {@link InterruptedException} was issued by the caller.
     *
     * @param caller       the object that caused the exception.
     * @param categoryName the category name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10208, value = "Caught InterruptedException; Failing request by %s to lock %s")
    void caughtInterruptedException(Object caller, Serializable categoryName);


    /**
     * Logs a warning message indicating a call to {@code remoteLock} was called from itself.
     */
    @LogMessage(level = WARN)
    @Message(id = 10209, value = "Received remoteLock call from self")
    void receivedRemoteLockFromSelf();
}
