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

package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import javax.xml.stream.XMLStreamWriter;
import java.io.Closeable;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 02.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface ControllerLogger extends BasicLogger {

    /**
     * Default root logger with category of the package name.
     */
    ControllerLogger ROOT_LOGGER = Logger.getMessageLogger(ControllerLogger.class, ControllerLogger.class.getPackage().getName());

    /**
     * Logger for management operation messages.
     */
    ControllerLogger MGMT_OP_LOGGER = Logger.getMessageLogger(ControllerLogger.class, ControllerLogger.class.getPackage().getName() + ".management-operation");

    /**
     * A logger with the category {@code org.jboss.as.server}
     */
    ControllerLogger SERVER_LOGGER = Logger.getMessageLogger(ControllerLogger.class, "org.jboss.as.server");

    /**
     * A logger with the category {@code org.jboss.server.management}
     */
    ControllerLogger SERVER_MANAGEMENT_LOGGER = Logger.getMessageLogger(ControllerLogger.class, "org.jboss.server.management");

    /**
     * Logs a warning message indicating the address, represented by the {@code address} parameter, could not be
     * resolved, so cannot match it to any InetAddress.
     *
     * @param address the address that could not be resoloved.
     */
    @LogMessage(level = WARN)
    @Message(id = 14600, value = "Cannot resolve address %s, so cannot match it to any InetAddress")
    void cannotResolveAddress(ModelNode address);

    /**
     * Logs an error message indicating there was an error booting the container.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14601, value = "Error booting the container")
    void errorBootingContainer(@Cause Throwable cause);

    /**
     * Logs an error message indicating there was an error booting the container.
     *
     * @param cause         the cause of the error.
     * @param bootStackSize the boot stack size.
     * @param name          the property name to increase the boot stack size.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14602, value = "Error booting the container due to insufficient stack space for the thread used to " +
            "execute boot operations. The thread was configured with a stack size of [%1$d]. Setting " +
            "system property %2$s to a value higher than [%1$d] may resolve this problem.")
    void errorBootingContainer(@Cause Throwable cause, long bootStackSize, String name);

    /**
     * Logs an error message indicating the class, represented by the {@code className} parameter, caught exception
     * attempting to revert the operation, represented by the {@code op} parameter, at the address, represented by the
     * {@code address} parameter.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class that caught the error.
     * @param op        the operation.
     * @param address   the address.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14603, value = "%s caught exception attempting to revert operation %s at address %s")
    void errorRevertingOperation(@Cause Throwable cause, String className, String op, PathAddress address);

    /**
     * Logs an error message indicating a failure to execute the operation, represented by the {@code op} parameter, at
     * the address represented by the {@code path} parameter.
     *
     * @param cause the cause of the error.
     * @param op    the operation.
     * @param path  the path the operation was executed on.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14604, value = "Failed executing operation %s at address %s")
    void failedExecutingOperation(@Cause Throwable cause, ModelNode op, PathAddress path);

    /**
     * Logs an error message indicating a failure executing the subsystem, represented by the {@code name} parameter,
     * boot operations.
     *
     * @param cause the cause of the error.
     * @param name  the name of subsystem.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14605, value = "Failed executing subsystem %s boot operations")
    void failedSubsystemBootOperations(@Cause Throwable cause, String name);

    /**
     * Logs an error message indicating to failure to close the resource represented by the {@code closeable} parameter.
     *
     * @param cause     the cause of the error.
     * @param closeable the resource.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14606, value = "Failed to close resource %s")
    void failedToCloseResource(@Cause Throwable cause, Closeable closeable);

    /**
     * Logs an error message indicating to failure to close the resource represented by the {@code writer} parameter.
     *
     * @param cause  the cause of the error.
     * @param writer the resource.
     */
    @LogMessage(level = ERROR)
    void failedToCloseResource(@Cause Throwable cause, XMLStreamWriter writer);

    /**
     * Logs an error message indicating a failure to persist configuration change.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14607, value = "Failed to persist configuration change")
    void failedToPersistConfigurationChange(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to store the configuration file.
     *
     * @param cause the cause of the error.
     * @param name  the name of the configuration.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14608, value = "Failed to store configuration to %s")
    void failedToStoreConfiguration(@Cause Throwable cause, String name);

    /**
     * Logs an error message indicating an invalid value for the system property, represented by the {@code name}
     * parameter, was found.
     *
     * @param value        the invalid value.
     * @param name         the name of the system property.
     * @param defaultValue the default value being used.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14609, value = "Invalid value %s for system property %s -- using default value [%d]")
    void invalidSystemPropertyValue(String value, String name, int defaultValue);

    /**
     * Logs a warning message indicating the address, represented by the {@code address} parameter, is a wildcard
     * address and will not match any specific address.
     *
     * @param address        the wildcard address.
     * @param inetAddress    the inet-address tag.
     * @param anyAddress     the any-address tag.
     * @param anyIpv4Address the any-ipv4-address tag.
     * @param anyIpv6Address the any-ipv6-address tag.
     */
    @LogMessage(level = WARN)
    @Message(id = 14610, value = "Address %1$s is a wildcard address, which will not match against any specific address. Do not use " +
            "the '%2$s' configuration element to specify that an interface should use a wildcard address; " +
            "use '%3$s', '%4$s', or '%5$s'")
    void invalidWildcardAddress(ModelNode address, String inetAddress, String anyAddress, String anyIpv4Address, String anyIpv6Address);

    /**
     * Logs an error message indicating no handler for the step operation, represented by the {@code stepOpName}
     * parameter, at {@code address}.
     *
     * @param stepOpName the step operation name.
     * @param address    the address.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14611, value = "No handler for %s at address %s")
    void noHandler(String stepOpName, PathAddress address);

    /**
     * Logs an error message indicating operation failed.
     *
     * @param cause     the cause of the error.
     * @param op        the operation that failed.
     * @param opAddress the address the operation failed on.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14612, value = "Operation (%s) failed - address: (%s)")
    void operationFailed(@Cause Throwable cause, ModelNode op, ModelNode opAddress);

    /**
     * Logs an error message indicating operation failed.
     *
     * @param op                 the operation that failed.
     * @param opAddress          the address the operation failed on.
     * @param failureDescription the failure description.
     */
    @LogMessage(level = ERROR)
    @Message(id = Message.INHERIT, value = "Operation (%s) failed - address: (%s) - failure description: %s")
    void operationFailed(ModelNode op, ModelNode opAddress, ModelNode failureDescription);

    /**
     * Logs an error message indicating operation failed.
     *
     * @param cause        the cause of the error.
     * @param op           the operation that failed.
     * @param opAddress    the address the operation failed on.
     * @param propertyName the boot stack size property name.
     * @param defaultSize  the default boot stack size property size.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14613, value = "Operation (%s) failed - address: (%s) -- due to insufficient stack space for the thread used to " +
            "execute operations. If this error is occurring during server boot, setting " +
            "system property %s to a value higher than [%d] may resolve this problem.")
    void operationFailed(@Cause Throwable cause, ModelNode op, ModelNode opAddress, String propertyName, int defaultSize);

    /**
     * Logs a warning message indicating a wildcard address was detected and will ignore other interface criteria.
     */
    @LogMessage(level = WARN)
    @Message(id = 14614, value = "Wildcard address detected - will ignore other interface criteria.")
    void wildcardAddressDetected();

    /**
     * Logs a warning message indicating an invocation on a {@link ProxyController} did not provide a final response.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14615, value = "Received no final outcome response for operation %s with address %s from remote " +
            "process at address %s. The result of this operation will only include the remote process' preliminary response to" +
            "the request.")
    void noFinalProxyOutcomeReceived(ModelNode op, ModelNode opAddress, ModelNode proxyAddress);

    /**
     * Logs an error message indicating operation failed due to a client error (e.g. an invalid request).
     *
     * @param op                 the operation that failed.
     * @param opAddress          the address the operation failed on.
     * @param failureDescription the failure description.
     */
    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 14616, value = "Operation (%s) failed - address: (%s) - failure description: %s")
    void operationFailedOnClientError(ModelNode op, ModelNode opAddress, ModelNode failureDescription);
}
