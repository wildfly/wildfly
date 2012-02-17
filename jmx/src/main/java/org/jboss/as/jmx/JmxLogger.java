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

package org.jboss.as.jmx;

import javax.management.ObjectName;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface JmxLogger extends BasicLogger {

    /**
     * A logger with the category of the package name.
     */
    JmxLogger ROOT_LOGGER = Logger.getMessageLogger(JmxLogger.class, JmxLogger.class.getPackage().getName());

    /**
     * Creates an exception indicating the inability to shutdown the RMI registry.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11300, value = "Could not shutdown rmi registry")
    void cannotShutdownRmiRegistry(@Cause Throwable cause);

    /**
     * Creates an exception indicating the JMX connector could not unbind from the registry.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11301, value = "Could not stop connector server")
    void cannotStopConnectorServer(@Cause Throwable cause);

    /**
     * Creates an exception indicating the JMX connector could not unbind from the registry.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11302, value = "Could not unbind jmx connector from registry")
    void cannotUnbindConnector(@Cause Throwable cause);

    /**
     * Logs a warning message indicating no {@link javax.management.ObjectName} is available to unregister.
     */
    @LogMessage(level = WARN)
    @Message(id = 11303, value = "No ObjectName available to unregister")
    void cannotUnregisterObject();

    /**
     * Logs an error message indicating a failure to unregister the object name.
     *
     * @param cause the cause of the error.
     * @param name  the name of the object name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11304, value = "Failed to unregister [%s]")
    void unregistrationFailure(@Cause Throwable cause, ObjectName name);


    /**
     * The jmx-connector element is no longer supported.
     *
     */
    @LogMessage(level = WARN)
    @Message(id = 11305, value = "<jmx-connector/> is no longer supporting. <remoting-connector/> should be used instead to allow remote connections via JBoss Remoting.")
    void jmxConnectorNotSupported();
}
