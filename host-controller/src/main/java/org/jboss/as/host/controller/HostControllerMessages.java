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

package org.jboss.as.host.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.security.sasl.SaslException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.protocol.mgmt.RequestProcessingException;
import org.jboss.as.server.ServerState;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 10800-10999. This file is using the subset 10940-10999 for host
 * controller non-logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 * <p/>
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface HostControllerMessages {

    /**
     * The messages.
     */
    HostControllerMessages MESSAGES = Messages.getBundle(HostControllerMessages.class);

    /**
     * A message indicating an argument was expected for the option.
     *
     * @param option the option that expects the argument.
     *
     * @return the message.
     */
    @Message(id = 10940, value = "Argument expected for option %s")
    String argumentExpected(String option);

    /**
     * Creates an exception indicating an attempt was made to set the {@code attributeToSet} when the {@code
     * attributeAlreadySet} was already set.
     *
     * @param attributeToSet      the attribute to set.
     * @param attributeAlreadySet the attribute was already set.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10941, value = "Attempting to set '%s' when '%s' was already set")
    IllegalArgumentException attemptingToSet(String attributeToSet, String attributeAlreadySet);

    /**
     * Creates an exception indicating an inability to connect due to authentication failures.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10942, value = "Unable to connect due to authentication failure.")
    IllegalStateException authenticationFailureUnableToConnect(@Cause Throwable cause);

    /**
     * Creates an exception indicating the remote file repository cannot be accessed from the master domain controller.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10943, value = "Cannot access a remote file repository from the master domain controller")
    IllegalStateException cannotAccessRemoteFileRepository();

    /**
     * Creates an exception indicating the inability to create a local directory.
     *
     * @param path the directory that failed to create.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 10944, value = "Unable to create local directory: %s")
    IOException cannotCreateLocalDirectory(File path);

    /**
     * Creates an exception indicating the default address cannot be obtained for communicating with the
     * ProcessController.
     *
     * @param cause          the cause of the error.
     * @param defaultAddress the default address.
     * @param option         the option.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10945, value = "Cannot obtain a valid default address for communicating with " +
            "the ProcessController using either %s or InetAddress.getLocalHost(). Please check your system's " +
            "network configuration or use the %s command line switch to configure a valid address")
    RuntimeException cannotObtainValidDefaultAddress(@Cause Throwable cause, String defaultAddress, String option);

    /**
     * A message indicating the server, represented by the {@code serverName} parameter, cannot restart as it is not
     * currently started.
     *
     * @param serverName the name of the server.
     * @param status     the status of the server.
     *
     * @return the message.
     */
    @Message(id = 10946, value = "Cannot restart server %s as it is not currently started; it is %s")
    String cannotRestartServer(String serverName, ServerStatus status);

    /**
     * A message indicating the servers cannot start when the host controller is running in the mode represented by the
     * {@code mode} parameter.
     *
     * @param mode the running mode.
     *
     * @return the message.
     */
    @Message(id = 10947, value = "Cannot start servers when the Host Controller running mode is %s")
    String cannotStartServersInvalidMode(RunningMode mode);

    /**
     * Creates an exception indicating the close should be managed by the service.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 10948, value = "Close should be managed by the service")
    UnsupportedOperationException closeShouldBeManagedByService();

    /**
     * Creates an exception indicating the configuration persister for the domain model is already initialized.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10949, value = "Configuration persister for domain model is already initialized")
    IllegalStateException configurationPersisterAlreadyInitialized();

    /**
     * Creates an exception indicating an interruption while trying to connect to master.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10950, value = "Interrupted while trying to connect to master")
    IllegalStateException connectionToMasterInterrupted();

    /**
     * Creates an exception indicating the connection to master could not be completed within the number of retries and
     * timeout.
     *
     * @param cause   the cause of the error.
     * @param retries the number of retries.
     * @param timeout the timeout in milliseconds..
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10951, value = "Could not connect to master in %d attempts within %s ms")
    IllegalStateException connectionToMasterTimeout(@Cause Throwable cause, int retries, long timeout);

    /**
     * Creates an exception indicating the server inventory could bot bre retrieved in the time.
     *
     * @param time     the time.
     * @param timeUnit the time unit.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10952, value = "Could not get the server inventory in %d %s")
    RuntimeException couldNotGetServerInventory(long time, String timeUnit);

    /**
     * Creates an exception indicating the entire file was not read.
     *
     * @param missing the missing length.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 10953, value = "Did not read the entire file. Missing: %d")
    IOException didNotReadEntireFile(long missing);

    /**
     * Creates an exception indicating there was an error closing down the host.
     *
     * @param cause the cause of the host.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10954, value = "Error closing down host")
    RuntimeException errorClosingDownHost(@Cause Throwable cause);

    /**
     * A message indicating a failure to retrieve the profile operations from the domain controller.
     *
     * @return the message.
     */
    @Message(id = 10955, value = "Failed to retrieve profile operations from domain controller")
    String failedProfileOperationsRetrieval();

    /**
     * Creates an exception indicating a failure to get the file from a remote repository.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10956, value = "Failed to get file from remote repository")
    RuntimeException failedToGetFileFromRemoteRepository(@Cause Throwable cause);

    /**
     * A message indicating a failure to get the server status.
     *
     * @return the message.
     */
    @Message(id = 10957, value = "Failed to get server status")
    String failedToGetServerStatus();

    /**
     * A message indicating a failure to read the authentication key.
     *
     * @param cause the cause of the error.
     *
     * @return the message.
     */
    @Message(id = 10958, value = "Failed to read authentication key: %s")
    String failedToReadAuthenticationKey(Throwable cause);

    /**
     * Creates an exception indicating there is already a connection for the host.
     *
     * @param hostName the name of the host.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10959, value = "Already have a connection for host %s")
    IllegalArgumentException hostNameAlreadyConnected(String hostName);

    /**
     * Creates an exception indicating the information provided could be not used to generate a hash.
     *
     * @return an {@link SaslException} for the error.
     */
    @Message(id = 10960, value = "Insufficient information to generate hash.")
    SaslException insufficientInformationToGenerateHash();

    /**
     * A message indicating the option is invalid.
     *
     * @param option the invalid option.
     *
     * @return the message.
     */
    @Message(id = 10961, value = "Invalid option '%s'")
    String invalidOption(String option);

    /**
     * Creates an exception indicating an invalid root id.
     *
     * @param rootId the invalid root id.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 10962, value = "Invalid root id [%d]")
    RequestProcessingException invalidRootId(int rootId);

    /**
     * A message indicating the value is invalid.
     *
     * @param name  the name of the option.
     * @param type  the type for the value.
     * @param value the value.
     *
     * @return the message.
     */
    @Message(id = 10963, value = "Value for %s is not an %s -- %s")
    String invalidValue(String name, String type, Object value);

    /**
     * Creates an exception indicating invocations of the operation, represented by the {@code name} parameter, after
     * HostController boot are not allowed.
     *
     * @param name the name of the operation.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10964, value = "Invocations of %s after HostController boot are not allowed")
    IllegalStateException invocationNotAllowedAfterBoot(String name);

    /**
     * Creates an exception indicating invocations of the operation after HostController boot are not allowed.
     *
     * @param operation the operation.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    String invocationNotAllowedAfterBoot(ModelNode operation);

    /**
     * A message indicating the provided for the option is malformed.
     *
     * @param option the option.
     *
     * @return the message.
     */
    @Message(id = 10965, value = "Malformed URL provided for option %s")
    String malformedUrl(String option);

    /**
     * Creates an exception indicating the need to call the method, represented by the {@code methodName} parameter,
     * before checking the slave status.
     *
     * @param methodName the name of the method to invoke.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10966, value = "Must call %s before checking for slave status")
    IllegalStateException mustInvokeBeforeCheckingSlaveStatus(String methodName);

    /**
     * Creates an exception indicating the need to call the method, represented by the {@code methodName} parameter,
     * before persisting the domain model.
     *
     * @param methodName the name of the method to invoke.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10967, value = "Must call %s before persisting the domain model")
    IllegalStateException mustInvokeBeforePersisting(String methodName);

    /**
     * Creates an exception indicating there is no channel for the host.
     *
     * @param hostName the name of the host.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10968, value = "No channel for host %s")
    IllegalArgumentException noChannelForHost(String hostName);

    /**
     * Creates an exception indicating a host connecting to a remove domain controller must have its name attribute
     * set.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10969, value = "A host connecting to a remote domain controller must have its name attribute set")
    IllegalArgumentException noNameAttributeOnHost();

    /**
     * Creates an exception indicating there is no server inventory.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10970, value = "No server inventory")
    IllegalStateException noServerInventory();

    /**
     * Creates an exception indicating the property already exists.
     *
     * @param name the name of the property.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10971, value = "Property %s already exists")
    IllegalArgumentException propertyAlreadyExists(String name);

    /**
     * Creates an exception indicating the property does not exist.
     *
     * @param name the name of the property.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10972, value = "Property %s does not exist")
    IllegalArgumentException propertyNotFound(String name);

    /**
     * Creates an exception indicating the value for the property is {@code null}.
     *
     * @param name the name of the property.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10973, value = "Value for property %s is null")
    IllegalArgumentException propertyValueNull(String name);

    /**
     * Creates an exception indicating the property has a {@code null} value.
     *
     * @param name the name of the property.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10974, value = "Property %s has a null value")
    IllegalStateException propertyValueHasNullValue(String name);

    /**
     * Creates an exception indicating the variable name is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10975, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating there is already a registered server with the name represented by the {@code
     * serverName} parameter.
     *
     * @param serverName the name of the server.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10976, value = "There is already a registered server named '%s'")
    IllegalArgumentException serverNameAlreadyRegistered(String serverName);

    /**
     * A message indicating the server, represented by the {@code name} parameter, is still running.
     *
     * @param name the name of the server.
     *
     * @return the message.
     */
    @Message(id = 10977, value = "Server (%s) still running")
    String serverStillRunning(String name);

    /**
     * Creates an exception indicating the inability to generate the hash.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link SaslException} for the error.
     */
    @Message(id = 10978, value = "Unable to generate hash")
    SaslException unableToGenerateHash(@Cause Throwable cause);

    /**
     * A message indicating the inability to load properties from the URL.
     *
     * @param url the URL.
     *
     * @return the message.
     */
    @Message(id = 10979, value = "Unable to load properties from URL %s")
    String unableToLoadProperties(URL url);

    /**
     * Creates an exception indicating the socket binding group for the server is undefined.
     *
     * @param serverName the name of the server.
     *
     * @return an {@link IllegalArgumentException} for the exception.
     */
    @Message(id = 10980, value = "Undefined socket binding group for server %s")
    IllegalArgumentException undefinedSocketBinding(String serverName);

    /**
     * Creates an exception indicating the socket binding group is undefined.
     *
     * @param name the name of the group.
     *
     * @return an {@link IllegalStateException} for the exception.
     */
    @Message(id = 10981, value = "Included socket binding group %s is not defined")
    IllegalStateException undefinedSocketBindingGroup(String name);

    /**
     * Creates an exception indicating the service state was unexpected.
     *
     * @param state the unexpected state.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10982, value = "Unexpected state %s")
    IllegalStateException unexpectedState(ServerState state);

    /**
     * Creates an exception indicating the {@code value} for the {@code name} is unknown.
     *
     * @param name  the name.
     * @param value the value.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10983, value = "Unknown %s %s")
    IllegalArgumentException unknown(String name, Object value);

    /**
     * A message indicating the value is not a known host.
     *
     * @param name  the name of the option.
     * @param value the value.
     *
     * @return the message.
     */
    @Message(id = 10984, value = "Value for %s is not a known host -- %s")
    String unknownHostValue(String name, Object value);

    /**
     * Creates an exception indicating the type is unrecognized.
     *
     * @param type the unrecognized type.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 10985, value = "unrecognized type %s")
    IOException unrecognizedType(byte type);

    /**
     * Creates an exception indication that the host controller was already shutdown.
     * @return an {@link Exception} for the error
     */
    @Message(id = 10986, value = "Host-Controller is already shutdown.")
    IllegalStateException hostAlreadyShutdown();

    /**
     * Creates an exception indicating no server group could be found with the given name
     *
     * @param groupName the profile name
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 10987, value = "No server group called: %s")
    OperationFailedException noServerGroupCalled(String groupName);

}
