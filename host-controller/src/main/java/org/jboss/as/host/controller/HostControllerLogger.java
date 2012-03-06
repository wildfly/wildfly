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

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.server.ServerState;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.remoting3.Channel;

import java.io.IOException;

/**
 * This module is using message IDs in the range 10800-10999. This file is using the subset 10900-10939 for host
 * controller logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 * <p/>
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface HostControllerLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    HostControllerLogger ROOT_LOGGER = Logger.getMessageLogger(HostControllerLogger.class, HostControllerLogger.class.getPackage().getName());

    /**
     * A logger with a category of {@code org.jboss.as}
     */
    HostControllerLogger AS_ROOT_LOGGER = Logger.getMessageLogger(HostControllerLogger.class, "org.jboss.as");

    /**
     * A logger with a category of {@code org.jboss.as.domain.controller.mgmt}.
     */
    HostControllerLogger CONTROLLER_MANAGEMENT_LOGGER = Logger.getMessageLogger(HostControllerLogger.class, "org.jboss.as.domain.controller.mgmt");

    /**
     * A logger with a category of {@code org.jboss.as.domain}
     */
    HostControllerLogger DOMAIN_LOGGER = Logger.getMessageLogger(HostControllerLogger.class, "org.jboss.as.domain");

    /**
     * Logs a warning message indicating the remote domain controller could not connect.
     *
     * @param hostAddress the host name.
     * @param port        the port.
     * @param cause       the cause
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10900, value = "Could not connect to remote domain controller %s:%d: %s")
    void cannotConnect(String hostAddress, int port, IOException cause);

    /**
     * Logs an error message indicating this host is a slave and cannot connect to the master host controller.
     *
     * @param e the cause of the error.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 10901, value = "Could not connect to master. Aborting. Error was: %s")
    void cannotConnectToMaster(Exception e);

    /**
     * Logs an informational message indicating the creation of HTTP management server using the network interface.
     *
     * @param interfaceName the interface name.
     * @param port          the port number.
     * @param securePort    the secure port number.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10902, value = "Creating http management service using network interface (%s) port (%d) securePort (%d)")
    void creatingHttpManagementService(String interfaceName, int port, int securePort);

    /**
     * Logs a warning message indicating an error retrieving domain model from the remote domain controller.
     *
     * @param hostName     the name of the host.
     * @param port         the port number.
     * @param errorMessage the error message.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10903, value = "Error retrieving domain model from remote domain controller %s:%d: %s")
    void errorRetrievingDomainModel(String hostName, int port, String errorMessage);

    /**
     * Logs a warning message indicating the existing server is already defined with a different state.
     *
     * @param serverName the name of the server.
     * @param state      the current state.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10904, value = "Existing server [%s] with status: %s")
    void existingServerWithState(String serverName, ServerStatus state);

    /**
     * Logs an error message indicating a failure to create a server process.
     *
     * @param cause      the cause of the error.
     * @param serverName the server name that failed.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 10905, value = "Failed to create server process %s")
    void failedToCreateServerProcess(@Cause Throwable cause, String serverName);

    /**
     * Logs an error message indicating a failure to send a reconnect message to the server.
     *
     * @param cause      the cause of the error.
     * @param serverName the server name.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 10906, value = "Failed to send reconnect message to server %s")
    void failedToSendReconnect(@Cause Throwable cause, String serverName);

    /**
     * Logs an error message indicating a failure to start the server, represented by the {@code serverName} parameter.
     *
     * @param cause      the cause of the error.
     * @param serverName the name of the server.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 10907, value = "Failed to start server (%s)")
    void failedToStartServer(@Cause Throwable cause, String serverName);

    /**
     * Logs an error message indicating a failure to stop the server, represented by the {@code serverName} parameter.
     *
     * @param cause      the cause of the error.
     * @param serverName the name of the server.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 10908, value = "Failed to stop server (%s)")
    void failedToStopServer(@Cause Throwable cause, String serverName);

    /**
     * Logs a warning message indicating graceful shutdown of servers is not supported.
     *
     * @param serverName the name fo the server.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10909, value = "Graceful shutdown of server %s was requested but is not presently supported. Falling back to rapid shutdown.")
    void gracefulShutdownNotSupported(String serverName);

    /**
     * Logs a warning message indicating {@literal <permgen>} is being ignored.
     *
     * @param type the jvm type.
     * @param jvm  the jvm.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10910, value = "Ignoring <permgen> for jvm '%s' type jvm: %s")
    void ignoringPermGen(JvmType type, String jvm);

    /**
     * Logs an error message indicating this host had no domain controller configuration and cannot start if not in
     * {@link org.jboss.as.controller.RunningMode#ADMIN_ONLY} mode.
     *
     * @return a message for the error.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 10911, value = "No <domain-controller> configuration was provided and the current running mode ('%s') " +
            "requires access to the Domain Controller host. Startup will be aborted. Use the %s command line argument " +
            "to start in %s mode if you need to start without a domain controller connection and then use the management " +
            "tools to configure one.")
    void noDomainControllerConfigurationProvided(RunningMode currentRunningMode, String adminOnlyCmdLineArg, RunningMode validRunningMode);

    /**
     * Logs a warning message indicating no security realm was defined for the HTTP management service. All access will
     * be unrestricted.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10912, value = "No security realm defined for http management service, all access will be unrestricted.")
    void noSecurityRealmDefined();

    /**
     * Logs an error message indicating no server with the server name is available.
     *
     * @param serverName the name of the server.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 10913, value = "No server called %s available")
    void noServerAvailable(String serverName);

    /**
     * Logs an error message indicating the connection to the remote host controller closed.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10914, value = "Connection to remote host-controller closed. Trying to reconnect.")
    void lostRemoteDomainConnection();

    /**
     * Logs a warning message indicating the option for the jvm was already set and is being ignored.
     *
     * @param option  the option.
     * @param jvm     the jvm.
     * @param element the schema element.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10915, value = "Ignoring <option value=\"%s\" for jvm '%s' since '%s' was set")
    void optionAlreadySet(String option, String jvm, String element);

    /**
     * Logs an informational message indicating a reconnection to master.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10916, value = "Reconnected to master")
    void reconnectedToMaster();

    /**
     * Logs an informational message indicating the server is being reconnected.
     *
     * @param serverName the name of the server.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10917, value = "Reconnecting server %s")
    void reconnectingServer(String serverName);

    /**
     * Logs an informational message indicating the host has been registered as a remote slave.
     *
     * @param hostName the host name
     * @param productName the product name
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10918, value = "Registered remote slave host \"%s\", %s")
    void registeredRemoteSlaveHost(String hostName, String productName);

    /**
     * Logs an informational message indicating the server, represented by the {@code name} parameter, is being
     * registered.
     *
     * @param name the name of the server.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10919, value = "Registering server %s")
    void registeringServer(String name);

    /**
     * Logs an informational message indicating the server, represented by the {@code name} parameter, was registered
     * using the connection represented by the {@code channel} parameter.
     *
     * @param name    the name of the server.
     * @param channel the channel used to register the connection.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10920, value = "Server [%s] registered using connection [%s]")
    void serverRegistered(String name, Channel channel);

    /**
     * Logs a warning message indicating the service shutdown did not complete.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10921, value = "Service shutdown did not complete")
    void serviceShutdownIncomplete(@Cause Throwable cause);

    /**
     * Logs an informational message indicating the server is starting.
     *
     * @param serverName the name of the server that is starting.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10922, value = "Starting server %s")
    void startingServer(String serverName);

    /**
     * Logs an informational message indicating the server is stopping.
     *
     * @param serverName the name of the server.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10923, value = "Stopping server %s")
    void stoppingServer(String serverName);

    /**
     * Logs a warning message indicating the server is not in the expected state.
     *
     * @param serverName the name of the server.
     * @param expected   the expected state.
     * @param current    the current state.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10924, value = "Server %s is not in the expected %s state: %s")
    void unexpectedServerState(String serverName, ServerState expected, ServerState current);

    /**
     * Logs an informational message indicating the host has been unregistered as a remote slave.
     *
     * @param host the host.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10925, value = "Unregistered remote slave host \"%s\"")
    void unregisteredRemoteSlaveHost(String host);

    /**
     * Logs an informational message indicating the server, represented by the {@code name} parameter, is being
     * unregistered.
     *
     * @param name the name of the server.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10926, value = "Unregistering server %s")
    void unregisteringServer(String name);

    /**
     * Informal log message indicating the local host registered at the remote domain controller.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10927, value =  "Registered at domain controller")
    void registeredAtRemoteHostController();

    /**
     * Informal log message indicating the local host unregistered at the remote domain controller.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10928, value =  "Unregistered at domain controller")
    void unregisteredAtRemoteHostController();

    @LogMessage(level = Level.WARN)
    @Message(id = 10929, value = "Connection to remote host \"%s\" closed unexpected")
    void lostConnectionToRemoteHost(String hostId);

    @LogMessage(level = Level.WARN)
    @Message(id = 10930, value = "Cannot load the domain model using using --backup")
    void invalidRemoteBackupPersisterState();

    @LogMessage(level = Level.WARN)
    @Message(id = 10931, value = "Cannot store the domain model using using --cached-dc")
    void invalidCachedPersisterState();

    @LogMessage(level = Level.ERROR)
    @Message(id = 10932, value = "Caught exception during boot")
    void caughtExceptionDuringBoot(@Cause Exception e);

    @LogMessage(level = Level.FATAL)
    @Message(id = 10933, value = "Host Controller boot has failed in an unrecoverable manner; exiting. See previous messages for details.")
    void unsuccessfulBoot();

    @LogMessage(level = Level.ERROR)
    @Message(id = 10934, value = "Installation of the domain-wide configuration has failed. Because the running mode of this Host Controller is ADMIN_ONLY boot has been allowed to proceed. If ADMIN_ONLY mode were not in effect the process would be terminated due to a critical boot failure.")
    void reportAdminOnlyDomainXmlFailure();
}
