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

package org.jboss.as.controller.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.NoSuchResourceException;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.UnauthorizedException;
import org.jboss.as.controller._private.OperationCancellationException;
import org.jboss.as.controller._private.OperationFailedRuntimeException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.interfaces.InterfaceCriteria;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.protocol.mgmt.RequestProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleNotFoundException;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYCTL", length = 4)
public interface ControllerLogger extends BasicLogger {

    /**
     * Default root logger with category of the package name.
     */
    ControllerLogger ROOT_LOGGER = Logger.getMessageLogger(ControllerLogger.class, "org.jboss.as.controller");

    /**
     * Logger for management operation messages.
     */
    ControllerLogger MGMT_OP_LOGGER = Logger.getMessageLogger(ControllerLogger.class, "org.jboss.as.controller.management-operation");

    /**
     * A logger with the category {@code org.jboss.as.server}
     */
    ControllerLogger SERVER_LOGGER = Logger.getMessageLogger(ControllerLogger.class, "org.jboss.as.server");

    /**
     * A logger with the category {@code org.jboss.server.management}
     */
    ControllerLogger SERVER_MANAGEMENT_LOGGER = Logger.getMessageLogger(ControllerLogger.class, "org.jboss.server.management");

    /**
     * A logger for logging deprecated resources usage
     */
    ControllerLogger DEPRECATED_LOGGER = Logger.getMessageLogger(ControllerLogger.class, "org.jboss.as.controller.management-deprecated");

    /**
     * A logger for logging problems in the transformers
     */
    ControllerLogger TRANSFORMER_LOGGER = Logger.getMessageLogger(ControllerLogger.class, "org.jboss.as.controller.transformer");

    /**
     * A logger for access control related messages.
     */
    ControllerLogger ACCESS_LOGGER = Logger.getMessageLogger(ControllerLogger.class, "org.jboss.as.controller.access-control");

    /**
     * Logs a warning message indicating the address, represented by the {@code address} parameter, could not be
     * resolved, so cannot match it to any InetAddress.
     *
     * @param address the address that could not be resolved.
     */
    @LogMessage(level = WARN)
    @Message(id = 1, value = "Cannot resolve address %s, so cannot match it to any InetAddress")
    void cannotResolveAddress(String address);

    /**
     * Logs an error message indicating there was an error booting the container.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Error booting the container")
    void errorBootingContainer(@Cause Throwable cause);

    /**
     * Logs an error message indicating there was an error booting the container.
     *
     * @param cause         the cause of the error.
     * @param bootStackSize the boot stack size.
     * @param name          the property name to increase the boot stack size.
     */
    @LogMessage(level = ERROR)
    @Message(id = 3, value = "Error booting the container due to insufficient stack space for the thread used to " +
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
    @Message(id = 4, value = "%s caught exception attempting to revert operation %s at address %s")
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
    @Message(id = 5, value = "Failed executing operation %s at address %s")
    void failedExecutingOperation(@Cause Throwable cause, ModelNode op, PathAddress path);

    /**
     * Logs an error message indicating a failure executing the subsystem, represented by the {@code name} parameter,
     * boot operations.
     *
     * @param cause the cause of the error.
     * @param name  the name of subsystem.
     */
    @LogMessage(level = ERROR)
    @Message(id = 6, value = "Failed executing subsystem %s boot operations")
    void failedSubsystemBootOperations(@Cause Throwable cause, String name);

    /**
     * Logs an error message indicating to failure to close the resource represented by the {@code closeable} parameter.
     *
     * @param cause     the cause of the error.
     * @param closeable the resource.
     */
    @LogMessage(level = ERROR)
    @Message(id = 7, value = "Failed to close resource %s")
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
    @Message(id = 8, value = "Failed to persist configuration change")
    void failedToPersistConfigurationChange(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to store the configuration file.
     *
     * @param cause the cause of the error.
     * @param name  the name of the configuration.
     */
    @LogMessage(level = ERROR)
    @Message(id = 9, value = "Failed to store configuration to %s")
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
    @Message(id = 10, value = "Invalid value %s for system property %s -- using default value [%d]")
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
    @Message(id = 11, value = "Address %1$s is a wildcard address, which will not match against any specific address. Do not use " +
            "the '%2$s' configuration element to specify that an interface should use a wildcard address; " +
            "use '%3$s', '%4$s', or '%5$s'")
    void invalidWildcardAddress(String address, String inetAddress, String anyAddress, String anyIpv4Address, String anyIpv6Address);

    /**
     * Logs an error message indicating no handler for the step operation, represented by the {@code stepOpName}
     * parameter, at {@code address}.
     *
     * @param stepOpName the step operation name.
     * @param address    the address
     * @deprecated use {@link #noSuchResourceType(PathAddress)} or {@link #noHandlerForOperation(String, PathAddress)}
     */
//    @LogMessage(level = ERROR)
//    @Message(id = 12, value = "No handler for %s at address %s")
//    void noHandler(String stepOpName, PathAddress address);

    /**
     * Logs an error message indicating operation failed.
     *
     * @param cause     the cause of the error.
     * @param op        the operation that failed.
     * @param opAddress the address the operation failed on.
     */
    @LogMessage(level = ERROR)
    @Message(id = 13, value = "Operation (%s) failed - address: (%s)")
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
    @Message(id = 14, value = "Operation (%s) failed - address: (%s) -- due to insufficient stack space for the thread used to " +
            "execute operations. If this error is occurring during server boot, setting " +
            "system property %s to a value higher than [%d] may resolve this problem.")
    void operationFailedInsufficientStackSpace(@Cause Throwable cause, ModelNode op, ModelNode opAddress, String propertyName, int defaultSize);

    /**
     * Logs a warning message indicating a wildcard address was detected and will ignore other interface criteria.
     */
    @LogMessage(level = WARN)
    @Message(id = 15, value = "Wildcard address detected - will ignore other interface criteria.")
    void wildcardAddressDetected();

    /**
     * Logs a warning message indicating an invocation on a {@link org.jboss.as.controller.ProxyController} did not provide a final response.
     */
    @LogMessage(level = ERROR)
    @Message(id = 16, value = "Received no final outcome response for operation %s with address %s from remote " +
            "process at address %s. The result of this operation will only include the remote process' preliminary response to " +
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
    @Message(id = 17, value = "Operation (%s) failed - address: (%s) - failure description: %s")
    void operationFailedOnClientError(ModelNode op, ModelNode opAddress, ModelNode failureDescription);

    /**
     * Logs an error indicating that createWrapper should be called
     *
     * @param name the subsystem name
     */
    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 18, value = "A subsystem '%s' was registered without calling ExtensionContext.createTracker(). The subsystems are registered normally but won't be cleaned up when the extension is removed.")
    void registerSubsystemNoWrapper(String name);

    /**
     * Logs a warning message indicating graceful shutdown of native management request handling
     * communication did not complete within the given timeout period.
     *
     * @param timeout the timeout, in ms.
     */
    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 19, value = "Graceful shutdown of the handler used for native management requests did not complete within [%d] ms but shutdown of the underlying communication channel is proceeding")
    void gracefulManagementChannelHandlerShutdownTimedOut(int timeout);

    /**
     * Logs a warning message indicating graceful shutdown of native management request handling
     * communication failed.
     *
     * @param cause the timeout, in ms.
     */
    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 20, value = "Graceful shutdown of the handler used for native management requests failed but shutdown of the underlying communication channel is proceeding")
    void gracefulManagementChannelHandlerShutdownFailed(@Cause Throwable cause);

    /**
     * Logs a warning message indicating graceful shutdown of management request handling of slave HC to master HC
     * communication failed.
     *
     * @param cause        the the cause of the failure
     * @param propertyName the name of the system property
     * @param propValue    the value provided
     */
    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 21, value = "Invalid value '%s' for system property '%s' -- value must be convertible into an int")
    void invalidChannelCloseTimeout(@Cause NumberFormatException cause, String propertyName, String propValue);

    /**
     * Logs a warning message indicating multiple addresses or nics matched the selection criteria provided for
     * an interface
     *
     * @param interfaceName    the name of the interface configuration
     * @param addresses        the matching addresses
     * @param nis              the matching nics
     * @param inetAddress      the selected address
     * @param networkInterface the selected nic
     */
    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 22, value = "Multiple addresses or network interfaces matched the selection criteria for interface '%s'. Matching addresses: %s.  Matching network interfaces: %s. The interface will use address %s and network interface %s.")
    void multipleMatchingAddresses(String interfaceName, Set<InetAddress> addresses, Set<String> nis, InetAddress inetAddress, String networkInterface);

    /**
     * Logs a warning message indicating multiple addresses or nics matched the selection criteria provided for
     * an interface
     *
     * @param toMatch   the name of the interface configuration
     * @param addresses the matching addresses
     * @param nis       the matching nics
     */
    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 23, value = "Value '%s' for interface selection criteria 'inet-address' is ambiguous, as more than one address or network interface available on the machine matches it. Because of this ambiguity, no address will be selected as a match. Matching addresses: %s.  Matching network interfaces: %s.")
    void multipleMatchingAddresses(String toMatch, Set<InetAddress> addresses, Set<String> nis);

    /**
     * Logs an error message indicating the target definition could not be read.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 24, value = "Could not read target definition!")
    void cannotReadTargetDefinition(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to transform.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 25, value = "Could not transform")
    void cannotTransform(@Cause Throwable cause);

    /**
     * Logs a warning message indicating the there is not transformer for the subsystem.
     *
     * @param subsystemName the subsystem name
     * @param major         the major version
     * @param minor         the minor version
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 26, value = "We have no transformer for subsystem: %s-%d.%d model transfer can break!")
    void transformerNotFound(String subsystemName, int major, int minor);

    /**
     * Logs a warning message indicating that an operation was interrupted before service stability was reached
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 27, value = "Operation was interrupted before stability could be reached")
    void interruptedWaitingStability();

    @LogMessage(level = Level.INFO)
    @Message(id = 28, value = "Attribute %s is deprecated, and it might be removed in future version!")
    void attributeDeprecated(String name);

    /**
     * Logs a warning message indicating a temp file could not be deleted.
     *
     * @param name temp filename
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 29, value = "Cannot delete temp file %s, will be deleted on exit")
    void cannotDeleteTempFile(String name);

    @Message(id = 30, value = "No resource definition is registered for address %s")
    String noSuchResourceType(PathAddress address);

    @Message(id = 31, value = "No operation named '%s' exists at address %s")
    String noHandlerForOperation(String operationName, PathAddress address);

    @Message(id = 32, value = "There were problems during the transformation process for target host: '%s' %nProblems found: %n%s")
    @LogMessage(level = WARN)
    void transformationWarnings(String hostName, Set<String> problems);

    @Message(id = 33, value = "Extension '%s' is deprecated and may not be supported in future versions")
    @LogMessage(level = WARN)
    void extensionDeprecated(String extensionName);

    @Message(id = 34, value = "Subsystems %s provided by legacy extension '%s' are not supported on servers running this version. " +
            "The extension is only supported for use by hosts running a previous release in a mixed-version managed domain. " +
            "On this server the extension will not register any subsystems, and future attempts to create or address " +
            "subsystem resources on this server will result in failure.")
    @LogMessage(level = INFO)
    void ignoringUnsupportedLegacyExtension(List<String> subsystemNames, String extensionName);

    /**
     * Logs an error message indicating that updating the audit log failed
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 35, value = "Update of the management operation audit log failed")
    void failedToUpdateAuditLog(@Cause Exception e);

    /**
     * Logs an error message indicating that audit logging is being disabled due to logging failures.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 36, value = "[%d] consecutive management operation audit logging failures have occurred; disabling audit logging")
    void disablingLoggingDueToFailures(short failureCount);

    /**
     * Logs an error message indicating that a handler failed writing a log message
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 37, value = "Update of the management operation audit log failed in handler '%s'")
    void logHandlerWriteFailed(@Cause Throwable t, String name);

    /**
     * Logs an error message indicating that audit logging is being disabled due to logging failures.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 38, value = "[%d] consecutive management operation audit logging failures have occurred in handler '%s'; disabling this handler for audit logging")
    void disablingLogHandlerDueToFailures(int failureCount, String name);

    /**
     * Creates an exception indicating the {@code name} is already defined.
     *
     * @param name     the name that is already defined.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 39, value = "%s already defined")
    XMLStreamException alreadyDefined(String name, @Param Location location);

    /**
     * Creates an exception indicating the {@code value} has already been declared.
     *
     * @param name     the attribute name.
     * @param value    the value that has already been declared.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 40, value = "%s %s already declared")
    XMLStreamException alreadyDeclared(String name, String value, @Param Location location);

    /**
     * Creates an exception indicating the {@code value} has already been declared.
     *
     * @param name        the attribute name.
     * @param value       the value that has already been declared.
     * @param parentName  the name of the parent.
     * @param parentValue the parent value.
     * @param location    the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 41, value = "A %s %s already declared has already been declared in %s %s")
    XMLStreamException alreadyDeclared(String name, String value, String parentName, String parentValue, @Param Location location);

    /**
     * Creates an exception indicating the {@code value} has already been declared.
     *
     * @param name1       the first attribute name.
     * @param name2       the second attribute name.
     * @param value       the value that has already been declared.
     * @param parentName  the name of the parent.
     * @param parentValue the parent value.
     * @param location    the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 42, value = "A %s or a %s %s already declared has already been declared in %s %s")
    XMLStreamException alreadyDeclared(String name1, String name2, String value, String parentName, String parentValue, @Param Location location);

    /**
     * Creates an exception indicating the {@code type} with the {@code name} is already registered at the
     * {@code location}.
     *
     * @param type     the type.
     * @param name     the name.
     * @param location the location.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 43, value = "An %s named '%s' is already registered at location '%s'")
    IllegalArgumentException alreadyRegistered(String type, String name, String location);

    /**
     * Creates an exception indicating an ambiguous file name was found as there are multiple files ending in the
     * {@code suffix} were found in the directory.
     *
     * @param backupType the backup type.
     * @param searchDir  the search directory.
     * @param suffix     the file suffix.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 44, value = "Ambiguous configuration file name '%s' as there are multiple files in %s that end in %s")
    IllegalStateException ambiguousConfigurationFiles(String backupType, File searchDir, String suffix);

    /**
     * Creates an exception indicating an ambiguous name, represented by the {@code prefix} parameter, was found in
     * the directory, represented by the {@code dir} parameter.
     *
     * @param prefix the file prefix.
     * @param dir    the search directory.
     * @param files  the ambiguous files.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 45, value = "Ambiguous name '%s' in %s: %s")
    IllegalArgumentException ambiguousName(String prefix, String dir, Collection<String> files);

    /**
     * Creates an exception indicating a thread was interrupted waiting for a response for asynch operation.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 46, value = "Thread was interrupted waiting for a response for asynch operation")
    RequestProcessingException asynchOperationThreadInterrupted();

    /**
     * Creates an exception indicating no asynch request with the batch id, represented by the {@code batchId}
     * parameter.
     *
     * @param batchId the batch id.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 47, value = "No asynch request with batch id %d")
    RequestProcessingException asynchRequestNotFound(int batchId);

    /**
     * A message indicating the attribute, represented by the {@code attributeName} parameter, is not writable.
     *
     * @param attributeName the attribute name.
     *
     * @return the message.
     */
    @Message(id = 48, value = "Attribute %s is not writable")
    String attributeNotWritable(String attributeName);

    /**
     * A message indicating the attribute, represented by the {@code attributeName} parameter, is a registered child of
     * the resource.
     *
     * @param attributeName the name of the attribute.
     * @param resource      the resource the attribute is a child of.
     *
     * @return the message.
     */
    @Message(id = 49, value = "'%s' is a registered child of resource (%s)")
    String attributeRegisteredOnResource(String attributeName, ModelNode resource);

    /**
     * Creates an exception indicating the inability to determine a default name based on the local host name.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 50, value = "Unable to determine a default name based on the local host name")
    RuntimeException cannotDetermineDefaultName(@Cause Throwable cause);

    /**
     * Creates an exception indicating the file could not be created.
     *
     * @param path the path to the file.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 51, value = "Could not create %s")
    IllegalStateException cannotCreate(String path);

    /**
     * Creates an exception indicating the file could not be deleted.
     *
     * @param file the file to delete.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 52, value = "Could not delete %s")
    IllegalStateException cannotDelete(File file);

    /**
     * Creates an exception indicating a submodel cannot be registered with a {@code null} path.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 53, value = "Cannot register submodels with a null PathElement")
    IllegalArgumentException cannotRegisterSubmodelWithNullPath();

    /**
     * Creates an exception indicating a non-runtime-only submodel cannot be registered with a runtime-only parent.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 54, value = "Cannot register non-runtime-only submodels with a runtime-only parent")
    IllegalArgumentException cannotRegisterSubmodel();

    /**
     * Creates an exception indicating the inability to remove the {@code name}.
     *
     * @param name the name.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 55, value = "Cannot remove %s")
    OperationFailedRuntimeException cannotRemove(String name);

    /**
     * Creates an exception indicating the file could not be renamed.
     *
     * @param fromPath the from file.
     * @param toPath   the to file.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 56, value = "Could not rename %s to %s")
    IllegalStateException cannotRename(String fromPath, String toPath);

    /**
     * Creates an exception indicating the inability to write the {@code name}.
     *
     * @param name the name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 57, value = "Cannot write to %s")
    IllegalArgumentException cannotWriteTo(String name);

    /**
     * Creates an exception indicating a child, represented by the {@code childName} parameter, of the parent element,
     * represented by the {@code parentName} parameter, has already been declared.
     *
     * @param childName  the child element name.
     * @param parentName the parent element name.
     * @param location   the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 58, value = "Child %s of element %s already declared")
    XMLStreamException childAlreadyDeclared(String childName, String parentName, @Param Location location);

    /**
     * Creates an exception indicating the canonical file for the boot file could not be found.
     *
     * @param cause the cause of the error.
     * @param file  the boot file.
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 59, value = "Could not get canonical file for boot file: %s")
    RuntimeException canonicalBootFileNotFound(@Cause Throwable cause, File file);

    /**
     * Creates an exception indicating the canonical file for the main file could not be found.
     *
     * @param cause the cause of the error.
     * @param file  the main file.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 60, value = "Could not get canonical file for main file: %s")
    IllegalStateException canonicalMainFileNotFound(@Cause Throwable cause, File file);

    /**
     * A message indicating the channel is closed.
     *
     * @return the message.
     */
    @Message(id = 61, value = "Channel closed")
    String channelClosed();

    /**
     * A message indicating the composite operation failed and was rolled back.
     *
     * @return the message.
     */
    @Message(id = 62, value = "Composite operation failed and was rolled back. Steps that failed:")
    String compositeOperationFailed();

    /**
     * A message indicating the composite operation was rolled back.
     *
     * @return the message.
     */
    @Message(id = 63, value = "Composite operation was rolled back")
    String compositeOperationRolledBack();

    /**
     * Creates an exception indicating a configuration file whose complete name is the same as the {@code backupType} is
     * not allowed.
     *
     * @param backupType the backup type.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 64, value = "Configuration files whose complete name is %s are not allowed")
    IllegalArgumentException configurationFileNameNotAllowed(String backupType);

    /**
     * Creates an exception indicating no configuration file ending in the {@code suffix} was found in the directory,
     * represented by the {@code dir} parameter.
     *
     * @param suffix the suffix.
     * @param dir    the search directory.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 65, value = "No configuration file ending in %s found in %s")
    IllegalStateException configurationFileNotFound(String suffix, File dir);

    /**
     * Creates an exception indicating the directory. represented by the {@code pathName} parameter, was not found.
     *
     * @param pathName the path name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 66, value = "No directory %s was found")
    IllegalArgumentException directoryNotFound(String pathName);

    /**
     * Creates an exception indicating either the {@code remoteName} or the {@code localName} domain controller
     * configuration must be declared.
     *
     * @param remoteName the remote element name.
     * @param localName  the local element name.
     * @param location   the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 67, value = "Either a %s or %s domain controller configuration must be declared.")
    XMLStreamException domainControllerMustBeDeclared(String remoteName, String localName, @Param Location location);

    /**
     * Creates an exception indicating an attribute, represented by the {@code name} parameter, has already been
     * declared.
     *
     * @param name     the attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 68, value = "An attribute named '%s' has already been declared")
    XMLStreamException duplicateAttribute(String name, @Param Location location);

    /**
     * Creates an exception indicating a duplicate declaration.
     *
     * @param name     the name of the duplicate entry.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 69, value = "Duplicate %s declaration")
    XMLStreamException duplicateDeclaration(String name, @Param Location location);

    /**
     * Creates an exception indicating a duplicate declaration.
     *
     * @param name     the name of the duplicate entry.
     * @param value    the duplicate entry.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 70, value = "Duplicate %s declaration %s")
    XMLStreamException duplicateDeclaration(String name, String value, @Param Location location);

    /**
     * Creates an exception indicating ad duplicate path element, represented by the {@code name} parameter, was found.
     *
     * @param name the name of the duplicate entry.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 71, value = "Duplicate path element '%s' found")
    OperationFailedRuntimeException duplicateElement(String name);

    /**
     * Creates an exception indicating a duplicate interface declaration.
     *
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 72, value = "Duplicate interface declaration")
    XMLStreamException duplicateInterfaceDeclaration(@Param Location location);

    /**
     * Creates an exception indicating an element, represented by the {@code name} parameter, has already been
     * declared.
     *
     * @param name     the element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 73, value = "An element of this type named '%s' has already been declared")
    XMLStreamException duplicateNamedElement(String name, @Param Location location);

    /**
     * Creates an exception indicating a duplicate profile was included.
     *
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 74, value = "Duplicate profile included")
    XMLStreamException duplicateProfile(@Param Location location);

    /**
     * Creates an exception indicating the resource is a duplicate.
     *
     * @param name the name of the resource.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 75, value = "Duplicate resource %s")
    IllegalStateException duplicateResource(String name);

    /**
     * Creates an exception indicating the resource type is a duplicate.
     *
     * @param type the duplicate type.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 76, value = "Duplicate resource type %s")
    IllegalStateException duplicateResourceType(String type);

    /**
     * A message indicating the element, represented by the {@code name} parameter, is not supported the file,
     * represented by the {@code file} parameter.
     *
     * @param name     the name of the element.
     * @param fileName the file name.
     *
     * @return the message.
     */
    @Message(id = 77, value = "Element %s is not supported in a %s file")
    String elementNotSupported(String name, String fileName);

    /**
     * A message indicating an error waiting for Tx commit/rollback.
     *
     * @return the message.
     */
    @Message(id = 78, value = "Error waiting for Tx commit/rollback")
    String errorWaitingForTransaction();

    /**
     * Creates an exception indicating a failure to initialize the module.
     *
     * @param cause the cause of the error.
     * @param name  the name of the module.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 79, value = "Failed initializing module %s")
    RuntimeException failedInitializingModule(@Cause Throwable cause, String name);

    /**
     * A message indicating the failed services.
     *
     * @return the message.
     */
    @Message(id = 80, value = "Failed services")
    String failedServices();

    /**
     * Creates an exception indicating a failure to backup the file, represented by the {@code file} parameter.
     *
     * @param cause the cause of the error.
     * @param file  the file that failed to backup.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 81, value = "Failed to back up %s")
    ConfigurationPersistenceException failedToBackup(@Cause Throwable cause, File file);

    /**
     * Creates an exception indicating a failure to create backup copies of configuration the file, represented by the
     * {@code file} parameter.
     *
     * @param cause the cause of the error.
     * @param file  the configuration file that failed to backup.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 82, value = "Failed to create backup copies of configuration file %s")
    ConfigurationPersistenceException failedToCreateConfigurationBackup(@Cause Throwable cause, File file);

    /**
     * Creates an exception indicating a failure to load a module.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 83, value = "Failed to load module")
    XMLStreamException failedToLoadModule(@Cause Throwable cause);

    /**
     * Creates an exception indicating a failure to load a module.
     *
     * @param cause the cause of the error.
     * @param name  the module name.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = Message.INHERIT, value = "Failed to load module %s")
    XMLStreamException failedToLoadModule(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating a failure to marshal the configuration.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 84, value = "Failed to marshal configuration")
    ConfigurationPersistenceException failedToMarshalConfiguration(@Cause Throwable cause);

    /**
     * Creates an exception indicating a failure to parse the configuration.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 85, value = "Failed to parse configuration")
    ConfigurationPersistenceException failedToParseConfiguration(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to persist configuration change.
     *
     * @param cause the cause of the error.
     *
     * @return the message.
     */
    @Message(id = 86, value = "Failed to persist configuration change: %s")
    String failedToPersistConfigurationChange(String cause);


    /**
     * Creates an exception indicating a failure to store the configuration.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 87, value = "Failed to store configuration")
    ConfigurationPersistenceException failedToStoreConfiguration(@Cause Throwable cause);

    /**
     * Creates an exception indicating a failure to take a snapshot of the file, represented by the {@code file}
     * parameter.
     *
     * @param cause    the cause of the error.
     * @param file     the file that failed to take the snapshot of.
     * @param snapshot the snapshot file.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 88, value = "Failed to take a snapshot of %s to %s")
    ConfigurationPersistenceException failedToTakeSnapshot(@Cause Throwable cause, File file, File snapshot);

    /**
     * Creates an exception indicating a failure to write the configuration.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 89, value = "Failed to write configuration")
    ConfigurationPersistenceException failedToWriteConfiguration(@Cause Throwable cause);

    /**
     * Creates an exception indicating {@code path1} does not exist.
     *
     * @param path1 the first non-existing path.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 90, value = "%s does not exist")
    IllegalArgumentException fileNotFound(String path1);

    /**
     * Creates an exception indicating no files beginning with the {@code prefix} were found in the directory,
     * represented by the {@code dir} parameter.
     *
     * @param prefix the file prefix.
     * @param dir    the search directory.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 91, value = "No files beginning with '%s' found in %s")
    IllegalArgumentException fileNotFoundWithPrefix(String prefix, String dir);

    /**
     * Creates an exception indicating the {@code clazz} cannot be used except in a full server boot.
     *
     * @param clazz the class that cannot be used.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 92, value = "%s cannot be used except in a full server boot")
    IllegalStateException fullServerBootRequired(Class<?> clazz);

    /**
     * A message indicating that no included group with the name, represented by the {@code name} parameter, was found.
     *
     * @param name the name of the group.
     *
     * @return the message.
     */
    @Message(id = 93, value = "No included group with name %s found")
    String groupNotFound(String name);

    /**
     * A message indicating the interface criteria must be of the type represented by the {@code valueType} parameter.
     *
     * @param invalidType the invalid type.
     * @param validType   the valid type.
     *
     * @return the message.
     */
    @Message(id = 94, value = "Illegal interface criteria type %s; must be %s")
    String illegalInterfaceCriteria(ModelType invalidType, ModelType validType);

    /**
     * A message indicating the value, represented by the {@code valueType} parameter, is invalid for the interface
     * criteria, represented by the {@code id} parameter.
     *
     * @param valueType the type of the invalid value.
     * @param id        the id of the criteria interface.
     * @param validType the valid type.
     *
     * @return the message.
     */
    @Message(id = 95, value = "Illegal value %s for interface criteria %s; must be %s")
    String illegalValueForInterfaceCriteria(ModelType valueType, String id, ModelType validType);

    /**
     * Creates an exception indicating the resource is immutable.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 96, value = "Resource is immutable")
    UnsupportedOperationException immutableResource();

    /**
     * An exception indicating the type is invalid.
     *
     * @param name        the name the invalid type was found for.
     * @param validTypes  a collection of valid types.
     * @param invalidType the invalid type.
     *
     * @return the exception.
     */
    @Message(id = 97, value = "Wrong type for %s. Expected %s but was %s")
    OperationFailedException incorrectType(String name, Collection<ModelType> validTypes, ModelType invalidType);

    /**
     * A message indicating interrupted while waiting for request.
     *
     * @return the message.
     */
    @Message(id = 98, value = "Interrupted while waiting for request")
    String interruptedWaitingForRequest();

    /**
     * A message indicating the {@code name} is invalid.
     *
     * @param name the name of the invalid attribute.
     *
     * @return the message.
     */
    @Message(id = 99, value = "%s is invalid")
    String invalid(String name);

    /**
     * A message indicating the {@code value} is invalid.
     *
     * @param cause    the cause of the error.
     * @param value    the invalid value.
     * @param name     the name of the invalid attribute.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 100, value = "%d is not a valid %s")
    XMLStreamException invalid(@Cause Throwable cause, int value, String name, @Param Location location);

    /**
     * A message indicating the address, represented by the {@code address} parameter, is invalid.
     *
     * @param address the invalid address.
     * @param msg     the error message.
     *
     * @return the message.
     */
    @Message(id = 101, value = "Invalid address %s (%s)")
    String invalidAddress(String address, String msg);

    /**
     * A message indicating the value, represented by the {@code value} parameter, is invalid and must be of the form
     * address/mask.
     *
     * @param value the invalid value.
     *
     * @return the message.
     */
    @Message(id = 102, value = "Invalid 'value' %s -- must be of the form address/mask")
    String invalidAddressMaskValue(String value);

    /**
     * A message indicating the mask, represented by the {@code mask} parameter, is invalid.
     *
     * @param mask the invalid mask.
     * @param msg  the error message.
     *
     * @return the message.
     */
    @Message(id = 103, value = "Invalid mask %s (%s)")
    String  invalidAddressMask(String mask, String msg);

    /**
     * A message indicating the address value, represented by the {@code value} parameter, is invalid.
     *
     * @param value the invalid address value.
     * @param msg   the error message.
     *
     * @return the message.
     */
    @Message(id = 104, value = "Invalid address %s (%s)")
    String invalidAddressValue(String value, String msg);

    /**
     * A message indicating the attribute, represented by the {@code attributeName} parameter, is invalid in
     * combination with the {@code combos} parameter.
     *
     * @param attributeName the attribute name.
     * @param combos        the combinations.
     *
     * @return the message.
     */
    @Message(id = 105, value = "%s is invalid in combination with %s")
    String invalidAttributeCombo(String attributeName, StringBuilder combos);

    /**
     * Creates an exception indicating an invalid value, represented by the {@code value} parameter, was found for the
     * attribute, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 106, value = "Invalid value '%s' for attribute '%s'")
    XMLStreamException invalidAttributeValue(String value, QName name, @Param Location location);

    /**
     * Creates an exception indicating an invalid value, represented by the {@code value} parameter, was found for the
     * attribute, represented by the {@code name} parameter. The value must be between the {@code minInclusive} and
     * {@code maxInclusive} values.
     *
     * @param value        the invalid value.
     * @param name         the attribute name.
     * @param minInclusive the minimum value allowed.
     * @param maxInclusive the maximum value allowed.
     * @param location     the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 107, value = "Illegal value %d for attribute '%s' must be between %d and %d (inclusive)")
    XMLStreamException invalidAttributeValue(int value, QName name, int minInclusive, int maxInclusive, @Param Location location);

    /**
     * Creates an exception indicating an invalid integer value, represented by the {@code value} parameter, was found
     * for the attribute, represented by the {@code name} parameter.
     *
     * @param cause    the cause of the error.
     * @param value    the invalid value.
     * @param name     the attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 108, value = "Illegal value '%s' for attribute '%s' must be an integer")
    XMLStreamException invalidAttributeValueInt(@Cause Throwable cause, String value, QName name, @Param Location location);

    /**
     * A message indicating the pattern, represented by the {@code pattern} parameter, for the interface criteria,
     * represented by the {@code name} parameter, is invalid.
     *
     * @param pattern the pattern.
     * @param name    the interface criteria.
     *
     * @return the message.
     */
    @Message(id = 109, value = "Invalid pattern %s for interface criteria %s")
    String invalidInterfaceCriteriaPattern(String pattern, String name);

    /**
     * Creates an exception indicating the {@code key} is invalid.
     *
     * @param element the path element
     * @param key the invalid value.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 110, value = "Invalid resource address element '%s'. The key '%s' is not valid for an element in a resource address.")
    String invalidPathElementKey(String element, String key);

    /**
     * Creates an exception indicating the load factor must be greater than 0 and less than or equal to 1.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 111, value = "Load factor must be greater than 0 and less than or equal to 1")
    IllegalArgumentException invalidLoadFactor();

    /**
     * A message indicating the {@code value} parameter is invalid and must have a maximum length, represented by the
     * {@code length} parameter.
     *
     * @param value  the invalid value.
     * @param name   the name of the parameter.
     * @param length the maximum length.
     *
     * @return the message.
     */
    @Message(id = 112, value = "'%s' is an invalid value for parameter %s. Values must have a maximum length of %d characters")
    String invalidMaxLength(String value, String name, int length);

    /**
     * A message indicating the {@code value} parameter is invalid and must have a minimum length, represented by the
     * {@code length} parameter.
     *
     * @param value  the invalid value.
     * @param name   the name of the parameter.
     * @param length the minimum length.
     *
     * @return the message.
     */
    @Message(id = 113, value = "'%s' is an invalid value for parameter %s. Values must have a minimum length of %d characters")
    String invalidMinLength(String value, String name, int length);

    /**
     * A message indicating the {@code size} is an invalid size for the parameter, represented by the {@code name}
     * parameter.
     *
     * @param size    the invalid size.
     * @param name    the name of the parameter.
     * @param maxSize the maximum size allowed.
     *
     * @return the message
     */
    @Message(id = 114, value = "[%d] is an invalid size for parameter %s. A maximum length of [%d] is required")
    String invalidMaxSize(int size, String name, int maxSize);

    /**
     * A message indicating the {@code size} is an invalid size for the parameter, represented by the {@code name}
     * parameter.
     *
     * @param size    the invalid size.
     * @param name    the name of the parameter.
     * @param minSize the minimum size allowed.
     *
     * @return the message
     */
    @Message(id = 115, value = "[%d] is an invalid size for parameter %s. A minimum length of [%d] is required")
    String invalidMinSize(int size, String name, int minSize);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param maxValue the minimum value required.
     *
     * @return the message.
     */
    @Message(id = 116, value = "%d is an invalid value for parameter %s. A maximum value of %d is required")
    String invalidMaxValue(int value, String name, int maxValue);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param maxValue the minimum value required.
     *
     * @return the message.
     */
    String invalidMaxValue(long value, String name, long maxValue);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param minValue the minimum value required.
     *
     * @return the message.
     */
    @Message(id = 117, value = "%d is an invalid value for parameter %s. A minimum value of %d is required")
    String invalidMinValue(int value, String name, int minValue);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param minValue the minimum value required.
     *
     * @return the message.
     */
    String invalidMinValue(long value, String name, long minValue);

    /**
     * Creates an exception indicated an invalid modification after completed ste.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 118, value = "Invalid modification after completed step")
    IllegalStateException invalidModificationAfterCompletedStep();

    /**
     * Creates an exception indicating the {@code value} for the attribute, represented by the {@code name} parameter,
     * is not a valid multicast address.
     *
     * @param value    the invalid value.
     * @param name     the name of the attribute.\
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 119, value = "Value %s for attribute %s is not a valid multicast address")
    OperationFailedException invalidMulticastAddress(String value, String name);

    /**
     * Creates an exception indicating an outbound socket binding cannot have both the {@code localTag} and the
     * {@code remoteTag}.
     *
     * @param name      the name of the socket binding.
     * @param localTag  the local tag.
     * @param remoteTag the remote tag.
     * @param location  the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 120, value = "An outbound socket binding: %s cannot have both %s as well as a %s at the same time")
    XMLStreamException invalidOutboundSocketBinding(String name, String localTag, String remoteTag, @Param Location location);

    /**
     * Creates an exception indicating the {@code flag} is invalid.
     *
     * @param flag       the invalid flag.
     * @param name       the name of the parameter.
     * @param validFlags a collection of valid flags.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 121, value = "%s is not a valid value for parameter %s -- must be one of %s")
    IllegalArgumentException invalidParameterValue(OperationEntry.Flag flag, String name, Collection<OperationEntry.Flag> validFlags);

    /**
     * Creates an exception indicating the {@code value} for the attribute, represented by the {@code name} parameter,
     * does not represent a properly hex-encoded SHA1 hash.
     *
     * @param cause    the cause of the error.
     * @param value    the invalid value.
     * @param name     the name of the attribute.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 122, value = "Value %s for attribute %s does not represent a properly hex-encoded SHA1 hash")
    XMLStreamException invalidSha1Value(@Cause Throwable cause, String value, String name, @Param Location location);

    /**
     * Creates an exception indicating the stage is not valid for the context process type.
     *
     * @param stage the stage.
     * @param processType the context process type.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 123, value = "Stage %s is not valid for context process type %s")
    IllegalStateException invalidStage(OperationContext.Stage stage, ProcessType processType);

    /**
     * Creates an exception indicating an invalid step stage specified.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 124, value = "Invalid step stage specified")
    IllegalArgumentException invalidStepStage();

    /**
     * Creates an exception indicating an invalid step stage for this context type.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 125, value = "Invalid step stage for this context type")
    IllegalArgumentException invalidStepStageForContext();

    /**
     * Creates an exception indicating the table cannot have a negative size.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 126, value = "Can not have a negative size table!")
    IllegalArgumentException invalidTableSize();

    /**
     * A message indicating the type, represented by the {@code type} parameter, is invalid.
     *
     * @param type the invalid type.
     *
     * @return the message.
     */
    @Message(id = 127, value = "Invalid type %s")
    String invalidType(ModelType type);

    /**
     * Creates an exception indicating the {@code value} is invalid.
     *
     * @param element the path element
     * @param value the invalid value.
     * @param character the invalid character
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 128, value = "Invalid resource address element '%s'. The value '%s' is not valid for an element in a resource address. Character '%s' is not allowed.")
    String invalidPathElementValue(String element, String value, Character character);

    /**
     * A message indicating the {@code value} for the parameter, represented by the {@code name} parameter, is invalid.
     *
     * @param value       the invalid value.
     * @param name        the name of the parameter.
     * @param validValues a collection of valid values.
     *
     * @return the message.
     */
    @Message(id = 129, value = "Invalid value %s for %s; legal values are %s")
    String invalidValue(String value, String name, Collection<?> validValues);

    /**
     * Creates an exception indicating the {@code value} for the {@code name} must be greater than the minimum value,
     * represented by the {@code minValue} parameter.
     *
     * @param name     the name for the value that cannot be negative.
     * @param value    the invalid value.
     * @param minValue the minimum value.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 130, value = "Illegal '%s' value %s -- must be greater than %s")
    XMLStreamException invalidValueGreaterThan(String name, int value, int minValue, @Param Location location);

    /**
     * Creates an exception indicating the {@code value} for the {@code name} cannot be negative.
     *
     * @param name     the name for the value that cannot be negative.
     * @param value    the invalid value.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 131, value = "Illegal '%s' value %s -- cannot be negative")
    XMLStreamException invalidValueNegative(String name, int value, @Param Location location);

    /**
     * Creates an exception indicating there must be one of the elements, represented by the {@code sb} parameter,
     * included.
     *
     * @param sb       the acceptable elements.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 132, value = "Must include one of the following elements: %s")
    XMLStreamException missingOneOf(StringBuilder sb, @Param Location location);

    /**
     * Creates an exception indicating there are missing required attribute(s).
     *
     * @param sb       the missing attributes.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 133, value = "Missing required attribute(s): %s")
    XMLStreamException missingRequiredAttributes(StringBuilder sb, @Param Location location);

    /**
     * Creates an exception indicating there are missing required element(s).
     *
     * @param sb       the missing element.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 134, value = "Missing required element(s): %s")
    XMLStreamException missingRequiredElements(StringBuilder sb, @Param Location location);

    /**
     * Creates an exception indicating an interruption awaiting to load the module.
     *
     * @param name the name of the module.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 135, value = "Interrupted awaiting loading of module %s")
    XMLStreamException moduleLoadingInterrupted(String name);

    /**
     * Creates an exception indicating an interruption awaiting to initialize the module.
     *
     * @param name the name of the module.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 136, value = "Interrupted awaiting initialization of module %s")
    RuntimeException moduleInitializationInterrupted(String name);

    /**
     * Creates an exception indicating a model contains multiple nodes.
     *
     * @param name the name of the node.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 137, value = "Model contains multiple %s nodes")
    IllegalStateException multipleModelNodes(String name);

    /**
     * A message indicating a namespace with the prefix, represented by the {@code prefix} parameter, is already
     * registered with the schema URI, represented by the {@code uri} parameter.
     *
     * @param prefix the namespace prefix.
     * @param uri    the schema URI.
     *
     * @return the message.
     */
    @Message(id = 138, value = "Namespace with prefix %s already registered with schema URI %s")
    String namespaceAlreadyRegistered(String prefix, String uri);

    /**
     * A message indicating no namespace with the URI {@code prefix}, was found.
     *
     * @param prefix the prefix.
     *
     * @return the message.
     */
    @Message(id = 139, value = "No namespace with URI %s found")
    String namespaceNotFound(String prefix);

    /**
     * A message indicating the element, represented by the {@code element} parameter, does not allow nesting.
     *
     * @param element the element.
     *
     * @return the message.
     */
    @Message(id = 140, value = "Nested %s not allowed")
    String nestedElementNotAllowed(Element element);

    /**
     * Creates an exception indicating no active request was found for handling the report represented by the {@code id}
     * parameter.
     *
     * @param id the batch id.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 141, value = "No active request found for handling report %d")
    RequestProcessingException noActiveRequestForHandlingReport(int id);

    /**
     * Creates an exception indicating no active request was found for proxy control represented by the {@code id}
     * parameter.
     *
     * @param id the batch id.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 142, value = "No active request found for proxy operation control %d")
    RequestProcessingException noActiveRequestForProxyOperation(int id);

    /**
     * Creates an exception indicating no active request was found for reading the inputstream report represented by
     * the {@code id} parameter.
     *
     * @param id the batch id.
     *
     * @return a {@link IOException} for the error.
     */
    @Message(id = 143, value = "No active request found for reading inputstream report %d")
    IOException noActiveRequestForReadingInputStreamReport(int id);

    /**
     * Creates an exception indicating there is no active step.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 144, value = "No active step")
    IllegalStateException noActiveStep();

    /**
     * Creates an exception indicating no active transaction found for the {@code id}.
     *
     * @param id the id.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 145, value = "No active tx found for id %d")
    RuntimeException noActiveTransaction(int id);

    /**
     * A message indicating there is no child registry for the child, represented by the {@code childType} and
     * {@code child} parameters.
     *
     * @param childType the child type.
     * @param child     the child.
     *
     * @return the message.
     */
    @Message(id = 146, value = "No child registry for (%s, %s)")
    String noChildRegistry(String childType, String child);

    /**
     * Creates an exception indicating no child type for the {@code name}.
     *
     * @param name the name.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 147, value = "No child type %s")
    OperationFailedRuntimeException noChildType(String name);

    /**
     * A message indicating no handler for the step operation, represented by the {@code stepOpName} parameter, at
     * {@code address}.
     *
     * @param stepOpName the step operation name.
     * @param address    the address.
     *
     * @return the message
     *
     * @deprecated use {@link #noSuchResourceType(PathAddress)} or {@link #noHandlerForOperation(String, PathAddress)}
     */
//    @Message(id = 148, value = "No handler for %s at address %s")
//    String noHandler(String stepOpName, PathAddress address);

    /**
     * A message indicating that no interface criteria was provided.
     *
     * @return the message.
     */
    @Message(id = 149, value = "No interface criteria was provided")
    String noInterfaceCriteria();

    /**
     * A message indicating there is no operation handler.
     *
     * @return the message.
     */
    @Message(id = 150, value = "No operation handler")
    String noOperationHandler();

    /**
     * Creates an exception indicating a node is already registered at the location.
     *
     * @param location the location the node is registered at.
     * @param value    the node value.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 151, value = "A node is already registered at '%s%s)'")
    IllegalArgumentException nodeAlreadyRegistered(String location, String value);

    /**
     * Creates an exception indicating the {@code path} is not a directory.
     *
     * @param path the path.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 152, value = "%s is not a directory")
    IllegalStateException notADirectory(String path);

    /**
     * Creates an exception indicating no {@code path/className} was found for the module identifier.
     *
     * @param path      the path of the SPI.
     * @param className the class name.
     * @param id        the module identifier.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 153, value = "No %s%s found for %s")
    IllegalStateException notFound(String path, String className, ModuleIdentifier id);

    /**
     * Creates an exception indicating an asynchronous operation cannot execute without an executor.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 154, value = "Cannot execute asynchronous operation without an executor")
    IllegalStateException nullAsynchronousExecutor();

    /**
     * An exception indicating the {@code name} may not be {@code null}.
     *
     * @param name the name that cannot be {@code null}.
     *
     * @return the exception.
     */
    @Message(id = 155, value = "%s may not be null")
    OperationFailedException nullNotAllowed(String name);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, was {@code null}.
     *
     * @param name the name of the variable that was {@code null}.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 156, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates a message indicating the operation step.
     *
     * @param step the step.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Operation %s")
    String operation(String step);

    /**
     * Creates an exception indicating the operation is already complete.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 157, value = "Operation already complete")
    IllegalStateException operationAlreadyComplete();

    /**
     * A message indicating the operation handler failed.
     *
     * @param msg the failure message.
     *
     * @return the message.
     */
    @Message(id = 158, value = "Operation handler failed: %s")
    String operationHandlerFailed(String msg);

    /**
     * A message indicating the operation handler failed to complete.
     *
     * @return the message.
     */
    @Message(id = 159, value = "Operation handler failed to complete")
    String operationHandlerFailedToComplete();

    /**
     * A message indicating the operation is rolling back.
     *
     * @return the message.
     */
    @Message(id = 160, value = "Operation rolling back")
    String operationRollingBack();

    /**
     * A message indicating the operation succeeded and is committing.
     *
     * @return the message.
     */
    @Message(id = 161, value = "Operation succeeded, committing")
    String operationSucceeded();

    /**
     * A message indicating there is no operation, represented by the {@code op} parameter, registered at the address,
     * represented by the {@code address} parameter.
     *
     * @param op      the operation.
     * @param address the address.
     *
     * @return the message.
     */
    @Message(id = 162, value = "There is no operation %s registered at address %s")
    String operationNotRegistered(String op, PathAddress address);


    /**
     * Creates an exception indicating an operation reply value type description is required but was not implemented
     * for the operation represented by the {@code operationName} parameter.
     *
     * @param operationName the name of the operation that requires the reply value type description.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 163, value = "An operation reply value type description is required but was not implemented for operation %s")
    IllegalStateException operationReplyValueTypeRequired(String operationName);

    /**
     * A message indicating there was a parsing problem.
     *
     * @param row the row the problem occurred at.
     * @param col the column the problem occurred at.
     * @param msg a message to concatenate.
     *
     * @return the message.
     */
    @Message(id = 164, value = "Parsing problem at [row,col]:[%d ,%d]%nMessage: %s")
    String parsingProblem(int row, int col, String msg);

    /**
     * Creates an exception indicating no configuration persister was injected.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 165, value = "No configuration persister was injected")
    StartException persisterNotInjected();

    /**
     * Creates an exception indicating the thread was interrupted waiting for the operation to prepare/fail.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 166, value = "Thread was interrupted waiting for the operation to prepare/fail")
    RequestProcessingException prepareFailThreadInterrupted();

    /**
     * Creates an exception indicating the profile has no subsystem configurations.
     *
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    //No longer used
    //@Message(id = 167, value = "Profile has no subsystem configurations")
    //XMLStreamException profileHasNoSubsystems(@Param Location location);

    /**
     * Creates an exception indicating no profile found for inclusion.
     *
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 168, value = "No profile found for inclusion")
    XMLStreamException profileNotFound(@Param Location location);

    /**
     * Creates an exception indicating the proxy handler is already registered at the location.
     *
     * @param location the location.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 169, value = "A proxy handler is already registered at location '%s'")
    IllegalArgumentException proxyHandlerAlreadyRegistered(String location);

    /**
     * Creates an exception indicating a thread was interrupted waiting to read attachment input stream from a remote
     * caller.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 170, value = "Thread was interrupted waiting to read attachment input stream from remote caller")
    RuntimeException remoteCallerThreadInterrupted();

    /**
     * A message indicating that removing services has lead to unsatisfied dependencies.
     * <p/>
     * ** Note: Use with {@link #removingServiceUnsatisfiedDependencies(String)}
     *
     * @return the message.
     */
    @Message(id = 171, value = "Removing services has lead to unsatisfied dependencies:")
    String removingServiceUnsatisfiedDependencies();

    /**
     * A message indicating that removing services has lead to unsatisfied dependencies.
     * <p/>
     * ** Note: Use with {@link #removingServiceUnsatisfiedDependencies()}
     *
     * @param name the name of the service.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "%nService %s was depended upon by ")
    String removingServiceUnsatisfiedDependencies(String name);

    /**
     * A message indicating the {@code name} is required.
     *
     * @param name the name of the required attribute.
     *
     * @return the message.
     */
    @Message(id = 172, value = "%s is required")
    String required(String name);

    /**
     * Creates an exception indicating the {@code name} is reserved.
     *
     * @param name     the name that is reserved.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 173, value = "%s is reserved")
    XMLStreamException reserved(String name, @Param Location location);

    /**
     * A message indicating a resource does not exist.
     *
     * @param resource the resource.
     *
     * @return the message.
     */
    @Message(id = 174, value = "Resource does not exist: %s")
    String resourceNotFound(ModelNode resource);

    /**
     * Creates an exception indicating a resource does not exist.
     *
     * @param ancestor the ancestor path.
     * @param address  the address.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 175, value = "Resource %s does not exist; a resource at address %s cannot be created until all ancestor resources have been added")
    OperationFailedRuntimeException resourceNotFound(PathAddress ancestor, PathAddress address);

    /**
     * Creates an exception indicating the rollback has already been invoked.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 176, value = "rollback() has already been invoked")
    IllegalStateException rollbackAlreadyInvoked();

    /**
     * A message indicating a schema with URI, represented by the {@code schemaUri} parameter, is already registered
     * with the location, represented by the {@code location} parameter.
     *
     * @param schemaUri the schema URI.
     * @param location  the location.
     *
     * @return the message.
     */
    @Message(id = 177, value = "Schema with URI %s already registered with location %s")
    String schemaAlreadyRegistered(String schemaUri, String location);

    /**
     * A message indicating the schema was not found wit the {@code uri}.
     *
     * @param uri the schema URI.
     *
     * @return the message.
     */
    @Message(id = 178, value = "No schema location with URI %s found")
    String schemaNotFound(String uri);

    /**
     * Creates an exception indicating the service install was cancelled.
     *
     * @return a {@link CancellationException} for the error.
     */
    @Message(id = 179, value = "Service install was cancelled")
    CancellationException serviceInstallCancelled();

    /**
     * A message indicating the missing services.
     *
     * @param sb the missing services.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "is missing [%s]")
    String servicesMissing(StringBuilder sb);

    /**
     * A message that indicates there are services with missing or unavailable dependencies.
     *
     * @return the message.
     */
    @Message(id = 180, value = "Services with missing/unavailable dependencies")
    String servicesMissingDependencies();

    /**
     * Creates an exception indicating the get service registry only supported in runtime operations.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 181, value = "Get service registry only supported in runtime operations")
    IllegalStateException serviceRegistryRuntimeOperationsOnly();

    /**
     * Creates an exception indicating the service removal only supported in runtime operations.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 182, value = "Service removal only supported in runtime operations")
    IllegalStateException serviceRemovalRuntimeOperationsOnly();

    /**
     * A message for the service status report header.
     *
     * @return the message.
     */
    @Message(id = 183, value = "Service status report%n")
    String serviceStatusReportHeader();

    /**
     * A message for the service status report indicating new missing or unsatisfied dependencies.
     *
     * @return the message.
     */
    @Message(id = 184, value = "   New missing/unsatisfied dependencies:%n")
    String serviceStatusReportDependencies();

    /**
     * A message for the service status report for missing dependencies.
     *
     * @param serviceName the name of the service
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "      %s (missing) dependents: %s %n")
    String serviceStatusReportMissing(ServiceName serviceName, String dependents);

    /**
     * A message for the service status report for unavailable dependencies.
     *
     * @param serviceName the name of the service
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "      %s (unavailable) dependents: %s %n")
    String serviceStatusReportUnavailable(ServiceName serviceName, String dependents);

    /**
     * A message for the service status report indicating new corrected service.
     *
     * @return the message.
     */
    @Message(id = 185, value = "   Newly corrected services:%n")
    String serviceStatusReportCorrected();

    /**
     * A message for the service status report for no longer required dependencies.
     *
     * @param serviceName the name of the service
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "      %s (no longer required)%n")
    String serviceStatusReportNoLongerRequired(ServiceName serviceName);

    /**
     * A message for the service status report for unavailable dependencies.
     *
     * @param serviceName the name of the service
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "      %s (new available)%n")
    String serviceStatusReportAvailable(ServiceName serviceName);

    /**
     * A message for the service status report for failed services.
     *
     * @return the message.
     */
    @Message(id = 186, value = "  Services which failed to start:")
    String serviceStatusReportFailed();

    /**
     * Creates an exception indicating the get service target only supported in runtime operations.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 187, value = "Get service target only supported in runtime operations")
    IllegalStateException serviceTargetRuntimeOperationsOnly();

    /**
     * Creates an exception indicating the stage is already complete.
     *
     * @param stage the stage.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 188, value = "Stage %s is already complete")
    IllegalStateException stageAlreadyComplete(OperationContext.Stage stage);

    /**
     * A message indicating the step handler failed after completion.
     *
     * @param handler the handler that failed.
     *
     * @return the message.
     */
    @Message(id = 189, value = "Step handler %s failed after completion")
    String stepHandlerFailed(OperationStepHandler handler);

    /**
     * A message indicating the step handler for the operation failed handling operation rollback.
     *
     * @param handler the handler that failed.
     * @param op      the operation.
     * @param address the path address.
     * @param cause   the error.
     *
     * @return the message.
     */
    @Message(id = 190, value = "Step handler %s for operation %s at address %s failed handling operation rollback -- %s")
    String stepHandlerFailedRollback(OperationStepHandler handler, String op, PathAddress address, Throwable cause);

    /**
     * A message indicating an interruption awaiting subsystem boot operation execution.
     *
     * @return the message.
     */
    @Message(id = 191, value = "Interrupted awaiting subsystem boot operation execution")
    String subsystemBootInterrupted();

    /**
     * A message indicating the boot operations for the subsystem, represented by the {@code name} parameter, failed
     * without explanation.
     *
     * @param name the name of the subsystem.
     *
     * @return the message.
     */
    @Message(id = 192, value = "Boot operations for subsystem %s failed without explanation")
    String subsystemBootOperationFailed(String name);

    /**
     * A message indicating a failure executing subsystem boot operations.
     *
     * @return the message.
     */
    @Message(id = 193, value = "Failed executing subsystem %s boot operations")
    String subsystemBootOperationFailedExecuting(String name);

    /**
     * Creates an exception indicating the table is full.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 194, value = "Table is full!")
    IllegalStateException tableIsFull();

    /**
     * Creates an exception indicating an interruption awaiting a transaction commit or rollback.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 195, value = "Interrupted awaiting transaction commit or rollback")
    RuntimeException transactionInterrupted();

    /**
     * Creates an exception indicating a timeout occurred waiting for the transaction.
     *
     * @param type the transaction type.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 196, value = "A timeout occurred waiting for the transaction to %s")
    RuntimeException transactionTimeout(String type);

    /**
     * Creates an exception indicating an unexpected attribute, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 197, value = "Unexpected attribute '%s' encountered")
    XMLStreamException unexpectedAttribute(QName name, @Param Location location);

    /**
     * Creates an exception indicating an unexpected element, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 198, value = "Unexpected element '%s' encountered")
    XMLStreamException unexpectedElement(QName name, @Param Location location);

    /**
     * Creates an exception indicating an unexpected end of an element, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 199, value = "Unexpected end of element '%s' encountered")
    XMLStreamException unexpectedEndElement(QName name, @Param Location location);

    /**
     * Creates an exception indicating the {@code storage} was unexpected.
     *
     * @param storage the storage that was unexpected.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 200, value = "Unexpected storage %s")
    IllegalStateException unexpectedStorage(AttributeAccess.Storage storage);

    /**
     * A message indicating the attribute, represented by the {@code name} parameter, is unknown.
     *
     * @param name the attribute name.
     *
     * @return the message.
     */
    @Message(id = 201, value = "Unknown attribute %s")
    String unknownAttribute(String name);

    /**
     * A message indicating there is no known child type with the name, represented by the {@code name} parameter.
     *
     * @param name the name of the child.
     *
     * @return the message.
     */
    @Message(id = 202, value = "No known child type named %s")
    String unknownChildType(String name);

    /**
     * Creates an exception indicating the property, represented by the {@code name} parameter, is unknown.
     *
     * @param name the name of the property.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 203, value = "Unknown property in interface criteria list: %s")
    RuntimeException unknownCriteriaInterfaceProperty(String name);

    /**
     * A message indicating the interface criteria type, represented by the {@code type} parameter, is unknown.
     *
     * @param type the unknown criteria type.
     *
     * @return the message.
     */
    @Message(id = 204, value = "Unknown interface criteria type %s")
    String unknownCriteriaInterfaceType(String type);

    /**
     * Creates an exception indicating the interface, represented by the {@code value} attribute, for the attribute,
     * represented by the {@code attributeName} parameter, is unknown on in the element.
     *
     * @param value         the value of the attribute.
     * @param attributeName the attribute name.
     * @param elementName   the element name for the attribute.
     * @param location      the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 205, value = "Unknown interface %s %s must be declared in element %s")
    XMLStreamException unknownInterface(String value, String attributeName, String elementName, @Param Location location);

    /**
     * Creates an exception indicating an unknown {@code elementName1} {@code value} {@code elementName2} must be
     * declared in the element represented by the {@code parentElement} parameter.
     *
     * @param elementName1  the name of the first element.
     * @param value         the value.
     * @param elementName2  the name of the second element.
     * @param parentElement the parent element name.
     * @param location      the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 206, value = "Unknown %s %s %s must be declared in element %s")
    XMLStreamException unknownValueForElement(String elementName1, String value, String elementName2, String parentElement, @Param Location location);

    /**
     * A message indicating the validation failed.
     *
     * @param name the parameter name the validation failed on.
     *
     * @return the message.
     */
    @Message(id = 207, value = "Validation failed for %s")
    String validationFailed(String name);

    /**
     * A message indicating that there are more services than would be practical to display
     *
     * @param number the number of services that were not displayed
     *
     * @return the message.
     */
    @Message(id = 208, value = "... and %s more")
    String andNMore(int number);

    /**
     * Creates an exception indicating an invalid value, represented by the {@code value} parameter, was found for the
     * attribute, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the attribute name.
     * @param validValues the legal values for the attribute
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 209, value = "Invalid value '%s' for attribute '%s' -- valid values are %s")
    XMLStreamException invalidAttributeValue(String value, QName name, Set<String> validValues, @Param Location location);

    /**
     * Creates an exception message indicating an expression could not be resolved due to lack of security permissions.
     *
     * @param toResolve  the node being resolved
     * @param e the SecurityException
     * @return an {@link OperationFailedException} for the caller
     */
    @Message(id = 210, value = "Caught SecurityException attempting to resolve expression '%s' -- %s")
    String noPermissionToResolveExpression(ModelNode toResolve, SecurityException e);

    /**
     * Creates an exception message indicating an expression could not be resolved due to no corresponding system property
     * or environment variable.
     *
     * @param toResolve  the node being resolved
     * @param e the SecurityException
     * @return an {@link OperationFailedException} for the caller
     */
    @Message(id = 211, value = "Cannot resolve expression '%s' -- %s")
    String cannotResolveExpression(ModelNode toResolve, IllegalStateException e);

    /**
     * Creates an exception indicating the resource is a duplicate.
     *
     * @param address the address of the resource.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 212, value = "Duplicate resource %s")
    OperationFailedRuntimeException duplicateResourceAddress(PathAddress address);

    /**
     * Creates an exception indicating a resource cannot be removed due to the existence of child resources.
     *
     * @param children the address elements for the children.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 213, value = "Cannot remove resource before removing child resources %s")
    OperationFailedException cannotRemoveResourceWithChildren(List<PathElement> children);

    /**
     * Creates an exception indicating the canonical file for the main file could not be found.
     *
     * @param name  the main file.
     * @param configurationDir the configuration directory
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 214, value = "Could not get main file: %s. Specified files must be relative to the configuration dir: %s")
    IllegalStateException mainFileNotFound(String name, File configurationDir);

    // 215 free

    /**
     * Creates an exception indicating a resource cannot be found.
     *
     * @param pathAddress the address for the resource.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 216, value = "Management resource '%s' not found")
    NoSuchResourceException managementResourceNotFound(PathAddress pathAddress);

    /**
     * Creates an exception message indicating a child resource cannot be found.
     *
     * @param childAddress the address element for the child.
     *
     * @return an message for the error.
     */
    @Message(id = 217, value = "Child resource '%s' not found")
    String childResourceNotFound(PathElement childAddress);

    /**
     * Creates an exception indicating a node is already registered at the location.
     *
     * @param location the location of the existing node.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 218, value = "A node is already registered at '%s'")
    IllegalArgumentException nodeAlreadyRegistered(String location);

    /**
     * Creates an exception indicating that an attempt was made to remove an extension before removing all of its
     * subsystems.
     *
     * @param moduleName the name of the extension
     * @param subsystem the name of the subsystem
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 219, value = "An attempt was made to unregister extension %s which still has subsystem %s registered")
    IllegalStateException removingExtensionWithRegisteredSubsystem(String moduleName, String subsystem);

    /**
     * Creates an exception indicating that an attempt was made to register an override model for the root model
     * registration.
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 220, value = "An override model registration is not allowed for the root model registration")
    IllegalStateException cannotOverrideRootRegistration();

    /**
     * Creates an exception indicating that an attempt was made to register an override model for a non-wildcard
     * registration.
     *
     * @param valueName the name of the non-wildcard registration that cannot be overridden
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 221, value = "An override model registration is not allowed for non-wildcard model registrations. This registration is for the non-wildcard name '%s'.")
    IllegalStateException cannotOverrideNonWildCardRegistration(String valueName);

    /**
     * Creates an exception indicating that an attempt was made to remove a wildcard model registration via
     * the unregisterOverrideModel API.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 222, value = "A registration named '*' is not an override model and cannot be unregistered via the unregisterOverrideModel API.")
    IllegalArgumentException wildcardRegistrationIsNotAnOverride();

    /**
     * Creates an exception indicating that an attempt was made to remove a resource registration from the root registration.
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 223, value = "The root resource registration does not support overrides, so no override can be removed.")
    IllegalStateException rootRegistrationIsNotOverridable();

    /**
     * Creates an exception indicating there is no operation, represented by the {@code op} parameter, registered at the address,
     * represented by the {@code address} parameter.
     *
     * @param op      the operation.
     * @param address the address.
     * @return the message.
     */
    @Message(id = 224, value = "There is no operation %s registered at address %s")
    IllegalArgumentException operationNotRegisteredException(String op, PathAddress address);


    /**
     * Creates a runtime exception indicating there was a failure to recover services during an operation rollback
     *
     * @param cause the cause of the failure
     * @return the runtime exception.
     */
    @Message(id = 225, value = "Failed to recover services during operation rollback")
    RuntimeException failedToRecoverServices(@Param OperationFailedException cause);

    /**
     * Creates an IllegalStateException indicating a subsystem with the given name has already been registered by
     * a different extension.
     *
     * @param subsystemName the cause of the failure
     * @return the runtime exception.
     */
    @Message(id = 226, value = "A subsystem named '%s' cannot be registered by extension '%s' -- a subsystem with that name has already been registered by extension '%s'.")
    IllegalStateException duplicateSubsystem(String subsystemName, String duplicatingModule, String existingModule);

    /**
     * Creates an exception indicating that the operation is missing one of the standard fields.
     *
     * @param field the standard field name
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 227, value = "Operation has no '%s' field. %s")
    IllegalArgumentException validationFailedOperationHasNoField(String field, String operation);

    /**
     * Creates an exception indicating that the operation has an empty name.
     *
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 228, value = "Operation has a null or empty name. %s")
    IllegalArgumentException validationFailedOperationHasANullOrEmptyName(String operation);

    /**
     * Creates an exception indicating that the operation could not be found
     *
     * @param name the name of the operation
     * @param address the operation address
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 229, value = "No operation called '%s' at '%s'. %s")
    IllegalArgumentException validationFailedNoOperationFound(String name, PathAddress address, String operation);

    /**
     * Creates an exception indicating that the operation contains a parameter not in its descriptor
     *
     * @param paramName the name of the parameter in the operation
     * @param parameterNames the valid parameter names
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 230, value = "Operation contains a parameter '%s' which is not one of the expected parameters %s. %s")
    IllegalArgumentException validationFailedActualParameterNotDescribed(String paramName, Set<String> parameterNames, String operation);

    /**
     * Creates an exception indicating that the operation does not contain a required parameter
     *
     * @param paramName the name of the required parameter
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 231, value = "Required parameter %s is not present. %s")
    IllegalArgumentException validationFailedRequiredParameterNotPresent(String paramName, String operation);

    /**
     * Creates an exception indicating that the operation contains both an alternative and a required parameter
     *
     * @param alternative the name of the alternative parameter
     * @param paramName the name of the required parameter
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 232, value = "Alternative parameter '%s' for required parameter '%s' was used. Please use one or the other. %s")
    IllegalArgumentException validationFailedRequiredParameterPresentAsWellAsAlternative(String alternative, String paramName, String operation);

    /**
     * Creates an exception indicating that an operation parameter could not be converted to the required type
     *
     * @param paramName the name of the required parameter
     * @param type the required type
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 233, value = "Could not convert the parameter '%s' to a %s. %s")
    IllegalArgumentException validationFailedCouldNotConvertParamToType(String paramName, ModelType type, String operation);

    /**
     * Creates an exception indicating that an operation parameter value is smaller than the allowed minimum value
     *
     * @param value the name of the required parameter
     * @param paramName the name of the required parameter
     * @param min the minimum value
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 234, value = "The value '%s' passed in for '%s' is smaller than the minimum value '%s'. %s")
    IllegalArgumentException validationFailedValueIsSmallerThanMin(Number value, String paramName, Number min, String operation);

    /**
     * Creates an exception indicating that an operation parameter value is greater than the allowed minimum value
     *
     * @param value the name of the required parameter
     * @param paramName the name of the required parameter
     * @param max the minimum value
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 235, value = "The value '%s' passed in for '%s' is bigger than the maximum value '%s'. %s")
    IllegalArgumentException validationFailedValueIsGreaterThanMax(Number value, String paramName, Number max, String operation);

    /**
     * Creates an exception indicating that an operation parameter value is shorter than the allowed minimum length
     *
     * @param value the name of the required parameter
     * @param paramName the name of the required parameter
     * @param minLength the minimum value
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 236, value = "The value '%s' passed in for '%s' is shorter than the minimum length '%s'. %s")
    IllegalArgumentException validationFailedValueIsShorterThanMinLength(Object value, String paramName, Object minLength, String operation);

    /**
     * Creates an exception indicating that an operation parameter value is longer than the allowed maximum length
     *
     * @param value the name of the required parameter
     * @param paramName the name of the required parameter
     * @param maxLength the minimum value
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 237, value = "The value '%s' passed in for '%s' is longer than the maximum length '%s'. %s")
    IllegalArgumentException validationFailedValueIsLongerThanMaxLength(Object value, String paramName, Object maxLength, String operation);

    /**
     * Creates an exception indicating that an operation parameter list value has an element that is not of the accepted type
     *
     * @param paramName the name of the required parameter
     * @param elementType the expected element type
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 238, value = "%s is expected to be a list of %s. %s")
    IllegalArgumentException validationFailedInvalidElementType(String paramName, ModelType elementType, String operation);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * the required attribute of a parameter in the operation description is not a boolean.
     *
     * @param paramName the name of the parameter
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 239, value = "'" + ModelDescriptionConstants.REQUIRED + "' parameter: '%s' must be a boolean in the description of the operation at %s: %s")
    String invalidDescriptionRequiredFlagIsNotABoolean(String paramName, PathAddress address, ModelNode description);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * a parameter is undefined in the operation description.
     *
     * @param name the name of the parameter
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 240, value = "Undefined request property '%s' in description of the operation at %s: %s")
    String invalidDescriptionUndefinedRequestProperty(String name, PathAddress address, ModelNode description);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * a parameter has no type in the operation description
     *
     * @param paramName the name of the parameter
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 241, value = "There is no type for parameter '%s' in the description of the operation at %s: %s")
    String invalidDescriptionNoParamTypeInDescription(String paramName, PathAddress address, ModelNode description);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * a parameter has an invalid type in the operation description
     *
     * @param paramName the name of the parameter
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 242, value = "Could not determine the type of parameter '%s' in the description of the operation at %s: %s")
    String invalidDescriptionInvalidParamTypeInDescription(String paramName, PathAddress address, ModelNode description);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * a parameter has a min or max attribute value of a different type from its expected value.
     *
     * @param minOrMax {@code min} or {@code max}
     * @param paramName the name of the parameter
     * @param expectedType the expected type
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 243, value = "The '%s' attribute of the '%s' parameter can not be converted to its type: %s in the description of the operation at %s: %s")
    String invalidDescriptionMinMaxForParameterHasWrongType(String minOrMax, String paramName, ModelType expectedType, PathAddress address, ModelNode description);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * a parameter has a min-length or max-length attribute value that is not an integer.
     *
     * @param minOrMaxLength {@code min} or {@code max}
     * @param paramName the name of the parameter
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 244, value = "The '%s' attribute of the '%s' parameter can not be converted to an integer in the description of the operation at %s: %s")
    String invalidDescriptionMinMaxLengthForParameterHasWrongType(String minOrMaxLength, String paramName, PathAddress address, ModelNode description);

    /**
     * Creates an exception indicating the {@code value} for the {@code name} must be a valid port number.
     *
     * @param name     the name for the value that must be a port number.
     * @param value    the invalid value.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 245, value = "Illegal '%s' value %s -- must be a valid port number")
    XMLStreamException invalidPort(String name, String value, @Param Location location);

    @Message(id = 246, value = "Cannot resolve the localhost address to create a UUID-based name for this process")
    RuntimeException cannotResolveProcessUUID(@Cause UnknownHostException cause);

    /**
     * Creates an exception indicating a user tried calling ServiceController.setMode(REMOVE) from an operation handler.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 247, value = "Do not call ServiceController.setMode(REMOVE), use OperationContext.removeService() instead.")
    IllegalStateException useOperationContextRemoveService();

    /**
     * Creates an exception indicating that the value of the specified parameter does not match any of the allowed
     * values.
     *
     * @param value the parameter value.
     * @param parameterName the parameter name.
     * @param allowedValues a set containing the allowed values.
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 248, value="Invalid value %s for %s; legal values are %s")
    OperationFailedException invalidEnumValue(String value, String parameterName, Set<?> allowedValues);

    /*
     * Creates an exception indicating a user tried to directly update the configuration model of a managed domain
     * server rather, bypassing the Host Controller.
     *
     * @param operation the name of the operation
     * @param address the address of the operation
     *
     * @return a {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 249, value = "Operation '%s' targeted at resource '%s' was directly invoked by a user. " +
            "User operations are not permitted to directly update the persistent configuration of a server in a managed domain.")
    OperationFailedRuntimeException modelUpdateNotAuthorized(String operation, PathAddress address);

    /*
     * Creates an exception indicating an operation handler tried to access the server results portion of an operation
     * response on a non-HostController process.
     *
     * @param validType ProcessType.HOST_CONTROLLER -- a param here so it won't be translated
     * @param processType the type of process that tried to access the server results
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 250, value = "An operation handler attempted to access the operation response server results object " +
            "on a process type other than '%s'. The current process type is '%s'")
    IllegalStateException serverResultsAccessNotAllowed(ProcessType validType, ProcessType processType);

    @Message(id = 251, value = "Can't have both loopback and inet-address criteria")
    String cantHaveBothLoopbackAndInetAddressCriteria();

    @Message(id = 252, value = "Can't have both link-local and inet-address criteria")
    String cantHaveBothLinkLocalAndInetAddressCriteria();

    @Message(id = 253, value = "Can't have same criteria for both not and inclusion %s")
    String cantHaveSameCriteriaForBothNotAndInclusion(InterfaceCriteria interfaceCriteria);

    @Message(id = 254, value = "Invalid value '%s' for attribute '%s' -- no interface configuration with that name exists")
    OperationFailedException nonexistentInterface(String attributeValue, String attributeName);

    @Message(id = 255, value = "%s is empty")
    IllegalArgumentException emptyVar(String name);

    @Message(id = 256, value = "Could not find a path called '%s'")
    IllegalArgumentException pathEntryNotFound(String pathName);

    @Message(id = 257, value="Path entry is read-only: '%s'")
    IllegalArgumentException pathEntryIsReadOnly(String pathName);

    @Message(id = 258, value="There is already a path entry called: '%s'")
    IllegalArgumentException pathEntryAlreadyExists(String pathName);

    @Message(id = 259, value="Could not find relativeTo path '%s' for relative path '%s'")
    IllegalStateException pathEntryNotFoundForRelativePath(String relativePath, String pathName);

    @Message(id = 260, value="Invalid relativePath value '%s'")
    IllegalArgumentException invalidRelativePathValue(String relativePath);

    @Message(id = 261, value="'%s' is a Windows absolute path")
    IllegalArgumentException pathIsAWindowsAbsolutePath(String path);

    @Message(id = 262, value="Path '%s' is read-only; it cannot be removed")
    OperationFailedException cannotRemoveReadOnlyPath(String pathName);

    @Message(id = 263, value="Path '%s' is read-only; it cannot be modified")
    OperationFailedException cannotModifyReadOnlyPath(String pathName);
    /**
     * An exception indicating the {@code name} may not be {@link ModelType#EXPRESSION}.
     *
     * @param name the name of the attribute or parameter value that cannot be an expression
     *
     * @return the exception.
     */
    @Message(id = 264, value = "%s may not be ModelType.EXPRESSION")
    OperationFailedException expressionNotAllowed(String name);

    @Message(id = 265, value = "PathManager not available on processes of type '%s'")
    IllegalStateException pathManagerNotAvailable(ProcessType processType);

    /**
     * Creates an exception indicating the {@code value} for the attribute, represented by the {@code name} parameter,
     * is not a valid multicast address.
     *
     * @param cause    the cause of the error.
     * @param value    the invalid value.
     * @param name     the name of the attribute.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 266, value = "Value %s for attribute %s is not a valid multicast address")
    OperationFailedException unknownMulticastAddress(@Cause UnknownHostException cause, String value, String name);

    @Message(id = 267, value="Path '%s' cannot be removed, since the following paths depend on it: %s")
    OperationFailedException cannotRemovePathWithDependencies(String pathName, Set<String> dependencies);

    @Message(id = 268, value = "Failed to rename temp file %s to %s")
    ConfigurationPersistenceException failedToRenameTempFile(@Cause Throwable cause, File temp, File file);


    @Message(id = 269, value = "Invalid locale format:  %s")
    String invalidLocaleString(String unparsed);

    @Message(id = 270, value = "<one or more transitive dependencies>")
    String transitiveDependencies();

    /**
     * Creates a message indicating the operation is cancelled.
     *
     * @return the message.
     */
    @Message(id = 271, value = "Operation cancelled")
    String operationCancelled();

    /**
     * Creates a message indicating the operation is cancelled asynchronously.
     *
     * @return a {@link CancellationException} for the error.
     */
    @Message(id = 272, value = "Operation cancelled asynchronously")
    OperationCancellationException operationCancelledAsynchronously();

    @Message(id = 273, value = "Stream was killed")
    IOException streamWasKilled();

    @Message(id = 274, value = "Stream was closed")
    IOException streamWasClosed();

    @Message(id = 275, value = "Cannot define both '%s' and '%s'")
    OperationFailedException cannotHaveBothParameters(String nameA, String name2);

    @Message(id = 276, value = "Failed to delete file %s")
    IllegalStateException couldNotDeleteFile(File file);

    @Message(id = 277, value = "An alias is already registered at location '%s'")
    IllegalArgumentException aliasAlreadyRegistered(String location);

    @Message(id = 278, value = "Expected an address under '%s', was '%s'")
    IllegalArgumentException badAliasConvertAddress(PathAddress aliasAddress, PathAddress actual);

    @Message(id = 279, value = "Alias target address not found: %s")
    IllegalArgumentException aliasTargetResourceRegistrationNotFound(PathAddress targetAddress);

    @Message(id = 280, value = "No operation called '%s' found for alias address '%s' which maps to '%s'")
    IllegalArgumentException aliasStepHandlerOperationNotFound(String name, PathAddress aliasAddress, PathAddress targetAddress);

    @Message(id = 281, value = "Resource registration is not an alias")
    IllegalStateException resourceRegistrationIsNotAnAlias();

    @Message(id = 282, value = "Model contains fields that are not known in definition, fields: %s, path: %s")
    RuntimeException modelFieldsNotKnown(Set<String> fields, PathAddress address);


    @Message(id = 283, value = "Could not marshal attribute as element: %s")
    UnsupportedOperationException couldNotMarshalAttributeAsElement(String attributeName);

    @Message(id = 284, value = "Could not marshal attribute as attribute: %s")
    UnsupportedOperationException couldNotMarshalAttributeAsAttribute(String attributeName);

    @Message(id = 285, value = "Operation %s invoked against multiple target addresses failed at address %s with failure description %s")
    String wildcardOperationFailedAtSingleAddress(String operation, PathAddress address, String failureMessage);

    @Message(id = 286, value = "Operation %s invoked against multiple target addresses failed at address %s. See the operation result for details.")
    String wildcardOperationFailedAtSingleAddressWithComplexFailure(String operation, PathAddress address);

    @Message(id = 287, value = "Operation %s invoked against multiple target addresses failed at addresses %s. See the operation result for details.")
    String wildcardOperationFailedAtMultipleAddresses(String operation, Set<PathAddress> addresses);

    @Message(id = 288, value = "One or more services were unable to start due to one or more indirect dependencies not being available.")
    String missingTransitiveDependencyProblem();

    @Message(id = Message.NONE, value = "Services that were unable to start:")
    String missingTransitiveDependents();

    @Message(id = Message.NONE, value = "Services that may be the cause:")
    String missingTransitiveDependencies();

    @Message(id = 289, value = "No operation entry called '%s' registered at '%s'")
    String noOperationEntry(String op, PathAddress pathAddress);

    @Message(id = 290, value = "No operation handler called '%s' registered at '%s'")
    String noOperationHandler(String op, PathAddress pathAddress);

    @Message(id = 291, value = "There is no registered path to resolve with path attribute '%s' and/or relative-to attribute '%s on: %s")
    IllegalStateException noPathToResolve(String pathAttributeName, String relativeToAttributeName, ModelNode model);

    @Message(id = 292, value = "Attributes do not support expressions in the target model version and this resource will need to be ignored on the target host.")
    String attributesDontSupportExpressions();

    @Message(id = 293, value = "Attributes are not understood in the target model version and this resource will need to be ignored on the target host.")
    String attributesAreNotUnderstoodAndMustBeIgnored();

    @Message(id = 294, value = "Transforming resource %s to core model version '%s' -- %s %s")
    String transformerLoggerCoreModelResourceTransformerAttributes(PathAddress pathAddress, ModelVersion modelVersion, String attributeNames, String message);

    @Message(id = 295, value = "Transforming operation %s at resource %s to core model version '%s' -- %s %s")
    String transformerLoggerCoreModelOperationTransformerAttributes(ModelNode op, PathAddress pathAddress, ModelVersion modelVersion, String attributeNames, String message);

    @Message(id = 296, value = "Transforming resource %s to subsystem '%s' model version '%s' -- %s %s")
    String transformerLoggerSubsystemModelResourceTransformerAttributes(PathAddress pathAddress, String subsystem, ModelVersion modelVersion, String attributeNames, String message);

    @Message(id = 297, value = "Transforming operation %s at resource %s to subsystem '%s' model version '%s' -- %s %s")
    String transformerLoggerSubsystemModelOperationTransformerAttributes(ModelNode op, PathAddress pathAddress, String subsystem, ModelVersion modelVersion, String attributeNames, String message);

    @Message(id = 298, value="Node contains an unresolved expression %s -- a resolved model is required")
    OperationFailedException illegalUnresolvedModel(String expression);

    //The 'details' attribute needs to list the rejected attributes and what was wrong with them, an example is message id 14898
    @Message(id = 299, value = "Transforming resource %s for host controller '%s' to core model version '%s' -- there were problems with some of the attributes and this resource will need to be ignored on that host. Details of the problems: %s")
    OperationFailedException rejectAttributesCoreModelResourceTransformer(PathAddress pathAddress, String legacyHostName, ModelVersion modelVersion, List<String> details);

    @Message(id = 300, value = "Transforming resource %s for host controller '%s' to subsystem '%s' model version '%s' --there were problems with some of the attributes and this resource will need to be ignored on that host. Details of problems: %s")
    OperationFailedException rejectAttributesSubsystemModelResourceTransformer(PathAddress pathAddress, String legacyHostName, String subsystem, ModelVersion modelVersion, List<String> details);

    @Message(id = 301, value = "The following attributes do not support expressions: %s")
    String attributesDoNotSupportExpressions(Set<String> attributeNames);

    /** The attribute name list fragment to include in the transformation logging messages */
    @Message(id = Message.NONE, value = "attributes %s")
    String attributeNames(Set<String> attributes);

    @Message(id = 302, value = "The following attributes are not understood in the target model version and this resource will need to be ignored on the target host: %s")
    String attributesAreNotUnderstoodAndMustBeIgnored(Set<String> attributeNames);

    @Message(id = 303, value = "Resource %s is rejected on the target host, and will need to be ignored on the host")
    String rejectedResourceResourceTransformation(PathAddress address);

    @Message(id = 304, value = "Resource %s is rejected on the target host and will need to be ignored on the host: %s")
    String rejectResourceOperationTransformation(PathAddress address, ModelNode operation);

    /**
     * Creates an exception indicating that {@code discoveryOptionsName} must be declared
     * or the {@code hostName} and {@code portName} need to be provided.
     *
     * @param discoveryOptionsName the discovery-options element name.
     * @param hostName the host attribute name.
     * @param portName the port attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 305, value = "Unless the Host Controller is started with command line option %s and the %s " +
            "attribute is not set to %s, %s must be declared or the %s and the %s need to be provided.")
    XMLStreamException discoveryOptionsMustBeDeclared(String adminOnlyCmd, String policyAttribute, String fetchValue,
                                                      String discoveryOptionsName, String hostName, String portName,
                                                      @Param Location location);

    @Message(id = 306, value = "read only context")
    IllegalStateException readOnlyContext();

    @Message(id = 307, value = "We are trying to read data from the master host controller, which is currently busy executing another set of operations. This is a temporary situation, please retry")
    String cannotGetControllerLock();

    @Message(id = 308, value = "Cannot configure an interface to use 'any-ipv6-address' when system property java.net.preferIPv4Stack is true")
    String invalidAnyIPv6();

    @Message(id = 309, value = "Legacy extension '%s' is not supported on servers running this version. The extension " +
            "is only supported for use by hosts running a previous release in a mixed-version managed domain")
    String unsupportedLegacyExtension(String extensionName);

    @Message(id = 310, value = "Extension module %s not found")
    OperationFailedException extensionModuleNotFound(@Cause ModuleNotFoundException cause, String module);

    @Message(id = 311, value = "Failed to load Extension module %s")
    RuntimeException extensionModuleLoadingFailure(@Cause ModuleLoadException cause, String module);

    @Message(id = 312, value = "no context to delegate with id: %s")
    IllegalStateException noContextToDelegateTo(int operationId);

    @Message(id = 313, value = "Unauthorized to execute operation '%s' for resource '%s' -- %s")
    UnauthorizedException unauthorized(String name, PathAddress address, ModelNode explanation);

    @Message(id = 314, value = "Users with multiple roles are not allowed")
    SecurityException illegalMultipleRoles();

    @Message(id = 315, value = "An unexpected number of AccountPrincipals %d have been found in the current Subject.")
    IllegalStateException unexpectedAccountPrincipalCount(int count);

    @Message(id = 316, value = "Different realms '%s' '%s' found in single Subject")
    IllegalStateException differentRealmsInSubject(String realmOne, String realmTwo);

    @Message(id = 317, value = "There is no handler called '%s'")
    IllegalStateException noHandlerCalled(String name);

    @Message(id = 318, value = "The operation context is not an AbstractOperationContext")
    OperationFailedException operationContextIsNotAbstractOperationContext();

    @Message(id = 319, value = "The handler is referenced by %s and so cannot be removed")
    IllegalStateException handlerIsReferencedBy(Set<PathAddress> references);

    @Message(id = 320, value = "The resolved file %s either does not exist or is a directory")
    IllegalStateException resolvedFileDoesNotExistOrIsDirectory(File file);

    @Message(id = 321, value = "Could not back up '%s' to '%s'")
    IllegalStateException couldNotBackUp(@Cause IOException cause, String absolutePath, String absolutePath1);

    @Message(id = 322, value = "Attempt was made to both remove and add a handler from a composite operation - update the handler instead")
    IllegalStateException attemptToBothRemoveAndAddHandlerUpdateInstead();

    @Message(id = 323, value = "Attempt was made to both add and remove a handler from a composite operation")
    IllegalStateException attemptToBothAddAndRemoveAndHandlerFromCompositeOperation();

    @Message(id = 324, value = "Attempt was made to both update and remove a handler from a composite operation")
    IllegalStateException attemptToBothUpdateAndRemoveHandlerFromCompositeOperation();

    @Message(id = 325, value = "Attempt was made to both remove and add a handler reference from a composite operation")
    IllegalStateException attemptToBothRemoveAndAddHandlerReferenceFromCompositeOperation();

    // This Message ID never made it to a Final release although it was close so may be referenced!

    //@Message(id = 326, value = "Unable to unmarshall Subject received for request.")
    //IOException unableToUnmarshallSubject(@Cause ClassNotFoundException e);

    @Message(id = 327, value = "Unknown role '%s'")
    IllegalArgumentException unknownRole(String roleName);

    @Message(id = 328, value = "Cannot remove standard role '%s'")
    IllegalStateException cannotRemoveStandardRole(String roleName);

    @Message(id = 329, value = "Unknown base role '%s'")
    IllegalArgumentException unknownBaseRole(String roleName);

    @Message(id = 330, value = "Role '%s' is already registered")
    IllegalStateException roleIsAlreadyRegistered(String roleName);

    @Message(id = 331, value = "Can only create child audit logger for main audit logger")
    IllegalStateException canOnlyCreateChildAuditLoggerForMainAuditLogger();

    @Message(id = 332, value = "Permission denied")
    String permissionDenied();

    @Message(id = 333, value = "Cannot add a Permission to a readonly PermissionCollection")
    SecurityException permissionCollectionIsReadOnly();

    @Message(id = 334, value = "Incompatible permission type %s")
    IllegalArgumentException incompatiblePermissionType(Class<?> clazz);

    @Message(id = 335, value = "Management resource '%s' not found")
    String managementResourceNotFoundMessage(PathAddress pathAddress);

    @Message(id = 336, value = "The following attributes are nillable in the current model but must be defined in the target model version: %s")
    String attributesMustBeDefined(Set<String> keySet);

    @Message(id = 337, value = "Unsupported Principal type '%X' received.")
    IOException unsupportedPrincipalType(byte type);

    @Message(id = 338, value = "Unsupported Principal parameter '%X' received parsing principal type '%X'.")
    IOException unsupportedPrincipalParameter(byte parameterType, byte principalType);

    @Message(id = 339, value = "The following attributes must be defined as %s in the current model: %s")
    String attributesMustBeDefinedAs(ModelNode value, Set<String> names);

    @Message(id = 340, value = "The following attributes must NOT be defined as %s in the current model: %s")
    String attributesMustNotBeDefinedAs(ModelNode value, Set<String> names);

    @Message(id = 341, value="A uri with bad syntax '%s' was passed for validation.")
    OperationFailedException badUriSyntax(String uri);

    @Message(id = 342, value = "Illegal value %d for operation header %s; value must be greater than zero")
    IllegalStateException invalidBlockingTimeout(long timeout, String headerName);

    @Message(id = 343, value = "The service container has been destabilized by a previous operation and further runtime updates cannot be processed. Restart is required.")
    String timeoutAwaitingInitialStability();

    @Message(id = 344, value = "Operation timed out awaiting service container stability")
    String timeoutExecutingOperation();

    @Message(id = 345, value = "Timeout after %d seconds waiting for existing service %s to be removed so a new instance can be installed.")
    IllegalStateException serviceInstallTimedOut(long timeout, ServiceName name);

    @LogMessage(level = Level.ERROR)
    @Message(id = 346, value = "Invalid value %s for property %s; must be a numeric value greater than zero. Default value of %d will be used.")
    void invalidDefaultBlockingTimeout(String sysPropValue, String sysPropName, long defaultUsed);

    @LogMessage(level = Level.DEBUG)
    @Message(id = 347, value = "Timeout after [%d] seconds waiting for initial service container stability before allowing runtime changes for operation '%s' at address '%s'. Operation will roll back; process restart is required.")
    void timeoutAwaitingInitialStability(long blockingTimeout, String name, PathAddress address);

    @LogMessage(level = Level.ERROR)
    @Message(id = 348, value = "Timeout after [%d] seconds waiting for service container stability. Operation will roll back. Step that first updated the service container was '%s' at address '%s'")
    void timeoutExecutingOperation(long blockingTimeout, String name, PathAddress address);

    @LogMessage(level = Level.ERROR)
    @Message(id = 349, value = "Timeout after [%d] seconds waiting for service container stability while finalizing an operation. Process must be restarted. Step that first updated the service container was '%s' at address '%s'")
    void timeoutCompletingOperation(long blockingTimeout, String name, PathAddress address);

    @LogMessage(level = Level.INFO)
    @Message(id = 350, value = "Execution of operation '%s' on remote process at address '%s' interrupted while awaiting initial response; remote process has been notified to cancel operation")
    void interruptedAwaitingInitialResponse(String operation, PathAddress proxyNodeAddress);

    @LogMessage(level = Level.INFO)
    @Message(id = 351, value = "Execution of operation '%s' on remote process at address '%s' interrupted while awaiting final response; remote process has been notified to terminate operation")
    void interruptedAwaitingFinalResponse(String operation, PathAddress proxyNodeAddress);

    @LogMessage(level = Level.INFO)
    @Message(id = 352, value = "Cancelling operation '%s' with id '%d' running on thread '%s'")
    void cancellingOperation(String operation, int id, String thread);

    @Message(id = 353, value = "No response handler for request %s")
    IOException responseHandlerNotFound(int id);

    @LogMessage(level = Level.INFO)
    @Message(id = 354, value = "Attempting reconnect to syslog handler '%s; after timeout of %d seconds")
    void attemptingReconnectToSyslog(String name, int timeout);

    @LogMessage(level = Level.INFO)
    @Message(id = 355, value = "Reconnecting to syslog handler '%s failed")
    void reconnectToSyslogFailed(String name, @Cause Throwable e);

}
