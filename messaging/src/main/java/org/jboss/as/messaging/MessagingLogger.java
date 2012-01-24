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

package org.jboss.as.messaging;

import org.jboss.as.controller.PathAddress;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 10.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface MessagingLogger extends BasicLogger {
    /**
     * The logger with the category of the package.
     */
    MessagingLogger ROOT_LOGGER = Logger.getMessageLogger(MessagingLogger.class, MessagingLogger.class.getPackage().getName());

    /**
     * A logger with the category {@code org.jboss.messaging}.
     */
    MessagingLogger MESSAGING_LOGGER = Logger.getMessageLogger(MessagingLogger.class, "org.jboss.messaging");

    /**
     * Logs a warning message indicating AIO was not found.
     */
    @LogMessage(level = WARN)
    @Message(id = 11600, value = "AIO wasn't located on this platform, it will fall back to using pure Java NIO. If your platform is Linux, install LibAIO to enable the AIO journal")
    void aioWarning();

    /**
     * Logs an informational message indicating a messaging object was bound to the JNDI name represented by the
     * {@code jndiName} parameter.
     *
     * @param jndiName the name the messaging object was bound to.
     */
    @LogMessage(level = INFO)
    @Message(id = 11601, value = "Bound messaging object to jndi name %s")
    void boundJndiName(String jndiName);

    /**
     * Logs an error message indicating an exception occurred while stopping the JMS server.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11602, value = "Exception while stopping JMS server")
    void errorStoppingJmsServer(@Cause Throwable cause);

    /**
     * Logs a warning message indicating the connection factory was not destroyed.
     *
     * @param cause       the cause of the error.
     * @param description the description of what failed to get destroyed.
     * @param name        the name of the factory.
     */
    @LogMessage(level = WARN)
    @Message(id = 11603, value = "Failed to destroy %s: %s")
    void failedToDestroy(@Cause Throwable cause, String description, String name);

    /**
     * Logs a warning message indicating the connection factory was not destroyed.
     *
     * @param description the description of what failed to get destroyed.
     * @param name        the name of the factory.
     */
    @LogMessage(level = WARN)
    void failedToDestroy(String description, String name);

    /**
     * Logs an error message indicating the class, represented by the {@code className} parameter, caught an exception
     * attempting to revert the operation, represented by the {@code operation} parameter, at the address, respresented
     * by the {@code address} parameter.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class that caused the error.
     * @param operation the operation.
     * @param address   the address.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11604, value = "%s caught exception attempting to revert operation %s at address %s")
    void revertOperationFailed(@Cause Throwable cause, String className, String operation, PathAddress address);

    /**
     * Logs an informational message indicating a messaging object was unbound from the JNDI name represented by the
     * {@code jndiName} parameter.
     *
     * @param jndiName the name the messaging object was bound to.
     */
    @LogMessage(level = INFO)
    @Message(id = 11605, value = "Unbound messaging object to jndi name %s")
    void unboundJndiName(String jndiName);

    /**
     */
    @LogMessage(level = ERROR)
    @Message(id = 11606, value = "Could not close file %s")
    void couldNotCloseFile(String file, @Cause Throwable cause);
}
