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

package org.jboss.as.clustering.jgroups;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Date: 29.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface JGroupsLogger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = JGroupsLogger.class.getPackage().getName();

    /**
     * The root logger.
     */
    JGroupsLogger ROOT_LOGGER = Logger.getMessageLogger(JGroupsLogger.class, ROOT_LOGGER_CATEGORY);

    /**
     * Logs an informational message indicating the JGroups subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 10260, value = "Activating JGroups subsystem.")
    void activatingSubsystem();

    @LogMessage(level = TRACE)
    @Message(id = 10261, value = "Setting %s.%s=%d")
    void setProtocolPropertyValue(String protocol, String property, Object value);

    @LogMessage(level = TRACE)
    @Message(id = 10262, value = "Failed to set non-existent %s.%s=%d")
    void nonExistentProtocolPropertyValue(@Cause Throwable cause, String protocolName, String propertyName, Object propertyValue);

    @LogMessage(level = TRACE)
    @Message(id = 10263, value = "Could not set %s.%s and %s.%s, %s socket binding does not specify a multicast socket")
    void couldNotSetAddressAndPortNoMulticastSocket(@Cause Throwable cause, String protocolName, String addressProperty, String protocolNameAgain, String portProperty, String bindingName);

    @LogMessage(level = ERROR)
    @Message(id = 10264, value = "Error accessing original value for property %s of protocol %s")
    void unableToAccessProtocolPropertyValue(@Cause Throwable cause, String propertyName, String protocolName);

    @LogMessage(level = WARN)
    @Message(id = 10265, value = "property %s for protocol %s attempting to override socket binding value %s : property value %s will be ignored")
    void unableToOverrideSocketBindingValue(String propertyName, String protocolName, String bindingName, Object propertyValue);

}
