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
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;
import org.jboss.as.process.ProcessControllerClient;
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
import org.jboss.remoting3.Channel;

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

    private Channel.Key closeHandlerRegistration;
    private Channel serverManagementChannel;
    private volatile int respawnCount;

    private volatile InternalState requiredState = InternalState.STOPPED;
    private volatile InternalState internalState = InternalState.STOPPED;

    private static final AtomicIntegerFieldUpdater<ManagedServer> respawnCountUpdater = AtomicIntegerFieldUpdater.newUpdater(ManagedServer.class, "respawnCount");

    ManagedServer(final String hostControllerName, final String serverName, final ProcessControllerClient processControllerClient,
            final InetSocketAddress managementSocket, final ManagedServer.ManagedServerBootConfiguration bootConfiguration) {

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

        final byte[] authKey = new byte[16];
        new Random(new SecureRandom().nextLong()).nextBytes(authKey);
        this.authKey = authKey;
    }

    byte[] getAuthKey() {
        return authKey;
    }

    public String getServerName() {
        return serverName;
    }

    /**
     * Determine the current state the server is in.
     *
     * @return the server status
     */
    public ServerStatus getState() {
        final boolean start = this.requiredState == InternalState.SERVER_STARTED;
        final InternalState state = internalState;
        switch (state) {
            case STOPPED:
                return ServerStatus.STOPPED;
            case SERVER_STARTED:
                return  ServerStatus.STARTED;
            case FAILED:
                return ServerStatus.FAILED;
            default: {
                if(start) {
                    return ServerStatus.STARTING;
                } else {
                    return ServerStatus.STOPPING;
                }
            }
        }
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
        resetRespawnCount();
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
            internalSetState(new ReconnectTask(), InternalState.STOPPED, InternalState.SERVER_STARTING);
        }
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

    /**
     * Register the management channel.
     *
     * @param task the transition task
     * @param mgmtChannel the channel
     * @param registration the close handler
     */
    protected synchronized void callbackRegistered(final TransitionTask task, final Channel mgmtChannel, final Channel.Key registration) {
        this.serverManagementChannel = mgmtChannel;
        this.closeHandlerRegistration = registration;
        // TODO use the mgmt protocol to signal if the server is started
        internalSetState(task, InternalState.SERVER_STARTING, InternalState.SERVER_STARTED);
    }

    /**
     * Unregister the mgmt channel.
     */
    protected synchronized void callbackUnregistered() {
        final Channel.Key registration = this.closeHandlerRegistration;
        this.closeHandlerRegistration = null;
        this.serverManagementChannel = null;
        if(registration != null) {
            registration.remove();
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
            // In any case it's stopped now
            this.internalState = InternalState.PROCESS_STOPPED;
            if(respawnCountUpdater.incrementAndGet(this) >= 10) {
                this.requiredState = InternalState.FAILED;
                return;
            }
            this.internalState = required == InternalState.SERVER_STARTED ? InternalState.PROCESS_ADDED : InternalState.PROCESS_STOPPED;
            ROOT_LOGGER.infof("respawning server: %s", serverName);
            transition();
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
                case PROCESS_STARTING:
                    this.internalState = InternalState.PROCESS_ADDED;
                    break;
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

    private void internalSetState(final TransitionTask task, final InternalState current, final InternalState next) {
        assert Thread.holdsLock(this); // Call under lock
        final InternalState internalState = this.internalState;
        if(internalState == current) {
            try {
                if(task != null) {
                    task.execute(this);
                }
                this.internalState = next;
            } catch (final Exception e) {
                transitionFailed(current);
            } finally {
                notifyAll();
            }
        }
    }

    private TransitionTask getTransitionTask(final InternalState next) {
        switch (next) {
            case PROCESS_ADDING: {
                return new ProcessAddTask();
            } case PROCESS_STARTING: {
                return new ProcessStartTask();
            } case SERVER_STARTING: {
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

    /**
     * Reset the respawn count
     *
     * @return the respawn count
     */
    private int resetRespawnCount() {
        return respawnCountUpdater.getAndSet(this, 0);
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
                    return InternalState.SERVER_STARTING;
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
    public static interface ManagedServerBootConfiguration {
        /**
         * Get a list of boot updates.
         *
         * @return the boot updates
         */
        List<ModelNode> getBootUpdates();

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
         * @param processName
         */
        List<String> getServerLaunchCommand(String processName);

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
    }

    static enum InternalState {

        STOPPED,
        PROCESS_ADDING(true),
        PROCESS_ADDED,
        PROCESS_STARTING(true),
        PROCESS_STARTED,
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

    static interface TransitionTask {

        void execute(ManagedServer server) throws Exception;

    }


    private class ProcessAddTask implements TransitionTask {

        @Override
        public void execute(ManagedServer server) throws Exception {
            assert Thread.holdsLock(ManagedServer.this); // Call under lock
            final List<String> command = bootConfiguration.getServerLaunchCommand(serverProcessName);
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
            final List<ModelNode> bootUpdates = bootConfiguration.getBootUpdates();
            // Send std.in
            final ServiceActivator hostControllerCommActivator = HostCommunicationServices.createServerCommuncationActivator(managementSocket, serverName, serverProcessName, authKey, bootConfiguration.isManagementSubsystemEndpoint());
            final ServerStartTask startTask = new ServerStartTask(hostControllerName, serverName, 0, Collections.<ServiceActivator>singletonList(hostControllerCommActivator), bootUpdates);
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
            resetRespawnCount();
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
            processControllerClient.reconnectProcess(serverProcessName, managementSocket.getAddress().getHostName(), managementSocket.getPort(), bootConfiguration.isManagementSubsystemEndpoint(), authKey);
        }
    }

}
