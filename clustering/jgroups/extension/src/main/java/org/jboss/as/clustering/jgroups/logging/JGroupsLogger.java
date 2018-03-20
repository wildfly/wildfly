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

package org.jboss.as.clustering.jgroups.logging;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.net.URL;
import java.net.UnknownHostException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYCLJG", length = 4)
public interface JGroupsLogger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = "org.jboss.as.clustering.jgroups";

    /**
     * The root logger.
     */
    JGroupsLogger ROOT_LOGGER = Logger.getMessageLogger(JGroupsLogger.class, ROOT_LOGGER_CATEGORY);

    /**
     * Logs an informational message indicating the JGroups subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating JGroups subsystem. JGroups version %s")
    void activatingSubsystem(String version);

//    @LogMessage(level = TRACE)
//    @Message(id = 2, value = "Setting %s.%s=%d")
//    void setProtocolPropertyValue(String protocol, String property, Object value);

//    @LogMessage(level = TRACE)
//    @Message(id = 3, value = "Failed to set non-existent %s.%s=%d")
//    void nonExistentProtocolPropertyValue(@Cause Throwable cause, String protocolName, String propertyName, Object propertyValue);

//    @LogMessage(level = TRACE)
//    @Message(id = 4, value = "Could not set %s.%s and %s.%s, %s socket binding does not specify a multicast socket")
//    void couldNotSetAddressAndPortNoMulticastSocket(@Cause Throwable cause, String protocolName, String addressProperty, String protocolNameAgain, String portProperty, String bindingName);

//    @LogMessage(level = ERROR)
//    @Message(id = 5, value = "Error accessing original value for property %s of protocol %s")
//    void unableToAccessProtocolPropertyValue(@Cause Throwable cause, String propertyName, String protocolName);

//    @LogMessage(level = WARN)
//    @Message(id = 6, value = "property %s for protocol %s attempting to override socket binding value %s : property value %s will be ignored")
//    void unableToOverrideSocketBindingValue(String propertyName, String protocolName, String bindingName, Object propertyValue);


    /**
     * A message indicating a file could not be parsed.
     *
     * @param url the path to the file.
     * @return the message.
     */
    @Message(id = 7, value = "Failed to parse %s")
    String parserFailure(URL url);

    /**
     * A message indicating a resource could not be located.
     *
     * @param resource the resource that could not be located.
     * @return the message.
     */
    @Message(id = 8, value = "Failed to locate %s")
    String notFound(String resource);

//    @Message(id = 9, value = "A node named %s already exists in this cluster. Perhaps there is already a server running on this host? If so, restart this server with a unique node name, via -Djboss.node.name=<node-name>")
//    IllegalStateException duplicateNodeName(String name);

    @Message(id = 10, value = "Transport for stack %s is not defined. Please specify both a transport and protocol list, either as optional parameters to add() or via batching.")
    OperationFailedException transportNotDefined(String stackName);

//    @Message(id = 11, value = "Protocol list for stack %s is not defined. Please specify both a transport and protocol list, either as optional parameters to add() or via batching.")
//    OperationFailedException protocolListNotDefined(String stackName);

//    @Message(id = 12, value = "Protocol with relative path %s is already defined.")
//    OperationFailedException protocolAlreadyDefined(String relativePath);

//    @Message(id = 13, value = "Protocol with relative path %s is not defined.")
//    OperationFailedException protocolNotDefined(String relativePath);

//    @Message(id = 14, value = "Property %s for protocol with relative path %s is not defined.")
//    OperationFailedException propertyNotDefined(String propertyName, String protocolRelativePath);

    @Message(id = 15, value = "Unknown metric %s")
    String unknownMetric(String metricName);

    @Message(id = 16, value = "Unable to load protocol class %s")
    OperationFailedException unableToLoadProtocolClass(String protocolName);

    @Message(id = 17, value = "Privileged access exception on attribute/method %s")
    String privilegedAccessExceptionForAttribute(String attrName);

//    @Message(id = 18, value = "Instantiation exception on converter for attribute/method %s")
//    String instantiationExceptionOnConverterForAttribute(String attrName);

//    @Message(id = 20, value = "Unable to load protocol class %s")
//    String unableToLoadProtocol(String protocolName);

    @Message(id = 21, value = "Attributes referencing threads subsystem can only be used to support older slaves in the domain.")
    String threadsAttributesUsedInRuntime();

    @Message(id = 22, value = "%s entry not found in configured key store")
    IllegalArgumentException keyEntryNotFound(String alias);

    @Message(id = 23, value = "%s key store entry is not of the expected type: %s")
    IllegalArgumentException unexpectedKeyStoreEntryType(String alias, String type);

    @Message(id = 24, value = "%s key store entry does not contain a secret key")
    IllegalArgumentException secretKeyStoreEntryExpected(String alias);

    @Message(id = 25, value = "Configured credential source does not reference a clear-text password credential")
    IllegalArgumentException unexpectedCredentialSource();

//    @Message(id = 26, value = "No %s operation registered at %s")
//    OperationFailedException operationNotDefined(String operation, String address);

//    @Message(id = 27, value = "Failed to synthesize key-store add operation due to missing %s property")
//    OperationFailedException missingKeyStoreProperty(String propertyName);

    @Message(id = 28, value = "Could not resolve destination address for outbound socket binding named '%s'")
    IllegalArgumentException failedToResolveSocketBinding(@Cause UnknownHostException cause, OutboundSocketBinding binding);

//    @LogMessage(level = WARN)
//    @Message(id = 29, value = "Ignoring unrecognized %s properties: %s")
//    void ignoredProperties(String protocol, Map<String, String> properties);

    @LogMessage(level = WARN)
    @Message(id = 30, value = "Protocol %s is obsolete and will be auto-updated to %s")
    void legacyProtocol(String legacyProtocol, String targetProtocol);
}
