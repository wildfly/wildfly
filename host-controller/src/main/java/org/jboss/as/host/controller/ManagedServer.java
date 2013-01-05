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

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.TransformingProxyController;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;

import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.as.controller.remote.TransactionalProtocolHandlers;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.Transformers;
import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;

import org.jboss.as.network.NetworkUtils;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.server.DomainServerCommunicationServices;
import org.jboss.as.server.ServerStartTask;
import org.jboss.dmr.ModelNode;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceActivator;

/**
 * Represents a managed server.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry
 * @author Emanuel Muckenhuber
 */
class ManagedServer {

    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;

    static {
        try {
            MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", Module.getModuleFromCallerModuleLoader(ModuleIdentifier.fromString("org.jboss.marshalling.river")).getClassLoader());
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
        final ClassLoader cl = ManagedServer.class.getClassLoader();
        final MarshallingConfiguration config = new MarshallingConfiguration();
        config.setVersion(2);
        config.setClassResolver(new SimpleClassResolver(cl));
        CONFIG = config;
    }

    /**
     * Prefix applied to a server's name to create it's process name.
     */
    public static String SERVER_PROCESS_NAME_PREFIX = "Server:";

    public static String getServerProcessName(String serverName) {
        return SERVER_PROCESS_NAME_PREFIX + serverName;
    }

    public static boolean isServerProcess(String serverProcessName) {
        return serverProcessName.startsWith(SERVER_PROCESS_NAME_PREFIX);
    }

    public static String getServerName(String serverProcessName) {
        return serverProcessName.substring(SERVER_PROCESS_NAME_PREFIX.length());
    }

    private final byte[] authKey;
    private final String serverName;
    private final String serverProcessName;
    private final String hostControllerName;

    private final InetSocketAddress managementSocket;
    private final ProcessControllerClient processControllerClient;
    private final ManagedServer.ManagedServerBootConfiguration bootConfiguration;

    private final ManagedServerProxy protocolClient;
    private final TransformationTarget transformationTarget;
    private final TransformingProxyController proxyController;

    private volatile boolean requiresReload;

    private volatile InternalState requiredState = InternalState.STOPPED;
    private volatile InternalState internalState = InternalState.STOPPED;

    ManagedServer(final String hostControllerName, final String serverName, final byte[] authKey,
                  final ProcessControllerClient processControllerClient, final InetSocketAddress managementSocket,
                  final ManagedServer.ManagedServerBootConfiguration bootConfiguration, final TransformationTarget transformationTarget) {

        assert hostControllerName  != null : "hostControllerName is null";
        assert serverName  != null : "serverName is null";
        assert processControllerClient != null : "processControllerSlave is null";
        assert managementSocket != null : "managementSocket is null";

        this.hostControllerName = hostControllerName;
        this.serverName = serverName;
        this.serverProcessName = getServerProcessName(serverName);
        this.processControllerClient = processControllerClient;
        this.managementSocket = managementSocket;
        this.bootConfiguration = bootConfiguration;
        this.transformationTarget = transformationTarget;

        this.authKey = authKey;

        // Setup the proxy controller
        final PathElement serverPath = PathElement.pathElement(RUNNING_SERVER, serverName);
        final PathAddress address = PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(HOST, hostControllerName), serverPath);
        this.protocolClient = new ManagedServerProxy(this, address);
        this.proxyController = TransformingProxyController.Factory.create(protocolClient,
                                Transformers.Factory.create(transformationTarget),
                                address, ProxyOperationAddressTranslator.SERVER);
    }

    /**
     * Get the process auth key.
     *
     * @return the auth key
     */
    byte[] getAuthKey() {
        return authKey;
    }

    /**
     * Get the server name.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Get the transforming proxy controller instance.
     *
     * @return the proxy controller
     */
    public TransformingProxyController getProxyController() {
        return proxyController;
    }

    /**
     * Determine the current state the server is in.
     *
     * @return the server status
     */
    public ServerStatus getState() {
        final InternalState requiredState = this.requiredState;
        final InternalState state = internalState;
        if(requiredState == InternalState.FAILED) {
            return ServerStatus.FAILED;
        }
        switch (state) {
            case STOPPED:
                return ServerStatus.STOPPED;
            case SERVER_STARTED:
                return  ServerStatus.STARTED;
            default: {
                if(requiredState == InternalState.SERVER_STARTED) {
                    return ServerStatus.STARTING;
                } else {
                    return ServerStatus.STOPPING;
                }
            }
        }
    }

    protected boolean isRequiresReload() {
        return requiresReload;
    }

    protected void requireReload() {
        requiresReload = true;
    }

    /**
     * Start a managed server.
     */
    protected synchronized void start() {
        final InternalState required = this.requiredState;
        // Ignore if the server is already started
        if(required == InternalState.SERVER_STARTED) {
            return;
        }
        // In case the server failed to start, try to start it again
        if(required != InternalState.FAILED) {
            final InternalState current = this.internalState;
            if(current != required) {
                // TODO this perhaps should wait?
                throw new IllegalStateException();
            }
        }
        this.requiredState = InternalState.SERVER_STARTED;
        ROOT_LOGGER.startingServer(serverName);
        transition();
    }

    /**
     * Stop a managed server.
     */
    protected synchronized void stop() {
        final InternalState required = this.requiredState;
        if(required != InternalState.STOPPED) {
            this.requiredState = InternalState.STOPPED;
            ROOT_LOGGER.stoppingServer(serverName);
            // Transition, but don't wait for async notifications to complete
            transition(false);
        }
    }

    /**
     * Try to reconnect to a started server.
     */
    protected synchronized void reconnectServerProcess() {
        if(this.requiredState != InternalState.SERVER_STARTED) {
            ROOT_LOGGER.reconnectingServer(serverName);
            this.requiredState = InternalState.SERVER_STARTED;
            internalSetState(new ReconnectTask(), InternalState.STOPPED, InternalState.SEND_STDIN);
        }
    }

    /**
     * On host controller reload, remove a not running server registered in the process controller declared as down.
     */
    protected synchronized void removeServerProcess() {
        this.requiredState = InternalState.STOPPED;
        internalSetState(new ProcessRemoveTask(), InternalState.STOPPED, InternalState.PROCESS_REMOVING);
    }

    /**
     * On host controller reload, remove a not running server registered in the process controller declared as stopping.
     */
    protected synchronized void setServerProcessStopping() {
        this.requiredState = InternalState.STOPPED;
        internalSetState(null, InternalState.STOPPED, InternalState.PROCESS_STOPPING);
    }

    /**
     * Await a state.
     *
     * @param expected the expected state
     * @return {@code true} if the state was reached, {@code false} otherwise
     */
    protected boolean awaitState(final InternalState expected) {
        synchronized (this) {
            final InternalState initialRequired = this.requiredState;
            for(;;) {
                final InternalState required = this.requiredState;
                // Stop in case the server failed to reach the state
                if(required == InternalState.FAILED) {
                    return false;
                // Stop in case the required state changed
                } else if (initialRequired != required) {
                    return false;
                }
                final InternalState current = this.internalState;
                if(expected == current) {
                    return true;
                }
                try {
                    wait();
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
    }

    /**
     * Notification that the process was added
     */
    protected void processAdded() {
        finishTransition(InternalState.PROCESS_ADDING, InternalState.PROCESS_ADDED);
    }

    /**
     * Notification that the process was started.
     */
    protected void processStarted() {
        finishTransition(InternalState.PROCESS_STARTING, InternalState.PROCESS_STARTED);
    }

    protected synchronized TransactionalProtocolClient channelRegistered(final ManagementChannelHandler channelAssociation) {
        final InternalState current = this.internalState;
        // Create the remote controller client
        final TransactionalProtocolClient remoteClient = TransactionalProtocolHandlers.createClient(channelAssociation);

        // TODO better handling for server :reload operation (reset reloadRequired)
        if(current == InternalState.SERVER_STARTED && proxyController == null) {
            // Update the current remote connection
            protocolClient.connected(remoteClient);
            this.requiresReload = false;
        } else {
            internalSetState(new TransitionTask() {
                @Override
                public void execute(final ManagedServer server) throws Exception {
                    // Update the current remote connection
                    protocolClient.connected(remoteClient);
                }
            // TODO we just check that we are in the correct state, perhaps introduce a new state
            }, InternalState.SEND_STDIN, InternalState.SERVER_STARTING);
        }
        return remoteClient;
    }

    protected synchronized void serverStarted(final TransitionTask task) {
        internalSetState(task, InternalState.SERVER_STARTING, InternalState.SERVER_STARTED);
    }

    protected synchronized void serverStartFailed() {
        internalSetState(null, InternalState.SERVER_STARTING, InternalState.FAILED);
    }

    /**
     * Unregister the mgmt channel.
     *
     * @param old the proxy controller to unregister
     * @param shuttingDown whether the server inventory is shutting down
     * @return whether the registration can be removed from the domain-controller
     */
    protected synchronized boolean callbackUnregistered(final TransactionalProtocolClient old, final boolean shuttingDown) {
        // Disconnect the remote connection
        protocolClient.disconnected(old);

        // If the connection dropped without us stopping the process ask for reconnection
        if(! shuttingDown && requiredState == InternalState.SERVER_STARTED) {
            final InternalState state = internalState;
            if(state == InternalState.PROCESS_STOPPED
                    || state == InternalState.PROCESS_STOPPING
                    || state == InternalState.STOPPED) {
                // In case it stopped we don't reconnect
                return true;
            }
            try {
                HostControllerLogger.ROOT_LOGGER.tracef("trying to reconnect to %s current-state (%s) required-state (%s)", serverName, state, requiredState);
                internalSetState(new ReconnectTask(), state, InternalState.SEND_STDIN);
            } catch (Exception e) {
                HostControllerLogger.ROOT_LOGGER.debugf(e, "failed to send reconnect task");
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * Notification that the server process finished.
     */
    protected synchronized void processFinished() {
        final InternalState required = this.requiredState;
        final InternalState state = this.internalState;
        // If the server was not stopped
        if(required == InternalState.STOPPED && state == InternalState.PROCESS_STOPPING) {
            finishTransition(InternalState.PROCESS_STOPPING, InternalState.PROCESS_STOPPED);
        } else {
            this.requiredState = InternalState.FAILED;
            internalSetState(null, state, InternalState.PROCESS_STOPPED);
        }
    }

    /**
     * Notification that the process got removed from the process controller.
     */
    protected void processRemoved() {
        finishTransition(InternalState.PROCESS_REMOVING, InternalState.STOPPED);
    }

    private void transition() {
        transition(true);
    }

    private synchronized void transition(boolean checkAsync) {
        final InternalState required = this.requiredState;
        final InternalState current = this.internalState;
        // Check if we are waiting for a notification from the server
        if(checkAsync && current.isAsync()) {
            return;
        }
        final InternalState next = nextState(current, required);
        if(next != null) {
            final TransitionTask task = getTransitionTask(next);
            internalSetState(task, current, next);
        }
    }

    /**
     * Notification that a state transition failed.
     *
     * @param state the failed transition
     */
    synchronized void transitionFailed(final InternalState state) {
        final InternalState current = this.internalState;
        if(state == current) {
            // Revert transition and mark as failed
            switch (current) {
                case PROCESS_ADDING:
                    this.internalState = InternalState.PROCESS_STOPPED;
                    break;
                case PROCESS_STARTED:
                    internalSetState(getTransitionTask(InternalState.PROCESS_STOPPING), InternalState.PROCESS_STARTED, InternalState.PROCESS_ADDED);
                    break;
                case PROCESS_STARTING:
                    this.internalState = InternalState.PROCESS_ADDED;
                    break;
                case SEND_STDIN:
                case SERVER_STARTING:
                    this.internalState = InternalState.PROCESS_STARTED;
                    break;
            }
            this.requiredState = InternalState.FAILED;
            notifyAll();
        }
    }

    /**
     * Finish a state transition from a notification.
     *
     * @param current
     * @param next
     */
    private synchronized void finishTransition(final InternalState current, final InternalState next) {
        internalSetState(getTransitionTask(next), current, next);
        transition();
    }

    private boolean internalSetState(final TransitionTask task, final InternalState current, final InternalState next) {
        assert Thread.holdsLock(this); // Call under lock
        final InternalState internalState = this.internalState;
        if(internalState == current) {
            try {
                if(task != null) {
                    task.execute(this);
                }
                this.internalState = next;
                return true;
            } catch (final Exception e) {
                ROOT_LOGGER.debugf(e, "transition (%s > %s) failed for server \"%s\"", current, next, serverName);
                transitionFailed(current);
            } finally {
                notifyAll();
            }
        }
        return false;
    }

    private TransitionTask getTransitionTask(final InternalState next) {
        switch (next) {
            case PROCESS_ADDING: {
                return new ProcessAddTask();
            } case PROCESS_STARTING: {
                return new ProcessStartTask();
            } case SEND_STDIN: {
                return new SendStdInTask();
            } case SERVER_STARTED: {
                return new ServerStartedTask();
            } case PROCESS_STOPPING: {
                return new ServerStopTask();
            } case PROCESS_REMOVING: {
                return new ProcessRemoveTask();
            } default: {
                return null;
            }
        }
    }

    private static InternalState nextState(final InternalState state, final InternalState required) {
        switch (state) {
            case STOPPED: {
                if(required == InternalState.SERVER_STARTED) {
                    return InternalState.PROCESS_ADDING;
                }
                break;
            } case PROCESS_ADDING: {
                if(required == InternalState.SERVER_STARTED) {
                    return InternalState.PROCESS_ADDED;
                }
                break;
            } case PROCESS_ADDED: {
                if(required == InternalState.SERVER_STARTED) {
                    return InternalState.PROCESS_STARTING;
                } else if( required == InternalState.STOPPED) {
                    return InternalState.PROCESS_REMOVING;
                }
                break;
            } case PROCESS_STARTING: {
                if(required == InternalState.SERVER_STARTED) {
                    return InternalState.PROCESS_STARTED;
                }
                break;
            } case PROCESS_STARTED: {
                if(required == InternalState.SERVER_STARTED) {
                    return InternalState.SEND_STDIN;
                } else if( required == InternalState.STOPPED) {
                    return InternalState.PROCESS_STOPPING;
                }
                break;
            } case SEND_STDIN: {
                if(required == InternalState.SERVER_STARTED) {
                    return InternalState.SERVER_STARTING;
                } else if( required == InternalState.STOPPED) {
                    return InternalState.PROCESS_STOPPING;
                }
                break;
            } case SERVER_STARTING: {
                if(required == InternalState.SERVER_STARTED) {
                    return InternalState.SERVER_STARTED;
                } else if( required == InternalState.STOPPED) {
                    return InternalState.PROCESS_STOPPING;
                }
                break;
            } case SERVER_STARTED: {
                if(required == InternalState.STOPPED) {
                    return InternalState.PROCESS_STOPPING;
                }
                break;
            } case PROCESS_STOPPING: {
                if(required == InternalState.STOPPED) {
                    return InternalState.PROCESS_STOPPED;
                }
                break;
            } case PROCESS_STOPPED: {
                if(required == InternalState.SERVER_STARTED) {
                    return InternalState.PROCESS_STARTING;
                } else if( required == InternalState.STOPPED) {
                    return InternalState.PROCESS_REMOVING;
                }
                break;
            } case PROCESS_REMOVING: {
                if(required == InternalState.STOPPED) {
                    return InternalState.STOPPED;
                }
                break;
            } default: {
                return null;
            }
        }
        return null;
    }

    /**
     * The managed server boot configuration.
     */
    public interface ManagedServerBootConfiguration {
        /**
         * Get the server launch environment.
         *
         * @return the launch environment
         */
        Map<String, String> getServerLaunchEnvironment();

        /**
         * Get server launch command.
         *
         * @return the launch command
         */
        List<String> getServerLaunchCommand();

        /**
         * Get the host controller environment.
         *
         * @return the host controller environment
         */
        HostControllerEnvironment getHostControllerEnvironment();

        /**
         * Get whether the native management remoting connector should use the endpoint set up by
         */
        boolean isManagementSubsystemEndpoint();

        /**
         * Get the subsystem endpoint configuration, in case we use the subsystem.
         *
         * @return the subsystem endpoint config
         */
        ModelNode getSubsystemEndpointConfiguration();

    }

    static enum InternalState {

        STOPPED,
        PROCESS_ADDING(true),
        PROCESS_ADDED,
        PROCESS_STARTING(true),
        PROCESS_STARTED,
        SEND_STDIN(true),
        SERVER_STARTING(true),
        SERVER_STARTED,
        PROCESS_STOPPING(true),
        PROCESS_STOPPED,
        PROCESS_REMOVING(true),

        FAILED,
        ;

        /** State transition creates an async task. */
        private final boolean async;

        InternalState() {
            this(false);
        }

        InternalState(boolean async) {
            this.async = async;
        }

        public boolean isAsync() {
            return async;
        }
    }

    interface TransitionTask {

        void execute(ManagedServer server) throws Exception;

    }


    private class ProcessAddTask implements TransitionTask {

        @Override
        public void execute(ManagedServer server) throws Exception {
            assert Thread.holdsLock(ManagedServer.this); // Call under lock
            final List<String> command = bootConfiguration.getServerLaunchCommand();
            final Map<String, String> env = bootConfiguration.getServerLaunchEnvironment();
            final HostControllerEnvironment environment = bootConfiguration.getHostControllerEnvironment();
            // Add the process to the process controller
            processControllerClient.addProcess(serverProcessName, authKey, command.toArray(new String[command.size()]), environment.getHomeDir().getAbsolutePath(), env);
        }

    }

    private class ProcessRemoveTask implements TransitionTask {
        @Override
        public void execute(ManagedServer server) throws Exception {
            assert Thread.holdsLock(ManagedServer.this); // Call under lock
            // Remove process
            processControllerClient.removeProcess(serverProcessName);
        }
    }


    private class ProcessStartTask implements TransitionTask {

        @Override
        public void execute(ManagedServer server) throws Exception {
            assert Thread.holdsLock(ManagedServer.this); // Call under lock
            // Start the process
            processControllerClient.startProcess(serverProcessName);
        }

    }

    private class SendStdInTask implements TransitionTask {

        @Override
        public void execute(ManagedServer server) throws Exception {
            assert Thread.holdsLock(ManagedServer.this); // Call under lock
            // Get the standalone boot updates
            final List<ModelNode> bootUpdates = Collections.emptyList(); // bootConfiguration.getBootUpdates();
            final Map<String, String> launchProperties = parseLaunchProperties(bootConfiguration.getServerLaunchCommand());
            final boolean useSubsystemEndpoint = bootConfiguration.isManagementSubsystemEndpoint();
            final ModelNode endpointConfig = bootConfiguration.getSubsystemEndpointConfiguration();
            // Send std.in
            final ServiceActivator hostControllerCommActivator = DomainServerCommunicationServices.create(endpointConfig, managementSocket, serverName, serverProcessName, authKey, useSubsystemEndpoint);
            final ServerStartTask startTask = new ServerStartTask(hostControllerName, serverName, 0, Collections.<ServiceActivator>singletonList(hostControllerCommActivator), bootUpdates, launchProperties);
            final Marshaller marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
            final OutputStream os = processControllerClient.sendStdin(serverProcessName);
            marshaller.start(Marshalling.createByteOutput(os));
            marshaller.writeObject(startTask);
            marshaller.finish();
            marshaller.close();
            os.close();
        }
    }

    private class ServerStartedTask implements TransitionTask {

        @Override
        public void execute(ManagedServer server) throws Exception {
            //
        }

    }

    private class ServerStopTask implements TransitionTask {

        @Override
        public void execute(ManagedServer server) throws Exception {
            assert Thread.holdsLock(ManagedServer.this); // Call under lock
            // Stop process
            processControllerClient.stopProcess(serverProcessName);
        }
    }

    private class ReconnectTask implements TransitionTask {

        @Override
        public void execute(ManagedServer server) throws Exception {
            assert Thread.holdsLock(ManagedServer.this); // Call under lock
            // Reconnect
            final String hostName = InetAddress.getByName(managementSocket.getHostName()).getHostName();
            final int port = managementSocket.getPort();
            processControllerClient.reconnectProcess(serverProcessName, NetworkUtils.formatPossibleIpv6Address(hostName), port, bootConfiguration.isManagementSubsystemEndpoint(), authKey);
        }
    }

    private static Map<String, String> parseLaunchProperties(final List<String> commands) {
        final Map<String, String> result = new LinkedHashMap<String, String>();
        for (String cmd : commands) {
            if (cmd.startsWith("-D")) {
                final String[] parts = cmd.substring(2).split("=");
                if (parts.length == 2) {
                    result.put(parts[0], parts[1]);
                } else if (parts.length == 1) {
                    result.put(parts[0], "true");
                }
            }
        }
        return result;
    }

}
